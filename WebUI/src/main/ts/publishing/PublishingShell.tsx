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
}

const SECTIONS: { id: PublishSection; key: string }[] = [
  { id: "sites", key: MSG.PUBLISH_SECTION_SITES },
  { id: "status", key: MSG.PUBLISH_SECTION_STATUS },
  { id: "logs", key: MSG.PUBLISH_SECTION_LOGS },
  { id: "design", key: MSG.PUBLISH_SECTION_DESIGN },
  { id: "runtime", key: MSG.PUBLISH_SECTION_RUNTIME },
];

function PublishingShellInner({
  section,
  siteId,
  serverId,
}: PublishingShellProps): React.ReactElement {
  const start = useMemo(() => mapSectionParam(section), [section]);
  const safeSiteId = useMemo(() => mapIdParam(siteId), [siteId]);
  const safeServerId = useMemo(() => mapIdParam(serverId), [serverId]);
  const [active, setActive] = useState<PublishSection>(start);
  const { confirmIfDirty } = useDirtyForm();

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
      </header>
      <nav style={navStyle} aria-label={message(MSG.PUBLISH_TITLE)}>
        {SECTIONS.map((s) => (
          <button
            key={s.id}
            type="button"
            style={navButtonStyle(active === s.id)}
            aria-current={active === s.id ? "page" : undefined}
            onClick={() => navigate(s.id)}
          >
            {message(s.key)}
          </button>
        ))}
      </nav>
      <main style={mainStyle}>
        {active === "sites" && (
          <SitesSection
            initialSiteId={safeSiteId}
            initialServerId={safeServerId}
          />
        )}
        {active === "status" && <StatusSection />}
        {active === "logs" && <LogsSection />}
        {active === "design" && <DesignSection />}
        {active === "runtime" && <RuntimeSection />}
      </main>
    </div>
  );
}

/**
 * Unified Publishing shell (Track B). Ops first; Design/Runtime sections
 * fill in later stories (US4/US5).
 */
export function PublishingShell(props: PublishingShellProps): React.ReactElement {
  return (
    <DirtyFormProvider>
      <PublishingShellInner {...props} />
    </DirtyFormProvider>
  );
}
