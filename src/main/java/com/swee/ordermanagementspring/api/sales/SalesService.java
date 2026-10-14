package com.swee.ordermanagementspring.api.sales;

import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.common.DbSessionContext;
import com.swee.ordermanagementspring.api.common.MoneyDto;
import com.swee.ordermanagementspring.api.common.PageResponse;
import com.swee.ordermanagementspring.api.sales.SalesDtos.*;
import com.swee.ordermanagementspring.api.sales.SalesRepository.SaleProduct;
import com.swee.ordermanagementspring.entities.StoreTime;
import com.swee.ordermanagementspring.entities.audit.AuditAction;
import com.swee.ordermanagementspring.entities.enums.Fulfillment;
import com.swee.ordermanagementspring.exceptions.ApiException;
import com.swee.ordermanagementspring.exceptions.ErrorCode;
import com.swee.ordermanagementspring.security.AuditService;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Casos de uso do PDV (RF-19). Preco, desconto e total so no servidor; reserva por UPDATE condicional em ordem
 * crescente de product_id; saida so na confirmacao; o vendedor do pedido e o usuario da sessao.
 */
@Service
public class SalesService {
    private final SalesRepository sales;
    private final JdbcClient jdbc;
    private final DbSessionContext session;
    private final AuditService audit;

    public SalesService(SalesRepository sales, JdbcClient jdbc, DbSessionContext session, AuditService audit) {
        this.sales = sales;
        this.jdbc = jdbc;
        this.session = session;
        this.audit = audit;
    }

    // ---------------- catalogo e cotacao ----------------

    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional(readOnly = true)
    public PageResponse<CatalogItem> catalog(String q, boolean onlyAvailable, int page, int size) {
        String term = q == null ? "" : q.trim();
        List<Pricing.Promotion> promotions = sales.activePromotions();
        List<CatalogItem> items = sales.catalog(term, onlyAvailable, page, size).stream()
                .map(p -> new CatalogItem(p.id(), p.sku(), p.name(), SalesRepository.category(p.categoryId(), p.categoryName()),
                        MoneyDto.of(p.price()), Math.max(p.available(), 0),
                        Pricing.best(promotions, p.id(), p.categoryId(), p.price()).map(SalesService::active).orElse(null)))
                .toList();
        return new PageResponse<>(items, page, size, sales.catalogCount(term, onlyAvailable));
    }

    private static ActivePromotion active(Pricing.Promotion p) {
        return new ActivePromotion(p.id(), p.name(), p.kind(), p.value(), p.productId(), p.categoryId(), p.endsAt());
    }

    /** Item ja precificado: base da cotacao e da venda. */
    record PricedItem(SaleProduct product, int quantity, Pricing.Promotion promotion, BigDecimal unitPrice,
                      BigDecimal discount, BigDecimal subtotal) { }

