package com.example.orderservice.service;

import com.example.orderservice.service.MemberQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    public Mono<Boolean> createOrder(String memberId, Long productId, int quantity){
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
    }

    //HACK : Kafka 주문 성공 이벤트 전송, 저장
}
