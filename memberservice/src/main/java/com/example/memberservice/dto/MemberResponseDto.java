package com.example.memberservice.dto;

public class MemberResponseDto {
    private Long memberId;
    private int roleType;
    private String email;
    private String name;

    public MemberResponseDto(Long memberId, int roleType, String email, String name) {
        this.memberId = memberId;
        this.roleType = roleType;
        this.email = email;
        this.name = name;
    }

    public Long getMemberId() {
        return memberId;
    }

    public int getRoleType() {
        return roleType;
    }

    public String getEmail() {
        return email;
    }

    public String getName() {
        return name;
    }
}
