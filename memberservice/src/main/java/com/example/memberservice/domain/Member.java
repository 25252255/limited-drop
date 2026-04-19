package com.example.memberservice.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "members")
public class Member {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long memberId;
    private String password;
    private int roleType;
    private String email;
    private String name;

    public Member() {
    }

    public Member(String password, int roleType, String email, String name) {
        this.password = password;
        this.roleType = roleType;
        this.email = email;
        this.name = name;
    }

    public Long getMemberId() {
        return memberId;
    }

    public String getPassword() {
        return password;
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
