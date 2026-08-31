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

import React, { useEffect, useRef, useState } from "react";
import { resolveContentTypeObjectGuid } from "../api/displayFormatGuid";
import {
  asContentTypeText,
  getContentTypeAllowedTemplates,
  getContentTypeDetail,
  getContentTypeItemExits,
  getContentTypeSearchIndexing,
  getFieldControlProperties,
  lockContentType,
  replaceContentTypeAllowedTemplates,
  replaceContentTypeItemExits,
  replaceFieldControlProperties,
  setContentTypeAllowedWorkflows,
  setContentTypeEnabled,
  setContentTypeSearchIndexing,
  unlockContentType,
  updateContentTypeDetail,
  type ContentTypeUpdateBody,
} from "../api/developer/contentTypesApi";
import {
  cloneContentTypeItemExits,
  contentTypeItemExitsEqual,
  emptyContentTypeItemExits,
  itemExitListsEqual,
} from "../api/developer/contentTypeItemExits";
import { ContentTypeItemExitsSection } from "./ContentTypeItemExitsSection";
import {
  normalizeContentTypeDesignGaps,
  normalizeContentTypeFields,
  normalizeContentTypeStringList,
  normalizeNamedObjectRefs,
} from "../api/developer/contentTypeLists";
import type {
  ContentTypeControlProperty,
  ContentTypeDetail,
  ContentTypeFieldSummary,
  ContentTypeItemExits,
  NamedObjectRef,
} from "../api/developer/types";
import {
  cloneControlProperties,
  controlPropertiesEqual,
  toControlPropertyPayload,
} from "./contentTypeControlProperties";
import {
  buildAllowedWorkflowsReplaceBody,
  cloneNamedObjectRefs,
  namedObjectRefsEqual,
  toNamedObjectRefPayload,
  withDefaultWorkflowFlags,
} from "./contentTypeWorkflows";
import {
  designGapCode,
  designGapKey,
  formatDesignGap,
  type DesignGapWire,
} from "../api/developer/designGaps";
import { ObjectAclSection } from "./ObjectAclSection";
import {
  ContentTypeFieldRulesSection,
  type ContentTypeFieldRulesHandle,
} from "./ContentTypeFieldRulesSection";
import { isApiError } from "../api/client";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import { catalogColors, tableHeaderRow, tableRow } from "./catalogStyles";


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

type FieldDraft = {
  name: string;
  searchable: boolean;
  required: boolean;
};

function fieldKey(f: ContentTypeFieldSummary): string {
  return `${f.fieldSet || "parent"}:${f.name || ""}`;
}

function toDrafts(fields: unknown): Record<string, FieldDraft> {
  const out: Record<string, FieldDraft> = {};
  for (const f of normalizeContentTypeFields(fields)) {
    if (!f.name) continue;
    out[fieldKey(f)] = {
      name: f.name,
      searchable: !!f.searchable,
      required: !!f.required,
    };
  }
  return out;
}

function contentTypeFields(detail: ContentTypeDetail | null | undefined): ContentTypeFieldSummary[] {
  return normalizeContentTypeFields(detail?.fields);
}

function contentTypeChildSets(detail: ContentTypeDetail | null | undefined): string[] {
  return normalizeContentTypeStringList(detail?.childFieldSets);
}

function contentTypeDesignGaps(detail: ContentTypeDetail | null | undefined): DesignGapWire[] {
  return normalizeContentTypeDesignGaps(detail?.designGaps);
}

function normalizeDetailLists(d: ContentTypeDetail): ContentTypeDetail {
  return {
    ...d,
    fields: normalizeContentTypeFields(d.fields),
    childFieldSets: normalizeContentTypeStringList(d.childFieldSets),
    allowedWorkflows: normalizeNamedObjectRefs(d.allowedWorkflows),
    allowedTemplates: normalizeNamedObjectRefs(d.allowedTemplates),
    designGaps: normalizeContentTypeDesignGaps(d.designGaps),
  };
}

function refKey(r: NamedObjectRef, index: number): string {
  if (r.name) return `name:${r.name}`;
  if (r.guid?.stringValue) return `guid:${r.guid.stringValue}`;
  if (r.guid?.uuid != null) return `uuid:${r.guid.uuid}`;
  return `idx:${index}`;
}

/** Canonical Percussion GUID shape: type-host-uuid (three numeric groups). */
const PERC_GUID_RE = /^\d+-\d+-\d+$/;

