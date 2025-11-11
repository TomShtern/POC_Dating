# POC Dating Web Application

## Overview

React TypeScript web frontend for the POC Dating application.

## Purpose

User-facing application providing:
- User authentication and profile management
- Swiping interface for potential matches
- Real-time messaging with matched users
- Personalized recommendations
- User settings and preferences

## Port
**3000** (development)

## Architecture

### Directory Structure

```
frontend/
├── src/
│   ├── components/          # Reusable React components
│   │   ├── common/         # Shared components (Button, Input, etc)
│   │   ├── auth/           # Auth-related components
│   │   ├── profile/        # Profile components
│   │   ├── match/          # Matching/swiping components
│   │   └── chat/           # Chat components
│   │
│   ├── pages/              # Page-level components (routes)
│   │   ├── LoginPage.tsx
│   │   ├── RegisterPage.tsx
│   │   ├── ProfilePage.tsx
│   │   ├── SwipePage.tsx
│   │   ├── ChatPage.tsx
│   │   └── PreferencesPage.tsx
│   │
│   ├── services/           # API communication
│   │   ├── api.ts          # Axios instance with interceptors
│   │   ├── userService.ts
│   │   ├── matchService.ts
│   │   ├── chatService.ts
│   │   └── recommendationService.ts
│   │
│   ├── store/              # State management (Zustand)
│   │   ├── authStore.ts    # Authentication state
│   │   ├── userStore.ts    # User profile state
│   │   ├── matchStore.ts   # Match/swipe state
│   │   ├── chatStore.ts    # Chat state
│   │   └── uiStore.ts      # UI state (modals, notifications)
│   │
│   ├── hooks/              # Custom React hooks
│   │   ├── useAuth.ts
│   │   ├── useWebSocket.ts
│   │   └── useApi.ts
│   │
│   ├── types/              # TypeScript type definitions
│   │   ├── user.ts
│   │   ├── match.ts
│   │   ├── message.ts
│   │   └── api.ts
│   │
│   ├── utils/              # Utility functions
│   │   ├── validators.ts   # Form validation
│   │   ├── formatters.ts   # Date/time formatting
│   │   └── storage.ts      # LocalStorage helpers
│   │
│   ├── styles/             # Global styles
│   │   ├── tailwind.css
│   │   └── globals.css
│   │
│   ├── App.tsx             # Main App component
│   └── main.tsx            # Entry point
│
├── public/                 # Static assets
│   ├── index.html
│   ├── favicon.ico
│   └── assets/
│
├── package.json
├── tsconfig.json
├── tailwind.config.js
├── vite.config.ts
├── jest.config.js
└── README.md
```

## Key Technologies

- **React 18**: UI library with hooks
- **TypeScript**: Type safety
- **Vite**: Fast build tool and dev server
- **React Router v6**: Client-side routing
- **Zustand**: Lightweight state management
- **Axios**: HTTP client
- **Tailwind CSS**: Utility-first CSS framework
- **Jest**: Unit testing
- **React Testing Library**: Component testing

## Why These Choices

### Vite over Create React App
- Faster development server (HMR)
- Faster build times
- Modern ES modules
- Smaller bundle size

### Zustand over Redux
- Simpler API, less boilerplate
- Less learning curve
- Sufficient for POC scope
- Can migrate to Redux later if needed

### Tailwind over Custom CSS
- Rapid UI development
- Consistent design system
- Utility-first approach
- Easy to customize

### TypeScript
- Type safety catches bugs early
- Better IDE autocomplete
- Self-documenting code
- Easier refactoring

## State Management (Zustand)

### Store Structure

```typescript
// authStore.ts
interface AuthState {
  token: string | null;
  userId: string | null;
  isLoading: boolean;
  error: string | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
}

// userStore.ts
interface UserState {
  currentUser: User | null;
  isLoading: boolean;
  updateProfile: (data: Partial<User>) => Promise<void>;
}

// matchStore.ts
interface MatchState {
  feed: User[];
  matches: Match[];
  currentCard: User | null;
  swipe: (targetId: string, action: 'like' | 'pass') => Promise<void>;
  loadFeed: () => Promise<void>;
}

// chatStore.ts
interface ChatState {
  conversations: Conversation[];
  currentConversation: Conversation | null;
  messages: Message[];
  sendMessage: (text: string) => Promise<void>;
  loadMessages: () => Promise<void>;
}
```

## API Integration

### Request Interceptor
```typescript
// Adds JWT token to all requests
// Refreshes token if expired
// Handles 401 responses
```

### Error Handling
```typescript
// Global error handler
// Display notifications to user
// Redirect to login on 401
```

## WebSocket Integration

### Chat Connection
```typescript
// WebSocket connection to /api/chat/ws
// JWT authentication in header
// Subscribe/unsubscribe to conversations
// Real-time message delivery
// Connection state management
```

## Authentication Flow

```
1. User enters email/password
2. POST /api/users/auth/login
3. Receive JWT token
4. Store in localStorage & memory
5. Add token to Authorization header (all requests)
6. Refresh token on expiration
7. Logout → clear token
```

## Pages & Routes

```
/                      → Homepage (redirect based on auth)
/login                 → Login page
/register              → Registration page
/profile               → User profile (auth required)
/settings/preferences  → Preference settings
/swipe                 → Swiping interface
/matches               → All matches list
/chat/:matchId         → Chat with matched user
/chat                  → All conversations
/404                   → Not found
```

## Component Examples (To Be Implemented)

### Reusable Components
- `Button`: Styled button with variants
- `Input`: Text input with validation feedback
- `Card`: Generic card container
- `Modal`: Dialog wrapper
- `Loader`: Loading spinner
- `Toast/Notification`: Success/error messages
- `Avatar`: User profile picture

### Feature Components
- `SwipeCard`: Individual card for swiping
- `SwipeStack`: Stack of cards for swiping
- `MessageList`: Chat message list
- `MessageInput`: Message input with send
- `ProfileCard`: User profile display
- `PreferenceForm`: Preferences form

## Testing Strategy

### Unit Tests
- Store actions (Zustand)
- Utility functions
- Validators

### Component Tests
- Component rendering
- User interactions (clicks, typing)
- Props variations
- Error states

### Integration Tests
- API integration
- Full page flows (login → swipe → chat)
- Navigation

### E2E Tests (Cypress)
- Complete user journeys
- Real API calls (staging env)

## Development

### Setup
```bash
cd frontend
npm install
cp .env.example .env.local
npm run dev
```

### Environment Variables
```env
REACT_APP_API_URL=http://localhost:8080
REACT_APP_WS_URL=ws://localhost:8080/ws
```

### Build
```bash
npm run build    # Production build
npm run preview  # Preview production build
```

## Performance Optimization

- Code splitting by route
- Lazy loading of pages
- Image optimization
- Bundle size monitoring
- Lighthouse audits

## Browser Support

- Chrome (latest)
- Firefox (latest)
- Safari (latest)
- Edge (latest)
- Mobile browsers (iOS Safari, Chrome Mobile)

## Future Enhancements

- Mobile app (React Native)
- PWA support (offline, push notifications)
- Image upload optimization
- Video call integration
- Dark mode
- Internationalization (i18n)
- Advanced filters
- Analytics tracking
- Social sharing
