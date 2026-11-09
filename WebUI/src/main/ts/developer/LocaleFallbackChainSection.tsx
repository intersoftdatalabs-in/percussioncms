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
import { isApiError } from "../api/client";
import { normalizeLanguageString, updateLocale } from "../api/developer/localesApi";
import type { LocaleDetail } from "../api/developer/types";
import { catalogColors } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import {
  isFallbackChainCode,
  localeFallbackChainWrite,
  savedLocaleFallbackChain,
  storedLocaleFallbackChain,
} from "./localeFallbackChain";
import { LOC_FALLBACK_MSG } from "./localeFallbackChainMessages";

const inputStyle: React.CSSProperties = {
  padding: "6px 8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
};

const smallBtnStyle: React.CSSProperties = {
  padding: "6px 10px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  background: catalogColors.surface,
  cursor: "pointer",
};

function saveFallback(err: unknown): string {
  if (isApiError(err) && err.status === 400) return LOC_FALLBACK_MSG.INVALID;
  if (isApiError(err) && err.status === 403) return LOC_FALLBACK_MSG.FORBIDDEN;
  if (isApiError(err) && err.status === 409) return LOC_FALLBACK_MSG.CONFLICT;
  return LOC_FALLBACK_MSG.ERROR;
}

/**
 * Per-locale fallback chain inside the locale editor (#5788).
 *
 * <p>Shows this locale's stored ordered chain and saves edits with a
 * fallback-chain-only locale PUT ({@code format.fallbackChain} only), so the
 * locale's language, label, description, status, base flag, and every other
 * format field stay. Removing every entry and saving clears only this
 * locale's chain. Unknown entries are rejected server-side with a 400.</p>
 */
