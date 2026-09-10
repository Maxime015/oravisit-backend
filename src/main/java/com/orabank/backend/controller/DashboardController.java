package com.orabank.backend.controller;

import com.orabank.backend.dto.DashboardResponse;
import com.orabank.backend.service.DashboardService;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Tableau de bord : vue du jour pour tous, statistiques en plus pour l'ADMIN. */
@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPTION')")
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardResponse load(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return dashboardService.load(from, to);
    }
}
