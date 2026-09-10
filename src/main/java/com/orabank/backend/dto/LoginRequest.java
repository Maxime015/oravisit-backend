package com.orabank.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Identifiants de connexion. */
public record LoginRequest(
        @NotBlank(message = "Le matricule est obligatoire.")
        @Size(max = 20, message = "Le matricule ne peut pas depasser 20 caracteres.")
        String matricule,

        @NotBlank(message = "Le mot de passe est obligatoire.")
        String password) {

    /** Evite d'ecrire le mot de passe dans les logs. */
    @Override
    public String toString() {
        return "LoginRequest(matricule=%s)".formatted(matricule);
    }
}
