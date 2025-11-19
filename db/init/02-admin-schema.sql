-- Admin Dashboard Database Schema
--
-- PURPOSE: Additional tables for admin functionality
--
-- TABLES:
-- - admin_audit_log: Immutable audit trail for admin actions
-- - app_configuration: Application settings with audit trail
-- - user_roles: Role assignments for users
-- - reports: User reports for moderation

-- ========================================
-- USER ROLES TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS user_roles (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    role VARCHAR(50) NOT NULL,
    granted_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    granted_by UUID REFERENCES users(id) ON DELETE SET NULL,
    UNIQUE(user_id, role),
    CONSTRAINT valid_role CHECK (role IN ('ROLE_USER', 'ROLE_MODERATOR', 'ROLE_ANALYST', 'ROLE_ADMIN'))
);

CREATE INDEX idx_user_roles_user_id ON user_roles(user_id);
CREATE INDEX idx_user_roles_role ON user_roles(role);

-- ========================================
-- ADMIN AUDIT LOG TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS admin_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id UUID,
    details JSONB,
    ip_address INET,
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_admin_audit_admin_time ON admin_audit_log(admin_id, created_at DESC);
CREATE INDEX idx_admin_audit_target ON admin_audit_log(target_type, target_id);
CREATE INDEX idx_admin_audit_action ON admin_audit_log(action);
CREATE INDEX idx_admin_audit_created_at ON admin_audit_log(created_at DESC);

-- ========================================
-- APP CONFIGURATION TABLE
-- ========================================
CREATE TABLE IF NOT EXISTS app_configuration (
    key VARCHAR(255) PRIMARY KEY,
    value TEXT NOT NULL,
    value_type VARCHAR(50) DEFAULT 'STRING',
    category VARCHAR(100) NOT NULL,
    description TEXT,
    is_sensitive BOOLEAN DEFAULT false,
    updated_by UUID REFERENCES users(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_value_type CHECK (value_type IN ('STRING', 'INTEGER', 'BOOLEAN', 'JSON', 'DOUBLE'))
);

CREATE INDEX idx_app_config_category ON app_configuration(category);

-- ========================================
-- USER REPORTS TABLE (for moderation)
-- ========================================
CREATE TABLE IF NOT EXISTS user_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reported_user_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    reason VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(50) DEFAULT 'PENDING',
    reviewed_by UUID REFERENCES users(id) ON DELETE SET NULL,
    reviewed_at TIMESTAMP,
    action_taken VARCHAR(100),
    admin_notes TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT valid_report_reason CHECK (reason IN ('SPAM', 'HARASSMENT', 'INAPPROPRIATE_CONTENT', 'FAKE_PROFILE', 'SCAM', 'OTHER')),
    CONSTRAINT valid_report_status CHECK (status IN ('PENDING', 'UNDER_REVIEW', 'RESOLVED', 'DISMISSED')),
    CONSTRAINT no_self_report CHECK (reporter_id != reported_user_id)
);

CREATE INDEX idx_reports_reporter ON user_reports(reporter_id);
CREATE INDEX idx_reports_reported_user ON user_reports(reported_user_id);
CREATE INDEX idx_reports_status ON user_reports(status);
CREATE INDEX idx_reports_created_at ON user_reports(created_at DESC);

-- ========================================
-- SYSTEM METRICS TABLE (for monitoring)
-- ========================================
CREATE TABLE IF NOT EXISTS system_metrics (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    metric_name VARCHAR(100) NOT NULL,
    metric_value DOUBLE PRECISION NOT NULL,
    metric_type VARCHAR(50) NOT NULL,
    service_name VARCHAR(100),
    metadata JSONB,
    recorded_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_system_metrics_name ON system_metrics(metric_name);
CREATE INDEX idx_system_metrics_service ON system_metrics(service_name);
CREATE INDEX idx_system_metrics_recorded_at ON system_metrics(recorded_at DESC);

-- ========================================
-- DEFAULT DATA
-- ========================================

-- Insert default configuration values
INSERT INTO app_configuration (key, value, value_type, category, description) VALUES
    ('match.algorithm.weight.interests', '40', 'INTEGER', 'matching', 'Weight for interest matching (0-100)'),
    ('match.algorithm.weight.age', '30', 'INTEGER', 'matching', 'Weight for age preference matching (0-100)'),
    ('match.algorithm.weight.distance', '30', 'INTEGER', 'matching', 'Weight for distance preference matching (0-100)'),
    ('rate_limit.swipes_per_day', '100', 'INTEGER', 'rate_limits', 'Maximum swipes per user per day'),
    ('rate_limit.super_likes_per_day', '5', 'INTEGER', 'rate_limits', 'Maximum super likes per user per day'),
    ('rate_limit.messages_per_hour', '50', 'INTEGER', 'rate_limits', 'Maximum messages per user per hour'),
    ('feature.super_like_enabled', 'true', 'BOOLEAN', 'features', 'Enable/disable super like feature'),
    ('feature.video_profiles_enabled', 'false', 'BOOLEAN', 'features', 'Enable/disable video profile uploads'),
    ('feature.read_receipts_enabled', 'true', 'BOOLEAN', 'features', 'Enable/disable message read receipts'),
    ('security.max_login_attempts', '5', 'INTEGER', 'security', 'Maximum login attempts before lockout'),
    ('security.lockout_duration_minutes', '30', 'INTEGER', 'security', 'Account lockout duration in minutes'),
    ('notification.email_enabled', 'true', 'BOOLEAN', 'notifications', 'Enable email notifications'),
    ('notification.push_enabled', 'true', 'BOOLEAN', 'notifications', 'Enable push notifications')
ON CONFLICT (key) DO NOTHING;

-- ========================================
-- COMMENTS
-- ========================================
COMMENT ON TABLE user_roles IS 'Role-based access control for users (USER, MODERATOR, ANALYST, ADMIN)';
COMMENT ON TABLE admin_audit_log IS 'Immutable audit trail for all admin actions - compliance requirement';
COMMENT ON TABLE app_configuration IS 'Application configuration with type safety and audit trail';
COMMENT ON TABLE user_reports IS 'User-submitted reports for content moderation';
COMMENT ON TABLE system_metrics IS 'Time-series system performance metrics';

-- Success message
DO $$
BEGIN
    RAISE NOTICE 'Admin schema initialized successfully!';
END $$;
