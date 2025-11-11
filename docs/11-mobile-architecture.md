# Mobile App Architecture & Performance

## Overview

React Native architecture patterns, state management, performance optimization, and best practices for building a high-performance dating app.

**Technologies**: React Native, TypeScript, Zustand/Redux, React Query

---

## Table of Contents

1. [Project Structure](#project-structure)
2. [State Management](#state-management)
3. [Navigation](#navigation)
4. [API Client & Data Fetching](#api-client--data-fetching)
5. [FlatList Optimization](#flatlist-optimization)
6. [Image Handling](#image-handling)
7. [WebSocket Integration](#websocket-integration)
8. [Performance Optimization](#performance-optimization)

---

## Project Structure

### Recommended React Native Structure

```
mobile/
├── src/
│   ├── api/                       # API client and endpoints
│   │   ├── client.ts              # Axios/Fetch configuration
│   │   ├── auth.ts                # Authentication endpoints
│   │   ├── users.ts               # User endpoints
│   │   ├── matches.ts             # Match endpoints
│   │   └── messages.ts            # Messaging endpoints
│   │
│   ├── components/                # Reusable components
│   │   ├── common/                # Generic components
│   │   │   ├── Button.tsx
│   │   │   ├── Input.tsx
│   │   │   ├── LoadingSpinner.tsx
│   │   │   └── Avatar.tsx
│   │   ├── profile/               # Profile-specific components
│   │   │   ├── ProfileCard.tsx
│   │   │   ├── PhotoGrid.tsx
│   │   │   └── EditProfileForm.tsx
│   │   ├── matching/
│   │   │   ├── SwipeCard.tsx
│   │   │   └── MatchModal.tsx
│   │   └── messaging/
│   │       ├── MessageBubble.tsx
│   │       ├── MessageInput.tsx
│   │       └── TypingIndicator.tsx
│   │
│   ├── screens/                   # Screen components
│   │   ├── auth/
│   │   │   ├── LoginScreen.tsx
│   │   │   ├── RegisterScreen.tsx
│   │   │   └── OnboardingScreen.tsx
│   │   ├── discovery/
│   │   │   └── DiscoveryScreen.tsx
│   │   ├── matches/
│   │   │   └── MatchesScreen.tsx
│   │   ├── messages/
│   │   │   ├── ConversationsScreen.tsx
│   │   │   └── ChatScreen.tsx
│   │   └── profile/
│   │       ├── ProfileScreen.tsx
│   │       └── EditProfileScreen.tsx
│   │
│   ├── navigation/                # Navigation configuration
│   │   ├── RootNavigator.tsx
│   │   ├── AuthNavigator.tsx
│   │   ├── MainNavigator.tsx
│   │   └── types.ts
│   │
│   ├── store/                     # State management
│   │   ├── authStore.ts           # Auth state (Zustand)
│   │   ├── userStore.ts           # User profile state
│   │   ├── matchStore.ts          # Matches state
│   │   └── messageStore.ts        # Messages state
│   │
│   ├── hooks/                     # Custom hooks
│   │   ├── useAuth.ts
│   │   ├── useMatches.ts
│   │   ├── useMessages.ts
│   │   ├── useWebSocket.ts
│   │   └── useLocation.ts
│   │
│   ├── services/                  # Business logic services
│   │   ├── authService.ts
│   │   ├── locationService.ts
│   │   ├── notificationService.ts
│   │   ├── websocketService.ts
│   │   └── imageService.ts
│   │
│   ├── utils/                     # Utility functions
│   │   ├── validators.ts
│   │   ├── formatters.ts
│   │   ├── dateUtils.ts
│   │   └── imageUtils.ts
│   │
│   ├── types/                     # TypeScript types
│   │   ├── user.ts
│   │   ├── match.ts
│   │   ├── message.ts
│   │   └── api.ts
│   │
│   ├── constants/                 # App constants
│   │   ├── colors.ts
│   │   ├── dimensions.ts
│   │   └── config.ts
│   │
│   └── App.tsx                    # App entry point
│
├── assets/                        # Static assets
│   ├── images/
│   ├── fonts/
│   └── icons/
│
├── android/                       # Android-specific code
├── ios/                           # iOS-specific code
├── package.json
├── tsconfig.json
├── babel.config.js
├── metro.config.js
└── README.md
```

---

## State Management

### Zustand (Recommended for Dating App)

**Why Zustand?**
- ✅ Lightweight (< 1KB)
- ✅ No boilerplate
- ✅ Great performance (minimal re-renders)
- ✅ Simple API
- ✅ TypeScript support

**Auth Store:**

```typescript
// src/store/authStore.ts
import { create } from 'zustand';
import { persist, createJSONStorage } from 'zustand/middleware';
import AsyncStorage from '@react-native-async-storage/async-storage';
import * as Keychain from 'react-native-keychain';

interface User {
  id: string;
  email: string;
  firstName: string;
  verified: boolean;
}

interface AuthState {
  user: User | null;
  accessToken: string | null;
  refreshToken: string | null;
  isAuthenticated: boolean;

  // Actions
  setUser: (user: User) => void;
  setTokens: (accessToken: string, refreshToken: string) => void;
  logout: () => void;
}

export const useAuthStore = create<AuthState>()(
  persist(
    (set) => ({
      user: null,
      accessToken: null,
      refreshToken: null,
      isAuthenticated: false,

      setUser: (user) => set({ user, isAuthenticated: true }),

      setTokens: async (accessToken, refreshToken) => {
        // Store refresh token securely
        await Keychain.setGenericPassword('refreshToken', refreshToken);
        set({ accessToken, refreshToken });
      },

      logout: async () => {
        await Keychain.resetGenericPassword();
        set({
          user: null,
          accessToken: null,
          refreshToken: null,
          isAuthenticated: false,
        });
      },
    }),
    {
      name: 'auth-storage',
      storage: createJSONStorage(() => AsyncStorage),
      partialize: (state) => ({
        user: state.user,
        isAuthenticated: state.isAuthenticated,
        // Don't persist tokens - they're in Keychain
      }),
    }
  )
);
```

**Match Store:**

```typescript
// src/store/matchStore.ts
import { create } from 'zustand';
import { immer } from 'zustand/middleware/immer';

interface Match {
  id: string;
  user: {
    id: string;
    firstName: string;
    age: number;
    photos: string[];
  };
  matchedAt: string;
  lastMessage?: {
    content: string;
    sentAt: string;
  };
  unreadCount: number;
}

interface MatchState {
  matches: Match[];
  currentCandidates: any[];
  isLoading: boolean;

  // Actions
  setMatches: (matches: Match[]) => void;
  addMatch: (match: Match) => void;
  removeMatch: (matchId: string) => void;
  updateUnreadCount: (matchId: string, count: number) => void;
  setCandidates: (candidates: any[]) => void;
}

export const useMatchStore = create<MatchState>()(
  immer((set) => ({
    matches: [],
    currentCandidates: [],
    isLoading: false,

    setMatches: (matches) =>
      set((state) => {
        state.matches = matches;
      }),

    addMatch: (match) =>
      set((state) => {
        state.matches.unshift(match);
      }),

    removeMatch: (matchId) =>
      set((state) => {
        state.matches = state.matches.filter((m) => m.id !== matchId);
      }),

    updateUnreadCount: (matchId, count) =>
      set((state) => {
        const match = state.matches.find((m) => m.id === matchId);
        if (match) {
          match.unreadCount = count;
        }
      }),

    setCandidates: (candidates) =>
      set((state) => {
        state.currentCandidates = candidates;
      }),
  }))
);
```

---

### Redux Toolkit (Alternative for Complex Apps)

**Store Setup:**

```typescript
// src/store/index.ts
import { configureStore } from '@reduxjs/toolkit';
import authReducer from './slices/authSlice';
import matchReducer from './slices/matchSlice';
import messageReducer from './slices/messageSlice';

export const store = configureStore({
  reducer: {
    auth: authReducer,
    matches: matchReducer,
    messages: messageReducer,
  },
  middleware: (getDefaultMiddleware) =>
    getDefaultMiddleware({
      serializableCheck: false, // Disable for dates, etc.
    }),
});

export type RootState = ReturnType<typeof store.getState>;
export type AppDispatch = typeof store.dispatch;
```

**Slice Example:**

```typescript
// src/store/slices/matchSlice.ts
import { createSlice, PayloadAction } from '@reduxjs/toolkit';

interface MatchState {
  matches: Match[];
  isLoading: boolean;
  error: string | null;
}

const initialState: MatchState = {
  matches: [],
  isLoading: false,
  error: null,
};

const matchSlice = createSlice({
  name: 'matches',
  initialState,
  reducers: {
    setMatches: (state, action: PayloadAction<Match[]>) => {
      state.matches = action.payload;
    },
    addMatch: (state, action: PayloadAction<Match>) => {
      state.matches.unshift(action.payload);
    },
    removeMatch: (state, action: PayloadAction<string>) => {
      state.matches = state.matches.filter((m) => m.id !== action.payload);
    },
  },
});

export const { setMatches, addMatch, removeMatch } = matchSlice.actions;
export default matchSlice.reducer;
```

---

## Navigation

### React Navigation Setup

```typescript
// src/navigation/RootNavigator.tsx
import React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import { useAuthStore } from '../store/authStore';
import AuthNavigator from './AuthNavigator';
import MainNavigator from './MainNavigator';

const Stack = createNativeStackNavigator();

export default function RootNavigator() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  return (
    <NavigationContainer>
      <Stack.Navigator screenOptions={{ headerShown: false }}>
        {isAuthenticated ? (
          <Stack.Screen name="Main" component={MainNavigator} />
        ) : (
          <Stack.Screen name="Auth" component={AuthNavigator} />
        )}
      </Stack.Navigator>
    </NavigationContainer>
  );
}
```

**Main Navigator (Tab + Stack):**

```typescript
// src/navigation/MainNavigator.tsx
import React from 'react';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import Icon from 'react-native-vector-icons/Ionicons';

import DiscoveryScreen from '../screens/discovery/DiscoveryScreen';
import MatchesScreen from '../screens/matches/MatchesScreen';
import MessagesScreen from '../screens/messages/ConversationsScreen';
import ProfileScreen from '../screens/profile/ProfileScreen';
import ChatScreen from '../screens/messages/ChatScreen';

const Tab = createBottomTabNavigator();
const Stack = createNativeStackNavigator();

// Messages stack
function MessagesStack() {
  return (
    <Stack.Navigator>
      <Stack.Screen name="Conversations" component={MessagesScreen} />
      <Stack.Screen name="Chat" component={ChatScreen} />
    </Stack.Navigator>
  );
}

export default function MainNavigator() {
  return (
    <Tab.Navigator
      screenOptions={({ route }) => ({
        tabBarIcon: ({ focused, color, size }) => {
          let iconName: string;

          switch (route.name) {
            case 'Discovery':
              iconName = focused ? 'flame' : 'flame-outline';
              break;
            case 'Matches':
              iconName = focused ? 'heart' : 'heart-outline';
              break;
            case 'Messages':
              iconName = focused ? 'chatbubbles' : 'chatbubbles-outline';
              break;
            case 'Profile':
              iconName = focused ? 'person' : 'person-outline';
              break;
            default:
              iconName = 'help';
          }

          return <Icon name={iconName} size={size} color={color} />;
        },
        tabBarActiveTintColor: '#FF6B6B',
        tabBarInactiveTintColor: 'gray',
      })}
    >
      <Tab.Screen name="Discovery" component={DiscoveryScreen} />
      <Tab.Screen name="Matches" component={MatchesScreen} />
      <Tab.Screen name="Messages" component={MessagesStack} />
      <Tab.Screen name="Profile" component={ProfileScreen} />
    </Tab.Navigator>
  );
}
```

---

## API Client & Data Fetching

### Axios Setup with Interceptors

```typescript
// src/api/client.ts
import axios from 'axios';
import { useAuthStore } from '../store/authStore';

const API_URL = __DEV__
  ? 'http://localhost:3000/api/v1'
  : 'https://api.datingapp.com/api/v1';

export const apiClient = axios.create({
  baseURL: API_URL,
  timeout: 10000,
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request interceptor - add auth token
apiClient.interceptors.request.use(
  (config) => {
    const accessToken = useAuthStore.getState().accessToken;

    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }

    return config;
  },
  (error) => Promise.reject(error)
);

// Response interceptor - handle token refresh
apiClient.interceptors.response.use(
  (response) => response,
  async (error) => {
    const originalRequest = error.config;

    // Token expired - try to refresh
    if (error.response?.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true;

      try {
        const refreshToken = useAuthStore.getState().refreshToken;
        const { data } = await axios.post(`${API_URL}/auth/refresh`, {
          refreshToken,
        });

        const { accessToken, refreshToken: newRefreshToken } = data;

        // Update tokens
        await useAuthStore.getState().setTokens(accessToken, newRefreshToken);

        // Retry original request
        originalRequest.headers.Authorization = `Bearer ${accessToken}`;
        return apiClient(originalRequest);
      } catch (refreshError) {
        // Refresh failed - logout
        useAuthStore.getState().logout();
        return Promise.reject(refreshError);
      }
    }

    return Promise.reject(error);
  }
);
```

### React Query Integration

```typescript
// src/hooks/useMatches.ts
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query';
import { apiClient } from '../api/client';

export function useMatches() {
  const queryClient = useQueryClient();

  const { data, isLoading, error } = useQuery({
    queryKey: ['matches'],
    queryFn: async () => {
      const { data } = await apiClient.get('/matches');
      return data.matches;
    },
    staleTime: 30000, // 30 seconds
  });

  const unmatchMutation = useMutation({
    mutationFn: async (matchId: string) => {
      await apiClient.delete(`/matches/${matchId}`);
    },
    onSuccess: () => {
      // Invalidate matches query to refetch
      queryClient.invalidateQueries({ queryKey: ['matches'] });
    },
  });

  return {
    matches: data || [],
    isLoading,
    error,
    unmatch: unmatchMutation.mutate,
  };
}
```

**Candidates with Infinite Query:**

```typescript
// src/hooks/useCandidates.ts
import { useInfiniteQuery } from '@tanstack/react-query';
import { apiClient } from '../api/client';

export function useCandidates() {
  const { data, fetchNextPage, hasNextPage, isLoading } = useInfiniteQuery({
    queryKey: ['candidates'],
    queryFn: async ({ pageParam = 0 }) => {
      const { data } = await apiClient.get('/matches/candidates', {
        params: {
          limit: 10,
          offset: pageParam,
        },
      });
      return data;
    },
    getNextPageParam: (lastPage, pages) => {
      if (!lastPage.hasMore) return undefined;
      return pages.length * 10;
    },
    staleTime: 300000, // 5 minutes
  });

  const candidates = data?.pages.flatMap((page) => page.candidates) || [];

  return {
    candidates,
    fetchNextPage,
    hasNextPage,
    isLoading,
  };
}
```

---

## FlatList Optimization

### Optimized Match List

```typescript
// src/screens/matches/MatchesScreen.tsx
import React, { useCallback, useMemo } from 'react';
import { FlatList, View, StyleSheet } from 'react-native';
import { useMatches } from '../../hooks/useMatches';
import MatchCard from '../../components/matching/MatchCard';

export default function MatchesScreen() {
  const { matches, isLoading } = useMatches();

  // Memoized key extractor
  const keyExtractor = useCallback((item: Match) => item.id, []);

  // Memoized render item
  const renderItem = useCallback(
    ({ item }: { item: Match }) => <MatchCard match={item} />,
    []
  );

  // Get item layout for better scrolling
  const getItemLayout = useCallback(
    (data: any, index: number) => ({
      length: ITEM_HEIGHT,
      offset: ITEM_HEIGHT * index,
      index,
    }),
    []
  );

  if (isLoading) {
    return <LoadingSpinner />;
  }

  return (
    <FlatList
      data={matches}
      renderItem={renderItem}
      keyExtractor={keyExtractor}
      getItemLayout={getItemLayout}
      // Performance optimizations
      maxToRenderPerBatch={10}
      updateCellsBatchingPeriod={50}
      initialNumToRender={10}
      windowSize={5}
      removeClippedSubviews={true}
      // Pull to refresh
      refreshing={isLoading}
      onRefresh={() => refetch()}
    />
  );
}

const ITEM_HEIGHT = 120;
```

**Memoized List Item:**

```typescript
// src/components/matching/MatchCard.tsx
import React, { memo } from 'react';
import { View, Text, Image, TouchableOpacity, StyleSheet } from 'react-native';
import FastImage from 'react-native-fast-image';

interface Props {
  match: Match;
}

const MatchCard = memo(({ match }: Props) => {
  const handlePress = () => {
    // Navigate to chat
  };

  return (
    <TouchableOpacity style={styles.container} onPress={handlePress}>
      <FastImage
        source={{ uri: match.user.photos[0] }}
        style={styles.avatar}
        resizeMode={FastImage.resizeMode.cover}
      />
      <View style={styles.info}>
        <Text style={styles.name}>
          {match.user.firstName}, {match.user.age}
        </Text>
        {match.lastMessage && (
          <Text style={styles.lastMessage} numberOfLines={1}>
            {match.lastMessage.content}
          </Text>
        )}
      </View>
      {match.unreadCount > 0 && (
        <View style={styles.badge}>
          <Text style={styles.badgeText}>{match.unreadCount}</Text>
        </View>
      )}
    </TouchableOpacity>
  );
});

// Compare function for React.memo
function areEqual(prevProps: Props, nextProps: Props) {
  return (
    prevProps.match.id === nextProps.match.id &&
    prevProps.match.unreadCount === nextProps.match.unreadCount &&
    prevProps.match.lastMessage?.content === nextProps.match.lastMessage?.content
  );
}

export default memo(MatchCard, areEqual);

const styles = StyleSheet.create({
  container: {
    flexDirection: 'row',
    padding: 16,
    height: 120,
    alignItems: 'center',
  },
  avatar: {
    width: 60,
    height: 60,
    borderRadius: 30,
  },
  info: {
    flex: 1,
    marginLeft: 12,
  },
  name: {
    fontSize: 16,
    fontWeight: '600',
  },
  lastMessage: {
    fontSize: 14,
    color: '#666',
    marginTop: 4,
  },
  badge: {
    backgroundColor: '#FF6B6B',
    borderRadius: 12,
    paddingHorizontal: 8,
    paddingVertical: 4,
  },
  badgeText: {
    color: 'white',
    fontSize: 12,
    fontWeight: '600',
  },
});
```

---

## Image Handling

### react-native-fast-image

```typescript
// src/components/common/OptimizedImage.tsx
import React from 'react';
import FastImage, { FastImageProps } from 'react-native-fast-image';

interface Props extends FastImageProps {
  uri: string;
  priority?: 'low' | 'normal' | 'high';
}

export default function OptimizedImage({ uri, priority = 'normal', ...props }: Props) {
  return (
    <FastImage
      source={{
        uri,
        priority: FastImage.priority[priority],
        cache: FastImage.cacheControl.immutable,
      }}
      {...props}
    />
  );
}

// Usage
<OptimizedImage
  uri="https://cdn.datingapp.com/photos/user-123/photo-1.jpg"
  style={{ width: 300, height: 400 }}
  resizeMode={FastImage.resizeMode.cover}
  priority="high"
/>
```

### Image Upload with Compression

```typescript
// src/services/imageService.ts
import ImagePicker from 'react-native-image-crop-picker';
import { manipulateAsync, SaveFormat } from 'expo-image-manipulator';
import { apiClient } from '../api/client';

export async function uploadProfilePhoto(): Promise<string> {
  // Pick image
  const image = await ImagePicker.openPicker({
    width: 1080,
    height: 1080,
    cropping: true,
    cropperCircleOverlay: false,
    compressImageQuality: 0.8,
    mediaType: 'photo',
  });

  // Compress image
  const compressedImage = await manipulateAsync(
    image.path,
    [{ resize: { width: 1080, height: 1080 } }],
    { compress: 0.8, format: SaveFormat.JPEG }
  );

  // Get upload URL
  const { data } = await apiClient.post('/media/upload-url');
  const { uploadUrl, photoId } = data;

  // Upload to S3
  const formData = new FormData();
  formData.append('photo', {
    uri: compressedImage.uri,
    type: 'image/jpeg',
    name: `photo-${photoId}.jpg`,
  } as any);

  await fetch(uploadUrl, {
    method: 'PUT',
    body: formData,
  });

  return photoId;
}
```

---

## WebSocket Integration

### WebSocket Service

```typescript
// src/services/websocketService.ts
import io, { Socket } from 'socket.io-client';
import { useAuthStore } from '../store/authStore';
import { useMessageStore } from '../store/messageStore';

class WebSocketService {
  private socket: Socket | null = null;

  connect() {
    const accessToken = useAuthStore.getState().accessToken;

    this.socket = io('wss://api.datingapp.com', {
      auth: { token: accessToken },
      transports: ['websocket'],
      reconnection: true,
      reconnectionDelay: 1000,
      reconnectionAttempts: 5,
    });

    this.setupListeners();
  }

  private setupListeners() {
    if (!this.socket) return;

    this.socket.on('connect', () => {
      console.log('WebSocket connected');
    });

    this.socket.on('disconnect', () => {
      console.log('WebSocket disconnected');
    });

    this.socket.on('message:new', (message) => {
      useMessageStore.getState().addMessage(message);
    });

    this.socket.on('message:delivered', (data) => {
      useMessageStore.getState().markDelivered(data.messageId);
    });

    this.socket.on('message:read', (data) => {
      useMessageStore.getState().markRead(data.messageId);
    });

    this.socket.on('user:typing', (data) => {
      useMessageStore.getState().setTyping(data.matchId, data.isTyping);
    });
  }

  sendMessage(matchId: string, content: string, tempId: string) {
    this.socket?.emit('message:send', {
      matchId,
      content,
      tempId,
    });
  }

  sendTypingIndicator(matchId: string, isTyping: boolean) {
    this.socket?.emit('message:typing', {
      matchId,
      isTyping,
    });
  }

  disconnect() {
    this.socket?.disconnect();
    this.socket = null;
  }
}

export const websocketService = new WebSocketService();
```

**Hook:**

```typescript
// src/hooks/useWebSocket.ts
import { useEffect } from 'react';
import { useAuthStore } from '../store/authStore';
import { websocketService } from '../services/websocketService';

export function useWebSocket() {
  const isAuthenticated = useAuthStore((state) => state.isAuthenticated);

  useEffect(() => {
    if (isAuthenticated) {
      websocketService.connect();

      return () => {
        websocketService.disconnect();
      };
    }
  }, [isAuthenticated]);
}
```

---

## Performance Optimization

### 1. Reduce Re-renders

```typescript
// Use React.memo
export default React.memo(MyComponent);

// Use useCallback for functions
const handlePress = useCallback(() => {
  // Handler logic
}, [dependencies]);

// Use useMemo for computed values
const sortedData = useMemo(
  () => data.sort((a, b) => a.timestamp - b.timestamp),
  [data]
);
```

### 2. Lazy Loading

```typescript
// Lazy load screens
const ProfileScreen = React.lazy(() => import('../screens/profile/ProfileScreen'));

// Use Suspense
<Suspense fallback={<LoadingSpinner />}>
  <ProfileScreen />
</Suspense>
```

### 3. Debounce Search

```typescript
import { debounce } from 'lodash';

const debouncedSearch = useMemo(
  () =>
    debounce((query: string) => {
      searchUsers(query);
    }, 300),
  []
);
```

### 4. Optimize Images

- Use WebP format
- Implement progressive loading
- Use CDN with caching headers
- Implement image placeholders

### 5. Bundle Size Optimization

```javascript
// metro.config.js
module.exports = {
  transformer: {
    minifierPath: 'metro-minify-terser',
    minifierConfig: {
      compress: {
        drop_console: true, // Remove console.log in production
      },
    },
  },
};
```

---

## Summary

This architecture provides:

✅ **Clean Code**: Well-organized, maintainable structure
✅ **Performance**: Optimized rendering and data fetching
✅ **Type Safety**: Full TypeScript support
✅ **Scalability**: Easy to add new features
✅ **Best Practices**: Industry-standard patterns

**Next**: See performance optimization guide for comprehensive tips.
