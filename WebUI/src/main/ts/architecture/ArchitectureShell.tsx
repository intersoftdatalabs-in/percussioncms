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

import React from "react";
import { catalogColors } from "../developer/catalogStyles";
import { ARCH_MSG } from "./messages";

export interface ArchitectureShellProps {
  /**
   * Optional site name from SPA path {@code /architecture/:site} or deep-link
   * query. Tree browsing lands in Slice C; this shell only surfaces context.
   */
  initialSite?: string | null;
  /**
   * When true (SPA AppLayout), shell is under product chrome — tighter padding.
   */
  embedded?: boolean;
}

/**
 * Architecture / Navigation SPA shell (#3094 / parent #3092).
 *
 * <p>Slice B: product chrome + route only. Empty / in-progress state until
 * read-only nav tree (Slice C) and structure editing (D–E) land. Primary
 * top-nav entry is SPA {@code /architecture} — not legacy
 * {@code siteArchitecture.jsp}.</p>
 */
export const ArchitectureShell: React.FC<ArchitectureShellProps> = ({
  initialSite = null,
  embedded = false,
}) => {
  const site =
    initialSite != null && String(initialSite).trim().length > 0
      ? String(initialSite).trim()
      : null;

  const siteHint = site
    ? ARCH_MSG.SITE_HINT.replace("{0}", site)
    : ARCH_MSG.SITE_NONE;

  return (
    <div
      className="perc-architecture-shell"
      data-testid="perc-architecture-shell"
      data-embedded={embedded ? "true" : "false"}
      data-site={site ?? ""}
      style={{
        fontFamily: "var(--perc-font-family, sans-serif)",
        padding: embedded ? "8px 12px 20px" : "20px",
        maxWidth: "960px",
        margin: "0 auto",
      }}
    >
      <header style={{ marginBottom: "16px" }}>
        <h1
          style={{ marginBottom: "8px" }}
          data-testid="architecture-shell-title"
        >
          {ARCH_MSG.TITLE}
        </h1>
        <p
          style={{ margin: 0, color: catalogColors.muted, maxWidth: "48rem" }}
          data-testid="architecture-shell-intro"
        >
          {ARCH_MSG.INTRO}
        </p>
      </header>

      <section
        aria-labelledby="architecture-empty-title"
        data-testid="architecture-empty-state"
        style={{
          border: `1px solid ${catalogColors.headerBorder}`,
          borderRadius: "8px",
          padding: "1.25rem 1.5rem",
          background: "#f8fafc",
        }}
      >
        <h2
          id="architecture-empty-title"
          style={{
            marginTop: 0,
            marginBottom: "0.5rem",
            fontSize: "1.1rem",
            color: "#1a202c",
          }}
          data-testid="architecture-empty-title"
        >
          {ARCH_MSG.EMPTY_TITLE}
        </h2>
        <p
          style={{
            margin: "0 0 0.75rem",
            color: catalogColors.muted,
            lineHeight: 1.5,
          }}
          data-testid="architecture-empty-body"
        >
          {ARCH_MSG.EMPTY_BODY}
        </p>
        <p
          style={{ margin: 0, color: catalogColors.empty, fontSize: "0.95rem" }}
          data-testid="architecture-site-hint"
        >
          {siteHint}
        </p>
      </section>
    </div>
  );
};

export default ArchitectureShell;
