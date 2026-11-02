package com.example.orderservice.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import com.example.orderservice.domain.OrderRepository;
import com.example.orderservice.domain.Order;
import reactor.core.scheduler.Schedulers;
import com.example.orderservice.dto.OrderResponseDto;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;
    private final OrderRepository orderRepository;

    /*public Mono<Boolean> createOrder(String memberId, Long productId, int quantity){
        String stockKey = "product:stock:" + productId;

        //1. Lua Script 정의
        //KEYS[1]: stockKey
        //ARGV[1]: quantity(구매하는 수량)
        String luaScript = "local stock = redis.call('get', KEYS[1]) "
                + "if not stock then return -1 end "// 상품 정보 없음
                + "if tonumber(stock) < tonumber(ARGV[1]) then return -2 end " // 재고 부족
                + "return redis.call('decrby', KEYS[1], ARGV[1])"; // 차감 후 남은 재고 반환

        //2. 스크립트 실행
        return reactiveRedisTemplate
                .execute(RedisScript.of(luaScript, Long.class)// 결과 타입을 Long으로 지정
                        , List.of(stockKey)// KEYS
                        , List.of(String.valueOf(quantity))// ARGV
                )
                .next() //Redis 스크립트는 여러 값FLUX을 줄 수도 있어서, 첫 번째 값 하나만MONO 변환해주기 위함
                .flatMap(result -> {
                    if (result == -1) return Mono.error(new RuntimeException("상품 정보가 없습니다."));
                    if (result == -2) return Mono.error(new RuntimeException("재고가 부족합니다."));

                    //성공 시 (result >= 0 이면 남은 재고 수량)
                    log.info("주문서>> [사용자: {}] [상품: {}] [잔여재고: {}]", memberId, productId, result);
                    return Mono.just(true);
                })
                .doOnError(e -> log.error("createOrder Error: {}", e.getMessage()));
    }*/
    /*public Mono<Boolean> createOrder(String memberId, Long productId, int quantity) {
        return decreaseStock(productId, quantity)
                .doOnNext(remainingStock ->
                        log.info(
                                "주문서>> [사용자: {}] [상품: {}] [잔여재고: {}]",
                                memberId,
                                productId,
                                remainingStock
                        )
                )
                .map(remainingStock -> true)
                .doOnError(error ->
                        log.error(
                                "createOrder Error: {}",
                                error.getMessage()
                        )
                );
    }*/
    public Mono<OrderResponseDto> createOrder(String idempotencyKey, Long memberId, Long productId, int quantity) {
        return findOrderByIdempotencyKey(idempotencyKey)
                .map(OrderResponseDto::from) //같은 idempotencyKey 주문 조회, 있으면 기존 주문을 DTO로 반환
                .switchIfEmpty(Mono.defer(() ->
                        createPendingOrder(idempotencyKey, memberId, productId, quantity)
                                .flatMap(this::processPendingOrder)
                                .map(OrderResponseDto::from)    //없으면 PENDING주문 저장, Lua재고 차감, 결과 DTO로 반환
                                //요청 A가 조회한 직후 아직 저장하기 전에 요청 B가 조회하면, 요청 B 입장에서도 DB에 주문이 없으므로 둘 다 “없음”
                                //두 요청 모두 조회 결과가 비었으므로 switchIfEmpty()의 새 주문 생성 코드를 실행하려함 -> 경쟁 상태(race condition)
                                //하지만 DB에 다음 UNIQUE 제약 조건이 있어서 실제 주문 두 개가 저장되지x
                                //따라서 먼저 저장한 요청: 주문 생성 성공, 나중에 저장한 요청: DataIntegrityViolationException 발생
                                //- 나중 요청은 실패로 끝내지 않고, 먼저 저장된 주문을 다시 조회해서 반환
                                .onErrorResume(DataIntegrityViolationException.class, error ->
                                        findOrderByIdempotencyKey(idempotencyKey)
                                                .switchIfEmpty(Mono.error(error))
                                                .map(OrderResponseDto::from)
                                )
                ))
                .doOnError(error ->
                        log.error("createOrder Error: {}", error.getMessage())
                );
    }

    //Lua Script 실행
    private Mono<Long> decreaseStock(Long productId, int quantity) {
        String stockKey = "product:stock:" + productId;

        String luaScript = "local stock = redis.call('get', KEYS[1]) "
                + "if not stock then return -1 end "
                + "if tonumber(stock) < tonumber(ARGV[1]) then return -2 end "
                + "return redis.call('decrby', KEYS[1], ARGV[1])";

        return reactiveRedisTemplate
                .execute(
                        RedisScript.of(luaScript, Long.class),
                        List.of(stockKey),
                        List.of(String.valueOf(quantity))
                )
                .next()
                .switchIfEmpty(
                        Mono.error(
                                new IllegalStateException("재고 차감 결과가 없습니다.")
                        )
                )
                .flatMap(result -> {
                    if (result == -1L) {
                        return Mono.error(
                                new RuntimeException("상품 정보가 없습니다.")
                        );}

                    if (result == -2L) {
                        return Mono.error(
                                new RuntimeException("재고가 부족합니다.")
                        );}

                    return Mono.just(result);
                });
    }

    //JPA의 동기식 주문 저장을 WebFlux의 Mono 흐름에 연결하기 위한 내부 메서드
    // - JPA는 DB 응답이 올 때까지 현재 스레드를 기다리게 하는 블로킹 방식
    // - JPA 자체가 논블로킹으로 바뀌는 것x, JPA의 블로킹 작업이 WebFlux의 이벤트 루프를 막지 않도록 별도 스레드로 격리
    private Mono<Order> saveOrder(Order order) {
        return Mono.fromCallable(() ->
                        orderRepository.saveAndFlush(order) //Mono가 구독될 때 이 DB 저장 작업을 실행한 뒤(DB에 저장), 실행 결과로 저장된 Order를 반환
                )
                .subscribeOn(Schedulers.boundedElastic());  //DB 저장 작업을 WebFlux 요청 처리 스레드가 아닌 블로킹 작업용 별도 스레드 풀에서 실행
    }

    //컨트롤러에서 사용
    //외부 응답용 OrderResponseDto 반환
    public Mono<OrderResponseDto> getOrderByIdempotencyKey(String idempotencyKey) {
        return findOrderByIdempotencyKey(idempotencyKey)
                .map(OrderResponseDto::from);
    }

    //멱등성 키로 이미 만들어진 주문이 있는지 조회
    //DB의 Order 엔티티 반환
    private Mono<Order> findOrderByIdempotencyKey(String idempotencyKey) {
        return Mono.fromCallable(() ->
                        orderRepository.findByIdempotencyKey(idempotencyKey)
                )
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(optionalOrder ->   //Mono<Optional<Order>>
                        Mono.justOrEmpty(optionalOrder) //기존 주문이 있으면 Order 반환, 없으면 빈 Mono 반환
                );
    }

    //PENDING 주문을 생성하고 저장
    private Mono<Order> createPendingOrder(String idempotencyKey, Long memberId, Long productId, int quantity) {
        Order order = new Order(//생성자로 만들어 놨음
                idempotencyKey,
                memberId,
                productId,
                quantity);

        return saveOrder(order);
    }

    //DB에 저장된 PENDING 주문에 Lua재고 차감 결과(COMPLETED/FAILED)를 반영
    private Mono<Order> processPendingOrder(Order order) {
        return decreaseStock(order.getProductId(), order.getQuantity())
                .flatMap(remainingStock -> {
                    order.complete();

                    return saveOrder(order)
                            .doOnSuccess(savedOrder ->
                                    log.info(
                                            "주문 성공>> [주문: {}] [사용자: {}] [상품: {}] [잔여재고: {}]",
                                            savedOrder.getOrderId(),
                                            savedOrder.getMemberId(),
                                            savedOrder.getProductId(),
                                            remainingStock
                                    )
                            );}
                )
                .onErrorResume(error -> {
                    order.fail();

                    return saveOrder(order)
                            .then(Mono.error(error));
                });
    }


    //HACK : Kafka 주문 성공 이벤트 전송, 저장
}
