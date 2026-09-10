package com.orabank.backend.security;

import com.orabank.backend.config.AppProperties;
import com.orabank.backend.entity.Employee;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

/** Emission et verification des jetons d'acces HS256 (un seul token, sans refresh). */
@Service
public class JwtService {

    private final SecretKey key;
    private final Duration expiration;
    private final String issuer;
    private final Clock clock;

    public JwtService(AppProperties properties, Clock clock) {
        this.key = buildKey(properties.jwtSecret());
        this.expiration = properties.jwtExpiration();
        this.issuer = properties.jwtIssuer();
        this.clock = clock;
    }

    public String generateToken(Employee employee) {
        Instant now = clock.instant();
        return Jwts.builder()
                .issuer(issuer)
                .subject(employee.getMatricule())
                .claim("uid", employee.getId())
                .claim("role", employee.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();
    }

    /** Verifie signature, emetteur et expiration. Leve une JwtException si le jeton est invalide. */
    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .requireIssuer(issuer)
                .clockSkewSeconds(30)
                .clock(() -> Date.from(clock.instant()))
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /** Duree de vie du jeton en secondes, renvoyee au frontend. */
    public long expiresInSeconds() {
        return expiration.getSeconds();
    }

    private static SecretKey buildKey(String secret) {
        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException("APP_JWT_SECRET est obligatoire.");
        }
        byte[] bytes = decode(secret);
        if (bytes.length < 32) {
            throw new IllegalStateException("APP_JWT_SECRET doit faire au moins 32 octets (256 bits).");
        }
        return Keys.hmacShaKeyFor(bytes);
    }

    private static byte[] decode(String secret) {
        try {
            return Decoders.BASE64.decode(secret);
        } catch (IllegalArgumentException notBase64) {
            return secret.getBytes(StandardCharsets.UTF_8);
        }
    }
}
