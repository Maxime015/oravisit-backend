package com.orabank.backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Annulation d'une visite en cours : le motif est obligatoire. */
public record CancelVisitRequest(
        @NotBlank(message = "Le motif d'annulation est obligatoire.")
        @Size(max = 255, message = "Le motif d'annulation ne peut pas depasser 255 caracteres.")
        String reason) {
}
