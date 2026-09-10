package com.orabank.backend.security;

import com.orabank.backend.entity.Employee;
import com.orabank.backend.entity.Role;
import com.orabank.backend.exception.ApiException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Employe connecte, lu dans le contexte de securite. C'est la seule source de
 * l'agent enregistreur d'une visite : jamais le corps de la requete.
 */
public final class CurrentUser {

    private CurrentUser() {
    }

    public static Employee require() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof AppUserDetails user) {
            return user.employee();
        }
        throw ApiException.unauthorized("UNAUTHENTICATED", "Authentification requise.");
    }

    public static boolean isAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null
                && authentication.getPrincipal() instanceof AppUserDetails user
                && user.employee().getRole() == Role.ADMIN;
    }
}
