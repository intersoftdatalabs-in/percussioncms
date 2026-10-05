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
  buildDeliveryTypeCopyBody,
  deliveryTypesAfterSuccessfulCopy,
  suggestedDeliveryTypeCopyName,
  validateDeliveryTypeCopyName,
} from "../deliveryTypeCopy";
import {
  buildDeliveryTypeRenameBody,
  deliveryTypesAfterSuccessfulRename,
  validateDeliveryTypeRenameName,
} from "../deliveryTypeRename";
import {
  deliveryTypesAfterSuccessfulDelete,
  mapDeliveryTypeDeleteError,
} from "../deliveryTypeDelete";
import { mapDeliveryTypeSaveError } from "../deliveryTypeSaveErrors";
import { useDirtyForm } from "../dirtyFormContext";
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
  const [copying, setCopying] = useState<DeliveryTypeSummary | null>(null);
  const [copyName, setCopyName] = useState("");
  const [renaming, setRenaming] = useState<DeliveryTypeSummary | null>(null);
  const [renameName, setRenameName] = useState("");
  const [name, setName] = useState("");
  const [beanName, setBeanName] = useState("");
  const [description, setDescription] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const { setDirty, confirmIfDirty } = useDirtyForm();

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
    setCopying(null);
    setRenaming(null);
    setName("");
    setBeanName("");
    setDescription("");
    setError(null);
    setDirty(false);
  }

  function openCopy(item: DeliveryTypeSummary): void {
    if (!item.deliveryTypeId) {
      return;
    }
    setCreating(false);
    setEditing(null);
    setRenaming(null);
    setCopying(item);
    setCopyName(suggestedDeliveryTypeCopyName(item.name));
    setError(null);
    setDirty(false);
  }

  function closeCopy(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setCopying(null);
  }

  function openRename(item: DeliveryTypeSummary): void {
    if (!item.deliveryTypeId) {
      return;
    }
    setCreating(false);
    setEditing(null);
    setCopying(null);
    setRenaming(item);
    setRenameName(item.name ?? "");
    setError(null);
    setDirty(false);
  }

  function closeRename(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setRenaming(null);
  }

  function openEdit(item: DeliveryTypeSummary): void {
    setCreating(false);
    setCopying(null);
    setRenaming(null);
    setEditing(item);
    setName(item.name ?? "");
    setBeanName(item.beanName ?? "");
    setDescription(item.description ?? "");
    setError(null);
    setDirty(false);
  }

  function closeEditor(): void {
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setCreating(false);
    setEditing(null);
    setRenaming(null);
  }

  async function save(): Promise<void> {
    if (!name.trim() || !beanName.trim()) {
      setError("Name and bean name are required");
      return;
    }
    setSaving(true);
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
      setDirty(false);
      setCreating(false);
      setEditing(null);
      reload();
    } catch (e) {
      setError(mapDeliveryTypeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function copyType(): Promise<void> {
    if (!copying?.deliveryTypeId || saving) {
      return;
    }
    const validated = validateDeliveryTypeCopyName(copyName);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setSaving(true);
    setError(null);
    const previous = items;
    try {
      const created = await createDeliveryType(
        buildDeliveryTypeCopyBody(copying, validated.name),
      );
      let refreshed: DeliveryTypeSummary[] | null = null;
      try {
        refreshed = await listDeliveryTypes();
      } catch {
        refreshed = null;
      }
      setItems(deliveryTypesAfterSuccessfulCopy(refreshed, created, previous));
      setDirty(false);
      setCopying(null);
    } catch (e) {
      setError(mapDeliveryTypeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function renameType(): Promise<void> {
    if (!renaming?.deliveryTypeId || saving) {
      return;
    }
    const validated = validateDeliveryTypeRenameName(renameName);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    const id = renaming.deliveryTypeId;
    setSaving(true);
    setError(null);
    const previous = items;
    try {
      await updateDeliveryType(id, buildDeliveryTypeRenameBody(validated.name));
      setDirty(false);
      setRenaming(null);
      let refreshed: DeliveryTypeSummary[] | null = null;
      try {
        refreshed = await listDeliveryTypes();
      } catch {
        refreshed = null;
      }
      setItems(
        deliveryTypesAfterSuccessfulRename(refreshed, id, validated.name, previous),
      );
    } catch (e) {
      setError(mapDeliveryTypeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function remove(id: string | number): Promise<void> {
    if (saving || id === "" || id == null) {
      return;
    }
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_DELETE_DESIGN))) {
      return;
    }
    setError(null);
    setSaving(true);
    const previous = items;
    try {
      await deleteDeliveryType(id);
      let refreshed: DeliveryTypeSummary[] | null = null;
      try {
        refreshed = await listDeliveryTypes();
      } catch {
        refreshed = null;
      }
      setItems(deliveryTypesAfterSuccessfulDelete(refreshed, id, previous));
    } catch (e) {
      setError(mapDeliveryTypeDeleteError(e));
    } finally {
      setSaving(false);
    }
  }

  if (renaming) {
    return (
      <div data-testid="delivery-type-rename-form">
        <h3>Rename delivery type</h3>
        <p>
          Bean name:{" "}
          <span data-testid="delivery-type-rename-bean">{renaming.beanName ?? ""}</span>
        </p>
        <p>
          Description:{" "}
          <span data-testid="delivery-type-rename-description">
            {renaming.description ?? ""}
          </span>
        </p>
        <div style={formRowStyle}>
          <label htmlFor="delivery-type-rename-name">* Name</label>
          <input
            id="delivery-type-rename-name"
            value={renameName}
            onChange={(e) => {
              setRenameName(e.target.value);
              setDirty(true);
            }}
          />
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
            data-testid="delivery-type-rename-submit"
            disabled={saving}
            onClick={() => void renameType()}
          >
            Rename delivery type
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="delivery-type-rename-cancel"
            disabled={saving}
            onClick={closeRename}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (copying) {
    return (
      <div data-testid="delivery-type-copy-form">
        <h3>Copy delivery type</h3>
        <p>Source: {copying.name ?? copying.deliveryTypeId}</p>
        <p>
          Bean name:{" "}
          <span data-testid="delivery-type-copy-bean">{copying.beanName ?? ""}</span>
        </p>
        <p>
          Description:{" "}
          <span data-testid="delivery-type-copy-description">
            {copying.description ?? ""}
          </span>
        </p>
        <div style={formRowStyle}>
          <label htmlFor="delivery-type-copy-name">* New name</label>
          <input
            id="delivery-type-copy-name"
            value={copyName}
            onChange={(e) => {
              setCopyName(e.target.value);
              setDirty(true);
            }}
          />
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
            data-testid="delivery-type-copy-submit"
            disabled={saving}
            onClick={() => void copyType()}
          >
            Copy delivery type
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="delivery-type-copy-cancel"
            disabled={saving}
            onClick={closeCopy}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (creating || editing) {
    return (
      <div data-testid="delivery-type-editor">
        <h3>{editing ? "Edit delivery type" : "Add delivery type"}</h3>
        <div style={formRowStyle}>
          <label htmlFor="dt-name">* Name</label>
          <input
            id="dt-name"
            value={name}
            onChange={(e) => {
              setName(e.target.value);
              setDirty(true);
            }}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="dt-bean">* Bean name</label>
          <input
            id="dt-bean"
            value={beanName}
            onChange={(e) => {
              setBeanName(e.target.value);
              setDirty(true);
            }}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="dt-desc">Description</label>
          <input
            id="dt-desc"
            value={description}
            onChange={(e) => {
              setDescription(e.target.value);
              setDirty(true);
            }}
          />
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
            data-testid="delivery-type-save"
            disabled={saving}
            onClick={() => void save()}
          >
            {message(MSG.PUBLISH_SAVE)}
          </button>
          <button type="button" style={buttonStyle} onClick={closeEditor}>
            {message(MSG.PUBLISH_BACK)}
          </button>
        </div>
      </div>
    );
  }

  return (
    <div data-testid="delivery-types-panel">
      <div style={toolbarStyle}>
        <button
          type="button"
          style={buttonStyle}
          data-testid="design-add-delivery-type"
          onClick={openCreate}
        >
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
            <span
              style={{ color: "#666" }}
              data-testid={
                t.deliveryTypeId ? `delivery-type-bean-${t.deliveryTypeId}` : undefined
              }
            >
              {t.beanName}
            </span>
            {t.deliveryTypeId && (
              <>
                <button
                  type="button"
                  style={buttonStyle}
                  data-testid="delivery-type-rename"
                  onClick={() => openRename(t)}
                >
                  Rename
                </button>
                <button
                  type="button"
                  style={buttonStyle}
                  data-testid="delivery-type-copy"
                  onClick={() => openCopy(t)}
                >
                  Copy
                </button>
                <button
                  type="button"
                  style={buttonStyle}
                  data-testid="delivery-type-delete"
                  disabled={saving}
                  onClick={() => void remove(t.deliveryTypeId!)}
                >
                  Delete
                </button>
              </>
            )}
          </li>
        ))}
      </ul>
    </div>
  );
}
