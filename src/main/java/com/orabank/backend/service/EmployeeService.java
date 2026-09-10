package com.orabank.backend.service;

import com.orabank.backend.dto.EmployeeCreateRequest;
import com.orabank.backend.dto.EmployeeLookupResponse;
import com.orabank.backend.dto.EmployeeResponse;
import com.orabank.backend.dto.EmployeeUpdateRequest;
import com.orabank.backend.dto.PageResponse;
import com.orabank.backend.dto.ResetPasswordRequest;
import com.orabank.backend.entity.Direction;
import com.orabank.backend.entity.Employee;
import com.orabank.backend.entity.Role;
import com.orabank.backend.exception.ApiException;
import com.orabank.backend.repository.EmployeeRepository;
import com.orabank.backend.security.CurrentUser;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gestion des employes : matricule et email uniques, desactivation logique
 * (jamais de suppression), et il reste toujours au moins un ADMIN actif.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployeeService {

    /** L'autocompletion de l'accueil n'affiche jamais plus de 20 suggestions. */
    private static final int LOOKUP_LIMIT = 20;

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public PageResponse<EmployeeResponse> search(String q, Role role, Direction direction,
                                                 Boolean active, Pageable pageable) {
        return PageResponse.of(employeeRepository.findAll(filters(q, role, direction, active), pageable),
                EmployeeResponse::from);
    }

    @Transactional(readOnly = true)
    public List<EmployeeLookupResponse> lookup(String q, Direction direction) {
        Pageable pageable = PageRequest.of(0, LOOKUP_LIMIT, Sort.by("lastName", "firstName"));
        return employeeRepository.findAll(filters(q, null, direction, true), pageable)
                .map(EmployeeLookupResponse::from)
                .getContent();
    }

    @Transactional(readOnly = true)
    public EmployeeResponse getById(Long id) {
        return EmployeeResponse.from(require(id));
    }

    @Transactional
    public EmployeeResponse create(EmployeeCreateRequest request) {
        String matricule = request.matricule().trim();
        String email = request.email().trim();
        if (employeeRepository.existsByMatriculeIgnoreCase(matricule)) {
            throw ApiException.conflict("MATRICULE_ALREADY_USED", "Ce matricule est deja utilise.");
        }
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_ALREADY_USED", "Cette adresse email est deja utilisee.");
        }

        Employee employee = employeeRepository.save(Employee.builder()
                .matricule(matricule)
                .firstName(request.firstName().trim())
                .lastName(request.lastName().trim())
                .email(email)
                .phone(trimToNull(request.phone()))
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .direction(request.direction())
                .active(true)
                .build());

        log.info("Employe cree : id={}, matricule={}, role={}",
                employee.getId(), employee.getMatricule(), employee.getRole());
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse update(Long id, EmployeeUpdateRequest request) {
        Employee employee = require(id);
        String matricule = request.matricule().trim();
        String email = request.email().trim();

        if (employeeRepository.existsByMatriculeIgnoreCaseAndIdNot(matricule, id)) {
            throw ApiException.conflict("MATRICULE_ALREADY_USED", "Ce matricule est deja utilise.");
        }
        if (employeeRepository.existsByEmailIgnoreCaseAndIdNot(email, id)) {
            throw ApiException.conflict("EMAIL_ALREADY_USED", "Cette adresse email est deja utilisee.");
        }
        if (employee.getRole() == Role.ADMIN && request.role() != Role.ADMIN) {
            requireAnotherActiveAdmin(employee,
                    "Impossible de retirer le role administrateur : c'est le dernier administrateur actif.");
        }

        employee.setMatricule(matricule);
        employee.setFirstName(request.firstName().trim());
        employee.setLastName(request.lastName().trim());
        employee.setEmail(email);
        employee.setPhone(trimToNull(request.phone()));
        employee.setRole(request.role());
        employee.setDirection(request.direction());

        log.info("Employe mis a jour : id={}, matricule={}", employee.getId(), employee.getMatricule());
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse activate(Long id) {
        Employee employee = require(id);
        employee.setActive(true);
        log.info("Employe active : id={}", id);
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public EmployeeResponse deactivate(Long id) {
        Employee employee = require(id);
        if (employee.getId().equals(CurrentUser.require().getId())) {
            throw ApiException.conflict("CANNOT_DEACTIVATE_SELF",
                    "Vous ne pouvez pas desactiver votre propre compte.");
        }
        if (employee.getRole() == Role.ADMIN && employee.isActive()) {
            requireAnotherActiveAdmin(employee, "Impossible de desactiver le dernier administrateur actif.");
        }

        employee.setActive(false);
        log.info("Employe desactive : id={}", id);
        return EmployeeResponse.from(employee);
    }

    @Transactional
    public void resetPassword(Long id, ResetPasswordRequest request) {
        Employee employee = require(id);
        employee.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        log.info("Mot de passe reinitialise pour l'employe {}", employee.getMatricule());
    }

    /** Filtres de la recherche : seuls les criteres fournis sont appliques. */
    private Specification<Employee> filters(String q, Role role, Direction direction, Boolean active) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("matricule")), pattern),
                        cb.like(cb.lower(root.get("firstName")), pattern),
                        cb.like(cb.lower(root.get("lastName")), pattern),
                        cb.like(cb.lower(root.get("email")), pattern)));
            }
            if (role != null) {
                predicates.add(cb.equal(root.get("role"), role));
            }
            if (direction != null) {
                predicates.add(cb.equal(root.get("direction"), direction));
            }
            if (active != null) {
                predicates.add(cb.equal(root.get("active"), active));
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /** Un telephone vide est stocke a null plutot qu'en chaine vide. */
    private String trimToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private Employee require(Long id) {
        return employeeRepository.findById(id).orElseThrow(() -> ApiException.notFound("Employe", id));
    }

    /** Verifie qu'un autre ADMIN actif subsistera apres l'operation. */
    private void requireAnotherActiveAdmin(Employee employee, String message) {
        long activeAdmins = employeeRepository.countByRoleAndActiveIsTrue(Role.ADMIN);
        if (employee.getRole() == Role.ADMIN && employee.isActive()) {
            activeAdmins--;
        }
        if (activeAdmins < 1) {
            throw ApiException.conflict("LAST_ACTIVE_ADMIN", message);
        }
    }
}
