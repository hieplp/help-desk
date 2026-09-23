package dev.hieplp.helpdesk.service.impl;

import dev.hieplp.helpdesk.exception.ApiException;
import dev.hieplp.helpdesk.model.dto.LoginRequest;
import dev.hieplp.helpdesk.model.dto.LoginResponse;
import dev.hieplp.helpdesk.model.dto.TokenResponse;
import dev.hieplp.helpdesk.model.dto.UserResponse;
import dev.hieplp.helpdesk.repository.UserRepository;
import dev.hieplp.helpdesk.security.JwtService;
import dev.hieplp.helpdesk.service.AuthService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final String DUMMY_HASH = "$2b$10$DAYpGoJiZlrl39dcRh5cmufbqWOqE2qS0uKvX3JGGb9tQY9F0JzdW";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

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
        return new LoginResponse(
                TokenResponse.from(jwt),
                UserResponse.from(user)
        );
    }
}
