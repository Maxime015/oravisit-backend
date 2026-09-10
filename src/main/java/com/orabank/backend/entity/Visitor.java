package com.orabank.backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Visiteur recu a l'accueil. Une fiche n'est jamais supprimee : elle est
 * archivee ({@code active = false}) pour conserver l'historique des visites.
 */
@Entity
@Table(name = "visitors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Visitor extends AuditableEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "first_name", nullable = false, length = 60)
    private String firstName;

    @Column(name = "last_name", nullable = false, length = 60)
    private String lastName;

    @Column(length = 120)
    private String email;

    @Column(nullable = false, length = 30)
    private String phone;

    /** L'unicite d'un visiteur porte sur le couple (type, numero) de piece. */
    @Enumerated(EnumType.STRING)
    @Column(name = "identity_document_type", nullable = false, length = 20)
    private IdentityDocumentType identityDocumentType;

    @Column(name = "identity_card_number", nullable = false, length = 40)
    private String identityCardNumber;

    @Builder.Default
    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    @Column(name = "archived_at")
    private Instant archivedAt;

    public String getFullName() {
        return firstName + " " + lastName;
    }
}
