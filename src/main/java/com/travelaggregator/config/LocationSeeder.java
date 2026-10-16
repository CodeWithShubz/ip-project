package com.travelaggregator.config;

import com.travelaggregator.entity.Location;
import com.travelaggregator.repository.LocationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Seeds the locations table with Indian cities, airports, railway stations,
 * and bus terminals for autocomplete. Only runs if the table is empty.
 */
@Component
@RequiredArgsConstructor
@Slf4j
@Order(1) // Run before DataSeeder
public class LocationSeeder implements CommandLineRunner {

    private final LocationRepository locationRepository;

    @Override
    public void run(String... args) {
        if (locationRepository.count() > 0) {
            log.info("Locations table already has {} entries. Skipping.", locationRepository.count());
            return;
        }

        log.info("Seeding location data…");
        locationRepository.saveAll(buildLocations());
        log.info("✅ Seeded {} locations.", locationRepository.count());
    }

    private List<Location> buildLocations() {
        return List.of(
            // ─── Delhi ────────────────────────────────────────
            loc("Delhi", "Indira Gandhi International Airport", "DEL", "AIRPORT"),
            loc("Delhi", "New Delhi Railway Station", "NDLS", "RAILWAY_STATION"),
            loc("Delhi", "Hazrat Nizamuddin Railway Station", "NZM", "RAILWAY_STATION"),
            loc("Delhi", "Anand Vihar ISBT", null, "BUS_TERMINAL"),
            loc("Delhi", "Kashmere Gate ISBT", null, "BUS_TERMINAL"),

            // ─── Mumbai ───────────────────────────────────────
            loc("Mumbai", "Chhatrapati Shivaji Maharaj International Airport", "BOM", "AIRPORT"),
            loc("Mumbai", "Mumbai Central Railway Station", "MMCT", "RAILWAY_STATION"),
            loc("Mumbai", "Chhatrapati Shivaji Terminus", "CSMT", "RAILWAY_STATION"),
            loc("Mumbai", "Dadar Bus Terminal", null, "BUS_TERMINAL"),

            // ─── Bangalore ────────────────────────────────────
            loc("Bangalore", "Kempegowda International Airport", "BLR", "AIRPORT"),
            loc("Bangalore", "Bangalore City Railway Station", "SBC", "RAILWAY_STATION"),
            loc("Bangalore", "Krantivira Sangolli Rayanna Station", "KSR", "RAILWAY_STATION"),
            loc("Bangalore", "Majestic Bus Station", null, "BUS_TERMINAL"),

            // ─── Chennai ──────────────────────────────────────
            loc("Chennai", "Chennai International Airport", "MAA", "AIRPORT"),
            loc("Chennai", "Chennai Central Railway Station", "MAS", "RAILWAY_STATION"),
            loc("Chennai", "CMBT Bus Terminal", null, "BUS_TERMINAL"),

            // ─── Goa ──────────────────────────────────────────
            loc("Goa", "Dabolim Airport", "GOI", "AIRPORT"),
            loc("Goa", "Manohar International Airport", "GOX", "AIRPORT"),
            loc("Goa", "Madgaon Railway Station", "MAO", "RAILWAY_STATION"),
            loc("Goa", "Kadamba Bus Terminal Panaji", null, "BUS_TERMINAL"),

            // ─── Pune ─────────────────────────────────────────
            loc("Pune", "Pune Airport", "PNQ", "AIRPORT"),
            loc("Pune", "Pune Junction Railway Station", "PUNE", "RAILWAY_STATION"),
            loc("Pune", "Swargate Bus Station", null, "BUS_TERMINAL"),
            loc("Pune", "Shivajinagar Bus Station", null, "BUS_TERMINAL"),

            // ─── Agra ─────────────────────────────────────────
            loc("Agra", "Agra Airport", "AGR", "AIRPORT"),
            loc("Agra", "Agra Cantt Railway Station", "AGC", "RAILWAY_STATION"),
            loc("Agra", "ISBT Agra", null, "BUS_TERMINAL"),

            // ─── Hyderabad ────────────────────────────────────
            loc("Hyderabad", "Rajiv Gandhi International Airport", "HYD", "AIRPORT"),
            loc("Hyderabad", "Secunderabad Junction", "SC", "RAILWAY_STATION"),
            loc("Hyderabad", "MGBS Bus Station", null, "BUS_TERMINAL"),

            // ─── Kolkata ──────────────────────────────────────
            loc("Kolkata", "Netaji Subhas Chandra Bose International Airport", "CCU", "AIRPORT"),
            loc("Kolkata", "Howrah Junction", "HWH", "RAILWAY_STATION"),
            loc("Kolkata", "Sealdah Station", "SDAH", "RAILWAY_STATION"),
            loc("Kolkata", "Esplanade Bus Terminal", null, "BUS_TERMINAL"),

            // ─── Jaipur ──────────────────────────────────────
            loc("Jaipur", "Jaipur International Airport", "JAI", "AIRPORT"),
            loc("Jaipur", "Jaipur Junction", "JP", "RAILWAY_STATION"),
            loc("Jaipur", "Sindhi Camp Bus Terminal", null, "BUS_TERMINAL"),

            // ─── Ahmedabad ────────────────────────────────────
            loc("Ahmedabad", "Sardar Vallabhbhai Patel International Airport", "AMD", "AIRPORT"),
            loc("Ahmedabad", "Ahmedabad Junction", "ADI", "RAILWAY_STATION"),
            loc("Ahmedabad", "Geeta Mandir Bus Station", null, "BUS_TERMINAL"),

            // ─── Lucknow ──────────────────────────────────────
            loc("Lucknow", "Chaudhary Charan Singh International Airport", "LKO", "AIRPORT"),
            loc("Lucknow", "Lucknow Junction", "LKO", "RAILWAY_STATION"),
            loc("Lucknow", "Alambagh Bus Station", null, "BUS_TERMINAL"),

            // ─── Varanasi ─────────────────────────────────────
            loc("Varanasi", "Lal Bahadur Shastri Airport", "VNS", "AIRPORT"),
            loc("Varanasi", "Varanasi Junction", "BSB", "RAILWAY_STATION"),

            // ─── Kochi ────────────────────────────────────────
            loc("Kochi", "Cochin International Airport", "COK", "AIRPORT"),
            loc("Kochi", "Ernakulam Junction", "ERS", "RAILWAY_STATION"),
            loc("Kochi", "KSRTC Bus Terminal Ernakulam", null, "BUS_TERMINAL"),

            // ─── Chandigarh ───────────────────────────────────
            loc("Chandigarh", "Chandigarh Airport", "IXC", "AIRPORT"),
            loc("Chandigarh", "Chandigarh Railway Station", "CDG", "RAILWAY_STATION"),
            loc("Chandigarh", "ISBT Sector 43", null, "BUS_TERMINAL")
        );
    }

    private Location loc(String city, String stationName, String code, String type) {
        return Location.builder()
                .city(city)
                .stationName(stationName)
                .stationCode(code)
                .locationType(type)
                .build();
    }
}
