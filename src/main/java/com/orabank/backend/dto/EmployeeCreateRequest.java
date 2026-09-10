package com.orabank.backend.dto;

import com.orabank.backend.entity.Direction;
import com.orabank.backend.entity.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Creation d'un compte employe (ADMIN uniquement : il n'y a pas d'inscription publique). */
public record EmployeeCreateRequest(
        @NotBlank(message = "Le matricule est obligatoire.")
        @Size(max = 20, message = "Le matricule ne peut pas depasser 20 caracteres.")
        String matricule,

        @NotBlank(message = "Le prenom est obligatoire.")
        @Size(max = 60, message = "Le prenom ne peut pas depasser 60 caracteres.")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 60, message = "Le nom ne peut pas depasser 60 caracteres.")
        String lastName,

        @NotBlank(message = "L'email est obligatoire.")
        @Email(message = "L'email est invalide.")
        @Size(max = 120, message = "L'email ne peut pas depasser 120 caracteres.")
        String email,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
                message = PasswordPolicy.SIZE_MESSAGE)
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
        String password,

        @Size(max = 30, message = "Le telephone ne peut pas depasser 30 caracteres.")
        @Pattern(regexp = "^[0-9+().\\s-]{6,30}$", message = "Le telephone est invalide.")
        String phone,

        @NotNull(message = "Le role est obligatoire.")
        Role role,

        @NotNull(message = "La direction est obligatoire.")
        Direction direction) {

    @Override
    public String toString() {
        return "EmployeeCreateRequest(matricule=%s, role=%s)".formatted(matricule, role);
    }
}
