/**
 * API Service Configuration
 *
 * PURPOSE: Axios instance with JWT interceptors and base configuration
 *
 * CONFIGURATION TO IMPLEMENT:
 * - Base URL from environment: VITE_API_URL
 * - Default headers: Content-Type: application/json
 * - Timeout: 30 seconds
 *
 * REQUEST INTERCEPTOR:
 * - Add JWT token from localStorage to Authorization header
 * - Format: "Bearer {token}"
 * - Skip auth header for login/register endpoints
 *
 * RESPONSE INTERCEPTOR:
 * - Handle 401 (Unauthorized)
 *   → Clear token and user from Zustand
 *   → Redirect to login page
 * - Handle 403 (Forbidden)
 *   → Show error notification
 * - Handle 429 (Too Many Requests)
 *   → Show rate limit error
 * - Handle 5xx errors
 *   → Show service unavailable message
 *
 * ERROR HANDLING:
 * - Parse error response for message
 * - Show user-friendly error notifications
 * - Log errors to console in dev mode
 *
 * TODO: Create axios instance
 * TODO: Add request interceptor for JWT
 * TODO: Add response interceptor for error handling
 * TODO: Export api instance and typed methods
 */

// TODO: Implement API service
