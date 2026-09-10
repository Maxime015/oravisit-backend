package com.orabank.backend.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Indicateurs du tableau de bord. Les champs de la vue analytique (a partir de
 * {@code totalVisitors}) ne sont remplis que pour un ADMIN ; ils sont nuls, donc
 * absents du JSON, pour un agent d'accueil.
 */
public record DashboardResponse(
        LocalDate from,
        LocalDate to,

        // Vue operationnelle (RECEPTION et ADMIN).
        long visitsToday,
        /* Visites en cours = visiteurs presents = badges en circulation. */
        long activeVisits,
        long completedToday,
        long cancelledToday,
        double averageVisitDurationMinutes,
        List<VisitResponse> currentVisitors,

        // Vue analytique (ADMIN uniquement).
        Long totalVisitors,
        Long archivedVisitors,
        List<DirectionCountResponse> visitsByDirection,
        List<VisitsTrendPointResponse> visitsTrend,
        List<TopHostEmployeeResponse> topHostEmployees) {
}
