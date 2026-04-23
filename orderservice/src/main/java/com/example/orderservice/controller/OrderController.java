package com.example.orderservice.controller;

import com.example.orderservice.dto.RegisterMemberResponseDto;
import com.example.orderservice.service.MemberQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
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

    //현재 대기 순번 조회
    @GetMapping(value = "/pass", produces = MediaType.TEXT_EVENT_STREAM_VALUE)  //연결 유지
    public Flux<ServerSentEvent<Long>> passMember(@RequestParam String memberId){
        return Flux
                //1초마다 신호 보내기(tick)
                .interval(Duration.ofSeconds(1))
                //tick 올 때마다 getRank실행
                .flatMap(tick -> memberQueueService.getRank(memberId)
                        .switchIfEmpty(Mono.defer(() -> {return Mono.just(-1L);}))
                )
                //가져온 순서가 isAllowed인지
                .flatMap(rank -> {
                            if (rank < 0L) { //대기열에 없으면 허용 상태로 넘어갔는지 확인
                                return memberQueueService.isAllowed(memberId).map(allowed -> allowed ? 0L : -1L);
                            }
                            return Mono.just(rank);
                        })
                //조회된 값을 SSE 형식으로 build
                .map(rank -> ServerSentEvent.<Long>builder()
                        .data(rank)
                        .event("pass")
                        .build())
                                            //event:pass
                                            //data:1    형태로 출력
                //스트림 종료
                .takeUntil(sse -> sse.data() != null && sse.data() <= 0L)   //대기열 이탈인지 순번 기다렸는지 검증 필요
                //.doOnTerminate(() -> log.debug("--- SSE 연결 종료 ---"))
                .doOnError(e -> log.error("passMember Error: {},{}", memberId, e.getMessage()));
    }

    /*
    //초기 설정 테스트
    @GetMapping("order")
    public Mono<String> getHello() {
        return Mono.just("hello");
    }*/
}
