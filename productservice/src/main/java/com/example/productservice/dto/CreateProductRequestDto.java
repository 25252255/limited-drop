package com.example.productservice.dto;

import com.example.productservice.domain.ProductStatus;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;

import java.time.LocalDateTime;

public class CreateProductRequestDto {
    private String name;
    private Long price;
    private int initialStock;
    @Enumerated(EnumType.STRING)
    private ProductStatus productStatus;
    private LocalDateTime saleStartAt;

    private Long memberId;

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
