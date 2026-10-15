package com.travelaggregator.repository;

import com.travelaggregator.entity.Booking;
import com.travelaggregator.entity.BookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository layer for Booking entities.
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {

    /** Find all bookings by passenger email (case-insensitive). */
    List<Booking> findByPassengerEmailIgnoreCaseOrderByBookingDateDesc(String email);

    /** Find a booking by its PNR number. */
    Optional<Booking> findByPnrNumber(String pnrNumber);

    /** Count bookings by status for dashboard stats. */
    long countByBookingStatus(BookingStatus status);

    /** Revenue summary – total of all confirmed bookings. */
    @Query("SELECT COALESCE(SUM(b.totalFare), 0) FROM Booking b WHERE b.bookingStatus = 'CONFIRMED'")
    java.math.BigDecimal totalConfirmedRevenue();

    /** All bookings for a given journey id. */
    List<Booking> findByJourneyId(Long journeyId);
}
