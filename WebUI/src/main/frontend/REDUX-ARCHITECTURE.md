# Redux State Management Architecture - Phase 5

## Overview

Phase 5 introduces Redux Toolkit for centralized state management across the Percussion CMS modern UI. This document covers the Redux architecture, store configuration, slices, and usage patterns.

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────┐
│         React Application with Redux                    │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  Components (App, Navigation, Dashboard, etc.)          │
│      ↓                                                   │
│  useAppSelector(state) → read state                     │
│  useAppDispatch() → dispatch actions                    │
│      ↓                                                   │
│  Redux Store (store.ts)                                 │
│  ├─ auth: AuthState                                     │
│  ├─ ui: UIState                                         │
│  ├─ navigation: NavigationState                         │
│  └─ dashboard: DashboardState                           │
│      ↓                                                   │
│  Redux Middleware                                        │
│  ├─ Thunk (async actions)                               │
│  ├─ Logger (dev only)                                   │
│  └─ DevTools (inspection)                               │
│      ↓                                                   │
│  Reducers (slice definitions)                           │
│  ├─ authSlice.ts                                        │
│  ├─ uiSlice.ts                                          │
│  ├─ navigationSlice.ts                                  │
│  └─ dashboardSlice.ts                                   │
│                                                         │
└─────────────────────────────────────────────────────────┘
```

## Store Configuration

### File Structure

```
src/main/ts/redux/
├── index.ts              # Main exports (store, hooks, slices)
├── store.ts              # Redux store configuration
├── hooks.ts              # Custom React hooks for Redux
├── slices/
│   ├── authSlice.ts      # User authentication state
│   ├── uiSlice.ts        # UI state (theme, modals, notifications)
│   ├── navigationSlice.ts # Current page and menu state
│   └── dashboardSlice.ts  # Dashboard data and stats
```

### Store Setup

The Redux store is configured in `store.ts` with:

- **Redux Toolkit's `configureStore()`**: Simplifies setup and includes Thunk middleware by default
- **DevTools Integration**: Enabled in development for debugging and time-travel
- **Logger Middleware**: Logs actions and state changes in dev mode
- **Serialization Checks**: Configured to ignore Set objects used in navigation

```typescript
export const store = configureStore({
  reducer: {
    auth: authReducer,
    ui: uiReducer,
    navigation: navigationReducer,
    dashboard: dashboardReducer,
  },
  middleware: (getDefaultMiddleware) => {
    const middleware = getDefaultMiddleware({
      serializableCheck: {
        ignoredActions: ['navigation/toggleMenuItem'],
        ignoredPaths: ['navigation.expandedMenuItems'],
      },
    });
    if (process.env.NODE_ENV === 'development') {
      const { logger } = require('redux-logger');
      middleware.push(logger);
    }
    return middleware;
  },
  devTools: process.env.NODE_ENV === 'development',
});
```

## State Slices

### 1. Auth Slice (`authSlice.ts`)

**Purpose**: Manage user authentication, permissions, and session state

**State Structure**:

```typescript
interface AuthState {
  user: User | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  error: string | null;
  token: string | null;
}
```

**Available Actions**:
- `setUser(user)` - Set authenticated user
- `setToken(token)` - Store authentication token
- `updatePermissions(permissions)` - Update user permissions
- `updateRoles(roles)` - Update user roles
- `logout()` - Clear auth state
- `sessionExpired()` - Mark session as expired
- `setError(message)` - Set authentication error
- `clearError()` - Clear error

**Usage Example**:

```typescript
const { user, isAuthenticated, isLoading } = useAppSelector((state) => state.auth);
const dispatch = useAppDispatch();

// Login user
dispatch(setUser(userData));

// Logout
dispatch(logout());
```

### 2. UI Slice (`uiSlice.ts`)

**Purpose**: Manage global UI state (theme, notifications, modals, loading)

**State Structure**:

```typescript
interface UIState {
  isLoading: boolean;
  isSidebarOpen: boolean;
  theme: 'light' | 'dark';
  notifications: Notification[];
  modals: Record<string, Modal>;
  globalError: string | null;
}

interface Notification {
  id: string;
  type: 'success' | 'error' | 'warning' | 'info';
  message: string;
  duration?: number; // Auto-dismiss after ms
}

