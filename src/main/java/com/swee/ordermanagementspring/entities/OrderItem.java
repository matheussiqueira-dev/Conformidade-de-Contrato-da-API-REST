package com.swee.ordermanagementspring.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.swee.ordermanagementspring.entities.product.Product;
import jakarta.persistence.*;

import java.math.BigDecimal;

@Entity
public class OrderItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Integer quantity;
    @Column(precision = Money.PRECISION, scale = Money.SCALE)
    private BigDecimal price;
    @ManyToOne
    @JsonIgnore
    private Order order; //composicao
    @ManyToOne(cascade = CascadeType.PERSIST)
    private Product product; //associacao

    public OrderItem () {

    }

    public OrderItem(Order order, Product product, Integer quantity, BigDecimal price) {
        this.order = order;
        this.product = product;
        this.quantity = quantity;
        this.price = Money.of(price);
    }

    public Order getOrder() {
        return order;
    }

    public void setOrder(Order order) {
        this.order = order;
    }

    public Long getId() {
        return id;
    }
    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = Money.of(price);
    }

    public Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        this.product = product;
    }

    /** Preco historico x quantidade, uma unica multiplicacao (QA-04). */
    public BigDecimal subTotal(){
        return Money.of(price.multiply(BigDecimal.valueOf(quantity)));
    }

}
