package com.kitchen.dto;

import lombok.Data;

@Data
public class LoginResponse {
    private Long userId;
    private String username;
    private String role;
    private String token;
}
