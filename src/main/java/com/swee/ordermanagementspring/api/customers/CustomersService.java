package com.swee.ordermanagementspring.api.customers;

import com.swee.ordermanagementspring.api.common.Addresses;
import com.swee.ordermanagementspring.api.common.CurrentUser;
import com.swee.ordermanagementspring.api.common.DbSessionContext;
import com.swee.ordermanagementspring.api.common.MoneyDto;
import com.swee.ordermanagementspring.api.common.PageResponse;
import com.swee.ordermanagementspring.api.customers.CustomerDtos.*;
import com.swee.ordermanagementspring.api.customers.CustomersRepository.HistoryRow;
import com.swee.ordermanagementspring.entities.audit.AuditAction;
import com.swee.ordermanagementspring.entities.customer.Documents;
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
import java.math.RoundingMode;
import java.util.List;
import java.util.Map;

/**
 * Clientes (RF-20, RF-21). O documento e gravado so com digitos e nunca muda; a edicao usa versao otimista;
 * o VENDEDOR ve o documento mascarado e, no historico, total e vendedor so dos pedidos dele (DEC-ERP-02).
 */
@Service
public class CustomersService {
    private final CustomersRepository customers;
    private final JdbcClient jdbc;
    private final DbSessionContext session;
    private final AuditService audit;

    public CustomersService(CustomersRepository customers, JdbcClient jdbc, DbSessionContext session, AuditService audit) {
        this.customers = customers;
        this.jdbc = jdbc;
        this.session = session;
        this.audit = audit;
    }

    private static ApiException notFound(long id) {
        return ApiException.notFound("Customer not found, id: " + id);
    }

    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional(readOnly = true)
    public PageResponse<CustomerSummary> search(CurrentUser user, String q, int page, int size) {
        var search = CustomersRepository.Search.of(q);
        return new PageResponse<>(customers.search(search, user.manager(), page, size), page, size, customers.count(search));
    }

    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional(readOnly = true)
    public CustomerDetail get(CurrentUser user, long id) {
        return customers.detail(id, user.manager()).orElseThrow(() -> notFound(id));
    }

    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional
    public CustomerDetail create(CurrentUser user, CustomerCreateRequest request) {
        session.apply(user.id(), null);
        Long addressId = request.address() == null ? null : Addresses.insert(jdbc, request.address());
        String cpf = request instanceof IndividualCustomerCreate i ? Documents.digitsOnly(i.cpf()) : null;
        String cnpj = request instanceof CorporateCustomerCreate c ? Documents.cnpjKey(c.cnpj()) : null;
        // D-EVO-40: documento ja cadastrado devolve o id em details para a tela oferecer o cliente existente;
        // a constraint UNIQUE continua cobrindo a corrida entre duas inclusoes.
        jdbc.sql("SELECT id FROM customer WHERE cpf = :cpf OR cnpj = :cnpj").param("cpf", cpf).param("cnpj", cnpj)
                .query(Long.class).optional().ifPresent(existing -> {
                    throw new ApiException(HttpStatus.CONFLICT, ErrorCode.DUPLICATE_DOCUMENT, "Document already registered",
                            List.of("customerId=" + existing));
                });
        long id = jdbc.sql("""
                INSERT INTO customer(customer_type, name, company_name, cpf, cnpj, email, phone, birth_date, address_id, created_by)
                VALUES (:type, :name, :company, :cpf, :cnpj, :email, :phone, :birth, :address, :user) RETURNING id""")
                .param("type", request instanceof IndividualCustomerCreate ? "INDIVIDUAL" : "CORPORATE")
                .param("name", request.name().trim())
                .param("company", request instanceof CorporateCustomerCreate c ? c.companyName().trim() : null)
                .param("cpf", cpf).param("cnpj", cnpj).param("email", request.email().trim())
                .param("phone", request.phone().trim())
                .param("birth", request instanceof IndividualCustomerCreate i ? i.birthDate() : null)
                .param("address", addressId).param("user", user.id())
                .query(Long.class).single();
        audit.record(AuditAction.CUSTOMER_CREATED, user.id(), "customer", id, null);
        return customers.detail(id, user.manager()).orElseThrow();
    }

    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional
    public CustomerDetail update(CurrentUser user, long id, CustomerUpdateRequest request) {
        session.apply(user.id(), null);
        var head = customers.head(id).orElseThrow(() -> notFound(id));
        if (head.version() != request.version()) {
            throw ApiException.conflict(ErrorCode.CONCURRENT_UPDATE, "Customer was changed by someone else; reload and try again");
        }
        if ("INDIVIDUAL".equals(head.type()) && request.companyName() != null) {
            throw ApiException.badRequest("companyName: only corporate customers have a company name");
        }
        jdbc.sql("""
                UPDATE customer SET name = :name, email = :email, phone = :phone,
                       company_name = coalesce(:company, company_name), birth_date = coalesce(:birth, birth_date),
                       version = version + 1, updated_at = now()
                 WHERE id = :id AND version = :version""")
                .param("name", request.name().trim()).param("email", request.email().trim())
                .param("phone", request.phone().trim())
                .param("company", request.companyName() == null ? null : request.companyName().trim())
                .param("birth", request.birthDate()).param("id", id).param("version", request.version()).update();
        if (request.address() != null) {
            if (head.addressId() != null) {
                Addresses.update(jdbc, head.addressId(), request.address());
            } else {
                long addressId = Addresses.insert(jdbc, request.address());
                jdbc.sql("UPDATE customer SET address_id = :address WHERE id = :id").param("address", addressId).param("id", id).update();
            }
        }
        audit.record(AuditAction.CUSTOMER_UPDATED, user.id(), "customer", id, null);
        return customers.detail(id, user.manager()).orElseThrow();
    }

