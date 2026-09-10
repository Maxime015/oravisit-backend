package com.orabank.backend.repository;

import com.orabank.backend.entity.Employee;
import com.orabank.backend.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface EmployeeRepository extends JpaRepository<Employee, Long>, JpaSpecificationExecutor<Employee> {

    Optional<Employee> findByMatriculeIgnoreCase(String matricule);

    boolean existsByMatriculeIgnoreCase(String matricule);

    boolean existsByMatriculeIgnoreCaseAndIdNot(String matricule, Long id);

    boolean existsByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCaseAndIdNot(String email, Long id);

    /** Sert a garantir qu'il reste toujours au moins un ADMIN actif. */
    long countByRoleAndActiveIsTrue(Role role);
}
