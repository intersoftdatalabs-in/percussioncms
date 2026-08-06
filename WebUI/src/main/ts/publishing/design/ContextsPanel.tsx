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

import React, { useEffect, useState } from "react";
import {
  createContext,
  createScheme,
  deleteContext,
  deleteScheme,
  getScheme,
  listContexts,
  listSchemesForContext,
  updateContext,
  updateScheme,
  type ContextSummary,
  type LocationSchemeSummary,
  type SchemeParameter,
} from "../../api/publishing/designApi";
import { message, MSG } from "../../i18n/message";
import {
  buttonStyle,
  emptyStyle,
  errorStyle,
  formRowStyle,
  listItemStyle,
  listStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";
import { normalizeSchemeType } from "./designLegacyTypes";
import { SiteRootBrowser } from "./SiteRootBrowser";

type Mode =
  | { kind: "list" }
  | { kind: "context-edit"; context: ContextSummary | null }
  | { kind: "scheme-edit"; scheme: LocationSchemeSummary | null; contextId: string };

/**
 * Contexts CRUD + location schemes with parameters and path browser.
 */
export function ContextsPanel(): React.ReactElement {
  const [contexts, setContexts] = useState<ContextSummary[]>([]);
  const [selected, setSelected] = useState("");
  const [schemes, setSchemes] = useState<LocationSchemeSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [mode, setMode] = useState<Mode>({ kind: "list" });
  const [showBrowser, setShowBrowser] = useState(false);

  // Context form
  const [ctxName, setCtxName] = useState("");
  const [ctxDesc, setCtxDesc] = useState("");

  // Scheme form
  const [schName, setSchName] = useState("");
  const [schGen, setSchGen] = useState("");
  const [schDesc, setSchDesc] = useState("");
  const [schCtype, setSchCtype] = useState("");
  const [schTemplate, setSchTemplate] = useState("");
  const [params, setParams] = useState<SchemeParameter[]>([]);
  const [paramName, setParamName] = useState("");
  const [paramType, setParamType] = useState("String");
  const [paramValue, setParamValue] = useState("");

  function reloadContexts(): void {
    setLoading(true);
    listContexts()
      .then((list) => {
        setContexts(list);
        if (list.length > 0 && !selected) {
          setSelected(String(list[0].contextId ?? ""));
        }
      })
      .catch(() => setError(message(MSG.PUBLISH_ERROR)))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    reloadContexts();
  }, []);

  useEffect(() => {
    if (!selected) {
      setSchemes([]);
      return;
    }
    listSchemesForContext(selected)
      .then(setSchemes)
      .catch(() => setSchemes([]));
  }, [selected]);

  function openContextEdit(c: ContextSummary | null): void {
    setCtxName(c?.name ?? "");
    setCtxDesc(c?.description ?? "");
    setMode({ kind: "context-edit", context: c });
  }

  async function openSchemeEdit(
    s: LocationSchemeSummary | null,
    contextId: string,
  ): Promise<void> {
    if (s?.schemeId) {
      try {
        const full = await getScheme(s.schemeId);
        setSchName(full.name ?? "");
        setSchGen(full.generator ?? "");
        setSchDesc(full.description ?? "");
        setSchCtype(full.contentTypeId != null ? String(full.contentTypeId) : "");
        setSchTemplate(full.templateId != null ? String(full.templateId) : "");
        setParams(full.parameters ?? []);
        setMode({ kind: "scheme-edit", scheme: full, contextId });
        return;
      } catch {
        /* fall through */
      }
    }
    setSchName(s?.name ?? "");
    setSchGen(s?.generator ?? "");
    setSchDesc(s?.description ?? "");
    setSchCtype("");
    setSchTemplate("");
    setParams([]);
    setMode({ kind: "scheme-edit", scheme: s, contextId });
  }

  async function saveContext(): Promise<void> {
    if (!ctxName.trim()) {
      setError("Name is required");
      return;
    }
    setError(null);
    try {
      if (mode.kind === "context-edit" && mode.context?.contextId) {
        await updateContext(mode.context.contextId, {
          name: ctxName.trim(),
          description: ctxDesc,
        });
      } else {
        await createContext({ name: ctxName.trim(), description: ctxDesc });
      }
      setMode({ kind: "list" });
      reloadContexts();
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    }
  }

  async function removeContext(id: string): Promise<void> {
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_DELETE_DESIGN))) {
      return;
    }
    try {
      await deleteContext(id);
      if (selected === id) {
        setSelected("");
      }
      reloadContexts();
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    }
  }

  function addParam(): void {
    if (!paramName.trim() || !paramValue.trim()) {
      return;
    }
    setParams((prev) => [
      ...prev,
      {
        name: paramName.trim(),
        type: paramType,
        value: paramValue,
        sequence: prev.length,
      },
    ]);
    setParamName("");
    setParamValue("");
  }

  async function saveScheme(): Promise<void> {
    if (mode.kind !== "scheme-edit") {
      return;
    }
    if (!schName.trim() || !schGen.trim()) {
      setError("Name and generator are required");
      return;
    }
    setError(null);
    const body: LocationSchemeSummary = {
      name: schName.trim(),
      generator: schGen.trim(),
      description: schDesc,
      contentTypeId: schCtype ? Number(schCtype) : undefined,
      templateId: schTemplate ? Number(schTemplate) : undefined,
      parameters: params,
      contextId: mode.contextId,
    };
    try {
      if (mode.scheme?.schemeId) {
        await updateScheme(mode.scheme.schemeId, body);
      } else {
        await createScheme(mode.contextId, body);
      }
      setMode({ kind: "list" });
      setSchemes(await listSchemesForContext(mode.contextId));
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    }
  }

  async function removeScheme(id: string): Promise<void> {
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_DELETE_DESIGN))) {
      return;
    }
    try {
      await deleteScheme(id);
      if (selected) {
        setSchemes(await listSchemesForContext(selected));
      }
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    }
  }

  if (mode.kind === "context-edit") {
    return (
      <div data-testid="context-editor">
        <h3>{mode.context ? "Edit context" : "Add context"}</h3>
        <div style={formRowStyle}>
          <label htmlFor="ctx-name">* Name</label>
          <input
            id="ctx-name"
            value={ctxName}
            onChange={(e) => setCtxName(e.target.value)}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="ctx-desc">Description</label>
          <input
            id="ctx-desc"
            value={ctxDesc}
            onChange={(e) => setCtxDesc(e.target.value)}
          />
        </div>
        {error && (
          <p style={errorStyle} role="alert">
            {error}
          </p>
        )}
        <div style={toolbarStyle}>
          <button type="button" style={primaryButtonStyle} onClick={() => void saveContext()}>
            {message(MSG.PUBLISH_SAVE)}
          </button>
          <button type="button" style={buttonStyle} onClick={() => setMode({ kind: "list" })}>
            {message(MSG.PUBLISH_BACK)}
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "scheme-edit") {
    return (
      <div data-testid="scheme-editor">
        <h3>{mode.scheme?.schemeId ? "Edit location scheme" : "Add location scheme"}</h3>
        <div style={formRowStyle}>
          <label htmlFor="sch-name">* Name</label>
          <input
            id="sch-name"
            value={schName}
            onChange={(e) => setSchName(e.target.value)}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="sch-gen">* Generator</label>
          <input
            id="sch-gen"
            value={schGen}
            onChange={(e) => setSchGen(e.target.value)}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="sch-desc">Description</label>
          <input
            id="sch-desc"
            value={schDesc}
            onChange={(e) => setSchDesc(e.target.value)}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="sch-ctype">Content type id</label>
          <input
            id="sch-ctype"
            value={schCtype}
            onChange={(e) => setSchCtype(e.target.value)}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="sch-tpl">Template id</label>
          <input
            id="sch-tpl"
            value={schTemplate}
            onChange={(e) => setSchTemplate(e.target.value)}
          />
        </div>
        <h4>Parameters (legacy schemes)</h4>
        <ul style={listStyle}>
          {params.map((p, i) => (
            <li key={`${p.name}-${i}`} style={listItemStyle}>
              <span>
                {p.name} ({p.type}): {p.value}
              </span>
              <button
                type="button"
                style={buttonStyle}
                onClick={() => setParams((prev) => prev.filter((_, j) => j !== i))}
              >
                Remove
              </button>
            </li>
          ))}
        </ul>
        <div style={toolbarStyle}>
          <input
            placeholder="name"
            value={paramName}
            onChange={(e) => setParamName(e.target.value)}
          />
          <select value={paramType} onChange={(e) => setParamType(e.target.value)}>
            <option value="String">String</option>
            <option value="BackendColumn">BackendColumn</option>
          </select>
          <input
            placeholder="value"
            value={paramValue}
            onChange={(e) => setParamValue(e.target.value)}
          />
          <button type="button" style={buttonStyle} onClick={addParam}>
            Add param
          </button>
        </div>
        <div style={{ marginTop: 12 }}>
          <button
            type="button"
            style={buttonStyle}
            onClick={() => setShowBrowser((v) => !v)}
          >
            {showBrowser ? "Hide" : "Show"} site root / path browser
          </button>
          {showBrowser && (
            <SiteRootBrowser
              rootPath="//Sites"
              onSelectPath={(p) => setParamValue(p)}
            />
          )}
        </div>
        {error && (
          <p style={errorStyle} role="alert">
            {error}
          </p>
        )}
        <div style={toolbarStyle}>
          <button type="button" style={primaryButtonStyle} onClick={() => void saveScheme()}>
            {message(MSG.PUBLISH_SAVE)}
          </button>
          <button type="button" style={buttonStyle} onClick={() => setMode({ kind: "list" })}>
            {message(MSG.PUBLISH_BACK)}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div data-testid="contexts-panel">
      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p style={errorStyle} role="alert">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        <label>
          Context{" "}
          <select
            value={selected}
            onChange={(e) => setSelected(e.target.value)}
            aria-label="Publishing context"
          >
            {contexts.map((c) => (
              <option key={c.contextId} value={c.contextId}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        <button type="button" style={buttonStyle} onClick={() => openContextEdit(null)}>
          Add context
        </button>
        {selected && (
          <>
            <button
              type="button"
              style={buttonStyle}
              onClick={() => {
                const c = contexts.find((x) => x.contextId === selected) ?? null;
                openContextEdit(c);
              }}
            >
              Edit context
            </button>
            <button
              type="button"
              style={buttonStyle}
              onClick={() => void removeContext(selected)}
            >
              Delete context
            </button>
            <button
              type="button"
              style={buttonStyle}
              onClick={() => void openSchemeEdit(null, selected)}
            >
              Add scheme
            </button>
          </>
        )}
      </div>
      {!loading && contexts.length === 0 && (
        <p style={emptyStyle}>No publishing contexts.</p>
      )}
      <h4>Location schemes</h4>
      {schemes.length === 0 ? (
        <p style={emptyStyle}>No schemes for this context.</p>
      ) : (
        <ul style={listStyle}>
          {schemes.map((s) => (
            <li key={s.schemeId ?? s.name} style={listItemStyle}>
              <button
                type="button"
                style={buttonStyle}
                onClick={() => void openSchemeEdit(s, selected)}
              >
                {s.name}
              </button>
              <span style={{ color: "#666" }}>
                {normalizeSchemeType(s.schemeType, s.generator)}
                {s.generator ? ` · ${s.generator}` : ""}
              </span>
              {s.schemeId && (
                <button
                  type="button"
                  style={buttonStyle}
                  onClick={() => void removeScheme(s.schemeId!)}
                >
                  Delete
                </button>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
