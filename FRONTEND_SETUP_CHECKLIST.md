# Frontend Setup Checklist

This checklist outlines the immediate steps needed to make the frontend functional.

## Phase 1: Configuration Files (Priority: CRITICAL)

- [ ] Create `vite.config.ts`
  ```typescript
  import { defineConfig } from 'vite'
  import react from '@vitejs/plugin-react'
  
  export default defineConfig({
    plugins: [react()],
    server: {
      port: 3000,
      proxy: {
        '/api': {
          target: 'http://localhost:8080',
          changeOrigin: true,
        }
      }
    },
    build: {
      target: 'es2020',
      outDir: 'dist',
      sourcemap: true,
    },
    resolve: {
      alias: {
        '@': '/src',
      },
    },
  })
  ```

- [ ] Create `tsconfig.json`
  ```json
  {
    "compilerOptions": {
      "target": "ES2020",
      "useDefineForClassFields": true,
      "lib": ["ES2020", "DOM", "DOM.Iterable"],
      "module": "ESNext",
      "skipLibCheck": true,
      "jsx": "react-jsx",
      "strict": true,
      "resolveJsonModule": true,
      "allowImportingTsExtensions": true,
      "noEmit": true,
      "baseUrl": ".",
      "paths": {
        "@/*": ["src/*"]
      }
    },
    "include": ["src"],
    "exclude": ["node_modules", "dist", "build"]
  }
  ```

- [ ] Create `tailwind.config.js`
  ```javascript
  export default {
    content: [
      "./src/**/*.{js,ts,jsx,tsx}",
    ],
    theme: {
      extend: {},
    },
    plugins: [],
  }
  ```

- [ ] Create `postcss.config.js`
  ```javascript
  export default {
    plugins: {
      tailwindcss: {},
      autoprefixer: {},
    },
  }
  ```

- [ ] Create `jest.config.js`
  ```javascript
  export default {
    preset: 'ts-jest',
    testEnvironment: 'jsdom',
    roots: ['<rootDir>/src'],
    setupFilesAfterEnv: ['<rootDir>/src/setupTests.ts'],
    moduleNameMapper: {
      '^@/(.*)$': '<rootDir>/src/$1',
    },
    moduleFileExtensions: ['ts', 'tsx', 'js', 'jsx'],
    collectCoverageFrom: [
      'src/**/*.{ts,tsx}',
      '!src/**/*.d.ts',
      '!src/main.tsx',
    ],
  }
  ```

- [ ] Create `.eslintrc.json`
  ```json
  {
    "extends": "react-app"
  }
  ```

- [ ] Create `frontend/.env.example`
  ```env
  VITE_API_URL=http://localhost:8080
  VITE_WS_URL=ws://localhost:8080/ws
  ```

- [ ] Create `frontend/.env.local` (for development)
  ```env
  VITE_API_URL=http://localhost:8080
  VITE_WS_URL=ws://localhost:8080/ws
  ```

## Phase 2: Dependencies (Priority: CRITICAL)

Update `package.json` and install:

- [ ] Remove incorrect dependencies
- [ ] Add missing build dependencies:
  - `postcss`
  - `autoprefixer`
  - `@types/node`

- [ ] Add missing testing dependencies:
  - `ts-jest`
  - `jest-environment-jsdom`
  - `@testing-library/user-event`

- [ ] Add missing quality dependencies:
  - `eslint`
  - `eslint-config-react-app`
  - `eslint-plugin-react`

- [ ] Add missing utility dependencies:
  - `date-fns` (for date formatting)
  - `js-cookie` (for cookie management)

- [ ] Optional: Replace Jest with Vitest (Vite-native)
  ```bash
  npm remove jest ts-jest jest-environment-jsdom
  npm install --save-dev vitest @vitest/ui jsdom
  ```

- [ ] Run `npm install` to verify all dependencies install correctly

## Phase 3: Entry Point (Priority: CRITICAL)

- [ ] Create `public/` directory
  - [ ] Create `public/index.html`
    ```html
    <!DOCTYPE html>
    <html lang="en">
      <head>
        <meta charset="UTF-8" />
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <meta name="description" content="POC Dating Web Application" />
        <title>POC Dating Web</title>
      </head>
      <body>
        <div id="root"></div>
        <script type="module" src="/src/main.tsx"></script>
      </body>
    </html>
    ```
  - [ ] Add `public/favicon.ico` (placeholder)
  - [ ] Create `public/assets/` directory

- [ ] Create `src/` directory structure
- [ ] Create `src/main.tsx`
  ```typescript
  import React from 'react'
  import ReactDOM from 'react-dom/client'
  import App from './App'
  import './styles/tailwind.css'
  
  ReactDOM.createRoot(document.getElementById('root')!).render(
    <React.StrictMode>
      <App />
    </React.StrictMode>,
  )
  ```

- [ ] Create `src/App.tsx` (basic component)
  ```typescript
  import { BrowserRouter as Router } from 'react-router-dom'
  
  function App() {
    return (
      <Router>
        <div className="min-h-screen bg-gray-50">
          <h1 className="text-3xl font-bold p-8">POC Dating Web</h1>
        </div>
      </Router>
    )
  }
  
  export default App
  ```

## Phase 4: Project Structure (Priority: HIGH)

Create directory structure:

