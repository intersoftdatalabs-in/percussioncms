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
import { emptyStyle, primaryButtonStyle } from "../publishing.styles";

export interface EmptyStateProps {
  title: string;
  nextAction?: string;
  actionLabel?: string;
  onAction?: () => void;
  testId?: string;
}

/** Consistent empty-state with optional next-action CTA (US7). */
export function EmptyState({
  title,
  nextAction,
  actionLabel,
  onAction,
  testId = "publish-empty-state",
}: EmptyStateProps): React.ReactElement {
  return (
    <div data-testid={testId} style={emptyStyle} role="status">
      <p style={{ margin: "0 0 8px", fontWeight: 600 }}>{title}</p>
      {nextAction && (
        <p style={{ margin: "0 0 12px", color: "#555" }}>{nextAction}</p>
      )}
      {actionLabel && onAction && (
        <button type="button" style={primaryButtonStyle} onClick={onAction}>
          {actionLabel}
        </button>
      )}
    </div>
  );
}
