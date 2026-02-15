package com.adhar.newapp.dto;

import lombok.Data;

@Data
public class LoginRequest {
    @com.fasterxml.jackson.annotation.JsonAlias("email")
    private String username;
    private String password;
}
