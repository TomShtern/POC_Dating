# Admin Dashboard Implementation Prompt

## Context
You are implementing a complete Admin Dashboard for a POC Dating Application. This is a Vaadin 24.3 web interface (100% Java, NO React/Angular/Vue) that allows administrators to manage users, monitor system health, view analytics, and handle moderation tasks.

**You have full internet access** - research any uncertainties, look up Vaadin components, Spring Security patterns, or charting libraries as needed.

---

## CRITICAL: Code Quality Standards

### CLEAN, MAINTAINABLE, MODULAR CODE
Every piece of code you write MUST be:
- **Clean**: Self-documenting, clear naming, no magic numbers
- **Maintainable**: Easy to modify without breaking other parts
- **Modular**: Single responsibility, reusable components, clear interfaces

### ABSTRACT When Appropriate
- Extract common patterns into base classes (e.g., `AbstractAdminView`, `AbstractCrudView`)
- Create interfaces for services that might have multiple implementations
- Use composition over inheritance where it makes sense
- Build reusable Vaadin components for common UI patterns

### LEVERAGE Modern Java 21 Features
```java
// Records for DTOs
public record UserStatsDTO(long totalUsers, long activeToday, long newThisWeek) {}

// Pattern matching
if (action instanceof BanAction ban) {
    processBan(ban.userId(), ban.reason(), ban.duration());
}

// Sealed interfaces for admin actions
public sealed interface AdminAction permits BanAction, WarnAction, DeleteAction {}
```

### LEVERAGE Modern Vaadin 24 Features
- Use Vaadin's built-in Grid with lazy loading
- Leverage FormLayout with Binder for type-safe forms
- Use Notification.show() for user feedback
- Implement Charts add-on for analytics visualization

---

## Implementation Scope

### 1. Core Admin Views (Vaadin)

#### Dashboard Home (`/admin`)
- Real-time statistics cards (total users, active today, matches today, messages today)
- Activity charts (registrations over time, daily active users)
- System health indicators (service status, database connections, cache hit rate)
- Recent alerts/issues requiring attention

#### User Management (`/admin/users`)
- Searchable, sortable, paginated Grid of all users
- Filters: status (active/suspended/banned), registration date, verification status
- Inline actions: view profile, edit, warn, suspend, ban, delete
- User detail dialog with full profile, activity history, reports against them
- Bulk actions: select multiple users for batch operations

#### Analytics (`/admin/analytics`)
- User growth charts (daily/weekly/monthly)
- Engagement metrics (swipes, matches, messages per user)
- Retention cohort analysis
- Geographic distribution (if location data available)
- Export to CSV functionality

#### System Monitoring (`/admin/system`)
- Service health status (all microservices)
- Database metrics (connections, query times)
- Cache statistics (Redis hit/miss rates)
- Message queue depth (RabbitMQ)
- Recent error logs

#### Configuration (`/admin/config`)
- Application settings (match algorithm weights, rate limits)
- Feature flags (enable/disable features)
- Notification templates
- Audit log of configuration changes

### 2. Backend Admin Services

#### AdminUserService
- `searchUsers(AdminUserSearchCriteria criteria, Pageable pageable)`
- `getUserDetails(UUID userId)` - full profile + activity + reports
- `updateUserStatus(UUID userId, UserStatus status, String reason)`
- `bulkUpdateStatus(List<UUID> userIds, UserStatus status, String reason)`
- `getUserActivityHistory(UUID userId)`

#### AdminAnalyticsService
- `getDashboardStats()` - aggregated metrics
- `getUserGrowthData(DateRange range, Granularity granularity)`
- `getEngagementMetrics(DateRange range)`
- `getRetentionCohorts(int weeks)`
- `exportAnalytics(AnalyticsExportRequest request)`

#### AdminSystemService
- `getServiceHealth()` - all microservice statuses
- `getDatabaseMetrics()`
- `getCacheMetrics()`
- `getRecentErrors(int limit)`

