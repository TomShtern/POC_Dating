# Content Moderation Implementation Prompt

## Context
You are implementing a complete Content Moderation system for a POC Dating Application. This includes a Vaadin 24.3 moderation interface (100% Java), backend services for processing reports and flagged content, automated filters, and user appeal handling.

**You have full internet access** - research any uncertainties, look up content filtering libraries, image moderation APIs, or moderation workflow patterns as needed.

---

## CRITICAL: Code Quality Standards

### CLEAN, MAINTAINABLE, MODULAR CODE
Every piece of code you write MUST be:
- **Clean**: Self-documenting, clear naming, no magic numbers
- **Maintainable**: Easy to modify without breaking other parts
- **Modular**: Single responsibility, reusable components, clear interfaces

### ABSTRACT When Appropriate
- Create base classes for moderation items (`AbstractModerationItem`)
- Use Strategy pattern for different content type handlers
- Build interfaces for filter implementations (can swap automated filters)
- Extract common moderation workflows into reusable services

### LEVERAGE Modern Java 21 Features
```java
// Records for moderation items
public record ModerationQueueItem(
    UUID id,
    ContentType type,
    UUID contentId,
    UUID reportedUserId,
    String reason,
    ModerationPriority priority,
    Instant createdAt
) {}

// Sealed interfaces for moderation decisions
public sealed interface ModerationDecision
    permits Approve, Reject, Escalate, RequestMoreInfo {}

public record Reject(String reason, PunishmentAction action) implements ModerationDecision {}

// Pattern matching for content handling
return switch (content) {
    case PhotoContent photo -> moderatePhoto(photo);
    case MessageContent message -> moderateMessage(message);
    case ProfileContent profile -> moderateProfile(profile);
};
```

### LEVERAGE Modern Vaadin 24 Features
- Grid with ComponentRenderer for action buttons
- Dialog for detailed content review
- Badge components for status indicators
- ContextMenu for quick actions

---

## Implementation Scope

### 1. Moderation Views (Vaadin)

#### Moderation Queue (`/admin/moderation`)
- Real-time queue of items requiring review
- Priority sorting (HIGH → MEDIUM → LOW)
- Filters: content type (photo/message/profile), priority, date range
- Queue statistics (pending, reviewed today, average review time)
- Quick action buttons on each item

#### Photo Moderation (`/admin/moderation/photos`)
- Grid showing reported/flagged photos
- Large image preview dialog
- Photo context: who uploaded, when, which profile section
- Action buttons: Approve, Reject (with reason), Request re-upload
- Automated flag indicators (nudity score, violence score)
- Bulk moderation for similar content

#### Message Moderation (`/admin/moderation/messages`)
- Reported messages with conversation context
- Show messages before/after the reported message
- User history: previous warnings, ban count
- Actions: Dismiss report, Warn sender, Mute sender, Ban sender
- Quick filters: harassment, spam, scam, inappropriate

#### Profile Moderation (`/admin/moderation/profiles`)
- Reported profiles with full profile view
- Specific field flagged (bio, interests, prompts)
- Actions: Approve, Request edit, Suspend pending changes
- History of profile edits

#### User Reports (`/admin/reports`)
- All user-submitted reports
- Report details: reporter, reported user, category, description
- Report resolution workflow
- Reporter feedback (was this helpful?)
- Report analytics: most reported users, common categories

#### Appeals (`/admin/appeals`)
- Users appealing moderation decisions
- Original decision context
- User's appeal message
- Actions: Uphold decision, Overturn, Partial overturn
- Appeal history per user

#### Moderation Analytics (`/admin/moderation/analytics`)
- Volume trends (reports over time)
- Resolution time metrics
- Moderator performance (items reviewed, accuracy)
- False positive/negative rates
- Content category breakdown

### 2. Backend Moderation Services

#### ModerationQueueService
```java
public interface ModerationQueueService {
    Page<ModerationQueueItem> getQueue(ModerationQueueFilter filter, Pageable pageable);
    ModerationQueueItem getItem(UUID itemId);
    void processDecision(UUID itemId, ModerationDecision decision, UUID moderatorId);
    QueueStatistics getQueueStatistics();
    void escalate(UUID itemId, String reason, UUID moderatorId);
}
```

