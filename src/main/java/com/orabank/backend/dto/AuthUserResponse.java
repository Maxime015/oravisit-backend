package com.orabank.backend.dto;

import com.orabank.backend.entity.Direction;
import com.orabank.backend.entity.Employee;
import com.orabank.backend.entity.Role;

/** Profil de l'utilisateur connecte. */
public record AuthUserResponse(
        Long id,
        String matricule,
        String firstName,
        String lastName,
        String fullName,
        String email,
        Role role,
        Direction direction) {

    public static AuthUserResponse from(Employee employee) {
        return new AuthUserResponse(
                employee.getId(),
                employee.getMatricule(),
                employee.getFirstName(),
                employee.getLastName(),
                employee.getFullName(),
                employee.getEmail(),
                employee.getRole(),
                employee.getDirection());
    }
}
