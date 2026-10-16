package com.travelaggregator.service;

import com.travelaggregator.dto.BookingRequest;
import com.travelaggregator.dto.BookingResponse;
import com.travelaggregator.dto.FareCalculationResponse;
import com.travelaggregator.entity.Booking;
import com.travelaggregator.entity.BookingStatus;
import com.travelaggregator.entity.Journey;
import com.travelaggregator.entity.TransportMode;
import com.travelaggregator.exception.ResourceNotFoundException;
import com.travelaggregator.exception.SeatUnavailableException;
import com.travelaggregator.repository.BookingRepository;
import com.travelaggregator.repository.JourneyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BookingServiceUnitTest {

    @Mock
    private BookingRepository bookingRepository;

    @Mock
    private JourneyRepository journeyRepository;

    @Mock
    private JourneyService journeyService;

    @InjectMocks
    private BookingService bookingService;

    private Journey demoJourney;

    @BeforeEach
    void setUp() {
        demoJourney = Journey.builder()
                .id(10L)
                .transportMode(TransportMode.FLIGHT)
                .operator("IndiGo")
                .origin("Delhi")
                .destination("Mumbai")
                .departureTime(LocalDateTime.now().plusDays(2))
                .arrivalTime(LocalDateTime.now().plusDays(2).plusHours(2))
                .durationMinutes(120)
                .baseFare(new BigDecimal("3000.00"))
                .availableSeats(50)
                .rating(new BigDecimal("4.5"))
                .build();
    }

    @Test
    @DisplayName("Should accurately calculate fare breakdown with add-ons and promo")
    void testCalculateFare() {
        when(journeyRepository.findById(10L)).thenReturn(Optional.of(demoJourney));
        when(journeyService.calculateServiceFee(any(), eq(TransportMode.FLIGHT)))
                .thenReturn(new BigDecimal("150.00")); // 5%
        when(journeyService.calculateGst(any(), eq(TransportMode.FLIGHT)))
                .thenReturn(new BigDecimal("157.50")); // 5%

        FareCalculationResponse fare = bookingService.calculateFare(10L, 1, List.of("MEAL"), "TRAVEL10");

        assertNotNull(fare);
        assertEquals(new BigDecimal("3000.00"), fare.getBaseFarePerSeat());
        assertEquals(new BigDecimal("350"), fare.getAddonsCost()); // Meal = 350
        assertTrue(fare.getDiscountAmount().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    @DisplayName("Should successfully create booking and decrement available seats")
    void testCreateBooking_Success() {
        BookingRequest req = new BookingRequest();
        req.setJourneyId(10L);
        req.setPassengerName("Rohan Sharma");
        req.setPassengerEmail("rohan@example.com");
        req.setPassengerPhone("9876543210");
        req.setPassengers(2);
        req.setTravelDate(LocalDate.now().plusDays(2));

        when(journeyRepository.findById(10L)).thenReturn(Optional.of(demoJourney));
        when(journeyService.calculateServiceFee(any(), any())).thenReturn(new BigDecimal("150.00"));
        when(journeyService.calculateGst(any(), any())).thenReturn(new BigDecimal("157.50"));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> {
            Booking b = inv.getArgument(0);
            b.setId(101L);
            return b;
        });

        BookingResponse res = bookingService.createBooking(req);

        assertNotNull(res);
        assertEquals(101L, res.getBookingId());
        assertNotNull(res.getPnrNumber());
        assertEquals(BookingStatus.CONFIRMED, res.getBookingStatus());
        assertEquals(48, demoJourney.getAvailableSeats()); // 50 - 2 = 48
        verify(journeyRepository).save(demoJourney);
    }

    @Test
    @DisplayName("Should throw SeatUnavailableException when requested seats exceed available")
    void testCreateBooking_SeatUnavailable() {
        BookingRequest req = new BookingRequest();
        req.setJourneyId(10L);
        req.setPassengers(100); // Only 50 available

        when(journeyRepository.findById(10L)).thenReturn(Optional.of(demoJourney));

        assertThrows(SeatUnavailableException.class, () -> bookingService.createBooking(req));
        verify(bookingRepository, never()).save(any());
    }

    @Test
    @DisplayName("Should cancel booking and restore seats to the journey")
    void testCancelBooking() {
        Booking booking = Booking.builder()
                .id(201L)
                .journey(demoJourney)
                .passengers(3)
                .bookingStatus(BookingStatus.CONFIRMED)
                .pnrNumber("TBTEST1234")
                .totalFare(new BigDecimal("9000.00"))
                .build();

        when(bookingRepository.findById(201L)).thenReturn(Optional.of(booking));
        when(bookingRepository.save(any(Booking.class))).thenAnswer(inv -> inv.getArgument(0));

        BookingResponse res = bookingService.cancelBooking(201L);

        assertEquals(BookingStatus.CANCELLED, res.getBookingStatus());
        assertEquals(53, demoJourney.getAvailableSeats()); // 50 + 3 = 53
        verify(journeyRepository).save(demoJourney);
    }
}
