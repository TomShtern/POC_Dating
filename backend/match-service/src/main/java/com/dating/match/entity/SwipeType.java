package com.dating.match.entity;

/**
 * Enum representing the type of swipe action a user can perform
 *
 * PURPOSE: Type-safe swipe actions for dating app interactions
 *
 * VALUES:
 * - LIKE: User is interested (swipe right)
 * - PASS: User is not interested (swipe left)
 * - SUPER_LIKE: User is very interested (premium feature)
 *
 * WHY ENUM vs String:
 * - Type safety (compile-time validation)
 * - Prevents invalid values
 * - Clear documentation of allowed actions
 * - Better IDE support
 *
 * ALTERNATIVES:
 * - String constants: Error-prone, no type safety
 * - Database table: Overkill for static values
 * - Boolean (like/pass only): Not extensible
 *
 * RATIONALE:
 * - Enums are perfect for fixed set of values
 * - JPA supports enum mapping natively
 * - Easy to extend with new actions if needed
 */
public enum SwipeType {
    LIKE,
    PASS,
    SUPER_LIKE
}
