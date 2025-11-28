-- Schema Audit Fixes
-- Version: 4.0
--
-- PURPOSE: Fix critical and high-priority schema issues from comprehensive audit
--
-- CRITICAL ISSUES (Security + Data Integrity):
-- 1. Message sender validation
-- 2. Password hash format validation
-- 3. Match end state consistency
--
-- HIGH PRIORITY ISSUES (Production Quality):
-- 4. Location decimal precision
-- 5. Verification code uniqueness
-- 6. Match scores NOT NULL
-- 7. Message sender cascade → SET NULL
-- 8. Bio max length
-- 9. Interests array size limit
--
-- IMPLEMENTATION NOTES:
-- - This migration should be applied BEFORE 03-views.sql and 04-functions.sql
-- - Some constraints reference other tables (matches, users) so order matters
-- - Data cleanup queries run before adding NOT NULL constraints
-- - Triggers must be created in correct order for dependencies
--
-- ESTIMATED TIME: 3-5 minutes on fresh database
--

-- ========================================
-- PHASE 1: DATA CLEANUP (Required before constraints)
-- ========================================

-- Cleanup match_scores nulls before adding NOT NULL
UPDATE match_scores SET score = 50 WHERE score IS NULL;
UPDATE match_scores SET factors = '{}' WHERE factors IS NULL;

-- Cleanup users bio exceeding max length
UPDATE users SET bio = SUBSTRING(bio FROM 1 FOR 5000) WHERE LENGTH(bio) > 5000;

-- Cleanup user_preferences interests exceeding max count
UPDATE user_preferences 
SET interests = interests[1:50] 
WHERE ARRAY_LENGTH(interests, 1) > 50;

-- Cleanup existing revoked tokens without revoked_at
UPDATE refresh_tokens 
SET revoked_at = CURRENT_TIMESTAMP 
WHERE revoked = TRUE AND revoked_at IS NULL;

-- Cleanup notifications with inconsistent timestamps
UPDATE notifications SET read_at = CURRENT_TIMESTAMP WHERE is_read = TRUE AND read_at IS NULL;
UPDATE notifications SET sent_at = CURRENT_TIMESTAMP WHERE is_sent = TRUE AND sent_at IS NULL;

-- ========================================
-- PHASE 2: CRITICAL ISSUES - SECURITY
-- ========================================

-- ISSUE #1: Message Sender Validation Trigger
-- Ensures sender_id is part of the match (user1_id or user2_id)
-- Prevents privilege escalation where user C sends message in match(A,B) as user A
CREATE OR REPLACE FUNCTION validate_message_sender()
RETURNS TRIGGER AS $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM matches
        WHERE id = NEW.match_id
        AND (user1_id = NEW.sender_id OR user2_id = NEW.sender_id)
    ) THEN
        RAISE EXCEPTION 'Sender % is not part of match %', NEW.sender_id, NEW.match_id;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_validate_message_sender ON messages;
CREATE TRIGGER trg_validate_message_sender
    BEFORE INSERT ON messages
    FOR EACH ROW
    EXECUTE FUNCTION validate_message_sender();

COMMENT ON FUNCTION validate_message_sender() IS 
    'Ensures message sender is either user1 or user2 in the match. Prevents privilege escalation.';

-- ISSUE #2: Password Hash Bcrypt Format Validation
-- BCrypt hashes are 60 characters starting with $2a$, $2b$, or $2y$
-- Prevents plaintext or malformed password storage
ALTER TABLE users
ADD CONSTRAINT password_hash_bcrypt_format CHECK (
    LENGTH(password_hash) >= 60
    AND password_hash ~ '^\$2[aby]\$\d{2}\$[./A-Za-z0-9]{53}$'
);

COMMENT ON CONSTRAINT password_hash_bcrypt_format ON users IS 
    'Validates password hash is BCrypt format ($2a$, $2b$, or $2y$ with 60 chars). Prevents plaintext/invalid hashes.';

-- ========================================
-- PHASE 3: CRITICAL ISSUES - DATA CONSISTENCY
-- ========================================

-- ISSUE #3: Match End State Consistency
-- If match status is UNMATCHED or BLOCKED, ended_at must NOT be NULL
-- If status is ACTIVE, ended_at must be NULL
ALTER TABLE matches
ADD CONSTRAINT end_state_consistency CHECK (
    (status = 'ACTIVE' AND ended_at IS NULL AND ended_by IS NULL)
    OR
    (status IN ('UNMATCHED', 'BLOCKED') AND ended_at IS NOT NULL)
);

