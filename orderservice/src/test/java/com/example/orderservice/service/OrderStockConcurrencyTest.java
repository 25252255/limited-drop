package com.example.orderservice.service;

import com.example.orderservice.domain.Order;
import com.example.orderservice.domain.OrderRepository;
import com.example.orderservice.domain.OrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class OrderStockConcurrencyTest {

    @Autowired
    private OrderService orderService;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private static final Long TEST_PRODUCT_ID = 999998L;
    private static final String STOCK_KEY = "product:stock:" + TEST_PRODUCT_ID;

    private final List<String> createdIdempotencyKeys = new ArrayList<>();

    //테스트 전 Redis 재고를 10개로 설정
    @BeforeEach
    void setUp() {
        createdIdempotencyKeys.clear();

        reactiveRedisTemplate
                .opsForValue()
                .set(STOCK_KEY, "10")   //재고 10
                .block();   //테스트 환경이라 사용 가능
    }

    @AfterEach
    void cleanUp() {
        for (String key : createdIdempotencyKeys) {
            orderRepository.findByIdempotencyKey(key)
                    .ifPresent(orderRepository::delete);
        }

        reactiveRedisTemplate
                .delete(STOCK_KEY)
                .block();
    }

    //재고 10개에 서로 다른 주문 20개 동시 요청 시 10건만 성공
    @Test
    void concurrentOrdersDoNotDecreaseStockBelowZero() {
        int requestCount = 20;
        int quantity = 1;

        String keyPrefix = "stock-concurrency-test-" + UUID.randomUUID() + "-";
        List<String> requestKeys = new ArrayList<>();

        //멱등성 키가 모두 다름, 서로 다른 주문 20건
        for (int i = 0; i < requestCount; i++) {
            requestKeys.add(keyPrefix + i);
        }

        createdIdempotencyKeys.addAll(requestKeys);

        //주문 20건 동시 실행
        List<Boolean> results = Flux.fromIterable(requestKeys)
                .flatMap(key ->
                                orderService.createOrder(
                                                key,
                                                2L,
                                                TEST_PRODUCT_ID,
                                                quantity    //주문 수량: 요청당 1
                                        )
                                        .map(response -> true)
                                        .onErrorReturn(false),  //성공과 실패를 Boolean으로 변환
                        requestCount
                )
                .collectList()
                .block(Duration.ofSeconds(20));

        //응답 수 검증
        assertNotNull(results);
        assertEquals(requestCount, results.size());

        //성공,실패 개수 검증
        long successCount = results.stream()
                .filter(Boolean::booleanValue)
                .count();

        long failedCount = results.stream()
                .filter(result -> !result)
                .count();

        assertEquals(10, successCount);//초기 재고: 10
        assertEquals(10, failedCount);

        //20개의 멱등성 키를 이용해 DB에 저장된 주문을 모두 조회
        List<Order> savedOrders = requestKeys.stream()
                //orderRepository: Spring Data JPA가 만든 구현 객체를 주입받아 DB에 접근
                //orElseThrow: Optional<Order>에 주문이 있으면 Order를 꺼내고, 없으면 예외 발생
                .map(key -> orderRepository.findByIdempotencyKey(key).orElseThrow())
                .toList();

        //성공,실패 개수 검증
        long completedCount = savedOrders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.COMPLETED)
                .count();

        long failedOrderCount = savedOrders.stream()
                .filter(order -> order.getOrderStatus() == OrderStatus.FAILED)
                .count();

        assertEquals(10, completedCount);
        assertEquals(10, failedOrderCount);

        String remainingStock = reactiveRedisTemplate
                .opsForValue()
                .get(STOCK_KEY)
                .block();

        assertEquals("0", remainingStock);//최종 재고: 0

        //추가 차감
        //- 성공 건수가 10건보다 많음
        //- 최종 재고가 음수
        //- COMPLETED 주문이 10건보다 많음
    }
}