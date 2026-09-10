package com.orabank.backend.security;

import com.orabank.backend.entity.Employee;
import java.util.Collection;
import java.util.List;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/** Utilisateur authentifie : un employe et son role (ROLE_ADMIN, ROLE_RECEPTION, ROLE_DEFAULT). */
public record AppUserDetails(Employee employee) implements UserDetails {

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + employee.getRole().name()));
    }

    @Override
    public String getPassword() {
        return employee.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return employee.getMatricule();
    }

    /** Un compte desactive ne peut plus s'authentifier. */
    @Override
    public boolean isEnabled() {
        return employee.isActive();
    }
}
