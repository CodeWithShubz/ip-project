package com.travelaggregator.config;

import com.travelaggregator.entity.*;
import com.travelaggregator.repository.BookingRepository;
import com.travelaggregator.repository.JourneyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Seeds demo journey data into the database on application startup.
 *
 * <p>Development refresh strategy:
 * <ul>
 *   <li>If journeys table is empty → seed fresh data.</li>
 *   <li>If journeys exist but ALL are in the past and no bookings exist
 *       → delete stale journeys and re-seed. This handles the outdated
 *       Dec 2024 data scenario safely.</li>
 *   <li>If bookings exist → never delete journeys (avoids orphaning
 *       booking foreign keys). Logs a warning instead.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(2) // Run after LocationSeeder
public class DataSeeder implements CommandLineRunner {

    private final JourneyRepository journeyRepository;
    private final BookingRepository bookingRepository;

    @org.springframework.beans.factory.annotation.Value("${app.seed.force-refresh:false}")
    private boolean forceRefresh;

    @Override
    public void run(String... args) {
        long journeyCount = journeyRepository.count();
        long bookingCount = bookingRepository.count();

        if (journeyCount == 0) {
            // Fresh database — seed now
            seed();
            return;
        }

        // Check if ALL existing journeys are in the past
        LocalDateTime now = LocalDateTime.now();
        boolean allStale = journeyRepository.findAll().stream()
                .allMatch(j -> j.getDepartureTime().isBefore(now));

        if (forceRefresh || allStale) {
            if (bookingCount == 0) {
                log.info("Refreshing seed data (forceRefresh={}, allStale={})…", forceRefresh, allStale);
                journeyRepository.deleteAll();
                seed();
            } else {
                log.info("Refreshing unbooked seed journeys while preserving {} booking(s)…", bookingCount);
                java.util.Set<Long> bookedJourneyIds = bookingRepository.findAll().stream()
                        .map(b -> b.getJourney().getId())
                        .collect(java.util.stream.Collectors.toSet());
                List<Journey> unbooked = journeyRepository.findAll().stream()
                        .filter(j -> !bookedJourneyIds.contains(j.getId()))
                        .toList();
                journeyRepository.deleteAll(unbooked);
                seed();
            }
        } else {
            log.info("Database has {} journey(s) with future departures. Skipping seed.", journeyCount);
        }
    }

    private void seed() {
        log.info("Seeding demo journey data…");
        List<Journey> journeys = buildDemoJourneys();
        journeyRepository.saveAll(journeys);
        log.info("✅ Seeded {} demo journeys (dates from {} to {}).",
                journeys.size(),
                LocalDate.now().plusDays(1),
                LocalDate.now().plusDays(14));
    }

