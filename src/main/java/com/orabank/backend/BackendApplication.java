package com.orabank.backend;

import com.orabank.backend.config.AppProperties;
import java.time.Clock;
import java.time.ZoneId;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/** OraVisit : API de gestion des visiteurs d'Orabank Togo. */
@SpringBootApplication
@EnableJpaAuditing
@EnableConfigurationProperties(AppProperties.class)
public class BackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(BackendApplication.class, args);
    }

    /**
     * Horloge du fuseau metier (Africa/Lome). Les services l'injectent au lieu
     * d'appeler Instant.now() : les calculs de dates restent testables.
     */
    @Bean
    public Clock clock(AppProperties properties) {
        return Clock.system(ZoneId.of(properties.timezone()));
    }
}
