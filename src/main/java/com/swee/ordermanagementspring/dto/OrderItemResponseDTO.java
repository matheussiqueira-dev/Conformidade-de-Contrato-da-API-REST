package com.swee.ordermanagementspring.dto;

import com.swee.ordermanagementspring.entities.OrderItem;

import java.math.BigDecimal;

public class OrderItemResponseDTO {

    private Long id;
    private ProductResponseDTO product;
    private Integer quantity;
    private BigDecimal price;

    public OrderItemResponseDTO(Long id, ProductResponseDTO product, Integer quantity, BigDecimal price) {
        this.id = id;
        this.product = product;
        this.quantity = quantity;
        this.price = price;
    }

    public static OrderItemResponseDTO from(OrderItem item) {
        return new OrderItemResponseDTO(
                item.getId(),
                ProductResponseDTO.from(item.getProduct()),
                item.getQuantity(),
                item.getPrice()
        );
    }

    public Long getId() {
        return id;
    }

    public ProductResponseDTO getProduct() {
        return product;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public BigDecimal getPrice() {
        return price;
    }
}