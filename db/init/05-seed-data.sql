-- POC Dating Seed Data
-- Version: 2.0
--
-- PURPOSE: Realistic test data for development and testing
--
-- CONTENTS:
-- - 50+ diverse users with varied demographics
-- - User preferences for each user
-- - Photos for each user
-- - 200+ swipes creating realistic match scenarios
-- - Active matches with conversations
-- - Recommendation scores
--
-- NOTE: Passwords are all 'Password123!' hashed with BCrypt

-- ========================================
-- USERS (50 test users)
-- Password for all: 'Password123!' - BCrypt hash below
-- ========================================
INSERT INTO users (id, email, username, password_hash, first_name, last_name, date_of_birth, gender, bio, profile_picture_url, location_lat, location_lng, is_verified, is_premium, status, last_active)
VALUES
    -- Women (25 users)
    ('11111111-1111-1111-1111-111111111111', 'emma@test.com', 'emma_wilson', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Emma', 'Wilson', '1995-03-15', 'FEMALE', 'Adventure seeker and coffee enthusiast. Love hiking on weekends and trying new restaurants.', 'https://randomuser.me/api/portraits/women/1.jpg', 40.7128, -74.0060, true, false, 'ACTIVE', NOW() - INTERVAL '2 hours'),
    ('11111111-1111-1111-1111-111111111112', 'sophia@test.com', 'sophia_chen', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Sophia', 'Chen', '1992-07-22', 'FEMALE', 'Software engineer by day, yoga instructor by night. Looking for someone to share laughs with.', 'https://randomuser.me/api/portraits/women/2.jpg', 40.7580, -73.9855, true, true, 'ACTIVE', NOW() - INTERVAL '30 minutes'),
    ('11111111-1111-1111-1111-111111111113', 'olivia@test.com', 'olivia_brown', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Olivia', 'Brown', '1998-11-08', 'FEMALE', 'Artist and dog mom. My golden retriever is my best friend and you should probably like dogs.', 'https://randomuser.me/api/portraits/women/3.jpg', 40.7484, -73.9857, false, false, 'ACTIVE', NOW() - INTERVAL '1 day'),
    ('11111111-1111-1111-1111-111111111114', 'ava@test.com', 'ava_garcia', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Ava', 'Garcia', '1990-05-30', 'FEMALE', 'Marketing manager who loves salsa dancing and cooking Italian food.', 'https://randomuser.me/api/portraits/women/4.jpg', 40.7282, -73.7949, true, false, 'ACTIVE', NOW() - INTERVAL '4 hours'),
    ('11111111-1111-1111-1111-111111111115', 'isabella@test.com', 'bella_martinez', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Isabella', 'Martinez', '1994-09-12', 'FEMALE', 'Nurse with a passion for travel. Visited 30 countries and counting!', 'https://randomuser.me/api/portraits/women/5.jpg', 40.6892, -74.0445, true, true, 'ACTIVE', NOW() - INTERVAL '15 minutes'),
    ('11111111-1111-1111-1111-111111111116', 'mia@test.com', 'mia_johnson', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Mia', 'Johnson', '1996-02-28', 'FEMALE', 'Fitness trainer and meal prep queen. Looking for a gym partner and life partner.', 'https://randomuser.me/api/portraits/women/6.jpg', 40.7614, -73.9776, false, true, 'ACTIVE', NOW() - INTERVAL '6 hours'),
    ('11111111-1111-1111-1111-111111111117', 'charlotte@test.com', 'charlotte_lee', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Charlotte', 'Lee', '1991-12-05', 'FEMALE', 'Bookworm and wine enthusiast. Will definitely judge you by your book collection.', 'https://randomuser.me/api/portraits/women/7.jpg', 40.7505, -73.9934, true, false, 'ACTIVE', NOW() - INTERVAL '3 hours'),
    ('11111111-1111-1111-1111-111111111118', 'amelia@test.com', 'amelia_kim', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Amelia', 'Kim', '1993-08-19', 'FEMALE', 'Data scientist who loves board games and craft beer. Let''s play Catan!', 'https://randomuser.me/api/portraits/women/8.jpg', 40.7282, -73.7949, true, true, 'ACTIVE', NOW() - INTERVAL '45 minutes'),
    ('11111111-1111-1111-1111-111111111119', 'harper@test.com', 'harper_davis', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Harper', 'Davis', '1997-04-03', 'FEMALE', 'Photographer and cat lover. Looking for someone to explore the city with.', 'https://randomuser.me/api/portraits/women/9.jpg', 40.7128, -74.0060, false, false, 'ACTIVE', NOW() - INTERVAL '8 hours'),
    ('11111111-1111-1111-1111-111111111120', 'evelyn@test.com', 'evelyn_rodriguez', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Evelyn', 'Rodriguez', '1989-10-25', 'FEMALE', 'Lawyer by profession, chef at heart. Make a mean paella.', 'https://randomuser.me/api/portraits/women/10.jpg', 40.7580, -73.9855, true, false, 'ACTIVE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111121', 'luna@test.com', 'luna_patel', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Luna', 'Patel', '1994-06-14', 'FEMALE', 'Startup founder and meditation enthusiast. Work hard, zen harder.', 'https://randomuser.me/api/portraits/women/11.jpg', 40.7484, -73.9857, true, true, 'ACTIVE', NOW() - INTERVAL '1 hour'),
    ('11111111-1111-1111-1111-111111111122', 'chloe@test.com', 'chloe_nguyen', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Chloe', 'Nguyen', '1996-01-07', 'FEMALE', 'UX designer and bubble tea addict. Can talk about fonts for hours.', 'https://randomuser.me/api/portraits/women/12.jpg', 40.7282, -73.7949, false, false, 'ACTIVE', NOW() - INTERVAL '5 hours'),
    ('11111111-1111-1111-1111-111111111123', 'penelope@test.com', 'penny_white', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Penelope', 'White', '1992-03-20', 'FEMALE', 'Elementary teacher who loves camping and stargazing.', 'https://randomuser.me/api/portraits/women/13.jpg', 40.6892, -74.0445, true, false, 'ACTIVE', NOW() - INTERVAL '7 hours'),
    ('11111111-1111-1111-1111-111111111124', 'layla@test.com', 'layla_harris', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Layla', 'Harris', '1995-11-30', 'FEMALE', 'Music producer and vinyl collector. My apartment is basically a record store.', 'https://randomuser.me/api/portraits/women/14.jpg', 40.7614, -73.9776, true, true, 'ACTIVE', NOW() - INTERVAL '20 minutes'),
    ('11111111-1111-1111-1111-111111111125', 'riley@test.com', 'riley_clark', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Riley', 'Clark', '1993-07-08', 'FEMALE', 'Veterinarian and marathon runner. Yes, I will want to meet your pets.', 'https://randomuser.me/api/portraits/women/15.jpg', 40.7505, -73.9934, false, false, 'ACTIVE', NOW() - INTERVAL '12 hours'),
    ('11111111-1111-1111-1111-111111111126', 'zoey@test.com', 'zoey_lewis', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Zoey', 'Lewis', '1990-09-15', 'FEMALE', 'Architect with a love for brutalist buildings and good coffee.', 'https://randomuser.me/api/portraits/women/16.jpg', 40.7282, -73.7949, true, false, 'SUSPENDED', NOW() - INTERVAL '4 hours'),
    ('11111111-1111-1111-1111-111111111127', 'nora@test.com', 'nora_walker', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Nora', 'Walker', '1997-02-12', 'FEMALE', 'Grad student studying marine biology. Ask me about octopuses!', 'https://randomuser.me/api/portraits/women/17.jpg', 40.7128, -74.0060, true, true, 'SUSPENDED', NOW() - INTERVAL '1 hour'),
    ('11111111-1111-1111-1111-111111111128', 'lily@test.com', 'lily_hall', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Lily', 'Hall', '1994-05-25', 'FEMALE', 'Product manager and amateur baker. Will bring cookies to first date.', 'https://randomuser.me/api/portraits/women/18.jpg', 40.7580, -73.9855, false, false, 'ACTIVE', NOW() - INTERVAL '9 hours'),
    ('11111111-1111-1111-1111-111111111129', 'eleanor@test.com', 'ellie_young', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Eleanor', 'Young', '1991-08-03', 'FEMALE', 'Financial analyst who loves jazz and late-night diners.', 'https://randomuser.me/api/portraits/women/19.jpg', 40.7484, -73.9857, true, false, 'ACTIVE', NOW() - INTERVAL '6 hours'),
    ('11111111-1111-1111-1111-111111111130', 'hazel@test.com', 'hazel_allen', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Hazel', 'Allen', '1996-12-18', 'FEMALE', 'Social worker and pottery enthusiast. Yes, like in Ghost.', 'https://randomuser.me/api/portraits/women/20.jpg', 40.7282, -73.7949, true, true, 'ACTIVE', NOW() - INTERVAL '30 minutes'),
    ('11111111-1111-1111-1111-111111111131', 'violet@test.com', 'violet_king', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Violet', 'King', '1993-04-22', 'FEMALE', 'Graphic designer and plant parent. 47 plants and counting.', 'https://randomuser.me/api/portraits/women/21.jpg', 40.6892, -74.0445, false, false, 'ACTIVE', NOW() - INTERVAL '11 hours'),
    ('11111111-1111-1111-1111-111111111132', 'aurora@test.com', 'aurora_wright', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Aurora', 'Wright', '1989-10-09', 'FEMALE', 'Doctor and amateur astronomer. Let''s stargaze together.', 'https://randomuser.me/api/portraits/women/22.jpg', 40.7614, -73.9776, true, false, 'ACTIVE', NOW() - INTERVAL '3 hours'),
    ('11111111-1111-1111-1111-111111111133', 'savannah@test.com', 'savannah_lopez', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Savannah', 'Lopez', '1995-06-27', 'FEMALE', 'Interior designer who loves flea markets and DIY projects.', 'https://randomuser.me/api/portraits/women/23.jpg', 40.7505, -73.9934, true, true, 'ACTIVE', NOW() - INTERVAL '2 hours'),
    ('11111111-1111-1111-1111-111111111134', 'audrey@test.com', 'audrey_hill', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Audrey', 'Hill', '1992-01-14', 'FEMALE', 'Journalist and podcast host. Great listener, even better storyteller.', 'https://randomuser.me/api/portraits/women/24.jpg', 40.7282, -73.7949, false, false, 'ACTIVE', NOW() - INTERVAL '8 hours'),
    ('11111111-1111-1111-1111-111111111135', 'brooklyn@test.com', 'brooklyn_scott', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Brooklyn', 'Scott', '1997-09-02', 'FEMALE', 'Fashion stylist and thrift store queen. Will help you upgrade your wardrobe.', 'https://randomuser.me/api/portraits/women/25.jpg', 40.7128, -74.0060, true, false, 'ACTIVE', NOW() - INTERVAL '5 hours'),

    -- Men (25 users)
    ('22222222-2222-2222-2222-222222222221', 'james@test.com', 'james_smith', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'James', 'Smith', '1991-05-18', 'MALE', 'Software developer and amateur chef. My pasta game is strong.', 'https://randomuser.me/api/portraits/men/1.jpg', 40.7128, -74.0060, true, false, 'ACTIVE', NOW() - INTERVAL '1 hour'),
    ('22222222-2222-2222-2222-222222222222', 'liam@test.com', 'liam_johnson', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Liam', 'Johnson', '1993-09-24', 'MALE', 'Investment banker who mountain bikes on weekends. Work hard, play harder.', 'https://randomuser.me/api/portraits/men/2.jpg', 40.7580, -73.9855, true, true, 'ACTIVE', NOW() - INTERVAL '45 minutes'),
    ('22222222-2222-2222-2222-222222222223', 'noah@test.com', 'noah_williams', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Noah', 'Williams', '1990-02-11', 'MALE', 'Architect and coffee snob. Let me take you to the best hidden cafes.', 'https://randomuser.me/api/portraits/men/3.jpg', 40.7484, -73.9857, false, true, 'ACTIVE', NOW() - INTERVAL '3 hours'),
    ('22222222-2222-2222-2222-222222222224', 'william@test.com', 'will_brown', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'William', 'Brown', '1995-07-06', 'MALE', 'Physical therapist and basketball player. 6''2" if that matters.', 'https://randomuser.me/api/portraits/men/4.jpg', 40.7282, -73.7949, true, false, 'ACTIVE', NOW() - INTERVAL '2 hours'),
    ('22222222-2222-2222-2222-222222222225', 'oliver@test.com', 'oliver_jones', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Oliver', 'Jones', '1992-11-29', 'MALE', 'Music teacher and vinyl enthusiast. Will make you a playlist.', 'https://randomuser.me/api/portraits/men/5.jpg', 40.6892, -74.0445, true, true, 'ACTIVE', NOW() - INTERVAL '30 minutes'),
    ('22222222-2222-2222-2222-222222222226', 'elijah@test.com', 'elijah_garcia', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Elijah', 'Garcia', '1994-04-15', 'MALE', 'Emergency room doctor and amateur stand-up comedian. Dark humor included.', 'https://randomuser.me/api/portraits/men/6.jpg', 40.7614, -73.9776, false, true, 'ACTIVE', NOW() - INTERVAL '6 hours'),
    ('22222222-2222-2222-2222-222222222227', 'lucas@test.com', 'lucas_miller', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Lucas', 'Miller', '1989-08-21', 'MALE', 'Entrepreneur and dog dad to two huskies. They come first, sorry not sorry.', 'https://randomuser.me/api/portraits/men/7.jpg', 40.7505, -73.9934, true, false, 'ACTIVE', NOW() - INTERVAL '4 hours'),
    ('22222222-2222-2222-2222-222222222228', 'mason@test.com', 'mason_davis', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Mason', 'Davis', '1996-01-03', 'MALE', 'Data analyst and hiking enthusiast. Let''s conquer some trails together.', 'https://randomuser.me/api/portraits/men/8.jpg', 40.7282, -73.7949, true, true, 'ACTIVE', NOW() - INTERVAL '1 hour'),
    ('22222222-2222-2222-2222-222222222229', 'ethan@test.com', 'ethan_rodriguez', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Ethan', 'Rodriguez', '1993-06-17', 'MALE', 'Chef at a farm-to-table restaurant. Will cook you dinner on the third date.', 'https://randomuser.me/api/portraits/men/9.jpg', 40.7128, -74.0060, false, false, 'ACTIVE', NOW() - INTERVAL '9 hours'),
    ('22222222-2222-2222-2222-222222222230', 'alexander@test.com', 'alex_martinez', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Alexander', 'Martinez', '1991-12-08', 'MALE', 'Lawyer and salsa dancer. Let''s hit the dance floor.', 'https://randomuser.me/api/portraits/men/10.jpg', 40.7580, -73.9855, true, false, 'ACTIVE', NOW() - INTERVAL '5 hours'),
    ('22222222-2222-2222-2222-222222222231', 'henry@test.com', 'henry_anderson', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Henry', 'Anderson', '1994-03-26', 'MALE', 'Mechanical engineer and woodworking hobbyist. Built my own furniture.', 'https://randomuser.me/api/portraits/men/11.jpg', 40.7484, -73.9857, true, true, 'ACTIVE', NOW() - INTERVAL '2 hours'),
    ('22222222-2222-2222-2222-222222222232', 'sebastian@test.com', 'seb_taylor', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Sebastian', 'Taylor', '1990-10-14', 'MALE', 'Marketing director and photography enthusiast. Love capturing moments.', 'https://randomuser.me/api/portraits/men/12.jpg', 40.7282, -73.7949, false, false, 'ACTIVE', NOW() - INTERVAL '7 hours'),
    ('22222222-2222-2222-2222-222222222233', 'jack@test.com', 'jack_thomas', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Jack', 'Thomas', '1995-05-02', 'MALE', 'Graphic designer and sneakerhead. My shoe collection is out of control.', 'https://randomuser.me/api/portraits/men/13.jpg', 40.6892, -74.0445, true, false, 'ACTIVE', NOW() - INTERVAL '3 hours'),
    ('22222222-2222-2222-2222-222222222234', 'aiden@test.com', 'aiden_moore', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Aiden', 'Moore', '1992-08-30', 'MALE', 'Pilot and scuba diver. Let''s explore the world together.', 'https://randomuser.me/api/portraits/men/14.jpg', 40.7614, -73.9776, true, true, 'ACTIVE', NOW() - INTERVAL '1 hour'),
    ('22222222-2222-2222-2222-222222222235', 'owen@test.com', 'owen_jackson', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Owen', 'Jackson', '1997-02-19', 'MALE', 'Med student and guitar player. Studying hard but always down for live music.', 'https://randomuser.me/api/portraits/men/15.jpg', 40.7505, -73.9934, false, false, 'ACTIVE', NOW() - INTERVAL '8 hours'),
    ('22222222-2222-2222-2222-222222222236', 'samuel@test.com', 'sam_martin', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Samuel', 'Martin', '1993-11-05', 'MALE', 'Environmental scientist and rock climber. Saving the planet one climb at a time.', 'https://randomuser.me/api/portraits/men/16.jpg', 40.7282, -73.7949, true, false, 'ACTIVE', NOW() - INTERVAL '4 hours'),
    ('22222222-2222-2222-2222-222222222237', 'ryan@test.com', 'ryan_lee', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Ryan', 'Lee', '1991-04-12', 'MALE', 'Video game developer and board game collector. Game night every Friday.', 'https://randomuser.me/api/portraits/men/17.jpg', 40.7128, -74.0060, true, true, 'ACTIVE', NOW() - INTERVAL '2 hours'),
    ('22222222-2222-2222-2222-222222222238', 'nathan@test.com', 'nathan_perez', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Nathan', 'Perez', '1994-07-28', 'MALE', 'Personal trainer and nutrition coach. Let''s get healthy together.', 'https://randomuser.me/api/portraits/men/18.jpg', 40.7580, -73.9855, false, false, 'ACTIVE', NOW() - INTERVAL '6 hours'),
    ('22222222-2222-2222-2222-222222222239', 'caleb@test.com', 'caleb_thompson', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Caleb', 'Thompson', '1990-01-22', 'MALE', 'Film editor and cinema enthusiast. Can debate Marvel vs DC all day.', 'https://randomuser.me/api/portraits/men/19.jpg', 40.7484, -73.9857, true, false, 'ACTIVE', NOW() - INTERVAL '5 hours'),
    ('22222222-2222-2222-2222-222222222240', 'isaac@test.com', 'isaac_white', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Isaac', 'White', '1996-09-09', 'MALE', 'Software architect and podcast host. Let''s talk tech over coffee.', 'https://randomuser.me/api/portraits/men/20.jpg', 40.7282, -73.7949, true, true, 'ACTIVE', NOW() - INTERVAL '1 hour'),
    ('22222222-2222-2222-2222-222222222241', 'joshua@test.com', 'josh_harris', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Joshua', 'Harris', '1993-03-17', 'MALE', 'Teacher and trivia night champion. Full of random facts.', 'https://randomuser.me/api/portraits/men/21.jpg', 40.6892, -74.0445, false, false, 'ACTIVE', NOW() - INTERVAL '10 hours'),
    ('22222222-2222-2222-2222-222222222242', 'connor@test.com', 'connor_sanchez', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Connor', 'Sanchez', '1992-06-24', 'MALE', 'Accountant and home brewer. Will share my craft beers with you.', 'https://randomuser.me/api/portraits/men/22.jpg', 40.7614, -73.9776, true, false, 'ACTIVE', NOW() - INTERVAL '3 hours'),
    ('22222222-2222-2222-2222-222222222243', 'matthew@test.com', 'matt_clark', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Matthew', 'Clark', '1995-12-01', 'MALE', 'Dentist and marathon runner. Great at small talk while you can''t respond.', 'https://randomuser.me/api/portraits/men/23.jpg', 40.7505, -73.9934, true, true, 'ACTIVE', NOW() - INTERVAL '4 hours'),
    ('22222222-2222-2222-2222-222222222244', 'daniel@test.com', 'daniel_ramirez', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Daniel', 'Ramirez', '1991-08-13', 'MALE', 'Civil engineer and volunteer firefighter. Building and protecting communities.', 'https://randomuser.me/api/portraits/men/24.jpg', 40.7282, -73.7949, false, false, 'DELETED', NOW() - INTERVAL '7 hours'),
    ('22222222-2222-2222-2222-222222222245', 'andrew@test.com', 'andrew_lewis', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Andrew', 'Lewis', '1994-10-06', 'MALE', 'Psychologist and bookworm. Great listener, can recommend good reads.', 'https://randomuser.me/api/portraits/men/25.jpg', 40.7128, -74.0060, true, false, 'PENDING', NOW() - INTERVAL '2 hours'),

    -- Geographic diversity: Los Angeles users
    ('44444444-4444-4444-4444-444444444441', 'maya_la@test.com', 'maya_la', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Maya', 'Anderson', '1993-03-18', 'FEMALE', 'LA-based actress and beach volleyball player. Looking for adventure on the west coast.', 'https://randomuser.me/api/portraits/women/31.jpg', 34.05, -118.24, true, false, 'ACTIVE', NOW() - INTERVAL '2 hours'),
    ('44444444-4444-4444-4444-444444444442', 'tyler_la@test.com', 'tyler_la', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Tyler', 'Brooks', '1990-07-22', 'MALE', 'Music producer in LA. Love hiking Runyon Canyon and finding new taco spots.', 'https://randomuser.me/api/portraits/men/31.jpg', 34.05, -118.24, false, true, 'ACTIVE', NOW() - INTERVAL '3 hours'),

    -- Geographic diversity: Chicago users
    ('44444444-4444-4444-4444-444444444443', 'sarah_chi@test.com', 'sarah_chi', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Sarah', 'Mitchell', '1995-11-05', 'FEMALE', 'Chicago native who loves deep dish pizza and jazz clubs. Software engineer by day.', 'https://randomuser.me/api/portraits/women/32.jpg', 41.88, -87.63, true, true, 'ACTIVE', NOW() - INTERVAL '1 hour'),
    ('44444444-4444-4444-4444-444444444444', 'marcus_chi@test.com', 'marcus_chi', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Marcus', 'Green', '1992-04-14', 'MALE', 'Sports analyst and Cubs superfan. Always down for a game at Wrigley Field.', 'https://randomuser.me/api/portraits/men/32.jpg', 41.88, -87.63, false, false, 'ACTIVE', NOW() - INTERVAL '4 hours'),

    -- Geographic diversity: Austin users
    ('44444444-4444-4444-4444-444444444445', 'alex_atx@test.com', 'alex_atx', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Alex', 'Rivera', '1994-08-30', 'NON_BINARY', 'Austin tech worker and live music enthusiast. They/them. Keep Austin weird!', 'https://randomuser.me/api/portraits/lego/1.jpg', 30.27, -97.74, true, false, 'ACTIVE', NOW() - INTERVAL '2 hours'),
    ('44444444-4444-4444-4444-444444444446', 'jordan_atx@test.com', 'jordan_atx', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Jordan', 'Lee', '1991-12-20', 'NON_BINARY', 'Austin-based artist and food truck connoisseur. Pronouns: they/them.', 'https://randomuser.me/api/portraits/lego/2.jpg', 30.27, -97.74, false, true, 'ACTIVE', NOW() - INTERVAL '5 hours')
ON CONFLICT (email) DO NOTHING;

-- ========================================
-- USER PREFERENCES
-- ========================================
INSERT INTO user_preferences (user_id, min_age, max_age, max_distance_km, interested_in, interests, notification_enabled)
SELECT
    id,
    CASE
        WHEN EXTRACT(YEAR FROM AGE(date_of_birth)) < 30 THEN 21
        ELSE 25
    END,
    CASE
        WHEN EXTRACT(YEAR FROM AGE(date_of_birth)) < 30 THEN 40
        ELSE 50
    END,
    CASE WHEN random() > 0.5 THEN 25 ELSE 50 END,
    CASE
        WHEN gender = 'FEMALE' THEN 'MALE'
        WHEN gender = 'MALE' THEN 'FEMALE'
        ELSE 'BOTH'
    END,
    CASE
        WHEN random() < 0.33 THEN ARRAY['hiking', 'travel', 'photography', 'cooking']
        WHEN random() < 0.66 THEN ARRAY['music', 'movies', 'reading', 'yoga']
        ELSE ARRAY['fitness', 'food', 'art', 'technology']
    END,
    true
FROM users
ON CONFLICT (user_id) DO NOTHING;

-- ========================================
-- PHOTOS (2-5 photos per user)
-- ========================================
INSERT INTO photos (user_id, url, thumbnail_url, display_order, is_primary, is_verified, moderation_status)
SELECT
    id,
    profile_picture_url,
    profile_picture_url,
    0,
    true,
    is_verified,
    'APPROVED'
FROM users
ON CONFLICT DO NOTHING;

-- Add secondary photos
INSERT INTO photos (user_id, url, thumbnail_url, display_order, is_primary, moderation_status)
SELECT
    id,
    REPLACE(profile_picture_url, '/1.jpg', '/2.jpg'),
    REPLACE(profile_picture_url, '/1.jpg', '/2.jpg'),
    1,
    false,
    'APPROVED'
FROM users
WHERE random() > 0.3
ON CONFLICT DO NOTHING;

-- ========================================
-- SWIPES (200+ swipes creating match scenarios)
-- ========================================

-- Create mutual likes (will become matches)
INSERT INTO swipes (user_id, target_user_id, action, created_at)
VALUES
    -- Match 1: Emma & James
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222221', 'LIKE', NOW() - INTERVAL '5 days'),
    ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Match 2: Sophia & Liam
    ('11111111-1111-1111-1111-111111111112', '22222222-2222-2222-2222-222222222222', 'SUPER_LIKE', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111112', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Match 3: Olivia & Noah
    ('11111111-1111-1111-1111-111111111113', '22222222-2222-2222-2222-222222222223', 'LIKE', NOW() - INTERVAL '6 days'),
    ('22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111113', 'LIKE', NOW() - INTERVAL '5 days'),
    -- Match 4: Ava & William
    ('11111111-1111-1111-1111-111111111114', '22222222-2222-2222-2222-222222222224', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222224', '11111111-1111-1111-1111-111111111114', 'SUPER_LIKE', NOW() - INTERVAL '2 days'),
    -- Match 5: Isabella & Oliver
    ('11111111-1111-1111-1111-111111111115', '22222222-2222-2222-2222-222222222225', 'LIKE', NOW() - INTERVAL '1 day'),
    ('22222222-2222-2222-2222-222222222225', '11111111-1111-1111-1111-111111111115', 'LIKE', NOW() - INTERVAL '1 day')
ON CONFLICT (user_id, target_user_id) DO NOTHING;

-- One-sided likes (no match yet)
INSERT INTO swipes (user_id, target_user_id, action, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111116', '22222222-2222-2222-2222-222222222226', 'LIKE', NOW() - INTERVAL '1 day'),
    ('11111111-1111-1111-1111-111111111117', '22222222-2222-2222-2222-222222222227', 'LIKE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111118', '22222222-2222-2222-2222-222222222228', 'SUPER_LIKE', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222229', '11111111-1111-1111-1111-111111111119', 'LIKE', NOW() - INTERVAL '1 day'),
    ('22222222-2222-2222-2222-222222222230', '11111111-1111-1111-1111-111111111120', 'LIKE', NOW() - INTERVAL '2 days')
ON CONFLICT (user_id, target_user_id) DO NOTHING;

-- Passes
INSERT INTO swipes (user_id, target_user_id, action, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222230', 'PASS', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111112', '22222222-2222-2222-2222-222222222231', 'PASS', NOW() - INTERVAL '4 days'),
    ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111130', 'PASS', NOW() - INTERVAL '2 days')
ON CONFLICT (user_id, target_user_id) DO NOTHING;

-- Additional swipes to reach 200+ total
-- Women swiping on men (likes)
INSERT INTO swipes (user_id, target_user_id, action, created_at)
VALUES
    -- Mia likes
    ('11111111-1111-1111-1111-111111111116', '22222222-2222-2222-2222-222222222227', 'LIKE', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111116', '22222222-2222-2222-2222-222222222228', 'LIKE', NOW() - INTERVAL '4 days'),
    ('11111111-1111-1111-1111-111111111116', '22222222-2222-2222-2222-222222222229', 'LIKE', NOW() - INTERVAL '5 days'),
    -- Charlotte likes
    ('11111111-1111-1111-1111-111111111117', '22222222-2222-2222-2222-222222222228', 'SUPER_LIKE', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111117', '22222222-2222-2222-2222-222222222229', 'LIKE', NOW() - INTERVAL '4 days'),
    ('11111111-1111-1111-1111-111111111117', '22222222-2222-2222-2222-222222222230', 'LIKE', NOW() - INTERVAL '5 days'),
    -- Amelia likes
    ('11111111-1111-1111-1111-111111111118', '22222222-2222-2222-2222-222222222229', 'LIKE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111118', '22222222-2222-2222-2222-222222222230', 'LIKE', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111118', '22222222-2222-2222-2222-222222222231', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Harper likes
    ('11111111-1111-1111-1111-111111111119', '22222222-2222-2222-2222-222222222221', 'LIKE', NOW() - INTERVAL '6 days'),
    ('11111111-1111-1111-1111-111111111119', '22222222-2222-2222-2222-222222222230', 'LIKE', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111119', '22222222-2222-2222-2222-222222222231', 'SUPER_LIKE', NOW() - INTERVAL '4 days'),
    -- Evelyn likes
    ('11111111-1111-1111-1111-111111111120', '22222222-2222-2222-2222-222222222221', 'LIKE', NOW() - INTERVAL '5 days'),
    ('11111111-1111-1111-1111-111111111120', '22222222-2222-2222-2222-222222222231', 'LIKE', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111120', '22222222-2222-2222-2222-222222222232', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Luna likes
    ('11111111-1111-1111-1111-111111111121', '22222222-2222-2222-2222-222222222232', 'LIKE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111121', '22222222-2222-2222-2222-222222222233', 'LIKE', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111121', '22222222-2222-2222-2222-222222222234', 'SUPER_LIKE', NOW() - INTERVAL '4 days'),
    -- Chloe likes
    ('11111111-1111-1111-1111-111111111122', '22222222-2222-2222-2222-222222222233', 'LIKE', NOW() - INTERVAL '1 day'),
    ('11111111-1111-1111-1111-111111111122', '22222222-2222-2222-2222-222222222234', 'LIKE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111122', '22222222-2222-2222-2222-222222222235', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Penelope likes
    ('11111111-1111-1111-1111-111111111123', '22222222-2222-2222-2222-222222222234', 'LIKE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111123', '22222222-2222-2222-2222-222222222235', 'SUPER_LIKE', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111123', '22222222-2222-2222-2222-222222222236', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Layla likes
    ('11111111-1111-1111-1111-111111111124', '22222222-2222-2222-2222-222222222235', 'LIKE', NOW() - INTERVAL '1 day'),
    ('11111111-1111-1111-1111-111111111124', '22222222-2222-2222-2222-222222222236', 'LIKE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111124', '22222222-2222-2222-2222-222222222237', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Riley likes
    ('11111111-1111-1111-1111-111111111125', '22222222-2222-2222-2222-222222222236', 'LIKE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111125', '22222222-2222-2222-2222-222222222237', 'SUPER_LIKE', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111125', '22222222-2222-2222-2222-222222222238', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Zoey likes
    ('11111111-1111-1111-1111-111111111126', '22222222-2222-2222-2222-222222222237', 'LIKE', NOW() - INTERVAL '1 day'),
    ('11111111-1111-1111-1111-111111111126', '22222222-2222-2222-2222-222222222238', 'LIKE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111126', '22222222-2222-2222-2222-222222222239', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Nora likes
    ('11111111-1111-1111-1111-111111111127', '22222222-2222-2222-2222-222222222238', 'LIKE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111127', '22222222-2222-2222-2222-222222222239', 'SUPER_LIKE', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111127', '22222222-2222-2222-2222-222222222240', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Lily likes
    ('11111111-1111-1111-1111-111111111128', '22222222-2222-2222-2222-222222222239', 'LIKE', NOW() - INTERVAL '1 day'),
    ('11111111-1111-1111-1111-111111111128', '22222222-2222-2222-2222-222222222240', 'LIKE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111128', '22222222-2222-2222-2222-222222222241', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Eleanor likes
    ('11111111-1111-1111-1111-111111111129', '22222222-2222-2222-2222-222222222240', 'LIKE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111129', '22222222-2222-2222-2222-222222222241', 'SUPER_LIKE', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111129', '22222222-2222-2222-2222-222222222242', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Hazel likes
    ('11111111-1111-1111-1111-111111111130', '22222222-2222-2222-2222-222222222241', 'LIKE', NOW() - INTERVAL '1 day'),
    ('11111111-1111-1111-1111-111111111130', '22222222-2222-2222-2222-222222222242', 'LIKE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111130', '22222222-2222-2222-2222-222222222243', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Violet likes
    ('11111111-1111-1111-1111-111111111131', '22222222-2222-2222-2222-222222222242', 'LIKE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111131', '22222222-2222-2222-2222-222222222243', 'SUPER_LIKE', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111131', '22222222-2222-2222-2222-222222222244', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Aurora likes
    ('11111111-1111-1111-1111-111111111132', '22222222-2222-2222-2222-222222222243', 'LIKE', NOW() - INTERVAL '1 day'),
    ('11111111-1111-1111-1111-111111111132', '22222222-2222-2222-2222-222222222244', 'LIKE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111132', '22222222-2222-2222-2222-222222222245', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Savannah likes
    ('11111111-1111-1111-1111-111111111133', '22222222-2222-2222-2222-222222222244', 'LIKE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111133', '22222222-2222-2222-2222-222222222245', 'SUPER_LIKE', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111133', '22222222-2222-2222-2222-222222222221', 'LIKE', NOW() - INTERVAL '7 days'),
    -- Audrey likes
    ('11111111-1111-1111-1111-111111111134', '22222222-2222-2222-2222-222222222245', 'LIKE', NOW() - INTERVAL '1 day'),
    ('11111111-1111-1111-1111-111111111134', '22222222-2222-2222-2222-222222222221', 'LIKE', NOW() - INTERVAL '6 days'),
    ('11111111-1111-1111-1111-111111111134', '22222222-2222-2222-2222-222222222222', 'LIKE', NOW() - INTERVAL '5 days'),
    -- Brooklyn likes
    ('11111111-1111-1111-1111-111111111135', '22222222-2222-2222-2222-222222222221', 'LIKE', NOW() - INTERVAL '5 days'),
    ('11111111-1111-1111-1111-111111111135', '22222222-2222-2222-2222-222222222222', 'SUPER_LIKE', NOW() - INTERVAL '4 days'),
    ('11111111-1111-1111-1111-111111111135', '22222222-2222-2222-2222-222222222223', 'LIKE', NOW() - INTERVAL '3 days')
ON CONFLICT (user_id, target_user_id) DO NOTHING;

-- Men swiping on women (likes)
INSERT INTO swipes (user_id, target_user_id, action, created_at)
VALUES
    -- Elijah likes
    ('22222222-2222-2222-2222-222222222226', '11111111-1111-1111-1111-111111111116', 'LIKE', NOW() - INTERVAL '1 day'),
    ('22222222-2222-2222-2222-222222222226', '11111111-1111-1111-1111-111111111117', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222226', '11111111-1111-1111-1111-111111111118', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Lucas likes
    ('22222222-2222-2222-2222-222222222227', '11111111-1111-1111-1111-111111111116', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222227', '11111111-1111-1111-1111-111111111118', 'SUPER_LIKE', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222227', '11111111-1111-1111-1111-111111111119', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Mason likes
    ('22222222-2222-2222-2222-222222222228', '11111111-1111-1111-1111-111111111117', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222228', '11111111-1111-1111-1111-111111111118', 'LIKE', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222228', '11111111-1111-1111-1111-111111111120', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Ethan likes
    ('22222222-2222-2222-2222-222222222229', '11111111-1111-1111-1111-111111111116', 'LIKE', NOW() - INTERVAL '4 days'),
    ('22222222-2222-2222-2222-222222222229', '11111111-1111-1111-1111-111111111118', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222229', '11111111-1111-1111-1111-111111111121', 'SUPER_LIKE', NOW() - INTERVAL '3 days'),
    -- Alexander likes
    ('22222222-2222-2222-2222-222222222230', '11111111-1111-1111-1111-111111111117', 'LIKE', NOW() - INTERVAL '4 days'),
    ('22222222-2222-2222-2222-222222222230', '11111111-1111-1111-1111-111111111118', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222230', '11111111-1111-1111-1111-111111111122', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Henry likes
    ('22222222-2222-2222-2222-222222222231', '11111111-1111-1111-1111-111111111118', 'LIKE', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222231', '11111111-1111-1111-1111-111111111119', 'SUPER_LIKE', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222231', '11111111-1111-1111-1111-111111111123', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Sebastian likes
    ('22222222-2222-2222-2222-222222222232', '11111111-1111-1111-1111-111111111120', 'LIKE', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222232', '11111111-1111-1111-1111-111111111121', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222232', '11111111-1111-1111-1111-111111111124', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Jack likes
    ('22222222-2222-2222-2222-222222222233', '11111111-1111-1111-1111-111111111121', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222233', '11111111-1111-1111-1111-111111111122', 'SUPER_LIKE', NOW() - INTERVAL '1 day'),
    ('22222222-2222-2222-2222-222222222233', '11111111-1111-1111-1111-111111111125', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Aiden likes
    ('22222222-2222-2222-2222-222222222234', '11111111-1111-1111-1111-111111111121', 'LIKE', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222234', '11111111-1111-1111-1111-111111111123', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222234', '11111111-1111-1111-1111-111111111126', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Owen likes
    ('22222222-2222-2222-2222-222222222235', '11111111-1111-1111-1111-111111111123', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222235', '11111111-1111-1111-1111-111111111124', 'SUPER_LIKE', NOW() - INTERVAL '1 day'),
    ('22222222-2222-2222-2222-222222222235', '11111111-1111-1111-1111-111111111127', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Samuel likes
    ('22222222-2222-2222-2222-222222222236', '11111111-1111-1111-1111-111111111124', 'LIKE', NOW() - INTERVAL '1 day'),
    ('22222222-2222-2222-2222-222222222236', '11111111-1111-1111-1111-111111111125', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222236', '11111111-1111-1111-1111-111111111128', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Ryan likes
    ('22222222-2222-2222-2222-222222222237', '11111111-1111-1111-1111-111111111125', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222237', '11111111-1111-1111-1111-111111111126', 'SUPER_LIKE', NOW() - INTERVAL '1 day'),
    ('22222222-2222-2222-2222-222222222237', '11111111-1111-1111-1111-111111111129', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Nathan likes
    ('22222222-2222-2222-2222-222222222238', '11111111-1111-1111-1111-111111111125', 'LIKE', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222238', '11111111-1111-1111-1111-111111111127', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222238', '11111111-1111-1111-1111-111111111130', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Caleb likes
    ('22222222-2222-2222-2222-222222222239', '11111111-1111-1111-1111-111111111126', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222239', '11111111-1111-1111-1111-111111111128', 'SUPER_LIKE', NOW() - INTERVAL '1 day'),
    ('22222222-2222-2222-2222-222222222239', '11111111-1111-1111-1111-111111111131', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Isaac likes
    ('22222222-2222-2222-2222-222222222240', '11111111-1111-1111-1111-111111111127', 'LIKE', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222240', '11111111-1111-1111-1111-111111111129', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222240', '11111111-1111-1111-1111-111111111132', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Joshua likes
    ('22222222-2222-2222-2222-222222222241', '11111111-1111-1111-1111-111111111128', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222241', '11111111-1111-1111-1111-111111111130', 'SUPER_LIKE', NOW() - INTERVAL '1 day'),
    ('22222222-2222-2222-2222-222222222241', '11111111-1111-1111-1111-111111111133', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Connor likes
    ('22222222-2222-2222-2222-222222222242', '11111111-1111-1111-1111-111111111129', 'LIKE', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222242', '11111111-1111-1111-1111-111111111131', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222242', '11111111-1111-1111-1111-111111111134', 'LIKE', NOW() - INTERVAL '4 days'),
    -- Matthew likes
    ('22222222-2222-2222-2222-222222222243', '11111111-1111-1111-1111-111111111130', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222243', '11111111-1111-1111-1111-111111111132', 'SUPER_LIKE', NOW() - INTERVAL '1 day'),
    ('22222222-2222-2222-2222-222222222243', '11111111-1111-1111-1111-111111111135', 'LIKE', NOW() - INTERVAL '3 days'),
    -- Daniel likes
    ('22222222-2222-2222-2222-222222222244', '11111111-1111-1111-1111-111111111131', 'LIKE', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222244', '11111111-1111-1111-1111-111111111133', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222244', '11111111-1111-1111-1111-111111111111', 'LIKE', NOW() - INTERVAL '7 days'),
    -- Andrew likes
    ('22222222-2222-2222-2222-222222222245', '11111111-1111-1111-1111-111111111132', 'LIKE', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222245', '11111111-1111-1111-1111-111111111134', 'SUPER_LIKE', NOW() - INTERVAL '1 day'),
    ('22222222-2222-2222-2222-222222222245', '11111111-1111-1111-1111-111111111112', 'LIKE', NOW() - INTERVAL '6 days')
ON CONFLICT (user_id, target_user_id) DO NOTHING;

-- Additional passes for realistic ratio
INSERT INTO swipes (user_id, target_user_id, action, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222231', 'PASS', NOW() - INTERVAL '4 days'),
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222232', 'PASS', NOW() - INTERVAL '5 days'),
    ('11111111-1111-1111-1111-111111111112', '22222222-2222-2222-2222-222222222232', 'PASS', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111112', '22222222-2222-2222-2222-222222222233', 'PASS', NOW() - INTERVAL '4 days'),
    ('11111111-1111-1111-1111-111111111113', '22222222-2222-2222-2222-222222222224', 'PASS', NOW() - INTERVAL '7 days'),
    ('11111111-1111-1111-1111-111111111113', '22222222-2222-2222-2222-222222222234', 'PASS', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111114', '22222222-2222-2222-2222-222222222225', 'PASS', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111114', '22222222-2222-2222-2222-222222222235', 'PASS', NOW() - INTERVAL '4 days'),
    ('11111111-1111-1111-1111-111111111115', '22222222-2222-2222-2222-222222222226', 'PASS', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111115', '22222222-2222-2222-2222-222222222236', 'PASS', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111131', 'PASS', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111132', 'PASS', NOW() - INTERVAL '4 days'),
    ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111133', 'PASS', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111134', 'PASS', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111114', 'PASS', NOW() - INTERVAL '6 days'),
    ('22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111135', 'PASS', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222224', '11111111-1111-1111-1111-111111111115', 'PASS', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222224', '11111111-1111-1111-1111-111111111116', 'PASS', NOW() - INTERVAL '4 days'),
    ('22222222-2222-2222-2222-222222222225', '11111111-1111-1111-1111-111111111116', 'PASS', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222225', '11111111-1111-1111-1111-111111111117', 'PASS', NOW() - INTERVAL '3 days')
ON CONFLICT (user_id, target_user_id) DO NOTHING;

-- Additional swipes to reach 200+ total
INSERT INTO swipes (user_id, target_user_id, action, created_at)
VALUES
    -- More cross-gender likes
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222226', 'LIKE', NOW() - INTERVAL '8 days'),
    ('11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222227', 'LIKE', NOW() - INTERVAL '9 days'),
    ('11111111-1111-1111-1111-111111111112', '22222222-2222-2222-2222-222222222226', 'LIKE', NOW() - INTERVAL '7 days'),
    ('11111111-1111-1111-1111-111111111112', '22222222-2222-2222-2222-222222222227', 'SUPER_LIKE', NOW() - INTERVAL '8 days'),
    ('11111111-1111-1111-1111-111111111113', '22222222-2222-2222-2222-222222222226', 'LIKE', NOW() - INTERVAL '8 days'),
    ('11111111-1111-1111-1111-111111111113', '22222222-2222-2222-2222-222222222227', 'LIKE', NOW() - INTERVAL '9 days'),
    ('11111111-1111-1111-1111-111111111114', '22222222-2222-2222-2222-222222222226', 'LIKE', NOW() - INTERVAL '6 days'),
    ('11111111-1111-1111-1111-111111111114', '22222222-2222-2222-2222-222222222227', 'LIKE', NOW() - INTERVAL '7 days'),
    ('11111111-1111-1111-1111-111111111115', '22222222-2222-2222-2222-222222222227', 'SUPER_LIKE', NOW() - INTERVAL '5 days'),
    ('11111111-1111-1111-1111-111111111115', '22222222-2222-2222-2222-222222222228', 'LIKE', NOW() - INTERVAL '6 days'),
    ('22222222-2222-2222-2222-222222222226', '11111111-1111-1111-1111-111111111119', 'LIKE', NOW() - INTERVAL '5 days'),
    ('22222222-2222-2222-2222-222222222226', '11111111-1111-1111-1111-111111111120', 'LIKE', NOW() - INTERVAL '6 days'),
    ('22222222-2222-2222-2222-222222222226', '11111111-1111-1111-1111-111111111121', 'SUPER_LIKE', NOW() - INTERVAL '7 days'),
    ('22222222-2222-2222-2222-222222222227', '11111111-1111-1111-1111-111111111120', 'LIKE', NOW() - INTERVAL '6 days'),
    ('22222222-2222-2222-2222-222222222227', '11111111-1111-1111-1111-111111111121', 'LIKE', NOW() - INTERVAL '7 days'),
    ('22222222-2222-2222-2222-222222222227', '11111111-1111-1111-1111-111111111122', 'LIKE', NOW() - INTERVAL '8 days'),
    ('22222222-2222-2222-2222-222222222228', '11111111-1111-1111-1111-111111111121', 'SUPER_LIKE', NOW() - INTERVAL '5 days'),
    ('22222222-2222-2222-2222-222222222228', '11111111-1111-1111-1111-111111111122', 'LIKE', NOW() - INTERVAL '6 days'),
    ('22222222-2222-2222-2222-222222222228', '11111111-1111-1111-1111-111111111123', 'LIKE', NOW() - INTERVAL '7 days'),
    ('22222222-2222-2222-2222-222222222229', '11111111-1111-1111-1111-111111111122', 'LIKE', NOW() - INTERVAL '5 days'),
    ('22222222-2222-2222-2222-222222222229', '11111111-1111-1111-1111-111111111123', 'LIKE', NOW() - INTERVAL '6 days'),
    ('22222222-2222-2222-2222-222222222229', '11111111-1111-1111-1111-111111111124', 'SUPER_LIKE', NOW() - INTERVAL '7 days'),
    ('22222222-2222-2222-2222-222222222230', '11111111-1111-1111-1111-111111111123', 'LIKE', NOW() - INTERVAL '5 days'),
    ('22222222-2222-2222-2222-222222222230', '11111111-1111-1111-1111-111111111124', 'LIKE', NOW() - INTERVAL '6 days'),
    ('22222222-2222-2222-2222-222222222230', '11111111-1111-1111-1111-111111111125', 'LIKE', NOW() - INTERVAL '7 days'),
    -- More passes for realistic ratios
    ('11111111-1111-1111-1111-111111111116', '22222222-2222-2222-2222-222222222241', 'PASS', NOW() - INTERVAL '5 days'),
    ('11111111-1111-1111-1111-111111111117', '22222222-2222-2222-2222-222222222242', 'PASS', NOW() - INTERVAL '6 days'),
    ('11111111-1111-1111-1111-111111111118', '22222222-2222-2222-2222-222222222243', 'PASS', NOW() - INTERVAL '7 days'),
    ('11111111-1111-1111-1111-111111111119', '22222222-2222-2222-2222-222222222244', 'PASS', NOW() - INTERVAL '8 days'),
    ('11111111-1111-1111-1111-111111111120', '22222222-2222-2222-2222-222222222245', 'PASS', NOW() - INTERVAL '9 days'),
    ('22222222-2222-2222-2222-222222222231', '11111111-1111-1111-1111-111111111124', 'PASS', NOW() - INTERVAL '5 days'),
    ('22222222-2222-2222-2222-222222222232', '11111111-1111-1111-1111-111111111125', 'PASS', NOW() - INTERVAL '6 days'),
    ('22222222-2222-2222-2222-222222222233', '11111111-1111-1111-1111-111111111126', 'PASS', NOW() - INTERVAL '7 days'),
    ('22222222-2222-2222-2222-222222222234', '11111111-1111-1111-1111-111111111127', 'PASS', NOW() - INTERVAL '8 days'),
    ('22222222-2222-2222-2222-222222222235', '11111111-1111-1111-1111-111111111128', 'PASS', NOW() - INTERVAL '9 days'),
    -- Additional likes
    ('11111111-1111-1111-1111-111111111121', '22222222-2222-2222-2222-222222222221', 'LIKE', NOW() - INTERVAL '10 days'),
    ('11111111-1111-1111-1111-111111111122', '22222222-2222-2222-2222-222222222222', 'LIKE', NOW() - INTERVAL '10 days'),
    ('11111111-1111-1111-1111-111111111123', '22222222-2222-2222-2222-222222222223', 'LIKE', NOW() - INTERVAL '10 days'),
    ('11111111-1111-1111-1111-111111111124', '22222222-2222-2222-2222-222222222224', 'SUPER_LIKE', NOW() - INTERVAL '10 days'),
    ('11111111-1111-1111-1111-111111111125', '22222222-2222-2222-2222-222222222221', 'LIKE', NOW() - INTERVAL '11 days'),
    ('22222222-2222-2222-2222-222222222236', '11111111-1111-1111-1111-111111111111', 'LIKE', NOW() - INTERVAL '10 days'),
    ('22222222-2222-2222-2222-222222222237', '11111111-1111-1111-1111-111111111112', 'LIKE', NOW() - INTERVAL '10 days'),
    ('22222222-2222-2222-2222-222222222238', '11111111-1111-1111-1111-111111111113', 'SUPER_LIKE', NOW() - INTERVAL '10 days'),
    ('22222222-2222-2222-2222-222222222239', '11111111-1111-1111-1111-111111111114', 'LIKE', NOW() - INTERVAL '10 days'),
    ('22222222-2222-2222-2222-222222222240', '11111111-1111-1111-1111-111111111115', 'LIKE', NOW() - INTERVAL '11 days')
ON CONFLICT (user_id, target_user_id) DO NOTHING;

-- ========================================
-- MATCHES (From mutual likes above)
-- ========================================
INSERT INTO matches (id, user1_id, user2_id, status, matched_at, ended_at, ended_by)
VALUES
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222221', 'ACTIVE', NOW() - INTERVAL '4 days', NULL, NULL),
    ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111112', '22222222-2222-2222-2222-222222222222', 'ACTIVE', NOW() - INTERVAL '3 days', NULL, NULL),
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111113', '22222222-2222-2222-2222-222222222223', 'ACTIVE', NOW() - INTERVAL '5 days', NULL, NULL),
    -- UNMATCHED status: William unmatched Ava
    ('33333333-3333-3333-3333-333333333334', '11111111-1111-1111-1111-111111111114', '22222222-2222-2222-2222-222222222224', 'UNMATCHED', NOW() - INTERVAL '2 days', NOW() - INTERVAL '1 day', '22222222-2222-2222-2222-222222222224'),
    ('33333333-3333-3333-3333-333333333335', '11111111-1111-1111-1111-111111111115', '22222222-2222-2222-2222-222222222225', 'ACTIVE', NOW() - INTERVAL '1 day', NULL, NULL),
    -- BLOCKED status: Mia blocked Elijah after a bad interaction
    ('33333333-3333-3333-3333-333333333336', '11111111-1111-1111-1111-111111111116', '22222222-2222-2222-2222-222222222226', 'BLOCKED', NOW() - INTERVAL '3 days', NOW() - INTERVAL '2 days', '11111111-1111-1111-1111-111111111116')
ON CONFLICT (user1_id, user2_id) DO NOTHING;

-- ========================================
-- MATCH SCORES
-- ========================================
INSERT INTO match_scores (match_id, score, factors)
SELECT
    id,
    60 + (random() * 35)::INT,
    jsonb_build_object(
        'interest_match', (random() * 40)::INT,
        'age_compatibility', (random() * 30)::INT,
        'preference_alignment', (random() * 30)::INT
    )
FROM matches
ON CONFLICT (match_id) DO NOTHING;

-- ========================================
-- MESSAGES (Conversations in matches)
-- ========================================
INSERT INTO messages (match_id, sender_id, content, status, created_at, read_at)
VALUES
    -- Conversation 1: Emma & James
    ('33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222221', 'Hey Emma! Love your hiking photos. What''s your favorite trail?', 'READ', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days' + INTERVAL '30 minutes'),
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', 'Hi James! Thanks! I love the Bear Mountain trail. Have you been?', 'READ', NOW() - INTERVAL '4 days' + INTERVAL '1 hour', NOW() - INTERVAL '4 days' + INTERVAL '2 hours'),
    ('33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222221', 'Not yet! Would love to check it out. Maybe we could go together sometime?', 'READ', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days' + INTERVAL '15 minutes'),
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', 'That sounds great! How about this weekend?', 'READ', NOW() - INTERVAL '3 days' + INTERVAL '30 minutes', NOW() - INTERVAL '3 days' + INTERVAL '45 minutes'),
    ('33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222221', 'Perfect! Saturday morning works for me. Should I bring coffee?', 'SENT', NOW() - INTERVAL '2 days', NULL),

    -- Conversation 2: Sophia & Liam
    ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111112', 'Hi Liam! I noticed you''re into mountain biking. I''ve been wanting to try it!', 'READ', NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days' + INTERVAL '20 minutes'),
    ('33333333-3333-3333-3333-333333333332', '22222222-2222-2222-2222-222222222222', 'Hey Sophia! It''s so fun! I''d be happy to take you on a beginner-friendly trail.', 'READ', NOW() - INTERVAL '3 days' + INTERVAL '1 hour', NOW() - INTERVAL '3 days' + INTERVAL '2 hours'),
    ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111112', 'That would be amazing! I''m also a yoga instructor if you ever want to try a class 😊', 'DELIVERED', NOW() - INTERVAL '2 days', NULL),

    -- Conversation 3: Olivia & Noah
    ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222223', 'Your golden retriever is adorable! What''s their name?', 'READ', NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days' + INTERVAL '10 minutes'),
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111113', 'Thank you! Her name is Luna. She''s the sweetest girl ever!', 'READ', NOW() - INTERVAL '5 days' + INTERVAL '30 minutes', NOW() - INTERVAL '5 days' + INTERVAL '1 hour'),
    ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222223', 'Luna! Great name. I''d love to meet her. Maybe a dog-friendly coffee date?', 'READ', NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days' + INTERVAL '2 hours'),

    -- Conversation 4: Ava & William
    ('33333333-3333-3333-3333-333333333334', '22222222-2222-2222-2222-222222222224', 'Hey Ava! I saw you love salsa dancing. I''ve always wanted to learn!', 'READ', NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days' + INTERVAL '15 minutes'),
    ('33333333-3333-3333-3333-333333333334', '11111111-1111-1111-1111-111111111114', 'Hi William! Salsa is so much fun! There''s a great class on Thursdays downtown.', 'SENT', NOW() - INTERVAL '1 day', NULL),

    -- Conversation 5: Isabella & Oliver
    ('33333333-3333-3333-3333-333333333335', '11111111-1111-1111-1111-111111111115', 'Hey Oliver! Fellow travel lover here! What''s the best place you''ve visited?', 'DELIVERED', NOW() - INTERVAL '1 day', NULL),
    ('33333333-3333-3333-3333-333333333335', '22222222-2222-2222-2222-222222222225', 'Hi Isabella! Great question. Japan was incredible - the culture, food, and people. You?', 'SENT', NOW() - INTERVAL '20 hours', NULL),

    -- Continue Conversation 1: Emma & James (add more depth)
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', 'Yes please! I''ll bring some pastries from this amazing bakery near me.', 'READ', NOW() - INTERVAL '2 days' + INTERVAL '1 hour', NOW() - INTERVAL '2 days' + INTERVAL '2 hours'),
    ('33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222221', 'That sounds perfect! Which bakery? I''m always looking for new spots.', 'READ', NOW() - INTERVAL '2 days' + INTERVAL '3 hours', NOW() - INTERVAL '2 days' + INTERVAL '4 hours'),
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', 'Levain Bakery on the Upper West Side. Their chocolate chip cookies are life-changing!', 'READ', NOW() - INTERVAL '2 days' + INTERVAL '5 hours', NOW() - INTERVAL '2 days' + INTERVAL '6 hours'),
    ('33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222221', 'Oh I love that place! We clearly have similar taste. Should I pick you up at 8am?', 'READ', NOW() - INTERVAL '1 day' + INTERVAL '2 hours', NOW() - INTERVAL '1 day' + INTERVAL '3 hours'),
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', '8am works great! I''ll text you my address. Can''t wait!', 'READ', NOW() - INTERVAL '1 day' + INTERVAL '4 hours', NOW() - INTERVAL '1 day' + INTERVAL '5 hours'),
    ('33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222221', 'Perfect! I just checked the weather and it''s going to be beautiful. See you Saturday!', 'READ', NOW() - INTERVAL '1 day' + INTERVAL '6 hours', NOW() - INTERVAL '1 day' + INTERVAL '7 hours'),
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', 'Yay! I''m so excited. I''ll bring my camera too. The views are amazing.', 'DELIVERED', NOW() - INTERVAL '18 hours', NULL),

    -- Continue Conversation 2: Sophia & Liam (more messages)
    ('33333333-3333-3333-3333-333333333332', '22222222-2222-2222-2222-222222222222', 'I''d love to try yoga! I''ve heard it''s great for flexibility after biking.', 'READ', NOW() - INTERVAL '1 day' + INTERVAL '10 hours', NOW() - INTERVAL '1 day' + INTERVAL '11 hours'),
    ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111112', 'It really is! I teach a class on Sunday mornings if you''re free.', 'READ', NOW() - INTERVAL '1 day' + INTERVAL '12 hours', NOW() - INTERVAL '1 day' + INTERVAL '13 hours'),
    ('33333333-3333-3333-3333-333333333332', '22222222-2222-2222-2222-222222222222', 'Sunday works! What time and where?', 'READ', NOW() - INTERVAL '1 day' + INTERVAL '14 hours', NOW() - INTERVAL '1 day' + INTERVAL '15 hours'),
    ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111112', '9am at Yoga Soul in Brooklyn. It''s a gentle flow class, perfect for beginners.', 'READ', NOW() - INTERVAL '20 hours', NOW() - INTERVAL '19 hours'),
    ('33333333-3333-3333-3333-333333333332', '22222222-2222-2222-2222-222222222222', 'Awesome! Should I bring anything special?', 'READ', NOW() - INTERVAL '18 hours', NOW() - INTERVAL '17 hours'),
    ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111112', 'Just comfortable clothes and water. They have mats there. See you Sunday!', 'SENT', NOW() - INTERVAL '16 hours', NULL),

    -- Continue Conversation 3: Olivia & Noah (dog date)
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111113', 'A dog-friendly coffee date sounds perfect! Do you know Bark & Brew?', 'READ', NOW() - INTERVAL '3 days' + INTERVAL '10 hours', NOW() - INTERVAL '3 days' + INTERVAL '11 hours'),
    ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222223', 'I''ve heard of it! They have a fenced area right? Luna would love that.', 'READ', NOW() - INTERVAL '3 days' + INTERVAL '12 hours', NOW() - INTERVAL '3 days' + INTERVAL '13 hours'),
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111113', 'Yes! It''s the best. How about this Saturday afternoon?', 'READ', NOW() - INTERVAL '3 days' + INTERVAL '14 hours', NOW() - INTERVAL '3 days' + INTERVAL '15 hours'),
    ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222223', 'Saturday at 2pm? I can''t wait to finally meet Luna!', 'READ', NOW() - INTERVAL '2 days' + INTERVAL '10 hours', NOW() - INTERVAL '2 days' + INTERVAL '11 hours'),
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111113', 'Perfect! She''s very excited to make a new friend. See you there!', 'READ', NOW() - INTERVAL '2 days' + INTERVAL '12 hours', NOW() - INTERVAL '2 days' + INTERVAL '13 hours'),
    ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222223', 'Can''t wait! I''ll be the one looking for the cutest golden retriever.', 'DELIVERED', NOW() - INTERVAL '1 day', NULL),

    -- Continue Conversation 4: Ava & William (salsa dancing)
    ('33333333-3333-3333-3333-333333333334', '22222222-2222-2222-2222-222222222224', 'Thursday downtown? I''m definitely interested! What''s the studio name?', 'READ', NOW() - INTERVAL '22 hours', NOW() - INTERVAL '21 hours'),
    ('33333333-3333-3333-3333-333333333334', '11111111-1111-1111-1111-111111111114', 'It''s called Salsa Caliente on 23rd Street. The beginner class is at 7pm.', 'READ', NOW() - INTERVAL '20 hours', NOW() - INTERVAL '19 hours'),
    ('33333333-3333-3333-3333-333333333334', '22222222-2222-2222-2222-222222222224', 'I''m in! Should I wear anything specific?', 'READ', NOW() - INTERVAL '18 hours', NOW() - INTERVAL '17 hours'),
    ('33333333-3333-3333-3333-333333333334', '11111111-1111-1111-1111-111111111114', 'Just comfortable clothes and shoes you can move in. No sneakers ideally.', 'READ', NOW() - INTERVAL '16 hours', NOW() - INTERVAL '15 hours'),
    ('33333333-3333-3333-3333-333333333334', '22222222-2222-2222-2222-222222222224', 'Got it! This is going to be fun. Fair warning - I have two left feet!', 'READ', NOW() - INTERVAL '14 hours', NOW() - INTERVAL '13 hours'),
    ('33333333-3333-3333-3333-333333333334', '11111111-1111-1111-1111-111111111114', 'Don''t worry, the teacher is amazing and it''s all about having fun! See you Thursday!', 'SENT', NOW() - INTERVAL '12 hours', NULL),

    -- Continued conversation 5: Isabella & Oliver (travel talk)
    ('33333333-3333-3333-3333-333333333335', '11111111-1111-1111-1111-111111111115', 'Japan is on my bucket list! I loved New Zealand - the hiking there is unreal.', 'READ', NOW() - INTERVAL '18 hours', NOW() - INTERVAL '17 hours'),
    ('33333333-3333-3333-3333-333333333335', '22222222-2222-2222-2222-222222222225', 'New Zealand! That''s amazing. Did you do the Tongariro Crossing?', 'READ', NOW() - INTERVAL '16 hours', NOW() - INTERVAL '15 hours'),
    ('33333333-3333-3333-3333-333333333335', '11111111-1111-1111-1111-111111111115', 'Yes! It was challenging but so worth it. The views were incredible.', 'READ', NOW() - INTERVAL '14 hours', NOW() - INTERVAL '13 hours'),
    ('33333333-3333-3333-3333-333333333335', '22222222-2222-2222-2222-222222222225', 'I''m impressed! Would you want to grab coffee and swap travel stories?', 'READ', NOW() - INTERVAL '12 hours', NOW() - INTERVAL '11 hours'),
    ('33333333-3333-3333-3333-333333333335', '11111111-1111-1111-1111-111111111115', 'I''d love that! I know a great cafe with photos from all over the world on the walls.', 'READ', NOW() - INTERVAL '10 hours', NOW() - INTERVAL '9 hours'),
    ('33333333-3333-3333-3333-333333333335', '22222222-2222-2222-2222-222222222225', 'That sounds perfect. When are you free?', 'READ', NOW() - INTERVAL '8 hours', NOW() - INTERVAL '7 hours'),
    ('33333333-3333-3333-3333-333333333335', '11111111-1111-1111-1111-111111111115', 'How about tomorrow evening? Say 6pm?', 'READ', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '5 hours'),
    ('33333333-3333-3333-3333-333333333335', '22222222-2222-2222-2222-222222222225', 'Tomorrow at 6 works great! Send me the address and I''ll be there.', 'DELIVERED', NOW() - INTERVAL '4 hours', NULL),

    -- Additional random messages for more variety
    ('33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222221', 'Good morning! Just wanted to say I''m really looking forward to Saturday.', 'READ', NOW() - INTERVAL '10 hours', NOW() - INTERVAL '9 hours'),
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', 'Me too! I checked the trail conditions and they look perfect.', 'READ', NOW() - INTERVAL '8 hours', NOW() - INTERVAL '7 hours'),
    ('33333333-3333-3333-3333-333333333332', '22222222-2222-2222-2222-222222222222', 'Quick question - is there parking near the yoga studio?', 'READ', NOW() - INTERVAL '6 hours', NOW() - INTERVAL '5 hours'),
    ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111112', 'Yes! There''s a garage on the corner. I''ll send you the details.', 'SENT', NOW() - INTERVAL '4 hours', NULL),
    ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222223', 'Hey, just confirming we''re still on for Saturday at Bark & Brew?', 'READ', NOW() - INTERVAL '5 hours', NOW() - INTERVAL '4 hours'),
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111113', 'Absolutely! Luna and I will be there at 2pm sharp!', 'SENT', NOW() - INTERVAL '3 hours', NULL),
    ('33333333-3333-3333-3333-333333333334', '22222222-2222-2222-2222-222222222224', 'Just looked up the studio - the reviews are great! Getting excited.', 'READ', NOW() - INTERVAL '4 hours', NOW() - INTERVAL '3 hours'),
    ('33333333-3333-3333-3333-333333333334', '11111111-1111-1111-1111-111111111114', 'It''s such a fun atmosphere. You''re going to love it!', 'DELIVERED', NOW() - INTERVAL '2 hours', NULL),
    ('33333333-3333-3333-3333-333333333335', '11111111-1111-1111-1111-111111111115', 'Perfect! The cafe is called Wanderlust on 7th Ave. See you there!', 'SENT', NOW() - INTERVAL '2 hours', NULL),

    -- More conversation depth
    ('33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222221', 'What''s your favorite type of cuisine? Just curious!', 'READ', NOW() - INTERVAL '36 hours', NOW() - INTERVAL '35 hours'),
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', 'Italian for sure! I make my own pasta sometimes. You?', 'READ', NOW() - INTERVAL '34 hours', NOW() - INTERVAL '33 hours'),
    ('33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222221', 'Japanese! Though Italian is close second. Homemade pasta is impressive!', 'READ', NOW() - INTERVAL '32 hours', NOW() - INTERVAL '31 hours'),
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', 'Maybe I can cook for you sometime. My carbonara is pretty good.', 'READ', NOW() - INTERVAL '30 hours', NOW() - INTERVAL '29 hours'),
    ('33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222221', 'That would be amazing! I''ll bring the wine.', 'DELIVERED', NOW() - INTERVAL '28 hours', NULL),
    ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111112', 'Do you have a favorite hiking spot around here?', 'READ', NOW() - INTERVAL '40 hours', NOW() - INTERVAL '39 hours'),
    ('33333333-3333-3333-3333-333333333332', '22222222-2222-2222-2222-222222222222', 'Harriman State Park is my go-to. So many great trails!', 'READ', NOW() - INTERVAL '38 hours', NOW() - INTERVAL '37 hours'),
    ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111112', 'I''ve never been but always wanted to check it out!', 'READ', NOW() - INTERVAL '36 hours', NOW() - INTERVAL '35 hours'),
    ('33333333-3333-3333-3333-333333333332', '22222222-2222-2222-2222-222222222222', 'We should go together sometime! There''s this great loop trail.', 'DELIVERED', NOW() - INTERVAL '34 hours', NULL),
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111113', 'Do you have any other pets besides dogs?', 'READ', NOW() - INTERVAL '50 hours', NOW() - INTERVAL '49 hours'),
    ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222223', 'Just Luna! But I grew up with cats too. How about you?', 'READ', NOW() - INTERVAL '48 hours', NOW() - INTERVAL '47 hours'),
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111113', 'Same! I have a cat named Whiskers. He''s very chill.', 'READ', NOW() - INTERVAL '46 hours', NOW() - INTERVAL '45 hours'),
    ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222223', 'A cat and a dog! That''s the dream combo. We''ll have to introduce them!', 'DELIVERED', NOW() - INTERVAL '44 hours', NULL),
    ('33333333-3333-3333-3333-333333333334', '22222222-2222-2222-2222-222222222224', 'How long have you been salsa dancing?', 'READ', NOW() - INTERVAL '30 hours', NOW() - INTERVAL '29 hours'),
    ('33333333-3333-3333-3333-333333333334', '11111111-1111-1111-1111-111111111114', 'About 3 years now! Started when I visited Cuba. Fell in love with it.', 'READ', NOW() - INTERVAL '28 hours', NOW() - INTERVAL '27 hours'),
    ('33333333-3333-3333-3333-333333333334', '22222222-2222-2222-2222-222222222224', 'Cuba! That must have been an incredible trip.', 'READ', NOW() - INTERVAL '26 hours', NOW() - INTERVAL '25 hours'),
    ('33333333-3333-3333-3333-333333333334', '11111111-1111-1111-1111-111111111114', 'It was magical. The music, the people, the food - I need to go back!', 'DELIVERED', NOW() - INTERVAL '24 hours', NULL),
    ('33333333-3333-3333-3333-333333333335', '22222222-2222-2222-2222-222222222225', 'What''s next on your travel bucket list?', 'READ', NOW() - INTERVAL '26 hours', NOW() - INTERVAL '25 hours'),
    ('33333333-3333-3333-3333-333333333335', '11111111-1111-1111-1111-111111111115', 'Iceland! I want to see the Northern Lights so badly.', 'READ', NOW() - INTERVAL '24 hours', NOW() - INTERVAL '23 hours'),
    ('33333333-3333-3333-3333-333333333335', '22222222-2222-2222-2222-222222222225', 'That''s on my list too! February is supposed to be the best time.', 'READ', NOW() - INTERVAL '22 hours', NOW() - INTERVAL '21 hours'),
    ('33333333-3333-3333-3333-333333333335', '11111111-1111-1111-1111-111111111115', 'I heard that! We should plan a trip together sometime.', 'DELIVERED', NOW() - INTERVAL '20 hours', NULL),

    -- Additional messages to reach 100+ total
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', 'Do you prefer morning hikes or afternoon?', 'READ', NOW() - INTERVAL '60 hours', NOW() - INTERVAL '59 hours'),
    ('33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222221', 'Morning for sure! The trails are less crowded.', 'READ', NOW() - INTERVAL '58 hours', NOW() - INTERVAL '57 hours'),
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', 'Same here! Early bird gets the best views.', 'READ', NOW() - INTERVAL '56 hours', NOW() - INTERVAL '55 hours'),
    ('33333333-3333-3333-3333-333333333331', '22222222-2222-2222-2222-222222222221', 'Have you ever tried sunrise hikes?', 'READ', NOW() - INTERVAL '54 hours', NOW() - INTERVAL '53 hours'),
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', 'A few times! We should do one together.', 'READ', NOW() - INTERVAL '52 hours', NOW() - INTERVAL '51 hours'),
    ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111112', 'What got you into mountain biking?', 'READ', NOW() - INTERVAL '50 hours', NOW() - INTERVAL '49 hours'),
    ('33333333-3333-3333-3333-333333333332', '22222222-2222-2222-2222-222222222222', 'A friend took me once and I was hooked!', 'READ', NOW() - INTERVAL '48 hours', NOW() - INTERVAL '47 hours'),
    ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111112', 'I totally get that. Same with yoga for me.', 'READ', NOW() - INTERVAL '46 hours', NOW() - INTERVAL '45 hours'),
    ('33333333-3333-3333-3333-333333333332', '22222222-2222-2222-2222-222222222222', 'Maybe we can swap hobbies? I teach biking, you teach yoga?', 'READ', NOW() - INTERVAL '44 hours', NOW() - INTERVAL '43 hours'),
    ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111112', 'Deal! Cross-training buddies!', 'READ', NOW() - INTERVAL '42 hours', NOW() - INTERVAL '41 hours'),
    ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222223', 'What breed is Luna?', 'READ', NOW() - INTERVAL '60 hours', NOW() - INTERVAL '59 hours'),
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111113', 'A purebred Golden Retriever. 4 years old!', 'READ', NOW() - INTERVAL '58 hours', NOW() - INTERVAL '57 hours'),
    ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222223', 'Adorable! I grew up with labs.', 'READ', NOW() - INTERVAL '56 hours', NOW() - INTERVAL '55 hours'),
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111113', 'You can borrow Luna anytime!', 'READ', NOW() - INTERVAL '54 hours', NOW() - INTERVAL '53 hours'),
    ('33333333-3333-3333-3333-333333333333', '22222222-2222-2222-2222-222222222223', 'That is so sweet! I might take you up on that.', 'READ', NOW() - INTERVAL '52 hours', NOW() - INTERVAL '51 hours'),
    ('33333333-3333-3333-3333-333333333334', '11111111-1111-1111-1111-111111111114', 'What kind of music do you usually listen to?', 'READ', NOW() - INTERVAL '40 hours', NOW() - INTERVAL '39 hours'),
    ('33333333-3333-3333-3333-333333333334', '22222222-2222-2222-2222-222222222224', 'A bit of everything! Rock, jazz, and Latin.', 'READ', NOW() - INTERVAL '38 hours', NOW() - INTERVAL '37 hours'),
    ('33333333-3333-3333-3333-333333333334', '11111111-1111-1111-1111-111111111114', 'Nice variety! I am more into pop and R&B.', 'READ', NOW() - INTERVAL '36 hours', NOW() - INTERVAL '35 hours'),
    ('33333333-3333-3333-3333-333333333334', '22222222-2222-2222-2222-222222222224', 'We should make each other playlists!', 'READ', NOW() - INTERVAL '34 hours', NOW() - INTERVAL '33 hours'),
    ('33333333-3333-3333-3333-333333333334', '11111111-1111-1111-1111-111111111114', 'I love that idea! Challenge accepted.', 'READ', NOW() - INTERVAL '32 hours', NOW() - INTERVAL '31 hours'),
    ('33333333-3333-3333-3333-333333333335', '22222222-2222-2222-2222-222222222225', 'Do you prefer solo travel or with friends?', 'READ', NOW() - INTERVAL '30 hours', NOW() - INTERVAL '29 hours'),
    ('33333333-3333-3333-3333-333333333335', '11111111-1111-1111-1111-111111111115', 'A mix! Solo for spontaneity, friends for memories.', 'READ', NOW() - INTERVAL '28 hours', NOW() - INTERVAL '27 hours'),
    ('33333333-3333-3333-3333-333333333335', '22222222-2222-2222-2222-222222222225', 'Perfect answer! Best of both worlds.', 'READ', NOW() - INTERVAL '26 hours' + INTERVAL '30 minutes', NOW() - INTERVAL '25 hours')
ON CONFLICT DO NOTHING;

-- ========================================
-- RECOMMENDATIONS (Pre-computed scores)
-- ========================================
INSERT INTO recommendations (user_id, target_user_id, score, algorithm_version, factors, expires_at)
SELECT
    u1.id,
    u2.id,
    50 + (random() * 45)::INT,
    'v1',
    jsonb_build_object(
        'interest_match', (random() * 100)::INT,
        'age_compatibility', (random() * 100)::INT,
        'activity_score', (random() * 100)::INT
    ),
    NOW() + INTERVAL '24 hours'
FROM users u1
CROSS JOIN users u2
WHERE u1.id != u2.id
  AND u1.gender != u2.gender
  AND random() > 0.7
LIMIT 500
ON CONFLICT (user_id, target_user_id) DO NOTHING;

-- ========================================
-- NOTIFICATIONS (40+ notifications)
-- ========================================
INSERT INTO notifications (user_id, type, title, body, data, is_read, is_sent, sent_at, created_at)
VALUES
    -- Match notifications for all 5 matches
    ('11111111-1111-1111-1111-111111111111', 'NEW_MATCH', 'New Match!', 'You matched with James!', '{"match_id": "33333333-3333-3333-3333-333333333331"}'::jsonb, true, true, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('22222222-2222-2222-2222-222222222221', 'NEW_MATCH', 'New Match!', 'You matched with Emma!', '{"match_id": "33333333-3333-3333-3333-333333333331"}'::jsonb, true, true, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('11111111-1111-1111-1111-111111111112', 'NEW_MATCH', 'New Match!', 'You matched with Liam!', '{"match_id": "33333333-3333-3333-3333-333333333332"}'::jsonb, true, true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222222', 'NEW_MATCH', 'New Match!', 'You matched with Sophia!', '{"match_id": "33333333-3333-3333-3333-333333333332"}'::jsonb, true, true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111113', 'NEW_MATCH', 'New Match!', 'You matched with Noah!', '{"match_id": "33333333-3333-3333-3333-333333333333"}'::jsonb, true, true, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    ('22222222-2222-2222-2222-222222222223', 'NEW_MATCH', 'New Match!', 'You matched with Olivia!', '{"match_id": "33333333-3333-3333-3333-333333333333"}'::jsonb, true, true, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    ('11111111-1111-1111-1111-111111111114', 'NEW_MATCH', 'New Match!', 'You matched with William!', '{"match_id": "33333333-3333-3333-3333-333333333334"}'::jsonb, true, true, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222224', 'NEW_MATCH', 'New Match!', 'You matched with Ava!', '{"match_id": "33333333-3333-3333-3333-333333333334"}'::jsonb, true, true, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111115', 'NEW_MATCH', 'New Match!', 'You matched with Oliver!', '{"match_id": "33333333-3333-3333-3333-333333333335"}'::jsonb, true, true, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    ('22222222-2222-2222-2222-222222222225', 'NEW_MATCH', 'New Match!', 'You matched with Isabella!', '{"match_id": "33333333-3333-3333-3333-333333333335"}'::jsonb, true, true, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    -- New message notifications
    ('11111111-1111-1111-1111-111111111111', 'NEW_MESSAGE', 'New Message', 'James sent you a message', '{"match_id": "33333333-3333-3333-3333-333333333331"}'::jsonb, true, true, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('22222222-2222-2222-2222-222222222221', 'NEW_MESSAGE', 'New Message', 'Emma replied to your message', '{"match_id": "33333333-3333-3333-3333-333333333331"}'::jsonb, true, true, NOW() - INTERVAL '3 days' + INTERVAL '1 hour', NOW() - INTERVAL '3 days' + INTERVAL '1 hour'),
    ('11111111-1111-1111-1111-111111111111', 'NEW_MESSAGE', 'New Message', 'James is excited for Saturday!', '{"match_id": "33333333-3333-3333-3333-333333333331"}'::jsonb, false, true, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    ('11111111-1111-1111-1111-111111111112', 'NEW_MESSAGE', 'New Message', 'Liam sent you a message', '{"match_id": "33333333-3333-3333-3333-333333333332"}'::jsonb, true, true, NOW() - INTERVAL '3 days' + INTERVAL '1 hour', NOW() - INTERVAL '3 days' + INTERVAL '1 hour'),
    ('22222222-2222-2222-2222-222222222222', 'NEW_MESSAGE', 'New Message', 'Sophia wants to go biking!', '{"match_id": "33333333-3333-3333-3333-333333333332"}'::jsonb, true, true, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111113', 'NEW_MESSAGE', 'New Message', 'Noah wants to meet Luna!', '{"match_id": "33333333-3333-3333-3333-333333333333"}'::jsonb, true, true, NOW() - INTERVAL '5 days' + INTERVAL '30 minutes', NOW() - INTERVAL '5 days' + INTERVAL '30 minutes'),
    ('22222222-2222-2222-2222-222222222223', 'NEW_MESSAGE', 'New Message', 'Olivia replied to your message', '{"match_id": "33333333-3333-3333-3333-333333333333"}'::jsonb, true, true, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('11111111-1111-1111-1111-111111111114', 'NEW_MESSAGE', 'New Message', 'William is excited about salsa!', '{"match_id": "33333333-3333-3333-3333-333333333334"}'::jsonb, true, true, NOW() - INTERVAL '2 days' + INTERVAL '15 minutes', NOW() - INTERVAL '2 days' + INTERVAL '15 minutes'),
    ('22222222-2222-2222-2222-222222222224', 'NEW_MESSAGE', 'New Message', 'Ava sent details about the class', '{"match_id": "33333333-3333-3333-3333-333333333334"}'::jsonb, true, true, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    ('11111111-1111-1111-1111-111111111115', 'NEW_MESSAGE', 'New Message', 'Oliver wants to meet up!', '{"match_id": "33333333-3333-3333-3333-333333333335"}'::jsonb, true, true, NOW() - INTERVAL '20 hours', NOW() - INTERVAL '20 hours'),
    ('22222222-2222-2222-2222-222222222225', 'NEW_MESSAGE', 'New Message', 'Isabella suggested a cafe', '{"match_id": "33333333-3333-3333-3333-333333333335"}'::jsonb, false, true, NOW() - INTERVAL '10 hours', NOW() - INTERVAL '10 hours'),
    -- Super like notifications
    ('22222222-2222-2222-2222-222222222222', 'SUPER_LIKE', 'Someone Super Liked You!', 'Sophia sent you a Super Like!', '{"user_id": "11111111-1111-1111-1111-111111111112"}'::jsonb, true, true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222224', 'SUPER_LIKE', 'Someone Super Liked You!', 'William received a Super Like from you!', '{"user_id": "11111111-1111-1111-1111-111111111114"}'::jsonb, true, true, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222228', 'SUPER_LIKE', 'Someone Super Liked You!', 'Someone sent you a Super Like!', '{"user_id": "11111111-1111-1111-1111-111111111118"}'::jsonb, false, true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    ('22222222-2222-2222-2222-222222222231', 'SUPER_LIKE', 'Someone Super Liked You!', 'Harper sent you a Super Like!', '{"user_id": "11111111-1111-1111-1111-111111111119"}'::jsonb, false, true, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('22222222-2222-2222-2222-222222222234', 'SUPER_LIKE', 'Someone Super Liked You!', 'Luna sent you a Super Like!', '{"user_id": "11111111-1111-1111-1111-111111111121"}'::jsonb, true, true, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('11111111-1111-1111-1111-111111111117', 'SUPER_LIKE', 'Someone Super Liked You!', 'Lucas sent you a Super Like!', '{"user_id": "22222222-2222-2222-2222-222222222227"}'::jsonb, true, true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111118', 'SUPER_LIKE', 'Someone Super Liked You!', 'Mason sent you a Super Like!', '{"user_id": "22222222-2222-2222-2222-222222222228"}'::jsonb, false, true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    ('11111111-1111-1111-1111-111111111121', 'SUPER_LIKE', 'Someone Super Liked You!', 'Ethan sent you a Super Like!', '{"user_id": "22222222-2222-2222-2222-222222222229"}'::jsonb, true, true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days'),
    -- System and promotion notifications
    ('11111111-1111-1111-1111-111111111111', 'SYSTEM', 'Welcome to POC Dating!', 'Complete your profile to get more matches.', '{"action": "complete_profile"}'::jsonb, true, true, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
    ('11111111-1111-1111-1111-111111111112', 'SYSTEM', 'Welcome to POC Dating!', 'Complete your profile to get more matches.', '{"action": "complete_profile"}'::jsonb, true, true, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
    ('11111111-1111-1111-1111-111111111113', 'SYSTEM', 'Welcome to POC Dating!', 'Complete your profile to get more matches.', '{"action": "complete_profile"}'::jsonb, true, true, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
    ('22222222-2222-2222-2222-222222222221', 'SYSTEM', 'Welcome to POC Dating!', 'Complete your profile to get more matches.', '{"action": "complete_profile"}'::jsonb, true, true, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
    ('22222222-2222-2222-2222-222222222222', 'SYSTEM', 'Welcome to POC Dating!', 'Complete your profile to get more matches.', '{"action": "complete_profile"}'::jsonb, true, true, NOW() - INTERVAL '10 days', NOW() - INTERVAL '10 days'),
    ('11111111-1111-1111-1111-111111111111', 'PROMOTION', 'Upgrade to Premium!', 'Get unlimited likes and see who likes you.', '{"action": "upgrade"}'::jsonb, false, true, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    ('11111111-1111-1111-1111-111111111113', 'PROMOTION', 'Upgrade to Premium!', 'Get unlimited likes and see who likes you.', '{"action": "upgrade"}'::jsonb, false, true, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    ('22222222-2222-2222-2222-222222222221', 'PROMOTION', 'Upgrade to Premium!', 'Get unlimited likes and see who likes you.', '{"action": "upgrade"}'::jsonb, false, true, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    ('22222222-2222-2222-2222-222222222223', 'PROMOTION', 'Upgrade to Premium!', 'Get unlimited likes and see who likes you.', '{"action": "upgrade"}'::jsonb, false, true, NOW() - INTERVAL '5 days', NOW() - INTERVAL '5 days'),
    ('11111111-1111-1111-1111-111111111114', 'SYSTEM', 'Profile Verified!', 'Your profile has been verified.', '{"action": "verified"}'::jsonb, true, true, NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
    ('11111111-1111-1111-1111-111111111115', 'SYSTEM', 'Profile Verified!', 'Your profile has been verified.', '{"action": "verified"}'::jsonb, true, true, NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
    ('22222222-2222-2222-2222-222222222221', 'SYSTEM', 'Profile Verified!', 'Your profile has been verified.', '{"action": "verified"}'::jsonb, true, true, NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
    ('22222222-2222-2222-2222-222222222225', 'SYSTEM', 'Profile Verified!', 'Your profile has been verified.', '{"action": "verified"}'::jsonb, true, true, NOW() - INTERVAL '7 days', NOW() - INTERVAL '7 days'),
    -- Profile view notifications
    ('11111111-1111-1111-1111-111111111116', 'PROFILE_VIEW', 'Someone Viewed Your Profile', 'Someone checked out your profile!', '{"viewer_count": 5}'::jsonb, false, true, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    ('11111111-1111-1111-1111-111111111117', 'PROFILE_VIEW', 'Someone Viewed Your Profile', 'Someone checked out your profile!', '{"viewer_count": 3}'::jsonb, false, true, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days'),
    ('22222222-2222-2222-2222-222222222226', 'PROFILE_VIEW', 'Someone Viewed Your Profile', 'Someone checked out your profile!', '{"viewer_count": 4}'::jsonb, true, true, NOW() - INTERVAL '1 day', NOW() - INTERVAL '1 day'),
    ('22222222-2222-2222-2222-222222222227', 'PROFILE_VIEW', 'Someone Viewed Your Profile', 'Someone checked out your profile!', '{"viewer_count": 2}'::jsonb, false, true, NOW() - INTERVAL '3 days', NOW() - INTERVAL '3 days')
ON CONFLICT DO NOTHING;

-- ========================================
-- USER PREFERENCES FOR NEW USERS
-- ========================================
INSERT INTO user_preferences (user_id, min_age, max_age, max_distance_km, interested_in, interests, notification_enabled)
VALUES
    -- Los Angeles users
    ('44444444-4444-4444-4444-444444444441', 21, 40, 50, 'MALE', ARRAY['acting', 'beach', 'volleyball', 'travel'], true),
    ('44444444-4444-4444-4444-444444444442', 25, 45, 50, 'FEMALE', ARRAY['music', 'hiking', 'food', 'production'], true),
    -- Chicago users
    ('44444444-4444-4444-4444-444444444443', 21, 38, 25, 'MALE', ARRAY['jazz', 'technology', 'food', 'coffee'], true),
    ('44444444-4444-4444-4444-444444444444', 25, 40, 50, 'FEMALE', ARRAY['sports', 'baseball', 'analytics', 'beer'], true),
    -- Austin users (NON_BINARY with interested_in='BOTH')
    ('44444444-4444-4444-4444-444444444445', 21, 45, 100, 'BOTH', ARRAY['technology', 'music', 'art', 'food'], true),
    ('44444444-4444-4444-4444-444444444446', 25, 50, 100, 'BOTH', ARRAY['art', 'food', 'music', 'outdoors'], true)
ON CONFLICT (user_id) DO NOTHING;

-- ========================================
-- GENDER PREFERENCE DIVERSITY
-- Update existing users to be interested in BOTH
-- ========================================
UPDATE user_preferences
SET interested_in = 'BOTH'
WHERE user_id IN (
    '11111111-1111-1111-1111-111111111121',  -- Luna Patel
    '22222222-2222-2222-2222-222222222231'   -- Henry Anderson
);

-- ========================================
-- USER BLOCKS (Blocking relationships)
-- ========================================
INSERT INTO user_blocks (blocker_id, blocked_id, reason, created_at)
VALUES
    -- Mia blocked Elijah after inappropriate messages (related to the BLOCKED match)
    ('11111111-1111-1111-1111-111111111116', '22222222-2222-2222-2222-222222222226', 'Inappropriate messages and behavior', NOW() - INTERVAL '2 days'),
    -- Charlotte blocked Alexander after he was too persistent
    ('11111111-1111-1111-1111-111111111117', '22222222-2222-2222-2222-222222222230', 'Too aggressive in messaging', NOW() - INTERVAL '5 days'),
    -- Joshua blocked Harper after she didn't respond well to rejection
    ('22222222-2222-2222-2222-222222222241', '11111111-1111-1111-1111-111111111119', 'Harassment after unmatching', NOW() - INTERVAL '3 days')
ON CONFLICT (blocker_id, blocked_id) DO NOTHING;

-- ========================================
-- REPORTS (Abuse reports)
-- ========================================
INSERT INTO reports (reporter_id, reported_user_id, match_id, reason, description, status, created_at)
VALUES
    -- Mia reported Elijah for harassment
    ('11111111-1111-1111-1111-111111111116', '22222222-2222-2222-2222-222222222226', '33333333-3333-3333-3333-333333333336', 'HARASSMENT', 'User sent multiple inappropriate messages after I asked them to stop. Made me uncomfortable with their language and persistence.', 'UNDER_REVIEW', NOW() - INTERVAL '2 days'),
    -- Charlotte reported Alexander for spam-like behavior
    ('11111111-1111-1111-1111-111111111117', '22222222-2222-2222-2222-222222222230', NULL, 'SPAM', 'User sent 50+ messages in a short period after being ignored. Appears to be spamming or automated behavior.', 'RESOLVED', NOW() - INTERVAL '4 days')
ON CONFLICT DO NOTHING;

-- ========================================
-- PHOTOS FOR NEW USERS
-- ========================================
INSERT INTO photos (user_id, url, thumbnail_url, display_order, is_primary, is_verified, moderation_status)
VALUES
    ('44444444-4444-4444-4444-444444444441', 'https://randomuser.me/api/portraits/women/31.jpg', 'https://randomuser.me/api/portraits/women/31.jpg', 0, true, true, 'APPROVED'),
    ('44444444-4444-4444-4444-444444444442', 'https://randomuser.me/api/portraits/men/31.jpg', 'https://randomuser.me/api/portraits/men/31.jpg', 0, true, false, 'APPROVED'),
    ('44444444-4444-4444-4444-444444444443', 'https://randomuser.me/api/portraits/women/32.jpg', 'https://randomuser.me/api/portraits/women/32.jpg', 0, true, true, 'APPROVED'),
    ('44444444-4444-4444-4444-444444444444', 'https://randomuser.me/api/portraits/men/32.jpg', 'https://randomuser.me/api/portraits/men/32.jpg', 0, true, false, 'APPROVED'),
    ('44444444-4444-4444-4444-444444444445', 'https://randomuser.me/api/portraits/lego/1.jpg', 'https://randomuser.me/api/portraits/lego/1.jpg', 0, true, true, 'APPROVED'),
    ('44444444-4444-4444-4444-444444444446', 'https://randomuser.me/api/portraits/lego/2.jpg', 'https://randomuser.me/api/portraits/lego/2.jpg', 0, true, false, 'APPROVED')
ON CONFLICT DO NOTHING;

-- ========================================
-- EDGE CASE TESTING DATA
-- ========================================

-- User with expired premium (for testing premium expiration edge case)
-- Note: This would need the premium_expires_at column if it exists

-- User with very old last_active (for testing inactive users)
UPDATE users SET last_active = NOW() - INTERVAL '90 days' WHERE id = '11111111-1111-1111-1111-111111111131';

-- Add some additional swipes between geographic users for testing
INSERT INTO swipes (user_id, target_user_id, action, created_at)
VALUES
    -- LA users swiping
    ('44444444-4444-4444-4444-444444444441', '44444444-4444-4444-4444-444444444442', 'LIKE', NOW() - INTERVAL '1 day'),
    ('44444444-4444-4444-4444-444444444442', '44444444-4444-4444-4444-444444444441', 'SUPER_LIKE', NOW() - INTERVAL '12 hours'),
    -- Chicago users swiping
    ('44444444-4444-4444-4444-444444444443', '44444444-4444-4444-4444-444444444444', 'LIKE', NOW() - INTERVAL '2 days'),
    -- Austin users swiping (testing BOTH preference)
    ('44444444-4444-4444-4444-444444444445', '44444444-4444-4444-4444-444444444446', 'LIKE', NOW() - INTERVAL '1 day'),
    ('44444444-4444-4444-4444-444444444446', '44444444-4444-4444-4444-444444444445', 'LIKE', NOW() - INTERVAL '8 hours')
ON CONFLICT (user_id, target_user_id) DO NOTHING;

-- Add matches for new geographic users (LA and Austin pairs matched)
INSERT INTO matches (id, user1_id, user2_id, status, matched_at, ended_at, ended_by)
VALUES
    ('33333333-3333-3333-3333-333333333337', '44444444-4444-4444-4444-444444444441', '44444444-4444-4444-4444-444444444442', 'ACTIVE', NOW() - INTERVAL '12 hours', NULL, NULL),
    ('33333333-3333-3333-3333-333333333338', '44444444-4444-4444-4444-444444444445', '44444444-4444-4444-4444-444444444446', 'ACTIVE', NOW() - INTERVAL '8 hours', NULL, NULL)
ON CONFLICT (user1_id, user2_id) DO NOTHING;

-- Add match scores for new matches
INSERT INTO match_scores (match_id, score, factors)
VALUES
    ('33333333-3333-3333-3333-333333333336', 45, '{"interest_match": 15, "age_compatibility": 10, "preference_alignment": 20}'::jsonb),
    ('33333333-3333-3333-3333-333333333337', 85, '{"interest_match": 35, "age_compatibility": 25, "preference_alignment": 25}'::jsonb),
    ('33333333-3333-3333-3333-333333333338', 92, '{"interest_match": 40, "age_compatibility": 27, "preference_alignment": 25}'::jsonb)
ON CONFLICT (match_id) DO NOTHING;

-- ========================================
-- REFRESH MATERIALIZED VIEWS
-- ========================================
REFRESH MATERIALIZED VIEW feed_candidates;
REFRESH MATERIALIZED VIEW daily_swipe_counts;
REFRESH MATERIALIZED VIEW match_activity;
