package com.orabank.backend.service;

import com.orabank.backend.dto.DashboardResponse;
import com.orabank.backend.dto.VisitResponse;
import com.orabank.backend.dto.VisitsTrendPointResponse;
import com.orabank.backend.entity.VisitStatus;
import com.orabank.backend.exception.ApiException;
import com.orabank.backend.repository.VisitRepository;
import com.orabank.backend.repository.VisitorRepository;
import com.orabank.backend.security.CurrentUser;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Indicateurs du tableau de bord. Tout est compte par la base (COUNT, GROUP BY,
 * AVG) : aucune entite n'est chargee pour etre comptee en memoire.
 * L'agent d'accueil recoit la vue du jour, l'ADMIN y ajoute les statistiques.
 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    /** Periode analytique par defaut : les 30 derniers jours, aujourd'hui inclus. */
    private static final int DEFAULT_TREND_DAYS = 30;
    private static final int MAX_RANGE_DAYS = 366;
    private static final int TOP_HOSTS_LIMIT = 5;

    private final VisitRepository visitRepository;
    private final VisitorRepository visitorRepository;
    private final Clock clock;

    @Transactional(readOnly = true)
    public DashboardResponse load(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(clock);
        LocalDate rangeEnd = to != null ? to : today;
        LocalDate rangeStart = from != null ? from : rangeEnd.minusDays(DEFAULT_TREND_DAYS - 1L);
        validateRange(rangeStart, rangeEnd);

        Instant startOfToday = startOfDay(today);
        Instant startOfTomorrow = startOfDay(today.plusDays(1));

        long visitsToday = visitRepository
                .countByCheckInAtGreaterThanEqualAndCheckInAtLessThan(startOfToday, startOfTomorrow);
        long activeVisits = visitRepository.countByStatus(VisitStatus.EN_COURS);
        long completedToday = visitRepository.countByStatusAndCheckInAtGreaterThanEqualAndCheckInAtLessThan(
                VisitStatus.TERMINEE, startOfToday, startOfTomorrow);
        long cancelledToday = visitRepository.countByStatusAndCheckInAtGreaterThanEqualAndCheckInAtLessThan(
                VisitStatus.ANNULEE, startOfToday, startOfTomorrow);
        double averageDuration = roundToOneDecimal(
                visitRepository.averageCompletedDurationMinutes(startOfToday, startOfTomorrow));
        List<VisitResponse> currentVisitors = visitRepository
                .findByStatusOrderByCheckInAtAsc(VisitStatus.EN_COURS).stream()
                .map(VisitResponse::from)
                .toList();

        if (!CurrentUser.isAdmin()) {
            return new DashboardResponse(null, null, visitsToday, activeVisits, completedToday, cancelledToday,
                    averageDuration, currentVisitors, null, null, null, null, null);
        }

        Instant rangeFrom = startOfDay(rangeStart);
        Instant rangeTo = startOfDay(rangeEnd.plusDays(1));
        return new DashboardResponse(
                rangeStart,
                rangeEnd,
                visitsToday,
                activeVisits,
                completedToday,
                cancelledToday,
                averageDuration,
                currentVisitors,
                visitorRepository.countByActiveIsTrue(),
                visitorRepository.countByActiveIsFalse(),
                visitRepository.countByDirection(rangeFrom, rangeTo),
                trend(rangeStart, rangeEnd, rangeFrom, rangeTo),
                visitRepository.findTopHostEmployees(rangeFrom, rangeTo, PageRequest.of(0, TOP_HOSTS_LIMIT)));
    }

    /** Courbe continue : les jours sans visite valent 0. */
    private List<VisitsTrendPointResponse> trend(LocalDate start, LocalDate end, Instant from, Instant to) {
        Map<LocalDate, Long> totalsByDay = visitRepository.countByDay(from, to, clock.getZone().getId()).stream()
                .collect(Collectors.toMap(VisitRepository.DayCount::getDay, VisitRepository.DayCount::getTotal));

        return start.datesUntil(end.plusDays(1))
                .map(day -> new VisitsTrendPointResponse(day, totalsByDay.getOrDefault(day, 0L)))
                .toList();
    }

    private void validateRange(LocalDate from, LocalDate to) {
        if (from.isAfter(to)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE", "La date de debut doit preceder la date de fin.");
        }
        if (from.plusDays(MAX_RANGE_DAYS).isBefore(to)) {
            throw ApiException.badRequest("INVALID_DATE_RANGE", "La periode ne peut pas depasser 366 jours.");
        }
    }

    private Instant startOfDay(LocalDate date) {
        return date.atStartOfDay(clock.getZone()).toInstant();
    }

    private double roundToOneDecimal(double value) {
        return Math.round(value * 10.0) / 10.0;
    }
}
