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

import React, { useCallback, useEffect, useState } from "react";
import { fetchSites } from "../../api/home/homeApi";
import {
  listContentLists,
  listEditionsBySite,
  type ContentListSummary,
  type EditionSummary,
} from "../../api/publishing/designApi";
import { message, MSG } from "../../i18n/message";
import { contentListsAfterSuccessfulCopy } from "../contentListCopy";
import { contentListsAfterSuccessfulDescription } from "../contentListDescription";
import { contentListsAfterSuccessfulGenerator } from "../contentListGenerator";
import { storedItemFilterLabel } from "../contentListItemFilter";
import { editionsAfterSuccessfulComment } from "../editionComment";
import { editionsAfterSuccessfulPriority } from "../editionPriority";
import { ContentListCopyPanel } from "../design/ContentListCopyPanel";
import { ContentListDescriptionPanel } from "../design/ContentListDescriptionPanel";
import { ContentListGeneratorPanel } from "../design/ContentListGeneratorPanel";
import { ContentListEditor } from "../design/ContentListEditor";
import {
  isLegacyContentList,
  normalizeListType,
} from "../design/designLegacyTypes";
import { ContextsPanel } from "../design/ContextsPanel";
import { DeliveryTypesPanel } from "../design/DeliveryTypesPanel";
import { EditionCommentPanel } from "../design/EditionCommentPanel";
import { EditionEditor, type EditionCopiedInfo } from "../design/EditionEditor";
import { EditionPriorityPanel } from "../design/EditionPriorityPanel";
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
  const [editionComment, setEditionComment] = useState<EditionSummary | null>(
    null,
  );
  const [editionPriority, setEditionPriority] = useState<EditionSummary | null>(
    null,
  );
  const [clEdit, setClEdit] = useState<ContentListSummary | null | "new">(null);
  const [clCopy, setClCopy] = useState<ContentListSummary | null>(null);
  const [clDescribe, setClDescribe] = useState<ContentListSummary | null>(null);
  const [clGenerator, setClGenerator] = useState<ContentListSummary | null>(null);

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

  if (editionComment !== null) {
    const commentEdition = editionComment;
    return (
      <div data-testid="publish-section-design">
        <EditionCommentPanel
          edition={commentEdition}
          onCancel={() => setEditionComment(null)}
          onSaved={async (comment) => {
            const id = commentEdition.editionId ?? "";
            setEditionComment(null);
            let refreshed: EditionSummary[] | null = null;
            try {
              if (siteId) {
                refreshed = await listEditionsBySite(siteId);
              }
            } catch {
              refreshed = null;
            }
            setEditions((prev) =>
              editionsAfterSuccessfulComment(refreshed, id, comment, prev),
            );
          }}
        />
      </div>
    );
  }

  if (editionPriority !== null) {
    const priorityEdition = editionPriority;
    return (
      <div data-testid="publish-section-design">
        <EditionPriorityPanel
          edition={priorityEdition}
          onCancel={() => setEditionPriority(null)}
          onSaved={async (priority) => {
            const id = priorityEdition.editionId ?? "";
            setEditionPriority(null);
            let refreshed: EditionSummary[] | null = null;
            try {
              if (siteId) {
                refreshed = await listEditionsBySite(siteId);
              }
            } catch {
              refreshed = null;
            }
            setEditions((prev) =>
              editionsAfterSuccessfulPriority(refreshed, id, priority, prev),
            );
          }}
        />
      </div>
    );
  }

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
          onCopied={(info: EditionCopiedInfo) => {
            setEditionEdit(null);
            const target = info.targetSiteId.trim();
            if (target && target !== siteId) {
              setSiteId(target);
              return;
            }
            reloadEditions();
          }}
        />
      </div>
    );
  }

  if (clCopy !== null) {
    return (
      <div data-testid="publish-section-design">
        <ContentListCopyPanel
          source={clCopy}
          onCancel={() => setClCopy(null)}
          onCopied={async (created) => {
            let refreshed: ContentListSummary[] | null = null;
            try {
              refreshed = await listContentLists();
            } catch {
              refreshed = null;
            }
            setContentLists((prev) =>
              contentListsAfterSuccessfulCopy(refreshed, created, prev),
            );
            setClCopy(null);
          }}
        />
      </div>
    );
  }

  if (clDescribe !== null) {
    const described = clDescribe;
    return (
      <div data-testid="publish-section-design">
        <ContentListDescriptionPanel
          contentList={described}
          onCancel={() => setClDescribe(null)}
          onSaved={async (description) => {
            const id = described.contentListId ?? "";
            setClDescribe(null);
            let refreshed: ContentListSummary[] | null = null;
            try {
              refreshed = await listContentLists();
            } catch {
              refreshed = null;
            }
            setContentLists((prev) =>
              contentListsAfterSuccessfulDescription(
                refreshed,
                id,
                description,
                prev,
              ),
            );
          }}
        />
      </div>
    );
  }

  if (clGenerator !== null) {
    const edited = clGenerator;
    return (
      <div data-testid="publish-section-design">
        <ContentListGeneratorPanel
          contentList={edited}
          onCancel={() => setClGenerator(null)}
          onSaved={async (generator) => {
            const id = edited.contentListId ?? "";
            setClGenerator(null);
            let refreshed: ContentListSummary[] | null = null;
            try {
              refreshed = await listContentLists();
            } catch {
              refreshed = null;
            }
            setContentLists((prev) =>
              contentListsAfterSuccessfulGenerator(
                refreshed,
                id,
                generator,
                prev,
              ),
            );
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
              data-testid="design-add-edition"
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
                  data-testid={
                    e.editionId
                      ? `design-edition-${e.editionId}`
                      : "design-edition-unnamed"
                  }
                  style={buttonStyle}
                  onClick={() => setEditionEdit(e)}
                >
                  {e.name}
                </button>
                <span
                  style={{ color: "#666" }}
                  data-testid={
                    e.editionId
                      ? `design-edition-comment-${e.editionId}`
                      : undefined
                  }
                >
                  {e.comment ?? ""}
                </span>
                {e.editionId && (
                  <button
                    type="button"
                    style={buttonStyle}
                    data-testid={`design-edition-set-comment-${e.editionId}`}
                    onClick={() => setEditionComment(e)}
                  >
                    Comment
                  </button>
                )}
                <span
                  data-testid={
                    e.editionId
                      ? `design-edition-priority-value-${e.editionId}`
                      : undefined
                  }
                >
                  {e.priority ?? ""}
                </span>
                {e.editionId && (
                  <button
                    type="button"
                    style={buttonStyle}
                    data-testid={`design-edition-set-priority-${e.editionId}`}
                    onClick={() => setEditionPriority(e)}
                  >
                    Priority
                  </button>
                )}
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
              data-testid="design-add-content-list"
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
                  data-testid={
                    c.contentListId
                      ? `design-content-list-${c.contentListId}`
                      : "design-content-list-unnamed"
                  }
                  style={buttonStyle}
                  onClick={() => setClEdit(c)}
                >
                  {c.name}
                </button>
                {c.contentListId && (
                  <button
                    type="button"
                    data-testid={`design-content-list-copy-${c.contentListId}`}
                    style={buttonStyle}
                    onClick={() => setClCopy(c)}
                  >
                    Copy
                  </button>
                )}
                {c.contentListId && (
                  <button
                    type="button"
                    style={buttonStyle}
                    data-testid={`design-content-list-describe-${c.contentListId}`}
                    onClick={() => setClDescribe(c)}
                  >
                    Description
                  </button>
                )}
                {c.contentListId &&
                  normalizeListType(c.listType) === "modern" && (
                    <button
                      type="button"
                      style={buttonStyle}
                      data-testid={`design-content-list-generator-${c.contentListId}`}
                      onClick={() => setClGenerator(c)}
                    >
                      Generator
                    </button>
                  )}
                <span
                  style={{ color: "#666" }}
                  data-testid={
                    c.contentListId
                      ? `design-content-list-type-${c.contentListId}`
                      : undefined
                  }
                >
                  {c.listType}
                </span>
                {c.contentListId && (
                  <span
                    data-testid={`design-content-list-source-${c.contentListId}`}
                  >
                    {isLegacyContentList(c.listType)
                      ? (c.url ?? "")
                      : (c.generator ?? "")}
                  </span>
                )}
                {c.contentListId && (
                  <span
                    data-testid={`design-content-list-description-${c.contentListId}`}
                  >
                    {c.description ?? ""}
                  </span>
                )}
                {c.contentListId && (
                  <span
                    data-testid={`design-content-list-filter-${c.contentListId}`}
                  >
                    {storedItemFilterLabel(c)}
                  </span>
                )}
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
