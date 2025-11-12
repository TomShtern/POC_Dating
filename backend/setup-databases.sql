-- ============================================
-- POC Dating Application - Database Setup
-- ============================================
--
-- This script creates all required databases for the POC Dating microservices.
-- Run this script as the postgres user.
--
-- USAGE:
--   psql -U postgres -f setup-databases.sql
--
-- OR connect to psql and run:
--   sudo -u postgres psql
--   \i /path/to/setup-databases.sql
--
-- ============================================

-- Create databases for each microservice
CREATE DATABASE dating_users;
CREATE DATABASE dating_matches;
CREATE DATABASE dating_chat;
CREATE DATABASE dating_recommendations;

-- Display created databases
\echo '============================================'
\echo 'Databases created successfully!'
\echo '============================================'
\echo ''
\echo 'Created databases:'
\echo '  - dating_users           (User Service - port 8081)'
\echo '  - dating_matches         (Match Service - port 8082)'
\echo '  - dating_chat            (Chat Service - port 8083)'
\echo '  - dating_recommendations (Recommendation Service - port 8084)'
\echo ''
\echo '============================================'
\echo 'Next steps:'
\echo '  1. Grant privileges (already granted to postgres user by default)'
\echo '  2. Start each microservice with: mvn spring-boot:run'
\echo '  3. Services will auto-create tables on startup (DDL-auto: update)'
\echo '============================================'

-- Grant all privileges to postgres user (usually already has them, but just to be explicit)
GRANT ALL PRIVILEGES ON DATABASE dating_users TO postgres;
GRANT ALL PRIVILEGES ON DATABASE dating_matches TO postgres;
GRANT ALL PRIVILEGES ON DATABASE dating_chat TO postgres;
GRANT ALL PRIVILEGES ON DATABASE dating_recommendations TO postgres;

-- Optional: Create a separate development user with same privileges
-- Uncomment the following lines if you want to create a dedicated dev user:

-- CREATE USER dating_dev WITH PASSWORD 'dating_dev_password';
-- GRANT ALL PRIVILEGES ON DATABASE dating_users TO dating_dev;
-- GRANT ALL PRIVILEGES ON DATABASE dating_matches TO dating_dev;
-- GRANT ALL PRIVILEGES ON DATABASE dating_chat TO dating_dev;
-- GRANT ALL PRIVILEGES ON DATABASE dating_recommendations TO dating_dev;
--
-- \echo ''
-- \echo 'Development user created:'
-- \echo '  Username: dating_dev'
-- \echo '  Password: dating_dev_password'
-- \echo ''
-- \echo 'To use the dev user, set environment variables:'
-- \echo '  export POSTGRES_USER=dating_dev'
-- \echo '  export DB_PASSWORD=dating_dev_password'

-- List all databases to verify
\l

\echo ''
\echo '============================================'
\echo 'Setup complete! You can now start the services.'
\echo '============================================'
