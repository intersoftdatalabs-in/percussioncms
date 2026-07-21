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
    if (!selectedId || !contextId) {
      setProps([]);
      return;
    }
    listSiteProperties(selectedId, contextId)
      .then(setProps)
      .catch(() => setProps([]));
  }, [selectedId, contextId]);

  const selected = sites.find((s) => s.siteId === selectedId);

  async function saveProp(): Promise<void> {
    if (!selectedId || !contextId || !propName.trim()) {
      setError("Site, context, and property name are required");
      return;
    }
    setError(null);
    try {
      await putSiteProperty(selectedId, {
        name: propName.trim(),
        contextId,
        value: propValue,
      });
      setPropName("");
      setPropValue("");
      setProps(await listSiteProperties(selectedId, contextId));
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
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
        <p style={errorStyle} role="alert">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        <label>
          Site{" "}
          <select
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
      <ul style={listStyle}>
        {props.map((p) => (
          <li key={p.name} style={listItemStyle}>
            <strong>{p.name}</strong>
            <span style={{ color: "#666" }}>{p.value}</span>
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
      <div style={formRowStyle}>
        <label htmlFor="prop-name">Name</label>
        <input
          id="prop-name"
          value={propName}
          onChange={(e) => setPropName(e.target.value)}
        />
      </div>
      <div style={formRowStyle}>
        <label htmlFor="prop-value">Value</label>
        <input
          id="prop-value"
          value={propValue}
          onChange={(e) => setPropValue(e.target.value)}
        />
      </div>
      <button type="button" style={primaryButtonStyle} onClick={() => void saveProp()}>
        Save property
      </button>
    </div>
  );
}
