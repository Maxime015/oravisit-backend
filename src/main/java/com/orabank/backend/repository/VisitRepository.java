package com.orabank.backend.repository;

import com.orabank.backend.dto.DirectionCountResponse;
import com.orabank.backend.dto.TopHostEmployeeResponse;
import com.orabank.backend.entity.Visit;
import com.orabank.backend.entity.VisitStatus;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VisitRepository extends JpaRepository<Visit, Long>, JpaSpecificationExecutor<Visit> {

    /** Le graphe evite une requete par visite pour charger visiteur et employes. */
    @Override
    @EntityGraph(attributePaths = {"visitor", "hostEmployee", "registeredBy"})
    Page<Visit> findAll(Specification<Visit> specification, Pageable pageable);

    @EntityGraph(attributePaths = {"visitor", "hostEmployee", "registeredBy"})
    Optional<Visit> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = {"visitor", "hostEmployee", "registeredBy"})
    Page<Visit> findByVisitorId(Long visitorId, Pageable pageable);

    @EntityGraph(attributePaths = {"visitor", "hostEmployee", "registeredBy"})
    List<Visit> findByStatusOrderByCheckInAtAsc(VisitStatus status);

    // --- Regles metier ----------------------------------------------------

    boolean existsByBadgeNumberIgnoreCaseAndStatus(String badgeNumber, VisitStatus status);

    boolean existsByVisitorIdAndStatus(Long visitorId, VisitStatus status);

    // --- Comptages (calcules en base, jamais en memoire) -------------------

    long countByStatus(VisitStatus status);

    long countByCheckInAtGreaterThanEqualAndCheckInAtLessThan(Instant from, Instant to);

    long countByStatusAndCheckInAtGreaterThanEqualAndCheckInAtLessThan(
            VisitStatus status, Instant from, Instant to);

    /** Nombre de visites de chaque visiteur d'une page : une seule requete groupee. */
    @Query("""
            SELECT v.visitor.id AS visitorId, COUNT(v) AS total
            FROM Visit v
            WHERE v.visitor.id IN :visitorIds
            GROUP BY v.visitor.id
            """)
    List<VisitorCount> countByVisitorIds(@Param("visitorIds") Collection<Long> visitorIds);

    /** Duree moyenne en minutes des visites terminees de la periode. */
    @Query(value = """
            SELECT COALESCE(AVG(EXTRACT(EPOCH FROM (v.check_out_at - v.check_in_at)) / 60.0), 0)
            FROM visits v
            WHERE v.status = 'TERMINEE'
              AND v.check_out_at IS NOT NULL
              AND v.check_in_at >= :from
              AND v.check_in_at < :to
            """, nativeQuery = true)
    double averageCompletedDurationMinutes(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT new com.orabank.backend.dto.DirectionCountResponse(v.direction, COUNT(v))
            FROM Visit v
            WHERE v.checkInAt >= :from AND v.checkInAt < :to
            GROUP BY v.direction
            ORDER BY COUNT(v) DESC, v.direction ASC
            """)
    List<DirectionCountResponse> countByDirection(@Param("from") Instant from, @Param("to") Instant to);

    @Query("""
            SELECT new com.orabank.backend.dto.TopHostEmployeeResponse(
                       h.id, h.matricule, CONCAT(h.firstName, ' ', h.lastName), h.direction, COUNT(v))
            FROM Visit v
            JOIN v.hostEmployee h
            WHERE v.checkInAt >= :from AND v.checkInAt < :to
            GROUP BY h.id, h.matricule, h.firstName, h.lastName, h.direction
            ORDER BY COUNT(v) DESC, h.lastName ASC
            """)
    List<TopHostEmployeeResponse> findTopHostEmployees(@Param("from") Instant from,
                                                       @Param("to") Instant to,
                                                       Pageable pageable);

    /** Visites par jour, le jour etant calcule dans le fuseau applicatif. */
    @Query(value = """
            SELECT CAST(v.check_in_at AT TIME ZONE :zone AS date) AS day,
                   COUNT(*)                                       AS total
            FROM visits v
            WHERE v.check_in_at >= :from AND v.check_in_at < :to
            GROUP BY 1
            ORDER BY 1
            """, nativeQuery = true)
    List<DayCount> countByDay(@Param("from") Instant from,
                              @Param("to") Instant to,
                              @Param("zone") String zone);

    interface VisitorCount {
        Long getVisitorId();

        long getTotal();
    }

    interface DayCount {
        LocalDate getDay();

        long getTotal();
    }
}
