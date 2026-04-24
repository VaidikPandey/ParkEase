-- ParkEase Database Initialization
-- Creates one database per microservice

SELECT 'CREATE DATABASE auth_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'auth_db')\gexec
SELECT 'CREATE DATABASE parking_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'parking_db')\gexec
SELECT 'CREATE DATABASE booking_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'booking_db')\gexec
SELECT 'CREATE DATABASE payment_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'payment_db')\gexec
SELECT 'CREATE DATABASE notification_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'notification_db')\gexec
SELECT 'CREATE DATABASE analytics_db' WHERE NOT EXISTS (SELECT FROM pg_database WHERE datname = 'analytics_db')\gexec
