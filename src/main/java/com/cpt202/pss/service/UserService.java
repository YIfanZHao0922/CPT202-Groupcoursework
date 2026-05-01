package com.cpt202.pss.service;

import com.cpt202.pss.dto.UserDto;
import com.cpt202.pss.entity.User;
import com.cpt202.pss.exception.BusinessException;
import com.cpt202.pss.exception.ResourceNotFoundException;
import com.cpt202.pss.repository.UserRepository;
import com.cpt202.pss.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public List<UserDto.Response> list(String keyword) {
        List<User> users = (keyword == null || keyword.isBlank())
                ? userRepository.findAll()
                : userRepository.searchByKeyword(keyword);
        return users.stream().map(this::toDto).toList();
    }

    public UserDto.Response get(Integer id) {
        return toDto(userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id)));
    }

    public UserDto.Response getCurrent() {
        Integer id = SecurityUtils.currentUserId();
        return get(id);
    }

    @Transactional
    public UserDto.Response updateProfile(Integer id, UserDto.UpdateProfileRequest req) {
        // A user may update only their own profile; admins are not constrained.
        Integer currentId = SecurityUtils.currentUserId();
        if (!currentId.equals(id) && SecurityUtils.currentRole() != User.Role.Admin) {
            throw new BusinessException(403, "You can only update your own profile");
        }
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        if (req.getEmail() != null && !req.getEmail().equals(u.getEmail())) {
            if (userRepository.existsByEmail(req.getEmail())) {
                throw new BusinessException("Email already used");
            }
            u.setEmail(req.getEmail());
        }
        if (req.getFullName() != null) u.setFullName(req.getFullName());
        return toDto(userRepository.save(u));
    }

    @Transactional
    public void changePassword(Integer id, UserDto.ChangePasswordRequest req) {
        Integer currentId = SecurityUtils.currentUserId();
        if (!currentId.equals(id)) {
            throw new BusinessException(403, "You can only change your own password");
        }
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        if (!passwordEncoder.matches(req.getOldPassword(), u.getPassword())) {
            throw new BusinessException("Old password is incorrect");
        }
        u.setPassword(passwordEncoder.encode(req.getNewPassword()));
        userRepository.save(u);
    }

    @Transactional
    public UserDto.Response adminUpdate(Integer id, UserDto.AdminUpdateRequest req) {
        User u = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + id));
        if (req.getEmail() != null) u.setEmail(req.getEmail());
        if (req.getFullName() != null) u.setFullName(req.getFullName());
        if (req.getRole() != null) u.setRole(req.getRole());
        if (req.getStatus() != null) u.setStatus(req.getStatus());
        return toDto(userRepository.save(u));
    }

    @Transactional
    public void delete(Integer id) {
        if (!userRepository.existsById(id)) {
            throw new ResourceNotFoundException("User not found: " + id);
        }
        userRepository.deleteById(id);
    }

    private UserDto.Response toDto(User u) {
        return UserDto.Response.builder()
                .userId(u.getUserId())
                .username(u.getUsername())
                .email(u.getEmail())
                .role(u.getRole())
                .fullName(u.getFullName())
                .status(u.getStatus())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
