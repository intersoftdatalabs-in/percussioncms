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

import React, { useCallback, useEffect, useMemo, useState } from "react";
import { fetchSites } from "../../api/home/homeApi";
import {
  listContentLists,
  listEditionsBySite,
  updateEdition,
  type ContentListSummary,
  type EditionSummary,
} from "../../api/publishing/designApi";
import { message, MSG } from "../../i18n/message";
import { contentListsAfterSuccessfulCopy } from "../contentListCopy";
import { contentListsAfterSuccessfulDescription } from "../contentListDescription";
import { contentListsAfterSuccessfulGenerator } from "../contentListGenerator";
import { contentListsAfterSuccessfulUrl } from "../contentListUrl";
import { storedItemFilterLabel } from "../contentListItemFilter";
import { editionsAfterSuccessfulComment } from "../editionComment";
import {
  filterDesignEditions,
  type EditionScheduleFilter,
} from "../editionListFilter";
import { editionsAfterSuccessfulPriority } from "../editionPriority";
import {
  buildEditionReorderBody,
  editionsAfterSuccessfulReorder,
  sortEditionsByPriority,
  type EditionReorderDirection,
} from "../editionPriorityReorder";
import { editionsAfterSuccessfulSchedule } from "../editionSchedule";
import { mapEditionReorderError } from "../editionSaveErrors";
import { ContentListCopyPanel } from "../design/ContentListCopyPanel";
import { ContentListDescriptionPanel } from "../design/ContentListDescriptionPanel";
import { ContentListGeneratorPanel } from "../design/ContentListGeneratorPanel";
import { ContentListUrlPanel } from "../design/ContentListUrlPanel";
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
import { EditionSchedulePanel } from "../design/EditionSchedulePanel";
import { SiteDesignPanel } from "../design/SiteDesignPanel";
import {
  buttonStyle,
  emptyStyle,
  errorStyle,
  listItemStyle,
  listStyle,
  toolbarStyle,
} from "../publishing.styles";
import { formatEditionScheduleLabel } from "../editionSchedule";

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
  const [editionSchedule, setEditionSchedule] = useState<EditionSummary | null>(
    null,
  );
  /** Edition id with an in-flight priority reorder; row buttons stay disabled. */
  const [movingEdition, setMovingEdition] = useState<string | null>(null);
  /** Operator search + schedule facet for the editions list. */
  const [editionSearch, setEditionSearch] = useState("");
  const [editionScheduleFilter, setEditionScheduleFilter] =
    useState<EditionScheduleFilter>("all");
  const [clEdit, setClEdit] = useState<ContentListSummary | null | "new">(null);
  const [clCopy, setClCopy] = useState<ContentListSummary | null>(null);
  const [clDescribe, setClDescribe] = useState<ContentListSummary | null>(null);
  const [clGenerator, setClGenerator] = useState<ContentListSummary | null>(null);
  const [clUrl, setClUrl] = useState<ContentListSummary | null>(null);

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

  /** Editions list after the operator search + schedule facet. */
  const visibleEditions = useMemo(
    () =>
      filterDesignEditions(
        sortEditionsByPriority(editions),
        editionSearch,
        editionScheduleFilter,
      ),
    [editions, editionSearch, editionScheduleFilter],
  );

  /**
   * Move one edition up (+1 priority) or down (−1) with a priority-only
   * update. The list is priority-ordered, so the stored write is what
   * persists the new order. Cancel and HTTP 400/403/409 keep the previous
   * order and surface the mapped error; name, comment, schedule, and
   * content-list order stay.
   */
  async function handleEditionReorder(
    edition: EditionSummary,
    direction: EditionReorderDirection,
  ): Promise<void> {
    const editionId = edition.editionId;
    if (!editionId || movingEdition !== null) {
      return;
    }
    const body = buildEditionReorderBody(edition.priority, direction);
    if (!body || body.priority == null) {
      return;
    }
    const confirmText = message(
      direction < 0
        ? MSG.PUBLISH.DESIGN.EDITIONS.CONFIRM_EDITION_MOVE_DOWN
        : MSG.PUBLISH.DESIGN.EDITIONS.CONFIRM_EDITION_MOVE_UP,
    );
    if (!window.confirm(confirmText)) {
      return;
    }
    const nextPriority = body.priority;
    setMovingEdition(editionId);
    setError(null);
    try {
      await updateEdition(editionId, body);
      let refreshed: EditionSummary[] | null = null;
      try {
        if (siteId) {
          refreshed = await listEditionsBySite(siteId);
        }
      } catch {
        refreshed = null;
      }
      setEditions((prev) =>
        editionsAfterSuccessfulReorder(refreshed, editionId, nextPriority, prev),
      );
    } catch (e) {
      setError(mapEditionReorderError(e));
    } finally {
      setMovingEdition(null);
    }
  }

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

  if (editionSchedule !== null) {
    const scheduleEdition = editionSchedule;
    return (
      <div data-testid="publish-section-design">
        <EditionSchedulePanel
          edition={scheduleEdition}
          onCancel={() => setEditionSchedule(null)}
          onSaved={async (dates) => {
            const id = scheduleEdition.editionId ?? "";
            setEditionSchedule(null);
            let refreshed: EditionSummary[] | null = null;
            try {
              if (siteId) {
                refreshed = await listEditionsBySite(siteId);
              }
            } catch {
              refreshed = null;
            }
            setEditions((prev) =>
              editionsAfterSuccessfulSchedule(refreshed, id, dates, prev),
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

  if (clUrl !== null) {
    const edited = clUrl;
    return (
      <div data-testid="publish-section-design">
        <ContentListUrlPanel
          contentList={edited}
          onCancel={() => setClUrl(null)}
          onSaved={async (url) => {
            const id = edited.contentListId ?? "";
            setClUrl(null);
            let refreshed: ContentListSummary[] | null = null;
            try {
              refreshed = await listContentLists();
            } catch {
              refreshed = null;
            }
            setContentLists((prev) =>
              contentListsAfterSuccessfulUrl(refreshed, id, url, prev),
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
            <label>
              <span className="sr-only">
                {message(MSG.PUBLISH.DESIGN.EDITIONS.SEARCH_EDITIONS)}
              </span>
              <input
                type="search"
                data-testid="design-editions-search"
                value={editionSearch}
                onChange={(e) => setEditionSearch(e.target.value)}
                placeholder={message(
                  MSG.PUBLISH.DESIGN.EDITIONS.SEARCH_EDITIONS,
                )}
                aria-label={message(
                  MSG.PUBLISH.DESIGN.EDITIONS.SEARCH_EDITIONS,
                )}
                style={{ padding: "6px 10px", minWidth: 200 }}
              />
            </label>
            <label>
              <span className="sr-only">
                {message(MSG.PUBLISH.DESIGN.EDITIONS.SCHEDULE_FILTER_ARIA)}
              </span>
              <select
                data-testid="design-editions-schedule-filter"
                value={editionScheduleFilter}
                onChange={(e) =>
                  setEditionScheduleFilter(
                    e.target.value as EditionScheduleFilter,
                  )
                }
                aria-label={message(
                  MSG.PUBLISH.DESIGN.EDITIONS.SCHEDULE_FILTER_ARIA,
                )}
              >
                <option value="all">
                  {message(
                    MSG.PUBLISH.DESIGN.EDITIONS.SCHEDULE_FILTER_ALL,
                  )}
                </option>
                <option value="scheduled">
                  {message(
                    MSG.PUBLISH.DESIGN.EDITIONS.SCHEDULE_FILTER_SCHEDULED,
                  )}
                </option>
                <option value="unscheduled">
                  {message(
                    MSG.PUBLISH.DESIGN.EDITIONS.SCHEDULE_FILTER_UNSCHEDULED,
                  )}
                </option>
              </select>
            </label>
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
          {!loading && editions.length > 0 && visibleEditions.length === 0 && (
            <p style={emptyStyle} data-testid="design-editions-empty-filter">
              {message(MSG.PUBLISH.DESIGN.EDITIONS.EMPTY_SEARCH_FILTER)}
            </p>
          )}
          <ul style={listStyle}>
            {visibleEditions.map((e) => (
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
                {e.editionId && (
                  <span style={{ display: "flex", gap: 8 }}>
                    <button
                      type="button"
                      data-testid={`design-edition-move-up-${e.editionId}`}
                      style={buttonStyle}
                      disabled={
                        movingEdition !== null ||
                        buildEditionReorderBody(e.priority, 1) === null
                      }
                      aria-label={message(
                        MSG.PUBLISH.DESIGN.EDITIONS.EDITION_MOVE_UP,
                      )}
                      onClick={() => void handleEditionReorder(e, 1)}
                    >
                      {message(MSG.PUBLISH.DESIGN.EDITIONS.EDITION_MOVE_UP)}
                    </button>
                    <button
                      type="button"
                      data-testid={`design-edition-move-down-${e.editionId}`}
                      style={buttonStyle}
                      disabled={
                        movingEdition !== null ||
                        buildEditionReorderBody(e.priority, -1) === null
                      }
                      aria-label={message(
                        MSG.PUBLISH.DESIGN.EDITIONS.EDITION_MOVE_DOWN,
                      )}
                      onClick={() => void handleEditionReorder(e, -1)}
                    >
                      {message(MSG.PUBLISH.DESIGN.EDITIONS.EDITION_MOVE_DOWN)}
                    </button>
                  </span>
                )}
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
                <span
                  data-testid={
                    e.editionId
                      ? `design-edition-schedule-value-${e.editionId}`
                      : undefined
                  }
                >
                  {formatEditionScheduleLabel(e.startDate, e.endDate)}
                </span>
                {e.editionId && (
                  <button
                    type="button"
                    style={buttonStyle}
                    data-testid={`design-edition-set-schedule-${e.editionId}`}
                    onClick={() => setEditionSchedule(e)}
                  >
                    Schedule
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
                {c.contentListId && isLegacyContentList(c.listType) && (
                  <button
                    type="button"
                    style={buttonStyle}
                    data-testid={`design-content-list-url-${c.contentListId}`}
                    onClick={() => setClUrl(c)}
                  >
                    Legacy URL
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