#### ContentFilterService (Automated Filters)
```java
public interface ContentFilterService {
    FilterResult analyzePhoto(byte[] imageData);
    FilterResult analyzeText(String text);
    boolean shouldAutoReject(FilterResult result);
    boolean shouldAutoApprove(FilterResult result);
    boolean requiresHumanReview(FilterResult result);
}

// Pluggable filter implementations
public interface TextFilter {
    TextFilterResult analyze(String text);
}

@Component
public class ProfanityFilter implements TextFilter { }

@Component
public class SpamDetectionFilter implements TextFilter { }

@Component
public class ScamPatternFilter implements TextFilter { }
```

#### ReportService
```java
public interface ReportService {
    Report createReport(CreateReportRequest request);
    Page<Report> getReports(ReportFilter filter, Pageable pageable);
    void resolveReport(UUID reportId, ReportResolution resolution, UUID moderatorId);
    ReportStatistics getStatistics(DateRange range);
    List<Report> getReportsAgainstUser(UUID userId);
    List<Report> getReportsByUser(UUID reporterId);
}
```

#### AppealService
```java
public interface AppealService {
    Appeal createAppeal(UUID moderationDecisionId, String appealMessage, UUID userId);
    Page<Appeal> getAppeals(AppealFilter filter, Pageable pageable);
    void resolveAppeal(UUID appealId, AppealDecision decision, String reason, UUID moderatorId);
    boolean canUserAppeal(UUID userId, UUID decisionId);
}
```

#### PunishmentService
```java
public interface PunishmentService {
    void warn(UUID userId, String reason, UUID moderatorId);
    void mute(UUID userId, Duration duration, String reason, UUID moderatorId);
    void suspend(UUID userId, Duration duration, String reason, UUID moderatorId);
    void ban(UUID userId, String reason, UUID moderatorId);
    void unban(UUID userId, String reason, UUID moderatorId);
    PunishmentHistory getHistory(UUID userId);
}
```

### 3. Automated Moderation Pipeline

#### Photo Upload Pipeline
```
User uploads photo
    → ContentFilterService.analyzePhoto()
    → If autoReject: reject immediately, notify user
    → If autoApprove: approve immediately
    → If humanReview: add to moderation queue with priority
```

#### Message Pipeline
```
User sends message
    → ContentFilterService.analyzeText()
    → If flagged: hold message, add to queue
    → If clean: deliver immediately
    → Track false positives for filter improvement
```

#### Report Pipeline
```
User submits report
    → Validate report (not duplicate, not self-report)
    → Calculate priority based on:
        - Content type
        - Reported user history
        - Reporter credibility
    → Add to moderation queue
    → Notify moderators if HIGH priority
```

### 4. Database Schema

```sql
-- Content moderation queue
CREATE TABLE moderation_queue (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    content_type VARCHAR(50) NOT NULL,  -- PHOTO, MESSAGE, PROFILE
    content_id UUID NOT NULL,
    reported_user_id UUID NOT NULL REFERENCES users(id),
    reporter_id UUID REFERENCES users(id),  -- NULL for automated flags
    reason VARCHAR(500),
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    auto_filter_scores JSONB,  -- Scores from automated filters
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    assigned_to UUID REFERENCES users(id),
    assigned_at TIMESTAMP
);

-- Moderation decisions
CREATE TABLE moderation_decisions (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    queue_item_id UUID NOT NULL REFERENCES moderation_queue(id),
    decision VARCHAR(50) NOT NULL,  -- APPROVE, REJECT, ESCALATE
    reason TEXT,
    punishment_action VARCHAR(50),  -- WARN, MUTE, SUSPEND, BAN
    moderator_id UUID NOT NULL REFERENCES users(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- User reports
CREATE TABLE user_reports (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    reporter_id UUID NOT NULL REFERENCES users(id),
    reported_user_id UUID NOT NULL REFERENCES users(id),
    content_type VARCHAR(50),
    content_id UUID,
    category VARCHAR(100) NOT NULL,
    description TEXT,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolution VARCHAR(50),
    resolution_notes TEXT,
    resolved_by UUID REFERENCES users(id),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- Prevent duplicate reports
    UNIQUE(reporter_id, reported_user_id, content_type, content_id)
);

-- Appeals
CREATE TABLE moderation_appeals (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    decision_id UUID NOT NULL REFERENCES moderation_decisions(id),
    user_id UUID NOT NULL REFERENCES users(id),
    appeal_message TEXT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
    resolution VARCHAR(50),
    resolution_reason TEXT,
    resolved_by UUID REFERENCES users(id),
    resolved_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,

    -- One appeal per decision
    UNIQUE(decision_id)
);

-- User punishments
CREATE TABLE user_punishments (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL REFERENCES users(id),
    type VARCHAR(50) NOT NULL,  -- WARN, MUTE, SUSPEND, BAN
    reason TEXT NOT NULL,
    duration_minutes INTEGER,  -- NULL for permanent/warnings
    issued_by UUID NOT NULL REFERENCES users(id),
    expires_at TIMESTAMP,
    revoked_at TIMESTAMP,
    revoked_by UUID REFERENCES users(id),
    revoke_reason TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Indexes for performance
CREATE INDEX idx_mod_queue_status_priority ON moderation_queue(status, priority, created_at);
CREATE INDEX idx_mod_queue_content ON moderation_queue(content_type, content_id);
CREATE INDEX idx_mod_queue_user ON moderation_queue(reported_user_id);
CREATE INDEX idx_reports_status ON user_reports(status, created_at);
CREATE INDEX idx_reports_reported ON user_reports(reported_user_id);
CREATE INDEX idx_reports_reporter ON user_reports(reporter_id);
CREATE INDEX idx_appeals_status ON moderation_appeals(status, created_at);
CREATE INDEX idx_punishments_user ON user_punishments(user_id, created_at DESC);
CREATE INDEX idx_punishments_active ON user_punishments(user_id)
    WHERE expires_at IS NULL OR expires_at > CURRENT_TIMESTAMP;
```

