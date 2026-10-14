package com.swee.ordermanagementspring.entities.payment;

import com.swee.ordermanagementspring.entities.StoreTime;

import com.swee.ordermanagementspring.entities.Order;
import com.swee.ordermanagementspring.entities.enums.OrderStatus;
import com.swee.ordermanagementspring.entities.enums.PaymentStatus;
import com.swee.ordermanagementspring.exceptions.PaymentException;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@DiscriminatorValue("CARD")
public class CardPayment extends Payment {

    // D022 (DEC-ERP-04, B-05): so os 4 ultimos digitos sao persistidos; card_number fica sempre nulo (CHECK da V7).
    @Column(name = "card_last4", length = 4)
    @JdbcTypeCode(SqlTypes.CHAR)
    private String cardLast4;
    private String cardHolder;
    private Integer installments;

    public CardPayment(){

    }

    public CardPayment(BigDecimal amount, Order order, String cardNumber, String cardHolder, Integer installments) {
        super(amount, order);
        this.cardLast4 = lastFour(cardNumber);
        this.cardHolder = cardHolder;
        this.installments = installments;
    }

    public String getCardLast4() {
        return cardLast4;
    }

    /** Recebe o numero informado e guarda so o final; o numero completo nunca chega ao banco. */
    public void setCardNumber(String cardNumber) {
        this.cardLast4 = lastFour(cardNumber);
    }

    static String lastFour(String cardNumber) {
        String digits = cardNumber == null ? "" : cardNumber.replaceAll("[^0-9]", "");
        return digits.length() < 4 ? null : digits.substring(digits.length() - 4);
    }

    public String getCardHolder() {
        return cardHolder;
    }

    public void setCardHolder(String cardHolder) {
        this.cardHolder = cardHolder;
    }

    public Integer getInstallments() {
        return installments;
    }

    public void setInstallments(Integer installments) {
        this.installments = installments;
    }

    @Override
    public boolean processPayment() {
        if (cardLast4 == null){
            throw new PaymentException("Invalid number");
        }
        this.status = PaymentStatus.APPROVED;
        this.paymentDate = StoreTime.now();
        return true;
    }
}
