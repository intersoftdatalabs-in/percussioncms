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

import React, { useEffect, useMemo, useState } from "react";
import { message, MSG } from "../../i18n/message";
import {
  buttonStyle,
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";
import {
  DATABASE_DRIVERS,
  emptyServerModel,
  FILE_DRIVERS,
  modelToSaveBody,
  serverToModel,
  type ServerEditorModel,
} from "../serverFormModel";
import { validateServerForm } from "../serverValidation";
import type { PublishServer } from "../types";
import { DatabaseDriverFields } from "./drivers/DatabaseDriverFields";
import { FileDriverFields } from "./drivers/FileDriverFields";

export interface ServerEditorProps {
  siteId: string | number;
  /** Existing server from API, or null for create */
  server: PublishServer | null;
  regions?: string[];
  isEC2?: boolean;
  onSave: (body: { serverInfo: Record<string, unknown> }, isCreate: boolean) => Promise<void>;
  onDelete?: () => Promise<void>;
  onCancel: () => void;
  onDirtyChange?: (dirty: boolean) => void;
}

export function ServerEditor({
  siteId: _siteId,
  server,
  regions = [],
  isEC2 = false,
  onSave,
  onDelete,
  onCancel,
  onDirtyChange,
}: ServerEditorProps): React.ReactElement {
  const initial = useMemo(
    () => (server ? serverToModel(server) : emptyServerModel()),
    [server],
  );
  const [model, setModel] = useState<ServerEditorModel>(initial);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const [baseline, setBaseline] = useState(() => JSON.stringify(initial));

  useEffect(() => {
    const next = server ? serverToModel(server) : emptyServerModel();
    setModel(next);
    setBaseline(JSON.stringify(next));
    setError(null);
  }, [server]);

  useEffect(() => {
    const dirty = JSON.stringify(model) !== baseline;
    onDirtyChange?.(dirty);
  }, [model, baseline, onDirtyChange]);

  function patch(partial: Partial<ServerEditorModel>): void {
    setModel((m) => ({ ...m, ...partial }));
  }

  function setProp(key: string, value: string): void {
    setModel((m) => ({
      ...m,
      properties: { ...m.properties, [key]: value },
      ...(key === "driver" ? { driver: value } : {}),
    }));
  }

  function onTypeChange(type: string): void {
    const driver = type === "Database" ? "MSSQL" : "Local";
    setModel((m) => ({
      ...m,
      type,
      driver,
      properties: { ...m.properties, driver },
    }));
  }

  function onDriverChange(driver: string): void {
    setModel((m) => ({
      ...m,
      driver,
      properties: { ...m.properties, driver },
    }));
  }

  async function handleSave(): Promise<void> {
    const validation = validateServerForm({
      serverName: model.serverName,
      driver: model.driver,
      properties: model.properties,
    });
    if (!validation.valid) {
      setError(`Missing required: ${validation.missing.join(", ")}`);
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const body = modelToSaveBody(model);
      const isCreate = !model.serverId;
      await onSave(body, isCreate);
      setBaseline(JSON.stringify(model));
      onDirtyChange?.(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    } finally {
      setSaving(false);
    }
  }

  async function handleDelete(): Promise<void> {
    if (!onDelete || !model.serverId) {
      return;
    }
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_DELETE_SERVER))) {
      return;
    }
    setSaving(true);
    try {
      await onDelete();
      onDirtyChange?.(false);
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    } finally {
      setSaving(false);
    }
  }

  const drivers =
    model.type === "Database" ? DATABASE_DRIVERS : FILE_DRIVERS;

  return (
    <div data-testid="publish-server-editor">
      <h3 style={{ fontSize: "1.05rem" }}>
        {model.serverId
          ? message(MSG.PUBLISH_EDIT_SERVER)
          : message(MSG.PUBLISH_ADD_SERVER)}
      </h3>

      <div style={formRowStyle}>
        <label htmlFor="serverName">* {message(MSG.PUBLISH_SERVER_NAME)}</label>
        <input
          id="serverName"
          value={model.serverName}
          onChange={(e) => patch({ serverName: e.target.value })}
          aria-required
        />
      </div>

      <div style={formRowStyle}>
        <label htmlFor="serverType">{message(MSG.PUBLISH_SERVER_TYPE)}</label>
        <select
          id="serverType"
          value={model.serverType}
          onChange={(e) => patch({ serverType: e.target.value })}
        >
          <option value="PRODUCTION">Production</option>
          <option value="STAGING">Staging</option>
        </select>
      </div>

      <div style={formRowStyle}>
        <label htmlFor="publishType">{message(MSG.PUBLISH_DELIVERY_TYPE)}</label>
        <select
          id="publishType"
          value={model.type}
          onChange={(e) => onTypeChange(e.target.value)}
        >
          <option value="File">File</option>
          <option value="Database">Database</option>
        </select>
      </div>

      <div style={formRowStyle}>
        <label htmlFor="driver">{message(MSG.PUBLISH_DRIVER)}</label>
        <select
          id="driver"
          value={model.driver}
          onChange={(e) => onDriverChange(e.target.value)}
        >
          {drivers.map((d) => (
            <option key={d} value={d}>
              {d}
            </option>
          ))}
        </select>
      </div>

      {model.type === "File" ? (
        <FileDriverFields
          driver={model.driver}
          properties={model.properties}
          onChange={setProp}
          regions={isEC2 || regions.length > 0 ? regions : []}
        />
      ) : (
        <DatabaseDriverFields
          driver={model.driver}
          properties={model.properties}
          onChange={setProp}
        />
      )}

      <div style={formRowStyle}>
        <label>
          <input
            type="checkbox"
            checked={model.isDefault}
            disabled={model.serverType === "STAGING"}
            onChange={(e) => patch({ isDefault: e.target.checked })}
          />{" "}
          {message(MSG.PUBLISH_SET_DEFAULT)}
        </label>
      </div>
      <div style={formRowStyle}>
        <label>
          <input
            type="checkbox"
            checked={model.ignoreUnModifiedAssets}
            onChange={(e) => patch({ ignoreUnModifiedAssets: e.target.checked })}
          />{" "}
          {message(MSG.PUBLISH_IGNORE_UNMODIFIED)}
        </label>
      </div>
      <div style={formRowStyle}>
        <label>
          <input
            type="checkbox"
            checked={model.publishRelatedItems}
            onChange={(e) => patch({ publishRelatedItems: e.target.checked })}
          />{" "}
          {message(MSG.PUBLISH_RELATED_ITEMS)}
        </label>
      </div>

      {error && (
        <p style={errorStyle} role="alert">
          {error}
        </p>
      )}

      <div style={toolbarStyle}>
        <button
          type="button"
          style={primaryButtonStyle}
          disabled={saving}
          onClick={() => void handleSave()}
        >
          {message(MSG.PUBLISH_SAVE)}
        </button>
        <button type="button" style={buttonStyle} onClick={onCancel}>
          {message(MSG.PUBLISH_BACK)}
        </button>
        {model.serverId && onDelete && (
          <button
            type="button"
            style={buttonStyle}
            disabled={saving}
            onClick={() => void handleDelete()}
          >
            {message(MSG.PUBLISH_DELETE_SERVER)}
          </button>
        )}
      </div>
    </div>
  );
}
