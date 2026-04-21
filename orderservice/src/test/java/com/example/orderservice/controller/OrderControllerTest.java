package com.example.orderservice.controller;

import com.example.orderservice.service.MemberQueueService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.autoconfigure.web.reactive.WebFluxTest;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.*;

//@WebFluxTest
@SpringBootTest
@AutoConfigureWebTestClient
class OrderControllerTest {

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Autowired
    private MemberQueueService memberQueueService;

    // 테스트 전마다 Redis 비우기
    @BeforeEach
    void clean() {
        reactiveRedisTemplate.opsForZSet().delete("order-queue").subscribe();
    }

    //대기열 등록
    @Test
    void registerMemberQueue() {
        StepVerifier.create(memberQueueService.registerMemberQueue("user1"))
                //구현한 메서드가 Mono<Long>을 반환해서 L타입 명시, +1해서 0L이 아닌 1L
                .expectNext(1L)
                .verifyComplete();

        StepVerifier.create(memberQueueService.registerMemberQueue("user2"))
                .expectNext(2L)
                .verifyComplete();
    }

    //순서 조회
    @Test
    void getRank() {
        memberQueueService.registerMemberQueue("user1").block();
        memberQueueService.registerMemberQueue("user2").block();

        StepVerifier.create(memberQueueService.getRank("user2"))
                .expectNext(2L)
                .verifyComplete();
    }


    /*
    //초기 설정 테스트
    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    @Test
    public void hello(){
        webTestClient.get().uri("/orders/order")
                .exchange().expectStatus().isOk()
                .expectBody(String.class).isEqualTo("hello");
    }

    @Test
    public void bye(){
        webTestClient.get().uri("/orders/order")
                .exchange().expectStatus().isOk()
                .expectBody(String.class).isEqualTo("bye");
    }

    @Test
    public void redisPingPong(){
        String key = "ping";
        String value = "pong";

        Mono<Boolean> set = reactiveRedisTemplate.opsForValue().set(key, value);

        Mono<String> get = reactiveRedisTemplate.opsForValue().get(key);

        //저장 성공 확인 후, 가져온 값이 pong인지 확인
        StepVerifier.create(set)
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(get)
                .expectNext("pong")
                //.expectNext("fail")
                .verifyComplete();

        //reactiveRedisTemplate.execute(connection -> connection.ping());
    }*/

}