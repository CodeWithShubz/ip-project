package com.travelaggregator.service;

import com.travelaggregator.dto.*;
import com.travelaggregator.entity.*;
import com.travelaggregator.exception.ResourceNotFoundException;
import com.travelaggregator.exception.SeatUnavailableException;
import com.travelaggregator.repository.BookingRepository;
import com.travelaggregator.repository.JourneyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Service layer for booking creation, fare calculation, and booking retrieval.
 *
 * <p>Fare computation sequence (per seat):
 * <pre>
 *   1. baseFare (from Journey entity)
 *   2. + serviceFee  (mode-dependent %)
 *   3. + GST         (on baseFare + serviceFee)
 *   4. + addonsCost  (fixed per pax per add-on)
 *   5. − discount    (promo code or standard 0 %)
 *   = totalFare per pax × passengers
 * </pre>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class BookingService {

    // ─── Add-on catalogue: key → (label, price per pax) ─────────────────
    private static final Map<String, BigDecimal> ADDON_PRICES = Map.of(
            "MEAL",              new BigDecimal("350"),
            "INSURANCE",         new BigDecimal("199"),
            "PRIORITY_BOARDING", new BigDecimal("149"),
            "EXTRA_BAGGAGE",     new BigDecimal("500"),
            "SEAT_SELECTION",    new BigDecimal("99")
    );

    private static final Map<String, String> ADDON_LABELS = Map.of(
            "MEAL",              "In-flight Meal",
            "INSURANCE",         "Travel Insurance",
            "PRIORITY_BOARDING", "Priority Boarding",
            "EXTRA_BAGGAGE",     "Extra 10kg Baggage",
            "SEAT_SELECTION",    "Seat Selection"
    );

    // ─── Promo codes (demo) ───────────────────────────────────────────────
    private static final Map<String, BigDecimal> PROMO_DISCOUNTS = Map.of(
            "TRAVEL10",  new BigDecimal("0.10"),   // 10% discount
            "FIRST20",   new BigDecimal("0.20"),   // 20% for first booking
            "SAVE5",     new BigDecimal("0.05"),   // 5% flat
            "STUDENT15", new BigDecimal("0.15")    // 15% student discount
    );

    private final BookingRepository bookingRepository;
    private final JourneyRepository journeyRepository;
    private final JourneyService    journeyService;

    // ─── Fare Calculation (no persistence) ───────────────────────────────

    /**
     * Calculates fare breakdown for a booking request WITHOUT saving to DB.
     * Used by the frontend "Calculate Fare" endpoint.
     */
    public FareCalculationResponse calculateFare(Long journeyId, int passengers, List<String> selectedAddons, String promoCode) {
        Journey journey = journeyRepository.findById(journeyId)
                .orElseThrow(() -> new ResourceNotFoundException("Journey not found with id: " + journeyId));

        return buildFareBreakdown(journey, passengers, selectedAddons, promoCode);
    }

    // ─── Booking Creation ─────────────────────────────────────────────────

    /**
     * Creates and persists a booking after recalculating the fare server-side.
     * Fares calculated on the client are NEVER trusted.
     */
    @Transactional
    public BookingResponse createBooking(BookingRequest req) {
        // 1. Fetch journey
        Journey journey = journeyRepository.findById(req.getJourneyId())
                .orElseThrow(() -> new ResourceNotFoundException("Journey not found with id: " + req.getJourneyId()));

        // 2. Check seat availability
        if (journey.getAvailableSeats() < req.getPassengers()) {
            throw new SeatUnavailableException(
                    "Only " + journey.getAvailableSeats() + " seats available. Requested: " + req.getPassengers());
        }

        // 3. Recalculate fare on the backend (trust no client values)
        FareCalculationResponse fare = buildFareBreakdown(journey, req.getPassengers(),
                req.getSelectedAddons(), req.getPromoCode());

        // 4. Decrement available seats
        journey.setAvailableSeats(journey.getAvailableSeats() - req.getPassengers());
        journeyRepository.save(journey);

        // 5. Persist booking
        Booking booking = Booking.builder()
                .journey(journey)
                .passengerName(req.getPassengerName().trim())
                .passengerEmail(req.getPassengerEmail().trim().toLowerCase())
                .passengerPhone(req.getPassengerPhone())
                .passengers(req.getPassengers())
                .travelDate(req.getTravelDate())
                .baseFarePerSeat(fare.getBaseFarePerSeat())
                .addonsCost(fare.getAddonsCost())
                .serviceFee(fare.getServiceFee())
                .gstAmount(fare.getGstAmount())
                .discountAmount(fare.getDiscountAmount())
                .totalFare(fare.getTotalFare())
                .selectedAddons(req.getSelectedAddons() != null
                        ? String.join(",", req.getSelectedAddons()) : "")
                .bookingStatus(BookingStatus.CONFIRMED)
                .bookingDate(LocalDateTime.now())
                .pnrNumber(generatePnr())
                .build();

        booking = bookingRepository.save(booking);
        log.info("Booking confirmed: PNR={}, journey={}, pax={}, total=₹{}",
                booking.getPnrNumber(), journey.getId(), req.getPassengers(), fare.getTotalFare());

        return mapToResponse(booking, fare);
    }

    // ─── Retrieval ────────────────────────────────────────────────────────

    public BookingResponse getBookingByPnr(String pnr) {
        Booking b = bookingRepository.findByPnrNumber(pnr.toUpperCase())
                .orElseThrow(() -> new ResourceNotFoundException("No booking found with PNR: " + pnr));
        return mapToResponse(b, null);
    }

    public List<BookingResponse> getBookingsByEmail(String email) {
        return bookingRepository
                .findByPassengerEmailIgnoreCaseOrderByBookingDateDesc(email)
                .stream()
                .map(b -> mapToResponse(b, null))
                .collect(Collectors.toList());
    }

    /**
     * Cancel a booking (sets status to CANCELLED and restores seats).
     */
    @Transactional
    public BookingResponse cancelBooking(Long bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking not found with id: " + bookingId));

        if (booking.getBookingStatus() == BookingStatus.CANCELLED) {
            throw new IllegalStateException("Booking is already cancelled");
        }

        booking.setBookingStatus(BookingStatus.CANCELLED);
        // Restore seats
        Journey journey = booking.getJourney();
        journey.setAvailableSeats(journey.getAvailableSeats() + booking.getPassengers());
        journeyRepository.save(journey);
        booking = bookingRepository.save(booking);
        log.info("Booking cancelled: PNR={}", booking.getPnrNumber());
        return mapToResponse(booking, null);
    }

    // ─── Internal Fare Computation ────────────────────────────────────────

    private FareCalculationResponse buildFareBreakdown(
            Journey journey, int passengers, List<String> selectedAddons, String promoCode) {

        BigDecimal baseFarePerSeat = journey.getBaseFare();
        BigDecimal baseFareTotal   = baseFarePerSeat.multiply(BigDecimal.valueOf(passengers));

        // Service fee (platform commission)
        BigDecimal serviceFeePerSeat = journeyService.calculateServiceFee(baseFarePerSeat, journey.getTransportMode());
        BigDecimal serviceFeeTotal   = serviceFeePerSeat.multiply(BigDecimal.valueOf(passengers));

        // GST on (baseFare + serviceFee)
        BigDecimal subPerSeat = baseFarePerSeat.add(serviceFeePerSeat);
        BigDecimal gstPerSeat = journeyService.calculateGst(subPerSeat, journey.getTransportMode());
        BigDecimal gstTotal   = gstPerSeat.multiply(BigDecimal.valueOf(passengers));

        // Add-ons cost (total across all pax)
        BigDecimal addonsCost = BigDecimal.ZERO;
        List<FareCalculationResponse.AddonInfo> addonInfoList = buildAddonList(selectedAddons);
        if (selectedAddons != null) {
            for (String addonKey : selectedAddons) {
                BigDecimal price = ADDON_PRICES.getOrDefault(addonKey.toUpperCase(), BigDecimal.ZERO);
                addonsCost = addonsCost.add(price.multiply(BigDecimal.valueOf(passengers)));
            }
        }

        // Sub-total before discount
        BigDecimal grossTotal = baseFareTotal.add(serviceFeeTotal).add(gstTotal).add(addonsCost);

        // Promo/discount
        BigDecimal discountAmount = BigDecimal.ZERO;
        if (promoCode != null && !promoCode.isBlank()) {
            BigDecimal discountRate = PROMO_DISCOUNTS.get(promoCode.toUpperCase().trim());
            if (discountRate != null) {
                discountAmount = grossTotal.multiply(discountRate).setScale(2, RoundingMode.HALF_UP);
            }
        }

        BigDecimal totalFare = grossTotal.subtract(discountAmount).setScale(2, RoundingMode.HALF_UP);
        if (totalFare.compareTo(BigDecimal.ZERO) < 0) totalFare = BigDecimal.ZERO;

        return FareCalculationResponse.builder()
                .baseFarePerSeat(baseFarePerSeat)
                .baseFareTotal(baseFareTotal)
                .addonsCost(addonsCost)
                .serviceFee(serviceFeeTotal.setScale(2, RoundingMode.HALF_UP))
                .gstAmount(gstTotal.setScale(2, RoundingMode.HALF_UP))
                .discountAmount(discountAmount)
                .totalFare(totalFare)
                .passengers(passengers)
                .addons(addonInfoList)
                .build();
    }

    private List<FareCalculationResponse.AddonInfo> buildAddonList(List<String> selected) {
        Set<String> selectedSet = selected != null
                ? selected.stream().map(String::toUpperCase).collect(Collectors.toSet())
                : Collections.emptySet();

        return ADDON_PRICES.entrySet().stream()
                .map(e -> FareCalculationResponse.AddonInfo.builder()
                        .key(e.getKey())
                        .label(ADDON_LABELS.getOrDefault(e.getKey(), e.getKey()))
                        .pricePerPax(e.getValue())
                        .selected(selectedSet.contains(e.getKey()))
                        .build())
                .sorted(Comparator.comparing(FareCalculationResponse.AddonInfo::getKey))
                .collect(Collectors.toList());
    }

    // ─── Mapping Helpers ──────────────────────────────────────────────────

    private BookingResponse mapToResponse(Booking b, FareCalculationResponse fare) {
        Journey j = b.getJourney();
        List<String> addons = (b.getSelectedAddons() != null && !b.getSelectedAddons().isBlank())
                ? Arrays.asList(b.getSelectedAddons().split(","))
                : Collections.emptyList();

        return BookingResponse.builder()
                .bookingId(b.getId())
                .pnrNumber(b.getPnrNumber())
                .bookingStatus(b.getBookingStatus())
                .bookingDate(b.getBookingDate())
                // Journey
                .journeyId(j.getId())
                .transportMode(j.getTransportMode())
                .operator(j.getOperator())
                .origin(j.getOrigin())
                .destination(j.getDestination())
                .departureTime(j.getDepartureTime())
                .arrivalTime(j.getArrivalTime())
                .durationFormatted(JourneyService.formatDuration(j.getDurationMinutes()))
                .travelDate(b.getTravelDate())
                // Passenger
                .passengerName(b.getPassengerName())
                .passengerEmail(b.getPassengerEmail())
                .passengerPhone(b.getPassengerPhone())
                .passengers(b.getPassengers())
                // Fare
                .baseFarePerSeat(b.getBaseFarePerSeat())
                .addonsCost(b.getAddonsCost())
                .serviceFee(b.getServiceFee())
                .gstAmount(b.getGstAmount())
                .discountAmount(b.getDiscountAmount())
                .totalFare(b.getTotalFare())
                .selectedAddons(addons)
                .message("Booking " + b.getBookingStatus().name().toLowerCase() + " successfully")
                .build();
    }

    // ─── Utility ──────────────────────────────────────────────────────────

    /** Generates a unique 10-character alphanumeric PNR. */
    private String generatePnr() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        StringBuilder sb = new StringBuilder("TB");
        Random rnd = new Random();
        for (int i = 0; i < 8; i++) sb.append(chars.charAt(rnd.nextInt(chars.length())));
        String pnr = sb.toString();
        // Ensure uniqueness
        if (bookingRepository.findByPnrNumber(pnr).isPresent()) return generatePnr();
        return pnr;
    }
}
