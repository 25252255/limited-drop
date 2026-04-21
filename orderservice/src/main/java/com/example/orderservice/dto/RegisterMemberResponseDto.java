package com.example.orderservice.dto;

public record RegisterMemberResponseDto(

        //세션(Session) / JWT(Token)
        //세션:
        // 장점- 보안 통제 가능
        // 단점- 서버에 정보 저장, 트래픽 증가 시 부담
        //토큰:
        // 장점- 유효성 판단을 위한 서버에 정보 저장,조회할 필요 x
        // 단점- 토큰 탈취 시 만료되기 전까지 무효화가 어려움

        String memberId, //Redis에는 String으로 저장해야 함
        Long rank,
        String message
){
}
