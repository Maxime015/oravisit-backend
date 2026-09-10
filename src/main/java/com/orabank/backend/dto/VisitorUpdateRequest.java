package com.orabank.backend.dto;

import com.orabank.backend.entity.IdentityDocumentType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Mise a jour d'une fiche visiteur. L'archivage a son propre endpoint. */
public record VisitorUpdateRequest(
        @NotBlank(message = "Le prenom est obligatoire.")
        @Size(max = 60, message = "Le prenom ne peut pas depasser 60 caracteres.")
        String firstName,

        @NotBlank(message = "Le nom est obligatoire.")
        @Size(max = 60, message = "Le nom ne peut pas depasser 60 caracteres.")
        String lastName,

        @Email(message = "L'email est invalide.")
        @Size(max = 120, message = "L'email ne peut pas depasser 120 caracteres.")
        String email,

        @NotBlank(message = "Le telephone est obligatoire.")
        @Size(max = 30, message = "Le telephone ne peut pas depasser 30 caracteres.")
        @Pattern(regexp = "^[0-9+().\\s-]{6,30}$", message = "Le telephone est invalide.")
        String phone,

        @NotNull(message = "Le type de piece est obligatoire.")
        IdentityDocumentType identityDocumentType,

        @NotBlank(message = "Le numero de piece est obligatoire.")
        @Size(max = 40, message = "Le numero de piece ne peut pas depasser 40 caracteres.")
        String identityCardNumber) {
}
