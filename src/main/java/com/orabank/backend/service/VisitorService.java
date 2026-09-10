package com.orabank.backend.service;

import com.orabank.backend.dto.PageResponse;
import com.orabank.backend.dto.VisitorCreateRequest;
import com.orabank.backend.dto.VisitorResponse;
import com.orabank.backend.dto.VisitorUpdateRequest;
import com.orabank.backend.entity.VisitStatus;
import com.orabank.backend.entity.Visitor;
import com.orabank.backend.exception.ApiException;
import com.orabank.backend.export.ExcelExporter;
import com.orabank.backend.export.ExportedFile;
import com.orabank.backend.repository.VisitRepository;
import com.orabank.backend.repository.VisitorRepository;
import jakarta.persistence.criteria.Predicate;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion des visiteurs : unicite du couple (type, numero) de piece et
 * archivage logique, pour ne jamais perdre l'historique des visites.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class VisitorService {

    private static final List<String> EXPORT_HEADERS = List.of(
            "Nom", "Prénom", "Téléphone", "Email", "Type de pièce", "N° de pièce",
            "Nombre de visites", "Fiche", "Archivée le", "Enregistrée le");

    private final VisitorRepository visitorRepository;
    private final VisitRepository visitRepository;
    private final ExcelExporter excelExporter;
    private final Clock clock;

    @Transactional(readOnly = true)
    public PageResponse<VisitorResponse> search(String q, boolean includeArchived, boolean archivedOnly,
                                                Pageable pageable) {
        Page<Visitor> page = visitorRepository.findAll(filters(q, includeArchived, archivedOnly), pageable);
        Map<Long, Long> visitCounts = countVisits(page.getContent());
        return PageResponse.of(page,
                visitor -> VisitorResponse.from(visitor, visitCounts.getOrDefault(visitor.getId(), 0L)));
    }

    @Transactional(readOnly = true)
    public VisitorResponse getById(Long id) {
        return withVisitCount(require(id));
    }

    @Transactional
    public VisitorResponse create(VisitorCreateRequest request) {
        String documentNumber = request.identityCardNumber().trim();
        if (visitorRepository.existsByIdentityDocumentTypeAndIdentityCardNumberIgnoreCase(
                request.identityDocumentType(), documentNumber)) {
            throw ApiException.conflict("VISITOR_DOCUMENT_ALREADY_USED",
                    "Un visiteur existe deja avec ce type et ce numero de piece.");
        }

        Visitor visitor = visitorRepository.save(Visitor.builder()
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(trimToNull(request.email()))
                .phone(request.phone().trim())
                .identityDocumentType(request.identityDocumentType())
                .identityCardNumber(documentNumber)
                .active(true)
                .build());

        log.info("Visiteur cree : id={}", visitor.getId());
        return VisitorResponse.from(visitor, 0L);
    }

    @Transactional
    public VisitorResponse update(Long id, VisitorUpdateRequest request) {
        Visitor visitor = require(id);
        String documentNumber = request.identityCardNumber().trim();
        if (visitorRepository.existsByIdentityDocumentTypeAndIdentityCardNumberIgnoreCaseAndIdNot(
                request.identityDocumentType(), documentNumber, id)) {
            throw ApiException.conflict("VISITOR_DOCUMENT_ALREADY_USED",
                    "Un visiteur existe deja avec ce type et ce numero de piece.");
        }

        visitor.setFirstName(request.firstName().trim());
        visitor.setLastName(request.lastName().trim());
        visitor.setEmail(trimToNull(request.email()));
        visitor.setPhone(request.phone().trim());
        visitor.setIdentityDocumentType(request.identityDocumentType());
        visitor.setIdentityCardNumber(documentNumber);

        log.info("Visiteur mis a jour : id={}", id);
        return withVisitCount(visitor);
    }

    /** Archivage logique : la fiche et ses visites passees sont conservees. */
    @Transactional
    public void archive(Long id) {
        Visitor visitor = require(id);
        if (!visitor.isActive()) {
            return;
        }
        if (visitRepository.existsByVisitorIdAndStatus(id, VisitStatus.EN_COURS)) {
            throw ApiException.conflict("VISITOR_ALREADY_CHECKED_IN",
                    "Ce visiteur a une visite en cours : effectuez d'abord son check-out.");
        }

        visitor.setActive(false);
        visitor.setArchivedAt(clock.instant());
        log.info("Visiteur archive : id={}", id);
    }

    @Transactional
    public VisitorResponse restore(Long id) {
        Visitor visitor = require(id);
        if (!visitor.isActive()) {
            visitor.setActive(true);
            visitor.setArchivedAt(null);
            log.info("Visiteur desarchive : id={}", id);
        }
        return withVisitCount(visitor);
    }

    @Transactional(readOnly = true)
    public ExportedFile export(String q, boolean includeArchived, boolean archivedOnly) {
        List<Visitor> visitors = visitorRepository.findAll(filters(q, includeArchived, archivedOnly),
                Sort.by("lastName", "firstName"));
        Map<Long, Long> visitCounts = countVisits(visitors);

        List<List<Object>> rows = visitors.stream()
                .map(visitor -> Arrays.<Object>asList(
                        visitor.getLastName(),
                        visitor.getFirstName(),
                        visitor.getPhone(),
                        visitor.getEmail(),
                        visitor.getIdentityDocumentType().getLabel(),
                        visitor.getIdentityCardNumber(),
                        visitCounts.getOrDefault(visitor.getId(), 0L),
                        visitor.isActive() ? "Active" : "Archivée",
                        localDateTime(visitor.getArchivedAt()),
                        localDateTime(visitor.getCreatedAt())))
                .toList();

        log.info("Export Excel de {} visiteur(s)", rows.size());
        return excelExporter.write("visiteurs", EXPORT_HEADERS, rows);
    }

    /** Filtres de la recherche : archivedOnly l'emporte sur includeArchived. */
    private Specification<Visitor> filters(String q, boolean includeArchived, boolean archivedOnly) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("phone")), pattern),
                        cb.like(cb.lower(root.get("identityCardNumber")), pattern)));
            }
            if (archivedOnly) {
                predicates.add(cb.isFalse(root.get("active")));
            } else if (!includeArchived) {
                predicates.add(cb.isTrue(root.get("active")));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Un seul comptage groupe pour toute la page : jamais une requete par ligne. */
    private Map<Long, Long> countVisits(List<Visitor> visitors) {
        if (visitors.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = visitors.stream().map(Visitor::getId).toList();
        return visitRepository.countByVisitorIds(ids).stream()
                .collect(Collectors.toMap(VisitRepository.VisitorCount::getVisitorId,
                        VisitRepository.VisitorCount::getTotal));
    }

    private VisitorResponse withVisitCount(Visitor visitor) {
        return VisitorResponse.from(visitor, countVisits(List.of(visitor)).getOrDefault(visitor.getId(), 0L));
    }

    /** Excel ne connait pas les instants : on affiche l'heure du fuseau applicatif. */
    private LocalDateTime localDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, clock.getZone());
    }

    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Visitor require(Long id) {
        return visitorRepository.findById(id).orElseThrow(() -> ApiException.notFound("Visiteur", id));
    }
}
