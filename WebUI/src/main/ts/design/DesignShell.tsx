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

import React, { useState } from "react";
import {
  DESIGN_SECTIONS,
  normalizeDesignSection as normalizeFromAllowlist,
  type DesignSection,
} from "../app/deepLinks/allowlists";
import { catalogColors } from "../developer/catalogStyles";
import { DESIGN_MSG } from "./messages";
import { TemplateLibraryPanel } from "./TemplateLibraryPanel";

export type { DesignSection };

const SECTION_LABEL: Record<DesignSection, string> = {
  templates: DESIGN_MSG.TAB_TEMPLATES,
};

/** Shell default when raw section is missing/unknown. */
export function normalizeDesignSection(
  raw: string | null | undefined,
): DesignSection {
  return normalizeFromAllowlist(raw) ?? "templates";
}

export interface DesignShellProps {
  initialSection?: DesignSection | string;
  /**
   * When true (SPA AppLayout), shell is under product chrome — tighter padding.
   */
  embedded?: boolean;
}

const tabStyle = (active: boolean): React.CSSProperties => ({
  padding: "10px 16px",
  border: "none",
  borderBottom: active
    ? `3px solid ${catalogColors.accent}`
    : "3px solid transparent",
  fontWeight: active ? 600 : 400,
  background: "transparent",
  cursor: "pointer",
  color: active ? catalogColors.accent : catalogColors.text,
});

/**
 * Design SPA shell — Phase 4 template library list entry (#2808 / parent #2631).
 * Later slices add source editor and assembler/slots under this surface.
 */
export const DesignShell: React.FC<DesignShellProps> = ({
  initialSection = "templates",
  embedded = false,
}) => {
  const [active, setActive] = useState<DesignSection>(() =>
    normalizeDesignSection(initialSection),
  );

  return (
    <div
      className="perc-design-shell"
      data-testid="perc-design-shell"
      data-embedded={embedded ? "true" : "false"}
      style={{
        fontFamily: "var(--perc-font-family, sans-serif)",
        padding: embedded ? "8px 12px 20px" : "20px",
        maxWidth: "1200px",
        margin: "0 auto",
      }}
    >
      <header style={{ marginBottom: "12px" }}>
        <h1 style={{ marginBottom: "8px" }} data-testid="design-shell-title">
          {DESIGN_MSG.TITLE}
        </h1>
        <p
          style={{ margin: 0, color: catalogColors.muted, maxWidth: "48rem" }}
          data-testid="design-shell-intro"
        >
          {DESIGN_MSG.INTRO}
        </p>
      </header>

      <nav
        className="perc-tab-nav"
        role="tablist"
        aria-label="Design sections"
        style={{
          display: "flex",
          flexWrap: "wrap",
          borderBottom: `1px solid ${catalogColors.headerBorder}`,
          marginBottom: "20px",
          gap: "4px",
        }}
      >
        {DESIGN_SECTIONS.map((section) => (
          <button
            key={section}
            type="button"
            role="tab"
            id={`tab-design-${section}`}
            aria-selected={active === section}
            aria-controls={`panel-design-${section}`}
            onClick={() => setActive(section)}
            style={tabStyle(active === section)}
            data-testid={`tab-design-${section}`}
          >
            {SECTION_LABEL[section]}
          </button>
        ))}
      </nav>

      <div
        role="tabpanel"
        id={`panel-design-${active}`}
        aria-labelledby={`tab-design-${active}`}
        data-testid={`panel-design-${active}`}
      >
        {active === "templates" ? <TemplateLibraryPanel /> : null}
      </div>
    </div>
  );
};
