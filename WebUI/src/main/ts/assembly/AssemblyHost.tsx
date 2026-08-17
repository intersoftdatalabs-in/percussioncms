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

/**
 * Preview-first Active Assembly host. Renders the assembled page or snippet
 * template in an iframe with a light overlay. Slot add / create / arrange
 * use relationship REST (no Data Flow HTML).
 */

import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router";
import {
  findAllowedTemplateMenus,
  mapActionMenusToMenuActions,
} from "../api/contentExplorer/actionMenuApi";
import {
  fetchPreviewLocation,
  type PreviewLocation,
} from "../api/contentExplorer/assemblyApi";
import {
  addSlotRelationship,
  changeSlotTemplateSlot,
  fetchSlotAllowedTemplates,
  fetchSlotAllowedTypes,
  fetchSlotCanvas,
  moveSlotRelationship,
  removeSlotRelationship,
  type SlotAllowedChoice,
  type SlotCanvas,
} from "../api/contentExplorer/slotRelationshipApi";
import type { MenuAction, SelectionResult } from "../api/contentExplorer/types";
import { ContentBrowser } from "../contentBrowser/ContentBrowser";
import {
  dispatchAction,
  parseTemplateIdFromAction,
} from "../contentExplorer/actionDispatch";
import { parseExplorerContentId } from "../contentExplorer/menuCatalogLoad";
import { createEditorItem } from "../editor/itemCreateApi";
import { EXPLORER_MSG } from "../contentExplorer/messages";
import { message } from "../i18n/message";
import { parsePositiveInt, withCmsContextPrefix } from "./assemblyHostUrl";
import styles from "./AssemblyHost.module.css";
import { ASSEMBLY_MSG } from "./messages";
import type { AssemblySlotContext } from "./slotContext";
import {
  replaceSlotDependentPickerSession,
  resolveSlotDependentPick,
  settleSlotDependentPickerSession,
  type SlotDependentPick,
  type SlotDependentPickerSession,
} from "./slotDependentPick";

export interface AssemblyTemplateOption {
  id: number;
  label: string;
}

export interface AssemblyHostProps {
  fetchPreview?: (
    contentId: number,
    templateId: number,
  ) => Promise<PreviewLocation>;
  loadTemplates?: (contentId: number) => Promise<MenuAction[]>;
  loadCanvas?: typeof fetchSlotCanvas;
  addToSlot?: typeof addSlotRelationship;
  removeSlotRel?: typeof removeSlotRelationship;
  moveSlotRel?: typeof moveSlotRelationship;
  changeSlotTemplate?: typeof changeSlotTemplateSlot;
  loadAllowedTypes?: typeof fetchSlotAllowedTypes;
  loadAllowedTemplates?: typeof fetchSlotAllowedTemplates;
  createItem?: typeof createEditorItem;
}

export function templateOptionsFromMenus(
  menus: MenuAction[],
): AssemblyTemplateOption[] {
  const out: AssemblyTemplateOption[] = [];
  const seen = new Set<number>();
  for (const menu of menus) {
    const id = parseTemplateIdFromAction(menu);
    if (id == null || seen.has(id)) {
      continue;
    }
    seen.add(id);
    out.push({ id, label: menu.label || menu.name || String(id) });
  }
  return out;
}

export async function loadAssemblyTemplates(
  contentId: number,
): Promise<MenuAction[]> {
  const aa = mapActionMenusToMenuActions(
    await findAllowedTemplateMenus(contentId, true),
  );
  if (aa.length > 0) {
    return aa;
  }
  return mapActionMenusToMenuActions(
    await findAllowedTemplateMenus(contentId, false),
  );
}

/** Catch fire-and-forget dialog work so a thrown API error surfaces a notice. */
export async function runSlotDialogWork(
  work: () => Promise<void>,
  onFail: () => void,
): Promise<void> {
  try {
    await work();
  } catch {
    onFail();
  }
}