interface Modal {
  isOpen: boolean;
  title: string;
  content: string;
  type: 'confirm' | 'alert' | 'form';
  onConfirm?: () => void;
}
```

**Available Actions**:
- `setLoading(boolean)` - Set global loading state
- `toggleSidebar()` - Toggle sidebar visibility
- `setSidebarOpen(boolean)` - Set sidebar state explicitly
- `setTheme('light' | 'dark')` - Change theme (persists to localStorage)
- `addNotification(notification)` - Add toast notification
- `removeNotification(id)` - Remove notification by ID
- `clearNotifications()` - Remove all notifications
- `openModal(name, modal)` - Open modal by name
- `closeModal(name)` - Close modal by name
- `setGlobalError(error)` - Set global error message
- `clearGlobalError()` - Clear global error

**Usage Example**:

```typescript
const { notifications, theme, isSidebarOpen } = useAppSelector((state) => state.ui);
const { notify, setTheme, toggleSidebar } = useUI();

// Show success notification
notify({
  type: 'success',
  message: 'Changes saved successfully',
  duration: 3000,
});

// Toggle dark mode
setTheme(theme === 'light' ? 'dark' : 'light');
```

### 3. Navigation Slice (`navigationSlice.ts`)

**Purpose**: Manage current page, menu state, and breadcrumbs

**State Structure**:

```typescript
interface NavigationState {
  currentPath: string;
  currentPage: string;
  menuItems: NavItem[];
  expandedMenuItems: Set<string>;
  breadcrumbs: BreadcrumbItem[];
  pageMetadata: PageMetadata;
  previousPage?: string;
}
```

**Available Actions**:
- `navigateToPage(path, page, metadata)` - Navigate to page and update breadcrumbs
- `setMenuItems(items)` - Set navigation menu structure
- `toggleMenuItem(id)` - Toggle menu item expansion
- `expandMenuItem(id)` - Expand menu item
- `collapseMenuItem(id)` - Collapse menu item
- `setBreadcrumbs(items)` - Set breadcrumb trail
- `addBreadcrumb(item)` - Add breadcrumb
- `removeBreadcrumb(path)` - Remove breadcrumb
- `goBack()` - Navigate to previous page

**Usage Example**:

```typescript
const { currentPage, menuItems, breadcrumbs } = useAppSelector((state) => state.navigation);
const { navigate } = useNavigation();

// Navigate to content page
navigate('/content', 'content', 'Content Management');

// Toggle menu item
dispatch(toggleMenuItem('admin'));
```

### 4. Dashboard Slice (`dashboardSlice.ts`)

**Purpose**: Manage dashboard data, statistics, and activity logs

**State Structure**:

```typescript
interface DashboardState {
  stats: DashboardStats | null;
  activityLog: ActivityLog[];
  isLoadingStats: boolean;
  isLoadingActivity: boolean;
  error: string | null;
  lastUpdated: number | null;
}
```

**Available Actions**:
- `setStatsLoading(boolean)` - Set loading state for stats
- `setActivityLoading(boolean)` - Set loading state for activity log
- `setStats(stats)` - Load dashboard statistics
- `setActivityLog(log)` - Load activity log entries
- `addActivityEntry(entry)` - Add activity log entry (max 50 recent)
- `clearActivityLog()` - Clear activity log
- `setError(error)` - Set error message
- `reset()` - Reset dashboard state

**Usage Example**:

```typescript
const { stats, activityLog, isLoadingStats } = useAppSelector((state) => state.dashboard);
const { loadStats } = useDashboard();

// Load dashboard stats
const stats = await api.get('/dashboard/stats');
loadStats(stats);
```

## Custom Hooks

### useAuth()

Convenience hook for authentication operations:

```typescript
const { user, isAuthenticated, isLoading, error } = useAuth();

const Auth = () => {
  if (isLoading) return <div>Loading...</div>;
  if (!isAuthenticated) return <div>Please log in</div>;
  return <div>Welcome, {user?.displayName}</div>;
};
```

### useUI()

Convenience hook for UI operations:

```typescript
const { theme, notifications, notify, setTheme } = useUI();

const NotificationCenter = () => {
  return (
    <div>
      {notifications.map((n) => (
        <div key={n.id} className={`notification-${n.type}`}>
          {n.message}
        </div>
      ))}
    </div>
  );
};
```

### useNavigation()

Convenience hook for navigation operations:

```typescript
const { currentPage, menuItems, navigate } = useNavigation();

