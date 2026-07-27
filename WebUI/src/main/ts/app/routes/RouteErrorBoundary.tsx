/*
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from "react";

interface Props {
  children: React.ReactNode;
  /** Short label for the failed screen (e.g. Admin tools) */
  label?: string;
}

interface State {
  error: Error | null;
}

/**
 * Catches lazy-load / render failures for SPA feature routes so the rest of
 * AppLayout stays usable (#1528 review).
 */
export class RouteErrorBoundary extends React.Component<Props, State> {
  state: State = { error: null };

  static getDerivedStateFromError(error: Error): State {
    return { error };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo): void {
    console.error(
      `[PercModernUI] Route load/render failed${this.props.label ? ` (${this.props.label})` : ""}`,
      error,
      info.componentStack,
    );
  }

  render(): React.ReactNode {
    if (this.state.error) {
      const label = this.props.label ?? "This screen";
      return (
        <div
          data-testid="route-error"
          role="alert"
          style={{
            padding: "1.5rem",
            maxWidth: 640,
            margin: "1rem auto",
            fontFamily: "system-ui, sans-serif",
          }}
        >
          <h2 style={{ marginTop: 0, fontSize: "1.15rem" }}>
            Unable to load {label}
          </h2>
          <p style={{ color: "#5b6478", lineHeight: 1.5 }}>
            Something went wrong loading this part of the application. Try
            reloading the page or return to Home from the navigation bar.
          </p>
        </div>
      );
    }
    return this.props.children;
  }
}

/**
 * ErrorBoundary + Suspense wrapper for lazy SPA shells.
 */
export function LazyRouteFrame({
  label,
  fallback,
  children,
}: {
  label: string;
  fallback: React.ReactNode;
  children: React.ReactNode;
}): React.ReactElement {
  return (
    <RouteErrorBoundary label={label}>
      <React.Suspense fallback={fallback}>{children}</React.Suspense>
    </RouteErrorBoundary>
  );
}