---

## Technical Requirements

### Parallel Execution
- **Call tools in parallel** when operations are independent (e.g., create services and DTOs simultaneously)
- **Launch multiple agents in parallel** for independent components (e.g., one for automated filters, one for moderation views)
- **Use agents to maintain context** across the task - delegate complex sub-tasks to specialized agents

### Configuration
```yaml
# application.yml
moderation:
  auto-reject-threshold: 0.95
  auto-approve-threshold: 0.1
  human-review-threshold: 0.3

  queue:
    high-priority-categories:
      - HARASSMENT
      - THREATS
      - CHILD_SAFETY
    assignment-timeout-minutes: 30

  punishments:
    warn-message-template: "Your content violated our community guidelines..."
    mute-durations-minutes: [60, 1440, 10080]  # 1h, 1d, 1w
    suspend-durations-days: [1, 7, 30]

  filters:
    profanity:
      enabled: true
      custom-word-list: classpath:profanity-list.txt
    spam:
      enabled: true
      max-links: 2
      max-caps-ratio: 0.5
```

### Performance Requirements
- Queue fetch: < 200ms
- Decision processing: < 500ms
- Filter analysis: < 1 second
- Use caching for user punishment history

---

## Implementation Loop

### PHASE 1: Core Implementation
Implement ALL components in this order:
1. Database schema (all tables, indexes)
2. Domain models and DTOs
3. Core services (ModerationQueueService, ReportService, PunishmentService)
4. Automated filters (ContentFilterService, TextFilters)
5. Vaadin views (Queue, Photos, Messages, Profiles, Reports, Appeals)
6. Pipeline integration (photo upload, message sending hooks)

**For each component:**
- Read existing code patterns first
- Implement with clean, modular code
- Use interfaces and abstractions appropriately
- Add appropriate tests
- Verify it compiles and integrates

### PHASE 2: Critical Review #1
After completing all components, perform a **critical review**:

1. **Code Quality Check**
   - Are all classes following single responsibility?
   - Is there code duplication that should be extracted?
   - Are naming conventions consistent?
   - Are interfaces used appropriately for pluggable components?

2. **Security Review**
   - Can moderators only access items they're authorized for?
   - Is reporter identity protected when needed?
   - Are punishment actions logged?
   - Can users manipulate the appeal system?

3. **Fairness & Consistency Review**
   - Are punishment guidelines consistent?
   - Are automated filters unbiased?
   - Is there a path for false positive correction?
   - Are appeals handled fairly?

4. **Performance Review**
   - Are queue queries optimized?
   - Is filter processing efficient?
   - Are there N+1 query problems?
   - Is caching used appropriately?

5. **Completeness Review**
   - Does every workflow complete end-to-end?
   - Are edge cases handled (no items in queue, appeal after ban expires)?
   - Are notifications sent appropriately?
   - Is the UX intuitive for moderators?

