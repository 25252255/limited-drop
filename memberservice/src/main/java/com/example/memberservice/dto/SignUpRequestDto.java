package com.example.memberservice.dto;

public class SignUpRequestDto {
    private String password;
    private int roleType;
    private String email;
    private String name;

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
