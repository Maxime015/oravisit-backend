package com.orabank.backend.service;

import com.orabank.backend.dto.AuthUserResponse;
import com.orabank.backend.dto.ChangePasswordRequest;
import com.orabank.backend.dto.LoginRequest;
import com.orabank.backend.dto.LoginResponse;
import com.orabank.backend.entity.Employee;
import com.orabank.backend.exception.ApiException;
import com.orabank.backend.repository.EmployeeRepository;
import com.orabank.backend.security.CurrentUser;
import com.orabank.backend.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Connexion par matricule et gestion de son propre mot de passe. */
@Slf4j
@Service
@RequiredArgsConstructor
public class AuthService {

    private final EmployeeRepository employeeRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional(readOnly = true)
    public LoginResponse login(LoginRequest request) {
        Employee employee = employeeRepository.findByMatriculeIgnoreCase(request.matricule().trim())
                .filter(Employee::isActive)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                // Meme message pour un compte inconnu, desactive ou un mauvais mot de passe.
                .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS",
                        "Matricule ou mot de passe incorrect."));

        log.info("Connexion reussie : matricule={}, role={}", employee.getMatricule(), employee.getRole());
        return new LoginResponse(jwtService.generateToken(employee), "Bearer",
                jwtService.expiresInSeconds(), AuthUserResponse.from(employee));
    }

    /** L'employe a ete relu en base par le filtre JWT : il est a jour. */
    public AuthUserResponse currentUser() {
        return AuthUserResponse.from(CurrentUser.require());
    }

    @Transactional
    public void changeOwnPassword(ChangePasswordRequest request) {
        Long id = CurrentUser.require().getId();
        Employee employee = employeeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Employe", id));

        if (!passwordEncoder.matches(request.currentPassword(), employee.getPasswordHash())) {
            throw ApiException.businessRule("INVALID_CURRENT_PASSWORD",
                    "Le mot de passe actuel est incorrect.", "currentPassword", "Mot de passe incorrect.");
        }
        if (passwordEncoder.matches(request.newPassword(), employee.getPasswordHash())) {
            throw ApiException.businessRule("VALIDATION_ERROR",
                    "Le nouveau mot de passe doit etre different de l'ancien.",
                    "newPassword", "Choisissez un mot de passe different.");
        }

        employee.setPasswordHash(passwordEncoder.encode(request.newPassword()));
        log.info("Mot de passe modifie par l'employe {}", employee.getMatricule());
    }
}
