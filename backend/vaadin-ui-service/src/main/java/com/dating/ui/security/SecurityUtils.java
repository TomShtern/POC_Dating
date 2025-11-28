package com.dating.ui.security;

import com.vaadin.flow.server.VaadinSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Utility class for security operations
 * - Get current user information from session
 * - Get JWT token for backend API calls
 * - Check authentication status
 * - Manage user roles for authorization
 */
public class SecurityUtils {

    private static final String USER_ID_ATTRIBUTE = "userId";
    private static final String AUTH_TOKEN_ATTRIBUTE = "authToken";
    private static final String USER_NAME_ATTRIBUTE = "userName";
    private static final String USER_ROLES_ATTRIBUTE = "userRoles";

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
     * Get the current user's roles from session
     */
    @SuppressWarnings("unchecked")
    public static List<String> getCurrentUserRoles() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            List<String> roles = (List<String>) session.getAttribute(USER_ROLES_ATTRIBUTE);
            return roles != null ? roles : new ArrayList<>();
        }
        return new ArrayList<>();
    }

    /**
     * Check if user is authenticated
     */
    public static boolean isAuthenticated() {
        return getCurrentToken() != null && getCurrentUserId() != null;
    }

    /**
     * Check if current user has a specific role
     */
    public static boolean hasRole(String role) {
        return getCurrentUserRoles().contains(role);
    }

    /**
     * Check if current user is an admin
     */
    public static boolean isAdmin() {
        return hasRole("ROLE_ADMIN");
    }

    /**
     * Check if current user is a moderator
     */
    public static boolean isModerator() {
        return hasRole("ROLE_MODERATOR") || isAdmin();
    }

    /**
     * Check if current user is an analyst
     */
    public static boolean isAnalyst() {
        return hasRole("ROLE_ANALYST") || isAdmin();
    }

    /**
     * Check if current user has any admin-level role
     */
    public static boolean hasAdminAccess() {
        List<String> roles = getCurrentUserRoles();
        return roles.contains("ROLE_ADMIN") ||
                roles.contains("ROLE_MODERATOR") ||
                roles.contains("ROLE_ANALYST");
    }

    /**
     * Store authentication information in session
     */
    public static void setAuthenticationInfo(String userId, String token, String userName) {
        setAuthenticationInfo(userId, token, userName, List.of("ROLE_USER"));
    }

    /**
     * Store authentication information in session with roles
     */
    public static void setAuthenticationInfo(String userId, String token, String userName, List<String> roles) {
        VaadinSession session = VaadinSession.getCurrent();
        if (session != null) {
            session.setAttribute(USER_ID_ATTRIBUTE, userId);
            session.setAttribute(AUTH_TOKEN_ATTRIBUTE, token);
            session.setAttribute(USER_NAME_ATTRIBUTE, userName);
            session.setAttribute(USER_ROLES_ATTRIBUTE, new ArrayList<>(roles));

            // Set up Spring Security context for @RolesAllowed
            Collection<GrantedAuthority> authorities = new ArrayList<>();
            for (String role : roles) {
                authorities.add(new SimpleGrantedAuthority(role));
            }

            Authentication auth = new UsernamePasswordAuthenticationToken(
                    userId, null, authorities
            );
            SecurityContextHolder.getContext().setAuthentication(auth);
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
            session.setAttribute(USER_ROLES_ATTRIBUTE, null);
            session.close();
        }
        SecurityContextHolder.clearContext();
    }
}
