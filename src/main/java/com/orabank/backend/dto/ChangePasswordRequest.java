package com.orabank.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Changement de son propre mot de passe. */
public record ChangePasswordRequest(
        @NotBlank(message = "Le mot de passe actuel est obligatoire.")
        String currentPassword,

        @NotBlank(message = "Le nouveau mot de passe est obligatoire.")
        @Size(min = PasswordPolicy.MIN_LENGTH, max = PasswordPolicy.MAX_LENGTH,
                message = PasswordPolicy.SIZE_MESSAGE)
        @Pattern(regexp = PasswordPolicy.PATTERN, message = PasswordPolicy.MESSAGE)
        String newPassword) {

    @Override
    public String toString() {
        return "ChangePasswordRequest(****)";
    }
}
