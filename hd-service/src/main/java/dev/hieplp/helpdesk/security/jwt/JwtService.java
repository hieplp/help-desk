package dev.hieplp.helpdesk.security.jwt;

import dev.hieplp.helpdesk.model.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Component
public class JwtService {

    public static final String ROLE_CLAIM = "role";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration ttl;

    public JwtService(
            JwtEncoder encoder,
            JwtDecoder decoder,
            @Value("${app.jwt.ttl}") Duration ttl
    ) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.ttl = ttl;
    }

    public Jwt issue(User user) {
        var expiresAt = Instant.now().plus(ttl).truncatedTo(ChronoUnit.SECONDS);
        var claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .claim(ROLE_CLAIM, user.getRole().toJson())
                .expiresAt(expiresAt)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
    }

    public Jwt parse(String token) {
        return decoder.decode(token);
    }
}
