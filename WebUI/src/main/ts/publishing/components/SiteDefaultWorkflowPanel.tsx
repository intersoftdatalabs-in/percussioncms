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

import React, { useEffect, useRef, useState } from "react";
import { isApiError } from "../../api/client";
import { getSite, updateSite } from "../../api/developer/sitesApi";
import { listWorkflows } from "../../api/developer/workflowsApi";
import { message, MSG } from "../../i18n/message";
import {
  normalizeWorkflowName,
  resolveCatalogWorkflow,
  siteDefaultWorkflowHttpFailure,
  workflowNamesMatch,
} from "../siteDefaultWorkflow";
import { buttonStyle, errorStyle, primaryButtonStyle } from "../publishing.styles";

function saveErrorMessage(err: unknown): string {
  if (isApiError(err)) {
    const kind = siteDefaultWorkflowHttpFailure(err.status);
    if (kind === "bad_request") {
      return message(MSG.PUBLISH_SITE_DEFAULT_WORKFLOW_BAD);
    }
    if (kind === "forbidden") {
      return message(MSG.PUBLISH_SITE_DEFAULT_WORKFLOW_FORBIDDEN);
    }
    if (kind === "conflict") {
      return message(MSG.PUBLISH_SITE_DEFAULT_WORKFLOW_CONFLICT);
    }
  }
  return message(MSG.PUBLISH_SITE_DEFAULT_WORKFLOW_ERROR);
}

function catalogNames(rows: { workflowName?: string }[]): string[] {
  const seen = new Set<string>();
  const names: string[] = [];
  for (const row of rows) {
    const name = normalizeWorkflowName(row.workflowName ?? "");
    if (!name) {
      continue;
    }
    const key = name.toLowerCase();
    if (seen.has(key)) {
      continue;
    }
    seen.add(key);
    names.push(name);
  }
  names.sort((a, b) => a.localeCompare(b));
  return names;
}

/**
 * Choose and save the open site's default workflow from the workflow catalog.
 * Cancel, an unchanged name, and an empty choice do not write and do not claim
 * success. HTTP 400, 403, and 409 stay on the form.
 */
export function SiteDefaultWorkflowPanel({
  siteName,
}: {
  siteName: string;
}): React.ReactElement {
  const [saved, setSaved] = useState("");
  const [draft, setDraft] = useState("");
  const [catalog, setCatalog] = useState<string[]>([]);
  const [editing, setEditing] = useState(false);
  const [notice, setNotice] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const inflight = useRef(false);
  const key = siteName.trim();

  useEffect(() => {
    let cancelled = false;
    if (!key) {
      setSaved("");
      setDraft("");
      setCatalog([]);
      return;
    }
    getSite(key)
      .then((site) => {
        if (cancelled) {
          return;
        }
        const text = site.workflowName ?? "";
        setSaved(text);
        setDraft(text);
      })
      .catch(() => {
        if (!cancelled) {
          setSaved("");
          setDraft("");
        }
      });
    listWorkflows()
      .then((workflows) => {
        if (!cancelled) {
          setCatalog(catalogNames(workflows));
        }
      })
      .catch(() => {
        if (!cancelled) {
          setCatalog([]);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [key]);

  function closeWithoutWrite(): void {
    setDraft(saved);
    setEditing(false);
    setError(null);
  }

  async function handleSave(): Promise<void> {
    if (!key || inflight.current || busy) {
      return;
    }
    const resolved = resolveCatalogWorkflow(draft, catalog);
    if (resolved === "empty") {
      setNotice(null);
      setError(message(MSG.PUBLISH_SITE_DEFAULT_WORKFLOW_EMPTY));
      return;
    }
    if (resolved === "unknown") {
      setNotice(null);
      setError(message(MSG.PUBLISH_SITE_DEFAULT_WORKFLOW_UNKNOWN));
      return;
    }
    if (workflowNamesMatch(saved, resolved)) {
      closeWithoutWrite();
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const updated = await updateSite(key, { name: key, workflowName: resolved });
      const text = updated.workflowName ?? resolved;
      setSaved(text);
      setDraft(text);
      setEditing(false);
      setNotice(message(MSG.PUBLISH_SITE_DEFAULT_WORKFLOW_SAVED));
    } catch (err: unknown) {
      setError(saveErrorMessage(err));
    } finally {
      inflight.current = false;
      setBusy(false);
    }
  }

  const shown = saved.trim() ? saved.trim() : "—";
  const selected = catalog.find((name) => workflowNamesMatch(name, draft)) ?? "";

  return (
    <div data-testid="publish-site-default-workflow-panel" style={{ marginBottom: 16 }}>
      <div style={{ display: "flex", gap: 8, alignItems: "center", flexWrap: "wrap" }}>
        <span>{message(MSG.PUBLISH_SITE_DEFAULT_WORKFLOW)}</span>
        <span data-testid="publish-site-default-workflow">{shown}</span>
        {!editing ? (
          <button
            type="button"
            style={buttonStyle}
            data-testid="publish-site-default-workflow-edit"
            onClick={() => {
              setDraft(saved);
              setEditing(true);
              setError(null);
              setNotice(null);
            }}
          >
            {message(MSG.PUBLISH_EDIT_SITE_DEFAULT_WORKFLOW)}
          </button>
        ) : null}
      </div>
      {notice && !editing ? (
        <p data-testid="publish-site-default-workflow-saved" style={{ color: "#276749" }}>
          {notice}
        </p>
      ) : null}
      {editing ? (
        <div data-testid="publish-site-default-workflow-form">
          <label>
            <span className="sr-only">{message(MSG.PUBLISH_SITE_DEFAULT_WORKFLOW)}</span>
            <select
              data-testid="publish-site-default-workflow-choice"
              value={selected}
              disabled={busy}
              onChange={(e) => setDraft(e.target.value)}
              style={{ display: "block", width: "100%", maxWidth: 480, margin: "8px 0" }}
            >
              <option value="">{message(MSG.PUBLISH_SITE_DEFAULT_WORKFLOW_CHOOSE)}</option>
              {catalog.map((name) => (
                <option key={name} value={name}>
                  {name}
                </option>
              ))}
            </select>
          </label>
          {error ? (
            <p role="alert" data-testid="publish-site-default-workflow-error" style={errorStyle}>
              {error}
            </p>
          ) : null}
          <div style={{ display: "flex", gap: 8 }}>
            <button
              type="button"
              data-testid="publish-site-default-workflow-save"
              style={primaryButtonStyle}
              disabled={busy || !key}
              onClick={() => void handleSave()}
            >
              {message(MSG.PUBLISH_SAVE)}
            </button>
            <button
              type="button"
              data-testid="publish-site-default-workflow-cancel"
              style={buttonStyle}
              disabled={busy}
              onClick={closeWithoutWrite}
            >
              {message(MSG.PUBLISH_CREATE_SITE_CANCEL)}
            </button>
          </div>
        </div>
      ) : null}
    </div>
  );
}
