package com.travelaggregator.controller;

import com.travelaggregator.dto.*;
import com.travelaggregator.service.BookingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST Controller for all booking operations.
 * Base URL: /api/bookings
 */
@RestController
@RequestMapping("/api/bookings")
@RequiredArgsConstructor
@CrossOrigin(origins = "*")
public class BookingController {

    private final BookingService bookingService;

    /**
     * POST /api/bookings
     * Create a new booking. Recalculates fare on the backend.
     * @Valid triggers bean-validation on BookingRequest
     */
    @PostMapping
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking confirmed! PNR: " + response.getPnrNumber(), response));
    }

    /**
     * GET /api/bookings/pnr/{pnr}
     * Retrieve a booking by PNR number.
     */
    @GetMapping("/pnr/{pnr}")
    public ResponseEntity<ApiResponse<BookingResponse>> getByPnr(@PathVariable String pnr) {
        return ResponseEntity.ok(ApiResponse.success(bookingService.getBookingByPnr(pnr)));
    }

    /**
     * GET /api/bookings/email/{email}
     * Retrieve all bookings for a passenger email.
     */
    @GetMapping("/email/{email}")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getByEmail(@PathVariable String email) {
        List<BookingResponse> bookings = bookingService.getBookingsByEmail(email);
        return ResponseEntity.ok(ApiResponse.success(bookings.size() + " booking(s) found.", bookings));
    }

    /**
     * PUT /api/bookings/{id}/cancel
     * Cancel an existing booking (restores seats).
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<BookingResponse>> cancelBooking(@PathVariable Long id) {
        BookingResponse response = bookingService.cancelBooking(id);
        return ResponseEntity.ok(ApiResponse.success("Booking cancelled successfully.", response));
    }

    /**
     * POST /api/bookings/calculate-fare
     * Calculate fare preview WITHOUT creating a booking.
     * Body: { journeyId, passengers, selectedAddons, promoCode }
     */
    @PostMapping("/calculate-fare")
    public ResponseEntity<ApiResponse<FareCalculationResponse>> calculateFare(
            @RequestBody BookingRequest request) {
        FareCalculationResponse fare = bookingService.calculateFare(
                request.getJourneyId(),
                request.getPassengers() < 1 ? 1 : request.getPassengers(),
                request.getSelectedAddons(),
                request.getPromoCode());
        return ResponseEntity.ok(ApiResponse.success("Fare calculated successfully.", fare));
    }
}
