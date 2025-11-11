package com.dating.ui.security;

import com.vaadin.flow.server.VaadinSession;

/**
 * Utility class for security operations
 * - Get current user information from session
 * - Get JWT token for backend API calls
 * - Check authentication status
 */
public class SecurityUtils {

    private static final String USER_ID_ATTRIBUTE = "userId";
    private static final String AUTH_TOKEN_ATTRIBUTE = "authToken";
    private static final String USER_NAME_ATTRIBUTE = "userName";

    /**
     * Get the current user's ID from session
     */
    public static String getCurrentUserId() {
        VaadinSession session = VaadinSession.getCurrent();
        return session != null ? (String) session.getAttribute(USER_ID_ATTRIBUTE) : null;
    }

    /**
     * Get the current user's JWT token from session
     */
    public static String getCurrentToken() {
        VaadinSession session = VaadinSession.getCurrent();
        return session != null ? (String) session.getAttribute(AUTH_TOKEN_ATTRIBUTE) : null;
    }

    /**
     * Get the current user's name from session
     */
    public static String getCurrentUserName() {
        VaadinSession session = VaadinSession.getCurrent();
        return session != null ? (String) session.getAttribute(USER_NAME_ATTRIBUTE) : null;
    }

    /**
     * Check if user is authenticated
     */
    public static boolean isAuthenticated() {
        return getCurrentToken() != null && getCurrentUserId() != null;
    }

    /**
     * Store authentication information in session
     */
    public static void setAuthenticationInfo(String userId, String token, String userName) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(USER_ID_ATTRIBUTE, userId);
            session.setAttribute(AUTH_TOKEN_ATTRIBUTE, token);
            session.setAttribute(USER_NAME_ATTRIBUTE, userName);
        }
    }

    /**
     * Clear authentication information (logout)
     */
    public static void clearAuthentication() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(USER_ID_ATTRIBUTE, null);
            session.setAttribute(AUTH_TOKEN_ATTRIBUTE, null);
            session.setAttribute(USER_NAME_ATTRIBUTE, null);
            session.close();
        }
    }
}
