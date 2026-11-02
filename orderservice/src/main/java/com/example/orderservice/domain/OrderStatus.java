package com.example.orderservice.domain;

public enum OrderStatus {
    PENDING,    //주문 요청을 접수했지만 재고 차감이 끝나지 않음(주문이 생성될 때 갖는 초기 상태)
    COMPLETED,  //Lua Script 재고 차감과 주문 처리가 성공함
    FAILED      //재고 부족이나 처리 오류로 주문이 실패함
}
