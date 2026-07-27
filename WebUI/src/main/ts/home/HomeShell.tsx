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
import type { ContentListItem } from "../api/home/types";
import { message, MSG } from "../i18n/message";
import {
  mapInitialScreenToSection,
  type HomeSection,
} from "./deepLinkMap";
import {
  headerStyle,
  mainStyle,
  navButtonStyle,
  navStyle,
  shellStyle,
} from "./home.styles";
import { BrandBar, BrandFooter, ThemeProvider } from "../ui-themes/components";
import { BookmarksSection } from "./sections/BookmarksSection";
import { CreateSection } from "./sections/CreateSection";
import { LibrarySection } from "./sections/LibrarySection";
import { RecentSection } from "./sections/RecentSection";
import { SearchSection } from "./sections/SearchSection";

export interface HomeShellProps {
  /** Legacy initialScreen or modern section name */
  initialSection?: string;
  /** When true, show admin empty-state messaging for no sites */
  isAdmin?: boolean;
  /**
   * When true (SPA AppLayout), omit ThemeProvider / BrandBar / BrandFooter so
   * chrome is not doubled under the product shell.
   */
  embedded?: boolean;
  /**
   * Optional open-item handoff. Defaults to navigating parent to editor
   * when id/path are present.
   */
  onOpenItem?: (item: ContentListItem) => void;
}

const SECTIONS: { id: HomeSection; key: string }[] = [
  { id: "recent", key: MSG.SECTION_RECENT },
  { id: "bookmarks", key: MSG.SECTION_BOOKMARKS },
  { id: "library", key: MSG.SECTION_LIBRARY },
  { id: "search", key: MSG.SECTION_SEARCH },
  { id: "create", key: MSG.SECTION_CREATE },
];

/**
 * Open content using path-first navigation (classic openPathItem style).
 * Falls back to id when path is absent.
 */
function defaultOpenItem(item: ContentListItem): void {
  const path = item.path != null ? String(item.path).trim() : "";
  const id = item.id != null ? String(item.id) : "";
  if (path) {
    // Editor view accepts path; product navigation historically opened by path
    window.location.href = `/cm/app/?view=editor&path=${encodeURIComponent(path)}`;
    return;
  }
  if (id) {
    window.location.href = `/cm/app/?view=editor&id=${encodeURIComponent(id)}`;
  }
}

function HomeShellBody({
  initialSection,
  isAdmin = false,
  onOpenItem = defaultOpenItem,
}: Omit<HomeShellProps, "embedded">): React.ReactElement {
  const start = useMemo(
    () => mapInitialScreenToSection(initialSection),
    [initialSection],
  );
  const [section, setSection] = useState<HomeSection>(start);

  return (
    <>
      <header style={headerStyle}>
        <h1 style={{ margin: 0, fontSize: "1.25rem" }}>
          {message(MSG.HOME_TITLE)}
        </h1>
      </header>
      <nav style={navStyle} aria-label={message(MSG.HOME_TITLE)}>
        {SECTIONS.map((s) => (
          <button
            key={s.id}
            type="button"
            style={navButtonStyle(section === s.id)}
            aria-current={section === s.id ? "page" : undefined}
            onClick={() => setSection(s.id)}
          >
            {message(s.key)}
          </button>
        ))}
      </nav>
      <main style={mainStyle}>
        {section === "recent" && <RecentSection onOpenItem={onOpenItem} />}
        {section === "bookmarks" && (
          <BookmarksSection onOpenItem={onOpenItem} />
        )}
        {section === "library" && (
          <LibrarySection isAdmin={isAdmin} onOpenItem={onOpenItem} />
        )}
        {section === "search" && <SearchSection onOpenItem={onOpenItem} />}
        {section === "create" && <CreateSection />}
      </main>
    </>
  );
}

export function HomeShell({
  embedded = false,
  ...bodyProps
}: HomeShellProps): React.ReactElement {
  const inner = (
    <div style={shellStyle} data-testid="home-shell">
      <HomeShellBody {...bodyProps} />
    </div>
  );
  if (embedded) {
    return inner;
  }
  return (
    <ThemeProvider
      as="div"
      className="perc-home-theme-root"
      data-testid="home-theme-root"
    >
      <BrandBar />
      {inner}
      <BrandFooter />
    </ThemeProvider>
  );
}
