package com.orabank.backend.dto;

import com.orabank.backend.entity.IdentityDocumentType;
import com.orabank.backend.entity.Visitor;

/** Visiteur resume, imbrique dans les reponses de visite. */
public record VisitorSummaryResponse(
        Long id,
        String fullName,
        String phone,
        IdentityDocumentType identityDocumentType,
        String identityCardNumber,
        boolean active) {

    public static VisitorSummaryResponse from(Visitor visitor) {
        return new VisitorSummaryResponse(
                visitor.getId(),
                visitor.getFullName(),
                visitor.getPhone(),
                visitor.getIdentityDocumentType(),
                visitor.getIdentityCardNumber(),
                visitor.isActive());
    }
}
