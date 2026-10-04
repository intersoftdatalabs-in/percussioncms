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

import React, { useEffect, useMemo, useRef, useState } from "react";
import { useDialogEscape } from "../architecture/useDialogEscape";
import type { FolderAllowedSiteChoice } from "../api/contentExplorer/folderAllowedSitesApi";
import { message } from "../i18n/message";
import { EXPLORER_MSG } from "./messages";
import { canonicalAllowedSites } from "./setFolderAllowedSites";

const FOCUSABLE =
  'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])';

export interface SetFolderAllowedSitesDialogProps {
  choices: FolderAllowedSiteChoice[];
  currentSites: string;
  busy: boolean;
  error: string;
  onSave: (siteIds: string[]) => void;
  onCancel: () => void;
}

export function SetFolderAllowedSitesDialog({
  choices,
  currentSites,
  busy,
  error,
  onSave,
  onCancel,
}: SetFolderAllowedSitesDialogProps): React.ReactElement {
  const current = canonicalAllowedSites(currentSites);
  const currentCanonical = current.ok ? current.canonical : "";
  const currentIds = currentCanonical ? currentCanonical.split(",") : [];
  const rows = useMemo(() => {
    const known = currentCanonical ? currentCanonical.split(",") : [];
    const next = [...choices];
    for (const id of known) {
      if (!next.some((row) => row.id === id)) {
        next.push({ id, name: id });
      }
    }
    return next;
  }, [choices, currentCanonical]);
  const [selected, setSelected] = useState<string[]>(currentIds);
  const rootRef = useRef<HTMLDivElement>(null);
  const firstRef = useRef<HTMLInputElement>(null);

  useDialogEscape(true, busy, onCancel);

  useEffect(() => {
    const root = rootRef.current;
    firstRef.current?.focus();
    if (!root) {
      return;
    }
    const onKey = (ev: KeyboardEvent) => {
      if (ev.key !== "Tab") {
        return;
      }
      const list = Array.from(root.querySelectorAll<HTMLElement>(FOCUSABLE));
      if (list.length === 0) {
        return;
      }
      const first = list[0];
      const last = list[list.length - 1];
      if (ev.shiftKey && document.activeElement === first) {
        ev.preventDefault();
        last.focus();
      } else if (!ev.shiftKey && document.activeElement === last) {
        ev.preventDefault();
        first.focus();
      }
    };
    root.addEventListener("keydown", onKey);
    return () => root.removeEventListener("keydown", onKey);
  }, []);

  function toggle(id: string): void {
    setSelected((prev) =>
      prev.includes(id) ? prev.filter((value) => value !== id) : [...prev, id],
    );
  }

  return (
    <div
      ref={rootRef}
      role="dialog"
      aria-modal="true"
      aria-labelledby="explorer-set-folder-allowed-sites-title"
      data-testid="explorer-set-folder-allowed-sites-dialog"
      data-current-sites={currentCanonical}
      style={{
        position: "fixed",
        inset: 0,
        background: "rgba(15,23,42,0.45)",
        display: "flex",
        alignItems: "center",
        justifyContent: "center",
        zIndex: 40,
      }}
    >
      <form
        style={{
          background: "#fff",
          color: "#0f172a",
          minWidth: 280,
          maxWidth: 440,
          padding: 16,
          borderRadius: 8,
          boxShadow: "0 8px 24px rgba(0,0,0,0.18)",
        }}
        onSubmit={(e) => {
          e.preventDefault();
          if (!busy) {
            onSave(selected);
          }
        }}
      >
        <h2 id="explorer-set-folder-allowed-sites-title" style={{ fontSize: 16, margin: "0 0 12px" }}>
          {message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_TITLE)}
        </h2>
        <fieldset
          style={{ border: "1px solid #e2e8f0", borderRadius: 6, margin: 0, padding: 8 }}
          disabled={busy}
        >
          <legend style={{ fontSize: 13, padding: "0 4px" }}>
            {message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_LABEL)}
          </legend>
          <div
            role="group"
            aria-describedby="explorer-set-folder-allowed-sites-hint"
            data-testid="explorer-set-folder-allowed-sites-list"
          >
            {rows.map((row, index) => {
              const inputId = `explorer-set-folder-allowed-site-${row.id}`;
              const checked = selected.includes(row.id);
              const isCurrent = currentIds.includes(row.id);
              return (
                <label key={row.id} htmlFor={inputId} style={{ display: "block", fontSize: 13, marginTop: 6 }}>
                  <input
                    ref={index === 0 ? firstRef : undefined}
                    id={inputId}
                    type="checkbox"
                    data-testid={inputId}
                    checked={checked}
                    disabled={busy}
                    onChange={() => toggle(row.id)}
                  />{" "}
                  {row.name}
                  {isCurrent ? ` (${message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_CURRENT)})` : ""}
                </label>
              );
            })}
          </div>
        </fieldset>
        <p id="explorer-set-folder-allowed-sites-hint" style={{ fontSize: 12, margin: "8px 0 0" }}>
          {message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_HINT)}
        </p>
        {error ? (
          <p data-testid="explorer-set-folder-allowed-sites-dialog-error" role="alert">
            {error}
          </p>
        ) : null}
        <div style={{ display: "flex", gap: 8, marginTop: 16, justifyContent: "flex-end" }}>
          <button
            type="button"
            data-testid="explorer-set-folder-allowed-sites-cancel"
            disabled={busy}
            onClick={onCancel}
          >
            {message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_CANCEL)}
          </button>
          <button type="submit" data-testid="explorer-set-folder-allowed-sites-save" disabled={busy}>
            {message(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_SAVE)}
          </button>
        </div>
      </form>
    </div>
  );
}
