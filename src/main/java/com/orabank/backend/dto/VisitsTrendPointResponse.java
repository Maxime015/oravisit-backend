package com.orabank.backend.dto;

import java.time.LocalDate;

/** Point de la courbe d'evolution : le nombre de visites d'une journee. */
public record VisitsTrendPointResponse(LocalDate date, long total) {
}
