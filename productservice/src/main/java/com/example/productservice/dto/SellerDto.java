package com.example.productservice.dto;

public class SellerDto {
    private Long memberId;
    private String name;

    public SellerDto(Long memberId, String name) {
        this.memberId = memberId;
        this.name = name;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getName() {
        return name;
    }
}
