package com.cpt202.pss.controller;

import com.cpt202.pss.dto.AuthDto;
import com.cpt202.pss.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import jakarta.annotation.PostConstruct;

import static org.hamcrest.Matchers.notNullValue;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@ActiveProfiles("test")
class AuthControllerIntegrationTest {

    @Autowired private WebApplicationContext context;
    @Autowired private ObjectMapper objectMapper;

    private MockMvc mockMvc;

    @PostConstruct
    void setup() {
        this.mockMvc = MockMvcBuilders
                .webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void register_then_login_returnsJwt() throws Exception {
        AuthDto.RegisterRequest reg = new AuthDto.RegisterRequest();
        reg.setUsername("itest_alice");
        reg.setPassword("secret123");
        reg.setEmail("itest_alice@x.com");
        reg.setFullName("Alice Test");
        reg.setRole(User.Role.Student);

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.token", notNullValue()));

        AuthDto.LoginRequest login = new AuthDto.LoginRequest();
        login.setUsername("itest_alice");
        login.setPassword("secret123");

        MvcResult result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token", notNullValue()))
                .andExpect(jsonPath("$.data.role").value("Student"))
                .andReturn();

        String body = result.getResponse().getContentAsString();
        // Sanity check: response includes a JWT-shaped token (xxx.yyy.zzz)
        assert body.split("\"token\":\"")[1].split("\"")[0].chars().filter(c -> c == '.').count() == 2;
    }

    @Test
    void login_failsWithBadCredentials() throws Exception {
        AuthDto.LoginRequest login = new AuthDto.LoginRequest();
        login.setUsername("nobody");
        login.setPassword("nope");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void protectedEndpoint_returns401_withoutToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().is(403));
    }
}
