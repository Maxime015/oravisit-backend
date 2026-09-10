package com.orabank.backend.dto;

import com.orabank.backend.entity.Direction;
import com.orabank.backend.entity.Employee;

/** Vue allegee pour l'autocompletion du choix de l'hote d'une visite. */
public record EmployeeLookupResponse(Long id, String matricule, String fullName, Direction direction) {

    public static EmployeeLookupResponse from(Employee employee) {
        return new EmployeeLookupResponse(
                employee.getId(),
                employee.getMatricule(),
                employee.getFullName(),
                employee.getDirection());
    }
}
