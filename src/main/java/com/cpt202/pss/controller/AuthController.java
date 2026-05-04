package com.cpt202.pss.controller;

import com.cpt202.pss.dto.ApiResponse;
import com.cpt202.pss.dto.AuthDto;
import com.cpt202.pss.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ApiResponse<AuthDto.LoginResponse> register(@Valid @RequestBody AuthDto.RegisterRequest req) {
        return ApiResponse.success("Registered", authService.register(req));
    }

    @PostMapping("/login")
    public ApiResponse<AuthDto.LoginResponse> login(@Valid @RequestBody AuthDto.LoginRequest req) {
        return ApiResponse.success("Login success", authService.login(req));
    }

    /**
     * Stateless JWT - logout is essentially a client-side token discard.
     * Endpoint kept for completeness so the front-end can call it.
     */
    @PostMapping("/logout")
    public ApiResponse<Void> logout() {
        return ApiResponse.success("Logged out", null);
    }
}
