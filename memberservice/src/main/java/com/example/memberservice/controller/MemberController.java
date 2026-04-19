package com.example.memberservice.controller;

import com.example.memberservice.dto.MemberResponseDto;
import com.example.memberservice.dto.SignUpRequestDto;
import com.example.memberservice.service.MemberService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/members")
public class MemberController {
    private final MemberService memberService;

    public MemberController(MemberService memberService) {
        this.memberService = memberService;
    }

    //회원가입
    @PostMapping("signUp")
    public ResponseEntity<Void> signUp(@RequestBody SignUpRequestDto signUpRequestDto){
        memberService.signUp(signUpRequestDto);
        return ResponseEntity.noContent().build(); //204로 응답 받기 위함
        //만약 return id를 하고 싶으면 생성된 id를 service에서 받아온 걸 리턴해야함
    }

    //회원조회, 1. 게시글 불러올 때 작성자 필요해서
    @GetMapping("{memberId}")
    public ResponseEntity<MemberResponseDto> getMember(@PathVariable Long memberId){
        MemberResponseDto memberResponseDto = memberService.getMember(memberId);
        return ResponseEntity.ok(memberResponseDto);
    }

    //여러회원조회
    @GetMapping
    public ResponseEntity<List<MemberResponseDto>> getMembersByIds(@RequestParam List<Long> ids){
        List<MemberResponseDto> memberResponseDtos = memberService.getMembersbyIds(ids);
        return ResponseEntity.ok(memberResponseDtos);
    }

}


