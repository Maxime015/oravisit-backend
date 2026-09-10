package com.orabank.backend.service;

import com.orabank.backend.dto.CancelVisitRequest;
import com.orabank.backend.dto.PageResponse;
import com.orabank.backend.dto.VisitCreateRequest;
import com.orabank.backend.dto.VisitResponse;
import com.orabank.backend.entity.Direction;
import com.orabank.backend.entity.Employee;
import com.orabank.backend.entity.Visit;
import com.orabank.backend.entity.VisitStatus;
import com.orabank.backend.entity.Visitor;
import com.orabank.backend.exception.ApiException;
import com.orabank.backend.export.ExcelExporter;
import com.orabank.backend.export.ExportedFile;
import com.orabank.backend.repository.EmployeeRepository;
import com.orabank.backend.repository.VisitRepository;
import com.orabank.backend.repository.VisitorRepository;
import com.orabank.backend.security.CurrentUser;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Regles des visites :
 * un badge et un visiteur ne peuvent avoir qu'une seule visite EN_COURS,
 * la direction est copiee depuis l'employe hote, l'hote doit etre actif et le
 * visiteur non archive, et seule une visite EN_COURS peut etre terminee ou annulee.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitService {

    private static final List<String> EXPORT_HEADERS = List.of(
            "Arrivée", "Sortie", "Durée (min)", "Visiteur", "Téléphone", "N° de pièce",
            "Employé rencontré", "Matricule", "Direction", "Motif", "Badge", "Statut",
            "Motif d'annulation", "Enregistrée par");

    private final VisitRepository visitRepository;
    private final VisitorRepository visitorRepository;
    private final EmployeeRepository employeeRepository;
    private final ExcelExporter excelExporter;
    private final Clock clock;

    @Transactional
    public VisitResponse create(VisitCreateRequest request) {
        Visitor visitor = visitorRepository.findById(request.visitorId())
                .orElseThrow(() -> ApiException.notFound("Visiteur", request.visitorId()));
        Employee host = employeeRepository.findById(request.hostEmployeeId())
                .orElseThrow(() -> ApiException.notFound("Employe", request.hostEmployeeId()));

        if (!visitor.isActive()) {
            throw ApiException.businessRule("VISITOR_ARCHIVED",
                    "Ce visiteur est archive : reactivez sa fiche avant d'enregistrer une visite.");
        }
        if (!host.isActive()) {
            throw ApiException.businessRule("HOST_EMPLOYEE_INACTIVE",
                    "L'employe selectionne est desactive : choisissez un autre hote.");
        }

        String badgeNumber = request.badgeNumber().trim();
        if (visitRepository.existsByBadgeNumberIgnoreCaseAndStatus(badgeNumber, VisitStatus.EN_COURS)) {
            throw ApiException.conflict("BADGE_ALREADY_IN_USE",
                    "Ce numero de badge est deja attribue a une visite en cours.");
        }
        if (visitRepository.existsByVisitorIdAndStatus(visitor.getId(), VisitStatus.EN_COURS)) {
            throw ApiException.conflict("VISITOR_ALREADY_CHECKED_IN", "Ce visiteur a deja une visite en cours.");
        }

        Long currentId = CurrentUser.require().getId();
        Employee registeredBy = employeeRepository.findById(currentId)
                .orElseThrow(() -> ApiException.notFound("Employe", currentId));

        Visit visit = Visit.builder()
                .visitor(visitor)
                .hostEmployee(host)
                .registeredBy(registeredBy)
                .direction(host.getDirection())
                .purpose(request.purpose().trim())
                .badgeNumber(badgeNumber)
                .status(VisitStatus.EN_COURS)
                .checkInAt(clock.instant())
                .build();

        // saveAndFlush : les index uniques de la base s'appliquent des maintenant,
        // ce qui transforme un conflit concurrent en 409 plutot qu'en 500.
        Visit saved = visitRepository.saveAndFlush(visit);
        log.info("Visite enregistree : id={}, visiteur={}, hote={}, badge={}",
                saved.getId(), visitor.getId(), host.getId(), badgeNumber);
        return VisitResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public PageResponse<VisitResponse> search(String q, VisitStatus status, Direction direction, Long hostEmployeeId,
                                              LocalDate dateFrom, LocalDate dateTo, Pageable pageable) {
        Specification<Visit> filters = filters(q, status, direction, hostEmployeeId, dateFrom, dateTo);
        return PageResponse.of(visitRepository.findAll(filters, pageable), VisitResponse::from);
    }

    @Transactional(readOnly = true)
    public PageResponse<VisitResponse> searchByVisitor(Long visitorId, Pageable pageable) {
        if (!visitorRepository.existsById(visitorId)) {
            throw ApiException.notFound("Visiteur", visitorId);
        }
        return PageResponse.of(visitRepository.findByVisitorId(visitorId, pageable), VisitResponse::from);
    }

    @Transactional(readOnly = true)
    public VisitResponse getById(Long id) {
        return VisitResponse.from(require(id));
    }

    @Transactional
    public VisitResponse checkout(Long id) {
        Visit visit = require(id);
        requireInProgress(visit, "Seule une visite en cours peut faire l'objet d'un check-out.");

        // La base interdit une sortie anterieure a l'arrivee (horloge remise a l'heure).
        Instant now = clock.instant();
        visit.setCheckOutAt(now.isBefore(visit.getCheckInAt()) ? visit.getCheckInAt() : now);
        visit.setStatus(VisitStatus.TERMINEE);

        log.info("Check-out effectue : visite={}, badge={}", id, visit.getBadgeNumber());
        return VisitResponse.from(visit);
    }

    @Transactional
    public VisitResponse cancel(Long id, CancelVisitRequest request) {
        Visit visit = require(id);
        requireInProgress(visit, "Seule une visite en cours peut etre annulee.");

        visit.setStatus(VisitStatus.ANNULEE);
        visit.setCancellationReason(request.reason().trim());

        log.info("Visite annulee : id={}, badge={}", id, visit.getBadgeNumber());
        return VisitResponse.from(visit);
    }

    @Transactional(readOnly = true)
    public ExportedFile export(String q, VisitStatus status, Direction direction, Long hostEmployeeId,
                               LocalDate dateFrom, LocalDate dateTo) {
        List<Visit> visits = visitRepository.findAll(
                filters(q, status, direction, hostEmployeeId, dateFrom, dateTo),
                Sort.by(Sort.Direction.DESC, "checkInAt"));

        List<List<Object>> rows = visits.stream()
                .map(visit -> Arrays.<Object>asList(
                        localDateTime(visit.getCheckInAt()),
                        localDateTime(visit.getCheckOutAt()),
                        durationMinutes(visit),
                        visit.getVisitor().getFullName(),
                        visit.getVisitor().getPhone(),
                        visit.getVisitor().getIdentityCardNumber(),
                        visit.getHostEmployee().getFullName(),
                        visit.getHostEmployee().getMatricule(),
                        visit.getDirection().getLabel(),
                        visit.getPurpose(),
                        visit.getBadgeNumber(),
                        visit.getStatus().getLabel(),
                        visit.getCancellationReason(),
                        visit.getRegisteredBy().getFullName()))
                .toList();

        log.info("Export Excel de {} visite(s)", rows.size());
        return excelExporter.write("visites", EXPORT_HEADERS, rows);
    }

    /** Filtres partages par la liste paginee et l'export. */
    private Specification<Visit> filters(String q, VisitStatus status, Direction direction, Long hostEmployeeId,
                                         LocalDate dateFrom, LocalDate dateTo) {
        if (dateFrom != null && dateTo != null && dateFrom.isAfter(dateTo)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE", "La date de debut doit preceder la date de fin.");
        }
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                var visitor = root.join("visitor", JoinType.INNER);
                var host = root.join("hostEmployee", JoinType.INNER);
                predicates.add(cb.or(
                        cb.like(cb.lower(visitor.get("firstName")), pattern),
                        cb.like(cb.lower(visitor.get("lastName")), pattern),
                        cb.like(cb.lower(visitor.get("phone")), pattern),
                        cb.like(cb.lower(visitor.get("identityCardNumber")), pattern),
                        cb.like(cb.lower(host.get("firstName")), pattern),
                        cb.like(cb.lower(host.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("badgeNumber")), pattern),
                        cb.like(cb.lower(root.get("purpose")), pattern)));
            }
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (direction != null) {
                predicates.add(cb.equal(root.get("direction"), direction));
            }
            if (hostEmployeeId != null) {
                predicates.add(cb.equal(root.get("hostEmployee").get("id"), hostEmployeeId));
            }
            // Bornes de dates incluses, exprimees dans le fuseau applicatif.
            if (dateFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("checkInAt"), startOfDay(dateFrom)));
            }
            if (dateTo != null) {
                predicates.add(cb.lessThan(root.get("checkInAt"), startOfDay(dateTo.plusDays(1))));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void requireInProgress(Visit visit, String message) {
        if (visit.getStatus() != VisitStatus.EN_COURS) {
            throw ApiException.conflict("VISIT_NOT_ACTIVE",
                    "%s (statut actuel : %s).".formatted(message, visit.getStatus().getLabel()));
        }
    }

    private Visit require(Long id) {
        return visitRepository.findWithDetailsById(id).orElseThrow(() -> ApiException.notFound("Visite", id));
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(clock.getZone()).toInstant();
    }

    private LocalDateTime localDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, clock.getZone());
    }

    private Long durationMinutes(Visit visit) {
        if (visit.getCheckOutAt() == null) {
            return null;
        }
        return Duration.between(visit.getCheckInAt(), visit.getCheckOutAt()).toMinutes();
    }
}
