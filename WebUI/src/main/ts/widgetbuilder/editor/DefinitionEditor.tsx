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

import React, { useState } from "react";
import { message } from "../../i18n/message";
import type { WidgetDefinition, WidgetField } from "../types";

const K = {
  SAVE: "perc.ui.widgetbuilder.modern@Save",
  VALIDATE: "perc.ui.widgetbuilder.modern@Validate",
  CANCEL: "perc.ui.widgetbuilder.modern@Cancel",
  ADD_FIELD: "perc.ui.widgetbuilder.modern@Add Field",
  LABEL_REQUIRED: "perc.ui.widgetbuilder.modern@Label Required",
  PREFIX_REQUIRED: "perc.ui.widgetbuilder.modern@Prefix Required",
};

export interface DefinitionEditorProps {
  value: WidgetDefinition;
  busy?: boolean;
  messages?: string[];
  status?: string | null;
  onChange: (next: WidgetDefinition) => void;
  onSave: () => void;
  onValidate: () => void;
  onCancel: () => void;
}

const fieldTypes = ["TEXT", "TEXT_AREA", "RICH_TEXT", "IMAGE", "FILE", "PAGE", "DATE"];

export function DefinitionEditor({
  value,
  busy,
  messages = [],
  status,
  onChange,
  onSave,
  onValidate,
  onCancel,
}: DefinitionEditorProps): React.ReactElement {
  const [fieldName, setFieldName] = useState("");
  const [fieldType, setFieldType] = useState(fieldTypes[0]);
  const [clientErrors, setClientErrors] = useState<string[]>([]);

  const set = <K extends keyof WidgetDefinition>(key: K, v: WidgetDefinition[K]) => {
    onChange({ ...value, [key]: v });
    if (key === "label" || key === "prefix") {
      setClientErrors([]);
    }
  };

  const requiredFieldErrors = (): string[] => {
    const errors: string[] = [];
    if (!value.label.trim()) {
      errors.push(message(K.LABEL_REQUIRED));
    }
    if (!value.prefix.trim()) {
      errors.push(message(K.PREFIX_REQUIRED));
    }
    return errors;
  };

  const canSubmit =
    value.label.trim().length > 0 && value.prefix.trim().length > 0;

  const guardRequired = (action: () => void): void => {
    const errors = requiredFieldErrors();
    if (errors.length > 0) {
      setClientErrors(errors);
      return;
    }
    setClientErrors([]);
    action();
  };

  const addField = () => {
    if (!fieldName.trim()) {
      return;
    }
    const fields: WidgetField[] = [
      ...value.fieldsList.fields,
      { name: fieldName.trim(), type: fieldType, label: fieldName.trim() },
    ];
    onChange({ ...value, fieldsList: { fields } });
    setFieldName("");
  };

  const removeField = (index: number) => {
    const fields = value.fieldsList.fields.filter((_, i) => i !== index);
    onChange({ ...value, fieldsList: { fields } });
  };

  const row: React.CSSProperties = {
    display: "flex",
    flexDirection: "column",
    gap: 4,
    marginBottom: 12,
    maxWidth: 560,
  };

  const displayMessages = [...clientErrors, ...messages];

  return (
    <div data-testid="wb-definition-editor">
      <div style={row}>
        <label htmlFor="wb-label">Label</label>
        <input
          id="wb-label"
          value={value.label}
          onChange={(e) => set("label", e.target.value)}
          required
          aria-required="true"
        />
      </div>
      <div style={row}>
        <label htmlFor="wb-prefix">Prefix</label>
        <input
          id="wb-prefix"
          value={value.prefix}
          onChange={(e) => set("prefix", e.target.value)}
          required
          aria-required="true"
        />
      </div>
      <div style={row}>
        <label htmlFor="wb-version">Version</label>
        <input
          id="wb-version"
          value={value.version}
          onChange={(e) => set("version", e.target.value)}
        />
      </div>
      <div style={row}>
        <label htmlFor="wb-author">Author</label>
        <input
          id="wb-author"
          value={value.author}
          onChange={(e) => set("author", e.target.value)}
        />
      </div>
      <div style={row}>
        <label htmlFor="wb-url">Publisher URL</label>
        <input
          id="wb-url"
          value={value.publisherUrl}
          onChange={(e) => set("publisherUrl", e.target.value)}
        />
      </div>
      <div style={row}>
        <label htmlFor="wb-desc">Description</label>
        <textarea
          id="wb-desc"
          rows={3}
          value={value.description}
          onChange={(e) => set("description", e.target.value)}
        />
      </div>
      <div style={row}>
        <label>
          <input
            type="checkbox"
            checked={value.responsive}
            onChange={(e) => set("responsive", e.target.checked)}
          />{" "}
          Responsive
        </label>
      </div>
      <div style={row}>
        <label htmlFor="wb-html">Widget HTML</label>
        <textarea
          id="wb-html"
          rows={8}
          value={value.widgetHtml}
          onChange={(e) => set("widgetHtml", e.target.value)}
          style={{ fontFamily: "monospace" }}
        />
      </div>
      <div style={row}>
        <label htmlFor="wb-js">JS resources (one per line)</label>
        <textarea
          id="wb-js"
          rows={3}
          value={value.jsFileList.resourceList.join("\n")}
          onChange={(e) =>
            set("jsFileList", {
              resourceList: e.target.value.split("\n").map((s) => s.trim()).filter(Boolean),
            })
          }
        />
      </div>
      <div style={row}>
        <label htmlFor="wb-css">CSS resources (one per line)</label>
        <textarea
          id="wb-css"
          rows={3}
          value={value.cssFileList.resourceList.join("\n")}
          onChange={(e) =>
            set("cssFileList", {
              resourceList: e.target.value.split("\n").map((s) => s.trim()).filter(Boolean),
            })
          }
        />
      </div>

      <fieldset style={{ marginBottom: 16 }}>
        <legend>Fields</legend>
        <ul>
          {value.fieldsList.fields.map((f, i) => (
            <li key={`${f.name}-${i}`}>
              {f.name} ({f.type}){" "}
              <button type="button" onClick={() => removeField(i)}>
                ×
              </button>
            </li>
          ))}
        </ul>
        <div style={{ display: "flex", gap: 8, flexWrap: "wrap" }}>
          <input
            placeholder="field name"
            value={fieldName}
            onChange={(e) => setFieldName(e.target.value)}
          />
          <select value={fieldType} onChange={(e) => setFieldType(e.target.value)}>
            {fieldTypes.map((t) => (
              <option key={t} value={t}>
                {t}
              </option>
            ))}
          </select>
          <button type="button" onClick={addField}>
            {message(K.ADD_FIELD)}
          </button>
        </div>
      </fieldset>

      {displayMessages.length > 0 && (
        <ul role="alert" style={{ color: "#a00" }}>
          {displayMessages.map((m, i) => (
            <li key={i}>{m}</li>
          ))}
        </ul>
      )}
      {status && <p role="status">{status}</p>}

      <div style={{ display: "flex", gap: 8 }}>
        <button
          type="button"
          disabled={busy || !canSubmit}
          onClick={() => guardRequired(onValidate)}
        >
          {message(K.VALIDATE)}
        </button>
        <button
          type="button"
          disabled={busy || !canSubmit}
          onClick={() => guardRequired(onSave)}
        >
          {message(K.SAVE)}
        </button>
        <button type="button" disabled={busy} onClick={onCancel}>
          {message(K.CANCEL)}
        </button>
      </div>
    </div>
  );
}
