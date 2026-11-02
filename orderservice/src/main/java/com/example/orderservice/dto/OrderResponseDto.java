package com.example.orderservice.dto;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderStatus;

import java.time.LocalDateTime;

public record OrderResponseDto(
        Long orderId,
        Long memberId,
        Long productId,
        int quantity,
        OrderStatus orderStatus,
        LocalDateTime createdAt
) {
    public static OrderResponseDto from(Order order) {
        return new OrderResponseDto(
                order.getOrderId(),
                order.getMemberId(),
                order.getProductId(),
                order.getQuantity(),
                order.getOrderStatus(),
                order.getCreatedAt()
        );
    }
}

/*
public class OrderResponseDto {

    private final Long orderId;
    private final Long memberId;
    private final Long productId;
    private final int quantity;
    private final OrderStatus orderStatus;
    private final LocalDateTime createdAt;

    public OrderResponseDto(
            Long orderId,
            Long memberId,
            Long productId,
            int quantity,
            OrderStatus orderStatus,
            LocalDateTime createdAt
    ) {
        this.orderId = orderId;
        this.memberId = memberId;
        this.productId = productId;
        this.quantity = quantity;
        this.orderStatus = orderStatus;
        this.createdAt = createdAt;
    }

    public static OrderResponseDto from(Order order) {
        return new OrderResponseDto(
                order.getOrderId(),
                order.getMemberId(),
                order.getProductId(),
                order.getQuantity(),
                order.getOrderStatus(),
                order.getCreatedAt()
        );
    }

    public Long getOrderId() {
        return orderId;
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
}*/