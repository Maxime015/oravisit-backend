package com.orabank.backend.exception;

import com.orabank.backend.exception.ApiException.FieldError;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mapping.PropertyReferenceException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/**
 * Traduit toutes les exceptions en une reponse JSON uniforme
 * ({@code status}, {@code detail}, {@code code}, {@code errors}).
 * Aucune trace technique ne sort du serveur.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ProblemDetail handleApiException(ApiException exception, HttpServletRequest request) {
        log.info("Erreur metier {} sur {} : {}", exception.getCode(), request.getRequestURI(), exception.getMessage());
        return problem(exception.getStatus(), exception.getCode(), exception.getMessage(), exception.getErrors());
    }

    /** Champs invalides d'un corps de requete (@Valid). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException exception) {
        List<FieldError> errors = exception.getBindingResult().getFieldErrors().stream()
                .map(error -> new FieldError(error.getField(), error.getDefaultMessage()))
                .sorted((a, b) -> a.field().compareTo(b.field()))
                .toList();
        return problem(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Certains champs sont invalides.", errors);
    }

    /** JSON illisible, mauvais type ou champ non prevu par le DTO. */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail handleUnreadableBody() {
        return problem(HttpStatus.BAD_REQUEST, "MALFORMED_REQUEST",
                "Le corps de la requete est invalide ou contient des champs non autorises.", List.of());
    }

    /** Parametre de requete du mauvais type (statut, direction, date...). */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ProblemDetail handleTypeMismatch(MethodArgumentTypeMismatchException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "La valeur du parametre « %s » est invalide.".formatted(exception.getName()),
                List.of(new FieldError(exception.getName(), "Valeur non reconnue.")));
    }

    /** Tri demande sur une propriete inexistante (parametre sort). */
    @ExceptionHandler(PropertyReferenceException.class)
    public ProblemDetail handleUnknownSort(PropertyReferenceException exception) {
        return problem(HttpStatus.BAD_REQUEST, "INVALID_PARAMETER",
                "Le tri demande sur « %s » n'est pas supporte.".formatted(exception.getPropertyName()),
                List.of(new FieldError("sort", "Propriete de tri inconnue.")));
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(HttpServletRequest request) {
        log.info("Acces refuse sur {}", request.getRequestURI());
        return problem(HttpStatus.FORBIDDEN, "ACCESS_DENIED",
                "Vous n'etes pas autorise a effectuer cette action.", List.of());
    }

    /**
     * Filet de securite des contraintes d'unicite : les index de la base
     * protegent des acces concurrents meme si la verification applicative est passee.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ProblemDetail handleDataIntegrity(DataIntegrityViolationException exception, HttpServletRequest request) {
        String cause = String.valueOf(exception.getMostSpecificCause().getMessage()).toLowerCase();
        log.warn("Violation de contrainte sur {} : {}", request.getRequestURI(), cause);

        if (cause.contains("ux_visits_badge_active")) {
            return problem(HttpStatus.CONFLICT, "BADGE_ALREADY_IN_USE",
                    "Ce numero de badge est deja attribue a une visite en cours.", List.of());
        }
        if (cause.contains("ux_visits_visitor_active")) {
            return problem(HttpStatus.CONFLICT, "VISITOR_ALREADY_CHECKED_IN",
                    "Ce visiteur a deja une visite en cours.", List.of());
        }
        if (cause.contains("uk_visitors_identity_document")) {
            return problem(HttpStatus.CONFLICT, "VISITOR_DOCUMENT_ALREADY_USED",
                    "Un visiteur existe deja avec ce type et ce numero de piece.", List.of());
        }
        if (cause.contains("uk_employees_matricule")) {
            return problem(HttpStatus.CONFLICT, "MATRICULE_ALREADY_USED",
                    "Ce matricule est deja utilise.", List.of());
        }
        if (cause.contains("uk_employees_email")) {
            return problem(HttpStatus.CONFLICT, "EMAIL_ALREADY_USED",
                    "Cette adresse email est deja utilisee.", List.of());
        }
        return problem(HttpStatus.CONFLICT, "DATA_INTEGRITY_VIOLATION",
                "L'operation entre en conflit avec des donnees existantes.", List.of());
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleUnexpected(Exception exception, HttpServletRequest request) {
        log.error("Erreur inattendue sur {}", request.getRequestURI(), exception);
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_ERROR",
                "Une erreur interne est survenue. Veuillez reessayer.", List.of());
    }

    private ProblemDetail problem(HttpStatus status, String code, String detail, List<FieldError> errors) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, detail);
        problem.setProperty("code", code);
        if (!errors.isEmpty()) {
            problem.setProperty("errors", errors);
        }
        return problem;
    }
}
