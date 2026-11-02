package com.example.orderservice.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "orders",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_orders_idempotency_key", columnNames = "idempotency_key")
        }
)
public class Order {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long orderId;

    @Column(name = "idempotency_key", nullable = false)
    private String idempotencyKey;

    @Column(nullable = false)
    private Long memberId;

    @Column(nullable = false)
    private Long productId;

    @Column(nullable = false)
    private int quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private OrderStatus orderStatus;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    protected Order() {
    }

    public Order(
            String idempotencyKey,
            Long memberId,
            Long productId,
            int quantity)
    {
        if (quantity <= 0) throw new IllegalArgumentException("주문 수량은 1개 이상이어야 합니다.");

        this.idempotencyKey = idempotencyKey;
        this.memberId = memberId;
        this.productId = productId;
        this.quantity = quantity;
        this.orderStatus = OrderStatus.PENDING;     //주문이 생성될 때 갖는 초기 상태
        this.createdAt = LocalDateTime.now();
    }

    public void complete() {
        this.orderStatus = OrderStatus.COMPLETED;   //주문 객체가 자기 상태를 변경하게
    }

    public void fail() {
        this.orderStatus = OrderStatus.FAILED;      //주문 객체가 자기 상태를 변경하게
    }

    public Long getOrderId() {
        return orderId;
    }

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public Long getMemberId() {
        return memberId;
    }

    public Long getProductId() {
        return productId;
    }

    public int getQuantity() {
        return quantity;
    }

    public OrderStatus getOrderStatus() {
        return orderStatus;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}