export function ContentTypeDetailPanel({
  idOrName,
  catalogGuid,
  onBack,
}: {
  idOrName: string;
  /** GUID from catalog list row when detail wire omits stringValue (#3319). */
  catalogGuid?: string | null;
  onBack: () => void;
}): React.ReactElement {
  const [detail, setDetail] = useState<ContentTypeDetail | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [notice, setNotice] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [label, setLabel] = useState("");
  const [description, setDescription] = useState("");
  const [enabled, setEnabled] = useState(true);
  /** Type-level search indexing (CD-10); default on. Distinct from per-field searchable. */
  const [searchIndexing, setSearchIndexing] = useState(true);
  const [savedSearchIndexing, setSavedSearchIndexing] = useState(true);
  const [fieldDrafts, setFieldDrafts] = useState<Record<string, FieldDraft>>({});
  const [workflows, setWorkflows] = useState<NamedObjectRef[]>([]);
  const [templates, setTemplates] = useState<NamedObjectRef[]>([]);
  const [newWfName, setNewWfName] = useState("");
  const [newTplName, setNewTplName] = useState("");
  const [itemExits, setItemExits] = useState<ContentTypeItemExits>(() =>
    emptyContentTypeItemExits(),
  );
  const [savedItemExits, setSavedItemExits] = useState<ContentTypeItemExits>(() =>
    emptyContentTypeItemExits(),
  );
  const [itemExitsLoaded, setItemExitsLoaded] = useState(false);
  const [itemExitsError, setItemExitsError] = useState<string | null>(null);
  const [selectedFieldName, setSelectedFieldName] = useState("");
  const [controlProps, setControlProps] = useState<ContentTypeControlProperty[]>([]);
  const [controlPropsInitial, setControlPropsInitial] = useState<
    ContentTypeControlProperty[]
  >([]);
  const [controlName, setControlName] = useState("");
  const [choiceSummary, setChoiceSummary] = useState<string | null>(null);
  const [controlPropsLoading, setControlPropsLoading] = useState(false);
  const [controlPropsError, setControlPropsError] = useState<string | null>(null);
  const [newPropName, setNewPropName] = useState("");
  const [newPropValue, setNewPropValue] = useState("");
  const [heldLock, setHeldLock] = useState(false);
  const heldLockRef = useRef(false);
  const fieldRulesRef = useRef<ContentTypeFieldRulesHandle | null>(null);
  const [fieldRulesDirty, setFieldRulesDirty] = useState(false);

  useEffect(() => {
    heldLockRef.current = heldLock;
  }, [heldLock]);

  useEffect(() => {
    const currentId = idOrName;
    return () => {
      if (heldLockRef.current) {
        heldLockRef.current = false;
        void unlockContentType(currentId).catch(() => undefined);
      }
    };
  }, [idOrName]);

  useEffect(() => {
    let cancelled = false;
    setDetail(null);
    setError(null);
    setNotice(null);
    setHeldLock(false);
    heldLockRef.current = false;
    setSearchIndexing(true);
    setSavedSearchIndexing(true);
    getContentTypeSearchIndexing(idOrName)
      .then((si) => {
        if (cancelled) return;
        const on = si.searchIndexing !== false;
        setSearchIndexing(on);
        setSavedSearchIndexing(on);
      })
      .catch(() => {
        if (cancelled) return;
        setSearchIndexing(true);
        setSavedSearchIndexing(true);
      });
    setItemExits(emptyContentTypeItemExits());
    setSavedItemExits(emptyContentTypeItemExits());
    setItemExitsLoaded(false);
    setItemExitsError(null);
    getContentTypeItemExits(idOrName)
      .then((env) => {
        if (cancelled) return;
        const cloned = cloneContentTypeItemExits(env);
        setItemExits(cloned);
        setSavedItemExits(cloneContentTypeItemExits(cloned));
        setItemExitsLoaded(true);
        setItemExitsError(null);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setItemExitsLoaded(false);
        setItemExitsError(panelErrMsg(err, DEV_MSG.CT_IE_LOAD_ERROR));
      });
    setSelectedFieldName("");
    setControlProps([]);
    setControlPropsInitial([]);
    setControlName("");
    setChoiceSummary(null);
    setControlPropsError(null);
    setNewPropName("");
    setNewPropValue("");
    setFieldRulesDirty(false);
    getContentTypeDetail(idOrName)
      .then((d) => {
        if (cancelled) return;
        const normalized = normalizeDetailLists(d);
        setDetail(normalized);
        setLabel(asContentTypeText(normalized.label));
        setDescription(asContentTypeText(normalized.description));
        setEnabled(normalized.enabled !== false);
        setFieldDrafts(toDrafts(normalized.fields));
        setWorkflows(
          withDefaultWorkflowFlags(normalized.allowedWorkflows, normalized.defaultWorkflow),
        );
        setTemplates(cloneNamedObjectRefs(normalized.allowedTemplates));
        setNewWfName("");
        setNewTplName("");
        const firstField =
          normalizeContentTypeFields(normalized.fields).find((f) => !!f.name)?.name || "";
        setSelectedFieldName(firstField);
        setControlProps([]);
        setControlPropsInitial([]);
        setControlName("");
        setChoiceSummary(null);
        setNewPropName("");
        setNewPropValue("");
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.CT_DETAIL_ERROR));
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName]);

  useEffect(() => {
    const field = selectedFieldName.trim();
    if (!field) {
      setControlProps([]);
      setControlPropsInitial([]);
      setControlName("");
      setChoiceSummary(null);
      setControlPropsLoading(false);
      return;
    }
    let cancelled = false;
    setControlPropsLoading(true);
    setControlPropsError(null);
    getFieldControlProperties(idOrName, field)
      .then((loaded) => {
        if (cancelled) return;
        const props = cloneControlProperties(loaded.properties);
        setControlProps(props);
        setControlPropsInitial(cloneControlProperties(props));
        setControlName(loaded.control || "");
        const choiceType = loaded.choices?.type?.trim();
        setChoiceSummary(choiceType ? choiceType : null);
        setNewPropName("");
        setNewPropValue("");
        setControlPropsError(null);
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setControlProps([]);
        setControlPropsInitial([]);
        setControlName("");
        setChoiceSummary(null);
        setControlPropsError(panelErrMsg(err, DEV_MSG.CT_CONTROL_PROPS_ERROR));
      })
      .finally(() => {
        if (!cancelled) {
          setControlPropsLoading(false);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName, selectedFieldName]);

  const initialDrafts = toDrafts(detail?.fields);
  const fieldsDirty =
    detail != null &&
    contentTypeFields(detail).some((f) => {
      if (!f.name) return false;
      const k = fieldKey(f);
      const d = fieldDrafts[k];
      const i = initialDrafts[k];
      if (!d || !i) return false;
      return d.searchable !== i.searchable || d.required !== i.required;
    });

  const initialWorkflows = withDefaultWorkflowFlags(
    detail?.allowedWorkflows,
    detail?.defaultWorkflow,
  );
  const initialTemplates = cloneNamedObjectRefs(detail?.allowedTemplates);
  const workflowsDirty = detail != null && !namedObjectRefsEqual(workflows, initialWorkflows);
  const templatesDirty = detail != null && !namedObjectRefsEqual(templates, initialTemplates);
  const itemExitsDirty =
    itemExitsLoaded && !contentTypeItemExitsEqual(itemExits, savedItemExits);
  const controlPropsDirty =
    selectedFieldName.trim().length > 0 &&
    !controlPropertiesEqual(controlProps, controlPropsInitial);

  const searchIndexingDirty = searchIndexing !== savedSearchIndexing;

  const dirty =
    detail != null &&
    (label !== (detail.label || "") ||
      description !== (detail.description || "") ||
      enabled !== (detail.enabled !== false) ||
      searchIndexingDirty ||
      fieldsDirty ||
      workflowsDirty ||
      templatesDirty ||
      itemExitsDirty ||
      controlPropsDirty ||
      fieldRulesDirty);

  const objectGuid = resolveContentTypeObjectGuid(detail, catalogGuid);
  const fieldRows = contentTypeFields(detail);
  const childSets = contentTypeChildSets(detail);
  const gapRows = contentTypeDesignGaps(detail);
  const canEdit = heldLock && !busy && detail != null;
  const canEditItemExits = canEdit && itemExitsLoaded;

  function toggleField(key: string, prop: "searchable" | "required") {
    setFieldDrafts((prev) => {
      const cur = prev[key];
      if (!cur) return prev;
      return { ...prev, [key]: { ...cur, [prop]: !cur[prop] } };
    });
    setNotice(null);
  }

  function removeWorkflow(index: number) {
    if (!heldLock) {
      return;
    }
    setWorkflows((prev) => {
      const next = prev.filter((_, i) => i !== index);
      if (next.length > 0 && !next.some((w) => w.isDefault)) {
        next[0] = { ...next[0], isDefault: true };
      }
      return next;
    });
    setNotice(null);
  }

  function addWorkflow() {
    if (!heldLock) {
      return;
    }
    const name = newWfName.trim();
    if (!name) return;
    if (workflows.some((w) => (w.name || "").toLowerCase() === name.toLowerCase())) {
      setNewWfName("");
      return;
    }
    setWorkflows((prev) => [
      ...prev,
      { name, label: name, isDefault: prev.length === 0 },
    ]);
    setNewWfName("");
    setNotice(null);
  }

  function setDefaultWorkflow(index: number) {
    if (!heldLock) {
      return;
    }
    setWorkflows((prev) => prev.map((w, i) => ({ ...w, isDefault: i === index })));
    setNotice(null);
  }

  function removeTemplate(index: number) {
    if (!heldLock) {
      return;
    }
    setTemplates((prev) => prev.filter((_, i) => i !== index));
    setNotice(null);
  }

  function addTemplate() {
    if (!heldLock) {
      return;
    }
    const raw = newTplName.trim();
    if (!raw) return;
    const looksLikeGuid = PERC_GUID_RE.test(raw);
    const exists = templates.some((t) => {
      if (looksLikeGuid) return t.guid?.stringValue === raw;
      return (t.name || "").toLowerCase() === raw.toLowerCase();
    });
    if (exists) {
      setNewTplName("");
      return;
    }
    setTemplates((prev) => [
      ...prev,
      looksLikeGuid
        ? { guid: { stringValue: raw }, name: raw, label: raw }
        : { name: raw, label: raw },
    ]);
    setNewTplName("");
    setNotice(null);
  }

  function setControlPropValue(index: number, value: string) {
    if (!heldLock) {
      return;
    }
    setControlProps((prev) => prev.map((p, i) => (i === index ? { ...p, value } : p)));
    setNotice(null);
  }

  function removeControlProp(index: number) {
    if (!heldLock) {
      return;
    }
    setControlProps((prev) => prev.filter((_, i) => i !== index));
    setNotice(null);
  }

  function addControlProp() {
    if (!heldLock) {
      return;
    }
    const name = newPropName.trim();
    if (!name) {
      return;
    }
    if (controlProps.some((p) => (p.name || "").toLowerCase() === name.toLowerCase())) {
      setNewPropName("");
      setNewPropValue("");
      return;
    }
    setControlProps((prev) => [...prev, { name, value: newPropValue }]);
    setNewPropName("");
    setNewPropValue("");
    setNotice(null);
  }

  async function handleLock() {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await lockContentType(idOrName);
      heldLockRef.current = true;
      setHeldLock(true);
      setNotice(DEV_MSG.CT_LOCKED);
    } catch (err: unknown) {
      heldLockRef.current = false;
      setHeldLock(false);
      setError(panelErrMsg(err, DEV_MSG.CT_LOCK_ERROR));
    } finally {
      setBusy(false);
    }
  }

  async function handleUnlock() {
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      await unlockContentType(idOrName);
      heldLockRef.current = false;
      setHeldLock(false);
      setControlProps(cloneControlProperties(controlPropsInitial));
      setNewPropName("");
      setNewPropValue("");
      setNotice(DEV_MSG.CT_UNLOCKED_NOTICE);
    } catch (err: unknown) {
      setError(panelErrMsg(err, DEV_MSG.CT_UNLOCK_ERROR));
    } finally {
      setBusy(false);
    }
  }

  async function handleBack() {
    if (heldLockRef.current) {
      try {
        await unlockContentType(idOrName);
      } catch {
        // Best-effort release so Back cannot trap the operator on a stale lock.
      }
      heldLockRef.current = false;
      setHeldLock(false);
    }
    onBack();
  }

  async function handleSave() {
    if (!heldLockRef.current) {
      setError(DEV_MSG.CT_LOCK_REQUIRED);
      return;
    }
    if (detail == null) {
      return;
    }
    const enabledDirty = enabled !== (detail.enabled !== false);
    const bulkNeeded =
      label !== (detail.label || "") ||
      description !== (detail.description || "") ||
      fieldsDirty;
    if (
      !enabledDirty &&
      !searchIndexingDirty &&
      !workflowsDirty &&
      !templatesDirty &&
      !bulkNeeded &&
      !itemExitsDirty &&
      !controlPropsDirty &&
      !fieldRulesDirty
    ) {
      return;
    }
    setBusy(true);
    setError(null);
    setNotice(null);
    try {
      const fieldPatches = Object.values(fieldDrafts)
        .filter((d) => {
          const initial = Object.values(initialDrafts).find((i) => i.name === d.name);
          return (
            !initial ||
            initial.searchable !== d.searchable ||
            initial.required !== d.required
          );
        })
        .map((d) => ({
          name: d.name,
          searchable: d.searchable,
          required: d.required,
        }));

      let saved: ContentTypeDetail | null = null;
      let workflowSaved: ContentTypeDetail | undefined;
      // Enabled PUT first so a failed enable/disable cannot leave bulk fields persisted
      // (CD-13 two-call save; no shared rollback).
      if (enabledDirty) {
        saved = normalizeDetailLists(await setContentTypeEnabled(idOrName, enabled));
      }
      if (searchIndexingDirty) {
        try {
          const si = await setContentTypeSearchIndexing(idOrName, searchIndexing);
          const on = si.searchIndexing !== false;
          setSearchIndexing(on);
          setSavedSearchIndexing(on);
          if (saved == null) {
            saved = normalizeDetailLists(detail);
          }
        } catch (siErr) {
          if (saved != null) {
            setDetail(saved);
            setEnabled(saved.enabled !== false);
          }
          throw siErr;
        }
      }
      if (workflowsDirty) {
        try {
          workflowSaved = await setContentTypeAllowedWorkflows(
            idOrName,
            buildAllowedWorkflowsReplaceBody(workflows),
          );
          saved = saved
            ? {
                ...normalizeDetailLists(workflowSaved),
                enabled: saved.enabled,
              }
            : normalizeDetailLists(workflowSaved);
        } catch (wfErr) {
          if (saved != null) {
            setDetail(saved);
            setEnabled(saved.enabled !== false);
          }
          throw wfErr;
        }
      }
      if (bulkNeeded) {
        const body: ContentTypeUpdateBody = {
          label,
          description,
          fields: fieldPatches,
        };
        try {
          const bulkSaved = normalizeDetailLists(await updateContentTypeDetail(idOrName, body));
          saved = {
            ...bulkSaved,
            enabled: saved?.enabled ?? bulkSaved.enabled,
            allowedWorkflows: workflowSaved?.allowedWorkflows ?? bulkSaved.allowedWorkflows,
            defaultWorkflow: workflowSaved?.defaultWorkflow ?? bulkSaved.defaultWorkflow,
          };
        } catch (bulkErr) {
          if (saved != null) {
            setDetail(saved);
            setEnabled(saved.enabled !== false);
            setWorkflows(
              withDefaultWorkflowFlags(saved.allowedWorkflows, saved.defaultWorkflow),
            );
          }
          throw bulkErr;
        }
      }
      if (templatesDirty) {
        try {
          await replaceContentTypeAllowedTemplates(
            idOrName,
            toNamedObjectRefPayload(templates),
          );
          const listed = await getContentTypeAllowedTemplates(idOrName);
          saved =
            saved != null
              ? { ...saved, allowedTemplates: listed }
              : { ...normalizeDetailLists(detail), allowedTemplates: listed };
        } catch (tplErr) {
          if (saved != null) {
            setDetail(saved);
            setEnabled(saved.enabled !== false);
            setWorkflows(
              withDefaultWorkflowFlags(saved.allowedWorkflows, saved.defaultWorkflow),
            );
          }
          throw tplErr;
        }
      }
      if (itemExitsDirty) {
        try {
          const includePipeExits =
            !itemExitListsEqual(itemExits.preExits, savedItemExits.preExits) ||
            !itemExitListsEqual(itemExits.postExits, savedItemExits.postExits);
          const includeMaxErrors =
            (itemExits.maxErrorsToStopValidation ?? null) !==
            (savedItemExits.maxErrorsToStopValidation ?? null);
          const exitsSaved = await replaceContentTypeItemExits(
            idOrName,
            itemExits,
            includePipeExits,
            includeMaxErrors,
          );
          const cloned = cloneContentTypeItemExits(exitsSaved);
          setItemExits(cloned);
          setSavedItemExits(cloneContentTypeItemExits(cloned));
          if (saved == null) {
            saved = normalizeDetailLists(detail);
          }
        } catch (ieErr) {
          if (saved != null) {
            setDetail(saved);
            setEnabled(saved.enabled !== false);
            setWorkflows(
              withDefaultWorkflowFlags(saved.allowedWorkflows, saved.defaultWorkflow),
            );
            if (saved.allowedTemplates) {
              setTemplates(cloneNamedObjectRefs(saved.allowedTemplates));
            }
          }
          throw ieErr;
        }
      }
      if (controlPropsDirty) {
        const field = selectedFieldName.trim();
        try {
          const replaced = await replaceFieldControlProperties(
            idOrName,
            field,
            toControlPropertyPayload(controlProps),
          );
          const listed = await getFieldControlProperties(idOrName, field);
          const nextProps = cloneControlProperties(
            listed.properties ?? replaced.properties,
          );
          setControlProps(nextProps);
          setControlPropsInitial(cloneControlProperties(nextProps));
          setControlName(listed.control || replaced.control || controlName);
          const choiceType = listed.choices?.type?.trim();
          setChoiceSummary(choiceType ? choiceType : null);
          if (saved == null) {
            saved = normalizeDetailLists(detail);
          }
        } catch (cpErr) {
          if (saved != null) {
            setDetail(saved);
            setEnabled(saved.enabled !== false);
            setWorkflows(
              withDefaultWorkflowFlags(saved.allowedWorkflows, saved.defaultWorkflow),
            );
            setTemplates(cloneNamedObjectRefs(saved.allowedTemplates));
          }
          throw cpErr;
        }
      }
      if (fieldRulesDirty) {
        try {
          await fieldRulesRef.current?.save();
          if (saved == null) {
            saved = normalizeDetailLists(detail);
          }
        } catch (frErr) {
          if (saved != null) {
            setDetail(saved);
            setEnabled(saved.enabled !== false);
            setWorkflows(
              withDefaultWorkflowFlags(saved.allowedWorkflows, saved.defaultWorkflow),
            );
            setTemplates(cloneNamedObjectRefs(saved.allowedTemplates));
          }
          throw frErr;
        }
      }
      if (saved == null && !itemExitsDirty && !controlPropsDirty && !fieldRulesDirty) {
        return;
      }
      if (saved == null) {
        saved = normalizeDetailLists(detail);
      }
      const normalized = normalizeDetailLists(saved);
      setDetail(normalized);
      setLabel(asContentTypeText(normalized.label));
      setDescription(asContentTypeText(normalized.description));
      setEnabled(normalized.enabled !== false);
      setFieldDrafts(toDrafts(normalized.fields));
      setWorkflows(
        withDefaultWorkflowFlags(normalized.allowedWorkflows, normalized.defaultWorkflow),
      );
      setTemplates(cloneNamedObjectRefs(normalized.allowedTemplates));
      setNotice(DEV_MSG.CT_SAVED);
    } catch (err: unknown) {
      if (isApiError(err) && err.status === 409) {
        heldLockRef.current = false;
        setHeldLock(false);
      }
      setError(panelErrMsg(err, DEV_MSG.CT_SAVE_ERROR));
    } finally {
      setBusy(false);
    }
  }

  return (
    <div data-testid="developer-ct-detail">
      <button
        type="button"
        onClick={() => void handleBack()}
        data-testid="developer-ct-back"
        aria-label={DEV_MSG.CT_BACK}
        style={{
          marginBottom: "12px",
          background: "transparent",
          border: `1px solid ${catalogColors.softBorder}`,
          borderRadius: "4px",
          padding: "6px 12px",
          cursor: "pointer",
        }}
      >
        ← {DEV_MSG.CT_BACK}
      </button>

      {error ? (
        <div role="alert" data-testid="developer-ct-detail-error" style={{ color: catalogColors.error }}>
          {error}
        </div>
      ) : null}
      {notice ? (
        <div data-testid="developer-ct-detail-notice" style={{ color: "#276749" }}>
          {notice}
        </div>
      ) : null}

      {/* Always mount lock/enabled/search-indexing/template chrome so Playwright
          and operators see it before GET detail finishes and without scrolling
          past the fields table (#3834 #3835 #3836 #4035). */}
      <div
        role="toolbar"
        aria-label={DEV_MSG.CT_LOCK_TOOLBAR}
        data-testid="developer-ct-lock-toolbar"
        style={{
          marginBottom: "16px",
          display: "flex",
          flexWrap: "wrap",
          gap: "8px",
          alignItems: "center",
          position: "sticky",
          top: 0,
          zIndex: 2,
          background: catalogColors.surface,
          padding: "8px 0",
        }}
      >
        <p style={{ margin: 0, width: "100%", color: catalogColors.muted, fontSize: "0.9rem" }}>
          {DEV_MSG.CT_LOCK_HINT}
        </p>
        <div
          role="status"
          aria-live="polite"
          data-testid="developer-ct-lock-status"
          style={{ marginRight: "8px", fontSize: "0.9rem" }}
        >
          {heldLock ? DEV_MSG.CT_LOCKED : DEV_MSG.CT_UNLOCKED}
        </div>
        <button
          type="button"
          data-testid="developer-ct-lock"
          aria-label={DEV_MSG.CT_LOCK}
          disabled={busy || heldLock || detail == null}
          onClick={() => void handleLock()}
          style={{
            padding: "8px 16px",
            background: heldLock ? catalogColors.disabled : catalogColors.accent,
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: busy || heldLock || detail == null ? "not-allowed" : "pointer",
          }}
        >
          {DEV_MSG.CT_LOCK}
        </button>
        <button
          type="button"
          data-testid="developer-ct-save"
          aria-label={DEV_MSG.CT_SAVE}
          disabled={busy || !heldLock || !dirty}
          onClick={() => void handleSave()}
          style={{
            padding: "8px 16px",
            background: heldLock && dirty ? catalogColors.accent : catalogColors.disabled,
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: busy || !heldLock || !dirty ? "not-allowed" : "pointer",
          }}
        >
          {DEV_MSG.CT_SAVE}
        </button>
        <button
          type="button"
          data-testid="developer-ct-unlock"
          aria-label={DEV_MSG.CT_UNLOCK}
          disabled={busy || !heldLock}
          onClick={() => void handleUnlock()}
          style={{
            padding: "8px 16px",
            background: "transparent",
            color: "inherit",
            border: `1px solid ${catalogColors.softBorder}`,
            borderRadius: "4px",
            cursor: busy || !heldLock ? "not-allowed" : "pointer",
          }}
        >
          {DEV_MSG.CT_UNLOCK}
        </button>
        <label style={{ display: "flex", alignItems: "center", gap: 8, marginLeft: "8px" }}>
          <input
            type="checkbox"
            data-testid="developer-ct-enabled"
            aria-label={DEV_MSG.CT_FORM_ENABLED}
            checked={enabled}
            onChange={() => {
              if (!canEdit) {
                return;
              }
              setEnabled((v) => !v);
            }}
            disabled={!canEdit}
          />
          {DEV_MSG.CT_FORM_ENABLED}
        </label>
        <label style={{ display: "flex", alignItems: "center", gap: 8 }}>
          <input
            type="checkbox"
            data-testid="developer-ct-search-indexing"
            aria-label={DEV_MSG.CT_FORM_SEARCH_INDEXING}
            checked={searchIndexing}
            onChange={() => {
              if (!canEdit) {
                return;
              }
              setSearchIndexing((v) => !v);
            }}
            disabled={!canEdit}
          />
          {DEV_MSG.CT_FORM_SEARCH_INDEXING}
        </label>
      </div>

      <div style={{ fontFamily: "monospace", color: catalogColors.muted, marginBottom: "12px" }}>
        <span data-testid="developer-ct-detail-name">
          {asContentTypeText(detail?.name) || idOrName}
        </span>
        {detail ? (
          <>
            {" · "}
            <span data-testid="developer-ct-detail-guid">{objectGuid || "—"}</span>
          </>
        ) : null}
      </div>

      <section style={{ marginBottom: "16px" }} data-testid="developer-ct-templates">
        <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_TEMPLATES}</h3>
        <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.CT_TEMPLATES_HINT}</p>
        {detail == null ? null : templates.length === 0 ? (
          <p style={{ color: catalogColors.empty }} data-testid="developer-ct-tpl-empty">
            {DEV_MSG.CT_NONE}
          </p>
        ) : (
          <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
            {templates.map((t, i) => (
              <li
                key={refKey(t, i)}
                data-testid={`developer-ct-tpl-row-${i}`}
                style={{
                  ...tableRow,
                  display: "flex",
                  alignItems: "center",
                  gap: 12,
                  padding: "6px 0",
                }}
              >
                <span>
                  {t.label || t.name}
                  {t.name ? (
                    <span
                      style={{
                        fontFamily: "monospace",
                        color: catalogColors.empty,
                        marginLeft: "8px",
                        fontSize: "0.85rem",
                      }}
                    >
                      {t.name}
                    </span>
                  ) : null}
                </span>
                <button
                  type="button"
                  data-testid={`developer-ct-tpl-remove-${i}`}
                  aria-label={`Remove template ${t.name || t.label}`}
                  disabled={!canEdit}
                  onClick={() => removeTemplate(i)}
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
            marginTop: "12px",
            display: "grid",
            gridTemplateColumns: "1fr auto",
            gap: "8px",
            alignItems: "end",
          }}
        >
          <div>
            <label htmlFor="ct-tpl-add" style={{ display: "block", marginBottom: 4 }}>
              {DEV_MSG.CT_TEMPLATES}
            </label>
            <input
              id="ct-tpl-add"
              type="text"
              autoComplete="off"
              data-testid="developer-ct-tpl-add-name"
              style={inputStyle}
              placeholder={DEV_MSG.CT_TPL_NAME_PLACEHOLDER}
              value={newTplName}
              onChange={(e) => {
                if (!canEdit) {
                  return;
                }
                setNewTplName(e.target.value);
              }}
              disabled={!canEdit}
              aria-disabled={canEdit ? undefined : true}
              readOnly={!canEdit}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  if (canEdit) {
                    addTemplate();
                  }
                }
              }}
            />
          </div>
          <button
            type="button"
            data-testid="developer-ct-tpl-add"
            disabled={!canEdit || !newTplName.trim()}
            onClick={addTemplate}
            style={{
              ...smallBtnStyle,
              padding: "8px 12px",
              cursor: !canEdit || !newTplName.trim() ? "not-allowed" : "pointer",
            }}
          >
            {DEV_MSG.CT_ASSOC_ADD}
          </button>
        </div>
      </section>

      <ContentTypeItemExitsSection
        value={itemExits}
        canEdit={canEditItemExits}
        loadError={itemExitsError}
        onChange={(next) => {
          if (!canEditItemExits) {
            return;
          }
          setItemExits(next);
          setNotice(null);
        }}
      />

      <section style={{ marginBottom: "16px" }} data-testid="developer-ct-control-props">
        <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_CONTROL_PROPS}</h3>
        <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
          {DEV_MSG.CT_CONTROL_PROPS_HINT}
        </p>
        <div style={{ marginBottom: "12px" }}>
          <label htmlFor="ct-cp-field" style={{ display: "block", marginBottom: 4 }}>
            {DEV_MSG.CT_CONTROL_PROPS_FIELD}
          </label>
          <select
            id="ct-cp-field"
            data-testid="developer-ct-cp-field"
            aria-label={DEV_MSG.CT_CONTROL_PROPS_FIELD}
            style={inputStyle}
            value={selectedFieldName}
            disabled={busy || detail == null || controlPropsDirty}
            onChange={(e) => {
              if (busy || detail == null || controlPropsDirty) {
                return;
              }
              setSelectedFieldName(e.target.value);
              setNotice(null);
            }}
          >
            {fieldRows.filter((f) => !!f.name).length === 0 ? (
              <option value="">{DEV_MSG.CT_CONTROL_PROPS_NO_FIELD}</option>
            ) : (
              fieldRows
                .filter((f) => !!f.name)
                .map((f) => (
                  <option key={fieldKey(f)} value={f.name}>
                    {f.label ? `${f.label} (${f.name})` : f.name}
                  </option>
                ))
            )}
          </select>
        </div>
        {controlName ? (
          <p
            data-testid="developer-ct-cp-control"
            style={{ color: catalogColors.muted, fontSize: "0.9rem", fontFamily: "monospace" }}
          >
            {DEV_MSG.CT_CONTROL_PROPS_CONTROL}: {controlName}
          </p>
        ) : null}
        {choiceSummary ? (
          <p
            data-testid="developer-ct-cp-choices"
            style={{ color: catalogColors.muted, fontSize: "0.9rem" }}
          >
            {DEV_MSG.CT_CONTROL_PROPS_CHOICES}: {choiceSummary}
          </p>
        ) : null}
        {controlPropsError ? (
          <p
            role="status"
            data-testid="developer-ct-cp-error"
            style={{ color: catalogColors.error }}
          >
            {controlPropsError}
          </p>
        ) : null}
        {controlPropsLoading ? (
          <p data-testid="developer-ct-cp-loading" style={{ color: catalogColors.muted }}>
            {DEV_MSG.CT_CONTROL_PROPS_LOADING}
          </p>
        ) : null}
        {!controlPropsLoading && selectedFieldName && controlProps.length === 0 ? (
          <p style={{ color: catalogColors.empty }} data-testid="developer-ct-cp-empty">
            {DEV_MSG.CT_CONTROL_PROPS_EMPTY}
          </p>
        ) : null}
        {!controlPropsLoading && controlProps.length > 0 ? (
          <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
            {controlProps.map((p, i) => (
              <li
                key={`${p.name || "prop"}-${i}`}
                data-testid={`developer-ct-cp-row-${i}`}
                style={{
                  ...tableRow,
                  display: "grid",
                  gridTemplateColumns: "1fr 1fr auto",
                  gap: 8,
                  alignItems: "center",
                  padding: "6px 0",
                }}
              >
                <span
                  data-testid={`developer-ct-cp-name-${i}`}
                  style={{ fontFamily: "monospace" }}
                >
                  {p.name}
                </span>
                <input
                  type="text"
                  data-testid={`developer-ct-cp-value-${i}`}
                  aria-label={`${DEV_MSG.CT_CONTROL_PROPS_VALUE} ${p.name || i}`}
                  style={inputStyle}
                  value={p.value || ""}
                  disabled={!canEdit}
                  readOnly={!canEdit}
                  onChange={(e) => setControlPropValue(i, e.target.value)}
                />
                <button
                  type="button"
                  data-testid={`developer-ct-cp-remove-${i}`}
                  aria-label={`${DEV_MSG.CT_ASSOC_REMOVE} ${p.name || i}`}
                  disabled={!canEdit}
                  onClick={() => removeControlProp(i)}
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
        ) : null}
        <div
          style={{
            marginTop: "12px",
            display: "grid",
            gridTemplateColumns: "1fr 1fr auto",
            gap: "8px",
            alignItems: "end",
          }}
        >
          <div>
            <label htmlFor="ct-cp-add-name" style={{ display: "block", marginBottom: 4 }}>
              {DEV_MSG.CT_CONTROL_PROPS_NAME}
            </label>
            <input
              id="ct-cp-add-name"
              type="text"
              autoComplete="off"
              data-testid="developer-ct-cp-add-name"
              style={inputStyle}
              placeholder={DEV_MSG.CT_CONTROL_PROPS_NAME_PLACEHOLDER}
              value={newPropName}
              disabled={!canEdit}
              readOnly={!canEdit}
              aria-disabled={canEdit ? undefined : true}
              onChange={(e) => {
                if (!canEdit) {
                  return;
                }
                setNewPropName(e.target.value);
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  if (canEdit) {
                    addControlProp();
                  }
                }
              }}
            />
          </div>
          <div>
            <label htmlFor="ct-cp-add-value" style={{ display: "block", marginBottom: 4 }}>
              {DEV_MSG.CT_CONTROL_PROPS_VALUE}
            </label>
            <input
              id="ct-cp-add-value"
              type="text"
              autoComplete="off"
              data-testid="developer-ct-cp-add-value"
              style={inputStyle}
              placeholder={DEV_MSG.CT_CONTROL_PROPS_VALUE_PLACEHOLDER}
              value={newPropValue}
              disabled={!canEdit}
              readOnly={!canEdit}
              aria-disabled={canEdit ? undefined : true}
              onChange={(e) => {
                if (!canEdit) {
                  return;
                }
                setNewPropValue(e.target.value);
              }}
              onKeyDown={(e) => {
                if (e.key === "Enter") {
                  e.preventDefault();
                  if (canEdit) {
                    addControlProp();
                  }
                }
              }}
            />
          </div>
          <button
            type="button"
            data-testid="developer-ct-cp-add"
            disabled={!canEdit || !newPropName.trim()}
            onClick={addControlProp}
            style={{
              ...smallBtnStyle,
              padding: "8px 12px",
              cursor: !canEdit || !newPropName.trim() ? "not-allowed" : "pointer",
            }}
          >
            {DEV_MSG.CT_ASSOC_ADD}
          </button>
        </div>
      </section>

      {!error && detail == null ? (
        <div data-testid="developer-ct-detail-loading">{DEV_MSG.CT_DETAIL_LOADING}</div>
      ) : null}

      {detail ? (
        <>
          <header style={{ marginBottom: "16px" }}>
            <h2 style={{ margin: "0 0 4px" }} data-testid="developer-ct-detail-title">
              {label || asContentTypeText(detail.name) || idOrName}
            </h2>
            <div style={{ marginTop: "12px" }}>
              <label htmlFor="ct-label" style={{ display: "block", marginBottom: 4 }}>
                {DEV_MSG.CT_FORM_LABEL}
              </label>
              <input
                id="ct-label"
                data-testid="developer-ct-label"
                style={inputStyle}
                value={label}
                onChange={(e) => setLabel(e.target.value)}
                disabled={!canEdit}
              />
            </div>
            <div style={{ marginTop: "12px" }}>
              <label htmlFor="ct-desc" style={{ display: "block", marginBottom: 4 }}>
                {DEV_MSG.CT_FORM_DESCRIPTION}
              </label>
              <input
                id="ct-desc"
                data-testid="developer-ct-description"
                style={inputStyle}
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                disabled={!canEdit}
              />
            </div>
            <dl
              style={{
                display: "grid",
                gridTemplateColumns: "auto 1fr",
                gap: "4px 16px",
                marginTop: "12px",
                fontSize: "0.9rem",
              }}
            >
              <dt>{DEV_MSG.CT_META_HIDDEN}</dt>
              <dd style={{ margin: 0 }}>
                {detail.hideFromMenu ? DEV_MSG.YES : DEV_MSG.NO}
              </dd>
              <dt>{DEV_MSG.CT_META_APP}</dt>
              <dd style={{ margin: 0, fontFamily: "monospace" }}>
                {detail.appName || "—"}
              </dd>
            </dl>
          </header>

          {childSets.length > 0 ? (
            <section style={{ marginBottom: "16px" }}>
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_CHILD_SETS}</h3>
              <ul data-testid="developer-ct-child-sets">
                {childSets.map((n) => (
                  <li key={n} style={{ fontFamily: "monospace" }}>
                    {n}
                  </li>
                ))}
              </ul>
            </section>
          ) : null}

          <section style={{ marginBottom: "16px" }} data-testid="developer-ct-workflows">
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_WORKFLOWS}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.CT_WORKFLOWS_HINT}</p>
            {workflows.length === 0 ? (
              <p style={{ color: catalogColors.empty }} data-testid="developer-ct-wf-empty">
                {DEV_MSG.CT_NONE}
              </p>
            ) : (
              <ul style={{ listStyle: "none", padding: 0, margin: 0 }}>
                {workflows.map((w, i) => (
                  <li
                    key={refKey(w, i)}
                    data-testid={`developer-ct-wf-row-${i}`}
                    style={{ ...tableRow, display: "flex",
                      alignItems: "center",
                      gap: 12,
                      padding: "6px 0"  }}
                  >
                    <label style={{ display: "flex", alignItems: "center", gap: 6 }}>
                      <input
                        type="radio"
                        name="ct-default-wf"
                        data-testid={`developer-ct-wf-default-${i}`}
                        checked={!!w.isDefault}
                        disabled={!canEdit}
                        onChange={() => setDefaultWorkflow(i)}
                        aria-label={`${DEV_MSG.CT_SET_DEFAULT} ${w.label || w.name}`}
                      />
                      <span style={{ fontSize: "0.85rem", color: catalogColors.muted }}>
                        {DEV_MSG.CT_SET_DEFAULT}
                      </span>
                    </label>
                    <span>
                      {w.label || w.name}
                      {w.name ? (
                        <span
                          style={{
                            fontFamily: "monospace",
                            color: catalogColors.empty,
                            marginLeft: "8px",
                            fontSize: "0.85rem",
                          }}
                        >
                          {w.name}
                        </span>
                      ) : null}
                    </span>
                    <button
                      type="button"
                      data-testid={`developer-ct-wf-remove-${i}`}
                      aria-label={`Remove workflow ${w.name || w.label}`}
                      disabled={!canEdit}
                      onClick={() => removeWorkflow(i)}
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
                marginTop: "12px",
                display: "grid",
                gridTemplateColumns: "1fr auto",
                gap: "8px",
                alignItems: "end",
              }}
            >
              <div>
                <label htmlFor="ct-wf-add" style={{ display: "block", marginBottom: 4 }}>
                  {DEV_MSG.CT_WORKFLOWS}
                </label>
                <input
                  id="ct-wf-add"
                  data-testid="developer-ct-wf-add-name"
                  style={inputStyle}
                  placeholder={DEV_MSG.CT_WF_NAME_PLACEHOLDER}
                  value={newWfName}
                  onChange={(e) => {
                    if (!canEdit) {
                      return;
                    }
                    setNewWfName(e.target.value);
                  }}
                  disabled={!canEdit}
                  aria-disabled={!canEdit}
                  onKeyDown={(e) => {
                    if (!canEdit) {
                      return;
                    }
                    if (e.key === "Enter") {
                      e.preventDefault();
                      addWorkflow();
                    }
                  }}
                />
              </div>
              <button
                type="button"
                data-testid="developer-ct-wf-add"
                disabled={!canEdit || !newWfName.trim()}
                onClick={addWorkflow}
                style={{
                  ...smallBtnStyle,
                  padding: "8px 12px",
                  cursor: !canEdit || !newWfName.trim() ? "not-allowed" : "pointer",
                }}
              >
                {DEV_MSG.CT_ASSOC_ADD}
              </button>
            </div>
          </section>

          <section style={{ marginBottom: "16px" }}>
            <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_FIELDS}</h3>
            <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.CT_FIELDS_HINT}</p>
            <div style={{ overflowX: "auto" }}>
              <table
                data-testid="developer-ct-fields-table"
                style={{
                  width: "100%",
                  borderCollapse: "collapse",
                  fontSize: "0.9rem",
                }}
              >
                <thead>
                  <tr style={tableHeaderRow}>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_FIELD}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_ORIGIN}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_DATATYPE}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_CONTROL}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_REQUIRED}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_READONLY}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_OCCURRENCE}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_RULES}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_SEARCH}</th>
                    <th style={{ padding: "8px" }}>{DEV_MSG.CT_COL_FIELDSET}</th>
                  </tr>
                </thead>
                <tbody>
                  {fieldRows.map((f) => {
                    const rules: string[] = [];
                    if (f.hasValidation) rules.push(DEV_MSG.CT_RULE_VALIDATION);
                    if (f.hasVisibilityRules) rules.push(DEV_MSG.CT_RULE_VISIBILITY);
                    if (f.hasInputTranslation) rules.push(DEV_MSG.CT_RULE_IN_XFORM);
                    if (f.hasOutputTranslation) rules.push(DEV_MSG.CT_RULE_OUT_XFORM);
                    const k = fieldKey(f);
                    const draft = fieldDrafts[k];
                    const isLocal = (f.fieldType || "").toLowerCase() === "local";
                    return (
                      <tr
                        key={k}
                        data-testid="developer-ct-field-row"
                        style={tableRow}
                      >
                        <td style={{ padding: "8px" }}>
                          <div>{f.label || f.name}</div>
                          <div
                            style={{
                              fontFamily: "monospace",
                              color: catalogColors.empty,
                              fontSize: "0.85rem",
                            }}
                          >
                            {f.name}
                          </div>
                        </td>
                        <td style={{ padding: "8px" }}>{f.fieldType || "—"}</td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {f.dataType || "—"}
                        </td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {f.control || "—"}
                        </td>
                        <td style={{ padding: "8px" }}>
                          {draft && isLocal ? (
                            <input
                              type="checkbox"
                              data-testid={`developer-ct-field-required-${f.name}`}
                              checked={draft.required}
                              disabled={!canEdit}
                              onChange={() => toggleField(k, "required")}
                              aria-label={`Required ${f.name}`}
                            />
                          ) : f.required ? (
                            DEV_MSG.YES
                          ) : (
                            DEV_MSG.NO
                          )}
                        </td>
                        <td style={{ padding: "8px" }}>
                          {f.readOnly ? DEV_MSG.YES : DEV_MSG.NO}
                        </td>
                        <td
                          style={{ padding: "8px", fontFamily: "monospace" }}
                          data-testid="developer-ct-field-occurrence"
                        >
                          {f.occurrence || "—"}
                        </td>
                        <td
                          style={{ padding: "8px", fontSize: "0.85rem", color: catalogColors.muted }}
                          data-testid="developer-ct-field-rules"
                        >
                          {rules.length > 0 ? rules.join(", ") : "—"}
                        </td>
                        <td style={{ padding: "8px" }}>
                          {draft ? (
                            <input
                              type="checkbox"
                              data-testid={`developer-ct-field-search-${f.name}`}
                              checked={draft.searchable}
                              disabled={!canEdit}
                              onChange={() => toggleField(k, "searchable")}
                              aria-label={`Searchable ${f.name}`}
                            />
                          ) : f.searchable ? (
                            DEV_MSG.YES
                          ) : (
                            DEV_MSG.NO
                          )}
                        </td>
                        <td style={{ padding: "8px", fontFamily: "monospace" }}>
                          {f.fieldSet || "—"}
                        </td>
                      </tr>
                    );
                  })}
                </tbody>
              </table>
            </div>
          </section>

          <ContentTypeFieldRulesSection
            ref={fieldRulesRef}
            idOrName={idOrName}
            fields={fieldRows}
            canEdit={canEdit}
            onDirtyChange={setFieldRulesDirty}
            onLockLost={() => {
              heldLockRef.current = false;
              setHeldLock(false);
            }}
          />

          {gapRows.length > 0 ? (
            <section data-testid="developer-ct-gaps">
              <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_GAPS}</h3>
              <ul style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>
                {gapRows.map((g, i) => (
                  <li key={designGapKey(g, i)} data-gap-code={designGapCode(g)}>
                    {formatDesignGap(g)}
                  </li>
                ))}
              </ul>
            </section>
          ) : null}
        </>
      ) : null}

      {/* Mount Object ACL from catalogGuid before GET detail finishes (#3810). */}
      <ObjectAclSection
        objectGuid={objectGuid}
        objectKind="content-type"
        testIdPrefix="developer-ct-acl"
      />
    </div>
  );
}
