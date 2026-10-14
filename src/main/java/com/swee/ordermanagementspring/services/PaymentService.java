package com.swee.ordermanagementspring.services;

import com.swee.ordermanagementspring.dto.PaymentRequestDTO;
import com.swee.ordermanagementspring.entities.Order;
import com.swee.ordermanagementspring.entities.enums.OrderStatus;
import com.swee.ordermanagementspring.entities.enums.PaymentStatus;
import com.swee.ordermanagementspring.entities.payment.BoletoPayment;
import com.swee.ordermanagementspring.entities.payment.CardPayment;
import com.swee.ordermanagementspring.entities.payment.Payment;
import com.swee.ordermanagementspring.entities.payment.PixPayment;
import com.swee.ordermanagementspring.exceptions.PaymentException;
import com.swee.ordermanagementspring.exceptions.ResourceNotFoundException;
import com.swee.ordermanagementspring.repositories.OrderRepository;
import com.swee.ordermanagementspring.repositories.PaymentRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class PaymentService {

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private OrderRepository orderRepository;


    @Transactional
    public Payment processAndSave(Payment payment) {
        // D004: pagamento ja aprovado, recusado ou expirado nao e processado de novo
        if (payment.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException("Only payments with PENDING status can be processed");
        }

        boolean approved = payment.processPayment();

        Payment saved = paymentRepository.save(payment);

        // D003: so pagamento aprovado confirma o pedido; boleto vencido mantem PENDING_PAYMENT
        if (approved) {
            saved.getOrder().setStatus(OrderStatus.PAID);
        }

        return saved;
    }

    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    public Payment findById(Long id) {
        return paymentRepository.findById(id).orElseThrow(() -> new ResourceNotFoundException("Payment not found, id: " + id));
    }

    @Transactional
    public Payment insert(PaymentRequestDTO dto) {
        Order order = orderRepository.findById(dto.getOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("Order not found, id: " + dto.getOrderId()));

        PaymentRules.requireAmountMatchesTotal(dto.getAmount(), order);
        Payment payment = buildPayment(dto, order);

        return paymentRepository.save(payment);
    }
    @Transactional
    public Payment update(Long id, PaymentRequestDTO dto) {
        Payment existing = findById(id);

        if (existing.getStatus() != PaymentStatus.PENDING) {
            throw new PaymentException("Only payments with PENDING status can be edited");
        }

        PaymentRules.requireAmountMatchesTotal(dto.getAmount(), existing.getOrder());
        String type = PaymentRules.normalizeType(dto.getType());

        boolean typeChanged =
                ("CARD".equals(type) && !(existing instanceof CardPayment))
                        || ("PIX".equals(type) && !(existing instanceof PixPayment))
                        || ("BOLETO".equals(type) && !(existing instanceof BoletoPayment));

        if (typeChanged) {
            Order order = existing.getOrder();

            //remove a referencia do payment antigo do banco
            order.setPayment(null);

            //remove o payment antigo
            paymentRepository.delete(existing);
            paymentRepository.flush();

            //cria um novo tipo de payment
            Payment updated = buildPayment(dto, order);

            //mantem os dois lados do relacionamento sincronizados
            order.setPayment(updated);
            updated.setOrder(order);

            return paymentRepository.save(updated);
        }

        switch (type) {

            case "CARD" -> {
                if (dto.getCardNumber() == null || dto.getCardNumber().isBlank()) {
                    throw new PaymentException("cardNumber is required for CARD payments");
                }

                CardPayment payment = (CardPayment) existing;

                payment.setAmount(dto.getAmount());
                payment.setCardNumber(dto.getCardNumber());
                payment.setCardHolder(dto.getCardHolder());
                payment.setInstallments(dto.getInstallments());
            }

            case "PIX" -> {
                if (dto.getPixKey() == null || dto.getPixKey().isBlank()) {
                    throw new PaymentException("pixKey is required for PIX payments");
                }

                PixPayment payment = (PixPayment) existing;

                payment.setAmount(dto.getAmount());
                payment.setPixKey(dto.getPixKey());
                payment.setPixHolderName(dto.getPixHolderName());
                payment.setPixKeyType(null);
            }

            case "BOLETO" -> {
                if (dto.getBarCode() == null || dto.getBarCode().isBlank()) {
                    throw new PaymentException("barCode is required for BOLETO payments");
                }

                PaymentRules.requireDueDate(dto.getDueDate());

                BoletoPayment payment = (BoletoPayment) existing;

                payment.setAmount(dto.getAmount());
                payment.setBarCode(dto.getBarCode());
                payment.setDueDate(dto.getDueDate());
            }

            default -> throw new PaymentException(
                    "Invalid payment type: " + dto.getType()
            );
        }

        return paymentRepository.save(existing);
    }

    private Payment buildPayment(PaymentRequestDTO dto, Order order) {
        return switch (PaymentRules.normalizeType(dto.getType())) {
            case "CARD" -> {
                if (dto.getCardNumber() == null || dto.getCardNumber().isBlank()) {
                    throw new PaymentException("cardNumber is required for CARD payments");
                }
                yield new CardPayment(dto.getAmount(), order, dto.getCardNumber(), dto.getCardHolder(), dto.getInstallments());
            }
            case "PIX" -> {
                if (dto.getPixKey() == null || dto.getPixKey().isBlank()) {
                    throw new PaymentException("pixKey is required for PIX payments");
                }
                yield new PixPayment(dto.getAmount(), order, dto.getPixKey(), dto.getPixHolderName(), null, null);
            }
            case "BOLETO" -> {
                if (dto.getBarCode() == null || dto.getBarCode().isBlank()) {
                    throw new PaymentException("barCode is required for BOLETO payments");
                }
                PaymentRules.requireDueDate(dto.getDueDate());
                yield new BoletoPayment(dto.getAmount(), order, dto.getBarCode(), dto.getDueDate());
            }
            default -> throw new PaymentException("Invalid payment type: " + dto.getType());
        };
    }

    public void delete(Long id) {
        // D011: excluir id inexistente responde 404, como a consulta
        if (!paymentRepository.existsById(id)) {
            throw new ResourceNotFoundException("Payment not found, id: " + id);
        }
        paymentRepository.deleteById(id);
    }
}
