package com.adwitiya.feedbackportal.unit;

import com.adwitiya.feedbackportal.config.properties.JwtProperties;
import com.adwitiya.feedbackportal.domain.enums.Role;
import com.adwitiya.feedbackportal.security.AppUserDetails;
import com.adwitiya.feedbackportal.security.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtService")
class JwtServiceTest {
    private static final String SECRET =
            Base64.getEncoder().encodeToString("a".repeat(64).getBytes());

    private JwtService jwtService;
    private AppUserDetails principal;

    @BeforeEach
    void setUp() {
        JwtProperties properties = new JwtProperties();
        properties.setSecret(SECRET);
        properties.setIssuer("test-issuer");
        properties.setAccessTokenTtl(Duration.ofMinutes(15));
        properties.setRefreshTokenTtl(Duration.ofDays(7));

        jwtService = new JwtService(properties);
        jwtService.initialiseSigningKey();

        principal = new AppUserDetails(42L, "admin@university.edu", "hash",
                "Priya Menon", Role.ADMIN, 7L, true, false);
    }

    @Test
    void issuesATokenCarryingEveryClaim() {
        String token = jwtService.generateAccessToken(principal);
        Optional<Claims> claims = jwtService.parseAccessToken(token);

        assertThat(claims).isPresent();
        assertThat(claims.get().getSubject()).isEqualTo("admin@university.edu");
        assertThat(claims.get().getIssuer()).isEqualTo("test-issuer");
        assertThat(claims.get().get(JwtService.CLAIM_USER_ID, Number.class).longValue()).isEqualTo(42L);
        assertThat(claims.get().get(JwtService.CLAIM_ROLE, String.class)).isEqualTo("ADMIN");
        assertThat(claims.get().get(JwtService.CLAIM_DEPARTMENT_ID, Number.class).longValue()).isEqualTo(7L);
    }

    @Test
    void rejectsATamperedToken() {
        String token = jwtService.generateAccessToken(principal);
        String tampered = token.substring(0, token.length() - 4) + "AAAA";

        assertThat(jwtService.parseAccessToken(tampered)).isEmpty();
    }

    @Test
    void rejectsATokenSignedWithAnotherKey() {
        JwtProperties other = new JwtProperties();
        other.setSecret(Base64.getEncoder().encodeToString("b".repeat(64).getBytes()));
        other.setIssuer("test-issuer");
        JwtService foreign = new JwtService(other);
        foreign.initialiseSigningKey();

        String foreignToken = foreign.generateAccessToken(principal);

        assertThat(jwtService.parseAccessToken(foreignToken)).isEmpty();
    }

    @Test
    void rejectsATokenFromAnotherIssuer() {
        JwtProperties other = new JwtProperties();
        other.setSecret(SECRET);
        other.setIssuer("some-other-service");
        JwtService foreign = new JwtService(other);
        foreign.initialiseSigningKey();

        assertThat(jwtService.parseAccessToken(foreign.generateAccessToken(principal))).isEmpty();
    }

    @Test
    void rejectsGarbage() {
        assertThat(jwtService.parseAccessToken("not-a-jwt")).isEmpty();
        assertThat(jwtService.parseAccessToken("")).isEmpty();
    }

    @Test
    void refusesToStartWithNoSecret() {
        JwtProperties empty = new JwtProperties();
        empty.setSecret("");

        assertThatThrownBy(() -> new JwtService(empty).initialiseSigningKey())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("app.jwt.secret");
    }

    @Test
    void refusesToStartWithAShortSecret() {
        JwtProperties weak = new JwtProperties();
        weak.setSecret(Base64.getEncoder().encodeToString("tooshort".getBytes()));

        assertThatThrownBy(() -> new JwtService(weak).initialiseSigningKey())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("HS512 requires at least");
    }

    @Test
    void refreshTokensAreRandomAndOnlyStoredAsHashes() {
        String first = jwtService.generateRefreshTokenValue();
        String second = jwtService.generateRefreshTokenValue();

        assertThat(first).isNotEqualTo(second).hasSizeGreaterThan(32);

        String hash = jwtService.hashRefreshToken(first);
        assertThat(hash).hasSize(64).matches("[0-9a-f]{64}");
        assertThat(hash).isNotEqualTo(first);
        assertThat(jwtService.hashRefreshToken(first)).isEqualTo(hash);
    }
}
