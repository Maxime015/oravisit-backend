package com.orabank.backend.controller;

import com.orabank.backend.dto.CancelVisitRequest;
import com.orabank.backend.dto.PageResponse;
import com.orabank.backend.dto.VisitCreateRequest;
import com.orabank.backend.dto.VisitResponse;
import com.orabank.backend.entity.Direction;
import com.orabank.backend.entity.VisitStatus;
import com.orabank.backend.export.ExportedFile;
import com.orabank.backend.service.VisitService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Enregistrement et cycle de vie des visites. Le statut, les horodatages,
 * la direction et l'agent enregistreur sont calcules par le serveur.
 */
@RestController
@RequestMapping("/api/v1/visits")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPTION')")
public class VisitController {

    private final VisitService visitService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitResponse create(@Valid @RequestBody VisitCreateRequest request) {
        return visitService.create(request);
    }

    /** Recherche sur le visiteur, l'hote, le badge et le motif ; bornes de dates incluses. */
    @GetMapping
    public PageResponse<VisitResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) VisitStatus status,
            @RequestParam(required = false) Direction direction,
            @RequestParam(required = false) Long hostEmployeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo,
            @PageableDefault(size = 20, sort = "checkInAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return visitService.search(q, status, direction, hostEmployeeId, dateFrom, dateTo, pageable);
    }

    /** Meme filtres que la liste, renvoyes sous forme de classeur .xlsx. */
    @GetMapping("/export")
    public ResponseEntity<byte[]> export(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) VisitStatus status,
            @RequestParam(required = false) Direction direction,
            @RequestParam(required = false) Long hostEmployeeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateTo) {
        ExportedFile file = visitService.export(q, status, direction, hostEmployeeId, dateFrom, dateTo);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ExportedFile.CONTENT_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename()).build().toString())
                .body(file.content());
    }

    @GetMapping("/{id}")
    public VisitResponse getById(@PathVariable Long id) {
        return visitService.getById(id);
    }

    /** EN_COURS vers TERMINEE : l'heure de sortie est fixee par le serveur. */
    @PatchMapping("/{id}/checkout")
    public VisitResponse checkout(@PathVariable Long id) {
        return visitService.checkout(id);
    }

    /** EN_COURS vers ANNULEE, avec motif obligatoire. */
    @PatchMapping("/{id}/cancel")
    public VisitResponse cancel(@PathVariable Long id, @Valid @RequestBody CancelVisitRequest request) {
        return visitService.cancel(id, request);
    }
}
