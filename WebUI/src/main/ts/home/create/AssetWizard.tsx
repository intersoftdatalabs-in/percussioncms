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
  fetchAssetTypes,
  fetchFolderChildren,
} from "../../api/home/homeApi";
import type { AssetTypeSummary } from "../../api/home/types";
import { message, MSG } from "../../i18n/message";
import { errorStyle, formRowStyle } from "../home.styles";
import { normalizeCmsPath } from "./filenameUtils";

export interface AssetWizardProps {
  onBack: () => void;
}

/**
 * Classic asset create navigates to edit-asset with widgetId + folderPath
 * (does not POST a finished asset in the adaptor).
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
        setTypes(assetTypes.filter((t) => t.id));
        const extra = children
          .filter(
            (c) =>
              c.folder === true ||
              String(c.type ?? "").toLowerCase().includes("folder"),
          )
          .map((c) =>
            c.path ? String(c.path) : `/Assets/${c.name ?? ""}`,
          );
        setFolders(["/Assets", ...extra]);
      })
      .catch(() => setError(message(MSG.ERROR_GENERIC)))
      .finally(() => setLoading(false));
  }, []);

  const onSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!typeId || !folderPath) {
      setError(message(MSG.CREATE_VALIDATION));
      return;
    }
    const folder = normalizeCmsPath(folderPath);
    // Classic: goToLocation VIEW_EDIT_ASSET with widgetId + folderPath
    const q = new URLSearchParams({
      view: "editAsset",
      widgetId: typeId,
      folderPath: folder,
    });
    window.location.href = `/cm/app/?${q.toString()}`;
  };

  if (loading) {
    return <p role="status">{message(MSG.LOADING)}</p>;
  }

  return (
    <form data-testid="asset-wizard" onSubmit={onSubmit}>
      <p>
        <button type="button" onClick={onBack}>
          {message(MSG.CREATE_BACK)}
        </button>
      </p>
      <h2 style={{ fontSize: "1.1rem" }}>{message(MSG.CREATE_TYPE_ASSET)}</h2>

      <div style={formRowStyle}>
        <label htmlFor="aw-type">{message(MSG.CREATE_ASSET_TYPE)}</label>
        <select
          id="aw-type"
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
        <p role="alert" style={errorStyle}>
          {error}
        </p>
      )}

      <button type="submit">{message(MSG.CREATE_SUBMIT)}</button>
    </form>
  );
}
