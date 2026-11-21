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

import React, { useEffect, useMemo, useRef, useState } from "react";
import { message, MSG } from "../i18n/message";
import { mapIdParam, mapJobIdParam, mapRequestIdParam, mapSectionParam } from "./deepLinkMap";
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
  /** Item id for history, schedule dates, and site-workspace takedown. */
  itemId?: string;
  /**
   * Publishing job to preselect when opening Logs (`jobId` deep-link param).
   * Sanitized with `mapJobIdParam`; unknown ids degrade to the Logs list.
   */
  jobId?: string;
  /**
   * Demand-publish request to surface when opening Status (`requestId`
   * deep-link param). Sanitized with `mapRequestIdParam`; unknown ids
   * degrade to the Status list.
   */
  requestId?: string;
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

/**
 * Resolve the next section-nav position for a keyboard movement key (#4531).
 * Returns `null` when the key is not a movement key. Left/Right wrap at
 * both ends so keyboard operators can cycle the section list; Home/End
 * jump to the first/last section.
 */
export function sectionNavMoveTarget(
  key: string,
  index: number,
  count: number,
): number | null {
  if (count <= 0 || index < 0) {
    return null;
  }
  switch (key) {
    case "ArrowRight":
      return (index + 1) % count;
    case "ArrowLeft":
      return (index - 1 + count) % count;
    case "Home":
      return 0;
    case "End":
      return count - 1;
    default:
      return null;
  }
}

function PublishingShellInner({
  section,
  siteId,
  serverId,
  itemId,
  jobId,
  requestId,
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
  const safeJobId = useMemo(() => mapJobIdParam(jobId), [jobId]);
  const safeRequestId = useMemo(() => mapRequestIdParam(requestId), [requestId]);
  const [historyItemId, setHistoryItemId] = useState(() => mapIdParam(itemId));
  const [active, setActive] = useState<PublishSection>(start);
  const [statusJobFocus, setStatusJobFocus] = useState<{
    id: string;
    token: number;
  } | null>(null);
  const { confirmIfDirty } = useDirtyForm();
  /**
   * Section-nav buttons in render order (#4531). Arrow-key traversal
   * focuses through this map instead of DOM queries so focus never lands
   * on a stale node after the section list changes (showDesign toggle).
   */
  const navRefs = useRef(new Map<PublishSection, HTMLButtonElement | null>());

  useEffect(() => {
    setHistoryItemId(mapIdParam(itemId));
  }, [itemId]);

  const sections = useMemo(() => {
    const advanced = showDesign
      ? ADVANCED_SECTIONS
      : ADVANCED_SECTIONS.filter((s) => s.id !== "design");
    return [...OPS_SECTIONS, ...advanced];
  }, [showDesign]);

  function navigate(next: PublishSection): boolean {
    if (next === active) {
      return true;
    }
    if (!confirmIfDirty()) {
      return false;
    }
    setActive(next);
    return true;
  }

  /**
   * Section-nav keyboard operation (#4531): ArrowLeft/ArrowRight move
   * across the sections with wrap, Home/End jump to the first/last
   * section. Movement uses automatic activation — the newly focused
   * section opens — so keyboard and pointer operators land on the same
   * view. A blocked dirty-form confirm keeps focus where it was.
   */
  function handleNavKeyDown(e: React.KeyboardEvent, id: PublishSection): void {
    const index = sections.findIndex((s) => s.id === id);
    const target = sectionNavMoveTarget(e.key, index, sections.length);
    if (target === null) {
      return;
    }
    e.preventDefault();
    const next = sections[target];
    if (!next) {
      return;
    }
    if (navigate(next.id)) {
      navRefs.current.get(next.id)?.focus();
    }
  }

  function openRunningJob(jobId: string): void {
    const id = jobId.trim();
    if (id === "") {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setStatusJobFocus({ id, token: Date.now() });
    setActive("status");
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
            ref={(el) => {
              if (el) {
                navRefs.current.set(s.id, el);
              } else {
                navRefs.current.delete(s.id);
              }
            }}
            style={navButtonStyle(active === s.id)}
            aria-current={active === s.id ? "page" : undefined}
            aria-label={message(s.key)}
            data-testid={`publish-nav-${s.id}`}
            tabIndex={active === s.id ? 0 : -1}
            onClick={() => navigate(s.id)}
            onKeyDown={(e) => handleNavKeyDown(e, s.id)}
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
            itemId={historyItemId}
            onItemIdChange={setHistoryItemId}
          />
        )}
        {active === "status" && (
          <StatusSection
            itemId={historyItemId}
            onItemIdChange={setHistoryItemId}
            onOpenSection={navigate}
            focusJob={statusJobFocus}
            onFocusJobClear={() => setStatusJobFocus(null)}
            initialRequestId={safeRequestId || undefined}
          />
        )}
        {active === "logs" && (
          <LogsSection
            itemId={historyItemId}
            onItemIdChange={setHistoryItemId}
            onOpenSection={navigate}
            initialJobId={safeJobId || undefined}
          />
        )}
        {active === "design" && showDesign && <DesignSection />}
        {active === "runtime" && (
          <RuntimeSection onOpenRunningJob={openRunningJob} />
        )}
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
