package com.orabank.backend.controller;

import com.orabank.backend.dto.EmployeeCreateRequest;
import com.orabank.backend.dto.EmployeeLookupResponse;
import com.orabank.backend.dto.EmployeeResponse;
import com.orabank.backend.dto.EmployeeUpdateRequest;
import com.orabank.backend.dto.PageResponse;
import com.orabank.backend.dto.ResetPasswordRequest;
import com.orabank.backend.entity.Direction;
import com.orabank.backend.entity.Role;
import com.orabank.backend.service.EmployeeService;
import jakarta.validation.Valid;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
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

/**
 * Comptes du personnel : reserve a l'ADMIN, sauf l'autocompletion
 * {@code /lookup} qui sert aussi a l'agent d'accueil.
 */
@RestController
@RequestMapping("/api/v1/employees")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class EmployeeController {

    private final EmployeeService employeeService;

    @GetMapping
    public PageResponse<EmployeeResponse> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) Role role,
            @RequestParam(required = false) Direction direction,
            @RequestParam(required = false) Boolean isActive,
            @PageableDefault(size = 20, sort = {"lastName", "firstName"}) Pageable pageable) {
        return employeeService.search(q, role, direction, isActive, pageable);
    }

    /** Employes actifs correspondants, pour choisir l'hote d'une visite. */
    @GetMapping("/lookup")
    @PreAuthorize("hasAnyRole('ADMIN', 'RECEPTION')")
    public List<EmployeeLookupResponse> lookup(@RequestParam(required = false) String q,
                                               @RequestParam(required = false) Direction direction) {
        return employeeService.lookup(q, direction);
    }

    @GetMapping("/{id}")
    public EmployeeResponse getById(@PathVariable Long id) {
        return employeeService.getById(id);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EmployeeResponse create(@Valid @RequestBody EmployeeCreateRequest request) {
        return employeeService.create(request);
    }

    @PutMapping("/{id}")
    public EmployeeResponse update(@PathVariable Long id, @Valid @RequestBody EmployeeUpdateRequest request) {
        return employeeService.update(id, request);
    }

    @PatchMapping("/{id}/activate")
    public EmployeeResponse activate(@PathVariable Long id) {
        return employeeService.activate(id);
    }

    /** Desactivation logique : ni son propre compte, ni le dernier ADMIN actif. */
    @PatchMapping("/{id}/deactivate")
    public EmployeeResponse deactivate(@PathVariable Long id) {
        return employeeService.deactivate(id);
    }

    @PatchMapping("/{id}/password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void resetPassword(@PathVariable Long id, @Valid @RequestBody ResetPasswordRequest request) {
        employeeService.resetPassword(id, request);
    }
}
