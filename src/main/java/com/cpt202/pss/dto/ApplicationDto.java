package com.cpt202.pss.dto;

import com.cpt202.pss.entity.Application;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

public class ApplicationDto {

    @Data
    public static class CreateRequest {
        @NotNull
        private Integer projectId;
        @Size(max = 200)
        private String notes;
    }

    @Data
    public static class DecisionRequest {
        /** ACCEPTED or REJECTED */
        @NotNull
        private Application.Status status;
        @Size(max = 200)
        private String feedback;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Integer applicationId;
        private Integer projectId;
        private String projectTitle;
        private Integer studentId;
        private String studentName;
        private Integer teacherId;
        private String teacherName;
        private Application.Status status;
        private String notes;
        private String feedback;
        private LocalDateTime appliedAt;
    }
}
