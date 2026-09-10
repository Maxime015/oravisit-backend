package com.orabank.backend.config;

import java.time.Duration;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Parametres applicatifs (prefixe {@code app} dans application.properties).
 * Les valeurs sensibles viennent des variables d'environnement.
 */
@ConfigurationProperties(prefix = "app")
public record AppProperties(
        String timezone,
        String jwtSecret,
        Duration jwtExpiration,
        String jwtIssuer,
        List<String> corsOrigins) {
}
