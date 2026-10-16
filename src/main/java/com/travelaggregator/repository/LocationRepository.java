package com.travelaggregator.repository;

import com.travelaggregator.entity.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repository for Location autocomplete lookups.
 */
@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    /**
     * Case-insensitive search by city or station name containing the query.
     * Returns up to 15 matches.
     */
    @Query("""
            SELECT l FROM Location l
            WHERE LOWER(l.city) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(l.stationName) LIKE LOWER(CONCAT('%', :query, '%'))
               OR LOWER(l.stationCode) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY l.city, l.stationName
            """)
    List<Location> searchByQuery(@Param("query") String query);
}
