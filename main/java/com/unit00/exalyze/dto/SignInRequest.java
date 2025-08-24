package com.unit00.exalyze.dto;


import lombok.Data;

@Data
public class SignInRequest {
    private String email;
    private String password;
}