package com.orabank.backend.dto;

/** Jeton d'acces et profil renvoyes a la connexion. */
public record LoginResponse(String accessToken, String tokenType, long expiresIn, AuthUserResponse user) {
}