```
src/
├── components/
│   ├── common/
│   │   ├── Button.tsx
│   │   ├── Input.tsx
│   │   ├── Card.tsx
│   │   ├── Modal.tsx
│   │   ├── Loader.tsx
│   │   ├── Toast.tsx
│   │   └── Avatar.tsx
│   ├── auth/
│   │   ├── LoginForm.tsx
│   │   ├── RegisterForm.tsx
│   │   └── ProtectedRoute.tsx
│   ├── profile/
│   │   ├── ProfileCard.tsx
│   │   ├── PreferenceForm.tsx
│   │   └── ProfileEditor.tsx
│   ├── match/
│   │   ├── SwipeCard.tsx
│   │   ├── SwipeStack.tsx
│   │   └── MatchList.tsx
│   └── chat/
│       ├── MessageList.tsx
│       ├── MessageInput.tsx
│       └── ConversationList.tsx
├── pages/
│   ├── HomePage.tsx
│   ├── LoginPage.tsx
│   ├── RegisterPage.tsx
│   ├── ProfilePage.tsx
│   ├── SwipePage.tsx
│   ├── MatchesPage.tsx
│   ├── ChatPage.tsx
│   └── NotFoundPage.tsx
├── services/
│   ├── api.ts (Axios instance with interceptors)
│   ├── userService.ts
│   ├── matchService.ts
│   ├── chatService.ts
│   └── recommendationService.ts
├── store/
│   ├── authStore.ts
│   ├── userStore.ts
│   ├── matchStore.ts
│   ├── chatStore.ts
│   └── uiStore.ts
├── hooks/
│   ├── useAuth.ts
│   ├── useApi.ts
│   └── useWebSocket.ts
├── types/
│   ├── user.ts
│   ├── match.ts
│   ├── message.ts
│   └── api.ts
├── utils/
│   ├── validators.ts
│   ├── formatters.ts
│   └── storage.ts
├── styles/
│   ├── tailwind.css
│   └── globals.css
├── setupTests.ts
├── App.tsx
└── main.tsx
```

- [ ] Create all directories
- [ ] Create placeholder files in each directory
- [ ] Add `setupTests.ts` for test configuration

## Phase 5: Verification (Priority: HIGH)

- [ ] Run `npm install` - should complete successfully
- [ ] Run `npm run dev` - should start dev server on port 3000
- [ ] Run `npm run build` - should create dist folder
- [ ] Run `npm run test` - should run tests (even if no tests yet)
- [ ] Run `npm run lint` - should lint code
- [ ] Navigate to `http://localhost:3000` - should see app

## Phase 6: Core Implementation (Priority: MEDIUM)

**API Service Layer:**
- [ ] Implement `services/api.ts`
  - JWT token interceptors
  - Error handling
  - Request/response transformation

- [ ] Implement service files
  - `services/userService.ts`
  - `services/matchService.ts`
  - `services/chatService.ts`
  - `services/recommendationService.ts`

**Zustand Stores:**
- [ ] Implement `store/authStore.ts`
- [ ] Implement `store/userStore.ts`
- [ ] Implement `store/matchStore.ts`
- [ ] Implement `store/chatStore.ts`
- [ ] Implement `store/uiStore.ts`

**Authentication:**
- [ ] Create login/register pages
- [ ] Implement auth flow
- [ ] Add protected routes
- [ ] Add JWT token management

**Features:**
- [ ] Implement matching/swiping interface
- [ ] Implement chat with WebSocket
- [ ] Implement profile management
- [ ] Implement user preferences

## Phase 7: Testing (Priority: MEDIUM)

- [ ] Create unit tests for utilities
- [ ] Create store tests for Zustand
- [ ] Create component tests with React Testing Library
- [ ] Add integration tests
- [ ] Achieve 80%+ code coverage

## Phase 8: Polish (Priority: LOW)

- [ ] Add error boundaries
- [ ] Improve error messages
- [ ] Optimize bundle size
- [ ] Add performance monitoring
- [ ] Implement dark mode (optional)
- [ ] Add animations (optional)
- [ ] Create storybook components (optional)

---

## Quick Reference: Critical Error Fixes

### Issue: `REACT_APP_API_URL` env vars not working
Fix: Change to `VITE_API_URL` in `.env` files

### Issue: Jest won't run
Fix: Create `jest.config.js` and install `ts-jest`, `jest-environment-jsdom`

### Issue: Vite dev server won't start
Fix: Create `vite.config.ts`

### Issue: TypeScript errors in React JSX
Fix: Ensure `tsconfig.json` has `"jsx": "react-jsx"`

### Issue: Tailwind CSS not working
Fix: Create `tailwind.config.js` and `postcss.config.js`, ensure imported in `src/main.tsx`

### Issue: Import errors with `@/` alias
Fix: Add to `tsconfig.json` paths configuration

---

## Timeline

- **Phase 1-4:** 2-3 days (setup & structure)
- **Phase 5:** 1 day (verification)
- **Phase 6:** 2-3 weeks (feature implementation)
- **Phase 7:** 1 week (testing)
- **Phase 8:** 1 week (polish)

**Total: 3-4 weeks for complete frontend**

---

## Testing Commands

Once everything is set up:

```bash
# Install dependencies
npm install

# Start development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview

# Run tests
npm test

# Run tests in watch mode
npm run test:watch

# Run tests with coverage
npm run test:coverage

# Lint code
npm run lint
```

---

## Notes

- The backend services run on ports 8081-8084
- API Gateway is on port 8080
- Frontend dev server runs on port 3000
- All API requests are proxied to localhost:8080 in dev
- WebSocket connects to the same backend

