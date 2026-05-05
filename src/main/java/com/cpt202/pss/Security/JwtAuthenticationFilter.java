package com.cpt202.pss.security;

import com.cpt202.pss.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserRepository userRepository;

    @Value("${app.jwt.header}")
    private String header;
    @Value("${app.jwt.prefix}")
    private String prefix;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        try {
            String token = resolveToken(request);
            if (StringUtils.hasText(token) && jwtUtil.validate(token)) {
                String username = jwtUtil.getUsername(token);
                userRepository.findByUsername(username).ifPresent(user -> {
                    UserPrincipal principal = new UserPrincipal(user);
                    UsernamePasswordAuthenticationToken auth =
                            new UsernamePasswordAuthenticationToken(
                                    principal, null, principal.getAuthorities());
                    auth.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(auth);
                });
            }
        } catch (Exception e) {
            // Token failures fall through; SecurityConfig will reject protected endpoints.
            logger.debug("JWT processing failed: " + e.getMessage());
        }
        chain.doFilter(request, response);
    }

    private String resolveToken(HttpServletRequest req) {
        String h = req.getHeader(header);
        if (StringUtils.hasText(h) && h.startsWith(prefix)) {
            return h.substring(prefix.length()).trim();
        }
        return null;
    }
}
