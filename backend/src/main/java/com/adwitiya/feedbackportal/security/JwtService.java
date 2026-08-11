package com.adwitiya.feedbackportal.security;

import com.adwitiya.feedbackportal.config.properties.JwtProperties;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.HexFormat;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtService {
    public static final String CLAIM_ROLE = "role";
    public static final String CLAIM_USER_ID = "uid";
    public static final String CLAIM_DEPARTMENT_ID = "dept";
    public static final String CLAIM_TOKEN_TYPE = "typ";
    public static final String TOKEN_TYPE_ACCESS = "access";

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final JwtProperties properties;
    private SecretKey signingKey;

    @PostConstruct
    public void initialiseSigningKey() {
        String secret = properties.getSecret();
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "app.jwt.secret is not set. Generate one with: openssl rand -base64 64");
        }
        byte[] keyBytes;
        try {
            keyBytes = Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException ex) {
            keyBytes = secret.getBytes(StandardCharsets.UTF_8);
        }
        if (keyBytes.length < JwtProperties.MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "app.jwt.secret decodes to %d bytes; HS512 requires at least %d"
                            .formatted(keyBytes.length, JwtProperties.MIN_SECRET_BYTES));
        }
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        log.info("JWT signing key initialised ({} bytes, issuer '{}')", keyBytes.length, properties.getIssuer());
    }

    public String generateAccessToken(AppUserDetails principal) {
        Instant now = Instant.now();
        Instant expiry = now.plus(properties.getAccessTokenTtl());

        return Jwts.builder()
                .subject(principal.email())
                .issuer(properties.getIssuer())
                .issuedAt(Date.from(now))
                .expiration(Date.from(expiry))

                .claim(CLAIM_USER_ID, principal.id())
                .claim(CLAIM_ROLE, principal.role().name())
                .claim(CLAIM_DEPARTMENT_ID, principal.departmentId() == null ? -1L : principal.departmentId())
                .claim(CLAIM_TOKEN_TYPE, TOKEN_TYPE_ACCESS)
                .signWith(signingKey, Jwts.SIG.HS512)
                .compact();
    }

    public Optional<Claims> parseAccessToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(signingKey)
                    .requireIssuer(properties.getIssuer())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!TOKEN_TYPE_ACCESS.equals(claims.get(CLAIM_TOKEN_TYPE, String.class))) {
                log.debug("Rejected token: wrong type claim");
                return Optional.empty();
            }
            return Optional.of(claims);
        } catch (JwtException | IllegalArgumentException ex) {
            log.debug("Rejected token: {}", ex.getMessage());
            return Optional.empty();
        }
    }

    public String generateRefreshTokenValue() {
        byte[] bytes = new byte[32];
        SECURE_RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public String hashRefreshToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is required by the JDK specification", ex);
        }
    }

    public Instant accessTokenExpiry() {
        return Instant.now().plus(properties.getAccessTokenTtl());
    }

    public Instant refreshTokenExpiry() {
        return Instant.now().plus(properties.getRefreshTokenTtl());
    }

    public long accessTokenTtlSeconds() {
        return properties.getAccessTokenTtl().toSeconds();
    }
}
