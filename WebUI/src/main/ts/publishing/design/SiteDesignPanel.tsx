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
  deleteSiteProperty,
  listContexts,
  listDesignSites,
  listSiteProperties,
  putSiteProperty,
  type ContextSummary,
  type SiteDesignSummary,
  type SitePropertyDto,
} from "../../api/publishing/designApi";
import { message, MSG } from "../../i18n/message";
import {
  CONTEXT_VARIABLE_EXISTS,
  contextVariableNameListed,
  contextVariablesAfterSuccessfulCreate,
  validateContextVariable,
} from "../contextVariable";
import { mapContextVariableSaveError } from "../contextVariableSaveErrors";
import {
  buildContextVariableValueBody,
  contextVariablesAfterSuccessfulValueChange,
  mapContextVariableValueSaveError,
  validateContextVariableValue,
} from "../contextVariableValue";
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

/** Design sites list + context variables (site properties) editor. */
export function SiteDesignPanel(): React.ReactElement {
  const [sites, setSites] = useState<SiteDesignSummary[]>([]);
  const [selectedId, setSelectedId] = useState("");
  const [contexts, setContexts] = useState<ContextSummary[]>([]);
  const [contextId, setContextId] = useState("");
  const [props, setProps] = useState<SitePropertyDto[]>([]);
  const [propName, setPropName] = useState("");
  const [propValue, setPropValue] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [valueTarget, setValueTarget] = useState<string | null>(null);
  const [valueDraft, setValueDraft] = useState("");
  const [valueError, setValueError] = useState<string | null>(null);
  const [valueSaving, setValueSaving] = useState(false);

  useEffect(() => {
    setLoading(true);
    Promise.all([listDesignSites(), listContexts()])
      .then(([s, c]) => {
        setSites(s);
        setContexts(c);
        if (s.length > 0) {
          setSelectedId(String(s[0].siteId ?? ""));
        }
        if (c.length > 0) {
          setContextId(String(c[0].contextId ?? ""));
        }
      })
      .catch(() => setError(message(MSG.PUBLISH_ERROR)))
      .finally(() => setLoading(false));
  }, []);

  useEffect(() => {
    setValueTarget(null);
    setValueDraft("");
    setValueError(null);
    if (!selectedId || !contextId) {
      setProps([]);
      return;
    }
    listSiteProperties(selectedId, contextId)
      .then(setProps)
      .catch(() => setProps([]));
  }, [selectedId, contextId]);

  const selected = sites.find((s) => s.siteId === selectedId);
  const valueName = (valueTarget ?? "").trim();
  const valueCurrent =
    props.find((row) => (row.name ?? "").trim() === valueName)?.value ?? "";
  const valueOthers = props.filter((row) => (row.name ?? "").trim() !== valueName);

  async function saveProp(): Promise<void> {
    if (saving) {
      return;
    }
    if (!selectedId || !contextId) {
      setError("Site, context, and property name are required");
      return;
    }
    const validated = validateContextVariable(propName, propValue);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    if (contextVariableNameListed(props, validated.name)) {
      setError(CONTEXT_VARIABLE_EXISTS);
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const saved = await putSiteProperty(selectedId, {
        name: validated.name,
        contextId,
        value: validated.value,
      });
      let refreshed: SitePropertyDto[] | null = null;
      try {
        refreshed = await listSiteProperties(selectedId, contextId);
      } catch {
        refreshed = null;
      }
      setProps(
        contextVariablesAfterSuccessfulCreate(
          refreshed,
          {
            name: saved.name ?? validated.name,
            contextId: saved.contextId ?? contextId,
            value: saved.value ?? validated.value,
          },
          props,
        ),
      );
      setPropName("");
      setPropValue("");
    } catch (e) {
      setError(mapContextVariableSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  function openValueChange(row: SitePropertyDto): void {
    setValueTarget(row.name ?? "");
    setValueDraft("");
    setValueError(null);
  }

  function closeValueChange(): void {
    if (valueSaving) {
      return;
    }
    setValueTarget(null);
    setValueDraft("");
    setValueError(null);
  }

  async function saveValue(): Promise<void> {
    if (valueSaving || saving || !valueTarget) {
      return;
    }
    if (!selectedId || !contextId) {
      setValueError("Site, context, and property name are required");
      return;
    }
    const validated = validateContextVariableValue(valueTarget, valueDraft, props);
    if (!validated.ok) {
      setValueError(validated.error);
      return;
    }
    const previous = props;
    setValueSaving(true);
    setValueError(null);
    try {
      const saved = await putSiteProperty(
        selectedId,
        buildContextVariableValueBody(validated.name, contextId, validated.value),
      );
      let refreshed: SitePropertyDto[] | null = null;
      try {
        refreshed = await listSiteProperties(selectedId, contextId);
      } catch {
        refreshed = null;
      }
      setProps(
        contextVariablesAfterSuccessfulValueChange(
          refreshed,
          {
            name: (saved.name ?? validated.name).trim(),
            contextId: saved.contextId ?? contextId,
            value: saved.value ?? validated.value,
          },
          previous,
        ),
      );
      setValueTarget(null);
      setValueDraft("");
    } catch (e) {
      setValueError(mapContextVariableValueSaveError(e));
    } finally {
      setValueSaving(false);
    }
  }

  async function removeProp(name: string): Promise<void> {
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_DELETE_DESIGN))) {
      return;
    }
    try {
      await deleteSiteProperty(selectedId, name, contextId);
      setProps(await listSiteProperties(selectedId, contextId));
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    }
  }

  return (
    <div data-testid="site-design-panel">
      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p style={errorStyle} role="alert" data-testid="context-variable-error">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        <label>
          Site{" "}
          <select
            data-testid="context-variable-site"
            value={selectedId}
            onChange={(e) => setSelectedId(e.target.value)}
            aria-label="Design site"
          >
            {sites.map((s) => (
              <option key={s.siteId} value={s.siteId}>
                {s.name}
              </option>
            ))}
          </select>
        </label>
        <label>
          Context{" "}
          <select
            data-testid="context-variable-context"
            value={contextId}
            onChange={(e) => setContextId(e.target.value)}
            aria-label="Property context"
          >
            {contexts.map((c) => (
              <option key={c.contextId} value={c.contextId}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
      </div>
      {selected && (
        <div style={{ marginBottom: 12, color: "#555", fontSize: "0.9rem" }}>
          <div>Folder root: {selected.folderRoot || "—"}</div>
          <div>Base URL: {selected.baseUrl || "—"}</div>
          {selected.description && <div>{selected.description}</div>}
        </div>
      )}
      <h4>Context variables</h4>
      {!loading && props.length === 0 && (
        <p style={emptyStyle}>No properties for this site/context.</p>
      )}
      <ul style={listStyle} data-testid="context-variable-list">
        {props.map((p) => (
          <li key={p.name} style={listItemStyle} data-testid="context-variable-row">
            <strong data-testid="context-variable-row-name">{p.name}</strong>
            <span style={{ color: "#666" }} data-testid="context-variable-row-value">
              {p.value}
            </span>
            <button
              type="button"
              style={buttonStyle}
              data-testid="context-variable-change-value"
              onClick={() => openValueChange(p)}
            >
              Change value
            </button>
            <button
              type="button"
              style={buttonStyle}
              onClick={() => void removeProp(p.name!)}
            >
              Delete
            </button>
          </li>
        ))}
      </ul>
      {valueTarget && (
        <div data-testid="context-variable-value-form">
          <h4>Change context variable value</h4>
          <p data-testid="context-variable-value-fields-note">
            The name stays. Other variables stay.
          </p>
          <p>
            Name: <span data-testid="context-variable-value-name">{valueName}</span>
          </p>
          <p>
            Current:{" "}
            <span data-testid="context-variable-value-current">{valueCurrent}</span>
          </p>
          <ul data-testid="context-variable-value-others" style={listStyle}>
            {valueOthers.map((row) => (
              <li key={row.name} data-testid="context-variable-value-other">
                {row.name}: {row.value}
              </li>
            ))}
          </ul>
          <div style={formRowStyle}>
            <label htmlFor="context-variable-value-input">Value</label>
            <input
              id="context-variable-value-input"
              data-testid="context-variable-value-input"
              value={valueDraft}
              onChange={(e) => setValueDraft(e.target.value)}
            />
          </div>
          {valueError && (
            <p style={errorStyle} role="alert" data-testid="context-variable-value-error">
              {valueError}
            </p>
          )}
          <div style={toolbarStyle}>
            <button
              type="button"
              style={primaryButtonStyle}
              data-testid="context-variable-value-save"
              disabled={valueSaving}
              onClick={() => void saveValue()}
            >
              Save value
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="context-variable-value-cancel"
              disabled={valueSaving}
              onClick={closeValueChange}
            >
              Cancel
            </button>
          </div>
        </div>
      )}
      <div data-testid="context-variable-form">
        <div style={formRowStyle}>
          <label htmlFor="prop-name">Name</label>
          <input
            id="prop-name"
            data-testid="context-variable-name"
            value={propName}
            onChange={(e) => setPropName(e.target.value)}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="prop-value">Value</label>
          <input
            id="prop-value"
            data-testid="context-variable-value"
            value={propValue}
            onChange={(e) => setPropValue(e.target.value)}
          />
        </div>
        <button
          type="button"
          style={primaryButtonStyle}
          data-testid="context-variable-save"
          disabled={saving}
          onClick={() => void saveProp()}
        >
          Save property
        </button>
      </div>
    </div>
  );
}
