package com.example.orderservice.service;

import com.example.orderservice.domain.OrderRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderStatus;
import com.example.orderservice.dto.OrderResponseDto;
import org.junit.jupiter.api.Test;
import reactor.core.publisher.Flux;

import java.util.UUID;
import java.time.Duration;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class OrderServiceConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private static final Long TEST_PRODUCT_ID = 999999L;
    private static final String STOCK_KEY = "product:stock:" + TEST_PRODUCT_ID;

    private String idempotencyKey;

    //테스트 전 Redis 재고를 10개로 설정
    @BeforeEach
    void setUp() {
        idempotencyKey = "concurrency-test-" + UUID.randomUUID();   //테스트를 실행할 때마다 새로운 멱등성 키 생성

        reactiveRedisTemplate
                .opsForValue()
                .set(STOCK_KEY, "10")
                .block();
    }

    //테스트로 만든 DB 주문과 Redis 재고를 삭제
    @AfterEach
    void cleanUp() {
        orderRepository.findByIdempotencyKey(idempotencyKey)
                .ifPresent(orderRepository::delete);

        reactiveRedisTemplate
                .delete(STOCK_KEY)
                .block();   //실제 WebFlux 요청 코드가 아니라 테스트 환경 설정이므로 여기선 사용 가능
    }

    //동일 멱등성 키 20개 동시 요청 시 주문 1건 생성 및 재고 1회 차감
    @Test
    void sameIdempotencyKeyConcurrentRequestsCreateOnlyOneOrder() {
        int requestCount = 20;

        List<OrderResponseDto> responses = Flux.range(0, requestCount)  //0부터 19까지 총 20개의 신호를 만들기
                .flatMap(requestNumber ->   //신호 하나가 발생할 때마다
                                orderService.createOrder(
                                        idempotencyKey,   //모든 호출에 동일한 값 넣음
                                        2L,
                                        TEST_PRODUCT_ID,
                                        2
                                ),
                        requestCount    //20개의 createOrder()를 순서대로 기다리지 않고 동시에 구독(requestCount는 최대 동시 실행 수)
                )
                .collectList()  //응답을 Mono<List<OrderResponseDto>>로
                .block(Duration.ofSeconds(20));

        //응답 수 검증
        assertNotNull(responses);
        assertEquals(requestCount, responses.size());

        //모든 응답의 주문 ID 비교
        long distinctOrderCount = responses.stream()
                .map(OrderResponseDto::orderId)
                .distinct()
                .count();

        assertEquals(1, distinctOrderCount);

        //실제 DB 저장 결과 검증
        Order savedOrder = orderRepository
                .findByIdempotencyKey(idempotencyKey)
                .orElseThrow();

        assertEquals(OrderStatus.COMPLETED, savedOrder.getOrderStatus());   //DB에 저장된 주문의 최종 상태가 COMPLETED인지 확인

        String remainingStock = reactiveRedisTemplate
                .opsForValue()
                .get(STOCK_KEY)
                .block();

        assertEquals("8", remainingStock);
    }
}