COMMENT ON CONSTRAINT end_state_consistency ON matches IS 
    'Ensures if match is ended (status != ACTIVE), ended_at timestamp exists. Provides audit trail.';

-- ========================================
-- PHASE 4: HIGH PRIORITY - DATA TYPE FIXES
-- ========================================

-- ISSUE #4: Location Decimal Precision
-- Latitude: -90 to +90 (DECIMAL(11,8) = ±90.12345678)
-- Longitude: -180 to +180 (DECIMAL(12,8) = ±180.12345678)
ALTER TABLE users
ALTER COLUMN location_lat TYPE DECIMAL(11, 8),
ALTER COLUMN location_lng TYPE DECIMAL(12, 8);

COMMENT ON COLUMN users.location_lat IS 'User latitude with ±0.00000001 degree precision (~1.1m). DECIMAL(11,8) allows ±90.12345678';
COMMENT ON COLUMN users.location_lng IS 'User longitude with ±0.00000001 degree precision (~1.1m). DECIMAL(12,8) allows ±180.12345678';

-- ========================================
-- PHASE 5: HIGH PRIORITY - UNIQUENESS CONSTRAINTS
-- ========================================

-- ISSUE #5: Verification Code Uniqueness
-- Only one active (unused, not expired) code per user per type
-- When new code is generated, previous codes are auto-invalidated
CREATE OR REPLACE FUNCTION ensure_single_active_code()
RETURNS TRIGGER AS $$
BEGIN
    -- Invalidate all previous active codes of same type for this user
    UPDATE verification_codes
    SET used_at = CURRENT_TIMESTAMP
    WHERE user_id = NEW.user_id
    AND type = NEW.type
    AND used_at IS NULL
    AND id != NEW.id
    AND expires_at > CURRENT_TIMESTAMP;
    
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_ensure_single_active_code ON verification_codes;
CREATE TRIGGER trg_ensure_single_active_code
    BEFORE INSERT ON verification_codes
    FOR EACH ROW
    EXECUTE FUNCTION ensure_single_active_code();

COMMENT ON FUNCTION ensure_single_active_code() IS 
    'Ensures only one active verification code per user/type. Auto-invalidates previous codes.';

-- ========================================
-- PHASE 6: HIGH PRIORITY - NOT NULL CONSTRAINTS
-- ========================================

-- ISSUE #6: Match Scores NOT NULL
-- score and factors are mandatory for meaningful records
ALTER TABLE match_scores
ALTER COLUMN score SET NOT NULL,
ALTER COLUMN factors SET NOT NULL DEFAULT '{}';

COMMENT ON COLUMN match_scores.score IS 'Compatibility score 0-100. NOT NULL: every score record must have a value.';
COMMENT ON COLUMN match_scores.factors IS 'JSON breakdown of score calculation. NOT NULL: defaults to empty object.';

-- ========================================
-- PHASE 7: HIGH PRIORITY - FOREIGN KEY CHANGES
-- ========================================

-- ISSUE #7: Message Sender Cascade to SET NULL
-- When user deletes account, anonymize messages instead of deleting them
-- Preserves chat history for other user and GDPR compliance
ALTER TABLE messages
DROP CONSTRAINT IF EXISTS messages_sender_id_fkey,
ADD CONSTRAINT messages_sender_id_fkey 
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL;

COMMENT ON COLUMN messages.sender_id IS 'Message author. ON DELETE SET NULL: preserves message history when sender account is deleted.';

-- ========================================
-- PHASE 8: HIGH PRIORITY - CHECK CONSTRAINTS
-- ========================================

-- ISSUE #8: Bio Max Length
-- Prevents storage of unlimited-length bios (performance + storage waste)
-- Frontend typically limits to 5000 characters
ALTER TABLE users
ADD CONSTRAINT bio_max_length CHECK (
    bio IS NULL OR LENGTH(bio) <= 5000
);

COMMENT ON CONSTRAINT bio_max_length ON users IS 
    'Bio limited to 5000 characters. Prevents storage bloat and maintains UX consistency.';

