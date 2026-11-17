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
import { isApiError } from "../api/client";
import {
  isApiKeyNameReady,
  listApiKeyAssignments,
  listApiKeyScopePresets,
  listApiKeyScopes,
  matchPresetForScopes,
  normalizeApiKeyName,
  saveApiKeyScopes,
  scopeGroupRank,
  type ApiKeyAssignment,
  type ApiKeyScope,
  type ApiKeyScopePreset,
} from "../api/developer/apiKeysApi";
import { CatalogHint, CatalogStatus } from "./CatalogTable";
import { catalogColors, errorAlert } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

const rowButtonStyle = (active: boolean): React.CSSProperties => ({
  display: "block",
  width: "100%",
  textAlign: "left",
  padding: "8px 10px",
  border: `1px solid ${active ? catalogColors.accent : catalogColors.softBorder}`,
  borderRadius: "4px",
  background: active ? "#eef4ff" : catalogColors.surface,
  cursor: "pointer",
  font: "inherit",
});

const presetButtonStyle = (active: boolean): React.CSSProperties => ({
  padding: "6px 12px",
  borderRadius: "4px",
  border: `1px solid ${active ? catalogColors.accent : catalogColors.softBorder}`,
  background: active ? catalogColors.accent : catalogColors.surface,
  color: active ? "#fff" : catalogColors.text,
  cursor: "pointer",
  font: "inherit",
  fontWeight: active ? 600 : 400,
});

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  boxSizing: "border-box",
};

function saveErrMsg(err: unknown): string {
  if (isApiError(err)) {
    if (err.status === 403) return DEV_MSG.API_KEYS_SAVE_FORBIDDEN;
    if (err.status === 400) return DEV_MSG.API_KEYS_SAVE_INVALID;
    if (err.status === 404) return DEV_MSG.API_KEYS_SAVE_NOT_FOUND;
  }
  return panelErrMsg(err, DEV_MSG.API_KEYS_SAVE_ERROR);
}

/**
 * Developer API key scope editor (#6481).
 *
 * Admin list of named API keys plus a least-privilege scope editor: preset
 * shortcuts select the smallest scope set for the job, individual scopes stay
 * toggleable, and Save PUTs the set only on explicit action (Cancel/Reset never
 * calls the server).
 */