6. **Testing Review**
   - Do unit tests cover all decision paths?
   - Are filter accuracy tests included?
   - Do integration tests verify workflows?

**LIST ALL FINDINGS** - every issue, no matter how small.

### PHASE 3: Fix All Findings
Implement fixes for EVERY issue found in the critical review:
- Fix each issue completely
- Verify the fix doesn't break other functionality
- Add tests for the fix if applicable
- Update documentation if needed

### PHASE 4: Critical Review #2
Perform another critical review using the same checklist.
- If issues found → List them, fix them, return to PHASE 4
- If no issues found → Proceed to PHASE 5

### PHASE 5: Independent Super-Critical Review
Perform a completely fresh, independent review as if seeing the code for the first time:

1. **Workflow Analysis**
   - Trace a report from submission to resolution
   - Trace a photo from upload to approval/rejection
   - Trace an appeal from creation to decision
   - Are there any dead ends or loops?

2. **Edge Case Scenarios**
   - What if a user reports themselves?
   - What if a banned user's content is in queue?
   - What if a moderator reviews their own content?
   - What if appeal is submitted after ban expires?
   - What if filter service is unavailable?

3. **Abuse Scenarios**
   - Can users flood the report system?
   - Can users game the appeal system?
   - Can moderators abuse their power?
   - Are there rate limits?

4. **Scale Considerations**
   - What happens with 10,000 items in queue?
   - Is the database schema scalable?
   - Are background jobs needed for any operations?

5. **Production Readiness**
   - Are all error messages appropriate?
   - Is logging comprehensive but not excessive?
   - Are metrics exposed for monitoring?
   - Is the code ready for code review?

**If ANY issues found** → Fix them and return to PHASE 4
**If NO issues found** → Proceed to completion

---

## Completion

When all reviews pass with no issues, provide:

### Implementation Brief
A concise summary including:
- Components implemented (views, services, filters, tables)
- Key design decisions (filter strategy, punishment escalation)
- Moderation workflow diagrams (text-based)
- Lines of code / number of files
- Test coverage achieved
- Assumptions and trade-offs
- Recommendations for production (e.g., ML-based filters, moderator training)

### Files Created/Modified
List all files with brief descriptions

### How to Test
Step-by-step instructions to verify:
- Report submission and resolution
- Photo moderation workflow
- Message filtering
- Appeal process
- Punishment application and expiry

---

## Example Code Patterns

### Pluggable Filter Architecture
```java
public interface TextFilter {
    String getName();
    int getPriority();  // Lower = runs first
    TextFilterResult analyze(String text);
}

@Service
@RequiredArgsConstructor
public class ContentFilterService {
    private final List<TextFilter> textFilters;  // Auto-injected by Spring

    public FilterResult analyzeText(String text) {
        Map<String, TextFilterResult> results = new HashMap<>();
        double maxScore = 0;

        // Filters are sorted by priority
        for (TextFilter filter : textFilters) {
            TextFilterResult result = filter.analyze(text);
            results.put(filter.getName(), result);
            maxScore = Math.max(maxScore, result.score());

            // Short-circuit if definitely bad
            if (result.score() >= autoRejectThreshold) {
                break;
            }
        }

        return new FilterResult(maxScore, results);
    }
}
```

### Moderation Decision Handler
```java
@Service
@RequiredArgsConstructor
@Transactional
public class ModerationQueueService {
    private final ModerationQueueRepository queueRepository;
    private final PunishmentService punishmentService;
    private final NotificationService notificationService;
    private final AdminAuditService auditService;

    public void processDecision(UUID itemId, ModerationDecision decision, UUID moderatorId) {
        ModerationQueueItem item = queueRepository.findById(itemId)
            .orElseThrow(() -> new ModerationItemNotFoundException(itemId));

        // Record decision
        ModerationDecisionEntity decisionEntity = switch (decision) {
            case Approve approve -> handleApprove(item, approve, moderatorId);
            case Reject reject -> handleReject(item, reject, moderatorId);
            case Escalate escalate -> handleEscalate(item, escalate, moderatorId);
            case RequestMoreInfo info -> handleRequestInfo(item, info, moderatorId);
        };

        // Update queue item status
        item.setStatus(decisionEntity.getStatus());
        item.setResolvedAt(Instant.now());
        queueRepository.save(item);

        // Audit log
        auditService.log("MODERATION_DECISION", "QUEUE_ITEM", itemId,
            Map.of("decision", decision), moderatorId);
    }

    private ModerationDecisionEntity handleReject(
            ModerationQueueItem item,
            Reject reject,
            UUID moderatorId) {

        // Apply punishment if specified
        if (reject.action() != null) {
            switch (reject.action()) {
                case WARN -> punishmentService.warn(
                    item.getReportedUserId(), reject.reason(), moderatorId);
                case MUTE -> punishmentService.mute(
                    item.getReportedUserId(), Duration.ofHours(24), reject.reason(), moderatorId);
                case SUSPEND -> punishmentService.suspend(
                    item.getReportedUserId(), Duration.ofDays(7), reject.reason(), moderatorId);
                case BAN -> punishmentService.ban(
                    item.getReportedUserId(), reject.reason(), moderatorId);
            }
        }

        // Delete/hide the content
        contentService.hideContent(item.getContentType(), item.getContentId());

        // Notify user
        notificationService.notifyContentRejected(
            item.getReportedUserId(), item.getContentType(), reject.reason());

        return createDecisionEntity(item, "REJECT", reject.reason(), moderatorId);
    }
}
```

