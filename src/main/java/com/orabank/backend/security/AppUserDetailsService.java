package com.orabank.backend.security;

import com.orabank.backend.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Chargement de l'utilisateur a partir de son matricule. */
@Service
@RequiredArgsConstructor
public class AppUserDetailsService implements UserDetailsService {

    private final EmployeeRepository employeeRepository;

    @Override
    @Transactional(readOnly = true)
    public AppUserDetails loadUserByUsername(String matricule) {
        return employeeRepository.findByMatriculeIgnoreCase(matricule)
                .map(AppUserDetails::new)
                // Message neutre : ne revele pas si le compte existe.
                .orElseThrow(() -> new UsernameNotFoundException("Identifiants invalides."));
    }
}
