package com.orabank.backend.exception;

import java.util.List;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Erreur metier renvoyee au client. Le {@code code} est stable : le frontend
 * s'en sert pour afficher le bon message, il ne doit pas etre renomme.
 */
@Getter
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String code;
    private final transient List<FieldError> errors;

    public ApiException(HttpStatus status, String code, String message, List<FieldError> errors) {
        super(message);
        this.status = status;
        this.code = code;
        this.errors = errors;
    }

    /** Erreur rattachee a un champ, affichee sous le champ concerne. */
    public record FieldError(String field, String message) {
    }

    public static ApiException unauthorized(String code, String message) {
        return new ApiException(HttpStatus.UNAUTHORIZED, code, message, List.of());
    }

    public static ApiException badRequest(String code, String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, code, message, List.of());
    }

    /** 404 : « Visiteur introuvable (id 42). » */
    public static ApiException notFound(String resource, Object id) {
        return new ApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND",
                "%s introuvable (id %s).".formatted(resource, id), List.of());
    }

    /** 409 : conflit d'unicite ou transition d'etat interdite. */
    public static ApiException conflict(String code, String message) {
        return new ApiException(HttpStatus.CONFLICT, code, message, List.of());
    }

    /** 422 : requete bien formee mais contraire a une regle metier. */
    public static ApiException businessRule(String code, String message) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, message, List.of());
    }

    public static ApiException businessRule(String code, String message, String field, String fieldMessage) {
        return new ApiException(HttpStatus.UNPROCESSABLE_ENTITY, code, message,
                List.of(new FieldError(field, fieldMessage)));
    }
}