export function ApiKeysPanel(): React.ReactElement {
  const [scopes, setScopes] = useState<ApiKeyScope[] | null>(null);
  const [presets, setPresets] = useState<ApiKeyScopePreset[] | null>(null);
  const [keys, setKeys] = useState<ApiKeyAssignment[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<string | null>(null);
  const [draft, setDraft] = useState<Set<string> | null>(null);
  const [draftLabel, setDraftLabel] = useState("");
  const [saving, setSaving] = useState(false);
  const [saveError, setSaveError] = useState<string | null>(null);
  const [savedNote, setSavedNote] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [newName, setNewName] = useState("");
  const [newLabel, setNewLabel] = useState("");
  const [newPresetId, setNewPresetId] = useState<string>("readonly");

  useEffect(() => {
    let cancelled = false;
    Promise.all([listApiKeyScopes(), listApiKeyScopePresets(), listApiKeyAssignments()])
      .then(([scopeList, presetList, keyList]) => {
        if (cancelled) return;
        setScopes(scopeList);
        setPresets(presetList);
        setKeys(keyList);
      })
      .catch((e: unknown) => {
        if (cancelled) return;
        if (isApiError(e) && e.status === 403) {
          setError(DEV_MSG.API_KEYS_FORBIDDEN);
        } else {
          setError(panelErrMsg(e, DEV_MSG.API_KEYS_ERROR));
        }
      });
    return () => {
      cancelled = true;
    };
  }, []);

  const stored = useMemo(
    () => keys?.find((k) => k.name === selected) ?? null,
    [keys, selected],
  );

  const openKey = useCallback(
    (name: string) => {
      const found = keys?.find((k) => k.name === name) ?? null;
      setSelected(name);
      setDraft(new Set(found?.scopes ?? []));
      setDraftLabel(found?.displayName ?? "");
      setSaveError(null);
      setSavedNote(null);
      setCreating(false);
    },
    [keys],
  );

  const groupedScopes = useMemo(() => {
    const groups = new Map<string, ApiKeyScope[]>();
    for (const scope of scopes ?? []) {
      const list = groups.get(scope.group) ?? [];
      list.push(scope);
      groups.set(scope.group, list);
    }
    return [...groups.entries()].sort((a, b) => scopeGroupRank(a[0]) - scopeGroupRank(b[0]));
  }, [scopes]);

  const draftList = useMemo(() => (draft ? [...draft].sort() : []), [draft]);
  const draftPresetId = useMemo(
    () => (presets ? matchPresetForScopes(presets, draftList) : null),
    [presets, draftList],
  );
  const dirty = useMemo(() => {
    if (!stored || !draft) return false;
    const base = new Set(stored.scopes);
    if (base.size !== draft.size) return true;
    for (const s of draft) {
      if (!base.has(s)) return true;
    }
    return (stored.displayName ?? "") !== draftLabel;
  }, [stored, draft, draftLabel]);

  const toggleScope = useCallback((key: string) => {
    setDraft((prev) => {
      const next = new Set(prev ?? []);
      if (next.has(key)) {
        next.delete(key);
      } else {
        next.add(key);
      }
      return next;
    });
    setSavedNote(null);
    setSaveError(null);
  }, []);

  const applyPreset = useCallback(
    (presetId: string) => {
      const preset = presets?.find((p) => p.id === presetId);
      if (!preset) return;
      setDraft(new Set(preset.scopes));
      setSavedNote(null);
      setSaveError(null);
    },
    [presets],
  );

  const resetDraft = useCallback(() => {
    if (!stored) return;
    setDraft(new Set(stored.scopes));
    setDraftLabel(stored.displayName ?? "");
    setSaveError(null);
    setSavedNote(null);
  }, [stored]);

  async function persist(name: string, label: string, scopeSet: Set<string>): Promise<boolean> {
    setSaving(true);
    setSaveError(null);
    setSavedNote(null);
    try {
      const saved = await saveApiKeyScopes(name, {
        scopes: [...scopeSet],
        displayName: label.trim() ? label.trim() : undefined,
      });
      setKeys((prev) => {
        const rest = (prev ?? []).filter((k) => k.name !== saved.name);
        return [...rest, saved].sort((a, b) =>
          a.name.localeCompare(b.name, undefined, { sensitivity: "base" }),
        );
      });
      setSelected(saved.name);
      setDraft(new Set(saved.scopes));
      setDraftLabel(saved.displayName ?? "");
      setSavedNote(DEV_MSG.API_KEYS_SAVED);
      setCreating(false);
      setNewName("");
      setNewLabel("");
      return true;
    } catch (e: unknown) {
      setSaveError(saveErrMsg(e));
      return false;
    } finally {
      setSaving(false);
    }
  }

  if (error) {
    return (
      <CatalogStatus testId="developer-api-keys-error" error>
        {error}
      </CatalogStatus>
    );
  }
  if (scopes == null || presets == null || keys == null) {
    return (
      <CatalogStatus testId="developer-api-keys-loading">
        {DEV_MSG.API_KEYS_LOADING}
      </CatalogStatus>
    );
  }

  return (
    <div data-testid="developer-api-keys-panel">
      <CatalogHint>{DEV_MSG.API_KEYS_HINT}</CatalogHint>

      <div
        aria-live="polite"
        data-testid="developer-api-keys-status"
        style={{ minHeight: "1.5em", marginBottom: "8px" }}
      >
        {saveError ? (
          <span data-testid="developer-api-keys-save-error" style={errorAlert} role="alert">
            {saveError}
          </span>
        ) : savedNote ? (
          <span data-testid="developer-api-keys-saved">{savedNote}</span>
        ) : dirty ? (
          // mkd-code-review[secrets.heuristic]: adjudicated FP in #6481 (precedent #6341) — React test ID for the scope editor, not credential material.
          <span data-testid="developer-api-keys-dirty">{DEV_MSG.API_KEYS_DIRTY}</span>
        ) : null}
      </div>

      <div style={{ display: "flex", gap: "24px", flexWrap: "wrap" }}>
        <section aria-label={DEV_MSG.API_KEYS_COL_NAME} style={{ minWidth: "240px", flex: "0 1 300px" }}>
          {keys.length === 0 && !creating ? (
            <div style={{ marginBottom: "12px" }}>
              <CatalogStatus testId="developer-api-keys-empty">
                {DEV_MSG.API_KEYS_EMPTY}
              </CatalogStatus>
              <p style={{ color: catalogColors.muted, fontSize: "0.9em" }}>
                {DEV_MSG.API_KEYS_EMPTY_HINT}
              </p>
            </div>
          ) : (
            <ul
              data-testid="developer-api-keys-list"
              style={{ listStyle: "none", padding: 0, margin: "0 0 12px" }}
            >
              {keys.map((k) => {
                const active = k.name === selected && !creating;
                return (
                  <li key={k.name} style={{ marginBottom: "6px" }}>
                    <button
                      type="button"
                      data-testid={`developer-api-keys-row-${k.name}`}
                      data-apikey-name={k.name}
                      aria-current={active ? "true" : undefined}
                      onClick={() => openKey(k.name)}
                      onKeyDown={(ev) => {
                        if (ev.key === "Enter" || ev.key === " ") {
                          ev.preventDefault();
                          openKey(k.name);
                        }
                      }}
                      style={rowButtonStyle(active)}
                    >
                      <span style={{ fontWeight: 600 }}>{k.displayName || k.name}</span>
                      {k.displayName ? (
                        <span style={{ color: catalogColors.muted }}> — {k.name}</span>
                      ) : null}
                      <br />
                      <span
                        data-testid={`developer-api-keys-row-preset-${k.name}`}
                        style={{ color: catalogColors.muted, fontSize: "0.9em" }}
                      >
                        {k.presetId ?? DEV_MSG.API_KEYS_PRESET_CUSTOM} · {k.scopes.length}
                      </span>
                    </button>
                  </li>
                );
              })}
            </ul>
          )}

          {creating ? (
            <form
              data-testid="developer-api-keys-new-form"
              aria-label={DEV_MSG.API_KEYS_NEW_TITLE}
              onSubmit={(ev) => {
                ev.preventDefault();
                const name = normalizeApiKeyName(newName);
                if (!isApiKeyNameReady(name)) {
                  setSaveError(DEV_MSG.API_KEYS_NAME_REQUIRED);
                  return;
                }
                const preset = presets.find((p) => p.id === newPresetId);
                void persist(name, newLabel, new Set(preset?.scopes ?? []));
              }}
              style={{
                border: `1px solid ${catalogColors.softBorder}`,
                borderRadius: "4px",
                padding: "12px",
              }}
            >
              <h3 style={{ marginTop: 0 }}>{DEV_MSG.API_KEYS_NEW_TITLE}</h3>
              <label style={{ display: "block", marginBottom: "8px" }}>
                {DEV_MSG.API_KEYS_NEW_NAME}
                <input
                  data-testid="developer-api-keys-new-name"
                  value={newName}
                  onChange={(ev) => setNewName(ev.target.value)}
                  style={inputStyle}
                  autoComplete="off"
                />
              </label>
              <label style={{ display: "block", marginBottom: "8px" }}>
                {DEV_MSG.API_KEYS_NEW_LABEL}
                <input
                  data-testid="developer-api-keys-new-label"
                  value={newLabel}
                  onChange={(ev) => setNewLabel(ev.target.value)}
                  style={inputStyle}
                  autoComplete="off"
                />
              </label>
              <label style={{ display: "block", marginBottom: "8px" }}>
                {DEV_MSG.API_KEYS_EDIT_PRESET}
                <select
                  data-testid="developer-api-keys-new-preset"
                  value={newPresetId}
                  onChange={(ev) => setNewPresetId(ev.target.value)}
                  style={inputStyle}
                >
                  {presets.map((preset) => (
                    <option key={preset.id} value={preset.id}>
                      {preset.name} — {preset.scopes.length}
                    </option>
                  ))}
                </select>
              </label>
              <div style={{ display: "flex", gap: "8px" }}>
                <button
                  type="submit"
                  data-testid="developer-api-keys-new-save"
                  disabled={saving}
                  style={presetButtonStyle(false)}
                >
                  {DEV_MSG.API_KEYS_NEW_SAVE}
                </button>
                <button
                  type="button"
                  data-testid="developer-api-keys-new-cancel"
                  onClick={() => {
                    setCreating(false);
                    setNewName("");
                    setNewLabel("");
                    setSaveError(null);
                  }}
                  style={presetButtonStyle(false)}
                >
                  {DEV_MSG.API_KEYS_NEW_CANCEL}
                </button>
              </div>
            </form>
          ) : (
            <button
              type="button"
              data-testid="developer-api-keys-new"
              onClick={() => {
                setCreating(true);
                setSelected(null);
                setDraft(null);
                setSaveError(null);
                setSavedNote(null);
              }}
              style={presetButtonStyle(false)}
            >
              {DEV_MSG.API_KEYS_NEW_TITLE}
            </button>
          )}
        </section>

        {selected && stored && draft && !creating ? (
          <section
            aria-label={stored.displayName || stored.name}
            data-testid="developer-api-keys-editor"
            data-apikey-editing={stored.name}
            style={{ flex: "1 1 420px" }}
          >
            <h3 style={{ marginTop: 0 }} data-testid="developer-api-keys-editor-title">
              {stored.displayName ? `${stored.displayName} (${stored.name})` : stored.name}
            </h3>
            <label style={{ display: "block", marginBottom: "12px", maxWidth: "24rem" }}>
              {DEV_MSG.API_KEYS_NEW_LABEL}
              <input
                data-testid="developer-api-keys-label"
                value={draftLabel}
                onChange={(ev) => {
                  setDraftLabel(ev.target.value);
                  setSavedNote(null);
                }}
                style={inputStyle}
                autoComplete="off"
              />
            </label>

            <fieldset style={{ border: `1px solid ${catalogColors.softBorder}`, borderRadius: "4px", marginBottom: "12px" }}>
              <legend>{DEV_MSG.API_KEYS_EDIT_PRESET}</legend>
              <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }} role="group" aria-label={DEV_MSG.API_KEYS_EDIT_PRESET}>
                {presets.map((preset) => {
                  const active = draftPresetId === preset.id;
                  return (
                    <button
                      key={preset.id}
                      type="button"
                      data-testid={`developer-api-keys-preset-${preset.id}`}
                      data-preset-active={active ? "true" : "false"}
                      title={preset.description ?? preset.name}
                      aria-pressed={active}
                      onClick={() => applyPreset(preset.id)}
                      style={presetButtonStyle(active)}
                    >
                      {preset.name}
                    </button>
                  );
                })}
                <span
                  data-testid="developer-api-keys-preset-current"
                  style={{ alignSelf: "center", color: catalogColors.muted, fontSize: "0.9em" }}
                >
                  {draftPresetId ?? DEV_MSG.API_KEYS_PRESET_CUSTOM}
                </span>
              </div>
            </fieldset>

            <fieldset style={{ border: `1px solid ${catalogColors.softBorder}`, borderRadius: "4px", marginBottom: "12px" }}>
              <legend>{DEV_MSG.API_KEYS_EDIT_SCOPES}</legend>
              {groupedScopes.map(([group, list]) => (
                <fieldset
                  key={group}
                  style={{ border: "none", padding: 0, margin: "0 0 8px" }}
                >
                  <legend
                    style={{ fontWeight: 600, textTransform: "capitalize", padding: 0 }}
                    data-testid={`developer-api-keys-group-${group}`}
                  >
                    {group}
                  </legend>
                  {list.map((scope) => {
                    const checked = draft.has(scope.key);
                    return (
                      <label
                        key={scope.key}
                        style={{ display: "block", padding: "2px 0", cursor: "pointer" }}
                      >
                        <input
                          type="checkbox"
                          data-testid={`developer-api-keys-scope-${scope.key}`}
                          checked={checked}
                          onChange={() => toggleScope(scope.key)}
                        />{" "}
                        <strong>{scope.name}</strong>{" "}
                        <code style={{ fontSize: "0.85em" }}>{scope.key}</code>
                        {scope.description ? (
                          <span style={{ color: catalogColors.muted }}> — {scope.description}</span>
                        ) : null}
                      </label>
                    );
                  })}
                </fieldset>
              ))}
            </fieldset>

            <div style={{ display: "flex", gap: "8px" }}>
              <button
                type="button"
                data-testid="developer-api-keys-save"
                disabled={!dirty || saving}
                onClick={() => void persist(stored.name, draftLabel, draft)}
                style={presetButtonStyle(false)}
              >
                {DEV_MSG.API_KEYS_SAVE}
              </button>
              <button
                type="button"
                data-testid="developer-api-keys-reset"
                disabled={!dirty || saving}
                onClick={resetDraft}
                style={presetButtonStyle(false)}
              >
                {DEV_MSG.API_KEYS_RESET}
              </button>
            </div>
          </section>
        ) : null}
      </div>
    </div>
  );
}
