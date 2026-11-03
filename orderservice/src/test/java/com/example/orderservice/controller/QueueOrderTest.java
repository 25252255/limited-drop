package com.example.orderservice.controller;

import com.example.orderservice.domain.OrderRepository;
import com.example.orderservice.service.MemberQueueService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderStatus;
import com.example.orderservice.dto.OrderResponseDto;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.springframework.http.MediaType;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;
import java.time.Duration;
import java.util.UUID;

@SpringBootTest
@AutoConfigureWebTestClient //Controller에 HTTP 요청을 보낼 WebTestClient를 자동 설정
class QueueOrderTest {

    @Autowired
    private WebTestClient webTestClient; //Controller에 HTTP 요청

    @Autowired
    private MemberQueueService memberQueueService; //대기열 회원을 입장 허용 상태로 이동

    @Autowired
    private OrderRepository orderRepository; //DB 주문 조회 및 테스트 데이터 삭제

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate; //Redis 대기열·입장 권한·재고 확인

    private static final Long MEMBER_ID = 2L;
    private static final Long TEST_PRODUCT_ID = 999997L;
    private static final String STOCK_KEY = "product:stock:" + TEST_PRODUCT_ID;

    private String idempotencyKey;

    private static final String QUEUE_KEY = "order-queue";          //대기열 키
    private static final String ALLOW_KEY = "allow:" + MEMBER_ID;   //입장 권한 키

    @BeforeEach
    void setUp() {
        idempotencyKey = "queue-order-test-" + UUID.randomUUID();   //매번 새로운 멱등키 생성

        //이전 테스트에서 남아 있을 수 있는 Redis 데이터 삭제
        reactiveRedisTemplate.delete(QUEUE_KEY).block();
        reactiveRedisTemplate.delete(ALLOW_KEY).block();
        reactiveRedisTemplate.delete(STOCK_KEY).block();

        //테스트 상품의 재고를 10개로 설정
        reactiveRedisTemplate.opsForValue()
                .set(STOCK_KEY, "10")
                .block();
    }

    @AfterEach
    void cleanUp() {
        //테스트에서 생성된 주문이 있으면 DB에서 삭제
        orderRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(orderRepository::delete);

        //테스트에서 사용한 Redis 데이터 삭제
        reactiveRedisTemplate.delete(QUEUE_KEY).block();
        reactiveRedisTemplate.delete(ALLOW_KEY).block();
        reactiveRedisTemplate.delete(STOCK_KEY).block();
    }

    @Test
    void orderSucceedsWithQueuePermission() {
        String redisMemberId = String.valueOf(MEMBER_ID);

        //Controller를 통해 대기열 등록
        webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/orders/queue")
                        .queryParam("memberId", redisMemberId)
                        .build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.memberId").isEqualTo(redisMemberId)
                .jsonPath("$.rank").isEqualTo(1);

        //Scheduler가 호출하는 메서드를 직접 실행하여 입장 권한 생성
        memberQueueService.popMember(1L).block();

        Boolean allowedBeforeOrder = memberQueueService
                .isAllowed(redisMemberId)
                .block();

        assertTrue(Boolean.TRUE.equals(allowedBeforeOrder));

        //입장 권한에 5분 이내의 TTL이 설정됐는지 확인
        Duration allowTtl = reactiveRedisTemplate
                .getExpire(ALLOW_KEY)
                .block();

        assertNotNull(allowTtl);
        assertTrue(allowTtl.toSeconds() > 0);
        assertTrue(allowTtl.toSeconds() <= 300);

        //Controller를 통해 주문 요청
        OrderResponseDto response = webTestClient.post()
                .uri(uriBuilder -> uriBuilder
                        .path("/orders/order")
                        .queryParam("memberId", MEMBER_ID)
                        .queryParam("productId", TEST_PRODUCT_ID)
                        .queryParam("quantity", 1)
                        .build())
                .header("Idempotency-Key", idempotencyKey)
                .exchange()
                .expectStatus().isOk()
                .expectBody(OrderResponseDto.class)
                .returnResult()
                .getResponseBody();

        assertNotNull(response);
        assertEquals(OrderStatus.COMPLETED, response.orderStatus());

        //주문 처리 후 입장 권한이 삭제됐는지 확인
        Boolean allowedAfterOrder = memberQueueService
                .isAllowed(redisMemberId)
                .block();

        assertFalse(Boolean.TRUE.equals(allowedAfterOrder));

        //DB에 주문이 COMPLETED 상태로 저장됐는지 확인
        Order savedOrder = orderRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElseThrow();

        assertEquals(OrderStatus.COMPLETED, savedOrder.getOrderStatus());

        //Redis 재고가 10개에서 9개로 감소했는지 확인
        String remainingStock = reactiveRedisTemplate
                .opsForValue()
                .get(STOCK_KEY)
                .block();

        assertEquals("9", remainingStock);
    }
    @Test
    void sseShowsQueueRankAndAdmission() {
        String redisMemberId = String.valueOf(MEMBER_ID);

        //대기열 1순위로 등록
        memberQueueService.registerMemberQueue(redisMemberId).block();

        //Controller의 SSE 주소에 연결
        Flux<Long> rankEvents = webTestClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/orders/pass")
                        .queryParam("memberId", redisMemberId)
                        .build())
                .accept(MediaType.TEXT_EVENT_STREAM)
                .exchange()
                .expectStatus().isOk()
                .expectHeader()
                .contentTypeCompatibleWith(MediaType.TEXT_EVENT_STREAM)
                .returnResult(Long.class)
                .getResponseBody();

        StepVerifier.create(rankEvents)
                //대기열에 있을 때 1순위 전송
                .expectNext(1L)
                //Scheduler가 실행하는 입장 허용 처리를 직접 실행
                .then(() -> memberQueueService.popMember(1L).block())
                //대기열에서 제거되고 입장 권한이 생기면 0 전송
                .expectNext(0L)
                //Controller의 takeUntil 조건에 따라 SSE 연결 종료
                .verifyComplete();

        //1 이상 → 현재 대기 순위
        //0     → 입장 권한 획득
        //-1    → 대기열에도 없고 입장 권한도 없음
    }
}