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
  DEVELOPER_SECTIONS,
  normalizeDeveloperSection as normalizeFromAllowlist,
  type DeveloperSection,
} from "../app/deepLinks/allowlists";
import { ActionMenusPanel } from "./ActionMenusPanel";
import { CommunitiesPanel } from "./CommunitiesPanel";
import { CommunityVisibilityNavigatorPanel } from "./CommunityVisibilityNavigatorPanel";
import { ContentTypesPanel } from "./ContentTypesPanel";
import { ControlsPanel } from "./ControlsPanel";
import { DeveloperPreferencesPanel } from "./DeveloperPreferencesPanel";
import { ExtensionsPanel } from "./ExtensionsPanel";
import { DisplayFormatsPanel } from "./DisplayFormatsPanel";
import { ItemFiltersPanel } from "./ItemFiltersPanel";
import { KeywordsPanel } from "./KeywordsPanel";
import { LocalesPanel } from "./LocalesPanel";
import { DEV_MSG } from "./messages";
import { PipelinesPanel } from "./PipelinesPanel";
import { RelationshipTypesPanel } from "./RelationshipTypesPanel";
import { SearchesPanel } from "./SearchesPanel";
import { ServerConfigsPanel } from "./ServerConfigsPanel";
import { SharedFieldsPanel } from "./SharedFieldsPanel";
import { SitesPanel } from "./SitesPanel";
import { SystemDefPanel } from "./SystemDefPanel";
import { SlotsPanel } from "./SlotsPanel";
import { TemplatesPanel } from "./TemplatesPanel";
import { ViewsPanel } from "./ViewsPanel";
import { WorkflowsPanel } from "./WorkflowsPanel";
import { catalogColors } from "./catalogStyles";
import { DeveloperRelatedLinks } from "./DeveloperRelatedLinks";
import { DeveloperSectionErrorBoundary } from "./DeveloperSectionErrorBoundary";


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
  searches: DEV_MSG.TAB_SEARCHES,
  views: DEV_MSG.TAB_VIEWS,
  extensions: DEV_MSG.TAB_EXTENSIONS,
  "relationship-types": DEV_MSG.TAB_RELATIONSHIP_TYPES,
  workflows: DEV_MSG.TAB_WORKFLOWS,
  "server-configs": DEV_MSG.TAB_SERVER_CONFIGS,
  "ce-controls": DEV_MSG.TAB_CE_CONTROLS,
  sites: DEV_MSG.TAB_SITES,
  communities: DEV_MSG.TAB_COMMUNITIES,
  "community-visibility": DEV_MSG.TAB_COMMUNITY_VISIBILITY,
  pipelines: DEV_MSG.TAB_PIPELINES,
  preferences: DEV_MSG.TAB_PREFERENCES,
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
  borderBottom: active ? `3px solid ${catalogColors.accent}` : "3px solid transparent",
  fontWeight: active ? 600 : 400,
  background: "transparent",
  cursor: "pointer",
  color: active ? catalogColors.accent : catalogColors.text,
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
        <p style={{ margin: 0, color: catalogColors.muted, maxWidth: "48rem" }}>
          {DEV_MSG.INTRO}
        </p>
        <DeveloperRelatedLinks />
      </header>

      <nav
        className="perc-tab-nav"
        role="tablist"
        aria-label="Developer sections"
        style={{
          display: "flex",
          flexWrap: "wrap",
          borderBottom: `1px solid ${catalogColors.headerBorder}`,
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
        <DeveloperSectionErrorBoundary
          key={active}
          label={SECTION_LABEL[active]}
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
          ) : active === "searches" ? (
            <SearchesPanel />
          ) : active === "views" ? (
            <ViewsPanel />
          ) : active === "extensions" ? (
            <ExtensionsPanel />
          ) : active === "relationship-types" ? (
            <RelationshipTypesPanel />
          ) : active === "workflows" ? (
            <WorkflowsPanel />
          ) : active === "server-configs" ? (
            <ServerConfigsPanel />
          ) : active === "ce-controls" ? (
            <ControlsPanel />
          ) : active === "sites" ? (
            <SitesPanel />
          ) : active === "communities" ? (
            <CommunitiesPanel />
          ) : active === "community-visibility" ? (
            <CommunityVisibilityNavigatorPanel />
          ) : active === "pipelines" ? (
            <PipelinesPanel />
          ) : (
            <DeveloperPreferencesPanel />
          )}
        </DeveloperSectionErrorBoundary>
      </div>
    </div>
  );
};
