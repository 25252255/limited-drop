package com.example.productservice.domain;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "products")
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long productId;
    private String name;
    private Long price;
    private int initialStock;
    @Enumerated(EnumType.STRING)
    private ProductStatus productStatus;
    private LocalDateTime saleStartAt;

    private Long memberId;

    public Product() {
    }

    public Product(String name, Long price, int initialStock, ProductStatus productStatus, LocalDateTime saleStartAt, Long memberId) {
        this.name = name;
        this.price = price;
        this.initialStock = initialStock;
        this.productStatus = productStatus;
        this.saleStartAt = saleStartAt;
        this.memberId = memberId;
    }

    public Long getProductId() {
        return productId;
    }

    public String getName() {
        return name;
    }

    public Long getPrice() {
        return price;
    }

    public int getInitialStock() {
        return initialStock;
    }

    public ProductStatus getProductStatus() {
        return productStatus;
    }

    public LocalDateTime getSaleStartAt() {
        return saleStartAt;
    }

    public Long getMemberId() {
        return memberId;
    }
}
