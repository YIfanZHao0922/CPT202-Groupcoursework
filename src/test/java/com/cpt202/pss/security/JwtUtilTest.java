package com.cpt202.pss.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

class JwtUtilTest {

    private JwtUtil jwtUtil;

    @BeforeEach
    void setUp() {
        jwtUtil = new JwtUtil();
        ReflectionTestUtils.setField(jwtUtil, "secret",
                "unit-test-secret-key-must-be-long-enough-for-hs256-algorithm-padding-x");
        ReflectionTestUtils.setField(jwtUtil, "expirationMs", 3600000L);
    }

    @Test
    void generateAndParseToken_returnsOriginalClaims() {
        String token = jwtUtil.generateToken(42, "alice", "Student");

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.getUsername(token)).isEqualTo("alice");
        assertThat(jwtUtil.getUserId(token)).isEqualTo(42);
        assertThat(jwtUtil.getRole(token)).isEqualTo("Student");
        assertThat(jwtUtil.validate(token)).isTrue();
    }

    @Test
    void validate_returnsFalse_forTamperedToken() {
        String token = jwtUtil.generateToken(1, "bob", "Teacher");
        // Tamper with payload
        String tampered = token.substring(0, token.length() - 4) + "AAAA";
        assertThat(jwtUtil.validate(tampered)).isFalse();
    }

    @Test
    void validate_returnsFalse_forGarbage() {
        assertThat(jwtUtil.validate("not.a.token")).isFalse();
        assertThat(jwtUtil.validate("")).isFalse();
    }
}
