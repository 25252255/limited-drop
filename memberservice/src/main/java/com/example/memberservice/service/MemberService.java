package com.example.memberservice.service;

import com.example.memberservice.domain.Member;
import com.example.memberservice.dto.MemberResponseDto;
import com.example.memberservice.dto.SignUpRequestDto;
import com.example.memberservice.repository.MemberRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class MemberService {
    private final MemberRepository memberRepository;

    public MemberService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    public void signUp(SignUpRequestDto signUpRequestDto){
        Member member = new Member(
                signUpRequestDto.getPassword(),
                signUpRequestDto.getRoleType(),
                signUpRequestDto.getEmail(),
                signUpRequestDto.getName()
        );

        this.memberRepository.save(member);
    }

    public MemberResponseDto getMember(Long id){
        Member member = memberRepository.findById(id)
                .orElseThrow(()->new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        return new MemberResponseDto(
                member.getMemberId(),
                member.getRoleType(),
                member.getEmail(),
                member.getName()
        );
    }

    public List<MemberResponseDto> getMembersbyIds(List<Long> ids){
        List<Member> members = memberRepository.findAllById(ids);
        return members.stream()
                .map(member -> new MemberResponseDto(
                        member.getMemberId(),
                        member.getRoleType(),
                        member.getEmail(),
                        member.getName()
                ))
                .collect(Collectors.toList());
    }
}
