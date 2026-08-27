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
import {
  addItemExit,
  itemExitDisplay,
  removeItemExit,
  type ItemExitListKey,
} from "../api/developer/contentTypeItemExits";
import type { ContentTypeItemExits } from "../api/developer/types";
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

const LIST_DEFS: { key: ItemExitListKey; testId: string }[] = [
  { key: "inputTranslations", testId: "in" },
  { key: "outputTranslations", testId: "out" },
  { key: "validations", testId: "val" },
  { key: "preExits", testId: "pre" },
  { key: "postExits", testId: "post" },
];

function listLabel(key: ItemExitListKey): string {
  switch (key) {
    case "inputTranslations":
      return DEV_MSG.CT_IE_INPUT;
    case "outputTranslations":
      return DEV_MSG.CT_IE_OUTPUT;
    case "validations":
      return DEV_MSG.CT_IE_VALIDATIONS;
    case "preExits":
      return DEV_MSG.CT_IE_PRE;
    case "postExits":
      return DEV_MSG.CT_IE_POST;
  }
}

function ItemExitListEditor({
  listKey,
  testId,
  label,
  value,
  canEdit,
  onChange,
}: {
  listKey: ItemExitListKey;
  testId: string;
  label: string;
  value: ContentTypeItemExits;
  canEdit: boolean;
  onChange: (next: ContentTypeItemExits) => void;
}): React.ReactElement {
  const [fqn, setFqn] = useState("");
  const [param, setParam] = useState("");
  const rows = value[listKey] ?? [];
  const prefix = `developer-ct-ie-${testId}`;

  function handleAdd() {
    if (!canEdit) {
      return;
    }
    const next = addItemExit(value, listKey, fqn, param);
    if (next === value) {
      setFqn("");
      return;
    }
    onChange(next);
    setFqn("");
    setParam("");
  }

  return (
    <div data-testid={prefix} style={{ marginTop: "12px" }}>
      <h4 style={{ fontSize: "0.95rem", margin: "0 0 6px" }}>{label}</h4>
      {rows.length === 0 ? (
        <p style={{ color: catalogColors.empty, margin: "0 0 8px" }} data-testid={`${prefix}-empty`}>
          {DEV_MSG.CT_NONE}
        </p>
      ) : (
        <ul style={{ listStyle: "none", padding: 0, margin: "0 0 8px" }}>
          {rows.map((exit, i) => (
            <li
              key={`${itemExitDisplay(exit)}:${i}`}
              data-testid={`${prefix}-row-${i}`}
              style={{
                ...tableRow,
                display: "flex",
                alignItems: "center",
                gap: 12,
                padding: "6px 0",
              }}
            >
              <span style={{ fontFamily: "monospace", fontSize: "0.85rem" }}>
                {itemExitDisplay(exit)}
                {(exit.parameters ?? []).length > 0 ? (
                  <span style={{ color: catalogColors.empty, marginLeft: "8px" }}>
                    {(exit.parameters ?? [])
                      .map((p) => (p.name ? `${p.name}=${p.value ?? ""}` : p.value ?? ""))
                      .filter(Boolean)
                      .join(", ")}
                  </span>
                ) : null}
                {exit.condition ? (
                  <span style={{ color: catalogColors.muted, marginLeft: "8px" }}>
                    {exit.condition}
                  </span>
                ) : null}
              </span>
              <button
                type="button"
                data-testid={`${prefix}-remove-${i}`}
                aria-label={`Remove ${label} ${itemExitDisplay(exit)}`}
                disabled={!canEdit}
                onClick={() => {
                  if (!canEdit) {
                    return;
                  }
                  onChange(removeItemExit(value, listKey, i));
                }}
                style={{
                  ...smallBtnStyle,
                  marginLeft: "auto",
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
          display: "grid",
          gridTemplateColumns: "1fr 1fr auto",
          gap: "8px",
          alignItems: "end",
        }}
      >
        <div>
          <label htmlFor={`${prefix}-add-fqn`} style={{ display: "block", marginBottom: 4 }}>
            {label}
          </label>
          <input
            id={`${prefix}-add-fqn`}
            type="text"
            autoComplete="off"
            data-testid={`${prefix}-add-fqn`}
            style={inputStyle}
            placeholder={DEV_MSG.CT_IE_FQN_PLACEHOLDER}
            value={fqn}
            onChange={(e) => {
              if (!canEdit) {
                return;
              }
              setFqn(e.target.value);
            }}
            disabled={!canEdit}
            aria-disabled={canEdit ? undefined : true}
            readOnly={!canEdit}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                if (canEdit) {
                  handleAdd();
                }
              }
            }}
          />
        </div>
        <div>
          <label htmlFor={`${prefix}-add-param`} style={{ display: "block", marginBottom: 4 }}>
            {DEV_MSG.CT_IE_PARAM_PLACEHOLDER}
          </label>
          <input
            id={`${prefix}-add-param`}
            type="text"
            autoComplete="off"
            data-testid={`${prefix}-add-param`}
            style={inputStyle}
            placeholder={DEV_MSG.CT_IE_PARAM_PLACEHOLDER}
            value={param}
            onChange={(e) => {
              if (!canEdit) {
                return;
              }
              setParam(e.target.value);
            }}
            disabled={!canEdit}
            aria-disabled={canEdit ? undefined : true}
            readOnly={!canEdit}
          />
        </div>
        <button
          type="button"
          data-testid={`${prefix}-add`}
          disabled={!canEdit || !fqn.trim()}
          onClick={handleAdd}
          style={{
            ...smallBtnStyle,
            padding: "8px 12px",
            cursor: !canEdit || !fqn.trim() ? "not-allowed" : "pointer",
          }}
        >
          {DEV_MSG.CT_ASSOC_ADD}
        </button>
      </div>
    </div>
  );
}

export function ContentTypeItemExitsSection({
  value,
  canEdit,
  loadError,
  onChange,
}: {
  value: ContentTypeItemExits;
  canEdit: boolean;
  loadError?: string | null;
  onChange: (next: ContentTypeItemExits) => void;
}): React.ReactElement {
  return (
    <section style={{ marginBottom: "16px" }} data-testid="developer-ct-item-exits">
      <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_ITEM_EXITS}</h3>
      <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.CT_ITEM_EXITS_HINT}</p>
      <p style={{ color: catalogColors.muted, fontSize: "0.85rem" }}>
        {DEV_MSG.CT_IE_CONDITIONS_HINT}
      </p>
      {loadError ? (
        <div role="alert" data-testid="developer-ct-ie-error" style={{ color: catalogColors.error }}>
          {loadError}
        </div>
      ) : null}
      <div style={{ marginTop: "8px", maxWidth: "16rem" }}>
        <label htmlFor="ct-ie-max-errors" style={{ display: "block", marginBottom: 4 }}>
          {DEV_MSG.CT_IE_MAX_ERRORS}
        </label>
        <input
          id="ct-ie-max-errors"
          type="number"
          data-testid="developer-ct-ie-max-errors"
          style={inputStyle}
          value={value.maxErrorsToStopValidation ?? ""}
          disabled={!canEdit}
          onChange={(e) => {
            if (!canEdit) {
              return;
            }
            const raw = e.target.value;
            onChange({
              ...value,
              maxErrorsToStopValidation: raw === "" ? undefined : Number(raw),
            });
          }}
        />
      </div>
      {LIST_DEFS.map((list) => (
        <ItemExitListEditor
          key={list.key}
          listKey={list.key}
          testId={list.testId}
          label={listLabel(list.key)}
          value={value}
          canEdit={canEdit}
          onChange={onChange}
        />
      ))}
    </section>
  );
}
