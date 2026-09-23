package dev.hieplp.helpdesk.service;

import dev.hieplp.helpdesk.model.dto.LoginRequest;
import dev.hieplp.helpdesk.model.dto.LoginResponse;

public interface AuthService {

    LoginResponse login(LoginRequest request);

}
