package com.example.productservice.client;

import com.example.productservice.dto.MemberResponseDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Component
public class MemberClient {
    private static final Logger log = LoggerFactory.getLogger(MemberClient.class);
    private final RestClient restClient; //API통신tool

    public MemberClient(@Value("${client.memberservice.url}") String memberServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(memberServiceUrl)
                .build();
    }

    /*사용자 정보 조회 요청*/
    public Optional<MemberResponseDto> fetchMember(Long memberId){
        try{
            MemberResponseDto memberResponseDto = this.restClient.get()
                    .uri("/members/{memberId}", memberId)
                    .retrieve()
                    .body(MemberResponseDto.class);
            return Optional.ofNullable(memberResponseDto);
        }catch (RestClientException e) {
            log.error("product_사용자 정보 조회 실패");
            return Optional.empty();
        }
    }

    //장애 전파에 null로 대비
    public List<MemberResponseDto> fetchMembersByIds(List<Long> ids){
        try {
            return this.restClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/members")
                            .queryParam("ids", ids)
                            .build())
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
        } catch (RestClientException e) {
            return Collections.emptyList();
        }
    }
    /*
    //장애 전파에 대비 x
    public MemberResponseDto fetchMember(Long memberId){
        return this.restClient.get()
                .uri("/members/{memberId}", memberId)//방식 1.yml에 path 2.@FeignClient 3.Eureka
                .retrieve()
                .body(MemberResponseDto.class);
    }*/

}
