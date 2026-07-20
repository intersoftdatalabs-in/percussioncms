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

import React, { useEffect, useMemo, useState } from "react";
import { fetchSites } from "../../api/home/homeApi";
import type { SiteSummary } from "../../api/home/types";
import { message, MSG } from "../../i18n/message";
import { EmptyState } from "../components/EmptyState";
import {
  buttonStyle,
  cardGridStyle,
  cardStyle,
  errorStyle,
  listItemStyle,
  listStyle,
  toolbarStyle,
} from "../publishing.styles";
import { filterSitesByName, nextViewMode, siteKey } from "../siteListUtils";
import type { PublishSiteSummary, SiteListViewMode } from "../types";
import { SiteWorkspace } from "./SiteWorkspace";

export interface SitesSectionProps {
  /** Preselect site from deep link */
  initialSiteId?: string;
  initialServerId?: string;
}

function toPublishSite(s: SiteSummary): PublishSiteSummary {
  return {
    name: s.name,
    id: s.id,
    siteId: s.siteId ?? s.id,
  };
}

export function SitesSection({
  initialSiteId = "",
  initialServerId = "",
}: SitesSectionProps): React.ReactElement {
  const [sites, setSites] = useState<PublishSiteSummary[]>([]);
  const [filter, setFilter] = useState("");
  const [viewMode, setViewMode] = useState<SiteListViewMode>("card");
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [selectedKey, setSelectedKey] = useState(initialSiteId);

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    setError(null);
    fetchSites()
      .then((list) => {
        if (cancelled) {
          return;
        }
        setSites(list.map(toPublishSite));
      })
      .catch(() => {
        if (!cancelled) {
          setError(message(MSG.PUBLISH_ERROR));
        }
      })
      .finally(() => {
        if (!cancelled) {
          setLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const filtered = useMemo(
    () => filterSitesByName(sites, filter),
    [sites, filter],
  );

  const selectedSite = useMemo(
    () => sites.find((s) => siteKey(s) === selectedKey || s.name === selectedKey),
    [sites, selectedKey],
  );

  if (selectedSite) {
    return (
      <SiteWorkspace
        site={selectedSite}
        initialServerId={initialServerId}
        onBack={() => setSelectedKey("")}
      />
    );
  }

  return (
    <div data-testid="publish-section-sites">
      <div style={toolbarStyle}>
        <label>
          <span className="sr-only">{message(MSG.PUBLISH_FILTER_SITES)}</span>
          <input
            type="search"
            value={filter}
            onChange={(e) => setFilter(e.target.value)}
            placeholder={message(MSG.PUBLISH_FILTER_SITES)}
            aria-label={message(MSG.PUBLISH_FILTER_SITES)}
            style={{ padding: "6px 10px", minWidth: 200 }}
          />
        </label>
        <button
          type="button"
          style={buttonStyle}
          onClick={() => setViewMode(nextViewMode(viewMode))}
          aria-label={
            viewMode === "card"
              ? message(MSG.PUBLISH_LIST)
              : message(MSG.PUBLISH_CARD)
          }
        >
          {viewMode === "card"
            ? message(MSG.PUBLISH_LIST)
            : message(MSG.PUBLISH_CARD)}
        </button>
      </div>

      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p style={errorStyle} role="alert">
          {error}
        </p>
      )}
      {!loading && !error && filtered.length === 0 && (
        <EmptyState
          title={message(MSG.PUBLISH_EMPTY_SITES)}
          nextAction="Create or import a site, then return here to configure publish servers."
          testId="publish-empty-sites"
        />
      )}

      {!loading && viewMode === "card" && filtered.length > 0 && (
        <div style={cardGridStyle} role="list">
          {filtered.map((site) => (
            <button
              key={siteKey(site)}
              type="button"
              role="listitem"
              style={cardStyle}
              onClick={() => setSelectedKey(siteKey(site))}
            >
              <strong>{site.name}</strong>
            </button>
          ))}
        </div>
      )}

      {!loading && viewMode === "list" && filtered.length > 0 && (
        <ul style={listStyle}>
          {filtered.map((site) => (
            <li key={siteKey(site)} style={listItemStyle}>
              <button
                type="button"
                style={{ ...buttonStyle, flex: 1, textAlign: "left" }}
                onClick={() => setSelectedKey(siteKey(site))}
              >
                {site.name}
              </button>
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
