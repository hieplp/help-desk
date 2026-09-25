package dev.hieplp.helpdesk.security.jwt;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

/**
 * Reads {@code Authorization: Bearer <jwt>} and installs a {@link dev.hieplp.helpdesk.security.principal.Caller}-compatible
 * {@link org.springframework.security.core.Authentication} (principal = user id,
 * authority = role). A missing or bad token leaves the request anonymous — the
 * entry point answers 401 downstream.
 */
@RequiredArgsConstructor
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final String BEARER_PREFIX = "Bearer ";
    private final JwtService jwtService;

    /**
     * Authenticates the request when a Bearer header is present, then continues the chain.
     */
    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain chain
    ) throws ServletException, IOException {
        var header = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (header != null && header.startsWith(BEARER_PREFIX)) {
            authenticate(header.substring(BEARER_PREFIX.length()));
        }
        chain.doFilter(request, response);
    }

    /**
     * Parses the token and stores the authentication in the {@link SecurityContextHolder}.
     * Swallows token errors on purpose: anonymous requests are rejected later with 401.
     *
     * @param token raw JWT without the {@code Bearer } prefix
     */
    private void authenticate(String token) {
        try {
            var jwt = jwtService.parse(token);
            var userId = Long.valueOf(Objects.requireNonNull(jwt.getSubject()));
            var role = new SimpleGrantedAuthority(Objects.requireNonNull(jwt.getClaimAsString(JwtService.ROLE_CLAIM)));
            var auth = new UsernamePasswordAuthenticationToken(userId, null, List.of(role));
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (JwtException | IllegalArgumentException e) {
            // Bad token: stay anonymous, the entry point answers 401.
        }
    }
}
