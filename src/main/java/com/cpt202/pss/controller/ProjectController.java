package com.cpt202.pss.controller;

import com.cpt202.pss.dto.ApiResponse;
import com.cpt202.pss.dto.ProjectDto;
import com.cpt202.pss.entity.Project;
import com.cpt202.pss.service.ProjectService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;

    /** Browse / search / filter - any authenticated user. */
    @GetMapping
    public ApiResponse<List<ProjectDto.Response>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Project.Status status,
            @RequestParam(required = false) Integer teacherId,
            @RequestParam(required = false) Integer categoryId) {
        return ApiResponse.success(projectService.search(keyword, status, teacherId, categoryId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ProjectDto.Response> get(@PathVariable Integer id) {
        return ApiResponse.success(projectService.get(id));
    }

    /** Teacher's own project list. */
    @GetMapping("/mine")
    @PreAuthorize("hasAnyRole('Teacher','Admin')")
    public ApiResponse<List<ProjectDto.Response>> mine() {
        return ApiResponse.success(projectService.myProjects());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('Teacher','Admin')")
    public ApiResponse<ProjectDto.Response> create(@Valid @RequestBody ProjectDto.CreateRequest req) {
        return ApiResponse.success("Project created", projectService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('Teacher','Admin')")
    public ApiResponse<ProjectDto.Response> update(@PathVariable Integer id,
                                                   @Valid @RequestBody ProjectDto.UpdateRequest req) {
        return ApiResponse.success("Project updated", projectService.update(id, req));
    }

    /**
     * Convenience publish/close/archive endpoint.
     * Body: { "status": "AVAILABLE" | "CLOSED" | ... }
     */
    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('Teacher','Admin')")
    public ApiResponse<ProjectDto.Response> changeStatus(@PathVariable Integer id,
                                                         @RequestBody Map<String, Project.Status> body) {
        Project.Status s = body.get("status");
        return ApiResponse.success("Status changed", projectService.changeStatus(id, s));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('Teacher','Admin')")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        projectService.delete(id);
        return ApiResponse.success("Project deleted", null);
    }
}
