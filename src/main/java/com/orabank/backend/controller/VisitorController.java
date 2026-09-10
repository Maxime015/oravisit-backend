package com.orabank.backend.controller;

import com.orabank.backend.dto.PageResponse;
import com.orabank.backend.dto.VisitResponse;
import com.orabank.backend.dto.VisitorCreateRequest;
import com.orabank.backend.dto.VisitorResponse;
import com.orabank.backend.dto.VisitorUpdateRequest;
import com.orabank.backend.export.ExportedFile;
import com.orabank.backend.service.VisitService;
import com.orabank.backend.service.VisitorService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** Fiches des visiteurs et leur historique de visites. */
@RestController
@RequestMapping("/api/v1/visitors")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'RECEPTION')")
public class VisitorController {

    private final VisitorService visitorService;
    private final VisitService visitService;

    /**
     * Recherche sur nom, prenom, telephone et numero de piece.
     * Les fiches archivees sont exclues par defaut.
     */
    @GetMapping
    public PageResponse<VisitorResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(defaultValue = "false") boolean includeArchived,
            @RequestParam(defaultValue = "false") boolean archivedOnly,
            @PageableDefault(size = 20, sort = {"lastName", "firstName"}) Pageable pageable) {
        return visitorService.search(q, includeArchived, archivedOnly, pageable);
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam(required = false) String q,
                                         @RequestParam(defaultValue = "false") boolean includeArchived,
                                         @RequestParam(defaultValue = "false") boolean archivedOnly) {
        ExportedFile file = visitorService.export(q, includeArchived, archivedOnly);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(ExportedFile.CONTENT_TYPE))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment().filename(file.filename()).build().toString())
                .body(file.content());
    }

    @GetMapping("/{id}")
    public VisitorResponse getById(@PathVariable Long id) {
        return visitorService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public VisitorResponse create(@Valid @RequestBody VisitorCreateRequest request) {
        return visitorService.create(request);
    }

    @PutMapping("/{id}")
    public VisitorResponse update(@PathVariable Long id, @Valid @RequestBody VisitorUpdateRequest request) {
        return visitorService.update(id, request);
    }

    /** Archivage logique : la fiche est conservee avec son historique. */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void archive(@PathVariable Long id) {
        visitorService.archive(id);
    }

    @PatchMapping("/{id}/restore")
    public VisitorResponse restore(@PathVariable Long id) {
        return visitorService.restore(id);
    }

    @GetMapping("/{id}/visits")
    public PageResponse<VisitResponse> visits(
            @PathVariable Long id,
            @PageableDefault(size = 20, sort = "checkInAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return visitService.searchByVisitor(id, pageable);
    }
}
