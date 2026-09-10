package com.orabank.backend.repository;

import com.orabank.backend.entity.IdentityDocumentType;
import com.orabank.backend.entity.Visitor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VisitorRepository extends JpaRepository<Visitor, Long>, JpaSpecificationExecutor<Visitor> {

    boolean existsByIdentityDocumentTypeAndIdentityCardNumberIgnoreCase(
            IdentityDocumentType type, String identityCardNumber);

    boolean existsByIdentityDocumentTypeAndIdentityCardNumberIgnoreCaseAndIdNot(
            IdentityDocumentType type, String identityCardNumber, Long id);

    long countByActiveIsTrue();

    long countByActiveIsFalse();
}
