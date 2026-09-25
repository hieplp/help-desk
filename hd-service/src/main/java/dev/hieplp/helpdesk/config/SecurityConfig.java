package dev.hieplp.helpdesk.config;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import dev.hieplp.helpdesk.model.enums.Role;
import dev.hieplp.helpdesk.security.error.ApiAccessDeniedHandler;
import dev.hieplp.helpdesk.security.error.ApiAuthenticationEntryPoint;
import dev.hieplp.helpdesk.security.jwt.JwtAuthFilter;
import dev.hieplp.helpdesk.security.jwt.JwtService;
import java.nio.charset.StandardCharsets;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import tools.jackson.databind.ObjectMapper;

/**
 * Stateless JWT security: no sessions, no CSRF, no form login. {@code POST /auth/login} and the
 * OpenAPI/Swagger routes are public, {@code GET /users} is agent-only, everything else needs a
 * valid Bearer token. 401/403 responses are JSON via {@link ApiAuthenticationEntryPoint} and {@link
 * ApiAccessDeniedHandler}.
 */
@Configuration
public class SecurityConfig {

  /** Bcrypt encoder for password hashes. */
  @Bean
  PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }

  /**
   * HMAC key for HS256 tokens.
   *
   * @param secret {@code app.jwt.secret}
   * @return signing key
   * @throws IllegalStateException when the secret is under 32 bytes
   */
  @Bean
  SecretKey jwtKey(@Value("${app.jwt.secret}") String secret) {
    var keyBytes = secret.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < 32) {
      throw new IllegalStateException("app.jwt.secret must be at least 32 bytes");
    }
    return new SecretKeySpec(keyBytes, "HmacSHA256");
  }

  /** Nimbus encoder backed by {@link #jwtKey}. */
  @Bean
  JwtEncoder jwtEncoder(SecretKey jwtKey) {
    return new NimbusJwtEncoder(new ImmutableSecret<>(jwtKey));
  }

  /** Nimbus decoder verifying HS256 signatures with {@link #jwtKey}. */
  @Bean
  JwtDecoder jwtDecoder(SecretKey jwtKey) {
    return NimbusJwtDecoder.withSecretKey(jwtKey).macAlgorithm(MacAlgorithm.HS256).build();
  }

  /**
   * Filter chain: {@link JwtAuthFilter} before username/password auth, stateless sessions, route
   * rules per {@code docs/api-rules.md}.
   */
  @Bean
  SecurityFilterChain securityFilterChain(
      HttpSecurity http, JwtService jwtService, ObjectMapper objectMapper) throws Exception {
    return http.csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
        .authorizeHttpRequests(
            auth ->
                auth.requestMatchers(HttpMethod.POST, "/auth/login")
                    .permitAll()
                    .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html")
                    .permitAll()
                    .requestMatchers(HttpMethod.GET, "/users")
                    .hasAuthority(Role.AGENT.toJson())
                    .anyRequest()
                    .authenticated())
        .exceptionHandling(
            e ->
                e.authenticationEntryPoint(new ApiAuthenticationEntryPoint(objectMapper))
                    .accessDeniedHandler(new ApiAccessDeniedHandler(objectMapper)))
        .addFilterBefore(new JwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class)
        .build();
  }
}
