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
import { isSessionRedirectError } from "../../api/client";
import {
  fetchAssetTypes,
  fetchFolderChildren,
  formatApiError,
} from "../../api/home/homeApi";
import type { AssetTypeSummary } from "../../api/home/types";
import { message, MSG } from "../../i18n/message";
import {
  actionButtonStyle,
  errorStyle,
  formRowStyle,
} from "../home.styles";
import { normalizeCmsPath } from "./filenameUtils";

export interface AssetWizardProps {
  onBack: () => void;
}

/**
 * Classic asset create navigates to editAsset with widgetId + folderPath
 * (does not POST a finished asset body — editor creates on save).
 */
export function AssetWizard({ onBack }: AssetWizardProps): React.ReactElement {
  const [types, setTypes] = useState<AssetTypeSummary[]>([]);
  const [typeId, setTypeId] = useState("");
  const [folderPath, setFolderPath] = useState("/Assets");
  const [folders, setFolders] = useState<string[]>(["/Assets"]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    Promise.all([
      fetchAssetTypes(true),
      fetchFolderChildren("/Assets").catch(() => []),
    ])
      .then(([assetTypes, children]) => {
        setTypes(assetTypes);
        if (assetTypes.length === 1) {
          setTypeId(assetTypes[0].id);
        }
        const extra = children
          .filter(
            (c) =>
              c.folder === true ||
              String(c.type ?? "").toLowerCase().includes("folder") ||
              String((c as { category?: string }).category ?? "")
                .toLowerCase()
                .includes("folder"),
          )
          .map((c) =>
            normalizeCmsPath(
              c.path ? String(c.path) : `/Assets/${c.name ?? ""}`,
            ),
          )
          .filter(Boolean);
        const uniq = Array.from(new Set(["/Assets", ...extra]));
        setFolders(uniq);
      })
      .catch((err: unknown) => {
        if (isSessionRedirectError(err)) {
          return;
        }
        setError(formatApiError(err, message(MSG.ERROR_GENERIC)));
      })
      .finally(() => setLoading(false));
  }, []);

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!typeId || !folderPath) {
      setError(message(MSG.CREATE_VALIDATION));
      return;
    }
    const folder = normalizeCmsPath(folderPath);
    // Classic: VIEW_EDIT_ASSET with widgetId (string) + folderPath.
    // Prefer editAsset.jsp so SPA path fallback cannot swallow the view.
    const q = new URLSearchParams({
      widgetId: typeId,
      folderPath: folder,
    });
    window.location.href = `/cm/app/editAsset.jsp?${q.toString()}`;
  };

  if (loading) {
    return (
      <p role="status" data-testid="asset-wizard-loading">
        {message(MSG.LOADING)}
      </p>
    );
  }

  if (types.length === 0) {
    return (
      <div data-testid="asset-wizard-empty">
        <p>
          <button type="button" style={actionButtonStyle("ghost")} onClick={onBack}>
            {message(MSG.CREATE_BACK)}
          </button>
        </p>
        <p>{message(MSG.CREATE_NO_ASSET_TYPES)}</p>
      </div>
    );
  }

  return (
    <form data-testid="asset-wizard" onSubmit={onSubmit}>
      <p>
        <button type="button" style={actionButtonStyle("ghost")} onClick={onBack}>
          {message(MSG.CREATE_BACK)}
        </button>
      </p>
      <h2 style={{ fontSize: "1.1rem" }}>{message(MSG.CREATE_TYPE_ASSET)}</h2>
      <p style={{ color: "#555", fontSize: "0.9rem" }}>
        {message(MSG.CREATE_ASSET_HINT)}
      </p>

      <div style={formRowStyle}>
        <label htmlFor="aw-type">{message(MSG.CREATE_ASSET_TYPE)}</label>
        <select
          id="aw-type"
          data-testid="asset-wizard-type"
          value={typeId}
          onChange={(e) => setTypeId(e.target.value)}
          required
        >
          <option value="">{message(MSG.CREATE_SELECT)}</option>
          {types.map((t) => (
            <option key={t.id} value={t.id}>
              {t.label ?? t.name}
            </option>
          ))}
        </select>
      </div>

      <div style={formRowStyle}>
        <label htmlFor="aw-folder">{message(MSG.CREATE_FOLDER)}</label>
        <select
          id="aw-folder"
          data-testid="asset-wizard-folder"
          value={folderPath}
          onChange={(e) => setFolderPath(e.target.value)}
          required
        >
          {folders.map((f) => (
            <option key={f} value={f}>
              {f}
            </option>
          ))}
        </select>
      </div>

      {error && (
        <p role="alert" style={errorStyle} data-testid="asset-wizard-error">
          {error}
        </p>
      )}

      <button
        type="submit"
        data-testid="asset-wizard-submit"
        style={actionButtonStyle("primary")}
      >
        {message(MSG.CREATE_SUBMIT)}
      </button>
    </form>
  );
}
