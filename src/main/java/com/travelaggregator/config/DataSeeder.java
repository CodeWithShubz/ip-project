package com.travelaggregator.config;

import com.travelaggregator.entity.*;
import com.travelaggregator.repository.JourneyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds demo journey data into the database on application startup.
 * Only runs if the journeys table is empty — safe to restart.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class DataSeeder implements CommandLineRunner {

    private final JourneyRepository journeyRepository;

    @Override
    public void run(String... args) {
        if (journeyRepository.count() > 0) {
            log.info("Database already seeded ({} journeys). Skipping.", journeyRepository.count());
            return;
        }
        log.info("Seeding demo journey data…");
        journeyRepository.saveAll(buildDemoJourneys());
        log.info("✅ Seeded {} demo journeys.", journeyRepository.count());
    }

    private List<Journey> buildDemoJourneys() {
        return List.of(
            // ─── FLIGHTS ──────────────────────────────────────────────────────
            // Delhi → Mumbai
            j(TransportMode.FLIGHT,"IndiGo",    "Delhi","Mumbai",   dt(2024,12,20, 6, 0),dt(2024,12,20, 8, 5), 125,3499,"Meal,USB Charging,Extra Legroom",4.2,120),
            j(TransportMode.FLIGHT,"Air India",  "Delhi","Mumbai",   dt(2024,12,20, 9,30),dt(2024,12,20,11,40), 130,4999,"Meal,WiFi,Entertainment",4.5,85),
            j(TransportMode.FLIGHT,"SpiceJet",   "Delhi","Mumbai",   dt(2024,12,20,14,15),dt(2024,12,20,16,25), 130,2799,"Snacks,USB Charging",3.9,200),
            j(TransportMode.FLIGHT,"Vistara",    "Delhi","Mumbai",   dt(2024,12,20,18,45),dt(2024,12,20,20,55), 130,5499,"Meal,WiFi,Entertainment,Extra Legroom",4.7,60),
            j(TransportMode.FLIGHT,"GoFirst",    "Delhi","Mumbai",   dt(2024,12,20,22, 0),dt(2024,12,21, 0,10), 130,2499,"Snacks",3.7,150),
            // Mumbai → Delhi
            j(TransportMode.FLIGHT,"IndiGo",    "Mumbai","Delhi",   dt(2024,12,21, 5,30),dt(2024,12,21, 7,35), 125,3299,"Meal,USB Charging",4.2,110),
            j(TransportMode.FLIGHT,"Air India",  "Mumbai","Delhi",   dt(2024,12,21,10, 0),dt(2024,12,21,12,10), 130,4799,"Meal,WiFi,Entertainment",4.5,90),
            j(TransportMode.FLIGHT,"SpiceJet",   "Mumbai","Delhi",   dt(2024,12,21,15,30),dt(2024,12,21,17,40), 130,2699,"Snacks",3.8,180),
            // Delhi → Bangalore
            j(TransportMode.FLIGHT,"IndiGo",    "Delhi","Bangalore",dt(2024,12,20, 7, 0),dt(2024,12,20, 9,30), 150,4299,"Meal,USB Charging",4.3,140),
            j(TransportMode.FLIGHT,"Air India",  "Delhi","Bangalore",dt(2024,12,20,11, 0),dt(2024,12,20,13,30), 150,5799,"Meal,WiFi,Entertainment",4.6,75),
            j(TransportMode.FLIGHT,"Vistara",    "Delhi","Bangalore",dt(2024,12,20,16, 0),dt(2024,12,20,18,30), 150,6299,"Meal,WiFi,Entertainment,Extra Legroom",4.8,55),
            // Mumbai → Goa
            j(TransportMode.FLIGHT,"IndiGo",    "Mumbai","Goa",     dt(2024,12,20, 8, 0),dt(2024,12,20, 9,10),  70,2199,"Snacks,USB Charging",4.1,160),
            j(TransportMode.FLIGHT,"GoFirst",    "Mumbai","Goa",     dt(2024,12,20,13, 0),dt(2024,12,20,14,10),  70,1899,"Snacks",3.8,180),

            // ─── TRAINS ───────────────────────────────────────────────────────
            // Delhi → Mumbai
            j(TransportMode.TRAIN,"Indian Railways","Delhi","Mumbai", dt(2024,12,20,16,35),dt(2024,12,21, 8,15), 940, 895,"Sleeper,Pantry Car,Bedroll",4.1,300),
            j(TransportMode.TRAIN,"Indian Railways","Delhi","Mumbai", dt(2024,12,20,23, 0),dt(2024,12,21,15,55), 955,1245,"AC 3-Tier,Pantry Car,Bedroll",4.3,250),
            j(TransportMode.TRAIN,"Indian Railways","Delhi","Mumbai", dt(2024,12,20,11,40),dt(2024,12,21,11,45),1445,2150,"AC 2-Tier,Pantry Car,Bedroll,WiFi",4.5,80),
            // Mumbai → Delhi
            j(TransportMode.TRAIN,"Indian Railways","Mumbai","Delhi", dt(2024,12,21, 6, 0),dt(2024,12,21,23,55),1075, 895,"Sleeper,Pantry Car",4.0,350),
            j(TransportMode.TRAIN,"Indian Railways","Mumbai","Delhi", dt(2024,12,21,17,10),dt(2024,12,22, 9, 5), 955,1245,"AC 3-Tier,Pantry Car,Bedroll",4.2,220),
            // Delhi → Bangalore
            j(TransportMode.TRAIN,"Indian Railways","Delhi","Bangalore",dt(2024,12,20,20, 0),dt(2024,12,22,12,30),2190,1650,"Sleeper,Pantry Car",4.0,200),
            j(TransportMode.TRAIN,"Indian Railways","Delhi","Bangalore",dt(2024,12,20, 7,10),dt(2024,12,22, 6,45),2375,3200,"AC 2-Tier,Pantry Car,Bedroll,WiFi",4.4,60),
            // Mumbai → Goa
            j(TransportMode.TRAIN,"Indian Railways","Mumbai","Goa", dt(2024,12,20, 7,10),dt(2024,12,20,17, 0), 590, 550,"Sleeper,Pantry Car",4.2,400),
            j(TransportMode.TRAIN,"Indian Railways","Mumbai","Goa", dt(2024,12,20,22,10),dt(2024,12,21, 6, 0), 470, 890,"AC 3-Tier,Pantry Car,Bedroll",4.4,180),

            // ─── BUSES ────────────────────────────────────────────────────────
            // Delhi → Agra
            j(TransportMode.BUS,"RedBus Premium","Delhi","Agra",   dt(2024,12,20, 6, 0),dt(2024,12,20, 9,30), 210,450,"AC,WiFi,USB Charging,Blanket",4.3,40),
            j(TransportMode.BUS,"UPSRTC",        "Delhi","Agra",   dt(2024,12,20, 7,30),dt(2024,12,20,11,30), 240,280,"AC",3.8,60),
            j(TransportMode.BUS,"Neeta Tours",   "Delhi","Agra",   dt(2024,12,20,22, 0),dt(2024,12,21, 1,30), 210,520,"AC,Blanket,Snacks,USB Charging",4.5,35),
            // Mumbai → Pune
            j(TransportMode.BUS,"RedBus Premium","Mumbai","Pune",  dt(2024,12,20, 6, 0),dt(2024,12,20, 9,30), 210,380,"AC,WiFi,USB Charging",4.4,45),
            j(TransportMode.BUS,"Shivneri MSRTC","Mumbai","Pune",  dt(2024,12,20, 8, 0),dt(2024,12,20,11, 0), 180,320,"AC,WiFi",4.6,55),
            j(TransportMode.BUS,"Paulo Travels", "Mumbai","Pune",  dt(2024,12,20,21, 0),dt(2024,12,21, 0,30), 210,650,"Volvo AC,Blanket,Snacks,USB Charging,Entertainment",4.7,28),
            // Mumbai → Goa (Bus)
            j(TransportMode.BUS,"Paulo Travels", "Mumbai","Goa",   dt(2024,12,20,18, 0),dt(2024,12,21, 8, 0), 840,950,"Sleeper,AC,Blanket,USB Charging",4.5,40),
            j(TransportMode.BUS,"Kadamba Bus",   "Mumbai","Goa",   dt(2024,12,20,20, 0),dt(2024,12,21,10, 0), 840,750,"AC,Blanket",4.1,50),
            // Bangalore → Chennai
            j(TransportMode.BUS,"KSRTC Airavat", "Bangalore","Chennai",dt(2024,12,20,22, 0),dt(2024,12,21, 5,30),450,680,"AC,Blanket,Snacks",4.4,45),
            j(TransportMode.BUS,"Orange Tours",  "Bangalore","Chennai",dt(2024,12,20,23, 0),dt(2024,12,21, 7, 0),480,550,"AC,Blanket",4.2,50)
        );
    }

    private Journey j(TransportMode mode, String op, String from, String to,
                      LocalDateTime dep, LocalDateTime arr, int mins,
                      int fare, String amenities, double rating, int seats) {
        return Journey.builder()
                .transportMode(mode).operator(op).origin(from).destination(to)
                .departureTime(dep).arrivalTime(arr).durationMinutes(mins)
                .baseFare(new BigDecimal(fare)).amenities(amenities)
                .rating(new BigDecimal(String.valueOf(rating))).availableSeats(seats)
                .build();
    }

    private LocalDateTime dt(int yr, int mo, int day, int hr, int min) {
        return LocalDateTime.of(yr, mo, day, hr, min);
    }
}
