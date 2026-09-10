package com.orabank.backend.dto;

import com.orabank.backend.entity.Direction;
import com.orabank.backend.entity.Visit;
import com.orabank.backend.entity.VisitStatus;
import java.time.Duration;
import java.time.Instant;

/** Visite exposee par l'API. */
public record VisitResponse(
        Long id,
        VisitorSummaryResponse visitor,
        EmployeeSummaryResponse hostEmployee,
        EmployeeSummaryResponse registeredBy,
        Direction direction,
        String purpose,
        String badgeNumber,
        VisitStatus status,
        Instant checkInAt,
        Instant checkOutAt,
        String cancellationReason,
        Long durationMinutes) {

    public static VisitResponse from(Visit visit) {
        return new VisitResponse(
                visit.getId(),
                VisitorSummaryResponse.from(visit.getVisitor()),
                EmployeeSummaryResponse.from(visit.getHostEmployee()),
                EmployeeSummaryResponse.from(visit.getRegisteredBy()),
                visit.getDirection(),
                visit.getPurpose(),
                visit.getBadgeNumber(),
                visit.getStatus(),
                visit.getCheckInAt(),
                visit.getCheckOutAt(),
                visit.getCancellationReason(),
                durationMinutes(visit));
    }

    /** La duree n'existe qu'une fois le check-out effectue. */
    private static Long durationMinutes(Visit visit) {
        if (visit.getCheckOutAt() == null) {
            return null;
        }
        return Duration.between(visit.getCheckInAt(), visit.getCheckOutAt()).toMinutes();
    }
}