    private List<PricedItem> price(List<SaleItemRequest> items) {
        Map<Long, SaleProduct> products = sales.products(items.stream().map(SaleItemRequest::productId).toList())
                .stream().collect(Collectors.toMap(SaleProduct::id, Function.identity()));
        List<Pricing.Promotion> promotions = sales.activePromotions();
        List<PricedItem> priced = new ArrayList<>();
        for (SaleItemRequest item : items) {
            SaleProduct product = products.get(item.productId());
            if (product == null) throw ApiException.notFound("Product not found, id: " + item.productId());
            if (!product.active() || !product.sellable()) {
                throw ApiException.conflict(ErrorCode.STATE_CONFLICT, "Product " + product.id() + " is inactive or not for sale");
            }
            Pricing.Promotion promotion = null;
            if (item.promotionId() != null) {
                promotion = promotions.stream().filter(p -> p.id() == item.promotionId()
                                && p.appliesTo(product.id(), product.categoryId())).findFirst()
                        .orElseThrow(() -> ApiException.conflict(ErrorCode.STATE_CONFLICT,
                                "Promotion " + item.promotionId() + " is not active for product " + product.id()));
            }
            BigDecimal discount = Pricing.discount(product.price(), item.quantity(), promotion);
            BigDecimal gross = Pricing.gross(product.price(), item.quantity());
            priced.add(new PricedItem(product, item.quantity(), promotion, product.price(), discount, gross.subtract(discount)));
        }
        return priced;
    }

    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional(readOnly = true)
    public QuoteResponse quote(QuoteRequest request) {
        List<PricedItem> priced = price(request.items());
        List<QuoteItem> items = priced.stream().map(i -> new QuoteItem(i.product().id(), i.quantity(), MoneyDto.of(i.unitPrice()),
                MoneyDto.of(i.discount()), MoneyDto.of(i.subtotal()), Math.max(i.product().available(), 0))).toList();
        BigDecimal gross = priced.stream().map(i -> Pricing.gross(i.unitPrice(), i.quantity())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal discount = priced.stream().map(PricedItem::discount).reduce(BigDecimal.ZERO, BigDecimal::add);
        return new QuoteResponse(items, MoneyDto.of(gross), MoneyDto.of(discount), MoneyDto.of(gross.subtract(discount)));
    }

    // ---------------- venda ----------------

    /** Resultado da criacao: created = false quando a mesma chave e o mesmo corpo ja tinham criado o pedido. */
    public record Created(long orderId, boolean created) { }

    /** Repeticao com a mesma chave: mesmo corpo devolve o pedido; corpo diferente e conflito (regra 8). */
    @Transactional(readOnly = true)
    public java.util.Optional<Created> existing(CurrentUser user, UUID key, String hash) {
        return sales.byRequestKey(user.id(), key).map(found -> {
            if (!found.requestHash().equals(hash)) {
                throw ApiException.conflict(ErrorCode.IDEMPOTENCY_CONFLICT, "Idempotency-Key already used with a different body");
            }
            return new Created(found.id(), false);
        });
    }

    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional
    public Created create(CurrentUser user, UUID key, String hash, SalesOrderRequest request) {
        session.apply(user.id(), null);
        if (jdbc.sql("SELECT count(*) FROM customer WHERE id = :id").param("id", request.customerId()).query(Long.class).single() == 0) {
            throw ApiException.notFound("Customer not found, id: " + request.customerId());
        }
        List<PricedItem> priced = price(request.items());
        priced.sort(Comparator.comparingLong(i -> i.product().id()));

        Long addressId = null;
        if (request.fulfillment() == Fulfillment.DELIVERY) {
            AddressRequest a = request.shippingAddress();
            addressId = jdbc.sql("""
                    INSERT INTO address(street, number, complement, neighborhood, city, state, zip_code)
                    VALUES (:street, :number, :complement, :neighborhood, :city, :state, :zip) RETURNING id""")
                    .param("street", a.street()).param("number", a.number()).param("complement", a.complement())
                    .param("neighborhood", a.neighborhood()).param("city", a.city()).param("state", a.state())
                    .param("zip", a.zipCode()).query(Long.class).single();
        }
        // O pedido entra primeiro: a UNIQUE (seller_id, client_request_id) reivindica a chave.
        long orderId = jdbc.sql("""
                INSERT INTO orders(moment, status, customer_id, seller_id, fulfillment, address_id, client_request_id, request_hash)
                VALUES (:moment, 'PENDING_PAYMENT', :customer, :seller, :fulfillment, :address, :key, :hash) RETURNING id""")
                .param("moment", StoreTime.now()).param("customer", request.customerId()).param("seller", user.id())
                .param("fulfillment", request.fulfillment().name()).param("address", addressId)
                .param("key", key).param("hash", hash).query(Long.class).single();

        BigDecimal total = BigDecimal.ZERO;
        for (PricedItem item : priced) {
            int reserved = jdbc.sql("""
                    UPDATE stock_balance SET reserved = reserved + :q, updated_at = now()
                     WHERE product_id = :id AND on_hand - reserved >= :q""")
                    .param("q", item.quantity()).param("id", item.product().id()).update();
            if (reserved == 0) {
                int available = jdbc.sql("SELECT on_hand - reserved FROM stock_balance WHERE product_id = :id")
                        .param("id", item.product().id()).query(Integer.class).single();
                throw new ApiException(HttpStatus.CONFLICT, ErrorCode.STOCK_INSUFFICIENT,
                        "Insufficient stock for product " + item.product().id(),
                        List.of("productId=" + item.product().id() + "; available=" + Math.max(available, 0)));
            }
            jdbc.sql("""
                    INSERT INTO stock_movement(product_id, order_id, kind, quantity, created_by)
                    VALUES (:product, :order, 'RESERVA', :q, :user)""")
                    .param("product", item.product().id()).param("order", orderId).param("q", item.quantity())
                    .param("user", user.id()).update();
            jdbc.sql("""
                    INSERT INTO order_item(quantity, price, order_id, product_id, promotion_id, discount)
                    VALUES (:q, :price, :order, :product, :promotion, :discount)""")
                    .param("q", item.quantity()).param("price", item.unitPrice()).param("order", orderId)
                    .param("product", item.product().id()).param("promotion", item.promotion() == null ? null : item.promotion().id())
                    .param("discount", item.discount()).update();
            total = total.add(item.subtotal());
        }

        var payment = request.payment();
        jdbc.sql("""
                INSERT INTO payment(payment_type, amount, status, order_id, due_date, installments, card_last4, authorization_code)
                VALUES (:type, :amount, 'PENDING', :order, :due, :installments, :last4, :auth)""")
                .param("type", payment.type()).param("amount", total).param("order", orderId)
                .param("due", payment instanceof BoletoPaymentRequest b ? b.dueDate() : null)
                .param("installments", payment instanceof CardPaymentRequest c ? c.installments() : null)
                .param("last4", payment instanceof CardPaymentRequest c ? c.cardLast4() : null)
                .param("auth", payment instanceof CardPaymentRequest c ? c.authorizationCode() : null)
                .update();
        return new Created(orderId, true);
    }

    // ---------------- consulta ----------------

    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional(readOnly = true)
    public SalesOrderResponse get(CurrentUser user, long orderId) {
        var head = sales.head(orderId, false).orElseThrow(() -> ApiException.notFound("Order not found, id: " + orderId));
        if (!user.owns(head.sellerId())) throw ApiException.notFound("Order not found, id: " + orderId);
        return sales.order(orderId).orElseThrow();
    }

    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional(readOnly = true)
    public PageResponse<SalesOrderSummary> list(CurrentUser user, String status, Long sellerId, int page, int size) {
        Long seller = sellerId;
        if (!user.manager()) {
            // C-10: VENDEDOR so lista as proprias vendas; pedir as de outro vendedor e proibido.
            if (sellerId != null && sellerId != user.id()) {
                throw new ApiException(HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN, "Acesso negado");
            }
            seller = user.id();
        }
        return new PageResponse<>(sales.list(seller, status, page, size), page, size, sales.count(seller, status));
    }

    // ---------------- pagamento e cancelamento ----------------

    /** Regra 4: bloqueia o pedido; PAID repete sem efeito; PENDING_PAYMENT baixa estoque e grava o custo do momento. */
    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional
    public SalesOrderResponse confirmPayment(CurrentUser user, long orderId, String authorizationCode) {
        session.apply(user.id(), null);
        var head = sales.head(orderId, true).orElseThrow(() -> ApiException.notFound("Order not found, id: " + orderId));
        if (!user.owns(head.sellerId())) throw ApiException.notFound("Order not found, id: " + orderId);
        if ("PAID".equals(head.status())) return sales.order(orderId).orElseThrow();
        if (!"PENDING_PAYMENT".equals(head.status())) {
            throw ApiException.conflict(ErrorCode.STATE_CONFLICT, "Order " + orderId + " is " + head.status());
        }
        for (var item : orderItems(orderId)) {
            BigDecimal cost = jdbc.sql("""
                    UPDATE stock_balance SET reserved = reserved - :q, on_hand = on_hand - :q, updated_at = now()
                     WHERE product_id = :product RETURNING avg_cost""")
                    .param("q", item.quantity()).param("product", item.productId()).query(BigDecimal.class).optional().orElse(null);
            jdbc.sql("UPDATE order_item SET unit_cost = :cost WHERE id = :id").param("cost", cost).param("id", item.id()).update();
            jdbc.sql("""
                    INSERT INTO stock_movement(product_id, order_id, kind, quantity, created_by)
                    VALUES (:product, :order, 'SAIDA', :q, :user)""")
                    .param("product", item.productId()).param("order", orderId).param("q", item.quantity())
                    .param("user", user.id()).update();
        }
        jdbc.sql("""
                UPDATE payment SET status = 'APPROVED', payment_date = :now,
                       authorization_code = coalesce(:auth, authorization_code) WHERE order_id = :order""")
                .param("now", StoreTime.now()).param("auth", authorizationCode).param("order", orderId).update();
        jdbc.sql("UPDATE orders SET status = 'PAID' WHERE id = :id").param("id", orderId).update();
        return sales.order(orderId).orElseThrow();
    }

    /** Regra 5: so PENDING_PAYMENT; libera a reserva; repetir num pedido cancelado nao tem efeito (C-06). */
    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional
    public SalesOrderResponse cancel(CurrentUser user, long orderId) {
        session.apply(user.id(), null);
        var head = sales.head(orderId, true).orElseThrow(() -> ApiException.notFound("Order not found, id: " + orderId));
        if (!user.owns(head.sellerId())) throw ApiException.notFound("Order not found, id: " + orderId);
        if ("CANCELED".equals(head.status())) return sales.order(orderId).orElseThrow();
        if (!"PENDING_PAYMENT".equals(head.status())) {
            throw ApiException.conflict(ErrorCode.STATE_CONFLICT, "Only pending orders can be canceled; order is " + head.status());
        }
        for (var item : orderItems(orderId)) {
            jdbc.sql("UPDATE stock_balance SET reserved = reserved - :q, updated_at = now() WHERE product_id = :product")
                    .param("q", item.quantity()).param("product", item.productId()).update();
            jdbc.sql("""
                    INSERT INTO stock_movement(product_id, order_id, kind, quantity, created_by)
                    VALUES (:product, :order, 'LIBERACAO', :q, :user)""")
                    .param("product", item.productId()).param("order", orderId).param("q", item.quantity())
                    .param("user", user.id()).update();
        }
        jdbc.sql("UPDATE payment SET status = 'DECLINED' WHERE order_id = :order").param("order", orderId).update();
        jdbc.sql("UPDATE orders SET status = 'CANCELED' WHERE id = :id").param("id", orderId).update();
        audit.record(AuditAction.ORDER_CANCELED, user.id(), "orders", orderId, null);
        return sales.order(orderId).orElseThrow();
    }

    private record ItemRow(long id, long productId, int quantity) { }

    /** Itens em ordem crescente de product_id: ordem de bloqueio unica entre transacoes (regra 3). */
    private List<ItemRow> orderItems(long orderId) {
        return jdbc.sql("SELECT id, product_id, quantity FROM order_item WHERE order_id = :id ORDER BY product_id, id")
                .param("id", orderId)
                .query((rs, n) -> new ItemRow(rs.getLong("id"), rs.getLong("product_id"), rs.getInt("quantity"))).list();
    }

    static LocalDateTime now() {
        return StoreTime.now();
    }
}
