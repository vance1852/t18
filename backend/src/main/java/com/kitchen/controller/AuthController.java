package com.kitchen.controller;

import com.kitchen.dto.LoginRequest;
import com.kitchen.dto.LoginResponse;
import com.kitchen.dto.Result;
import com.kitchen.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<LoginResponse> login(@RequestBody LoginRequest request) {
        try {
            LoginResponse response = authService.login(request.getUsername(), request.getPassword());
            return Result.success(response);
        } catch (Exception e) {
            return Result.error(e.getMessage());
        }
    }
}
