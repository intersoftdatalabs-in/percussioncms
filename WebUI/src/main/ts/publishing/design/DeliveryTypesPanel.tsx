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
  createDeliveryType,
  deleteDeliveryType,
  listDeliveryTypes,
  updateDeliveryType,
  type DeliveryTypeSummary,
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

export function DeliveryTypesPanel(): React.ReactElement {
  const [items, setItems] = useState<DeliveryTypeSummary[]>([]);
  const [editing, setEditing] = useState<DeliveryTypeSummary | null>(null);
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState("");
  const [beanName, setBeanName] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  function reload(): void {
    setLoading(true);
    listDeliveryTypes()
      .then(setItems)
      .catch(() => setError(message(MSG.PUBLISH_ERROR)))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    reload();
  }, []);

  function openCreate(): void {
    setCreating(true);
    setEditing(null);
    setName("");
    setBeanName("");
    setDescription("");
  }

  function openEdit(item: DeliveryTypeSummary): void {
    setCreating(false);
    setEditing(item);
    setName(item.name ?? "");
    setBeanName(item.beanName ?? "");
    setDescription(item.description ?? "");
  }

  async function save(): Promise<void> {
    if (!name.trim() || !beanName.trim()) {
      setError("Name and bean name are required");
      return;
    }
    setError(null);
    try {
      const body: DeliveryTypeSummary = {
        name: name.trim(),
        beanName: beanName.trim(),
        description,
      };
      if (editing?.deliveryTypeId) {
        await updateDeliveryType(editing.deliveryTypeId, body);
      } else {
        await createDeliveryType(body);
      }
      setCreating(false);
      setEditing(null);
      reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    }
  }

  async function remove(id: string): Promise<void> {
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_DELETE_DESIGN))) {
      return;
    }
    try {
      await deleteDeliveryType(id);
      reload();
    } catch (e) {
      setError(e instanceof Error ? e.message : message(MSG.PUBLISH_ERROR));
    }
  }

  if (creating || editing) {
    return (
      <div data-testid="delivery-type-editor">
        <h3>{editing ? "Edit delivery type" : "Add delivery type"}</h3>
        <div style={formRowStyle}>
          <label htmlFor="dt-name">* Name</label>
          <input id="dt-name" value={name} onChange={(e) => setName(e.target.value)} />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="dt-bean">* Bean name</label>
          <input
            id="dt-bean"
            value={beanName}
            onChange={(e) => setBeanName(e.target.value)}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="dt-desc">Description</label>
          <input
            id="dt-desc"
            value={description}
            onChange={(e) => setDescription(e.target.value)}
          />
        </div>
        {error && (
          <p style={errorStyle} role="alert">
            {error}
          </p>
        )}
        <div style={toolbarStyle}>
          <button type="button" style={primaryButtonStyle} onClick={() => void save()}>
            {message(MSG.PUBLISH_SAVE)}
          </button>
          <button
            type="button"
            style={buttonStyle}
            onClick={() => {
              setCreating(false);
              setEditing(null);
            }}
          >
            {message(MSG.PUBLISH_BACK)}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div data-testid="delivery-types-panel">
      <div style={toolbarStyle}>
        <button type="button" style={buttonStyle} onClick={openCreate}>
          Add
        </button>
        <button type="button" style={buttonStyle} onClick={reload}>
          Refresh
        </button>
      </div>
      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p style={errorStyle} role="alert">
          {error}
        </p>
      )}
      {!loading && items.length === 0 && (
        <p style={emptyStyle}>No delivery types.</p>
      )}
      <ul style={listStyle}>
        {items.map((t) => (
          <li key={t.deliveryTypeId ?? t.name} style={listItemStyle}>
            <button type="button" style={buttonStyle} onClick={() => openEdit(t)}>
              {t.name}
            </button>
            <span style={{ color: "#666" }}>{t.beanName}</span>
            {t.deliveryTypeId && (
              <button
                type="button"
                style={buttonStyle}
                onClick={() => void remove(t.deliveryTypeId!)}
              >
                Delete
              </button>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
