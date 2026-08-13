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
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Residual JSP host for {@link FolderSecurityPanel} (#3268 / #2749).
 *
 * <p>The product ACL surface is Explorer chrome. This host exists so
 * {@code folderSecurityModern.jsp} can mount a stable folder-security-*
 * testid (loading / panel / error / no-access / no-folder) without
 * miller-column Finder chrome. It does not resolve session identities
 * (see #3253) — the caller supplies {@code currentUserIdentities}.</p>
 */

import React from "react";
import { message } from "../i18n/message";
import {
  FolderSecurityPanel,
  type FolderSecurityPanelProps,
} from "./FolderSecurityPanel";
import { EXPLORER_MSG } from "./messages";

export interface FolderSecurityHostProps
  extends Omit<FolderSecurityPanelProps, "folderId" | "currentUserIdentities"> {
  /** Folder id from the host query string; empty shows the no-folder hint. */
  folderId?: string | null;
  /**
   * Identities the current user holds. The host does not look up the
   * session (#3253); residual JSP pilots pass a fixed Admin list.
   */
  currentUserIdentities?: ReadonlyArray<string>;
}

interface BoundaryState {
  errorMessage: string | null;
}

class FolderSecurityMountBoundary extends React.Component<
  { children: React.ReactNode },
  BoundaryState
> {
  constructor(props: { children: React.ReactNode }) {
    super(props);
    this.state = { errorMessage: null };
  }

  static getDerivedStateFromError(error: unknown): BoundaryState {
    const errorMessage =
      error instanceof Error ? error.message : message(EXPLORER_MSG.ERROR_GENERIC);
    return { errorMessage };
  }

  override componentDidCatch(error: unknown): void {
    console.error("[FolderSecurityHost] panel render failed", error);
  }

  override render(): React.ReactNode {
    if (this.state.errorMessage) {
      return (
        <div role="alert" data-testid="folder-security-error">
          <p>{this.state.errorMessage}</p>
        </div>
      );
    }
    return this.props.children;
  }
}

export function FolderSecurityHost(
  props: FolderSecurityHostProps,
): React.JSX.Element {
  const {
    folderId,
    currentUserIdentities = [],
    ...panelProps
  } = props;
  const id = typeof folderId === "string" ? folderId.trim() : "";
  if (!id) {
    return (
      <p data-testid="perc-folder-security-no-folder">
        No folderId supplied. Append ?folderId=&lt;id&gt; to this URL.
      </p>
    );
  }
  return (
    <FolderSecurityMountBoundary>
      <FolderSecurityPanel
        {...panelProps}
        folderId={id}
        currentUserIdentities={currentUserIdentities}
      />
    </FolderSecurityMountBoundary>
  );
}

export default FolderSecurityHost;
