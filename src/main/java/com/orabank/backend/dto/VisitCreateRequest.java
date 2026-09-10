package com.orabank.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Enregistrement d'une visite. Ni le statut, ni les horodatages, ni la
 * direction, ni l'agent enregistreur n'y figurent : le serveur les calcule.
 */
public record VisitCreateRequest(
        @NotNull(message = "Le visiteur est obligatoire.")
        @Positive(message = "Le visiteur est invalide.")
        Long visitorId,

        @NotNull(message = "L'employe rencontre est obligatoire.")
        @Positive(message = "L'employe rencontre est invalide.")
        Long hostEmployeeId,

        @NotBlank(message = "Le motif est obligatoire.")
        @Size(max = 255, message = "Le motif ne peut pas depasser 255 caracteres.")
        String purpose,

        @NotBlank(message = "Le numero de badge est obligatoire.")
        @Size(max = 20, message = "Le numero de badge ne peut pas depasser 20 caracteres.")
        String badgeNumber) {
}
