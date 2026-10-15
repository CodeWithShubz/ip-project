-- ============================================================
--  Travel Booking Aggregator – Database Seed / Init Script
--  Run this AFTER the Spring Boot app creates the schema
--  (spring.jpa.hibernate.ddl-auto=update creates the tables)
-- ============================================================

-- Create database if it doesn't exist
CREATE DATABASE IF NOT EXISTS travel_db CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE travel_db;

-- ─── Clear existing seed data ─────────────────────────────────────────────
DELETE FROM bookings;
DELETE FROM journeys;

-- ─── FLIGHTS ──────────────────────────────────────────────────────────────
INSERT INTO journeys (transport_mode, operator, origin, destination,
                      departure_time, arrival_time, duration_minutes,
                      base_fare, available_seats, amenities, rating)
VALUES
-- Delhi → Mumbai
('FLIGHT', 'IndiGo',      'Delhi',   'Mumbai',   '2024-12-20 06:00:00', '2024-12-20 08:05:00', 125, 3499.00, 120, 'Meal,USB Charging,Extra Legroom', 4.2),
('FLIGHT', 'Air India',   'Delhi',   'Mumbai',   '2024-12-20 09:30:00', '2024-12-20 11:40:00', 130, 4999.00, 85,  'Meal,WiFi,Entertainment', 4.5),
('FLIGHT', 'SpiceJet',    'Delhi',   'Mumbai',   '2024-12-20 14:15:00', '2024-12-20 16:25:00', 130, 2799.00, 200, 'Snacks,USB Charging', 3.9),
('FLIGHT', 'Vistara',     'Delhi',   'Mumbai',   '2024-12-20 18:45:00', '2024-12-20 20:55:00', 130, 5499.00, 60,  'Meal,WiFi,Entertainment,Extra Legroom', 4.7),
('FLIGHT', 'GoFirst',     'Delhi',   'Mumbai',   '2024-12-20 22:00:00', '2024-12-21 00:10:00', 130, 2499.00, 150, 'Snacks', 3.7),

-- Mumbai → Delhi
('FLIGHT', 'IndiGo',      'Mumbai',  'Delhi',    '2024-12-21 05:30:00', '2024-12-21 07:35:00', 125, 3299.00, 110, 'Meal,USB Charging', 4.2),
('FLIGHT', 'Air India',   'Mumbai',  'Delhi',    '2024-12-21 10:00:00', '2024-12-21 12:10:00', 130, 4799.00, 90,  'Meal,WiFi,Entertainment', 4.5),
('FLIGHT', 'SpiceJet',    'Mumbai',  'Delhi',    '2024-12-21 15:30:00', '2024-12-21 17:40:00', 130, 2699.00, 180, 'Snacks', 3.8),

-- Delhi → Bangalore
('FLIGHT', 'IndiGo',      'Delhi',   'Bangalore','2024-12-20 07:00:00', '2024-12-20 09:30:00', 150, 4299.00, 140, 'Meal,USB Charging', 4.3),
('FLIGHT', 'Air India',   'Delhi',   'Bangalore','2024-12-20 11:00:00', '2024-12-20 13:30:00', 150, 5799.00, 75,  'Meal,WiFi,Entertainment', 4.6),
('FLIGHT', 'Vistara',     'Delhi',   'Bangalore','2024-12-20 16:00:00', '2024-12-20 18:30:00', 150, 6299.00, 55,  'Meal,WiFi,Entertainment,Extra Legroom', 4.8),

-- Mumbai → Goa
('FLIGHT', 'IndiGo',      'Mumbai',  'Goa',      '2024-12-20 08:00:00', '2024-12-20 09:10:00', 70,  2199.00, 160, 'Snacks,USB Charging', 4.1),
('FLIGHT', 'GoFirst',     'Mumbai',  'Goa',      '2024-12-20 13:00:00', '2024-12-20 14:10:00', 70,  1899.00, 180, 'Snacks', 3.8),

-- ─── TRAINS ────────────────────────────────────────────────────────────────
-- Delhi → Mumbai
('TRAIN',  'Indian Railways', 'Delhi',   'Mumbai',   '2024-12-20 16:35:00', '2024-12-21 08:15:00', 940,  895.00, 300, 'Sleeper,Pantry Car,Bedroll', 4.1),
('TRAIN',  'Indian Railways', 'Delhi',   'Mumbai',   '2024-12-20 23:00:00', '2024-12-21 15:55:00', 955,  1245.00, 250,'AC 3-Tier,Pantry Car,Bedroll', 4.3),
('TRAIN',  'Indian Railways', 'Delhi',   'Mumbai',   '2024-12-20 11:40:00', '2024-12-21 11:45:00', 1445, 2150.00, 80, 'AC 2-Tier,Pantry Car,Bedroll,WiFi', 4.5),

