# Vaadin UI Implementation Priorities

**Quick Reference - Prioritized List of 47 Items**

---

## CRITICAL (8 items) - Blocking Core Functionality

1. **No Profile Photo Upload** - RegisterView, ProfileView
   - Replace URL textfield with file upload component
   - Priority: IMMEDIATE (users can't add photos)

2. **No Photo Gallery** - ProfileCard, SwipeView
   - Multiple photos not supported; only single photo
   - Need: `photoUrls` list in User DTO + carousel component

3. **Missing Distance/Compatibility** - SwipeView
   - Users don't see match distance or compatibility score
   - Need: distance label + progress bar in ProfileCard

4. **No Online Status** - MatchesView, ChatView, ProfileCard
   - Users don't know if matches are active
   - Need: green/red badge, "Last active X ago"

5. **Chat No Image Sharing** - ChatView
   - Text-only messaging; no photo sharing
   - Need: upload button + image display in bubbles

6. **No Distance Slider** - PreferencesView
   - IntegerField is poor UX for range selection
   - Need: visual slider component

7. **Missing Notification Preferences** - SettingsView
   - No control over email/push notifications
   - Need: notification settings card

8. **Missing Privacy Settings** - SettingsView
   - No profile visibility or activity controls
   - Need: privacy toggles (online status, last active, messages)

---

## HIGH PRIORITY (15 items) - Important Features

9. **No Interests Component** - ProfileView
10. **No Verification Badge** - ProfileCard, UserDetailView
11. **MessagesView No Search** - MessagesView
12. **MessagesView No Delete** - MessagesView
13. **MatchesView No Sorting** - MatchesView
14. **NotificationsView Mark Read** - NotificationsView
15. **RegisterView No Terms** - RegisterView
16. **ProfileView No Completion Indicator** - ProfileView
17. **PreferencesView No Distance Visualization** - PreferencesView
18. **ChatView Read Receipts Incomplete** - ChatView
19. **ChatView Typing Animation Missing** - ChatView
20. **No Last Active Timestamp** - MatchesView, ProfileCard
21. **Incomplete Form Validation** - All forms
22. **Missing Loading States** - MatchesView, MessagesView
23. **CSS Animations Incomplete** - styles.css

---

## MEDIUM PRIORITY (24 items) - Polish & Edge Cases

24. Accessibility (ARIA labels, keyboard nav)
25. Better error messages with retry
26. Consistent empty state design
27. Image error handling + retry
28. Standardized confirmation dialogs
29. Centralized date formatting utility
30. Optional field indicators
31. Password strength meter
32. Photo crop/resize tool
33. WebSocket for real-time chat
34. Block/report consequences clarity
35. Profile completion checklist
36. Compatibility breakdown details
37. Photo source attribution
38. Save/favorite profiles feature
39. Undo limit (currently unlimited)
40. Auto-logout on inactivity
41. Activity feed view (NEW)
42. Profile view counter
43. Mutual interests highlight
44. Mobile responsiveness improvements
45. Dark mode / theme customization
46. Notification badge count in navbar
47. GDPR data export feature

---

## Implementation Order (Recommended)

### Week 1: Critical Path
1. ImageUploadComponent.java
2. Add upload to RegisterView
3. Add upload to ProfileView
4. PhotoGalleryComponent.java
5. Update ProfileCard with distance/compatibility
6. OnlineStatusBadge.java

### Week 2: Core Features
7. InterestTagsComponent.java + ProfileView integration
8. VerificationBadge.java
9. MessagesView search
10. MessagesView delete with confirmation
11. MatchesView sorting

### Week 3: Settings & Polish
12. SettingsView notification prefs
13. SettingsView privacy settings
14. ChatView image upload
15. Form validation enhancements
16. Loading state indicators
17. CSS animation fixes

---

## Quick Wins (< 1 hour each)

- [ ] Add terms checkbox to RegisterView
- [ ] Add search field to MessagesView
- [ ] Add delete button column to MessagesView grid
- [ ] Add sorting to MatchesView grid columns
- [ ] Fix typing dots animation in CSS
- [ ] Add password strength indicator (RegisterView)
- [ ] Add notification badge to MainLayout nav
- [ ] Create DateFormatUtil utility class
- [ ] Display "Last active X minutes ago"
- [ ] Improve empty state messages (NotificationsView example to copy)

---

## Missing Components to Create

Location: `/backend/vaadin-ui-service/src/main/java/com/dating/ui/components/`

1. ImageUploadComponent.java
2. PhotoGalleryComponent.java
3. OnlineStatusBadge.java
4. VerificationBadge.java
5. InterestTagsComponent.java
6. ConfirmDialogWrapper.java
7. EmptyStateCard.java
8. LoadingSpinner.java
9. PasswordStrengthMeter.java
10. DistanceDisplay.java

---

## Data Model Updates Needed

Add to `User.java` DTO:

```java
private List<String> photoUrls;           // Multi-photo gallery
private List<String> interests;           // Interest tags
private Boolean isVerified;               // Verification badge
private Double distance;                  // Distance from user
private Integer compatibilityScore;       // Match percentage
private Boolean isOnline;                 // Online status
private Instant lastActiveAt;             // Last activity time
private Boolean emailNotifications;       // Notification setting
private Boolean profileVisible;           // Privacy setting
private Integer profileViewCount;         // Profile view count
```

---

## Testing Checklist

- [ ] File upload accepts images only
- [ ] Gallery navigation works (prev/next)
- [ ] Search in MessagesView filters in real-time
- [ ] Delete removes conversation from UI
- [ ] Online status updates without refresh
- [ ] Read receipts change dynamically
- [ ] Form validation blocks invalid input
- [ ] Empty states show in all views
- [ ] Mobile responsiveness at 360px width
- [ ] Error messages are helpful + have retry

---

## Effort Estimate

| Phase | Items | Days | Total |
|-------|-------|------|-------|
| Critical | 8 | 5-7 | **5-7 days** |
| High | 15 | 8-10 | **8-10 days** |
| Medium | 24 | 7-10 | **7-10 days** |
| **TOTAL** | **47** | | **3-4 weeks** |

---

## Start Here

1. **Read full audit:** VAADIN_UI_AUDIT.md
2. **Create ImageUploadComponent** - blocks multiple other features
3. **Implement photo uploads** - RegisterView & ProfileView
4. **Follow Week 1 roadmap** - establish patterns for other components

