package com.orabank.backend.dto;

import com.orabank.backend.entity.Direction;
import com.orabank.backend.entity.Employee;

/** Employe resume, imbrique dans les reponses de visite. */
public record EmployeeSummaryResponse(
        Long id,
        String matricule,
        String fullName,
        Direction direction,
        boolean active) {

    public static EmployeeSummaryResponse from(Employee employee) {
        return new EmployeeSummaryResponse(
                employee.getId(),
                employee.getMatricule(),
                employee.getFullName(),
                employee.getDirection(),
                employee.isActive());
    }
}