function resolveAssemblerHref(previewUrl: string): string | null {
  const trimmed = previewUrl.trim();
  if (!trimmed) {
    return null;
  }
  const href = /^https?:\/\//i.test(trimmed)
    ? trimmed
    : withCmsContextPrefix(trimmed.startsWith("/") ? trimmed : `/${trimmed}`);
  if (!href.toLowerCase().includes("/assembler/render")) {
    return null;
  }
  return href;
}

export function AssemblyHost({
  fetchPreview = fetchPreviewLocation,
  loadTemplates = loadAssemblyTemplates,
  loadCanvas = fetchSlotCanvas,
  addToSlot = addSlotRelationship,
  removeSlotRel = removeSlotRelationship,
  moveSlotRel = moveSlotRelationship,
  changeSlotTemplate = changeSlotTemplateSlot,
  loadAllowedTypes = fetchSlotAllowedTypes,
  loadAllowedTemplates = fetchSlotAllowedTemplates,
  createItem = createEditorItem,
}: AssemblyHostProps = {}): React.ReactElement {
  const [params] = useSearchParams();
  const contentId = parsePositiveInt(params.get("contentId"));
  const requestedTemplateId = parsePositiveInt(params.get("templateId"));

  const [templates, setTemplates] = useState<AssemblyTemplateOption[]>([]);
  const [templateId, setTemplateId] = useState<number | null>(null);
  const [previewHref, setPreviewHref] = useState<string | null>(null);
  const [errorKey, setErrorKey] = useState<string | null>(
    contentId == null ? ASSEMBLY_MSG.MISSING_ITEM : null,
  );
  const [loading, setLoading] = useState(contentId != null);
  const [canvas, setCanvas] = useState<SlotCanvas | null>(null);
  const [selectedSlotId, setSelectedSlotId] = useState<number | null>(null);
  const [selectedRelId, setSelectedRelId] = useState<number | null>(null);
  const [slotNotice, setSlotNotice] = useState<string | null>(null);
  const [dialog, setDialog] = useState<
    null | "add" | "create" | "change"
  >(null);
  const [choices, setChoices] = useState<SlotAllowedChoice[]>([]);
  const [typeChoices, setTypeChoices] = useState<SlotAllowedChoice[]>([]);
  const [pickedType, setPickedType] = useState("");
  const [pickedTemplate, setPickedTemplate] = useState("");
  const [pickedSlot, setPickedSlot] = useState("");
  const [pickedFolder, setPickedFolder] = useState("");
  const slotPickerRef = useRef<SlotDependentPickerSession | null>(null);

  useEffect(() => {
    document.title = message(ASSEMBLY_MSG.TITLE);
  }, []);

  useEffect(() => {
    if (contentId == null) {
      setLoading(false);
      setErrorKey(ASSEMBLY_MSG.MISSING_ITEM);
      return;
    }
    let cancelled = false;
    setLoading(true);
    setErrorKey(null);
    void (async () => {
      try {
        const menus = await loadTemplates(contentId);
        if (cancelled) {
          return;
        }
        const options = templateOptionsFromMenus(menus);
        setTemplates(options);
        const requestedInList =
          requestedTemplateId != null &&
          options.some((o) => o.id === requestedTemplateId);
        if (
          requestedTemplateId != null &&
          options.length > 0 &&
          !requestedInList
        ) {
          setTemplateId(null);
          setPreviewHref(null);
          setErrorKey(ASSEMBLY_MSG.TEMPLATE_MISMATCH);
          setLoading(false);
          return;
        }
        const nextId =
          (requestedInList ||
          (requestedTemplateId != null && options.length === 0)
            ? requestedTemplateId
            : null) ??
          options[0]?.id ??
          null;
        setTemplateId(nextId);
        if (nextId == null) {
          setPreviewHref(null);
          setErrorKey(ASSEMBLY_MSG.NO_TEMPLATE);
          setLoading(false);
        }
      } catch {
        if (cancelled) {
          return;
        }
        setTemplateId(null);
        setPreviewHref(null);
        setErrorKey(ASSEMBLY_MSG.NO_TEMPLATE);
        setLoading(false);
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [contentId, requestedTemplateId, loadTemplates]);

  useEffect(() => {
    if (contentId == null || templateId == null) {
      return;
    }
    let cancelled = false;
    setLoading(true);
    setErrorKey(null);
    void (async () => {
      try {
        const loc = await fetchPreview(contentId, templateId);
        if (cancelled) {
          return;
        }
        const href = resolveAssemblerHref(loc.previewUrl);
        if (href == null) {
          setPreviewHref(null);
          setErrorKey(ASSEMBLY_MSG.PREVIEW_FAILED);
        } else {
          setPreviewHref(href);
        }
      } catch {
        if (!cancelled) {
          setPreviewHref(null);
          setErrorKey(ASSEMBLY_MSG.PREVIEW_FAILED);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [contentId, templateId, fetchPreview]);

  const selectedLabel = useMemo(() => {
    return templates.find((t) => t.id === templateId)?.label;
  }, [templates, templateId]);

  const refreshCanvas = useCallback(async () => {
    if (contentId == null) {
      setCanvas(null);
      return;
    }
    try {
      const next = await loadCanvas(contentId, templateId);
      setCanvas(next);
    } catch {
      setCanvas({ ownerId: contentId, templateId, slots: [] });
    }
  }, [contentId, templateId, loadCanvas]);

  useEffect(() => {
    void refreshCanvas();
  }, [refreshCanvas]);

  const selectedSlot = canvas?.slots.find((s) => s.slotId === selectedSlotId) ?? null;
  const slotCtx: AssemblySlotContext | null =
    contentId != null && selectedSlotId != null
      ? {
          ownerId: contentId,
          slotId: selectedSlotId,
          relationshipId: selectedRelId,
          snippetTemplateId:
            selectedSlot?.items.find((i) => i.relationshipId === selectedRelId)
              ?.templateId ?? null,
          ownerTemplateId: templateId,
          folderPath: pickedFolder || null,
        }
      : null;

  const finishSlotDependentPick = useCallback((value: SlotDependentPick | null) => {
    const current = slotPickerRef.current;
    slotPickerRef.current = null;
    setDialog((open) => (open === "add" ? null : open));
    settleSlotDependentPickerSession(current, value);
  }, []);

  const pickSlotDependent = useCallback(
    (slot: AssemblySlotContext) => {
      return new Promise<SlotDependentPick | null>((resolve) => {
        slotPickerRef.current = replaceSlotDependentPickerSession(
          slotPickerRef.current,
          { slot, resolve },
        );
        setDialog("add");
      });
    },
    [],
  );

  useEffect(() => {
    return () => {
      const current = slotPickerRef.current;
      slotPickerRef.current = null;
      settleSlotDependentPickerSession(current, null);
    };
  }, []);

  async function runSlotAction(name: string): Promise<void> {
    setSlotNotice(null);
    try {
      const result = await dispatchAction(
        { name, label: name, url: "", sortRank: 0, menuType: "MENUITEM" },
        {
          item: null,
          slot: slotCtx,
          addToSlot,
          removeSlotRel,
          moveSlotRel,
          changeSlotTemplate,
          createItem,
          pickSlotDependent,
          pickSlotCreate: async () => {
            setDialog("create");
            return null;
          },
          pickSlotTemplateSlot: async () => {
            setDialog("change");
            return null;
          },
        },
      );
      if (result.messageKey) {
        setSlotNotice(message(result.messageKey));
      }
      if (result.refresh) {
        await refreshCanvas();
      }
    } catch {
      setSlotNotice(message(ASSEMBLY_MSG.SLOT_FAILED));
    }
  }

  return (
    <div className={styles.root} data-testid="assembly-host">
      <header className={styles.bar} data-testid="assembly-overlay">
        <span className={styles.title}>{message(ASSEMBLY_MSG.TITLE)}</span>
        <span className={styles.badge}>{message(ASSEMBLY_MSG.BADGE_PREVIEW)}</span>
        {contentId != null ? (
          <span className={styles.meta} data-testid="assembly-content-id">
            <span className={styles.label}>{message(ASSEMBLY_MSG.CONTENT_ID)}</span>
            {contentId}
          </span>
        ) : null}
        {errorKey === ASSEMBLY_MSG.TEMPLATE_MISMATCH && requestedTemplateId != null ? (
          <span className={styles.meta} data-testid="assembly-requested-template">
            <span className={styles.label}>{message(ASSEMBLY_MSG.TEMPLATE_LABEL)}</span>
            {requestedTemplateId}
          </span>
        ) : null}
        {templates.length > 0 ? (
          <label>
            <span className={styles.label}>{message(ASSEMBLY_MSG.TEMPLATE_LABEL)}</span>
            <select
              className={styles.select}
              data-testid="assembly-template-select"
              aria-label={message(ASSEMBLY_MSG.TEMPLATE_LABEL)}
              value={templateId ?? ""}
              onChange={(e) => {
                const next = parsePositiveInt(e.target.value);
                setTemplateId(next);
              }}
            >
              {templates.map((t) => (
                <option key={t.id} value={t.id}>
                  {t.label}
                </option>
              ))}
            </select>
          </label>
        ) : selectedLabel || templateId != null || requestedTemplateId != null ? (
          <span className={styles.meta} data-testid="assembly-template-id">
            <span className={styles.label}>{message(ASSEMBLY_MSG.TEMPLATE_LABEL)}</span>
            {selectedLabel ?? templateId ?? requestedTemplateId}
          </span>
        ) : null}
        <span className={styles.note}>{message(ASSEMBLY_MSG.NOTE)}</span>
        <button
          type="button"
          className={styles.close}
          data-testid="assembly-close"
          onClick={() => {
            if (typeof window !== "undefined") {
              window.close();
            }
          }}
        >
          {message(ASSEMBLY_MSG.CLOSE)}
        </button>
      </header>
      <div className={styles.slotBar} data-testid="assembly-slot-bar">
        <span className={styles.label}>{message(ASSEMBLY_MSG.SLOTS)}</span>
        {canvas == null || canvas.slots.length === 0 ? (
          <span data-testid="assembly-slot-empty">{message(ASSEMBLY_MSG.SLOT_EMPTY)}</span>
        ) : (
          <div className={styles.slotList} data-testid="assembly-slot-list">
            {canvas.slots.map((slot) => (
              <button
                key={slot.slotId}
                type="button"
                className={styles.slotChip}
                data-testid={`assembly-slot-${slot.slotId}`}
                data-selected={selectedSlotId === slot.slotId ? "true" : "false"}
                onClick={() => {
                  setSelectedSlotId(slot.slotId);
                  setSelectedRelId(null);
                  setSlotNotice(null);
                }}
              >
                {slot.label || slot.name || slot.slotId}
              </button>
            ))}
          </div>
        )}
        {selectedSlot && selectedSlot.items.length > 0 ? (
          <div className={styles.slotList} data-testid="assembly-slot-items">
            {selectedSlot.items.map((item) => (
              <button
                key={item.relationshipId}
                type="button"
                className={styles.itemChip}
                data-testid={`assembly-slot-item-${item.relationshipId}`}
                data-selected={selectedRelId === item.relationshipId ? "true" : "false"}
                onClick={() => setSelectedRelId(item.relationshipId)}
              >
                {message(ASSEMBLY_MSG.SLOT_ITEM)} {item.dependentId}
              </button>
            ))}
          </div>
        ) : null}
        <div className={styles.slotActions}>
          <button
            type="button"
            className={styles.slotBtn}
            data-testid="assembly-slot-add"
            disabled={selectedSlotId == null}
            onClick={() => void runSlotAction("Slot_Add")}
          >
            {message(ASSEMBLY_MSG.SLOT_ADD)}
          </button>
          <button
            type="button"
            className={styles.slotBtn}
            data-testid="assembly-slot-create"
            disabled={selectedSlotId == null}
            onClick={() => void runSlotAction("Slot_Create")}
          >
            {message(ASSEMBLY_MSG.SLOT_CREATE)}
          </button>
          <button
            type="button"
            className={styles.slotBtn}
            data-testid="assembly-slot-move-up"
            disabled={selectedRelId == null}
            onClick={() => void runSlotAction("Arrange_MoveUpLeft")}
          >
            {message(ASSEMBLY_MSG.SLOT_MOVE_UP)}
          </button>
          <button
            type="button"
            className={styles.slotBtn}
            data-testid="assembly-slot-move-down"
            disabled={selectedRelId == null}
            onClick={() => void runSlotAction("Arrange_MoveDownRight")}
          >
            {message(ASSEMBLY_MSG.SLOT_MOVE_DOWN)}
          </button>
          <button
            type="button"
            className={styles.slotBtn}
            data-testid="assembly-slot-change"
            disabled={selectedRelId == null}
            onClick={() => void runSlotAction("Arrange_ChangeTemplateSlot")}
          >
            {message(ASSEMBLY_MSG.SLOT_CHANGE)}
          </button>
          <button
            type="button"
            className={styles.slotBtn}
            data-testid="assembly-slot-remove"
            disabled={selectedRelId == null}
            onClick={() => void runSlotAction("Arrange_Remove")}
          >
            {message(ASSEMBLY_MSG.SLOT_REMOVE)}
          </button>
        </div>
        {slotNotice ? (
          <span role="status" data-testid="assembly-slot-notice">
            {slotNotice}
          </span>
        ) : null}
      </div>
      {dialog === "add" && selectedSlotId != null && contentId != null ? (
        <div className={styles.dialog} data-testid="assembly-slot-add-dialog">
          <div className={styles.dialogPanel}>
            <ContentBrowser
              mode="select"
              allowItemSelect
              allowFolderSelect={false}
              title={message(ASSEMBLY_MSG.SLOT_BROWSER_TITLE)}
              onCancel={() => finishSlotDependentPick(null)}
              onConfirm={(selection: SelectionResult) => {
                void runSlotDialogWork(
                  async () => {
                    const slotId =
                      slotPickerRef.current?.slot.slotId ?? selectedSlotId;
                    const picked = await resolveSlotDependentPick(
                      selection,
                      slotId,
                      loadAllowedTemplates,
                    );
                    finishSlotDependentPick(picked);
                  },
                  () => {
                    finishSlotDependentPick(null);
                    setSlotNotice(message(ASSEMBLY_MSG.SLOT_FAILED));
                  },
                );
              }}
            />
          </div>
        </div>
      ) : null}
      {dialog === "create" && selectedSlotId != null && contentId != null ? (
        <SlotCreateDialog
          slotId={selectedSlotId}
          folderPath={pickedFolder}
          typeChoices={typeChoices}
          templateChoices={choices}
          pickedType={pickedType}
          pickedTemplate={pickedTemplate}
          onFolder={setPickedFolder}
          onType={setPickedType}
          onTemplate={setPickedTemplate}
          onLoad={async () => {
            const types = await loadAllowedTypes(selectedSlotId);
            setTypeChoices(types);
            if (types[0] && !pickedType) {
              setPickedType(types[0].name || String(types[0].id));
            }
            const tpls = await loadAllowedTemplates(selectedSlotId);
            setChoices(tpls);
            if (tpls[0] && !pickedTemplate) {
              setPickedTemplate(String(tpls[0].id));
            }
          }}
          onCancel={() => setDialog(null)}
          onApply={() => {
            void runSlotDialogWork(
              async () => {
                if (!pickedFolder || !pickedType) {
                  setSlotNotice(message(ASSEMBLY_MSG.SLOT_FAILED));
                  return;
                }
                const snippetTemplateId = Number(pickedTemplate);
                if (!Number.isFinite(snippetTemplateId) || snippetTemplateId <= 0) {
                  setSlotNotice(message(EXPLORER_MSG.ACTION_NEEDS_TEMPLATE));
                  return;
                }
                const created = await createItem({
                  contentType: pickedType,
                  folderPath: pickedFolder,
                });
                const createdId = parseExplorerContentId(created.itemId);
                if (createdId == null) {
                  setSlotNotice(message(ASSEMBLY_MSG.SLOT_FAILED));
                  return;
                }
                await addToSlot({
                  ownerId: contentId,
                  dependentId: createdId,
                  slotId: selectedSlotId,
                  templateId: snippetTemplateId,
                });
                setDialog(null);
                await refreshCanvas();
              },
              () => setSlotNotice(message(ASSEMBLY_MSG.SLOT_FAILED)),
            );
          }}
        />
      ) : null}
      {dialog === "change" && selectedRelId != null ? (
        <SlotChangeDialog
          slots={canvas?.slots ?? []}
          templates={choices}
          pickedSlot={pickedSlot || String(selectedSlotId ?? "")}
          pickedTemplate={pickedTemplate}
          onSlot={setPickedSlot}
          onTemplate={setPickedTemplate}
          onLoad={async () => {
            if (selectedSlotId != null) {
              const tpls = await loadAllowedTemplates(selectedSlotId);
              setChoices(tpls);
              if (tpls[0] && !pickedTemplate) {
                setPickedTemplate(String(tpls[0].id));
              }
            }
            if (!pickedSlot && selectedSlotId != null) {
              setPickedSlot(String(selectedSlotId));
            }
          }}
          onCancel={() => setDialog(null)}
          onApply={() => {
            void runSlotDialogWork(
              async () => {
                const nextSlot = Number(pickedSlot);
                const nextTpl = Number(pickedTemplate);
                if (
                  !Number.isFinite(nextSlot) ||
                  nextSlot <= 0 ||
                  !Number.isFinite(nextTpl) ||
                  nextTpl <= 0
                ) {
                  setSlotNotice(message(EXPLORER_MSG.ACTION_NEEDS_TEMPLATE));
                  return;
                }
                await changeSlotTemplate(selectedRelId, nextSlot, nextTpl);
                setDialog(null);
                await refreshCanvas();
              },
              () => setSlotNotice(message(ASSEMBLY_MSG.SLOT_FAILED)),
            );
          }}
        />
      ) : null}
      <div className={styles.stage} data-testid="assembly-stage">
        {errorKey ? (
          <div className={styles.status} role="alert" data-testid="assembly-error">
            {message(errorKey)}
          </div>
        ) : loading && !previewHref ? (
          <div className={styles.status} role="status" data-testid="assembly-loading">
            {message(ASSEMBLY_MSG.LOADING)}
          </div>
        ) : previewHref ? (
          <iframe
            className={styles.iframe}
            data-testid="assembly-preview-frame"
            title={message(ASSEMBLY_MSG.IFRAME_TITLE)}
            src={previewHref}
          />
        ) : null}
      </div>
    </div>
  );
}

function SlotCreateDialog({
  slotId,
  folderPath,
  typeChoices,
  templateChoices,
  pickedType,
  pickedTemplate,
  onFolder,
  onType,
  onTemplate,
  onLoad,
  onCancel,
  onApply,
}: {
  slotId: number;
  folderPath: string;
  typeChoices: SlotAllowedChoice[];
  templateChoices: SlotAllowedChoice[];
  pickedType: string;
  pickedTemplate: string;
  onFolder: (path: string) => void;
  onType: (value: string) => void;
  onTemplate: (value: string) => void;
  onLoad: () => Promise<void>;
  onCancel: () => void;
  onApply: () => void;
}): React.ReactElement {
  useEffect(() => {
    void onLoad();
  }, [slotId]);
  return (
    <div className={styles.dialog} data-testid="assembly-slot-create-dialog">
      <div className={`${styles.dialogPanel} ${styles.picker}`}>
        <label>
          {message(ASSEMBLY_MSG.SLOT_FOLDER_LABEL)}
          <input
            data-testid="assembly-slot-create-folder"
            value={folderPath}
            onChange={(e) => onFolder(e.target.value)}
          />
        </label>
        <label>
          {message(ASSEMBLY_MSG.SLOT_TYPE_LABEL)}
          <select
            data-testid="assembly-slot-create-type"
            value={pickedType}
            onChange={(e) => onType(e.target.value)}
          >
            {typeChoices.map((t) => (
              <option key={t.id} value={t.name || String(t.id)}>
                {t.label || t.name || t.id}
              </option>
            ))}
          </select>
        </label>
        <label>
          {message(ASSEMBLY_MSG.SLOT_TEMPLATE_LABEL)}
          <select
            data-testid="assembly-slot-create-template"
            value={pickedTemplate}
            onChange={(e) => onTemplate(e.target.value)}
          >
            {templateChoices.map((t) => (
              <option key={t.id} value={t.id}>
                {t.label || t.name || t.id}
              </option>
            ))}
          </select>
        </label>
        <div className={styles.pickerActions}>
          <button type="button" data-testid="assembly-slot-create-cancel" onClick={onCancel}>
            {message(ASSEMBLY_MSG.SLOT_CANCEL)}
          </button>
          <button type="button" data-testid="assembly-slot-create-apply" onClick={onApply}>
            {message(ASSEMBLY_MSG.SLOT_APPLY)}
          </button>
        </div>
      </div>
    </div>
  );
}

function SlotChangeDialog({
  slots,
  templates,
  pickedSlot,
  pickedTemplate,
  onSlot,
  onTemplate,
  onLoad,
  onCancel,
  onApply,
}: {
  slots: SlotCanvas["slots"];
  templates: SlotAllowedChoice[];
  pickedSlot: string;
  pickedTemplate: string;
  onSlot: (value: string) => void;
  onTemplate: (value: string) => void;
  onLoad: () => Promise<void>;
  onCancel: () => void;
  onApply: () => void;
}): React.ReactElement {
  useEffect(() => {
    void onLoad();
  }, []);
  return (
    <div className={styles.dialog} data-testid="assembly-slot-change-dialog">
      <div className={`${styles.dialogPanel} ${styles.picker}`}>
        <label>
          {message(ASSEMBLY_MSG.SLOTS)}
          <select
            data-testid="assembly-slot-change-slot"
            value={pickedSlot}
            onChange={(e) => onSlot(e.target.value)}
          >
            {slots.map((s) => (
              <option key={s.slotId} value={s.slotId}>
                {s.label || s.name || s.slotId}
              </option>
            ))}
          </select>
        </label>
        <label>
          {message(ASSEMBLY_MSG.SLOT_TEMPLATE_LABEL)}
          <select
            data-testid="assembly-slot-change-template"
            value={pickedTemplate}
            onChange={(e) => onTemplate(e.target.value)}
          >
            {templates.map((t) => (
              <option key={t.id} value={t.id}>
                {t.label || t.name || t.id}
              </option>
            ))}
          </select>
        </label>
        <div className={styles.pickerActions}>
          <button type="button" data-testid="assembly-slot-change-cancel" onClick={onCancel}>
            {message(ASSEMBLY_MSG.SLOT_CANCEL)}
          </button>
          <button type="button" data-testid="assembly-slot-change-apply" onClick={onApply}>
            {message(ASSEMBLY_MSG.SLOT_APPLY)}
          </button>
        </div>
      </div>
    </div>
  );
}