const handleMenuClick = (path: string, label: string) => {
  navigate(path, path.substring(1), label);
};
```

### useDashboard()

Convenience hook for dashboard operations:

```typescript
const { stats, activityLog, isLoadingStats } = useDashboard();

useEffect(() => {
  if (!stats) {
    loadDashboardData();
  }
}, []);
```

## Integration Points

### Provider Setup (index.tsx)

```typescript
import { Provider } from 'react-redux';
import { store } from './redux';
import { App } from './components';

root.render(
  <React.StrictMode>
    <Provider store={store}>
      <App />
    </Provider>
  </React.StrictMode>
);
```

### Component Integration

```typescript
import { useAppSelector, useAppDispatch } from '@/redux';
import { setUser } from '@/redux';

const MyComponent = () => {
  const dispatch = useAppDispatch();
  const user = useAppSelector((state) => state.auth.user);

  const handleLogin = (userData) => {
    dispatch(setUser(userData));
  };

  return <div>User: {user?.username}</div>;
};
```

## Async Operations (Future: Phase 5+)

For async API calls, Redux Thunk middleware is pre-configured:

```typescript
// Create async thunk (example for Phase 5+)
import { createAsyncThunk } from '@reduxjs/toolkit';

export const fetchUser = createAsyncThunk(
  'auth/fetchUser',
  async (userId: string) => {
    const response = await api.get(`/users/${userId}`);
    return response.data;
  }
);

// Dispatch async thunk
dispatch(fetchUser('123'));
```

## Development Tools

### Redux DevTools

In development mode, Redux DevTools extension is enabled. Install the browser extension to:
- Inspect state changes
- Time-travel debug
- Replay actions
- Dispatch actions directly

### Redux Logger

In development mode, all actions and state changes are logged to the console.

### Typing

All hooks are fully typed with TypeScript:

```typescript
const { user }: { user: User | null } = useAppSelector((state) => state.auth);
const dispatch: AppDispatch = useAppDispatch();
```

## Serialization

Redux checks that all state is serializable. The following are configured as exceptions:

- `Set<string>` in `navigation.expandedMenuItems`
- Navigation reducer actions (toggleMenuItem, etc.)

Do not add non-serializable values to the store without configuring them in `configureStore()`.

## Performance Considerations

1. **Selector Memoization**: Use `useAppSelector` which handles memoization
2. **Avoid Creating Objects in Selectors**: Leads to unnecessary re-renders
3. **Batch Related State**: Group related data in slices to minimize state tree depth
4. **Lazy Load State**: Only load data when needed (dashboard stats, activity logs)

## Testing Redux

Example unit tests for Redux slices:

```typescript
import { configureStore } from '@reduxjs/toolkit';
import authReducer, { setUser, logout } from '@/redux/slices/authSlice';

describe('Auth Slice', () => {
  it('should handle setUser', () => {
    const store = configureStore({ reducer: { auth: authReducer } });
    const user = { id: '1', username: 'admin', ... };

    store.dispatch(setUser(user));

    expect(store.getState().auth.user).toEqual(user);
    expect(store.getState().auth.isAuthenticated).toBe(true);
  });

  it('should handle logout', () => {
    const store = configureStore({ reducer: { auth: authReducer } });
    // ... setup

    store.dispatch(logout());

    expect(store.getState().auth.user).toBeNull();
    expect(store.getState().auth.isAuthenticated).toBe(false);
  });
});
```

## Next Steps (Phase 5+)

1. **Async Thunks**: Implement `createAsyncThunk` for API calls
2. **Selectors**: Create reusable selectors with `createSelector` for derived state
3. **Persistence**: Add Redux persist middleware to save state to localStorage
4. **Middleware**: Add custom middleware for analytics, error tracking
5. **Testing**: Expand test coverage for all slices and async thunks
6. **Documentation**: Update component-level usage documentation

## References

- [Redux Toolkit Documentation](https://redux-toolkit.js.org/)
- [Redux Style Guide](https://redux.js.org/style-guide/style-guide)
- [Redux DevTools](https://github.com/reduxjs/redux-devtools)
- [React-Redux Hooks API](https://react-redux.js.org/api/hooks)

