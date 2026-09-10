package com.orabank.backend.dto;

import com.orabank.backend.entity.IdentityDocumentType;
import com.orabank.backend.entity.Visitor;
import java.time.Instant;

/** Visiteur expose par l'API. */
public record VisitorResponse(
        Long id,
        String firstName,
        String lastName,
        String fullName,
        String email,
        String phone,
        IdentityDocumentType identityDocumentType,
        String identityCardNumber,
        boolean active,
        Instant archivedAt,
        long visitCount,
        Instant createdAt,
        Instant updatedAt) {

    public static VisitorResponse from(Visitor visitor, long visitCount) {
        return new VisitorResponse(
                visitor.getId(),
                visitor.getFirstName(),
                visitor.getLastName(),
                visitor.getFullName(),
                visitor.getEmail(),
                visitor.getPhone(),
                visitor.getIdentityDocumentType(),
                visitor.getIdentityCardNumber(),
                visitor.isActive(),
                visitor.getArchivedAt(),
                visitCount,
                visitor.getCreatedAt(),
                visitor.getUpdatedAt());
    }
}
