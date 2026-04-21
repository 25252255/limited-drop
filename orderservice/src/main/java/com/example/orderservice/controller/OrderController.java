package com.example.orderservice.controller;

import com.example.orderservice.dto.RegisterMemberResponseDto;
import com.example.orderservice.service.MemberQueueService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequiredArgsConstructor //생성자 주입, 불변성 vs @Autowired
@RequestMapping("/orders")
public class OrderController {
    private final MemberQueueService memberQueueService;

    //진입 요청
    @PostMapping("/queue")
    public Mono<RegisterMemberResponseDto> registerMember(@RequestParam String memberId){
        return memberQueueService.registerMemberQueue(memberId)
                .map(rank -> new RegisterMemberResponseDto(memberId, rank, "대기열에 등록되었습니다."));
    }

    /*
    //초기 설정 테스트
    @GetMapping("order")
    public Mono<String> getHello() {
        return Mono.just("hello");
    }*/
}