### Moderation Queue View
```java
@Route(value = "admin/moderation", layout = AdminLayout.class)
@RolesAllowed({"ADMIN", "MODERATOR"})
public class ModerationQueueView extends AbstractAdminView {
    private final ModerationQueueService queueService;
    private final Grid<ModerationQueueItem> grid;

    public ModerationQueueView(
            ModerationQueueService queueService,
            AdminAuditService auditService) {
        super(auditService);
        this.queueService = queueService;

        add(createHeader(), createFilters(), createGrid());
        refreshQueue();
    }

    private Grid<ModerationQueueItem> createGrid() {
        grid = new Grid<>(ModerationQueueItem.class, false);

        grid.addColumn(item -> item.contentType().getDisplayName())
            .setHeader("Type")
            .setWidth("100px");

        grid.addColumn(item -> item.priority().name())
            .setHeader("Priority")
            .setWidth("100px")
            .setClassNameGenerator(item -> "priority-" + item.priority().name().toLowerCase());

        grid.addColumn(item -> item.reason())
            .setHeader("Reason")
            .setFlexGrow(1);

        grid.addColumn(new LocalDateTimeRenderer<>(
                ModerationQueueItem::createdAt,
                "MMM d, HH:mm"))
            .setHeader("Reported")
            .setWidth("120px");

        grid.addComponentColumn(this::createActions)
            .setHeader("Actions")
            .setWidth("200px");

        grid.setPageSize(50);
        grid.addItemClickListener(e -> openDetailDialog(e.getItem()));

        return grid;
    }

    private HorizontalLayout createActions(ModerationQueueItem item) {
        Button approve = new Button(VaadinIcon.CHECK.create(), e -> quickApprove(item));
        approve.addThemeVariants(ButtonVariant.LUMO_SUCCESS, ButtonVariant.LUMO_SMALL);
        approve.getElement().setAttribute("title", "Approve");

        Button reject = new Button(VaadinIcon.CLOSE.create(), e -> openRejectDialog(item));
        reject.addThemeVariants(ButtonVariant.LUMO_ERROR, ButtonVariant.LUMO_SMALL);
        reject.getElement().setAttribute("title", "Reject");

        Button escalate = new Button(VaadinIcon.ARROW_UP.create(), e -> escalate(item));
        escalate.addThemeVariants(ButtonVariant.LUMO_CONTRAST, ButtonVariant.LUMO_SMALL);
        escalate.getElement().setAttribute("title", "Escalate");

        return new HorizontalLayout(approve, reject, escalate);
    }
}
```

---

## Success Criteria

The implementation is complete when:
- [ ] All moderation views are functional (Queue, Photos, Messages, Profiles, Reports, Appeals)
- [ ] Backend services handle all moderation workflows
- [ ] Automated filters are pluggable and configurable
- [ ] Report → Queue → Decision → Punishment pipeline works end-to-end
- [ ] Appeal system allows users to contest decisions
- [ ] All actions are audit logged
- [ ] Role-based access restricts moderator actions appropriately
- [ ] Unit tests achieve 70%+ coverage
- [ ] Integration tests verify complete workflows
- [ ] Two critical reviews pass with no issues
- [ ] Super-critical review passes with no issues
- [ ] Code is clean, maintainable, and modular

**BEGIN IMPLEMENTATION**
