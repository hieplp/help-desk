package dev.hieplp.helpdesk;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class HdServiceApplication {

    static void main(String[] args) {
        SpringApplication.run(HdServiceApplication.class, args);
    }

}