-- Mumbai → Delhi
('TRAIN',  'Indian Railways', 'Mumbai',  'Delhi',    '2024-12-21 06:00:00', '2024-12-21 23:55:00', 1075, 895.00, 350, 'Sleeper,Pantry Car', 4.0),
('TRAIN',  'Indian Railways', 'Mumbai',  'Delhi',    '2024-12-21 17:10:00', '2024-12-22 09:05:00', 955,  1245.00, 220,'AC 3-Tier,Pantry Car,Bedroll', 4.2),

-- Delhi → Bangalore
('TRAIN',  'Indian Railways', 'Delhi',   'Bangalore','2024-12-20 20:00:00', '2024-12-22 12:30:00', 2190, 1650.00, 200,'Sleeper,Pantry Car', 4.0),
('TRAIN',  'Indian Railways', 'Delhi',   'Bangalore','2024-12-20 07:10:00', '2024-12-22 06:45:00', 2375, 3200.00, 60, 'AC 2-Tier,Pantry Car,Bedroll,WiFi', 4.4),

-- Mumbai → Goa
('TRAIN',  'Indian Railways', 'Mumbai',  'Goa',      '2024-12-20 07:10:00', '2024-12-20 17:00:00', 590,  550.00, 400, 'Sleeper,Pantry Car', 4.2),
('TRAIN',  'Indian Railways', 'Mumbai',  'Goa',      '2024-12-20 22:10:00', '2024-12-21 06:00:00', 470,  890.00, 180, 'AC 3-Tier,Pantry Car,Bedroll', 4.4),

-- ─── BUSES ─────────────────────────────────────────────────────────────────
-- Delhi → Agra
('BUS',    'RedBus Premium', 'Delhi',   'Agra',     '2024-12-20 06:00:00', '2024-12-20 09:30:00', 210,  450.00, 40,  'AC,WiFi,USB Charging,Blanket', 4.3),
('BUS',    'UPSRTC',         'Delhi',   'Agra',     '2024-12-20 07:30:00', '2024-12-20 11:30:00', 240,  280.00, 60,  'AC', 3.8),
('BUS',    'Neeta Tours',    'Delhi',   'Agra',     '2024-12-20 22:00:00', '2024-12-21 01:30:00', 210,  520.00, 35,  'AC,Blanket,Snacks,USB Charging', 4.5),

-- Mumbai → Pune
('BUS',    'RedBus Premium', 'Mumbai',  'Pune',     '2024-12-20 06:00:00', '2024-12-20 09:30:00', 210,  380.00, 45,  'AC,WiFi,USB Charging', 4.4),
('BUS',    'Shivneri MSRTC', 'Mumbai',  'Pune',     '2024-12-20 08:00:00', '2024-12-20 11:00:00', 180,  320.00, 55,  'AC,WiFi', 4.6),
('BUS',    'Paulo Travels',  'Mumbai',  'Pune',     '2024-12-20 21:00:00', '2024-12-21 00:30:00', 210,  650.00, 28,  'Volvo AC,Blanket,Snacks,USB Charging,Entertainment', 4.7),

-- Mumbai → Goa (Bus)
('BUS',    'Paulo Travels',  'Mumbai',  'Goa',      '2024-12-20 18:00:00', '2024-12-21 08:00:00', 840,  950.00, 40,  'Sleeper,AC,Blanket,USB Charging', 4.5),
('BUS',    'Kadamba Bus',    'Mumbai',  'Goa',      '2024-12-20 20:00:00', '2024-12-21 10:00:00', 840,  750.00, 50,  'AC,Blanket', 4.1),

-- Bangalore → Chennai
('BUS',    'KSRTC Airavat',  'Bangalore','Chennai', '2024-12-20 22:00:00', '2024-12-21 05:30:00', 450,  680.00, 45,  'AC,Blanket,Snacks', 4.4),
('BUS',    'Orange Tours',   'Bangalore','Chennai', '2024-12-20 23:00:00', '2024-12-21 07:00:00', 480,  550.00, 50,  'AC,Blanket', 4.2);