#### AdminConfigService
- `getConfiguration(String category)`
- `updateConfiguration(String key, String value, UUID adminId)`
- `getConfigurationAuditLog()`

### 3. Security & Access Control

#### Role-Based Access
- ROLE_ADMIN: Full access
- ROLE_MODERATOR: User management, moderation only
- ROLE_ANALYST: Analytics view only

#### Audit Logging
- Log ALL admin actions with: admin ID, action, target, timestamp, IP address
- Immutable audit trail for compliance

#### Route Guards
```java
@Route(value = "admin", layout = AdminLayout.class)
@RolesAllowed({"ADMIN", "MODERATOR"})
public class AdminDashboardView extends VerticalLayout { }
```

### 4. Database Requirements

#### New Tables
```sql
-- Admin audit log
CREATE TABLE admin_audit_log (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    admin_id UUID NOT NULL REFERENCES users(id),
    action VARCHAR(100) NOT NULL,
    target_type VARCHAR(50),
    target_id UUID,
    details JSONB,
    ip_address INET,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Application configuration
CREATE TABLE app_configuration (
    key VARCHAR(255) PRIMARY KEY,
    value TEXT NOT NULL,
    category VARCHAR(100),
    description TEXT,
    updated_by UUID REFERENCES users(id),
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for common queries
CREATE INDEX idx_audit_admin_time ON admin_audit_log(admin_id, created_at DESC);
CREATE INDEX idx_audit_target ON admin_audit_log(target_type, target_id);
CREATE INDEX idx_config_category ON app_configuration(category);
```

---

## Technical Requirements

### Parallel Execution
- **Call tools in parallel** when operations are independent (e.g., create multiple views simultaneously)
- **Launch multiple agents in parallel** for independent components (e.g., one agent for views, one for services)
- **Use agents to maintain context** across the task - delegate complex sub-tasks to specialized agents

### Dependencies to Add
```xml
<!-- pom.xml additions -->
<dependency>
    <groupId>com.vaadin</groupId>
    <artifactId>vaadin-charts-flow</artifactId>
</dependency>
```

### Performance Requirements
- Grid pagination: 50 items per page, lazy loading
- Analytics queries: < 2 seconds
- Dashboard load: < 1 second
- Use caching for expensive aggregations

---

## Implementation Loop

### PHASE 1: Core Implementation
Implement ALL components in this order:
1. Database schema (tables, indexes)
2. Backend services (AdminUserService, AdminAnalyticsService, etc.)
3. Vaadin views (Dashboard, UserManagement, Analytics, System, Config)
4. Security configuration (roles, route guards)
5. Audit logging

**For each component:**
- Read existing code patterns first
- Implement with clean, modular code
- Add appropriate tests
- Verify it compiles and integrates

### PHASE 2: Critical Review #1
After completing all components, perform a **critical review**:

1. **Code Quality Check**
   - Are all classes following single responsibility?
   - Is there code duplication that should be extracted?
   - Are naming conventions consistent?
   - Are there magic numbers or hardcoded values?

2. **Security Review**
   - Are all routes protected with @RolesAllowed?
   - Is input validated and sanitized?
   - Are SQL queries parameterized?
   - Is audit logging comprehensive?

3. **Performance Review**
   - Are Grid queries using pagination?
   - Are expensive operations cached?
   - Are there N+1 query problems?
   - Are indexes appropriate for queries?

4. **Completeness Review**
   - Does every button/action work?
   - Are error states handled?
   - Are loading states shown?
   - Is the UX intuitive?

5. **Testing Review**
   - Do unit tests cover critical paths?
   - Are edge cases tested?
   - Do integration tests verify security?

**LIST ALL FINDINGS** - every issue, no matter how small.

### PHASE 3: Fix All Findings
Implement fixes for EVERY issue found in the critical review:
- Fix each issue completely
- Verify the fix doesn't break other functionality
- Add tests for the fix if applicable

### PHASE 4: Critical Review #2
Perform another critical review using the same checklist.
- If issues found → List them, fix them, return to PHASE 4
- If no issues found → Proceed to PHASE 5

