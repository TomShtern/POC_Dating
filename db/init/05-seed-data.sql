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
    ('11111111-1111-1111-1111-111111111116', 'mia@test.com', 'mia_johnson', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Mia', 'Johnson', '1996-02-28', 'FEMALE', 'Fitness trainer and meal prep queen. Looking for a gym partner and life partner.', 'https://randomuser.me/api/portraits/women/6.jpg', 40.7614, -73.9776, false, false, 'ACTIVE', NOW() - INTERVAL '6 hours'),
    ('11111111-1111-1111-1111-111111111117', 'charlotte@test.com', 'charlotte_lee', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Charlotte', 'Lee', '1991-12-05', 'FEMALE', 'Bookworm and wine enthusiast. Will definitely judge you by your book collection.', 'https://randomuser.me/api/portraits/women/7.jpg', 40.7505, -73.9934, true, false, 'ACTIVE', NOW() - INTERVAL '3 hours'),
    ('11111111-1111-1111-1111-111111111118', 'amelia@test.com', 'amelia_kim', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Amelia', 'Kim', '1993-08-19', 'FEMALE', 'Data scientist who loves board games and craft beer. Let''s play Catan!', 'https://randomuser.me/api/portraits/women/8.jpg', 40.7282, -73.7949, true, true, 'ACTIVE', NOW() - INTERVAL '45 minutes'),
    ('11111111-1111-1111-1111-111111111119', 'harper@test.com', 'harper_davis', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Harper', 'Davis', '1997-04-03', 'FEMALE', 'Photographer and cat lover. Looking for someone to explore the city with.', 'https://randomuser.me/api/portraits/women/9.jpg', 40.7128, -74.0060, false, false, 'ACTIVE', NOW() - INTERVAL '8 hours'),
    ('11111111-1111-1111-1111-111111111120', 'evelyn@test.com', 'evelyn_rodriguez', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Evelyn', 'Rodriguez', '1989-10-25', 'FEMALE', 'Lawyer by profession, chef at heart. Make a mean paella.', 'https://randomuser.me/api/portraits/women/10.jpg', 40.7580, -73.9855, true, false, 'ACTIVE', NOW() - INTERVAL '2 days'),
    ('11111111-1111-1111-1111-111111111121', 'luna@test.com', 'luna_patel', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Luna', 'Patel', '1994-06-14', 'FEMALE', 'Startup founder and meditation enthusiast. Work hard, zen harder.', 'https://randomuser.me/api/portraits/women/11.jpg', 40.7484, -73.9857, true, true, 'ACTIVE', NOW() - INTERVAL '1 hour'),
    ('11111111-1111-1111-1111-111111111122', 'chloe@test.com', 'chloe_nguyen', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Chloe', 'Nguyen', '1996-01-07', 'FEMALE', 'UX designer and bubble tea addict. Can talk about fonts for hours.', 'https://randomuser.me/api/portraits/women/12.jpg', 40.7282, -73.7949, false, false, 'ACTIVE', NOW() - INTERVAL '5 hours'),
    ('11111111-1111-1111-1111-111111111123', 'penelope@test.com', 'penny_white', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Penelope', 'White', '1992-03-20', 'FEMALE', 'Elementary teacher who loves camping and stargazing.', 'https://randomuser.me/api/portraits/women/13.jpg', 40.6892, -74.0445, true, false, 'ACTIVE', NOW() - INTERVAL '7 hours'),
    ('11111111-1111-1111-1111-111111111124', 'layla@test.com', 'layla_harris', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Layla', 'Harris', '1995-11-30', 'FEMALE', 'Music producer and vinyl collector. My apartment is basically a record store.', 'https://randomuser.me/api/portraits/women/14.jpg', 40.7614, -73.9776, true, true, 'ACTIVE', NOW() - INTERVAL '20 minutes'),
    ('11111111-1111-1111-1111-111111111125', 'riley@test.com', 'riley_clark', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Riley', 'Clark', '1993-07-08', 'FEMALE', 'Veterinarian and marathon runner. Yes, I will want to meet your pets.', 'https://randomuser.me/api/portraits/women/15.jpg', 40.7505, -73.9934, false, false, 'ACTIVE', NOW() - INTERVAL '12 hours'),
    ('11111111-1111-1111-1111-111111111126', 'zoey@test.com', 'zoey_lewis', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Zoey', 'Lewis', '1990-09-15', 'FEMALE', 'Architect with a love for brutalist buildings and good coffee.', 'https://randomuser.me/api/portraits/women/16.jpg', 40.7282, -73.7949, true, false, 'ACTIVE', NOW() - INTERVAL '4 hours'),
    ('11111111-1111-1111-1111-111111111127', 'nora@test.com', 'nora_walker', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Nora', 'Walker', '1997-02-12', 'FEMALE', 'Grad student studying marine biology. Ask me about octopuses!', 'https://randomuser.me/api/portraits/women/17.jpg', 40.7128, -74.0060, true, true, 'ACTIVE', NOW() - INTERVAL '1 hour'),
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
    ('22222222-2222-2222-2222-222222222223', 'noah@test.com', 'noah_williams', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Noah', 'Williams', '1990-02-11', 'MALE', 'Architect and coffee snob. Let me take you to the best hidden cafes.', 'https://randomuser.me/api/portraits/men/3.jpg', 40.7484, -73.9857, false, false, 'ACTIVE', NOW() - INTERVAL '3 hours'),
    ('22222222-2222-2222-2222-222222222224', 'william@test.com', 'will_brown', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'William', 'Brown', '1995-07-06', 'MALE', 'Physical therapist and basketball player. 6''2" if that matters.', 'https://randomuser.me/api/portraits/men/4.jpg', 40.7282, -73.7949, true, false, 'ACTIVE', NOW() - INTERVAL '2 hours'),
    ('22222222-2222-2222-2222-222222222225', 'oliver@test.com', 'oliver_jones', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Oliver', 'Jones', '1992-11-29', 'MALE', 'Music teacher and vinyl enthusiast. Will make you a playlist.', 'https://randomuser.me/api/portraits/men/5.jpg', 40.6892, -74.0445, true, true, 'ACTIVE', NOW() - INTERVAL '30 minutes'),
    ('22222222-2222-2222-2222-222222222226', 'elijah@test.com', 'elijah_garcia', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Elijah', 'Garcia', '1994-04-15', 'MALE', 'Emergency room doctor and amateur stand-up comedian. Dark humor included.', 'https://randomuser.me/api/portraits/men/6.jpg', 40.7614, -73.9776, false, false, 'ACTIVE', NOW() - INTERVAL '6 hours'),
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
    ('22222222-2222-2222-2222-222222222244', 'daniel@test.com', 'daniel_ramirez', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Daniel', 'Ramirez', '1991-08-13', 'MALE', 'Civil engineer and volunteer firefighter. Building and protecting communities.', 'https://randomuser.me/api/portraits/men/24.jpg', 40.7282, -73.7949, false, false, 'ACTIVE', NOW() - INTERVAL '7 hours'),
    ('22222222-2222-2222-2222-222222222245', 'andrew@test.com', 'andrew_lewis', '$2a$12$LQv3c1yqBWVHxkd0LHAkCOYz6TtxMQJqhN8/X4.V.HN4XkT4POuaa', 'Andrew', 'Lewis', '1994-10-06', 'MALE', 'Psychologist and bookworm. Great listener, can recommend good reads.', 'https://randomuser.me/api/portraits/men/25.jpg', 40.7128, -74.0060, true, false, 'ACTIVE', NOW() - INTERVAL '2 hours')
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

-- ========================================
-- MATCHES (From mutual likes above)
-- ========================================
INSERT INTO matches (id, user1_id, user2_id, status, matched_at)
VALUES
    ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222221', 'ACTIVE', NOW() - INTERVAL '4 days'),
    ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111112', '22222222-2222-2222-2222-222222222222', 'ACTIVE', NOW() - INTERVAL '3 days'),
    ('33333333-3333-3333-3333-333333333333', '11111111-1111-1111-1111-111111111113', '22222222-2222-2222-2222-222222222223', 'ACTIVE', NOW() - INTERVAL '5 days'),
    ('33333333-3333-3333-3333-333333333334', '11111111-1111-1111-1111-111111111114', '22222222-2222-2222-2222-222222222224', 'ACTIVE', NOW() - INTERVAL '2 days'),
    ('33333333-3333-3333-3333-333333333335', '11111111-1111-1111-1111-111111111115', '22222222-2222-2222-2222-222222222225', 'ACTIVE', NOW() - INTERVAL '1 day')
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
    ('33333333-3333-3333-3333-333333333335', '11111111-1111-1111-1111-111111111115', 'Hey Oliver! Fellow travel lover here! What''s the best place you''ve visited?', 'DELIVERED', NOW() - INTERVAL '1 day', NULL)
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
-- NOTIFICATIONS
-- ========================================
INSERT INTO notifications (user_id, type, title, body, data, is_read, is_sent, sent_at, created_at)
VALUES
    ('11111111-1111-1111-1111-111111111111', 'NEW_MATCH', 'New Match!', 'You matched with James!', '{"match_id": "33333333-3333-3333-3333-333333333331"}'::jsonb, true, true, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('22222222-2222-2222-2222-222222222221', 'NEW_MATCH', 'New Match!', 'You matched with Emma!', '{"match_id": "33333333-3333-3333-3333-333333333331"}'::jsonb, true, true, NOW() - INTERVAL '4 days', NOW() - INTERVAL '4 days'),
    ('11111111-1111-1111-1111-111111111111', 'NEW_MESSAGE', 'New Message', 'James sent you a message', '{"match_id": "33333333-3333-3333-3333-333333333331"}'::jsonb, false, true, NOW() - INTERVAL '2 days', NOW() - INTERVAL '2 days')
ON CONFLICT DO NOTHING;

-- ========================================
-- REFRESH MATERIALIZED VIEWS
-- ========================================
REFRESH MATERIALIZED VIEW feed_candidates;
REFRESH MATERIALIZED VIEW daily_swipe_counts;
REFRESH MATERIALIZED VIEW match_activity;
