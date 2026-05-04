package com.cpt202.pss.controller;

import com.cpt202.pss.dto.ApiResponse;
import com.cpt202.pss.dto.CategoryDto;
import com.cpt202.pss.entity.Category;
import com.cpt202.pss.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
@RequiredArgsConstructor
public class CategoryController {

    private final CategoryService categoryService;

    /** Public read - used by the project-browse filter UI even before login. */
    @GetMapping
    public ApiResponse<List<Category>> list() {
        return ApiResponse.success(categoryService.list());
    }

    @GetMapping("/{id}")
    public ApiResponse<Category> get(@PathVariable Integer id) {
        return ApiResponse.success(categoryService.get(id));
    }

    @PostMapping
    @PreAuthorize("hasRole('Admin')")
    public ApiResponse<Category> create(@Valid @RequestBody CategoryDto.CreateRequest req) {
        return ApiResponse.success("Category created", categoryService.create(req));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ApiResponse<Category> update(@PathVariable Integer id,
                                        @Valid @RequestBody CategoryDto.UpdateRequest req) {
        return ApiResponse.success("Category updated", categoryService.update(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        categoryService.delete(id);
        return ApiResponse.success("Category deleted", null);
    }
}
