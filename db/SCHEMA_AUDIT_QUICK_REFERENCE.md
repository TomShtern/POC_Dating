# Schema Audit Quick Reference

## All Issues at a Glance

| # | Issue | Table | Severity | Category | SQL Fix |
|---|-------|-------|----------|----------|---------|
| 1 | Missing sender validation in messages | messages | CRITICAL | Security | Add trigger to validate sender is in match |
| 2 | Password hash insufficient validation | users | CRITICAL | Security | Add CHECK for bcrypt format |
| 3 | Match ending without proper state | matches | CRITICAL | Data Integrity | Add CHECK: if status != ACTIVE then ended_at NOT NULL |
| 4 | Location data type precision loss | users | HIGH | Data Quality | ALTER location_lat DECIMAL(11,8), location_lng DECIMAL(12,8) |
| 5 | Missing verification code uniqueness | verification_codes | HIGH | Security/Logic | Add trigger to auto-invalidate previous codes |
| 6 | Missing NOT NULL in match scores | match_scores | HIGH | Data Integrity | ALTER score SET NOT NULL, factors SET NOT NULL |
| 7 | Message cascade deletes sender | messages | HIGH | Data Integrity | ALTER sender_id ON DELETE SET NULL |
| 8 | Bio field lacks size limit | users | HIGH | Security/UX | ADD CONSTRAINT bio_max_length CHECK (length <= 5000) |
| 9 | Interests array lacks size limit | user_preferences | HIGH | Performance | ADD CONSTRAINT interests_max_count CHECK (array_length <= 50) |
| 10 | Refresh tokens inconsistent revoke state | refresh_tokens | MEDIUM | Logic | ADD CONSTRAINT revoke_state_consistency |
| 11 | Verification code attempts not limited | verification_codes | MEDIUM | Security | ADD CONSTRAINT attempts_limit CHECK (>= 0 AND <= 5) |
| 12 | Interaction history missing action validation | interaction_history | MEDIUM | Data Quality | ADD CONSTRAINT valid_action |
| 13 | Messages missing updated_at | messages | MEDIUM | Auditability | ADD COLUMN updated_at + trigger |
| 14 | Swipes missing updated_at | swipes | MEDIUM | Auditability | ADD COLUMN updated_at + trigger |
| 15 | Notification state incomplete | notifications | MEDIUM | Logic | ADD CONSTRAINT for timestamp consistency |
| 16 | User blocks missing expiration | user_blocks | MEDIUM | Feature | ADD COLUMN expires_at, unblocked_at |
| 17 | Verification code format not validated | verification_codes | LOW | Security | ADD CONSTRAINT code_numeric_format |
| 18 | Timestamp timezone handling | All tables | LOW | Best Practice | ALTER to TIMESTAMPTZ (optional) |
| 19 | Audit logs missing type validation | audit_logs | LOW | Data Quality | ADD CONSTRAINT valid_entity_type, valid_action |
| 20 | No default for gender field | users | LOW | Data Quality | Make NOT NULL or document nullable |
| 21 | Missing index documentation | All tables | LOW | Documentation | Add COMMENT ON TABLE |

---

## Severity Distribution

- **CRITICAL (3):** Issues that block production + security risks
- **HIGH (6):** Production quality issues + data integrity
- **MEDIUM (8):** Quality improvements + consistency
- **LOW (5):** Polish + best practices

---

## Issues Requiring Database Migrations

### Critical (Implement First - 2-3 hours)
1. Issue #1: Message sender validation trigger
2. Issue #2: Password hash CHECK constraint
3. Issue #3: Match state consistency CHECK constraint

### High (Implement Before Production - 3-4 hours)
4. Issue #4: Location decimal precision (ALTER TYPE)
5. Issue #5: Verification code uniqueness trigger
6. Issue #6: Match scores NOT NULL
7. Issue #7: Message cascade to SET NULL
8. Issue #8: Bio max length CHECK
9. Issue #9: Interests array size CHECK

### Medium (Quality Phase - 4-5 hours)
10-16: Various constraints, triggers, and columns

### Low (Polish - 2-3 hours)
17-21: Format validation, timezone, documentation

---

## Implementation Commands

### Phase 1: CRITICAL ISSUES

```bash
# Create migration file
touch db/migrations/V4__critical_schema_fixes.sql

# Add these SQL statements to the migration
```

Critical SQL fixes:
1. Message sender validation trigger
2. Password hash format check
3. Match end state consistency

### Phase 2: HIGH PRIORITY ISSUES

```sql
-- Location precision
ALTER TABLE users
ALTER COLUMN location_lat TYPE DECIMAL(11, 8),
ALTER COLUMN location_lng TYPE DECIMAL(12, 8);

-- Verification code uniqueness trigger
CREATE OR REPLACE FUNCTION ensure_single_active_code() ...

-- Match scores NOT NULL
UPDATE match_scores SET score = 50 WHERE score IS NULL;
UPDATE match_scores SET factors = '{}' WHERE factors IS NULL;
ALTER TABLE match_scores
ALTER COLUMN score SET NOT NULL,
ALTER COLUMN factors SET NOT NULL DEFAULT '{}';

-- Message CASCADE → SET NULL
ALTER TABLE messages
DROP CONSTRAINT messages_sender_id_fkey,
ADD CONSTRAINT messages_sender_id_fkey 
    FOREIGN KEY (sender_id) REFERENCES users(id) ON DELETE SET NULL;

-- Bio max length
UPDATE users SET bio = substring(bio FROM 1 FOR 5000) WHERE length(bio) > 5000;
ALTER TABLE users
ADD CONSTRAINT bio_max_length CHECK (length(bio) <= 5000);

-- Interests array limit
UPDATE user_preferences 
SET interests = interests[1:50] 
WHERE array_length(interests, 1) > 50;
ALTER TABLE user_preferences
ADD CONSTRAINT interests_max_count CHECK (array_length(interests, 1) <= 50);
```

---

## Files Affected

- `db/init/01-schema.sql` - Add constraints/triggers
- `db/init/04-functions.sql` - Add/update functions
- `db/migrations/V4__critical_schema_fixes.sql` - New migration file (create)
- `db/README.md` - Document breaking changes

---

## Testing Checklist

After implementing each phase:

- [ ] Run fresh database initialization
- [ ] Test constraint violations are rejected
- [ ] Verify triggers execute correctly
- [ ] Check data integrity with sample queries
- [ ] Run Flyway migrations successfully
- [ ] Test application layer still works
- [ ] Verify no orphan records
- [ ] Check query performance unchanged

---

## Status Tracking

| Phase | Issues | Status | Time Est. |
|-------|--------|--------|-----------|
| Critical | 1, 2, 3 | Not Started | 2-3h |
| High | 4, 5, 6, 7, 8, 9 | Not Started | 3-4h |
| Medium | 10-16 | Not Started | 4-5h |
| Low | 17-21 | Not Started | 2-3h |
| **TOTAL** | **21** | **Not Started** | **12-16h** |

---

## Risk Assessment

### If NOT Fixed
- **CRITICAL issues:** Can lead to security breaches, data corruption
- **HIGH issues:** Production failures, data loss
- **MEDIUM issues:** Inconsistent state, hard to debug
- **LOW issues:** Technical debt accumulation

### If Fixed
- Production-ready database
- Strong data integrity guarantees
- Better debugging and auditability
- Future-proof architecture

---

## See Also

- Full detailed report: `db/SCHEMA_AUDIT_DETAILED.md`
- Existing audit: `db/DATABASE_AUDIT.md`
- Schema file: `db/init/01-schema.sql`
- Index documentation: `db/init/02-indexes.sql`

