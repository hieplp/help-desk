package dev.hieplp.helpdesk.security.jwt;

import dev.hieplp.helpdesk.model.entity.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

/**
 * Issues and parses HS256 JWTs. Claims: {@code sub} = user id,
 * {@code role} = {@link #ROLE_CLAIM}, {@code exp} = now + configured TTL (8h per spec).
 * No refresh token — the client drops the token to log out.
 */
@Component
public class JwtService {

    /** JWT claim carrying the user's role wire value. */
    public static final String ROLE_CLAIM = "role";

    private final JwtEncoder encoder;
    private final JwtDecoder decoder;
    private final Duration ttl;

    /**
     * Creates the service.
     *
     * @param encoder Nimbus encoder
     * @param decoder Nimbus decoder
     * @param ttl token lifetime ({@code app.jwt.ttl}, 8h per spec)
     */
    public JwtService(
            JwtEncoder encoder,
            JwtDecoder decoder,
            @Value("${app.jwt.ttl}") Duration ttl
    ) {
        this.encoder = encoder;
        this.decoder = decoder;
        this.ttl = ttl;
    }

    /**
     * Signs a token for the user.
     *
     * @param user authenticated user; id becomes {@code sub}, role becomes {@code role}
     * @return signed JWT expiring after the configured TTL
     */
    public Jwt issue(User user) {
        var expiresAt = Instant.now().plus(ttl).truncatedTo(ChronoUnit.SECONDS);
        var claims = JwtClaimsSet.builder()
                .subject(user.getId().toString())
                .claim(ROLE_CLAIM, user.getRole().toJson())
                .expiresAt(expiresAt)
                .build();
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims));
    }

    /**
     * Decodes and verifies a token.
     *
     * @param token raw JWT (without the {@code Bearer } prefix)
     * @return verified JWT
     * @throws org.springframework.security.oauth2.jwt.JwtException on bad signature, malformed token, or expiry
     */
    public Jwt parse(String token) {
        return decoder.decode(token);
    }
}
