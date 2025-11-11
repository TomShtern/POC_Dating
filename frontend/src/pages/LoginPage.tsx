/**
 * Login Page
 *
 * PURPOSE: User login page
 *
 * LAYOUT:
 * - Centered card with login form
 * - Email input
 * - Password input
 * - "Login" button
 * - "Don't have account? Register" link
 * - Error messages
 * - Loading spinner during submission
 *
 * FLOW:
 * 1. User enters email and password
 * 2. Click Login button
 * 3. Validate inputs (not empty)
 * 4. Call authStore.login(email, password)
 * 5. Show loading spinner
 * 6. On success:
 *    - Save token to Zustand
 *    - Navigate to /swipe page
 * 7. On error:
 *    - Show error message
 *    - Keep form visible for retry
 *
 * VALIDATION:
 * - Email: Required, valid email format
 * - Password: Required, at least 6 chars
 *
 * TODO: Create LoginPage component
 * TODO: Implement form state and validation
 * TODO: Connect to authStore.login()
 * TODO: Add navigation to register page
 */
export default function LoginPage() {
  // TODO: Implement LoginPage
  return <div>Login Page</div>
}
