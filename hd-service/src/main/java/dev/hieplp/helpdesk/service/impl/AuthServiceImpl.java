package dev.hieplp.helpdesk.service.impl;

import dev.hieplp.helpdesk.exception.ApiException;
import dev.hieplp.helpdesk.model.dto.auth.LoginRequest;
import dev.hieplp.helpdesk.model.dto.auth.LoginResponse;
import dev.hieplp.helpdesk.model.dto.auth.TokenResponse;
import dev.hieplp.helpdesk.model.dto.user.UserResponse;
import dev.hieplp.helpdesk.repository.UserRepository;
import dev.hieplp.helpdesk.security.jwt.JwtService;
import dev.hieplp.helpdesk.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Verifies credentials against the stored bcrypt hash and issues a JWT. On unknown email it still
 * runs a bcrypt compare against {@link #DUMMY_HASH} so response timing does not reveal whether the
 * account exists.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

  private static final String DUMMY_HASH =
      "$2b$10$DAYpGoJiZlrl39dcRh5cmufbqWOqE2qS0uKvX3JGGb9tQY9F0JzdW";

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;

  /** {@inheritDoc} */
  @Override
  public LoginResponse login(LoginRequest request) {
    log.info("Login attempt for email={}", request.email());

    var optionalUser = userRepository.findByEmail(request.email());
    if (optionalUser.isEmpty()) {
      log.warn("Login failed: unknown email={}", request.email());
      passwordEncoder.matches(request.password(), DUMMY_HASH);
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    final var user = optionalUser.get();

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      log.warn("Login failed: bad password for email={}", request.email());
      throw new ApiException(HttpStatus.UNAUTHORIZED, "Invalid email or password");
    }

    var jwt = jwtService.issue(user);
    return new LoginResponse(TokenResponse.from(jwt), UserResponse.from(user));
  }
}
