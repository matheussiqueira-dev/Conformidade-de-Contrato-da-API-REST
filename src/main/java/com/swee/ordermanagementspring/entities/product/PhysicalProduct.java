package com.swee.ordermanagementspring.entities.product;

import com.swee.ordermanagementspring.entities.Money;
import com.swee.ordermanagementspring.exceptions.ProductException;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;

import java.math.BigDecimal;

@Entity
@DiscriminatorValue("PHYSICAL")
public class PhysicalProduct extends Product{
    private Double weight;

    public PhysicalProduct() {
    }

    public PhysicalProduct(BigDecimal price, String name, String description, Double weight) {
        super(price, name, description);
        this.weight = weight;
    }

    public Double getWeight() {
        return weight;
    }

    public void setWeight(Double weight) {
        this.weight = weight;
    }

    @Override
    public BigDecimal calculateShippingValue() {
        BigDecimal shippingValue = new BigDecimal("20.00");
        if (weight < 0.0) {
            throw new ProductException("Invalid weight.");
        }
        if (weight > 2.0) {
            // peso continua Double (nao e dinheiro); o valor do frete e normalizado em centavos
            BigDecimal extraKg = BigDecimal.valueOf(weight).subtract(new BigDecimal("2"));
            return Money.of(shippingValue.add(extraKg.multiply(new BigDecimal("8.00"))));
        }
        return shippingValue;
    }
}