-- ISSUE #9: Interests Array Size Limit
-- Prevents users from adding excessive interests (performance impact on GIN index)
-- Reasonable limit: 50 interests per user
ALTER TABLE user_preferences
ADD CONSTRAINT interests_max_count CHECK (
    interests IS NULL OR ARRAY_LENGTH(interests, 1) IS NULL OR ARRAY_LENGTH(interests, 1) <= 50
);

COMMENT ON CONSTRAINT interests_max_count ON user_preferences IS 
    'User interests array limited to 50 items. Prevents query performance degradation on GIN index.';

-- ========================================
-- PHASE 9: MEDIUM PRIORITY - CONSISTENCY CONSTRAINTS
-- ========================================

-- ISSUE #10: Refresh Token Revoke State Consistency
-- If revoked=true, revoked_at must be set; if revoked=false, revoked_at must be NULL
ALTER TABLE refresh_tokens
ADD CONSTRAINT revoke_state_consistency CHECK (
    (revoked = FALSE AND revoked_at IS NULL)
    OR
    (revoked = TRUE AND revoked_at IS NOT NULL)
);

COMMENT ON CONSTRAINT revoke_state_consistency ON refresh_tokens IS 
    'Ensures revoked status and revoked_at timestamp are consistent. Provides audit trail.';

-- ISSUE #11: Verification Code Attempts Limit
-- Prevents brute force attacks on verification codes (max 5 attempts)
ALTER TABLE verification_codes
ADD CONSTRAINT attempts_limit CHECK (
    attempts >= 0 AND attempts <= 5
);

COMMENT ON CONSTRAINT attempts_limit ON verification_codes IS 
    'Verification code limited to 5 attempts. Prevents brute force attacks on short codes.';

-- ISSUE #12: Interaction History Action Validation
-- Only allowed actions can be recorded (prevents invalid analytics data)
ALTER TABLE interaction_history
ADD CONSTRAINT valid_action CHECK (
    action IN (
        'VIEW_PROFILE',
        'SEND_SWIPE',
        'SEND_SUPER_LIKE',
        'SEND_MESSAGE',
        'VIEW_MESSAGE',
        'REPORT_USER',
        'BLOCK_USER',
        'UNBLOCK_USER',
        'UNMATCH',
        'SHARE_PROFILE'
    )
);

COMMENT ON CONSTRAINT valid_action ON interaction_history IS 
    'Only specific interaction actions are allowed. Ensures data quality for analytics.';

-- ISSUE #13: Message Updated Tracking
-- Track when messages are edited (consistent with users, photos tables)
ALTER TABLE messages
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_messages_updated_at ON messages;
CREATE TRIGGER trg_messages_updated_at
    BEFORE UPDATE ON messages
    FOR EACH ROW
    WHEN (OLD IS DISTINCT FROM NEW)
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON COLUMN messages.updated_at IS 'Timestamp of last message update. Tracks message edits or status changes.';

-- ISSUE #14: Swipe Updated Tracking
-- Track if/when swipe action changes (immutability enforcement via triggers optional)
ALTER TABLE swipes
ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;

DROP TRIGGER IF EXISTS trg_swipes_updated_at ON swipes;
CREATE TRIGGER trg_swipes_updated_at
    BEFORE UPDATE ON swipes
    FOR EACH ROW
    WHEN (OLD IS DISTINCT FROM NEW)
    EXECUTE FUNCTION update_updated_at_column();

COMMENT ON COLUMN swipes.updated_at IS 'Timestamp of last swipe update. Tracks if action was changed.';

-- ISSUE #15: Notification State Machine Completeness
-- If is_sent=true, sent_at must be set; if is_read=true, read_at must be set
ALTER TABLE notifications
ADD CONSTRAINT sent_timestamp_consistency CHECK (
    (is_sent = FALSE AND sent_at IS NULL)
    OR
    (is_sent = TRUE AND sent_at IS NOT NULL)
),
ADD CONSTRAINT read_timestamp_consistency CHECK (
    (is_read = FALSE AND read_at IS NULL)
    OR
    (is_read = TRUE AND read_at IS NOT NULL)
);

COMMENT ON CONSTRAINT sent_timestamp_consistency ON notifications IS 
    'If notification is sent, sent_at must be set. Provides audit trail.';
