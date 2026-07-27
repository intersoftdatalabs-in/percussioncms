# React Router Implementation - Phase 6

## Overview

Phase 6 introduces React Router v6 for client-side Single Page Application (SPA) routing with lazy-loaded page components for automatic code splitting.

## Architecture

### Route Configuration

Routes are configured centrally in `router/routes.tsx` with the following structure:

```
/ (root)
├── / (redirect to /dashboard)
├── /dashboard (Dashboard page)
├── /content (Content Management)
├── /users (User Management)
├── /settings (System Settings)
└── * (catch-all → Dashboard)
```

Each route is wrapped with `<RootLayout>` which provides the header, navigation, and footer. Routes render inside the Layout using React Router's `<Outlet>`.

### Lazy Loading and Code Splitting

Page components are lazy-loaded using `React.lazy()` and `Suspense`:

```typescript
// In router/routes.tsx
const Dashboard = React.lazy(() =>
  import('@/pages/Dashboard').then((m) => ({ default: m.Dashboard }))
);
```

This creates separate chunks for each page:
- `Dashboard-*.js` - Dashboard page chunk
- `Content-*.js` - Content Management chunk
- `Users-*.js` - User Management chunk
- `Settings-*.js` - Settings chunk
- `index-*.js` - Main app + Redux + Router (shared)

**Bundle sizes in WAR**:

```
index-BO2YfwYL.js          256 KB  (main bundle with React, Redux, Router)
Dashboard-CnGr7VGW.js      1.3 KB  (lazy chunk)
Content-3MQVgVIk.js        0.9 KB  (lazy chunk)
Users-BqKgrBN2.js          0.9 KB  (lazy chunk)
Settings-DaWMxLpW.js       1.5 KB  (lazy chunk)
```

Each lazy chunk loads only when the user navigates to that page.

## File Structure

```
src/main/ts/
├── router/
│   ├── index.ts           # Exports router, routes, metadata
│   └── routes.tsx         # Route definitions with lazy loading
├── pages/
│   ├── index.ts           # Page component exports
│   ├── Dashboard.tsx       # Dashboard page (main/stats)
│   ├── Content.tsx         # Content management page (placeholder)
│   ├── Users.tsx           # User management page (placeholder)
│   └── Settings.tsx        # Settings/preferences page
├── components/
│   ├── App.tsx            # Updated to use RouterProvider
│   ├── Navigation.tsx      # Updated to use useNavigate hook
│   ├── Layout.tsx          # Split into RootLayout in routes.tsx
│   └── ...
└── ...
```

## Components Updated for Router

### App.tsx

```typescript
import { RouterProvider } from 'react-router';
import { router } from '../router';

export const App: React.FC = () => {
  return (
    <div className={`app theme-${theme}`}>
      <RouterProvider router={router} />
    </div>
  );
};
```

Instead of manually managing Layout, Navigation, and pages, the Router handles the tree based on the route configuration.

### Navigation.tsx

Updated to use React Router's `useNavigate` hook:

```typescript
import { useNavigate } from 'react-router';

export const Navigation: React.FC<{ items: NavItem[] }> = ({ items }) => {
  const navigate = useNavigate();

  const handleNavigation = (path: string) => {
    navigate(path);
  };

  return (
    <nav>
      {items.map(item => (
        <button onClick={() => handleNavigation(item.path)}>
          {item.label}
        </button>
      ))}
    </nav>
  );
};
```

### Layout.tsx

Layout is now wrapped in `RootLayout` component within the router config to provide the `<Outlet>` for page rendering:

```typescript
const RootLayout = () => (
  <Layout>
    <Outlet />
  </Layout>
);
```

## Navigation Flow

1. **User clicks menu item** → Button click handler
2. **Navigation component calls `navigate(path)`** → Router updates URL
3. **Route matches** → Component inside route renders (if lazy, loads chunk)
4. **Page component mounts** → Updates Redux navigation state
5. **Outlet in Layout** → Displays the matching page

## Page Components

### Dashboard (`pages/Dashboard.tsx`)

Main dashboard with statistics, activity log, and quick actions.

**Updates Redux on mount**:

```typescript
useEffect(() => {
  dispatch(navigateToPage({
    path: '/dashboard',
    page: 'dashboard',
    metadata: { title: 'Dashboard' }
  }));
}, [dispatch]);
```

### Content (`pages/Content.tsx`)

Content management page (placeholder for Phase 6+).

**Lazy-loaded** - Only downloaded when user navigates to `/content`

Features planned:
- Content browser and search
- Rich text editor
- Asset management
- Workflow and publishing

### Users (`pages/Users.tsx`)

User management page (placeholder for Phase 6+).

**Lazy-loaded** - Only downloaded when user navigates to `/users`

