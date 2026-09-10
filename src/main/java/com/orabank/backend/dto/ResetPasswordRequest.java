package com.orabank.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Reinitialisation du mot de passe d'un employe par un ADMIN. */
public record ResetPasswordRequest(
        @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
                message = PasswordPolicy.SIZE_MESSAGE)
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
        String newPassword) {

    @Override
    public String toString() {
        return "ResetPasswordRequest(****)";
    }
}
