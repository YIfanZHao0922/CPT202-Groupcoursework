package com.cpt202.pss.dto;

import com.cpt202.pss.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

public class AuthDto {

    @Data
    public static class LoginRequest {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @Data
    public static class RegisterRequest {
        @NotBlank
        @Size(min = 3, max = 50)
        private String username;
        @NotBlank
        @Size(min = 6, max = 100)
        private String password;
        @NotBlank
        @Email
        private String email;
        @NotBlank
        @Size(max = 100)
        private String fullName;
        @NotNull
        private User.Role role;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LoginResponse {
        private String token;
        private String tokenType = "Bearer";
        private Long expiresIn;
        private Integer userId;
        private String username;
        private String fullName;
        private User.Role role;
    }
}
