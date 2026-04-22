package com.example.orderservice.service;

import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;

@Service
@RequiredArgsConstructor
public class MemberQueueService {
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;    //non-blocking
    private final String QUEUE_KEY = "order-queue";

    //대기열 등록
    public Mono<Long> registerMemberQueue(String memberId){
        long unixTimestamp = Instant.now().getEpochSecond();
        return reactiveRedisTemplate
                .opsForZSet()
                .add(QUEUE_KEY, memberId, unixTimestamp)    //("Key", "Value", score): score가 낮은 순으로 줄세우기
                                                            //.add 기존에 있는 값 다시 넣으면 스코어 갱신되어 후순위로 재정렬됨
                .then(this.getRank(memberId));
    }

    //순서 조회
    public Mono<Long> getRank(String memberId){
        return reactiveRedisTemplate
                .opsForZSet()
                .rank(QUEUE_KEY, memberId)
                .map(rank -> rank+1);   //순위 1부터 보여주기 위한 +1: redis 순위(0부터 시작)
    }

    //대기열에서 특정 수만큼 인원 pop해서 입장 허용 목록으로 이동
    public Mono<Long> popMember(long count){
        return reactiveRedisTemplate
                .opsForZSet()
                .popMin(QUEUE_KEY, count)   //.popMin Removes and returns up to count members with the lowest scores in the sorted set stored at key
                .flatMap(tuple -> {

                    String memberId = tuple.getValue();
                    return reactiveRedisTemplate
                            .opsForValue()
                            .set("allow:" + memberId, "true", java.time.Duration.ofMinutes(5));    //5분간 키 유지
                                                                                                            //allow: 로 시작하는 모든 키 조회
                                                                                                            //keys allow:*
                                                                                                            //.opsForHash 하나의 키로 묶음 but 유효시간 설정x
                            //내장 TTL vs 작업 없는 거 시간 체크해서 튕겨내기
                })
                .count();
    }

    //입장 허용된 상태인지 확인
    public Mono<Boolean> isAllowed(String memberId){
        return reactiveRedisTemplate.hasKey("allow:" + memberId);
    }
}
