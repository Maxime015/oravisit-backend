package com.orabank.backend.dto;

/** Regles de mot de passe partagees par les DTOs qui en acceptent un. */
public final class PasswordPolicy {

    public static final int MIN_LENGTH = 8;
    /** BCrypt ne prend en compte que les 72 premiers octets. */
    public static final int MAX_LENGTH = 72;
    public static final String PATTERN = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).+$";
    public static final String MESSAGE =
            "Le mot de passe doit contenir au moins une minuscule, une majuscule et un chiffre.";
    public static final String SIZE_MESSAGE = "Le mot de passe doit contenir entre 8 et 72 caracteres.";

    private PasswordPolicy() {
    }
}