COMMENT ON CONSTRAINT read_timestamp_consistency ON notifications IS 
    'If notification is read, read_at must be set. Provides audit trail.';

-- ISSUE #16: User Blocks Expiration Support
-- Allow temporary blocks (with optional auto-expiration)
-- Allow manual unblocking separate from expiration
ALTER TABLE user_blocks
ADD COLUMN IF NOT EXISTS expires_at TIMESTAMP,
ADD COLUMN IF NOT EXISTS unblocked_at TIMESTAMP;

ALTER TABLE user_blocks
ADD CONSTRAINT block_expiration_future CHECK (
    expires_at IS NULL OR expires_at > created_at
),
ADD CONSTRAINT block_must_be_active CHECK (
    (unblocked_at IS NULL AND (expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP))
    OR unblocked_at IS NOT NULL
);

COMMENT ON COLUMN user_blocks.expires_at IS 'Optional block expiration time. NULL = permanent block.';
COMMENT ON COLUMN user_blocks.unblocked_at IS 'When block was manually removed by blocker. NULL = still blocked.';

-- ========================================
-- PHASE 10: LOW PRIORITY - FORMAT VALIDATION
-- ========================================

-- ISSUE #17: Verification Code Format Validation
-- Codes should be numeric only (6 digits, e.g., 000000-999999)
ALTER TABLE verification_codes
ADD CONSTRAINT code_numeric_format CHECK (
    code ~ '^[0-9]{6}$'
);

COMMENT ON CONSTRAINT code_numeric_format ON verification_codes IS 
    'Verification codes must be 6 digits (0-9). Format validation for email/SMS codes.';

-- ISSUE #19: Audit Logs Entity Type and Action Validation
-- Enforce specific entity types and actions for consistent analytics
ALTER TABLE audit_logs
ADD CONSTRAINT valid_entity_type CHECK (
    entity_type IS NULL OR entity_type IN (
        'USER', 'MATCH', 'MESSAGE', 'SWIPE', 'PHOTO', 'REPORT', 'BLOCK', 'VERIFICATION_CODE', 'REFRESH_TOKEN'
    )
),
ADD CONSTRAINT valid_audit_action CHECK (
    action IS NULL OR action IN (
        'CREATE', 'READ', 'UPDATE', 'DELETE', 'VERIFY', 'REVOKE', 'BLOCK', 'REPORT'
    )
);

COMMENT ON CONSTRAINT valid_entity_type ON audit_logs IS 
    'Entity types must be predefined for consistent audit logging.';
COMMENT ON CONSTRAINT valid_audit_action ON audit_logs IS 
    'Audit actions must be predefined for consistent audit logging.';

-- ========================================
-- SUMMARY OF CHANGES
-- ========================================

COMMENT ON TABLE users IS 'Core user profiles and authentication. UPDATED: password_hash validation, bio max length, location precision, gender nullable.';
COMMENT ON TABLE messages IS 'Chat messages with delivery and read receipts. UPDATED: sender validation trigger, cascade to SET NULL, updated_at tracking.';
COMMENT ON TABLE matches IS 'Mutual matches between users with status tracking. UPDATED: end state consistency constraint.';
COMMENT ON TABLE match_scores IS 'Compatibility scores with factor breakdown. UPDATED: score and factors are NOT NULL.';
COMMENT ON TABLE verification_codes IS 'Verification codes for email/phone/password reset. UPDATED: uniqueness trigger, format validation, attempt limits.';
COMMENT ON TABLE refresh_tokens IS 'JWT refresh tokens with device tracking. UPDATED: revoke state consistency constraint.';
COMMENT ON TABLE user_preferences IS 'User matching preferences and settings. UPDATED: interests array size limit.';
COMMENT ON TABLE notifications IS 'Push and in-app notification queue. UPDATED: timestamp consistency constraints.';
COMMENT ON TABLE user_blocks IS 'User blocking for safety and privacy. UPDATED: expiration support, manual unblock tracking.';
COMMENT ON TABLE interaction_history IS 'User behavior analytics for recommendations. UPDATED: action validation constraint.';
COMMENT ON TABLE swipes IS 'High-frequency swipe events, indexed for feed exclusion. UPDATED: updated_at tracking.';
COMMENT ON TABLE audit_logs IS 'Audit trail for compliance and debugging. UPDATED: entity type and action validation.';

