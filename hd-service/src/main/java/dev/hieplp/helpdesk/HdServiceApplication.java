package dev.hieplp.helpdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Help-desk backend entry point. JPA auditing fills
 * {@code createdAt}/{@code updatedAt} on {@code Auditable} entities.
 */
@EnableJpaAuditing
@SpringBootApplication
public class HdServiceApplication {

    /**
     * Boots the Spring application.
     */
    static void main(String[] args) {
        SpringApplication.run(HdServiceApplication.class, args);
    }

}
