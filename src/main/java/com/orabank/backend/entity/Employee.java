package com.orabank.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Employe Orabank : compte applicatif et hote potentiel d'une visite.
 * Jamais supprime, seulement desactive, pour conserver l'historique.
 */
@Entity
@Table(name = "employees")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Employee extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20, unique = true)
    private String matricule;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(nullable = false, length = 120, unique = true)
    private String email;

    @Column(length = 30)
    private String phone;

    /** Hash BCrypt : jamais expose par l'API. */
    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 64)
    private Direction direction;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
