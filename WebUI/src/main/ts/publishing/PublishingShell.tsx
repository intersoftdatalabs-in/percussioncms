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

import React, { useMemo, useState } from "react";
import { message, MSG } from "../i18n/message";
import { mapIdParam, mapSectionParam } from "./deepLinkMap";
import { DirtyFormProvider, useDirtyForm } from "./dirtyFormContext";
import {
  headerStyle,
  mainStyle,
  navButtonStyle,
  navStyle,
  shellStyle,
} from "./publishing.styles";
import { DesignSection } from "./sections/DesignSection";
import { LogsSection } from "./sections/LogsSection";
import { RuntimeSection } from "./sections/RuntimeSection";
import { SitesSection } from "./sections/SitesSection";
import { StatusSection } from "./sections/StatusSection";
import type { PublishSection } from "./types";

export interface PublishingShellProps {
  /** Query section=sites|status|logs|design|runtime */
  section?: string;
  /** Preselect site when opening Sites */
  siteId?: string;
  /** Preselect server in site workspace */
  serverId?: string;
  /**
   * When false, hide Design section (role-aware progressive disclosure).
   * Default true — server does not yet expose a dedicated design role to the shell.
   */
  showDesign?: boolean;
  /**
   * When true (SPA AppLayout), shell is already under product chrome.
   * Reserved for future chrome tweaks; currently no outer BrandBar.
   */
  embedded?: boolean;
}

/** Ops-first sections; design/runtime are secondary (US7 progressive disclosure). */
export const OPS_SECTIONS: { id: PublishSection; key: string }[] = [
  { id: "sites", key: MSG.PUBLISH_SECTION_SITES },
  { id: "status", key: MSG.PUBLISH_SECTION_STATUS },
  { id: "logs", key: MSG.PUBLISH_SECTION_LOGS },
];

export const ADVANCED_SECTIONS: { id: PublishSection; key: string }[] = [
  { id: "design", key: MSG.PUBLISH_SECTION_DESIGN },
  { id: "runtime", key: MSG.PUBLISH_SECTION_RUNTIME },
];

/** Default landing section when no query param (ops first). */
export function defaultLandingSection(): PublishSection {
  return "sites";
}

function PublishingShellInner({
  section,
  siteId,
  serverId,
  showDesign = true,
}: PublishingShellProps): React.ReactElement {
  const start = useMemo(() => {
    const mapped = mapSectionParam(section);
    if (mapped === "design" && !showDesign) {
      return defaultLandingSection();
    }
    return section == null || section === "" ? defaultLandingSection() : mapped;
  }, [section, showDesign]);
  const safeSiteId = useMemo(() => mapIdParam(siteId), [siteId]);
  const safeServerId = useMemo(() => mapIdParam(serverId), [serverId]);
  const [active, setActive] = useState<PublishSection>(start);
  const { confirmIfDirty } = useDirtyForm();

  const sections = useMemo(() => {
    const advanced = showDesign
      ? ADVANCED_SECTIONS
      : ADVANCED_SECTIONS.filter((s) => s.id !== "design");
    return [...OPS_SECTIONS, ...advanced];
  }, [showDesign]);

  function navigate(next: PublishSection): void {
    if (next === active) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setActive(next);
  }

  return (
    <div style={shellStyle} data-testid="publishing-shell">
      <header style={headerStyle}>
        <h1 style={{ margin: 0, fontSize: "1.25rem" }}>
          {message(MSG.PUBLISH_TITLE)}
        </h1>
        <p style={{ margin: "4px 0 0", fontSize: "0.85rem", color: "#555" }}>
          Sites &amp; servers first — Design and Runtime available when needed.
        </p>
      </header>
      <nav
        style={navStyle}
        aria-label={message(MSG.PUBLISH_TITLE)}
        role="navigation"
      >
        {sections.map((s) => (
          <button
            key={s.id}
            type="button"
            style={navButtonStyle(active === s.id)}
            aria-current={active === s.id ? "page" : undefined}
            aria-label={message(s.key)}
            onClick={() => navigate(s.id)}
          >
            {message(s.key)}
          </button>
        ))}
      </nav>
      <main style={mainStyle} id="perc-publishing-main" tabIndex={-1}>
        {active === "sites" && (
          <SitesSection
            initialSiteId={safeSiteId}
            initialServerId={safeServerId}
          />
        )}
        {active === "status" && <StatusSection />}
        {active === "logs" && <LogsSection />}
        {active === "design" && showDesign && <DesignSection />}
        {active === "runtime" && <RuntimeSection />}
      </main>
    </div>
  );
}

/**
 * Unified Publishing shell (Track B). Progressive disclosure: ops sections
 * first; Design/Runtime secondary.
 */
export function PublishingShell(props: PublishingShellProps): React.ReactElement {
  return (
    <DirtyFormProvider>
      <PublishingShellInner {...props} />
    </DirtyFormProvider>
  );
}
