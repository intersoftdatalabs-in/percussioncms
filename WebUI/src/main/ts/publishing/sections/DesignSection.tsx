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

import React, { useCallback, useEffect, useState } from "react";
import { fetchSites } from "../../api/home/homeApi";
import {
  listContentLists,
  listEditionsBySite,
  type ContentListSummary,
  type EditionSummary,
} from "../../api/publishing/designApi";
import { message, MSG } from "../../i18n/message";
import { ContentListEditor } from "../design/ContentListEditor";
import { ContextsPanel } from "../design/ContextsPanel";
import { DeliveryTypesPanel } from "../design/DeliveryTypesPanel";
import { EditionEditor } from "../design/EditionEditor";
import { SiteDesignPanel } from "../design/SiteDesignPanel";
import {
  buttonStyle,
  emptyStyle,
  errorStyle,
  listItemStyle,
  listStyle,
  toolbarStyle,
} from "../publishing.styles";

type DesignNav =
  | "sites"
  | "editions"
  | "contentlists"
  | "contexts"
  | "delivery";

/**
 * Design section IA: editions, content lists, contexts/schemes, delivery types.
 */
export function DesignSection(): React.ReactElement {
  const [nav, setNav] = useState<DesignNav>("sites");
  const [sites, setSites] = useState<Array<{ name: string; id: string }>>([]);
  const [siteId, setSiteId] = useState("");
  const [editions, setEditions] = useState<EditionSummary[]>([]);
  const [contentLists, setContentLists] = useState<ContentListSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(false);
  const [editionEdit, setEditionEdit] = useState<EditionSummary | null | "new">(
    null,
  );
  const [clEdit, setClEdit] = useState<ContentListSummary | null | "new">(null);

  useEffect(() => {
    fetchSites()
      .then((list) => {
        const mapped = list.map((s) => ({
          name: s.name,
          id: String(s.siteId ?? s.id ?? s.name),
        }));
        setSites(mapped);
        if (mapped.length > 0) {
          setSiteId(mapped[0].id);
        }
      })
      .catch(() => setError(message(MSG.PUBLISH_ERROR)));
  }, []);

  const reloadEditions = useCallback(() => {
    if (!siteId) {
      return;
    }
    setLoading(true);
    setError(null);
    listEditionsBySite(siteId)
      .then(setEditions)
      .catch(() => setError(message(MSG.PUBLISH_ERROR)))
      .finally(() => setLoading(false));
  }, [siteId]);

  const reloadContentLists = useCallback(() => {
    setLoading(true);
    setError(null);
    listContentLists()
      .then(setContentLists)
      .catch(() => setError(message(MSG.PUBLISH_ERROR)))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    if (nav === "editions") {
      reloadEditions();
    }
  }, [nav, reloadEditions]);

  useEffect(() => {
    if (nav === "contentlists") {
      reloadContentLists();
    }
  }, [nav, reloadContentLists]);

  if (editionEdit !== null) {
    return (
      <div data-testid="publish-section-design">
        <EditionEditor
          siteId={siteId}
          edition={editionEdit === "new" ? null : editionEdit}
          sites={sites}
          onCancel={() => setEditionEdit(null)}
          onSaved={() => {
            setEditionEdit(null);
            reloadEditions();
          }}
        />
      </div>
    );
  }

  if (clEdit !== null) {
    return (
      <div data-testid="publish-section-design">
        <ContentListEditor
          contentList={clEdit === "new" ? null : clEdit}
          onCancel={() => setClEdit(null)}
          onSaved={() => {
            setClEdit(null);
            reloadContentLists();
          }}
        />
      </div>
    );
  }

  return (
    <div data-testid="publish-section-design">
      <div
        style={toolbarStyle}
        role="tablist"
        aria-label={message(MSG.PUBLISH_SECTION_DESIGN)}
      >
        {(
          [
            ["sites", "Sites"],
            ["editions", "Editions"],
            ["contentlists", "Content lists"],
            ["contexts", "Contexts / schemes"],
            ["delivery", "Delivery types"],
          ] as const
        ).map(([id, label]) => (
          <button
            key={id}
            type="button"
            role="tab"
            aria-selected={nav === id}
            style={buttonStyle}
            onClick={() => setNav(id)}
          >
            {label}
          </button>
        ))}
      </div>

      {nav === "sites" && <SiteDesignPanel />}

      {nav === "editions" && (
        <>
          <div style={toolbarStyle}>
            <label>
              Site{" "}
              <select
                value={siteId}
                onChange={(e) => setSiteId(e.target.value)}
                aria-label="Design site"
              >
                {sites.map((s) => (
                  <option key={s.id} value={s.id}>
                    {s.name}
                  </option>
                ))}
              </select>
            </label>
            <button
              type="button"
              style={buttonStyle}
              onClick={() => setEditionEdit("new")}
            >
              Add edition
            </button>
          </div>
          {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
          {error && (
            <p style={errorStyle} role="alert">
              {error}
            </p>
          )}
          {!loading && editions.length === 0 && (
            <p style={emptyStyle}>No editions for this site.</p>
          )}
          <ul style={listStyle}>
            {editions.map((e) => (
              <li key={e.editionId ?? e.name} style={listItemStyle}>
                <button
                  type="button"
                  style={buttonStyle}
                  onClick={() => setEditionEdit(e)}
                >
                  {e.name}
                </button>
                <span style={{ color: "#666" }}>{e.comment ?? ""}</span>
              </li>
            ))}
          </ul>
        </>
      )}

      {nav === "contentlists" && (
        <>
          <div style={toolbarStyle}>
            <button
              type="button"
              style={buttonStyle}
              onClick={() => setClEdit("new")}
            >
              Add content list
            </button>
          </div>
          {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
          {error && (
            <p style={errorStyle} role="alert">
              {error}
            </p>
          )}
          {!loading && contentLists.length === 0 && (
            <p style={emptyStyle}>No content lists.</p>
          )}
          <ul style={listStyle}>
            {contentLists.map((c) => (
              <li key={c.contentListId ?? c.name} style={listItemStyle}>
                <button
                  type="button"
                  style={buttonStyle}
                  onClick={() => setClEdit(c)}
                >
                  {c.name}
                </button>
                <span style={{ color: "#666" }}>{c.listType}</span>
              </li>
            ))}
          </ul>
        </>
      )}

      {nav === "contexts" && <ContextsPanel />}
      {nav === "delivery" && <DeliveryTypesPanel />}
    </div>
  );
}
