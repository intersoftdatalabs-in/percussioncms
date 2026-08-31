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

import React, { useState } from "react";
import type {
  ContentTypeChoiceCatalog,
  ContentTypeChoiceDefaultSelected,
  ContentTypeChoiceEntry,
  ContentTypeChoiceFilterField,
} from "../api/developer/types";
import { catalogColors, tableRow } from "./catalogStyles";
import { DEV_MSG } from "./messages";

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  boxSizing: "border-box",
};

const smallBtnStyle: React.CSSProperties = {
  background: "transparent",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  padding: "4px 8px",
  cursor: "pointer",
};

function catalogType(catalog: ContentTypeChoiceCatalog | null): string {
  const t = catalog?.type?.trim();
  return t && t.length > 0 ? t : "none";
}

export function ContentTypeFieldChoicesSection({
  catalog,
  canEdit,
  onChange,
}: {
  catalog: ContentTypeChoiceCatalog | null;
  canEdit: boolean;
  onChange: (next: ContentTypeChoiceCatalog | null) => void;
}): React.ReactElement {
  const [newEntryValue, setNewEntryValue] = useState("");
  const [newEntryLabel, setNewEntryLabel] = useState("");
  const [newDefaultType, setNewDefaultType] = useState("text");
  const [newDefaultText, setNewDefaultText] = useState("");
  const [newDefaultSeq, setNewDefaultSeq] = useState("0");
  const [newFilterField, setNewFilterField] = useState("");
  const [newFilterDep, setNewFilterDep] = useState("optional");

  const type = catalogType(catalog);
  const showSource = type !== "none";

  function commit(next: ContentTypeChoiceCatalog | null) {
    if (!canEdit) {
      return;
    }
    onChange(next);
  }

  function patch(partial: Partial<ContentTypeChoiceCatalog>) {
    commit({ ...(catalog || {}), ...partial, type: partial.type ?? type });
  }

  function setType(nextType: string) {
    if (!canEdit) {
      return;
    }
    commit({ ...(catalog || {}), type: nextType });
  }

  function addEntry() {
    if (!canEdit) {
      return;
    }
    const value = newEntryValue.trim();
    if (!value) {
      return;
    }
    const entries = [...(catalog?.entries || []), { value, label: newEntryLabel.trim() || value }];
    patch({ entries });
    setNewEntryValue("");
    setNewEntryLabel("");
  }

  function updateEntry(index: number, field: keyof ContentTypeChoiceEntry, value: string) {
    if (!canEdit) {
      return;
    }
    const entries = (catalog?.entries || []).map((e, i) =>
      i === index ? { ...e, [field]: value } : e,
    );
    patch({ entries });
  }

  function removeEntry(index: number) {
    if (!canEdit) {
      return;
    }
    patch({ entries: (catalog?.entries || []).filter((_, i) => i !== index) });
  }

  function addDefaultSelected() {
    if (!canEdit) {
      return;
    }
    const row: ContentTypeChoiceDefaultSelected = { type: newDefaultType };
    if (newDefaultType === "text") {
      const text = newDefaultText.trim();
      if (!text) {
        return;
      }
      row.text = text;
    } else if (newDefaultType === "sequence") {
      const seq = Number(newDefaultSeq);
      if (!Number.isFinite(seq) || seq < 0) {
        return;
      }
      row.sequence = seq;
    }
    patch({ defaultSelected: [...(catalog?.defaultSelected || []), row] });
    setNewDefaultText("");
    setNewDefaultSeq("0");
  }

  function removeDefaultSelected(index: number) {
    if (!canEdit) {
      return;
    }
    patch({ defaultSelected: (catalog?.defaultSelected || []).filter((_, i) => i !== index) });
  }

  function addFilterField() {
    if (!canEdit) {
      return;
    }
    const fieldRef = newFilterField.trim();
    if (!fieldRef) {
      return;
    }
    const row: ContentTypeChoiceFilterField = {
      fieldRef,
      dependencyType: newFilterDep || "optional",
    };
    patch({
      filter: {
        ...(catalog?.filter || {}),
        dependentFields: [...(catalog?.filter?.dependentFields || []), row],
      },
    });
    setNewFilterField("");
  }

  function removeFilterField(index: number) {
    if (!canEdit) {
      return;
    }
    const dependentFields = (catalog?.filter?.dependentFields || []).filter((_, i) => i !== index);
    patch({
      filter: {
        ...(catalog?.filter || {}),
        dependentFields,
      },
    });
  }

  return (
    <div data-testid="developer-ct-choices" style={{ marginTop: "16px" }}>
      <h4 style={{ fontSize: "0.95rem", margin: "0 0 4px" }}>{DEV_MSG.CT_CONTROL_PROPS_CHOICES}</h4>
      <p style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>{DEV_MSG.CT_CHOICES_HINT}</p>
      <div style={{ marginBottom: "12px" }}>
        <label htmlFor="ct-ch-type" style={{ display: "block", marginBottom: 4 }}>
          {DEV_MSG.CT_CHOICES_TYPE}
        </label>
        <select
          id="ct-ch-type"
          data-testid="developer-ct-ch-type"
          aria-label={DEV_MSG.CT_CHOICES_TYPE}
          style={inputStyle}
          value={type}
          disabled={!canEdit}
          onChange={(e) => setType(e.target.value)}
        >
          <option value="none">{DEV_MSG.CT_CHOICES_TYPE_NONE}</option>
          <option value="local">{DEV_MSG.CT_CHOICES_TYPE_LOCAL}</option>
          <option value="global">{DEV_MSG.CT_CHOICES_TYPE_GLOBAL}</option>
          <option value="lookup">{DEV_MSG.CT_CHOICES_TYPE_LOOKUP}</option>
          <option value="internalLookup">{DEV_MSG.CT_CHOICES_TYPE_INTERNAL}</option>
          <option value="tableinfo">{DEV_MSG.CT_CHOICES_TYPE_TABLE}</option>
        </select>
      </div>
      {showSource ? (
        <div style={{ marginBottom: "12px" }}>
          <label htmlFor="ct-ch-sort" style={{ display: "block", marginBottom: 4 }}>
            {DEV_MSG.CT_CHOICES_SORT}
          </label>
          <select
            id="ct-ch-sort"
            data-testid="developer-ct-ch-sort"
            aria-label={DEV_MSG.CT_CHOICES_SORT}
            style={inputStyle}
            value={catalog?.sortOrder || "ascending"}
            disabled={!canEdit}
            onChange={(e) => patch({ sortOrder: e.target.value })}
          >
            <option value="ascending">{DEV_MSG.CT_CHOICES_SORT_ASC}</option>
            <option value="descending">{DEV_MSG.CT_CHOICES_SORT_DESC}</option>
            <option value="user">{DEV_MSG.CT_CHOICES_SORT_USER}</option>
          </select>
        </div>
      ) : null}
      {type === "global" ? (
        <div style={{ marginBottom: "12px" }}>
          <label htmlFor="ct-ch-global-id" style={{ display: "block", marginBottom: 4 }}>
            {DEV_MSG.CT_CHOICES_GLOBAL_ID}
          </label>
          <input
            id="ct-ch-global-id"
            data-testid="developer-ct-ch-global-id"
            type="number"
            min={0}
            style={inputStyle}
            value={catalog?.globalId ?? ""}
            disabled={!canEdit}
            readOnly={!canEdit}
            onChange={(e) => {
              if (!canEdit) {
                return;
              }
              const n = e.target.value === "" ? undefined : Number(e.target.value);
              patch({ globalId: n != null && Number.isFinite(n) ? n : undefined });
            }}
          />
        </div>
      ) : null}
      {type === "local" ? (
        <div style={{ marginBottom: "12px" }}>
          {(catalog?.entries || []).length === 0 ? (
            <p style={{ color: catalogColors.empty }} data-testid="developer-ct-ch-entries-empty">
              {DEV_MSG.CT_CHOICES_ENTRIES_EMPTY}
            </p>
          ) : (
            <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
              {(catalog?.entries || []).map((e, i) => (
                <li
                  key={`ch-entry-${i}`}
                  data-testid={`developer-ct-ch-entry-${i}`}
                  style={{
                    ...tableRow,
                    display: "grid",
                    gridTemplateColumns: "1fr 1fr auto",
                    gap: 8,
                    alignItems: "center",
                    padding: "6px 0",
                  }}
                >
                  <input
                    type="text"
                    data-testid={`developer-ct-ch-entry-value-${i}`}
                    aria-label={`${DEV_MSG.CT_CHOICES_ENTRY_VALUE} ${i}`}
                    style={inputStyle}
                    value={e.value || ""}
                    disabled={!canEdit}
                    readOnly={!canEdit}
                    onChange={(ev) => updateEntry(i, "value", ev.target.value)}
                  />
                  <input
                    type="text"
                    data-testid={`developer-ct-ch-entry-label-${i}`}
                    aria-label={`${DEV_MSG.CT_CHOICES_ENTRY_LABEL} ${i}`}
                    style={inputStyle}
                    value={e.label || ""}
                    disabled={!canEdit}
                    readOnly={!canEdit}
                    onChange={(ev) => updateEntry(i, "label", ev.target.value)}
                  />
                  <button
                    type="button"
                    data-testid={`developer-ct-ch-entry-remove-${i}`}
                    disabled={!canEdit}
                    onClick={() => removeEntry(i)}
                    style={{
                      ...smallBtnStyle,
                      cursor: canEdit ? "pointer" : "not-allowed",
                    }}
                  >
                    {DEV_MSG.CT_ASSOC_REMOVE}
                  </button>
                </li>
              ))}
            </ul>
          )}
          <div
            style={{
              marginTop: "8px",
              display: "grid",
              gridTemplateColumns: "1fr 1fr auto",
              gap: "8px",
              alignItems: "end",
            }}
          >
            <div>
              <label htmlFor="ct-ch-entry-add-value" style={{ display: "block", marginBottom: 4 }}>
                {DEV_MSG.CT_CHOICES_ENTRY_VALUE}
              </label>
              <input
                id="ct-ch-entry-add-value"
                data-testid="developer-ct-ch-entry-add-value"
                type="text"
                style={inputStyle}
                value={newEntryValue}
                disabled={!canEdit}
                readOnly={!canEdit}
                onChange={(e) => {
                  if (canEdit) {
                    setNewEntryValue(e.target.value);
                  }
                }}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    addEntry();
                  }
                }}
              />
            </div>
            <div>
              <label htmlFor="ct-ch-entry-add-label" style={{ display: "block", marginBottom: 4 }}>
                {DEV_MSG.CT_CHOICES_ENTRY_LABEL}
              </label>
              <input
                id="ct-ch-entry-add-label"
                data-testid="developer-ct-ch-entry-add-label"
                type="text"
                style={inputStyle}
                value={newEntryLabel}
                disabled={!canEdit}
                readOnly={!canEdit}
                onChange={(e) => {
                  if (canEdit) {
                    setNewEntryLabel(e.target.value);
                  }
                }}
                onKeyDown={(e) => {
                  if (e.key === "Enter") {
                    e.preventDefault();
                    addEntry();
                  }
                }}
              />
            </div>
            <button
              type="button"
              data-testid="developer-ct-ch-entry-add"
              disabled={!canEdit || !newEntryValue.trim()}
              onClick={addEntry}
              style={{
                ...smallBtnStyle,
                padding: "8px 12px",
                cursor: !canEdit || !newEntryValue.trim() ? "not-allowed" : "pointer",
              }}
            >
              {DEV_MSG.CT_ASSOC_ADD}
            </button>
          </div>
        </div>
      ) : null}
      {type === "lookup" || type === "internalLookup" ? (
        <div style={{ marginBottom: "12px", display: "grid", gap: 8 }}>
          <div>
            <label htmlFor="ct-ch-lookup-href" style={{ display: "block", marginBottom: 4 }}>
              {DEV_MSG.CT_CHOICES_LOOKUP_HREF}
            </label>
            <input
              id="ct-ch-lookup-href"
              data-testid="developer-ct-ch-lookup-href"
              type="text"
              style={inputStyle}
              value={catalog?.lookupHref || ""}
              disabled={!canEdit}
              readOnly={!canEdit}
              onChange={(e) => patch({ lookupHref: e.target.value })}
            />
          </div>
          <div>
            <label htmlFor="ct-ch-lookup-name" style={{ display: "block", marginBottom: 4 }}>
              {DEV_MSG.CT_CHOICES_LOOKUP_NAME}
            </label>
            <input
              id="ct-ch-lookup-name"
              data-testid="developer-ct-ch-lookup-name"
              type="text"
              style={inputStyle}
              value={catalog?.lookupName || ""}
              disabled={!canEdit}
              readOnly={!canEdit}
              onChange={(e) => patch({ lookupName: e.target.value })}
            />
          </div>
        </div>
      ) : null}
      {type === "tableinfo" ? (
        <div
          style={{
            marginBottom: "12px",
            display: "grid",
            gridTemplateColumns: "1fr 1fr",
            gap: 8,
          }}
        >
          <div>
            <label htmlFor="ct-ch-table-name" style={{ display: "block", marginBottom: 4 }}>
              {DEV_MSG.CT_CHOICES_TABLE_NAME}
            </label>
            <input
              id="ct-ch-table-name"
              data-testid="developer-ct-ch-table-name"
              type="text"
              style={inputStyle}
              value={catalog?.table?.tableName || ""}
              disabled={!canEdit}
              readOnly={!canEdit}
              onChange={(e) =>
                patch({ table: { ...(catalog?.table || {}), tableName: e.target.value } })
              }
            />
          </div>
          <div>
            <label htmlFor="ct-ch-table-ds" style={{ display: "block", marginBottom: 4 }}>
              {DEV_MSG.CT_CHOICES_TABLE_DS}
            </label>
            <input
              id="ct-ch-table-ds"
              data-testid="developer-ct-ch-table-ds"
              type="text"
              style={inputStyle}
              value={catalog?.table?.dataSource || ""}
              disabled={!canEdit}
              readOnly={!canEdit}
              onChange={(e) =>
                patch({ table: { ...(catalog?.table || {}), dataSource: e.target.value } })
              }
            />
          </div>
          <div>
            <label htmlFor="ct-ch-table-label" style={{ display: "block", marginBottom: 4 }}>
              {DEV_MSG.CT_CHOICES_TABLE_LABEL}
            </label>
            <input
              id="ct-ch-table-label"
              data-testid="developer-ct-ch-table-label"
              type="text"
              style={inputStyle}
              value={catalog?.table?.labelColumn || ""}
              disabled={!canEdit}
              readOnly={!canEdit}
              onChange={(e) =>
                patch({ table: { ...(catalog?.table || {}), labelColumn: e.target.value } })
              }
            />
          </div>
          <div>
            <label htmlFor="ct-ch-table-value" style={{ display: "block", marginBottom: 4 }}>
              {DEV_MSG.CT_CHOICES_TABLE_VALUE}
            </label>
            <input
              id="ct-ch-table-value"
              data-testid="developer-ct-ch-table-value"
              type="text"
              style={inputStyle}
              value={catalog?.table?.valueColumn || ""}
              disabled={!canEdit}
              readOnly={!canEdit}
              onChange={(e) =>
                patch({ table: { ...(catalog?.table || {}), valueColumn: e.target.value } })
              }
            />
          </div>
        </div>
      ) : null}
      {showSource ? (
        <>
          <label style={{ display: "flex", alignItems: "center", gap: 8, marginBottom: 8 }}>
            <input
              type="checkbox"
              data-testid="developer-ct-ch-null"
              checked={catalog?.nullEntry != null}
              disabled={!canEdit}
              onChange={(e) =>
                patch({
                  nullEntry: e.target.checked
                    ? {
                        value: catalog?.nullEntry?.value ?? "",
                        label: catalog?.nullEntry?.label || "None",
                        includeWhen: catalog?.nullEntry?.includeWhen || "always",
                        sortOrder: catalog?.nullEntry?.sortOrder || "first",
                      }
                    : undefined,
                })
              }
            />
            {DEV_MSG.CT_CHOICES_NULL}
          </label>
          {catalog?.nullEntry != null ? (
            <div
              style={{
                marginBottom: "12px",
                display: "grid",
                gridTemplateColumns: "1fr 1fr",
                gap: 8,
              }}
            >
              <div>
                <label htmlFor="ct-ch-null-value" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.CT_CHOICES_NULL_VALUE}
                </label>
                <input
                  id="ct-ch-null-value"
                  data-testid="developer-ct-ch-null-value"
                  type="text"
                  style={inputStyle}
                  value={catalog.nullEntry.value ?? ""}
                  disabled={!canEdit}
                  readOnly={!canEdit}
                  onChange={(e) =>
                    patch({ nullEntry: { ...catalog.nullEntry, value: e.target.value } })
                  }
                />
              </div>
              <div>
                <label htmlFor="ct-ch-null-label" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.CT_CHOICES_NULL_LABEL}
                </label>
                <input
                  id="ct-ch-null-label"
                  data-testid="developer-ct-ch-null-label"
                  type="text"
                  style={inputStyle}
                  value={catalog.nullEntry.label || ""}
                  disabled={!canEdit}
                  readOnly={!canEdit}
                  onChange={(e) =>
                    patch({ nullEntry: { ...catalog.nullEntry, label: e.target.value } })
                  }
                />
              </div>
              <div>
                <label htmlFor="ct-ch-null-when" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.CT_CHOICES_NULL_WHEN}
                </label>
                <select
                  id="ct-ch-null-when"
                  data-testid="developer-ct-ch-null-when"
                  style={inputStyle}
                  value={catalog.nullEntry.includeWhen || "always"}
                  disabled={!canEdit}
                  onChange={(e) =>
                    patch({ nullEntry: { ...catalog.nullEntry, includeWhen: e.target.value } })
                  }
                >
                  <option value="always">{DEV_MSG.CT_CHOICES_NULL_WHEN_ALWAYS}</option>
                  <option value="onlyIfNull">{DEV_MSG.CT_CHOICES_NULL_WHEN_NULL}</option>
                </select>
              </div>
              <div>
                <label htmlFor="ct-ch-null-sort" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.CT_CHOICES_NULL_SORT}
                </label>
                <select
                  id="ct-ch-null-sort"
                  data-testid="developer-ct-ch-null-sort"
                  style={inputStyle}
                  value={catalog.nullEntry.sortOrder || "first"}
                  disabled={!canEdit}
                  onChange={(e) =>
                    patch({ nullEntry: { ...catalog.nullEntry, sortOrder: e.target.value } })
                  }
                >
                  <option value="first">{DEV_MSG.CT_CHOICES_NULL_SORT_FIRST}</option>
                  <option value="last">{DEV_MSG.CT_CHOICES_NULL_SORT_LAST}</option>
                  <option value="sorted">{DEV_MSG.CT_CHOICES_NULL_SORT_SORTED}</option>
                </select>
              </div>
            </div>
          ) : null}
          <div style={{ marginBottom: "12px" }}>
            <p style={{ margin: "0 0 6px", fontSize: "0.9rem" }}>{DEV_MSG.CT_CHOICES_DEFAULT}</p>
            {(catalog?.defaultSelected || []).map((d, i) => (
              <div
                key={`ds-${i}`}
                data-testid={`developer-ct-ch-ds-${i}`}
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr auto",
                  gap: 8,
                  marginBottom: 6,
                  alignItems: "center",
                }}
              >
                <span data-testid={`developer-ct-ch-ds-type-${i}`}>{d.type}</span>
                <span data-testid={`developer-ct-ch-ds-value-${i}`}>
                  {d.type === "sequence" ? String(d.sequence ?? "") : d.text || ""}
                </span>
                <button
                  type="button"
                  data-testid={`developer-ct-ch-ds-remove-${i}`}
                  disabled={!canEdit}
                  onClick={() => removeDefaultSelected(i)}
                  style={{
                    ...smallBtnStyle,
                    cursor: canEdit ? "pointer" : "not-allowed",
                  }}
                >
                  {DEV_MSG.CT_ASSOC_REMOVE}
                </button>
              </div>
            ))}
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr auto",
                gap: 8,
                alignItems: "end",
              }}
            >
              <div>
                <label htmlFor="ct-ch-ds-add-type" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.CT_CHOICES_DEFAULT_TYPE}
                </label>
                <select
                  id="ct-ch-ds-add-type"
                  data-testid="developer-ct-ch-ds-add-type"
                  style={inputStyle}
                  value={newDefaultType}
                  disabled={!canEdit}
                  onChange={(e) => setNewDefaultType(e.target.value)}
                >
                  <option value="text">text</option>
                  <option value="nullEntry">nullEntry</option>
                  <option value="sequence">sequence</option>
                </select>
              </div>
              {newDefaultType === "sequence" ? (
                <div>
                  <label htmlFor="ct-ch-ds-add-seq" style={{ display: "block", marginBottom: 4 }}>
                    {DEV_MSG.CT_CHOICES_DEFAULT_SEQ}
                  </label>
                  <input
                    id="ct-ch-ds-add-seq"
                    data-testid="developer-ct-ch-ds-add-seq"
                    type="number"
                    min={0}
                    style={inputStyle}
                    value={newDefaultSeq}
                    disabled={!canEdit}
                    onChange={(e) => setNewDefaultSeq(e.target.value)}
                  />
                </div>
              ) : newDefaultType === "text" ? (
                <div>
                  <label htmlFor="ct-ch-ds-add-text" style={{ display: "block", marginBottom: 4 }}>
                    {DEV_MSG.CT_CHOICES_DEFAULT_TEXT}
                  </label>
                  <input
                    id="ct-ch-ds-add-text"
                    data-testid="developer-ct-ch-ds-add-text"
                    type="text"
                    style={inputStyle}
                    value={newDefaultText}
                    disabled={!canEdit}
                    onChange={(e) => setNewDefaultText(e.target.value)}
                  />
                </div>
              ) : (
                <div />
              )}
              <button
                type="button"
                data-testid="developer-ct-ch-ds-add"
                disabled={
                  !canEdit ||
                  (newDefaultType === "text" && !newDefaultText.trim()) ||
                  (newDefaultType === "sequence" && newDefaultSeq.trim() === "")
                }
                onClick={addDefaultSelected}
                style={{
                  ...smallBtnStyle,
                  padding: "8px 12px",
                  cursor: !canEdit ? "not-allowed" : "pointer",
                }}
              >
                {DEV_MSG.CT_ASSOC_ADD}
              </button>
            </div>
          </div>
          <div style={{ marginBottom: "12px" }}>
            <p style={{ margin: "0 0 6px", fontSize: "0.9rem" }}>{DEV_MSG.CT_CHOICES_FILTER}</p>
            <div style={{ display: "grid", gap: 8, marginBottom: 8 }}>
              <div>
                <label htmlFor="ct-ch-filter-href" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.CT_CHOICES_FILTER_HREF}
                </label>
                <input
                  id="ct-ch-filter-href"
                  data-testid="developer-ct-ch-filter-href"
                  type="text"
                  style={inputStyle}
                  value={catalog?.filter?.lookupHref || ""}
                  disabled={!canEdit}
                  readOnly={!canEdit}
                  onChange={(e) =>
                    patch({
                      filter: { ...(catalog?.filter || {}), lookupHref: e.target.value },
                    })
                  }
                />
              </div>
              <div>
                <label htmlFor="ct-ch-filter-name" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.CT_CHOICES_FILTER_NAME}
                </label>
                <input
                  id="ct-ch-filter-name"
                  data-testid="developer-ct-ch-filter-name"
                  type="text"
                  style={inputStyle}
                  value={catalog?.filter?.lookupName || ""}
                  disabled={!canEdit}
                  readOnly={!canEdit}
                  onChange={(e) =>
                    patch({
                      filter: { ...(catalog?.filter || {}), lookupName: e.target.value },
                    })
                  }
                />
              </div>
            </div>
            {(catalog?.filter?.dependentFields || []).map((f, i) => (
              <div
                key={`ff-${i}`}
                data-testid={`developer-ct-ch-filter-row-${i}`}
                style={{
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr auto",
                  gap: 8,
                  marginBottom: 6,
                  alignItems: "center",
                }}
              >
                <span data-testid={`developer-ct-ch-filter-field-${i}`}>{f.fieldRef}</span>
                <span data-testid={`developer-ct-ch-filter-dep-${i}`}>{f.dependencyType}</span>
                <button
                  type="button"
                  data-testid={`developer-ct-ch-filter-remove-${i}`}
                  disabled={!canEdit}
                  onClick={() => removeFilterField(i)}
                  style={{
                    ...smallBtnStyle,
                    cursor: canEdit ? "pointer" : "not-allowed",
                  }}
                >
                  {DEV_MSG.CT_ASSOC_REMOVE}
                </button>
              </div>
            ))}
            <div
              style={{
                display: "grid",
                gridTemplateColumns: "1fr 1fr auto",
                gap: 8,
                alignItems: "end",
              }}
            >
              <div>
                <label htmlFor="ct-ch-filter-add-field" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.CT_CHOICES_FILTER_FIELD}
                </label>
                <input
                  id="ct-ch-filter-add-field"
                  data-testid="developer-ct-ch-filter-add-field"
                  type="text"
                  style={inputStyle}
                  value={newFilterField}
                  disabled={!canEdit}
                  onChange={(e) => setNewFilterField(e.target.value)}
                />
              </div>
              <div>
                <label htmlFor="ct-ch-filter-add-dep" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.CT_CHOICES_FILTER_DEP}
                </label>
                <select
                  id="ct-ch-filter-add-dep"
                  data-testid="developer-ct-ch-filter-add-dep"
                  style={inputStyle}
                  value={newFilterDep}
                  disabled={!canEdit}
                  onChange={(e) => setNewFilterDep(e.target.value)}
                >
                  <option value="optional">optional</option>
                  <option value="required">required</option>
                </select>
              </div>
              <button
                type="button"
                data-testid="developer-ct-ch-filter-add"
                disabled={!canEdit || !newFilterField.trim()}
                onClick={addFilterField}
                style={{
                  ...smallBtnStyle,
                  padding: "8px 12px",
                  cursor: !canEdit || !newFilterField.trim() ? "not-allowed" : "pointer",
                }}
              >
                {DEV_MSG.CT_ASSOC_ADD}
              </button>
            </div>
          </div>
        </>
      ) : null}
    </div>
  );
}
