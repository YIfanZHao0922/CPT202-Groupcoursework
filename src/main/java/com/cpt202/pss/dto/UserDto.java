package com.cpt202.pss.dto;

import com.cpt202.pss.entity.User;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class UserDto {

    @Data
    public static class UpdateProfileRequest {
        @Email
        private String email;
        @Size(max = 100)
        private String fullName;
    }

    @Data
    public static class ChangePasswordRequest {
        @NotBlank
        private String oldPassword;
        @NotBlank
        @Size(min = 6, max = 100)
        private String newPassword;
    }

    @Data
    public static class AdminUpdateRequest {
        @Email
        private String email;
        @Size(max = 100)
        private String fullName;
        private User.Role role;
        private User.Status status;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Integer userId;
        private String username;
        private String email;
        private User.Role role;
        private String fullName;
        private User.Status status;
        private LocalDateTime createdAt;
    }
}
