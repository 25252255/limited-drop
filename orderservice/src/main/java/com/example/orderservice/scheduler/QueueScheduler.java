package com.example.orderservice.scheduler;

import com.example.orderservice.service.MemberQueueService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class QueueScheduler {
    private final MemberQueueService memberQueueService;

    //10초마다 10명씩 대기열에서 입장 허용 목록으로 이동
    @Scheduled(fixedDelay = 10000)  //이전 작업이 끝난 시점부터 10,000ms
    public void allowMember(){
        memberQueueService.popMember(10L) //10명 pop
                .subscribe(
                        popedCount -> {
                            if (popedCount > 0) {
                                log.info("{}명 허용 완료", popedCount);
                            } else {
                                log.debug("대기열 empty");
                                //log.info("대기열 empty");
                            }
                        },
                        error -> log.error("allowMemberScheduler Error:{}", error.getMessage())
                );
    }
}
