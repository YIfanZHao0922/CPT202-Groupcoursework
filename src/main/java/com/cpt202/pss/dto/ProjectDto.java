package com.cpt202.pss.dto;

import com.cpt202.pss.entity.Project;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

public class ProjectDto {

    @Data
    public static class CreateRequest {
        @NotBlank
        @Size(max = 200)
        private String title;
        @Size(max = 200)
        private String description;
        @Size(max = 200)
        private String requiredSkills;
        @Min(1)
        private Integer maxStudents = 1;
        private Set<Integer> categoryIds;
    }

    @Data
    public static class UpdateRequest {
        @Size(max = 200)
        private String title;
        @Size(max = 200)
        private String description;
        @Size(max = 200)
        private String requiredSkills;
        @Min(1)
        private Integer maxStudents;
        private Project.Status status;
        private Set<Integer> categoryIds;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Response {
        private Integer projectId;
        private Integer teacherId;
        private String teacherName;
        private String title;
        private String description;
        private String requiredSkills;
        private Integer maxStudents;
        private Integer currentStudents;
        private Project.Status status;
        private LocalDateTime createdAt;
        private List<CategoryBrief> categories;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryBrief {
        private Integer categoryId;
        private String name;
    }
}
