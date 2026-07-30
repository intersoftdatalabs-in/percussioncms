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

import React, { useState } from "react";
import {
  DEVELOPER_SECTIONS,
  normalizeDeveloperSection as normalizeFromAllowlist,
  type DeveloperSection,
} from "../app/deepLinks/allowlists";
import { ActionMenusPanel } from "./ActionMenusPanel";
import { CommunitiesPanel } from "./CommunitiesPanel";
import { ContentTypesPanel } from "./ContentTypesPanel";
import { DisplayFormatsPanel } from "./DisplayFormatsPanel";
import { ItemFiltersPanel } from "./ItemFiltersPanel";
import { KeywordsPanel } from "./KeywordsPanel";
import { LocalesPanel } from "./LocalesPanel";
import { DEV_MSG } from "./messages";
import { PipelinesPanel } from "./PipelinesPanel";
import { SharedFieldsPanel } from "./SharedFieldsPanel";
import { SystemDefPanel } from "./SystemDefPanel";
import { SlotsPanel } from "./SlotsPanel";
import { TemplatesPanel } from "./TemplatesPanel";

export type { DeveloperSection };

const SECTION_LABEL: Record<DeveloperSection, string> = {
  "content-types": DEV_MSG.TAB_CONTENT_TYPES,
  templates: DEV_MSG.TAB_TEMPLATES,
  slots: DEV_MSG.TAB_SLOTS,
  keywords: DEV_MSG.TAB_KEYWORDS,
  locales: DEV_MSG.TAB_LOCALES,
  "shared-fields": DEV_MSG.TAB_SHARED_FIELDS,
  "system-def": DEV_MSG.TAB_SYSTEM_DEF,
  "item-filters": DEV_MSG.TAB_ITEM_FILTERS,
  "display-formats": DEV_MSG.TAB_DISPLAY_FORMATS,
  "action-menus": DEV_MSG.TAB_ACTION_MENUS,
  communities: DEV_MSG.TAB_COMMUNITIES,
  pipelines: DEV_MSG.TAB_PIPELINES,
};

/** Shell default when raw section is missing/unknown (allowlist returns undefined). */
export function normalizeDeveloperSection(
  raw: string | null | undefined,
): DeveloperSection {
  return normalizeFromAllowlist(raw) ?? "content-types";
}

export interface DeveloperShellProps {
  initialSection?: DeveloperSection | string;
  /**
   * When true (SPA AppLayout), shell is under product chrome — tighter padding.
   */
  embedded?: boolean;
}

const tabStyle = (active: boolean): React.CSSProperties => ({
  padding: "10px 16px",
  border: "none",
  borderBottom: active ? "3px solid #007ea8" : "3px solid transparent",
  fontWeight: active ? 600 : 400,
  background: "transparent",
  cursor: "pointer",
  color: active ? "#007ea8" : "#2d3748",
});

/**
 * Developer module shell — P0 CMS design tools entry point.
 */
export const DeveloperShell: React.FC<DeveloperShellProps> = ({
  initialSection = "content-types",
  embedded = false,
}) => {
  const [active, setActive] = useState<DeveloperSection>(() =>
    normalizeDeveloperSection(initialSection),
  );

  return (
    <div
      className="perc-developer-shell"
      data-testid="perc-developer-shell"
      data-embedded={embedded ? "true" : "false"}
      style={{
        fontFamily: "var(--perc-font-family, sans-serif)",
        padding: embedded ? "8px 12px 20px" : "20px",
        maxWidth: "1200px",
        margin: "0 auto",
      }}
    >
      <header style={{ marginBottom: "12px" }}>
        <h1 style={{ marginBottom: "8px" }}>{DEV_MSG.TITLE}</h1>
        <p style={{ margin: 0, color: "#4a5568", maxWidth: "48rem" }}>
          {DEV_MSG.INTRO}
        </p>
      </header>

      <nav
        className="perc-tab-nav"
        role="tablist"
        aria-label="Developer sections"
        style={{
          display: "flex",
          flexWrap: "wrap",
          borderBottom: "1px solid #e2e8f0",
          marginBottom: "20px",
          gap: "4px",
        }}
      >
        {DEVELOPER_SECTIONS.map((section) => (
          <button
            key={section}
            type="button"
            role="tab"
            id={`tab-developer-${section}`}
            aria-selected={active === section}
            aria-controls={`panel-developer-${section}`}
            onClick={() => setActive(section)}
            style={tabStyle(active === section)}
            data-testid={`tab-developer-${section}`}
          >
            {SECTION_LABEL[section]}
          </button>
        ))}
      </nav>

      <div
        role="tabpanel"
        id={`panel-developer-${active}`}
        aria-labelledby={`tab-developer-${active}`}
        data-testid={`panel-developer-${active}`}
      >
        {active === "content-types" ? (
          <ContentTypesPanel />
        ) : active === "templates" ? (
          <TemplatesPanel />
        ) : active === "slots" ? (
          <SlotsPanel />
        ) : active === "keywords" ? (
          <KeywordsPanel />
        ) : active === "locales" ? (
          <LocalesPanel />
        ) : active === "shared-fields" ? (
          <SharedFieldsPanel />
        ) : active === "system-def" ? (
          <SystemDefPanel />
        ) : active === "item-filters" ? (
          <ItemFiltersPanel />
        ) : active === "display-formats" ? (
          <DisplayFormatsPanel />
        ) : active === "action-menus" ? (
          <ActionMenusPanel />
        ) : active === "communities" ? (
          <CommunitiesPanel />
        ) : (
          <PipelinesPanel />
        )}
      </div>
    </div>
  );
};
