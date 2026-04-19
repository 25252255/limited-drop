package com.example.productservice.dto;

import java.time.LocalDateTime;

public class ProductResponseDto {
    private String name;
    private Long price;
    private LocalDateTime saleStartAt;

    private SellerDto member;

    public ProductResponseDto(String name, Long price, LocalDateTime saleStartAt, SellerDto member) {
        this.name = name;
        this.price = price;
        this.saleStartAt = saleStartAt;
        this.member = member;
    }

    public String getName() {
        return name;
    }

    public Long getPrice() {
        return price;
    }

    public LocalDateTime getSaleStartAt() {
        return saleStartAt;
    }

    public SellerDto getMember() {
        return member;
    }
}
