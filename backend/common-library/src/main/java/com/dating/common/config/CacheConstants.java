package com.dating.common.config;

/**
 * Constants for Redis cache configuration.
 * Defines cache names and TTL (Time To Live) values
 * used across all microservices.
 */
public final class CacheConstants {

    private CacheConstants() {
        // Prevent instantiation
    }

    // ===========================================
    // CACHE NAMES
    // ===========================================

    /**
     * Cache for user profiles.
     */
    public static final String USERS_CACHE = "users";

    /**
     * Cache for user preferences.
     */
    public static final String USER_PREFERENCES_CACHE = "user_preferences";

    /**
     * Cache for user's match feed.
     */
    public static final String USER_FEED_CACHE = "user_feed";

    /**
     * Cache for matches.
     */
    public static final String MATCHES_CACHE = "matches";

    /**
     * Cache for user's active matches list.
     */
    public static final String USER_MATCHES_CACHE = "user_matches";

    /**
     * Cache for recommendations.
     */
    public static final String RECOMMENDATIONS_CACHE = "recommendations";

    /**
     * Cache for conversations.
     */
    public static final String CONVERSATIONS_CACHE = "conversations";

    /**
     * Cache for JWT token blacklist.
     */
    public static final String TOKEN_BLACKLIST_CACHE = "token_blacklist";

    /**
     * Cache for user sessions.
     */
    public static final String SESSION_CACHE = "sessions";

    /**
     * Cache for rate limiting.
     */
    public static final String RATE_LIMIT_CACHE = "rate_limit";

    // ===========================================
    // TTL VALUES (in seconds)
    // ===========================================

    /**
     * TTL for user profiles: 1 hour.
     * Relatively short because profiles can be updated frequently.
     */
    public static final long USER_TTL_SECONDS = 3600L; // 1 hour

    /**
     * TTL for user preferences: 1 hour.
     */
    public static final long USER_PREFERENCES_TTL_SECONDS = 3600L; // 1 hour

    /**
     * TTL for match feed: 24 hours.
     * Longer because feed generation is expensive.
     */
    public static final long FEED_TTL_SECONDS = 86400L; // 24 hours

    /**
     * TTL for matches: 24 hours.
     */
    public static final long MATCHES_TTL_SECONDS = 86400L; // 24 hours

    /**
     * TTL for recommendations: 24 hours.
     * Long because ML scoring is expensive.
     */
    public static final long RECOMMENDATIONS_TTL_SECONDS = 86400L; // 24 hours

    /**
     * TTL for conversations: 1 hour.
     */
    public static final long CONVERSATIONS_TTL_SECONDS = 3600L; // 1 hour

    /**
     * TTL for sessions: 30 minutes.
     * Security consideration - shorter is better.
     */
    public static final long SESSION_TTL_SECONDS = 1800L; // 30 minutes

    /**
     * TTL for token blacklist: 7 days.
     * Must match refresh token expiration.
     */
    public static final long TOKEN_BLACKLIST_TTL_SECONDS = 604800L; // 7 days

    /**
     * TTL for rate limit windows: 1 minute.
     */
    public static final long RATE_LIMIT_TTL_SECONDS = 60L; // 1 minute

    // ===========================================
    // RATE LIMITING CONSTANTS
    // ===========================================

    /**
     * Maximum swipes per day.
     */
    public static final int MAX_SWIPES_PER_DAY = 100;

    /**
     * Maximum super likes per day.
     */
    public static final int MAX_SUPER_LIKES_PER_DAY = 5;

    /**
     * Maximum messages per minute.
     */
    public static final int MAX_MESSAGES_PER_MINUTE = 30;

    /**
     * Maximum login attempts per hour.
     */
    public static final int MAX_LOGIN_ATTEMPTS_PER_HOUR = 10;

    // ===========================================
    // CACHE KEY PREFIXES
    // ===========================================

    /**
     * Prefix for user-related cache keys.
     */
    public static final String USER_KEY_PREFIX = "user:";

    /**
     * Prefix for match-related cache keys.
     */
    public static final String MATCH_KEY_PREFIX = "match:";

    /**
     * Prefix for session-related cache keys.
     */
    public static final String SESSION_KEY_PREFIX = "session:";

    /**
     * Prefix for token-related cache keys.
     */
    public static final String TOKEN_KEY_PREFIX = "token:";
}
