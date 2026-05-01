package com.cpt202.pss.controller;

import com.cpt202.pss.dto.ApiResponse;
import com.cpt202.pss.dto.UserDto;
import com.cpt202.pss.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /** Current user profile - any authenticated caller. */
    @GetMapping("/me")
    public ApiResponse<UserDto.Response> me() {
        return ApiResponse.success(userService.getCurrent());
    }

    @PutMapping("/me")
    public ApiResponse<UserDto.Response> updateMe(@Valid @RequestBody UserDto.UpdateProfileRequest req) {
        Integer myId = userService.getCurrent().getUserId();
        return ApiResponse.success("Profile updated", userService.updateProfile(myId, req));
    }

    @PostMapping("/me/change-password")
    public ApiResponse<Void> changePassword(@Valid @RequestBody UserDto.ChangePasswordRequest req) {
        Integer myId = userService.getCurrent().getUserId();
        userService.changePassword(myId, req);
        return ApiResponse.success("Password changed", null);
    }

    /* ---------- Admin-only ---------- */

    @GetMapping
    @PreAuthorize("hasRole('Admin')")
    public ApiResponse<List<UserDto.Response>> list(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(userService.list(keyword));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ApiResponse<UserDto.Response> get(@PathVariable Integer id) {
        return ApiResponse.success(userService.get(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ApiResponse<UserDto.Response> adminUpdate(@PathVariable Integer id,
                                                     @Valid @RequestBody UserDto.AdminUpdateRequest req) {
        return ApiResponse.success("User updated", userService.adminUpdate(id, req));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('Admin')")
    public ApiResponse<Void> delete(@PathVariable Integer id) {
        userService.delete(id);
        return ApiResponse.success("User deleted", null);
    }
}
