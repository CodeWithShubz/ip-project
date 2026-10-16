-- ============================================================
--  Travel Booking Aggregator – Database Init Script
--  Creates the database and grants minimum privileges.
--
--  NOTE: Demo journey data is now seeded dynamically by the
--  Java DataSeeder (config/DataSeeder.java) with dates relative
--  to LocalDate.now(). This file no longer contains INSERT
--  statements for journeys to avoid hardcoded stale dates.
--
--  To reset demo data: stop the app, run the optional commands
--  below, then restart:
--
--    -- ⚠️ DESTRUCTIVE: removes all bookings and journeys
--    -- DELETE FROM bookings;
--    -- DELETE FROM journeys;
--    -- DELETE FROM locations;
--
-- ============================================================

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS travel_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE travel_db;
