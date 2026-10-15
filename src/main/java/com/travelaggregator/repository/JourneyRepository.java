package com.travelaggregator.repository;

import com.travelaggregator.entity.Journey;
import com.travelaggregator.entity.TransportMode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Repository layer for Journey entities.
 * Spring Data JPA generates implementations at runtime.
 */
@Repository
public interface JourneyRepository extends JpaRepository<Journey, Long> {

    /**
     * Search journeys by route and date window with optional mode filter.
     * Filters out journeys with zero available seats.
     */
    @Query("""
            SELECT j FROM Journey j
            WHERE LOWER(j.origin)      = LOWER(:origin)
              AND LOWER(j.destination) = LOWER(:destination)
              AND j.departureTime      >= :startOfDay
              AND j.departureTime      <= :endOfDay
              AND j.availableSeats     >= :passengers
              AND (:mode IS NULL OR j.transportMode = :mode)
              AND j.baseFare           BETWEEN :minFare AND :maxFare
            ORDER BY j.baseFare ASC
            """)
    List<Journey> searchJourneys(
            @Param("origin")      String origin,
            @Param("destination") String destination,
            @Param("startOfDay")  LocalDateTime startOfDay,
            @Param("endOfDay")    LocalDateTime endOfDay,
            @Param("passengers")  int passengers,
            @Param("mode")        TransportMode mode,
            @Param("minFare")     BigDecimal minFare,
            @Param("maxFare")     BigDecimal maxFare
    );

    /** All distinct origin cities (for autocomplete / dropdowns). */
    @Query("SELECT DISTINCT j.origin FROM Journey j ORDER BY j.origin")
    List<String> findDistinctOrigins();

    /** All distinct destination cities (for autocomplete / dropdowns). */
    @Query("SELECT DISTINCT j.destination FROM Journey j ORDER BY j.destination")
    List<String> findDistinctDestinations();
}
