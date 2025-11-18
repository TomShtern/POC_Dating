-- Flyway Migration V2: Add Indexes
-- POC Dating Application
--
-- This migration adds all performance indexes

-- ========================================
-- USERS INDEXES
-- ========================================
CREATE INDEX IF NOT EXISTS idx_users_email ON users(email);
CREATE INDEX IF NOT EXISTS idx_users_username ON users(username);
CREATE INDEX IF NOT EXISTS idx_users_gender_age ON users(gender, age);
CREATE INDEX IF NOT EXISTS idx_users_gender_dob ON users(gender, date_of_birth);
CREATE INDEX IF NOT EXISTS idx_users_active ON users(status, last_active DESC)
    WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_users_location ON users(location_lat, location_lng)
    WHERE location_lat IS NOT NULL AND location_lng IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_users_premium ON users(is_premium)
    WHERE is_premium = true;
CREATE INDEX IF NOT EXISTS idx_users_bio_trgm ON users USING gin(bio gin_trgm_ops)
    WHERE bio IS NOT NULL;

-- ========================================
-- USER PREFERENCES INDEXES
-- ========================================
CREATE INDEX IF NOT EXISTS idx_user_preferences_user_id ON user_preferences(user_id);
CREATE INDEX IF NOT EXISTS idx_user_preferences_interests ON user_preferences USING gin(interests);

-- ========================================
-- PHOTOS INDEXES
-- ========================================
CREATE INDEX IF NOT EXISTS idx_photos_user_order ON photos(user_id, display_order);
CREATE INDEX IF NOT EXISTS idx_photos_primary ON photos(user_id, is_primary)
    WHERE is_primary = true;
CREATE INDEX IF NOT EXISTS idx_photos_moderation ON photos(moderation_status, created_at)
    WHERE moderation_status = 'PENDING';

-- ========================================
-- SWIPES INDEXES
-- ========================================
CREATE INDEX IF NOT EXISTS idx_swipes_user_target ON swipes(user_id, target_user_id);
CREATE INDEX IF NOT EXISTS idx_swipes_user_time ON swipes(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_swipes_target_user ON swipes(target_user_id);
CREATE INDEX IF NOT EXISTS idx_swipes_likes ON swipes(target_user_id, user_id, action)
    WHERE action IN ('LIKE', 'SUPER_LIKE');

-- ========================================
-- MATCHES INDEXES
-- ========================================
CREATE INDEX IF NOT EXISTS idx_matches_user1 ON matches(user1_id);
CREATE INDEX IF NOT EXISTS idx_matches_user2 ON matches(user2_id);
CREATE INDEX IF NOT EXISTS idx_matches_active_user1 ON matches(user1_id, matched_at DESC)
    WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_matches_active_user2 ON matches(user2_id, matched_at DESC)
    WHERE status = 'ACTIVE';
CREATE INDEX IF NOT EXISTS idx_matches_time ON matches(matched_at DESC);
CREATE INDEX IF NOT EXISTS idx_matches_status ON matches(status)
    WHERE status = 'ACTIVE';

-- ========================================
-- MATCH SCORES INDEXES
-- ========================================
CREATE INDEX IF NOT EXISTS idx_match_scores_match ON match_scores(match_id);
CREATE INDEX IF NOT EXISTS idx_match_scores_score ON match_scores(score DESC);

-- ========================================
-- MESSAGES INDEXES
-- ========================================
CREATE INDEX IF NOT EXISTS idx_messages_match_time ON messages(match_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_messages_sender ON messages(sender_id);
CREATE INDEX IF NOT EXISTS idx_messages_unread ON messages(match_id, status)
    WHERE status != 'READ' AND deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_messages_recent ON messages(match_id, created_at DESC)
    WHERE deleted_at IS NULL;

-- ========================================
-- REFRESH TOKENS INDEXES
-- ========================================
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_user ON refresh_tokens(user_id);
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_expires ON refresh_tokens(expires_at)
    WHERE revoked = false;
CREATE INDEX IF NOT EXISTS idx_refresh_tokens_valid ON refresh_tokens(token_hash)
    WHERE revoked = false;

-- ========================================
-- RECOMMENDATIONS INDEXES
-- ========================================
CREATE INDEX IF NOT EXISTS idx_recommendations_user_score ON recommendations(user_id, score DESC);
CREATE INDEX IF NOT EXISTS idx_recommendations_target ON recommendations(target_user_id);
CREATE INDEX IF NOT EXISTS idx_recommendations_expires ON recommendations(expires_at)
    WHERE expires_at IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_recommendations_active ON recommendations(user_id, score DESC)
    WHERE expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP;

-- ========================================
-- USER BLOCKS INDEXES
-- ========================================
CREATE INDEX IF NOT EXISTS idx_user_blocks_blocker ON user_blocks(blocker_id);
CREATE INDEX IF NOT EXISTS idx_user_blocks_blocked ON user_blocks(blocked_id);
CREATE INDEX IF NOT EXISTS idx_user_blocks_pair ON user_blocks(blocker_id, blocked_id);

-- ========================================
-- NOTIFICATIONS INDEXES
-- ========================================
CREATE INDEX IF NOT EXISTS idx_notifications_user ON notifications(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notifications_unread ON notifications(user_id, is_read)
    WHERE is_read = false;
CREATE INDEX IF NOT EXISTS idx_notifications_unsent ON notifications(is_sent, created_at)
    WHERE is_sent = false;

-- ========================================
-- VERIFICATION CODES INDEXES
-- ========================================
CREATE INDEX IF NOT EXISTS idx_verification_user ON verification_codes(user_id);
CREATE INDEX IF NOT EXISTS idx_verification_active ON verification_codes(user_id, type, expires_at)
    WHERE used_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_verification_expires ON verification_codes(expires_at)
    WHERE used_at IS NULL;

-- ========================================
-- INTERACTION HISTORY INDEXES
-- ========================================
CREATE INDEX IF NOT EXISTS idx_interaction_user ON interaction_history(user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_interaction_action ON interaction_history(action, created_at DESC);

-- ========================================
-- REPORTS INDEXES
-- ========================================
CREATE INDEX IF NOT EXISTS idx_reports_status ON reports(status, created_at)
    WHERE status IN ('PENDING', 'REVIEWING');
CREATE INDEX IF NOT EXISTS idx_reports_reporter ON reports(reporter_id);
CREATE INDEX IF NOT EXISTS idx_reports_reported ON reports(reported_user_id);

-- ========================================
-- AUDIT LOGS INDEXES
-- ========================================
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity_type, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_time ON audit_logs(created_at DESC);
CREATE INDEX IF NOT EXISTS idx_audit_user ON audit_logs(user_id, created_at DESC)
    WHERE user_id IS NOT NULL;

-- ========================================
-- UPDATE STATISTICS
-- ========================================
ANALYZE users;
ANALYZE user_preferences;
ANALYZE photos;
ANALYZE swipes;
ANALYZE matches;
ANALYZE messages;
ANALYZE recommendations;
ANALYZE notifications;
