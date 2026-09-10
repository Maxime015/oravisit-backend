package com.orabank.backend.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/** Pieces d'identite acceptees a l'accueil. */
@Getter
@RequiredArgsConstructor
public enum IdentityDocumentType {

    CNI("Carte nationale d'identité"),
    PASSEPORT("Passeport"),
    PERMIS("Permis de conduire"),
    CARTE_CONSULAIRE("Carte consulaire"),
    AUTRE("Autre");

    private final String label;
}
