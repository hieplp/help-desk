package dev.hieplp.helpdesk.controller;

import dev.hieplp.helpdesk.model.dto.auth.LoginRequest;
import dev.hieplp.helpdesk.model.dto.auth.LoginResponse;
import dev.hieplp.helpdesk.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authentication endpoints. {@code POST /auth/login} is the only route that works without a Bearer
 * token.
 */
@RestController
@RequestMapping(path = "/auth")
@RequiredArgsConstructor
public class AuthController {

  private final AuthService authService;

  /**
   * Exchanges credentials for a JWT.
   *
   * @param request email + password
   * @return 200 signed token + user profile
   * @throws dev.hieplp.helpdesk.exception.ApiException 401 on unknown email or wrong password —
   *         same message either way
   */
  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest request) {
    return authService.login(request);
  }
}
