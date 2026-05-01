package com.cpt202.pss.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

public class CategoryDto {

    @Data
    public static class CreateRequest {
        @NotBlank
        @Size(max = 50)
        private String name;
        @Size(max = 255)
        private String description;
    }

    @Data
    public static class UpdateRequest {
        @Size(max = 50)
        private String name;
        @Size(max = 255)
        private String description;
    }
}
