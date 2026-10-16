package com.travelaggregator.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Lightweight entity for location autocomplete suggestions.
 * Decoupled from Journey records so autocomplete works even with an empty journeys table.
 */
@Entity
@Table(name = "locations", indexes = {
        @Index(name = "idx_location_city", columnList = "city"),
        @Index(name = "idx_location_search", columnList = "city, station_name")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** City name, e.g. "Delhi" */
    @Column(nullable = false, length = 100)
    private String city;

    /** Station / Airport / Terminal name, e.g. "Indira Gandhi International Airport" */
    @Column(name = "station_name", length = 200)
    private String stationName;

    /** Station code, e.g. "DEL", "NDLS" */
    @Column(name = "station_code", length = 20)
    private String stationCode;

    /** Type: AIRPORT, RAILWAY_STATION, BUS_TERMINAL */
    @Column(name = "location_type", length = 30)
    private String locationType;
}
