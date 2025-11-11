/**
 * Authentication Type Definitions
 *
 * PURPOSE: TypeScript interfaces for auth-related data
 *
 * TYPES TO DEFINE:
 * User
 *   - id: UUID
 *   - email: string
 *   - username: string
 *   - firstName: string
 *   - lastName: string
 *   - age: number (calculated)
 *   - gender: 'MALE' | 'FEMALE' | 'OTHER'
 *   - bio: string
 *   - profilePictureUrl: string
 *   - status: 'ACTIVE' | 'SUSPENDED' | 'DELETED'
 *   - createdAt: ISO string
 *
 * AuthResponse
 *   - userId: UUID
 *   - token: JWT string
 *   - refreshToken: JWT string
 *   - expiresIn: number (seconds)
 *
 * LoginRequest
 *   - email: string
 *   - password: string
 *
 * RegisterRequest
 *   - email: string
 *   - username: string
 *   - password: string
 *   - firstName: string
 *   - lastName: string
 *   - dateOfBirth: ISO date string
 *   - gender: 'MALE' | 'FEMALE'
 *
 * AuthState (Zustand)
 *   - user: User | null
 *   - token: string | null
 *   - isLoading: boolean
 *   - error: string | null
 *   - isAuthenticated: boolean
 *   - login: (email, password) => Promise<void>
 *   - register: (data) => Promise<void>
 *   - logout: () => void
 *   - setUser: (user) => void
 *   - setToken: (token) => void
 */

// TODO: Define all auth-related types