    /**
     * Generates realistic demo journeys spread across the next 14 days.
     * Each route template is repeated for multiple days so that any date
     * the user picks (tomorrow through 2 weeks out) returns results.
     */
    private List<Journey> buildDemoJourneys() {
        LocalDate baseDate = LocalDate.now();
        List<Journey> all = new ArrayList<>();

        // Generate journeys for days +1 through +14
        for (int dayOffset = 1; dayOffset <= 14; dayOffset++) {
            LocalDate date = baseDate.plusDays(dayOffset);

            // ─── FLIGHTS ──────────────────────────────────────────────────
            // Delhi → Mumbai
            all.add(j(TransportMode.FLIGHT, "IndiGo",    "Delhi",    "Mumbai",    date, t(6,0),  t(8,5),   125, 3499, "Meal,USB Charging,Extra Legroom", 4.2, 120));
            all.add(j(TransportMode.FLIGHT, "Air India", "Delhi",    "Mumbai",    date, t(9,30), t(11,40), 130, 4999, "Meal,WiFi,Entertainment", 4.5, 85));
            all.add(j(TransportMode.FLIGHT, "SpiceJet",  "Delhi",    "Mumbai",    date, t(14,15),t(16,25), 130, 2799, "Snacks,USB Charging", 3.9, 200));
            all.add(j(TransportMode.FLIGHT, "Vistara",   "Delhi",    "Mumbai",    date, t(18,45),t(20,55), 130, 5499, "Meal,WiFi,Entertainment,Extra Legroom", 4.7, 60));
            all.add(j(TransportMode.FLIGHT, "GoFirst",   "Delhi",    "Mumbai",    date, t(22,0), t(0,10),  130, 2499, "Snacks", 3.7, 150));
            // Mumbai → Delhi
            all.add(j(TransportMode.FLIGHT, "IndiGo",    "Mumbai",   "Delhi",     date, t(5,30), t(7,35),  125, 3299, "Meal,USB Charging", 4.2, 110));
            all.add(j(TransportMode.FLIGHT, "Air India", "Mumbai",   "Delhi",     date, t(10,0), t(12,10), 130, 4799, "Meal,WiFi,Entertainment", 4.5, 90));
            all.add(j(TransportMode.FLIGHT, "SpiceJet",  "Mumbai",   "Delhi",     date, t(15,30),t(17,40), 130, 2699, "Snacks", 3.8, 180));
            // Delhi → Bangalore
            all.add(j(TransportMode.FLIGHT, "IndiGo",    "Delhi",    "Bangalore", date, t(7,0),  t(9,30),  150, 4299, "Meal,USB Charging", 4.3, 140));
            all.add(j(TransportMode.FLIGHT, "Air India", "Delhi",    "Bangalore", date, t(11,0), t(13,30), 150, 5799, "Meal,WiFi,Entertainment", 4.6, 75));
            all.add(j(TransportMode.FLIGHT, "Vistara",   "Delhi",    "Bangalore", date, t(16,0), t(18,30), 150, 6299, "Meal,WiFi,Entertainment,Extra Legroom", 4.8, 55));
            // Bangalore → Delhi (Reverse)
            all.add(j(TransportMode.FLIGHT, "IndiGo",    "Bangalore","Delhi",     date, t(6,30), t(9,0),   150, 4199, "Meal,USB Charging", 4.2, 130));
            all.add(j(TransportMode.FLIGHT, "Air India", "Bangalore","Delhi",     date, t(12,30),t(15,0),  150, 5699, "Meal,WiFi,Entertainment", 4.5, 80));
            all.add(j(TransportMode.FLIGHT, "Vistara",   "Bangalore","Delhi",     date, t(17,30),t(20,0),  150, 6199, "Meal,WiFi,Entertainment,Extra Legroom", 4.7, 60));

            // Mumbai → Goa
            all.add(j(TransportMode.FLIGHT, "IndiGo",    "Mumbai",   "Goa",       date, t(8,0),  t(9,10),  70,  2199, "Snacks,USB Charging", 4.1, 160));
            all.add(j(TransportMode.FLIGHT, "GoFirst",   "Mumbai",   "Goa",       date, t(13,0), t(14,10), 70,  1899, "Snacks", 3.8, 180));
            // Goa → Mumbai (Reverse)
            all.add(j(TransportMode.FLIGHT, "IndiGo",    "Goa",      "Mumbai",    date, t(10,30),t(11,40), 70,  2299, "Snacks,USB Charging", 4.2, 150));
            all.add(j(TransportMode.FLIGHT, "GoFirst",   "Goa",      "Mumbai",    date, t(15,30),t(16,40), 70,  1999, "Snacks", 3.9, 170));

            // ─── TRAINS ───────────────────────────────────────────────────
            // Delhi → Mumbai
            all.add(j(TransportMode.TRAIN, "Indian Railways", "Delhi",  "Mumbai",    date, t(16,35),t(8,15),  940,  895, "Sleeper,Pantry Car,Bedroll", 4.1, 300));
            all.add(j(TransportMode.TRAIN, "Indian Railways", "Delhi",  "Mumbai",    date, t(23,0), t(15,55), 955, 1245, "AC 3-Tier,Pantry Car,Bedroll", 4.3, 250));
            all.add(j(TransportMode.TRAIN, "Indian Railways", "Delhi",  "Mumbai",    date, t(11,40),t(11,45),1445, 2150, "AC 2-Tier,Pantry Car,Bedroll,WiFi", 4.5, 80));
            // Mumbai → Delhi
            all.add(j(TransportMode.TRAIN, "Indian Railways", "Mumbai", "Delhi",     date, t(6,0),  t(23,55),1075,  895, "Sleeper,Pantry Car", 4.0, 350));
            all.add(j(TransportMode.TRAIN, "Indian Railways", "Mumbai", "Delhi",     date, t(17,10),t(9,5),   955, 1245, "AC 3-Tier,Pantry Car,Bedroll", 4.2, 220));
            // Delhi → Bangalore
            all.add(j(TransportMode.TRAIN, "Indian Railways", "Delhi",  "Bangalore", date, t(20,0), t(12,30),2190, 1650, "Sleeper,Pantry Car", 4.0, 200));
            all.add(j(TransportMode.TRAIN, "Indian Railways", "Delhi",  "Bangalore", date, t(7,10), t(6,45), 2375, 3200, "AC 2-Tier,Pantry Car,Bedroll,WiFi", 4.4, 60));
            // Bangalore → Delhi (Reverse)
            all.add(j(TransportMode.TRAIN, "Indian Railways", "Bangalore","Delhi",   date, t(19,30),t(11,30),2160, 1650, "Sleeper,Pantry Car", 4.1, 210));
            all.add(j(TransportMode.TRAIN, "Indian Railways", "Bangalore","Delhi",   date, t(8,0),  t(7,15),  2355, 3200, "AC 2-Tier,Pantry Car,Bedroll,WiFi", 4.3, 70));

            // Mumbai → Goa
            all.add(j(TransportMode.TRAIN, "Indian Railways", "Mumbai", "Goa",       date, t(7,10), t(17,0),  590,  550, "Sleeper,Pantry Car", 4.2, 400));
            all.add(j(TransportMode.TRAIN, "Indian Railways", "Mumbai", "Goa",       date, t(22,10),t(6,0),   470,  890, "AC 3-Tier,Pantry Car,Bedroll", 4.4, 180));
            // Goa → Mumbai (Reverse)
            all.add(j(TransportMode.TRAIN, "Indian Railways", "Goa",    "Mumbai",    date, t(8,30), t(18,20), 590,  550, "Sleeper,Pantry Car", 4.2, 380));
            all.add(j(TransportMode.TRAIN, "Indian Railways", "Goa",    "Mumbai",    date, t(20,0), t(4,0),   480,  890, "AC 3-Tier,Pantry Car,Bedroll", 4.3, 190));

            // ─── BUSES ────────────────────────────────────────────────────
            // Delhi → Agra
            all.add(j(TransportMode.BUS, "RedBus Premium",  "Delhi",     "Agra",    date, t(6,0),  t(9,30),  210, 450, "AC,WiFi,USB Charging,Blanket", 4.3, 40));
            all.add(j(TransportMode.BUS, "UPSRTC",          "Delhi",     "Agra",    date, t(7,30), t(11,30), 240, 280, "AC", 3.8, 60));
            all.add(j(TransportMode.BUS, "Neeta Tours",     "Delhi",     "Agra",    date, t(22,0), t(1,30),  210, 520, "AC,Blanket,Snacks,USB Charging", 4.5, 35));
            // Agra → Delhi (Reverse)
            all.add(j(TransportMode.BUS, "RedBus Premium",  "Agra",      "Delhi",   date, t(14,0), t(17,30), 210, 450, "AC,WiFi,USB Charging,Blanket", 4.3, 40));
            all.add(j(TransportMode.BUS, "UPSRTC",          "Agra",      "Delhi",   date, t(16,0), t(20,0),  240, 280, "AC", 3.8, 60));

            // Mumbai → Pune
            all.add(j(TransportMode.BUS, "RedBus Premium",  "Mumbai",    "Pune",    date, t(6,0),  t(9,30),  210, 380, "AC,WiFi,USB Charging", 4.4, 45));
            all.add(j(TransportMode.BUS, "Shivneri MSRTC",  "Mumbai",    "Pune",    date, t(8,0),  t(11,0),  180, 320, "AC,WiFi", 4.6, 55));
            all.add(j(TransportMode.BUS, "Paulo Travels",   "Mumbai",    "Pune",    date, t(21,0), t(0,30),  210, 650, "Volvo AC,Blanket,Snacks,USB Charging,Entertainment", 4.7, 28));
            // Pune → Mumbai (Reverse)
            all.add(j(TransportMode.BUS, "RedBus Premium",  "Pune",      "Mumbai",  date, t(15,0), t(18,30), 210, 380, "AC,WiFi,USB Charging", 4.4, 45));
            all.add(j(TransportMode.BUS, "Shivneri MSRTC",  "Pune",      "Mumbai",  date, t(17,30),t(20,30), 180, 320, "AC,WiFi", 4.5, 50));

            // Mumbai → Goa (Bus)
            all.add(j(TransportMode.BUS, "Paulo Travels",   "Mumbai",    "Goa",     date, t(18,0), t(8,0),   840, 950, "Sleeper,AC,Blanket,USB Charging", 4.5, 40));
            all.add(j(TransportMode.BUS, "Kadamba Bus",     "Mumbai",    "Goa",     date, t(20,0), t(10,0),  840, 750, "AC,Blanket", 4.1, 50));
            // Goa → Mumbai (Reverse)
            all.add(j(TransportMode.BUS, "Paulo Travels",   "Goa",       "Mumbai",  date, t(17,0), t(7,0),   840, 950, "Sleeper,AC,Blanket,USB Charging", 4.4, 40));
            all.add(j(TransportMode.BUS, "Kadamba Bus",     "Goa",       "Mumbai",  date, t(19,30),t(9,30),  840, 750, "AC,Blanket", 4.2, 50));

            // Bangalore → Chennai
            all.add(j(TransportMode.BUS, "KSRTC Airavat",   "Bangalore", "Chennai", date, t(22,0), t(5,30),  450, 680, "AC,Blanket,Snacks", 4.4, 45));
            all.add(j(TransportMode.BUS, "Orange Tours",    "Bangalore", "Chennai", date, t(23,0), t(7,0),   480, 550, "AC,Blanket", 4.2, 50));
            // Chennai → Bangalore (Reverse)
            all.add(j(TransportMode.BUS, "KSRTC Airavat",   "Chennai",   "Bangalore",date, t(21,30),t(5,0),   450, 680, "AC,Blanket,Snacks", 4.3, 45));
            all.add(j(TransportMode.BUS, "Orange Tours",    "Chennai",   "Bangalore",date, t(22,30),t(6,30),  480, 550, "AC,Blanket", 4.1, 50));
        }
        return all;
    }

    /**
     * Build a Journey with departure/arrival computed from a base date and time.
     * The arrival time is calculated from departure + durationMinutes.
     */
    private Journey j(TransportMode mode, String op, String from, String to,
                      LocalDate date, LocalTime depTime, LocalTime arrTimeUnused,
                      int mins, int fare, String amenities, double rating, int seats) {
        LocalDateTime dep = date.atTime(depTime);
        LocalDateTime arr = dep.plusMinutes(mins);

        return Journey.builder()
                .transportMode(mode).operator(op).origin(from).destination(to)
                .departureTime(dep).arrivalTime(arr).durationMinutes(mins)
                .baseFare(new BigDecimal(fare)).amenities(amenities)
                .rating(new BigDecimal(String.valueOf(rating))).availableSeats(seats)
                .build();
    }

    private LocalTime t(int hr, int min) {
        return LocalTime.of(hr, min);
    }
}
