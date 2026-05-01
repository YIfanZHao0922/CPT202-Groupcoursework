package com.cpt202.pss.controller;

import com.cpt202.pss.dto.ApiResponse;
import com.cpt202.pss.dto.ApplicationDto;
import com.cpt202.pss.service.ApplicationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/applications")
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationService applicationService;

    /** Student: my applications history. */
    @GetMapping("/mine")
    @PreAuthorize("hasRole('Student')")
    public ApiResponse<List<ApplicationDto.Response>> mine() {
        return ApiResponse.success(applicationService.myApplications());
    }

    /** Teacher/Admin: all applications for one project. */
    @GetMapping("/project/{projectId}")
    @PreAuthorize("hasAnyRole('Teacher','Admin')")
    public ApiResponse<List<ApplicationDto.Response>> forProject(@PathVariable Integer projectId) {
        return ApiResponse.success(applicationService.forProject(projectId));
    }

    @GetMapping("/{id}")
    public ApiResponse<ApplicationDto.Response> get(@PathVariable Integer id) {
        return ApiResponse.success(applicationService.get(id));
    }

    /** Student creates an application. */
    @PostMapping
    @PreAuthorize("hasRole('Student')")
    public ApiResponse<ApplicationDto.Response> apply(@Valid @RequestBody ApplicationDto.CreateRequest req) {
        return ApiResponse.success("Application submitted", applicationService.apply(req));
    }

    /** Student withdraws their pending application. */
    @PostMapping("/{id}/withdraw")
    @PreAuthorize("hasRole('Student')")
    public ApiResponse<ApplicationDto.Response> withdraw(@PathVariable Integer id) {
        return ApiResponse.success("Application withdrawn", applicationService.withdraw(id));
    }

    /** Teacher/Admin accepts or rejects. Body: { "status": "ACCEPTED|REJECTED", "feedback": "..." } */
    @PostMapping("/{id}/decision")
    @PreAuthorize("hasAnyRole('Teacher','Admin')")
    public ApiResponse<ApplicationDto.Response> decide(@PathVariable Integer id,
                                                       @Valid @RequestBody ApplicationDto.DecisionRequest req) {
        return ApiResponse.success("Decision recorded", applicationService.decide(id, req));
    }
}
