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
import { listItemFilters } from "../../api/developer/itemFiltersApi";
import {
  createContentList,
  deleteContentList,
  updateContentList,
  type ContentListSummary,
} from "../../api/publishing/designApi";
import { message, MSG } from "../../i18n/message";
import {
  itemFilterChoices,
  NO_ITEM_FILTER_LABEL,
  storedItemFilterLabel,
  type ItemFilterChoice,
} from "../contentListItemFilter";
import {
  mapContentListDeleteError,
  mapContentListSaveError,
} from "../contentListSaveErrors";
import { useDirtyForm } from "../dirtyFormContext";
import {
  buttonStyle,
  errorStyle,
  formRowStyle,
  primaryButtonStyle,
  toolbarStyle,
} from "../publishing.styles";
import { isLegacyContentList } from "./designLegacyTypes";

export interface ContentListEditorProps {
  contentList: ContentListSummary | null;
  onSaved: () => void;
  onCancel: () => void;
}

export function ContentListEditor({
  contentList,
  onSaved,
  onCancel,
}: ContentListEditorProps): React.ReactElement {
  const [name, setName] = useState(contentList?.name ?? "");
  const [description, setDescription] = useState(contentList?.description ?? "");
  const [generator, setGenerator] = useState(contentList?.generator ?? "");
  const [url, setUrl] = useState(contentList?.url ?? "");
  const [listType, setListType] = useState(contentList?.listType ?? "modern");
  const [itemFilterId, setItemFilterId] = useState(contentList?.itemFilterId ?? "");
  const [filterChoices, setFilterChoices] = useState<ItemFilterChoice[]>([]);
  const [filtersReady, setFiltersReady] = useState(false);
  const [filtersNote, setFiltersNote] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);
  const { setDirty, confirmIfDirty } = useDirtyForm();

  useEffect(() => {
    setName(contentList?.name ?? "");
    setDescription(contentList?.description ?? "");
    setGenerator(contentList?.generator ?? "");
    setUrl(contentList?.url ?? "");
    setListType(contentList?.listType ?? "modern");
    setItemFilterId(contentList?.itemFilterId ?? "");
    setDirty(false);
  }, [contentList, setDirty]);

  const legacy = isLegacyContentList(listType);

  useEffect(() => {
    if (legacy) {
      return;
    }
    let cancelled = false;
    setFiltersReady(false);
    listItemFilters()
      .then((rows) => {
        if (cancelled) {
          return;
        }
        setFilterChoices(itemFilterChoices(rows));
        setFiltersNote(null);
        setFiltersReady(true);
      })
      .catch(() => {
        if (cancelled) {
          return;
        }
        setFilterChoices([]);
        setFiltersNote("Item filters could not be loaded");
        setFiltersReady(false);
      });
    return () => {
      cancelled = true;
    };
  }, [legacy]);

  async function handleSave(): Promise<void> {
    if (!name.trim()) {
      setError("Name is required");
      return;
    }
    setSaving(true);
    setError(null);
    try {
      const editing = Boolean(contentList?.contentListId);
      // Rename must not change list type, even if the disabled select is altered.
      const effectiveListType = editing
        ? (contentList?.listType ?? listType)
        : listType;
      const effectiveLegacy = isLegacyContentList(effectiveListType);
      const body: ContentListSummary = {
        name: name.trim(),
        description,
        generator: effectiveLegacy ? undefined : generator,
        url: effectiveLegacy ? url : undefined,
        listType: effectiveListType,
      };
      // Omit until the catalog is loaded so a failed lookup cannot clear the filter.
      if (!effectiveLegacy && filtersReady) {
        body.itemFilterId = itemFilterId;
      }
      if (contentList?.contentListId) {
        await updateContentList(contentList.contentListId, body);
      } else {
        await createContentList(body);
      }
      setDirty(false);
      onSaved();
    } catch (e) {
      setError(mapContentListSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  function handleCancel(): void {
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    onCancel();
  }

  async function handleDelete(): Promise<void> {
    if (!contentList?.contentListId) {
      return;
    }
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_DELETE_DESIGN))) {
      return;
    }
    setSaving(true);
    setError(null);
    try {
      await deleteContentList(contentList.contentListId);
      setDirty(false);
      onSaved();
    } catch (e) {
      setError(mapContentListDeleteError(e));
    } finally {
      setSaving(false);
    }
  }

  return (
    <div data-testid="contentlist-editor">
      <h3>
        {contentList?.contentListId ? "Edit content list" : "Create content list"}
      </h3>
      <div style={formRowStyle}>
        <label htmlFor="cl-name">* Name</label>
        <input
          id="cl-name"
          value={name}
          onChange={(e) => {
            setName(e.target.value);
            setDirty(true);
          }}
        />
      </div>
      <div style={formRowStyle}>
        <label htmlFor="cl-desc">Description</label>
        <input
          id="cl-desc"
          value={description}
          onChange={(e) => {
            setDescription(e.target.value);
            setDirty(true);
          }}
        />
      </div>
      <div style={formRowStyle}>
        <label htmlFor="cl-type">Type</label>
        <select
          id="cl-type"
          value={listType}
          onChange={(e) => {
            setListType(e.target.value);
            setDirty(true);
          }}
          disabled={Boolean(contentList?.contentListId)}
          title={
            contentList?.contentListId
              ? "Content list type cannot be changed"
              : undefined
          }
        >
          <option value="modern">Modern</option>
          <option value="legacy">Legacy</option>
        </select>
      </div>
      {legacy ? (
        <div style={formRowStyle}>
          <label htmlFor="cl-url">Legacy URL / resource</label>
          <input
            id="cl-url"
            value={url}
            onChange={(e) => {
              setUrl(e.target.value);
              setDirty(true);
            }}
          />
        </div>
      ) : (
        <div style={formRowStyle}>
          <label htmlFor="cl-gen">Generator</label>
          <input
            id="cl-gen"
            value={generator}
            onChange={(e) => {
              setGenerator(e.target.value);
              setDirty(true);
            }}
          />
        </div>
      )}
      {!legacy && (
        <div style={formRowStyle}>
          <label htmlFor="cl-item-filter">Item filter</label>
          <select
            id="cl-item-filter"
            data-testid="contentlist-item-filter"
            value={selectFilterValue(itemFilterId, filterChoices, contentList)}
            disabled={saving || !filtersReady}
            onChange={(e) => {
              setItemFilterId(e.target.value);
              setDirty(true);
            }}
          >
            <option value="">{NO_ITEM_FILTER_LABEL}</option>
            {filterOptions(itemFilterId, filterChoices, contentList).map((choice) => (
              <option key={choice.id} value={choice.id}>
                {choice.name}
              </option>
            ))}
          </select>
          <p data-testid="contentlist-stored-item-filter">
            Saved item filter: {storedItemFilterLabel(contentList)}
          </p>
          {filtersNote && <p data-testid="contentlist-item-filter-note">{filtersNote}</p>}
        </div>
      )}
      {error && (
        <p style={errorStyle} role="alert">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        <button
          type="button"
          style={primaryButtonStyle}
          data-testid="contentlist-save"
          disabled={saving}
          onClick={() => void handleSave()}
        >
          {message(MSG.PUBLISH_SAVE)}
        </button>
        <button type="button" style={buttonStyle} onClick={handleCancel}>
          {message(MSG.PUBLISH_BACK)}
        </button>
        {contentList?.contentListId && (
          <button
            type="button"
            data-testid="contentlist-delete"
            style={buttonStyle}
            disabled={saving}
            onClick={() => void handleDelete()}
          >
            Delete
          </button>
        )}
      </div>
    </div>
  );
}

function filterOptions(
  selectedId: string,
  choices: ItemFilterChoice[],
  contentList: ContentListSummary | null,
): ItemFilterChoice[] {
  if (!selectedId || choices.some((choice) => choice.id === selectedId)) {
    return choices;
  }
  const name = contentList?.itemFilterName?.trim() || selectedId;
  return [...choices, { id: selectedId, name }];
}

function selectFilterValue(
  selectedId: string,
  choices: ItemFilterChoice[],
  contentList: ContentListSummary | null,
): string {
  if (!selectedId) {
    return "";
  }
  return filterOptions(selectedId, choices, contentList).some(
    (choice) => choice.id === selectedId,
  )
    ? selectedId
    : "";
}
