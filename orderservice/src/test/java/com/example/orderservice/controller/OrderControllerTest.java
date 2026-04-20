package com.example.orderservice.controller;

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
    private WebTestClient webTestClient;

    @Autowired
    private ReactiveRedisTemplate<String, String> redisTemplate;

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

        Mono<Boolean> set = redisTemplate.opsForValue().set(key, value);

        Mono<String> get = redisTemplate.opsForValue().get(key);

        //저장 성공 확인 후, 가져온 값이 pong인지 확인
        StepVerifier.create(set)
                .expectNext(true)
                .verifyComplete();

        StepVerifier.create(get)
                .expectNext("pong")
                //.expectNext("fail")
                .verifyComplete();
    }

}