package com.orabank.backend.entity;

/**
 * Roles applicatifs, un seul par employe.
 * ADMIN : tout gerer. RECEPTION : agent d'accueil (visiteurs, visites, dashboard).
 * DEFAULT : employe visitable, sans acces a l'application.
 */
public enum Role {
    ADMIN,
    RECEPTION,
    DEFAULT
}
