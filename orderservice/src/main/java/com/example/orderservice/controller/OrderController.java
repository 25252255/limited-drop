package com.example.orderservice.controller;

import com.example.orderservice.dto.RegisterMemberResponseDto;
import com.example.orderservice.service.OrderService;
import com.example.orderservice.service.MemberQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import com.example.orderservice.dto.OrderResponseDto;

import java.time.Duration;

@Slf4j
@RestController
@RequiredArgsConstructor //생성자 주입, 불변성 vs @Autowired
@RequestMapping("/orders")
public class OrderController {
    private final MemberQueueService memberQueueService;
    private final OrderService orderService;

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
                .takeUntil(sse -> sse.data() != null && sse.data() <= 0L)   //대기열 이탈인지 순번 기다렸는지 권한 검증 필요
                //.doOnTerminate(() -> log.debug("--- SSE 연결 종료 ---"))
                .doOnError(e -> log.error("passMember Error: {},{}", memberId, e.getMessage()));
    }

    //상품 주문
    /*//HACK : 삼품 정보 캐싱 필요
    @PostMapping("/order")
    public Mono<String> order(@RequestParam String memberId, @RequestParam Long productId, @RequestParam int quantity){
        return
                //권한 검증
                memberQueueService.isAllowed(memberId)
                .flatMap(allowed -> {
                    if (!allowed) return Mono.error(new RuntimeException("권한이 없습니다."));

                    //주문 호출
                    return orderService.createOrder(memberId, productId, quantity);
                })
                //권한 삭제
                .flatMap(result -> memberQueueService.deleteAllowKey(memberId))
                .map(isSuccess -> {
                    log.info("주문 성공>> 회원 {}가 상품 {}을(를) {}개 주문했습니다.", memberId, productId, quantity);
                    return "주문 성공"+isSuccess;
                })
                .doOnError(e -> log.error("order Error: {},{}", memberId, e.getMessage()));
    }*/

    //요청 파라미터가 아니라 HTTP 헤더에서 멱등성 키를 받음.Postman에서는 Headers에 ex)Idempotency-Key: order-member2-product1-001
    @PostMapping("/order")
    public Mono<OrderResponseDto> order(@RequestHeader("Idempotency-Key") String idempotencyKey, @RequestParam Long memberId, @RequestParam Long productId, @RequestParam int quantity) {
        String redisMemberId = String.valueOf(memberId);

        return orderService.getOrderByIdempotencyKey(idempotencyKey)    ///OrderResponseDto / Mono.empty()
                .switchIfEmpty(Mono.defer(() ->
                        memberQueueService.isAllowed(redisMemberId)
                                .flatMap(allowed -> {
                                    if (!allowed) {
                                        return Mono.error(new RuntimeException("권한이 없습니다."));
                                    }

                                    return orderService.createOrder(
                                                    idempotencyKey,
                                                    memberId,
                                                    productId,
                                                    quantity
                                            )
                                            .flatMap(orderResponse ->
                                                    memberQueueService.deleteAllowKey(redisMemberId)
                                                            .thenReturn(orderResponse)  //입장 권한 삭제 완료 결과 대신에 orderResponse DTO반환
                                            );
                                })
                ))
                //기존 주문 있음
                //대기열 입장 권한 다시 검사x
                //TODO : 본인 확인(기존 주문이 이 사용자의 주문인지 JWT나 세션에서 꺼낸 회원 ID와 비교)
                .doOnSuccess(orderResponse ->
                        log.info(
                                "주문 처리 결과>> 주문: {}, 회원: {}, 상품: {}, 상태: {}",
                                orderResponse.orderId(),
                                orderResponse.memberId(),
                                orderResponse.productId(),
                                orderResponse.orderStatus()
                        )
                )
                .doOnError(error ->
                        log.error("order Error: {}, {}", memberId, error.getMessage())
                );
    }

    /*
    //초기 설정 테스트
    @GetMapping("order")
    public Mono<String> getHello() {
        return Mono.just("hello");
    }*/
}