### PHASE 5: Independent Super-Critical Review
Perform a completely fresh, independent review as if seeing the code for the first time:

1. **Architecture Review**
   - Is the overall structure maintainable?
   - Are dependencies properly managed?
   - Is the code testable?

2. **Edge Case Analysis**
   - What happens with 0 users?
   - What happens with 1 million audit logs?
   - What if a service is down?
   - What if the admin bans themselves?

3. **Production Readiness**
   - Are all error messages user-friendly?
   - Is logging appropriate (not too verbose, not too quiet)?
   - Are there any security vulnerabilities?
   - Is the code ready for code review?

**If ANY issues found** → Fix them and return to PHASE 4
**If NO issues found** → Proceed to completion

---

## Completion

When all reviews pass with no issues, provide:

### Implementation Brief
A concise summary including:
- Components implemented (views, services, tables)
- Key design decisions made
- Lines of code / number of files
- Test coverage achieved
- Any assumptions or trade-offs made
- Recommendations for future enhancements

### Files Created/Modified
List all files with brief descriptions

### How to Test
Step-by-step instructions to verify the implementation

---

## Example Code Patterns

### Reusable Base View
```java
public abstract class AbstractAdminView extends VerticalLayout {
    protected final AdminAuditService auditService;

    protected AbstractAdminView(AdminAuditService auditService) {
        this.auditService = auditService;
        addClassName("admin-view");
        setSizeFull();
    }

    protected void logAction(String action, String targetType, UUID targetId, String details) {
        auditService.log(action, targetType, targetId, details);
    }

    protected void showSuccess(String message) {
        Notification.show(message, 3000, Notification.Position.TOP_END)
            .addThemeVariants(NotificationVariant.LUMO_SUCCESS);
    }

    protected void showError(String message) {
        Notification.show(message, 5000, Notification.Position.TOP_END)
            .addThemeVariants(NotificationVariant.LUMO_ERROR);
    }
}
```

### Statistics Card Component
```java
public class StatCard extends Div {
    public StatCard(String title, String value, String trend, VaadinIcon icon) {
        addClassName("stat-card");

        Icon i = icon.create();
        i.addClassName("stat-icon");

        Span titleSpan = new Span(title);
        titleSpan.addClassName("stat-title");

        Span valueSpan = new Span(value);
        valueSpan.addClassName("stat-value");

        Span trendSpan = new Span(trend);
        trendSpan.addClassName("stat-trend");

        add(i, titleSpan, valueSpan, trendSpan);
    }
}
```

### Admin Service with Audit Logging
```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {
    private final UserRepository userRepository;
    private final AdminAuditService auditService;

    @Transactional
    public void updateUserStatus(UUID userId, UserStatus newStatus, String reason, UUID adminId) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new UserNotFoundException(userId));

        UserStatus oldStatus = user.getStatus();
        user.setStatus(newStatus);
        user.setStatusReason(reason);
        user.setStatusUpdatedAt(Instant.now());
        user.setStatusUpdatedBy(adminId);

        userRepository.save(user);

        auditService.log(
            "USER_STATUS_CHANGE",
            "USER",
            userId,
            Map.of(
                "oldStatus", oldStatus,
                "newStatus", newStatus,
                "reason", reason
            ),
            adminId
        );

        log.info("Admin {} changed user {} status from {} to {}",
            adminId, userId, oldStatus, newStatus);
    }
}
```

---

## Success Criteria

The implementation is complete when:
- [ ] All 5 admin views are functional and navigable
- [ ] All backend services are implemented with proper error handling
- [ ] Role-based security restricts access appropriately
- [ ] All admin actions are audit logged
- [ ] Grids use lazy loading and pagination
- [ ] Analytics charts display correctly
- [ ] Unit tests achieve 70%+ coverage
- [ ] Two critical reviews pass with no issues
- [ ] Super-critical review passes with no issues
- [ ] Code is clean, maintainable, and modular

**BEGIN IMPLEMENTATION**