export function LocaleFallbackChainSection({
  languageString,
  initialChain,
  detail,
}: {
  languageString: string;
  initialChain: string[];
  /** Full locale detail for save-accept checks (label/description/status/format stay). */
  detail: LocaleDetail;
}): React.ReactElement {
  const locale = normalizeLanguageString(languageString);
  const [chain, setChain] = useState<string[]>(initialChain || []);
  const [draft, setDraft] = useState<string[]>([]);
  const [addInput, setAddInput] = useState("");
  const [editing, setEditing] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const inflight = useRef(false);
  const mountedRef = useRef(true);

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  // State resets on locale navigation via the parent key={...languageString}.
  function startEdit(): void {
    if (busy || inflight.current) return;
    setDraft([...chain]);
    setAddInput("");
    setError(null);
    setNotice(null);
    setEditing(true);
  }

  function cancelEdit(): void {
    if (busy || inflight.current) return;
    setEditing(false);
    setDraft([]);
    setAddInput("");
    setError(null);
  }

  function addEntry(): void {
    const code = normalizeLanguageString(addInput);
    if (!code) {
      setAddInput("");
      return;
    }
    if (!isFallbackChainCode(code)) {
      setError(LOC_FALLBACK_MSG.INVALID);
      setNotice(null);
      return;
    }
    if (code === locale) {
      setError(LOC_FALLBACK_MSG.INVALID);
      setNotice(null);
      return;
    }
    setError(null);
    setNotice(null);
    setDraft((prev) => (prev.includes(code) ? prev : [...prev, code]));
    setAddInput("");
  }

  function removeEntry(index: number): void {
    setError(null);
    setNotice(null);
    setDraft((prev) => prev.filter((_, i) => i !== index));
  }

  function moveEntry(index: number, delta: -1 | 1): void {
    setError(null);
    setNotice(null);
    setDraft((prev) => {
      const next = [...prev];
      const target = index + delta;
      if (target < 0 || target >= next.length) {
        return prev;
      }
      const [entry] = next.splice(index, 1);
      next.splice(target, 0, entry);
      return next;
    });
  }

  async function handleSave(): Promise<void> {
    if (!locale || !editing || busy || inflight.current) {
      return;
    }
    const previousChain = chain;
    const sent = localeFallbackChainWrite(
      { languageString: locale, format: { fallbackChain: previousChain } },
      draft,
    );
    const previous: LocaleDetail = {
      ...detail,
      languageString: locale,
      format: { ...(detail.format || {}), fallbackChain: previousChain },
    };
    if (sent === "invalid") {
      setError(LOC_FALLBACK_MSG.INVALID);
      setNotice(null);
      return;
    }
    if (sent === "unchanged") {
      setEditing(false);
      setDraft([]);
      setAddInput("");
      setError(null);
      return;
    }
    inflight.current = true;
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const saved = await updateLocale(locale, sent);
      if (!mountedRef.current) return;
      const accepted = savedLocaleFallbackChain(sent, previous, saved);
      if (accepted == null) {
        setChain(previousChain);
        setError(LOC_FALLBACK_MSG.ERROR);
        setNotice(null);
        return;
      }
      setChain(accepted);
      setEditing(false);
      setDraft([]);
      setAddInput("");
      setNotice(accepted.length > 0 ? LOC_FALLBACK_MSG.SAVED : LOC_FALLBACK_MSG.CLEARED);
    } catch (err: unknown) {
      if (!mountedRef.current) return;
      setChain(previousChain);
      setError(panelErrMsg(err, saveFallback(err)));
      setNotice(null);
    } finally {
      inflight.current = false;
      if (mountedRef.current) {
        setBusy(false);
      }
    }
  }

  if (!locale) {
    return (
      <section data-testid="developer-loc-set-fallback" aria-label={LOC_FALLBACK_MSG.ACTION}>
        <div role="alert" data-testid="developer-loc-set-fallback-error">
          {LOC_FALLBACK_MSG.ERROR}
        </div>
      </section>
    );
  }

  return (
    <section
      data-testid="developer-loc-set-fallback"
      aria-label={LOC_FALLBACK_MSG.ACTION}
      style={{ marginBottom: "16px" }}
    >
      <h3 style={{ fontSize: "1rem", marginBottom: "8px" }}>{LOC_FALLBACK_MSG.ACTION}</h3>
      <p style={{ color: catalogColors.muted, marginTop: 0, fontSize: "0.85rem" }}>
        {LOC_FALLBACK_MSG.HINT}
      </p>
      {chain.length > 0 ? (
        <ol
          data-testid="developer-loc-set-fallback-text"
          data-loc-fallback={chain.join(",")}
          style={{ marginTop: 0, fontFamily: "monospace" }}
        >
          {chain.map((code) => (
            <li key={code}>{code}</li>
          ))}
        </ol>
      ) : (
        <p
          data-testid="developer-loc-set-fallback-text"
          data-loc-fallback=""
          style={{ marginTop: 0, color: catalogColors.muted }}
        >
          {LOC_FALLBACK_MSG.EMPTY}
        </p>
      )}
      {error ? (
        <div
          role="alert"
          data-testid="developer-loc-set-fallback-error"
          style={{ color: catalogColors.error, marginBottom: "8px" }}
        >
          {error}
        </div>
      ) : null}
      {notice ? (
        <div data-testid="developer-loc-set-fallback-notice" style={{ color: "#276749", marginBottom: "8px" }}>
          {notice}
        </div>
      ) : null}
      {editing ? (
        <div data-testid="developer-loc-set-fallback-editor">
          <ol style={{ marginTop: 0, paddingLeft: "1.25rem" }}>
            {draft.map((code, index) => (
              <li key={`${code}-${index}`} style={{ marginBottom: "4px" }}>
                <span style={{ fontFamily: "monospace" }}>{code}</span>{" "}
                <button
                  type="button"
                  data-testid={`developer-loc-set-fallback-up-${index}`}
                  aria-label={`${LOC_FALLBACK_MSG.MOVE_UP} ${code}`}
                  disabled={busy || index === 0}
                  onClick={() => moveEntry(index, -1)}
                  style={smallBtnStyle}
                >
                  ↑
                </button>{" "}
                <button
                  type="button"
                  data-testid={`developer-loc-set-fallback-down-${index}`}
                  aria-label={`${LOC_FALLBACK_MSG.MOVE_DOWN} ${code}`}
                  disabled={busy || index === draft.length - 1}
                  onClick={() => moveEntry(index, 1)}
                  style={smallBtnStyle}
                >
                  ↓
                </button>{" "}
                <button
                  type="button"
                  data-testid={`developer-loc-set-fallback-remove-${index}`}
                  aria-label={`${LOC_FALLBACK_MSG.REMOVE} ${code}`}
                  disabled={busy}
                  onClick={() => removeEntry(index)}
                  style={smallBtnStyle}
                >
                  ×
                </button>
              </li>
            ))}
          </ol>
          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap", marginBottom: "8px" }}>
            <label htmlFor="loc-set-fallback-input" style={{ alignSelf: "center" }}>
              {LOC_FALLBACK_MSG.FIELD}
            </label>
            <input
              id="loc-set-fallback-input"
              data-testid="developer-loc-set-fallback-input"
              style={inputStyle}
              value={addInput}
              disabled={busy}
              placeholder={LOC_FALLBACK_MSG.ADD_PLACEHOLDER}
              onChange={(e) => setAddInput(e.target.value)}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  addEntry();
                }
              }}
            />
            <button
              type="button"
              data-testid="developer-loc-set-fallback-add"
              disabled={busy}
              onClick={addEntry}
              style={smallBtnStyle}
            >
              {LOC_FALLBACK_MSG.ADD}
            </button>
          </div>
          <div style={{ display: "flex", gap: "8px", flexWrap: "wrap" }}>
            <button
              type="button"
              data-testid="developer-loc-set-fallback-save"
              aria-label={LOC_FALLBACK_MSG.SAVE}
              disabled={busy}
              onClick={() => void handleSave()}
              style={{
                padding: "8px 16px",
                background: busy ? catalogColors.disabled : catalogColors.accent,
                color: "#fff",
                border: "none",
                borderRadius: "4px",
                cursor: busy ? "wait" : "pointer",
              }}
            >
              {LOC_FALLBACK_MSG.SAVE}
            </button>
            <button
              type="button"
              data-testid="developer-loc-set-fallback-cancel"
              disabled={busy}
              onClick={cancelEdit}
              style={{
                ...smallBtnStyle,
                padding: "8px 16px",
              }}
            >
              {LOC_FALLBACK_MSG.CANCEL}
            </button>
          </div>
        </div>
      ) : (
        <button
          type="button"
          data-testid="developer-loc-set-fallback-edit"
          onClick={startEdit}
          style={{
            padding: "8px 16px",
            background: catalogColors.accent,
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          {LOC_FALLBACK_MSG.ACTION}
        </button>
      )}
    </section>
  );
}
