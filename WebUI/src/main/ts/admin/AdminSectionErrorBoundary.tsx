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
import { message } from "../i18n/message";
import { ADMIN_MSG } from "./messages";

export interface AdminSectionErrorBoundaryProps {
  /** Tab or tool label interpolated into SECTION_LOAD_FAILED ({0}). */
  label: string;
  children: React.ReactNode;
}

interface AdminSectionErrorBoundaryState {
  error: Error | null;
}

/**
 * Isolates a single Admin tab or System Tool. A TypeError in Security Audit
 * Log (or another tool) must not replace the whole Admin route with
 * RouteErrorBoundary (#3195). Peer of the inline boundary in PR #3229 —
 * Administration tabs stay there; this module is the shared extract for Tools.
 */
export class AdminSectionErrorBoundary extends React.Component<
  AdminSectionErrorBoundaryProps,
  AdminSectionErrorBoundaryState
> {
  state: AdminSectionErrorBoundaryState = { error: null };

  static getDerivedStateFromError(
    error: Error,
  ): AdminSectionErrorBoundaryState {
    return { error };
  }

  componentDidCatch(error: Error, info: React.ErrorInfo): void {
    console.error(
      `[PercModernUI] Admin section failed (${this.props.label})`,
      error,
      info.componentStack,
    );
  }

  render(): React.ReactNode {
    if (this.state.error) {
      return (
        <div data-testid="admin-section-error" role="alert">
          <p>
            {message(ADMIN_MSG.SECTION_LOAD_FAILED).replace(
              "{0}",
              this.props.label,
            )}
          </p>
        </div>
      );
    }
    return this.props.children;
  }
}
