package com.example.productservice.domain;

public enum ProductStatus {
    READY(0, "READY"),
    ON_SALE(1, "ON_SALE"),
    SOLD_OUT(2, "SOLD_OUT"),
    STOPPED(3, "STOPPED");

    private final int code;           // 숫자 코드 (0, 1, 2...)
    private final String description;  // 영문 설명 (READY, ON_SALE...)

    ProductStatus(int code, String description) {
        this.code = code;
        this.description = description;
    }

    public int getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }
}
