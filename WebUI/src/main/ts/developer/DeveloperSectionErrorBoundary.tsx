/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from "react";
import { DEV_MSG } from "./messages";

export interface DeveloperSectionErrorBoundaryProps {
  /** Tab or panel label interpolated into SECTION_LOAD_FAILED ({0}). */
  label: string;
  children: React.ReactNode;
  /** Override the in-panel error test id (default developer-section-error). */
  testId?: string;
}

interface DeveloperSectionErrorBoundaryState {
  error: Error | null;
}

/**
 * Isolates a single Developer tab or detail panel. A Template (or other
 * section) render/load throw must not replace the whole Developer route with
 * RouteErrorBoundary ("Unable to load Developer") — #3377 / peer of
 * AdminSectionErrorBoundary (#3195).
 */
export class DeveloperSectionErrorBoundary extends React.Component<
  DeveloperSectionErrorBoundaryProps,
  DeveloperSectionErrorBoundaryState
> {
  state: DeveloperSectionErrorBoundaryState = { error: null };

  static getDerivedStateFromError(
    error: Error,
  ): DeveloperSectionErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo): void {
    console.error(
      `[PercModernUI] Developer section failed (${this.props.label})`,
      error,
      info.componentStack,
    );
  }

  render(): React.ReactNode {
    if (this.state.error) {
      const testId = this.props.testId ?? "developer-section-error";
      return (
        <div data-testid={testId} role="alert">
          <p>
            {DEV_MSG.SECTION_LOAD_FAILED.replace("{0}", this.props.label)}
          </p>
        </div>
      );
    }
    return this.props.children;
  }
}