Features planned:
- User list and search
- User creation and editing
- Role assignment
- Permission management

### Settings (`pages/Settings.tsx`)

System settings and preferences page with theme toggle demo.

**Lazy-loaded** - Only downloaded when user navigates to `/settings`

**Theme toggle demo**:

```typescript
const { theme, setTheme } = useUI();

const toggleTheme = () => {
  dispatch(setReduxTheme(theme === 'light' ? 'dark' : 'light'));
};

// Demonstrates Redux integration with routing
```

Features planned:
- General system settings
- Email configuration
- Security settings
- Theme and appearance
- Plugin management

## Route Metadata

Route metadata maps route IDs to display information for breadcrumbs and page titles:

```typescript
export const routeMetadata: Record<string, PageMetadata> = {
  dashboard: {
    title: 'Dashboard',
    description: 'Main dashboard view',
    icon: 'dashboard',
  },
  content: {
    title: 'Content Management',
    description: 'Browse and manage content items',
    icon: 'folder',
  },
  // ...
};
```

## Styling

### Page Loader

While lazy chunks load, a loading indicator displays:

```css
.page-loader {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 400px;
  padding: 2rem;
}
```

### Theme Support

App component applies theme class:

```tsx
<div className={`app theme-${theme}`}>
  <RouterProvider router={router} />
</div>
```

CSS handles dark/light mode switching across all pages.

### Page Container

All pages use consistent `.page-container` styling for padding and typography:

```css
.page-container {
  padding: 2rem;
}

.page-container h1 {
  font-size: 2rem;
  color: #333;
}
```

## Error Handling

### 404 Routes

All unmatched routes (`*`) redirect to Dashboard:

```typescript
{
  path: '*',
  element: <PageWrapper><Dashboard /></PageWrapper>,
  id: 'not-found',
}
```

Phase 6+: Can add proper 404 page component.

### Fallback UI

`PageLoader` shows while chunks load. Can be customized per page in Phase 6+:

```typescript
const PageWrapper: React.FC = ({ children }) => (
  <Suspense fallback={<PageLoader />}>
    {children}
  </Suspense>
);
```

## Redux Integration

Routing coordinates with Redux navigation state:

1. **Page component mounts**
2. **Dispatches `navigateToPage`** action in useEffect
3. **Redux updates** navigation state (breadcrumbs, currentPage, etc.)
4. **Components can access** current page from Redux:

```typescript
const { currentPage } = useAppSelector((state) => state.navigation);
```

## Performance Optimizations

### Code Splitting

- Main bundle: React + Redux + Router (~250 KB gzipped)
- Page chunks: ~1-2 KB each (loaded on demand)
- Savings: Users only download code they need

### Lazy Loading

- Pages load only when accessed
- Initial page load faster
- Reduces perceived latency

### Suspense Boundaries

- Clean loading states between page transitions
- Can add skeleton screens in Phase 6+
- Graceful fallback UI

## URL Configuration

Router is configured with basename for deployment in a sub-path:

```typescript
export const router = createBrowserRouter(routes, {
  basename: '/cm/modern',
});
```

Adjust `basename` based on your deployment structure. Affects all generated URLs:
- Visited: `http://app/cm/modern/dashboard`
- Actual file: `/cm/modern/assets/index.js`

## Browser Compatibility

React Router v6+ requires modern browser support:
- Chrome 51+
- Firefox 54+
- Safari 10+
- Edge 15+

## Next Steps (Phase 6+)

1. **Route Guards**: Implement permission-based route protection
2. **Bread crumbs**: Auto-generate from current route
3. **Scroll to Top**: Reset scroll on page navigation
4. **Loading States**: Add skeleton screens for better UX
5. **Error Boundaries**: Catch and display page errors
6. **Route Transitions**: Add page transition animations
7. **Query Params**: Support filter/search persistence in URLs

## Testing Routes

To test routing locally:

```bash
# Build frontend
cd WebUI/src/main/frontend
npm run build

# Test with Maven build
cd /path/to/percussioncms
./mvn-env.sh -f WebUI/pom.xml clean package

# Or run dev server
cd WebUI/src/main/frontend
npm run dev
```

Navigate through menu to test:
- `/` → Dashboard
- `/content` → Content Management
- `/users` → User Management
- `/settings` → Settings
- Unknown path → Redirects to Dashboard

Monitor Network tab to see lazy chunks loading on navigation.

## References

- [React Router Documentation](https://reactrouter.com/en/main)
- [Lazy Loading Components](https://react.dev/reference/react/lazy)
- [Code Splitting Best Practices](https://webpack.js.org/guides/code-splitting/)
- [Route-Based Code Splitting](https://reactrouter.com/en/main/start/concepts#route-lazy)

