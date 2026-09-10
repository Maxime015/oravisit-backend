package com.orabank.backend.dto;

import com.orabank.backend.entity.Direction;

/** Employe figurant parmi les plus visites de la periode. */
public record TopHostEmployeeResponse(
        Long employeeId,
        String matricule,
        String fullName,
        Direction direction,
        long total) {
}
