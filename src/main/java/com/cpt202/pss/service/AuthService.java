package com.cpt202.pss.service;

import com.cpt202.pss.dto.AuthDto;
import com.cpt202.pss.entity.User;
import com.cpt202.pss.exception.BusinessException;
import com.cpt202.pss.repository.UserRepository;
import com.cpt202.pss.security.JwtUtil;
import com.cpt202.pss.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Transactional
    public AuthDto.LoginResponse register(AuthDto.RegisterRequest req) {
        if (userRepository.existsByUsername(req.getUsername())) {
            throw new BusinessException("Username already taken: " + req.getUsername());
        }
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BusinessException("Email already registered: " + req.getEmail());
        }
        // Self-registration cannot create Admin accounts (admins are created by other admins).
        if (req.getRole() == User.Role.Admin) {
            throw new BusinessException("Cannot self-register as Admin");
        }

        User user = User.builder()
                .username(req.getUsername())
                .password(passwordEncoder.encode(req.getPassword()))
                .email(req.getEmail())
                .role(req.getRole())
                .fullName(req.getFullName())
                .status(User.Status.ACTIVE)
                .build();
        user = userRepository.save(user);
        return tokenFor(user);
    }

    public AuthDto.LoginResponse login(AuthDto.LoginRequest req) {
        Authentication auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(req.getUsername(), req.getPassword()));
        UserPrincipal up = (UserPrincipal) auth.getPrincipal();
        if (!up.isEnabled()) {
            throw new BusinessException(403, "Account is INACTIVE");
        }
        User user = userRepository.findByUsername(up.getUsername())
                .orElseThrow(() -> new BusinessException(404, "User not found"));
        return tokenFor(user);
    }

    private AuthDto.LoginResponse tokenFor(User user) {
        String token = jwtUtil.generateToken(
                user.getUserId(), user.getUsername(), user.getRole().name());
        return AuthDto.LoginResponse.builder()
                .token(token)
                .tokenType("Bearer")
                .expiresIn(jwtUtil.getExpirationMs())
                .userId(user.getUserId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .role(user.getRole())
                .build();
    }
}
