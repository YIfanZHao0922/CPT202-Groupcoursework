package com.cpt202.pss.service;

import com.cpt202.pss.dto.AuthDto;
import com.cpt202.pss.entity.User;
import com.cpt202.pss.exception.BusinessException;
import com.cpt202.pss.repository.UserRepository;
import com.cpt202.pss.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtUtil jwtUtil;

    @InjectMocks private AuthService authService;

    private AuthDto.RegisterRequest req;

    @BeforeEach
    void setUp() {
        req = new AuthDto.RegisterRequest();
        req.setUsername("alice");
        req.setPassword("secret123");
        req.setEmail("alice@xjtlu.edu.cn");
        req.setFullName("Alice Wong");
        req.setRole(User.Role.Student);
    }

    @Test
    void register_succeeds_forNewStudent() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@xjtlu.edu.cn")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$encoded$");
        when(userRepository.save(any(User.class)))
                .thenAnswer(inv -> { User u = inv.getArgument(0); u.setUserId(1); return u; });
        when(jwtUtil.generateToken(eq(1), eq("alice"), eq("Student"))).thenReturn("jwt-token");
        when(jwtUtil.getExpirationMs()).thenReturn(3600000L);

        AuthDto.LoginResponse resp = authService.register(req);

        assertThat(resp.getToken()).isEqualTo("jwt-token");
        assertThat(resp.getUsername()).isEqualTo("alice");
        assertThat(resp.getRole()).isEqualTo(User.Role.Student);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_throws_whenUsernameTaken() {
        when(userRepository.existsByUsername("alice")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Username already taken");
        verify(userRepository, never()).save(any());
    }

    @Test
    void register_throws_whenSelfRegisteringAsAdmin() {
        req.setRole(User.Role.Admin);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByEmail(anyString())).thenReturn(false);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Cannot self-register as Admin");
    }

    @Test
    void register_throws_whenEmailExists() {
        when(userRepository.existsByUsername("alice")).thenReturn(false);
        when(userRepository.existsByEmail("alice@xjtlu.edu.cn")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(req))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("Email already registered");
    }
}
