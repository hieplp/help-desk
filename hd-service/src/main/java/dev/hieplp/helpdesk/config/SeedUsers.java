package dev.hieplp.helpdesk.config;

import dev.hieplp.helpdesk.model.entity.User;
import dev.hieplp.helpdesk.model.enums.Role;
import dev.hieplp.helpdesk.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.jspecify.annotations.NonNull;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SeedUsers implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(@NonNull ApplicationArguments args) {
        seed("agent@b.co", "Agent", Role.AGENT, "secret");
        seed("requester@b.co", "Requester", Role.REQUESTER, "secret");
    }

    private void seed(
            String email,
            String name,
            Role role,
            String password
    ) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }

        var user = new User();
        user.setEmail(email);
        user.setName(name);
        user.setRole(role);
        user.setPasswordHash(passwordEncoder.encode(password));
        userRepository.save(user);
    }
}
