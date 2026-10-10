package com.swee.ordermanagementspring.entities;

import com.swee.ordermanagementspring.entities.customer.Customer;
import com.swee.ordermanagementspring.entities.auth.AppUser;
import com.swee.ordermanagementspring.entities.enums.OrderStatus;
import com.swee.ordermanagementspring.entities.payment.Payment;
import jakarta.persistence.*;
import org.hibernate.engine.internal.Cascade;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "orders", indexes = @Index(name = "idx_orders_seller_id", columnList = "seller_id"))
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime moment = LocalDateTime.now();
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    @ManyToOne(cascade = CascadeType.PERSIST)
    private Customer customer;
    // Null only for orders created before authenticated authorship was introduced.
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "seller_id")
    private AppUser seller;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true) //implementa orphan removal
    private List<OrderItem> items = new ArrayList<>();
    @OneToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "address_id")
    private Address shippingAddress;
    @OneToOne(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private Payment payment;

    public Order(){

    }

    public Order(OrderStatus status, Customer customer, AppUser seller) {
        this.customer = customer;
        this.status = status;
        this.seller = java.util.Objects.requireNonNull(seller, "seller is required for new orders");
    }

    public List<OrderItem> getItems() {
        return items;
    }

    public void setItems(List<OrderItem> items) {
        this.items.clear();
        this.items.addAll(items);
    }

    public Long getId() {
        return id;
    }

    public LocalDateTime getMoment() {
        return moment;
    }

    public OrderStatus getStatus() {
        return status;
    }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public Customer getCustomer() {
        return customer;
    }

    public AppUser getSeller() {
        return seller;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }
    //nao vou precisar usar alguns desses metodos
    public void addItem(OrderItem item) {
        items.add(item);
    }

    public void removeItem(OrderItem item) {
        items.remove(item);
    }

    public Payment getPayment() {
        return payment;
    }

    public void setPayment(Payment payment) {
        this.payment = payment;
    }

    public Address getShippingAddress() {
        return shippingAddress;
    }

    public void setShippingAddress(Address shippingAddress) {
        this.shippingAddress = shippingAddress;
    }

    public Double total() {
        double sum = 0.0;
        for (OrderItem item : items) {
            sum += item.subTotal();
        }
        return sum;
    }

}
