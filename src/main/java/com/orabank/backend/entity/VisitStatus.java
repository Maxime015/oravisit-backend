package com.orabank.backend.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Cycle de vie d'une visite : creee EN_COURS, elle devient TERMINEE au
 * check-out ou ANNULEE. Aucune autre transition n'est autorisee.
 */
@Getter
@RequiredArgsConstructor
public enum VisitStatus {

    EN_COURS("En cours"),
    TERMINEE("Terminée"),
    ANNULEE("Annulée");

    /** Libelle utilise dans les exports Excel et les messages d'erreur. */
    private final String label;
}