    @PreAuthorize("hasRole('GERENTE')")
    @Transactional
    public void delete(CurrentUser user, long id) {
        session.apply(user.id(), null);
        var head = customers.head(id).orElseThrow(() -> notFound(id));
        if (customers.historyCount(id) > 0) {
            throw ApiException.conflict(ErrorCode.RESOURCE_IN_USE, "Customer has orders and cannot be deleted");
        }
        jdbc.sql("DELETE FROM customer WHERE id = :id").param("id", id).update();
        if (head.addressId() != null) {
            jdbc.sql("DELETE FROM address WHERE id = :id").param("id", head.addressId()).update();
        }
        audit.record(AuditAction.CUSTOMER_DELETED, user.id(), "customer", id, null);
    }

    /**
     * G-05: indicadores do perfil. O VENDEDOR ve valores so dos pedidos dele (scope OWN) e nao ve estornos da
     * loja; contagens, datas e produtos mais comprados valem para a loja toda (nao sao dado sensivel).
     */
    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional(readOnly = true)
    public CustomerPurchaseSummary summary(CurrentUser user, long id) {
        if (!customers.exists(id)) throw notFound(id);
        var store = customers.summaryTotals(id, null);
        var scoped = user.manager() ? store : customers.summaryTotals(id, user.id());
        long[] counts = customers.statusCounts(id);
        MoneyDto average = scoped.ordersPaid() == 0 ? null
                : MoneyDto.of(scoped.net().divide(BigDecimal.valueOf(scoped.ordersPaid()), 2, RoundingMode.HALF_UP));
        return new CustomerPurchaseSummary(id, user.manager() ? "STORE" : "OWN",
                CustomersRepository.local(store.first()), CustomersRepository.local(store.last()), store.ordersPaid(),
                counts[0], counts[1], MoneyDto.of(scoped.net()), average,
                user.manager() ? MoneyDto.of(store.refunds()) : null,
                customers.topProducts(id), customers.paymentMix(id, user.manager() ? null : user.id()));
    }

    @PreAuthorize("hasRole('VENDEDOR')")
    @Transactional(readOnly = true)
    public PageResponse<CustomerOrderSummary> history(CurrentUser user, long id, int page, int size) {
        if (!customers.exists(id)) throw notFound(id);
        List<HistoryRow> rows = customers.history(id, page, size);
        Map<Long, List<CustomerOrderItem>> items = customers.items(rows.stream().map(HistoryRow::id).toList());
        List<CustomerOrderSummary> content = rows.stream().map(row -> {
            boolean visible = user.owns(row.sellerId());
            return new CustomerOrderSummary(row.id(), CustomersRepository.local(row.moment()), row.status(),
                    Fulfillment.valueOf(row.fulfillment()), items.getOrDefault(row.id(), List.of()),
                    visible ? MoneyDto.of(row.total()) : null,
                    visible && row.sellerName() != null ? row.sellerName().trim().split("\\s+")[0] : null,
                    row.sellerId() != null && row.sellerId() == user.id());
        }).toList();
        return new PageResponse<>(content, page, size, customers.historyCount(id));
    }
}
