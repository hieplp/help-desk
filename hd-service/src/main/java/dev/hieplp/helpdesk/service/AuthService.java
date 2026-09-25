package dev.hieplp.helpdesk.service;

import dev.hieplp.helpdesk.model.dto.auth.LoginRequest;
import dev.hieplp.helpdesk.model.dto.auth.LoginResponse;

/** Credential check and token issuing. */
public interface AuthService {

  /**
   * Verifies credentials and issues a JWT.
   *
   * @param request email + password
   * @return signed token + user profile
   * @throws dev.hieplp.helpdesk.exception.ApiException 401 with a generic message on any credential
   *     failure — never reveals which part failed
   */
  LoginResponse login(LoginRequest request);
}
