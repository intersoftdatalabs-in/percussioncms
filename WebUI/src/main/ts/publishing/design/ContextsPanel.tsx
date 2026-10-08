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
  createContext,
  createScheme,
  deleteContext,
  deleteScheme,
  getScheme,
  listContexts,
  listSchemesForContext,
  updateContext,
  updateScheme,
  type ContextSummary,
  type LocationSchemeSummary,
  type SchemeParameter,
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
import { mapContextSaveError } from "../contextSaveErrors";
import {
  buildContextCopyBody,
  contextsAfterSuccessfulCopy,
  suggestedContextCopyName,
  validateContextCopyName,
} from "../contextCopy";
import {
  buildContextRenameBody,
  contextsAfterSuccessfulRename,
  validateContextRenameName,
} from "../contextRename";
import {
  buildContextDescriptionBody,
  contextsAfterSuccessfulDescription,
  validateContextDescription,
} from "../contextDescription";
import {
  contextsAfterSuccessfulDelete,
  mapContextDeleteError,
} from "../contextDelete";
import { mapLocationSchemeSaveError } from "../locationSchemeSaveErrors";
import {
  mapLocationSchemeDeleteError,
  schemesAfterSuccessfulDelete,
} from "../locationSchemeDelete";
import {
  buildLocationSchemeCopyBody,
  schemesAfterSuccessfulCopy,
  suggestedLocationSchemeCopyName,
  validateLocationSchemeCopyName,
} from "../locationSchemeCopy";
import {
  buildLocationSchemeRenameBody,
  schemesAfterSuccessfulRename,
  validateLocationSchemeRenameName,
} from "../locationSchemeRename";
import {
  buildLocationSchemeGeneratorBody,
  schemesAfterSuccessfulGenerator,
  validateLocationSchemeGenerator,
} from "../locationSchemeGenerator";
import {
  buildLocationSchemeDescriptionBody,
  schemesAfterSuccessfulDescription,
  validateLocationSchemeDescription,
} from "../locationSchemeDescription";
import {
  buildLocationSchemeContentTypeBody,
  schemesAfterSuccessfulContentType,
  validateLocationSchemeContentType,
} from "../locationSchemeContentType";
import {
  buildLocationSchemeTemplateBody,
  schemesAfterSuccessfulTemplate,
  validateLocationSchemeTemplate,
} from "../locationSchemeTemplate";
import {
  buildLocationSchemeAddParameterBody,
  schemesAfterSuccessfulAdd,
  validateLocationSchemeAddParameter,
} from "../locationSchemeAddParameter";
import {
  buildLocationSchemeRemoveParameterBody,
  schemesAfterSuccessfulRemove,
  validateLocationSchemeRemoveParameter,
} from "../locationSchemeRemoveParameter";
import {
  buildLocationSchemeParameterValueBody,
  schemesAfterSuccessfulParameterValue,
  validateLocationSchemeParameterValue,
} from "../locationSchemeParameterValue";
import {
  buildLocationSchemeParameterTypeBody,
  locationSchemeParameterTypeChoices,
  schemesAfterSuccessfulParameterType,
  validateLocationSchemeParameterType,
} from "../locationSchemeParameterType";
import {
  buildLocationSchemeParameterSequenceBody,
  schemesAfterSuccessfulParameterSequence,
  validateLocationSchemeParameterSequence,
} from "../locationSchemeParameterSequence";
import {
  buildLocationSchemeParameterNameBody,
  schemesAfterSuccessfulParameterName,
  validateLocationSchemeParameterName,
} from "../locationSchemeParameterName";
import { useDirtyForm } from "../dirtyFormContext";
import { normalizeSchemeType } from "./designLegacyTypes";
import { SiteRootBrowser } from "./SiteRootBrowser";

type Mode =
  | { kind: "list" }
  | { kind: "context-edit"; context: ContextSummary | null }
  | { kind: "context-copy"; source: ContextSummary }
  | { kind: "context-rename"; source: ContextSummary }
  | { kind: "context-describe"; source: ContextSummary }
  | { kind: "scheme-edit"; scheme: LocationSchemeSummary | null; contextId: string }
  | { kind: "scheme-copy"; source: LocationSchemeSummary; contextId: string }
  | { kind: "scheme-rename"; source: LocationSchemeSummary; contextId: string }
  | { kind: "scheme-generator"; source: LocationSchemeSummary; contextId: string }
  | { kind: "scheme-describe"; source: LocationSchemeSummary; contextId: string }
  | { kind: "scheme-content-type"; source: LocationSchemeSummary; contextId: string }
  | { kind: "scheme-template"; source: LocationSchemeSummary; contextId: string }
  | {
      kind: "scheme-add-parameter";
      source: LocationSchemeSummary;
      contextId: string;
    }
  | {
      kind: "scheme-remove-parameter";
      source: LocationSchemeSummary;
      contextId: string;
      parameter: SchemeParameter;
    }
  | {
      kind: "scheme-parameter-value";
      source: LocationSchemeSummary;
      contextId: string;
      parameter: SchemeParameter;
    }
  | {
      kind: "scheme-parameter-type";
      source: LocationSchemeSummary;
      contextId: string;
      parameter: SchemeParameter;
    }
  | {
      kind: "scheme-parameter-sequence";
      source: LocationSchemeSummary;
      contextId: string;
      parameter: SchemeParameter;
    }
  | {
      kind: "scheme-parameter-name";
      source: LocationSchemeSummary;
      contextId: string;
      parameter: SchemeParameter;
    };

/**
 * Contexts CRUD + location schemes with parameters and path browser.
 */
export function ContextsPanel(): React.ReactElement {
  const [contexts, setContexts] = useState<ContextSummary[]>([]);
  const [selected, setSelected] = useState("");
  const [schemes, setSchemes] = useState<LocationSchemeSummary[]>([]);
  const [error, setError] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);
  const [mode, setMode] = useState<Mode>({ kind: "list" });
  const [showBrowser, setShowBrowser] = useState(false);

  // Context form
  const [ctxName, setCtxName] = useState("");
  const [ctxDesc, setCtxDesc] = useState("");

  // Scheme form
  const [schName, setSchName] = useState("");
  const [schGen, setSchGen] = useState("");
  const [schDesc, setSchDesc] = useState("");
  const [schCtype, setSchCtype] = useState("");
  const [schTemplate, setSchTemplate] = useState("");
  const [params, setParams] = useState<SchemeParameter[]>([]);
  const [paramName, setParamName] = useState("");
  const [paramType, setParamType] = useState("String");
  const [paramValue, setParamValue] = useState("");
  const [saving, setSaving] = useState(false);
  const [copyName, setCopyName] = useState("");
  const [renameName, setRenameName] = useState("");
  const [schemeGenerator, setSchemeGenerator] = useState("");
  const [schemeDescription, setSchemeDescription] = useState("");
  const [schemeContentType, setSchemeContentType] = useState("");
  const [schemeTemplate, setSchemeTemplate] = useState("");
  const [paramAddName, setParamAddName] = useState("");
  const [paramAddType, setParamAddType] = useState("String");
  const [paramAddValue, setParamAddValue] = useState("");
  const [paramValueDraft, setParamValueDraft] = useState("");
  const [paramTypeDraft, setParamTypeDraft] = useState("");
  const [paramSequenceDraft, setParamSequenceDraft] = useState("");
  const [paramNameDraft, setParamNameDraft] = useState("");
  const [describeText, setDescribeText] = useState("");
  const { setDirty, confirmIfDirty } = useDirtyForm();

  function reloadContexts(): void {
    setLoading(true);
    listContexts()
      .then((list) => {
        setContexts(list);
        if (list.length > 0 && !selected) {
          setSelected(String(list[0].contextId ?? ""));
        }
      })
      .catch(() => setError(message(MSG.PUBLISH_ERROR)))
      .finally(() => setLoading(false));
  }

  useEffect(() => {
    reloadContexts();
  }, []);

  useEffect(() => {
    if (!selected) {
      setSchemes([]);
      return;
    }
    listSchemesForContext(selected)
      .then(setSchemes)
      .catch(() => setSchemes([]));
  }, [selected]);

  function openContextEdit(c: ContextSummary | null): void {
    setCtxName(c?.name ?? "");
    setCtxDesc(c?.description ?? "");
    setError(null);
    setDirty(false);
    setMode({ kind: "context-edit", context: c });
  }

  function closeContextEditor(): void {
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setMode({ kind: "list" });
  }

  function openContextCopy(): void {
    const source = contexts.find((row) => String(row.contextId ?? "") === selected);
    if (!source?.contextId) {
      return;
    }
    setCopyName(suggestedContextCopyName(source.name));
    setError(null);
    setDirty(false);
    setMode({ kind: "context-copy", source });
  }

  function closeContextCopy(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setMode({ kind: "list" });
  }

  function openContextRename(): void {
    const source = contexts.find((row) => String(row.contextId ?? "") === selected);
    if (!source?.contextId) {
      return;
    }
    setRenameName(source.name ?? "");
    setError(null);
    setDirty(false);
    setMode({ kind: "context-rename", source });
  }

  function closeContextRename(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setMode({ kind: "list" });
  }

  async function renameContext(): Promise<void> {
    if (mode.kind !== "context-rename" || !mode.source.contextId || saving) {
      return;
    }
    const validated = validateContextRenameName(renameName);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    const id = String(mode.source.contextId);
    setError(null);
    setSaving(true);
    const previous = contexts;
    try {
      await updateContext(id, buildContextRenameBody(validated.name));
      setDirty(false);
      setMode({ kind: "list" });
      let refreshed: ContextSummary[] | null = null;
      try {
        refreshed = await listContexts();
      } catch {
        refreshed = null;
      }
      const next = contextsAfterSuccessfulRename(refreshed, id, validated.name, previous);
      setContexts(next);
      setSelected((current) => {
        if (next.some((row) => String(row.contextId ?? "") === current)) {
          return current;
        }
        return next.some((row) => String(row.contextId ?? "") === id) ? id : current;
      });
    } catch (e) {
      setError(mapContextSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  function openContextDescribe(): void {
    const source = contexts.find((row) => String(row.contextId ?? "") === selected);
    if (!source?.contextId) {
      return;
    }
    setDescribeText(source.description ?? "");
    setError(null);
    setDirty(false);
    setMode({ kind: "context-describe", source });
  }

  function closeContextDescribe(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setMode({ kind: "list" });
  }

  async function saveContextDescription(): Promise<void> {
    if (mode.kind !== "context-describe" || !mode.source.contextId || saving) {
      return;
    }
    const validated = validateContextDescription(describeText);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    const id = String(mode.source.contextId);
    setError(null);
    setSaving(true);
    const previous = contexts;
    try {
      await updateContext(id, buildContextDescriptionBody(validated.description));
      setDirty(false);
      setMode({ kind: "list" });
      let refreshed: ContextSummary[] | null = null;
      try {
        refreshed = await listContexts();
      } catch {
        refreshed = null;
      }
      const next = contextsAfterSuccessfulDescription(
        refreshed,
        id,
        validated.description,
        previous,
      );
      setContexts(next);
      setSelected((current) => {
        if (next.some((row) => String(row.contextId ?? "") === current)) {
          return current;
        }
        return next.some((row) => String(row.contextId ?? "") === id) ? id : current;
      });
    } catch (e) {
      setError(mapContextSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function copyContext(): Promise<void> {
    if (mode.kind !== "context-copy" || !mode.source.contextId || saving) {
      return;
    }
    const validated = validateContextCopyName(copyName);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setError(null);
    setSaving(true);
    const previous = contexts;
    const sourceId = String(mode.source.contextId);
    try {
      const created = await createContext(
        buildContextCopyBody(mode.source, validated.name),
      );
      let refreshed: ContextSummary[] | null = null;
      try {
        refreshed = await listContexts();
      } catch {
        refreshed = null;
      }
      const next = contextsAfterSuccessfulCopy(refreshed, created, previous);
      setContexts(next);
      setSelected((current) => {
        if (next.some((row) => String(row.contextId ?? "") === current)) {
          return current;
        }
        return next.some((row) => String(row.contextId ?? "") === sourceId)
          ? sourceId
          : current;
      });
      setDirty(false);
      setMode({ kind: "list" });
    } catch (e) {
      setError(mapContextSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function openSchemeEdit(
    s: LocationSchemeSummary | null,
    contextId: string,
  ): Promise<void> {
    if (s?.schemeId) {
      try {
        const full = await getScheme(s.schemeId);
        setSchName(full.name ?? "");
        setSchGen(full.generator ?? "");
        setSchDesc(full.description ?? "");
        setSchCtype(full.contentTypeId != null ? String(full.contentTypeId) : "");
        setSchTemplate(full.templateId != null ? String(full.templateId) : "");
        setParams(full.parameters ?? []);
        setDirty(false);
        setMode({ kind: "scheme-edit", scheme: full, contextId });
        return;
      } catch {
        /* fall through */
      }
    }
    setSchName(s?.name ?? "");
    setSchGen(s?.generator ?? "");
    setSchDesc(s?.description ?? "");
    setSchCtype("");
    setSchTemplate("");
    setParams([]);
    setDirty(false);
    setMode({ kind: "scheme-edit", scheme: s, contextId });
  }

  function closeSchemeEditor(): void {
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setMode({ kind: "list" });
  }

  function openSchemeCopy(source: LocationSchemeSummary): void {
    if (!source.schemeId || !selected) {
      return;
    }
    setCopyName(suggestedLocationSchemeCopyName(source.name));
    setError(null);
    setDirty(false);
    setMode({ kind: "scheme-copy", source, contextId: selected });
  }

  function closeSchemeCopy(): void {
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setMode({ kind: "list" });
  }

  async function openSchemeRename(source: LocationSchemeSummary): Promise<void> {
    if (!source.schemeId || !selected || saving) {
      return;
    }
    setError(null);
    setDirty(false);
    const contextId = selected;
    let full = source;
    try {
      full = await getScheme(source.schemeId);
    } catch {
      full = source;
    }
    setRenameName(full.name ?? source.name ?? "");
    setMode({ kind: "scheme-rename", source: full, contextId });
  }

  function closeSchemeRename(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setMode({ kind: "list" });
  }

  async function renameScheme(): Promise<void> {
    if (mode.kind !== "scheme-rename" || !mode.source.schemeId || saving) {
      return;
    }
    const validated = validateLocationSchemeRenameName(renameName);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    const id = String(mode.source.schemeId);
    const contextId = mode.contextId;
    setError(null);
    setSaving(true);
    const previous = schemes;
    try {
      await updateScheme(id, buildLocationSchemeRenameBody(validated.name));
      setDirty(false);
      setMode({ kind: "list" });
      let refreshed: LocationSchemeSummary[] | null = null;
      try {
        refreshed = await listSchemesForContext(contextId);
      } catch {
        refreshed = null;
      }
      setSchemes(schemesAfterSuccessfulRename(refreshed, id, validated.name, previous));
    } catch (e) {
      setError(mapLocationSchemeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function openSchemeGenerator(source: LocationSchemeSummary): Promise<void> {
    if (!source.schemeId || !selected || saving) {
      return;
    }
    setError(null);
    setDirty(false);
    const contextId = selected;
    let full = source;
    try {
      full = await getScheme(source.schemeId);
    } catch {
      full = source;
    }
    setSchemeGenerator(full.generator ?? source.generator ?? "");
    setMode({ kind: "scheme-generator", source: full, contextId });
  }

  function closeSchemeGenerator(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setMode({ kind: "list" });
  }

  async function saveSchemeGenerator(): Promise<void> {
    if (mode.kind !== "scheme-generator" || !mode.source.schemeId || saving) {
      return;
    }
    const validated = validateLocationSchemeGenerator(schemeGenerator);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    const id = String(mode.source.schemeId);
    const contextId = mode.contextId;
    setError(null);
    setSaving(true);
    const previous = schemes;
    try {
      await updateScheme(id, buildLocationSchemeGeneratorBody(validated.generator));
      setDirty(false);
      setMode({ kind: "list" });
      let refreshed: LocationSchemeSummary[] | null = null;
      try {
        refreshed = await listSchemesForContext(contextId);
      } catch {
        refreshed = null;
      }
      setSchemes(
        schemesAfterSuccessfulGenerator(refreshed, id, validated.generator, previous),
      );
    } catch (e) {
      setError(mapLocationSchemeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function openSchemeDescribe(source: LocationSchemeSummary): Promise<void> {
    if (!source.schemeId || !selected || saving) {
      return;
    }
    setError(null);
    setDirty(false);
    const contextId = selected;
    let full = source;
    try {
      full = await getScheme(source.schemeId);
    } catch {
      full = source;
    }
    setSchemeDescription(full.description ?? source.description ?? "");
    setMode({ kind: "scheme-describe", source: full, contextId });
  }

  function closeSchemeDescribe(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setMode({ kind: "list" });
  }

  async function saveSchemeDescription(): Promise<void> {
    if (mode.kind !== "scheme-describe" || !mode.source.schemeId || saving) {
      return;
    }
    const validated = validateLocationSchemeDescription(schemeDescription);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    const id = String(mode.source.schemeId);
    const contextId = mode.contextId;
    setError(null);
    setSaving(true);
    const previous = schemes;
    try {
      await updateScheme(id, buildLocationSchemeDescriptionBody(validated.description));
      setDirty(false);
      setMode({ kind: "list" });
      let refreshed: LocationSchemeSummary[] | null = null;
      try {
        refreshed = await listSchemesForContext(contextId);
      } catch {
        refreshed = null;
      }
      setSchemes(
        schemesAfterSuccessfulDescription(refreshed, id, validated.description, previous),
      );
    } catch (e) {
      setError(mapLocationSchemeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function openSchemeContentType(source: LocationSchemeSummary): Promise<void> {
    if (!source.schemeId || !selected || saving) {
      return;
    }
    setError(null);
    setDirty(false);
    const contextId = selected;
    let full = source;
    try {
      full = await getScheme(source.schemeId);
    } catch {
      full = source;
    }
    const stored = full.contentTypeId ?? source.contentTypeId;
    setSchemeContentType(stored != null ? String(stored) : "");
    setMode({ kind: "scheme-content-type", source: full, contextId });
  }

  function closeSchemeContentType(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setMode({ kind: "list" });
  }

  async function saveSchemeContentType(): Promise<void> {
    if (mode.kind !== "scheme-content-type" || !mode.source.schemeId || saving) {
      return;
    }
    const validated = validateLocationSchemeContentType(schemeContentType);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    const id = String(mode.source.schemeId);
    const contextId = mode.contextId;
    setError(null);
    setSaving(true);
    const previous = schemes;
    try {
      await updateScheme(id, buildLocationSchemeContentTypeBody(validated.contentTypeId));
      setDirty(false);
      setMode({ kind: "list" });
      let refreshed: LocationSchemeSummary[] | null = null;
      try {
        refreshed = await listSchemesForContext(contextId);
      } catch {
        refreshed = null;
      }
      setSchemes(
        schemesAfterSuccessfulContentType(
          refreshed,
          id,
          validated.contentTypeId,
          previous,
        ),
      );
    } catch (e) {
      setError(mapLocationSchemeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function openSchemeTemplate(source: LocationSchemeSummary): Promise<void> {
    if (!source.schemeId || !selected || saving) {
      return;
    }
    setError(null);
    setDirty(false);
    const contextId = selected;
    let full = source;
    try {
      full = await getScheme(source.schemeId);
    } catch {
      full = source;
    }
    const stored = full.templateId ?? source.templateId;
    setSchemeTemplate(stored != null ? String(stored) : "");
    setMode({ kind: "scheme-template", source: full, contextId });
  }

  function closeSchemeTemplate(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setMode({ kind: "list" });
  }

  async function saveSchemeTemplate(): Promise<void> {
    if (mode.kind !== "scheme-template" || !mode.source.schemeId || saving) {
      return;
    }
    const validated = validateLocationSchemeTemplate(schemeTemplate);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    const id = String(mode.source.schemeId);
    const contextId = mode.contextId;
    setError(null);
    setSaving(true);
    const previous = schemes;
    try {
      await updateScheme(id, buildLocationSchemeTemplateBody(validated.templateId));
      setDirty(false);
      setMode({ kind: "list" });
      let refreshed: LocationSchemeSummary[] | null = null;
      try {
        refreshed = await listSchemesForContext(contextId);
      } catch {
        refreshed = null;
      }
      setSchemes(
        schemesAfterSuccessfulTemplate(refreshed, id, validated.templateId, previous),
      );
    } catch (e) {
      setError(mapLocationSchemeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function openSchemeAddParameter(
    source: LocationSchemeSummary,
  ): Promise<void> {
    if (!source.schemeId || !selected || saving) {
      return;
    }
    setError(null);
    setDirty(false);
    const contextId = selected;
    let full = source;
    try {
      full = await getScheme(source.schemeId);
    } catch {
      full = source;
    }
    setParamAddName("");
    setParamAddType("String");
    setParamAddValue("");
    setMode({ kind: "scheme-add-parameter", source: full, contextId });
  }

  function closeSchemeAddParameter(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setMode({ kind: "list" });
  }

  async function saveSchemeAddParameter(): Promise<void> {
    if (mode.kind !== "scheme-add-parameter" || !mode.source.schemeId || saving) {
      return;
    }
    const validated = validateLocationSchemeAddParameter(
      paramAddName,
      paramAddType,
      paramAddValue,
      mode.source.parameters,
    );
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    const id = String(mode.source.schemeId);
    const contextId = mode.contextId;
    const added = validated.parameter;
    setError(null);
    setSaving(true);
    const previous = schemes;
    try {
      await updateScheme(id, buildLocationSchemeAddParameterBody(added));
      setDirty(false);
      setMode({ kind: "list" });
      let refreshed: LocationSchemeSummary[] | null = null;
      try {
        refreshed = await listSchemesForContext(contextId);
      } catch {
        refreshed = null;
      }
      setSchemes(schemesAfterSuccessfulAdd(refreshed, id, added, previous));
    } catch (e) {
      setError(mapLocationSchemeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function openSchemeRemoveParameter(
    source: LocationSchemeSummary,
    parameter: SchemeParameter,
  ): Promise<void> {
    if (!source.schemeId || !selected || saving || !(parameter.name ?? "").trim()) {
      return;
    }
    setError(null);
    setDirty(false);
    const contextId = selected;
    let full = source;
    try {
      full = await getScheme(source.schemeId);
    } catch {
      full = source;
    }
    const name = (parameter.name ?? "").trim();
    const loaded = (full.parameters ?? source.parameters ?? []).find(
      (row) => (row.name ?? "").trim() === name,
    );
    setMode({
      kind: "scheme-remove-parameter",
      source: full,
      contextId,
      parameter: loaded ?? parameter,
    });
  }

  function closeSchemeRemoveParameter(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setMode({ kind: "list" });
  }

  async function saveSchemeRemoveParameter(): Promise<void> {
    if (mode.kind !== "scheme-remove-parameter" || !mode.source.schemeId || saving) {
      return;
    }
    const validated = validateLocationSchemeRemoveParameter(
      mode.parameter.name ?? "",
      mode.source.parameters,
    );
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    const id = String(mode.source.schemeId);
    const contextId = mode.contextId;
    const removed = validated.parameter;
    setError(null);
    setSaving(true);
    const previous = schemes;
    try {
      await updateScheme(id, buildLocationSchemeRemoveParameterBody(removed));
      setDirty(false);
      setMode({ kind: "list" });
      let refreshed: LocationSchemeSummary[] | null = null;
      try {
        refreshed = await listSchemesForContext(contextId);
      } catch {
        refreshed = null;
      }
      setSchemes(schemesAfterSuccessfulRemove(refreshed, id, removed.name, previous));
    } catch (e) {
      setError(mapLocationSchemeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function openSchemeParameterValue(
    source: LocationSchemeSummary,
    parameter: SchemeParameter,
  ): Promise<void> {
    if (!source.schemeId || !selected || saving || !(parameter.name ?? "").trim()) {
      return;
    }
    setError(null);
    setDirty(false);
    const contextId = selected;
    let full = source;
    try {
      full = await getScheme(source.schemeId);
    } catch {
      full = source;
    }
    const name = (parameter.name ?? "").trim();
    const loaded = (full.parameters ?? source.parameters ?? []).find(
      (row) => (row.name ?? "").trim() === name,
    );
    const shown = loaded ?? parameter;
    setParamValueDraft(shown.value ?? "");
    setMode({
      kind: "scheme-parameter-value",
      source: full,
      contextId,
      parameter: shown,
    });
  }

  function closeSchemeParameterValue(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setMode({ kind: "list" });
  }

  async function saveSchemeParameterValue(): Promise<void> {
    if (mode.kind !== "scheme-parameter-value" || !mode.source.schemeId || saving) {
      return;
    }
    const validated = validateLocationSchemeParameterValue(
      mode.parameter.name ?? "",
      paramValueDraft,
      mode.source.parameters,
    );
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    const id = String(mode.source.schemeId);
    const contextId = mode.contextId;
    const updated = validated.parameter;
    setError(null);
    setSaving(true);
    const previous = schemes;
    try {
      await updateScheme(id, buildLocationSchemeParameterValueBody(updated));
      setDirty(false);
      setMode({ kind: "list" });
      let refreshed: LocationSchemeSummary[] | null = null;
      try {
        refreshed = await listSchemesForContext(contextId);
      } catch {
        refreshed = null;
      }
      setSchemes(schemesAfterSuccessfulParameterValue(refreshed, id, updated, previous));
    } catch (e) {
      setError(mapLocationSchemeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function openSchemeParameterType(
    source: LocationSchemeSummary,
    parameter: SchemeParameter,
  ): Promise<void> {
    if (!source.schemeId || !selected || saving || !(parameter.name ?? "").trim()) {
      return;
    }
    setError(null);
    setDirty(false);
    const contextId = selected;
    let full = source;
    try {
      full = await getScheme(source.schemeId);
    } catch {
      full = source;
    }
    const name = (parameter.name ?? "").trim();
    const loaded = (full.parameters ?? source.parameters ?? []).find(
      (row) => (row.name ?? "").trim() === name,
    );
    const shown = loaded ?? parameter;
    setParamTypeDraft((shown.type ?? "").trim());
    setMode({
      kind: "scheme-parameter-type",
      source: full,
      contextId,
      parameter: shown,
    });
  }

  function closeSchemeParameterType(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setMode({ kind: "list" });
  }

  async function saveSchemeParameterType(): Promise<void> {
    if (mode.kind !== "scheme-parameter-type" || !mode.source.schemeId || saving) {
      return;
    }
    const validated = validateLocationSchemeParameterType(
      mode.parameter.name ?? "",
      paramTypeDraft,
      mode.source.parameters,
    );
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    const id = String(mode.source.schemeId);
    const contextId = mode.contextId;
    const updated = validated.parameter;
    setError(null);
    setSaving(true);
    const previous = schemes;
    try {
      await updateScheme(id, buildLocationSchemeParameterTypeBody(updated));
      setDirty(false);
      setMode({ kind: "list" });
      let refreshed: LocationSchemeSummary[] | null = null;
      try {
        refreshed = await listSchemesForContext(contextId);
      } catch {
        refreshed = null;
      }
      setSchemes(schemesAfterSuccessfulParameterType(refreshed, id, updated, previous));
    } catch (e) {
      setError(mapLocationSchemeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function openSchemeParameterSequence(
    source: LocationSchemeSummary,
    parameter: SchemeParameter,
  ): Promise<void> {
    if (!source.schemeId || !selected || saving || !(parameter.name ?? "").trim()) {
      return;
    }
    setError(null);
    setDirty(false);
    const contextId = selected;
    let full = source;
    try {
      full = await getScheme(source.schemeId);
    } catch {
      full = source;
    }
    const name = (parameter.name ?? "").trim();
    const loaded = (full.parameters ?? source.parameters ?? []).find(
      (row) => (row.name ?? "").trim() === name,
    );
    const shown = loaded ?? parameter;
    setParamSequenceDraft(shown.sequence != null ? String(shown.sequence) : "");
    setMode({
      kind: "scheme-parameter-sequence",
      source: full,
      contextId,
      parameter: shown,
    });
  }

  function closeSchemeParameterSequence(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setMode({ kind: "list" });
  }

  async function saveSchemeParameterSequence(): Promise<void> {
    if (mode.kind !== "scheme-parameter-sequence" || !mode.source.schemeId || saving) {
      return;
    }
    const validated = validateLocationSchemeParameterSequence(
      mode.parameter.name ?? "",
      paramSequenceDraft,
      mode.source.parameters,
    );
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    const id = String(mode.source.schemeId);
    const contextId = mode.contextId;
    const updated = validated.parameter;
    setError(null);
    setSaving(true);
    const previous = schemes;
    try {
      await updateScheme(id, buildLocationSchemeParameterSequenceBody(updated));
      setDirty(false);
      setMode({ kind: "list" });
      let refreshed: LocationSchemeSummary[] | null = null;
      try {
        refreshed = await listSchemesForContext(contextId);
      } catch {
        refreshed = null;
      }
      setSchemes(schemesAfterSuccessfulParameterSequence(refreshed, id, updated, previous));
    } catch (e) {
      setError(mapLocationSchemeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function openSchemeParameterName(
    source: LocationSchemeSummary,
    parameter: SchemeParameter,
  ): Promise<void> {
    if (!source.schemeId || !selected || saving || !(parameter.name ?? "").trim()) {
      return;
    }
    setError(null);
    setDirty(false);
    const contextId = selected;
    let full = source;
    try {
      full = await getScheme(source.schemeId);
    } catch {
      full = source;
    }
    const name = (parameter.name ?? "").trim();
    const loaded = (full.parameters ?? source.parameters ?? []).find(
      (row) => (row.name ?? "").trim() === name,
    );
    const shown = loaded ?? parameter;
    setParamNameDraft((shown.name ?? "").trim());
    setMode({
      kind: "scheme-parameter-name",
      source: full,
      contextId,
      parameter: shown,
    });
  }

  function closeSchemeParameterName(): void {
    if (saving) {
      return;
    }
    if (!confirmIfDirty()) {
      return;
    }
    setDirty(false);
    setError(null);
    setMode({ kind: "list" });
  }

  async function saveSchemeParameterName(): Promise<void> {
    if (mode.kind !== "scheme-parameter-name" || !mode.source.schemeId || saving) {
      return;
    }
    const validated = validateLocationSchemeParameterName(
      mode.parameter.name ?? "",
      paramNameDraft,
      mode.source.parameters,
    );
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    const id = String(mode.source.schemeId);
    const contextId = mode.contextId;
    const updated = validated.parameter;
    setError(null);
    setSaving(true);
    const previous = schemes;
    try {
      await updateScheme(id, buildLocationSchemeParameterNameBody(updated));
      setDirty(false);
      setMode({ kind: "list" });
      let refreshed: LocationSchemeSummary[] | null = null;
      try {
        refreshed = await listSchemesForContext(contextId);
      } catch {
        refreshed = null;
      }
      setSchemes(schemesAfterSuccessfulParameterName(refreshed, id, updated, previous));
    } catch (e) {
      setError(mapLocationSchemeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function copyScheme(): Promise<void> {
    if (mode.kind !== "scheme-copy" || !mode.source.schemeId) {
      return;
    }
    const validated = validateLocationSchemeCopyName(copyName);
    if (!validated.ok) {
      setError(validated.error);
      return;
    }
    setError(null);
    setSaving(true);
    const contextId = mode.contextId;
    try {
      const full = await getScheme(mode.source.schemeId);
      const created = await createScheme(
        contextId,
        buildLocationSchemeCopyBody(full, validated.name, contextId),
      );
      let refreshed: LocationSchemeSummary[] | null = null;
      try {
        refreshed = await listSchemesForContext(contextId);
      } catch {
        refreshed = null;
      }
      setSchemes(schemesAfterSuccessfulCopy(refreshed, created, schemes));
      setDirty(false);
      setMode({ kind: "list" });
    } catch (e) {
      setError(mapLocationSchemeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function saveContext(): Promise<void> {
    if (mode.kind !== "context-edit") {
      return;
    }
    if (!ctxName.trim()) {
      setError("Name is required");
      return;
    }
    setError(null);
    setSaving(true);
    try {
      if (mode.context?.contextId) {
        await updateContext(mode.context.contextId, {
          name: ctxName.trim(),
          description: ctxDesc,
        });
      } else {
        await createContext({ name: ctxName.trim(), description: ctxDesc });
      }
      setDirty(false);
      setMode({ kind: "list" });
      reloadContexts();
    } catch (e) {
      setError(mapContextSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function removeContext(id: string): Promise<void> {
    if (saving || !id) {
      return;
    }
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_DELETE_DESIGN))) {
      return;
    }
    setError(null);
    setSaving(true);
    const previous = contexts;
    try {
      await deleteContext(id);
      let refreshed: ContextSummary[] | null = null;
      try {
        refreshed = await listContexts();
      } catch {
        refreshed = null;
      }
      const next = contextsAfterSuccessfulDelete(refreshed, id, previous);
      setContexts(next);
      setSelected((current) => {
        if (next.some((row) => String(row.contextId ?? "") === current)) {
          return current;
        }
        return next.length > 0 ? String(next[0].contextId ?? "") : "";
      });
    } catch (e) {
      setError(mapContextDeleteError(e));
    } finally {
      setSaving(false);
    }
  }

  function addParam(): void {
    if (!paramName.trim() || !paramValue.trim()) {
      return;
    }
    setParams((prev) => [
      ...prev,
      {
        name: paramName.trim(),
        type: paramType,
        value: paramValue,
        sequence: prev.length,
      },
    ]);
    setParamName("");
    setParamValue("");
  }

  async function saveScheme(): Promise<void> {
    if (mode.kind !== "scheme-edit") {
      return;
    }
    if (!schName.trim() || !schGen.trim()) {
      setError("Name and generator are required");
      return;
    }
    setError(null);
    setSaving(true);
    const body: LocationSchemeSummary = {
      name: schName.trim(),
      generator: schGen.trim(),
      description: schDesc || undefined,
      contentTypeId: schCtype ? Number(schCtype) : undefined,
      templateId: schTemplate ? Number(schTemplate) : undefined,
      contextId: mode.contextId,
    };
    if (params.length > 0) {
      body.parameters = params;
    }
    try {
      if (mode.scheme?.schemeId) {
        await updateScheme(mode.scheme.schemeId, body);
      } else {
        await createScheme(mode.contextId, body);
      }
      setDirty(false);
      setMode({ kind: "list" });
      setSchemes(await listSchemesForContext(mode.contextId));
    } catch (e) {
      setError(mapLocationSchemeSaveError(e));
    } finally {
      setSaving(false);
    }
  }

  async function removeScheme(id: string): Promise<void> {
    if (saving) {
      return;
    }
    if (!window.confirm(message(MSG.PUBLISH_CONFIRM_DELETE_DESIGN))) {
      return;
    }
    setError(null);
    setSaving(true);
    const previous = schemes;
    try {
      await deleteScheme(id);
      let refreshed: LocationSchemeSummary[] | null = null;
      if (selected) {
        try {
          refreshed = await listSchemesForContext(selected);
        } catch {
          refreshed = null;
        }
      }
      setSchemes(schemesAfterSuccessfulDelete(refreshed, id, previous));
    } catch (e) {
      setError(mapLocationSchemeDeleteError(e));
    } finally {
      setSaving(false);
    }
  }

  if (mode.kind === "context-edit") {
    return (
      <div data-testid="context-editor">
        <h3>{mode.context ? "Edit context" : "Add context"}</h3>
        <div style={formRowStyle}>
          <label htmlFor="ctx-name">* Name</label>
          <input
            id="ctx-name"
            value={ctxName}
            onChange={(e) => {
              setCtxName(e.target.value);
              setDirty(true);
            }}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="ctx-desc">Description</label>
          <input
            id="ctx-desc"
            value={ctxDesc}
            onChange={(e) => {
              setCtxDesc(e.target.value);
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
            data-testid="context-save"
            disabled={saving}
            onClick={() => void saveContext()}
          >
            {message(MSG.PUBLISH_SAVE)}
          </button>
          <button type="button" style={buttonStyle} onClick={() => closeContextEditor()}>
            {message(MSG.PUBLISH_BACK)}
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "context-describe") {
    return (
      <div data-testid="context-describe-form">
        <h3>Context description</h3>
        <p>
          Name:{" "}
          <span data-testid="context-describe-name">{mode.source.name ?? ""}</span>
        </p>
        <p data-testid="context-describe-schemes-note">
          Location schemes stay on this context.
        </p>
        <ul data-testid="context-describe-schemes" style={listStyle}>
          {schemes.map((s) => (
            <li key={s.schemeId ?? s.name} data-testid="context-describe-scheme">
              {s.name ?? ""}
            </li>
          ))}
        </ul>
        <div style={formRowStyle}>
          <label htmlFor="context-describe-description">Description</label>
          <input
            id="context-describe-description"
            value={describeText}
            onChange={(e) => {
              setDescribeText(e.target.value);
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
            data-testid="context-describe-submit"
            disabled={saving}
            onClick={() => void saveContextDescription()}
          >
            Save description
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="context-describe-cancel"
            disabled={saving}
            onClick={closeContextDescribe}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "context-rename") {
    return (
      <div data-testid="context-rename-form">
        <h3>Rename context</h3>
        <p>
          Description:{" "}
          <span data-testid="context-rename-description">
            {mode.source.description ?? ""}
          </span>
        </p>
        <p data-testid="context-rename-schemes-note">
          Location schemes stay on this context.
        </p>
        <div style={formRowStyle}>
          <label htmlFor="context-rename-name">* Name</label>
          <input
            id="context-rename-name"
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
            data-testid="context-rename-submit"
            disabled={saving}
            onClick={() => void renameContext()}
          >
            Rename context
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="context-rename-cancel"
            disabled={saving}
            onClick={closeContextRename}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "context-copy") {
    return (
      <div data-testid="context-copy-form">
        <h3>Copy context</h3>
        <p>Source: {mode.source.name ?? mode.source.contextId}</p>
        <p>
          Description:{" "}
          <span data-testid="context-copy-description">
            {mode.source.description ?? ""}
          </span>
        </p>
        <p data-testid="context-copy-schemes-note">
          Location schemes stay on the source context.
        </p>
        <div style={formRowStyle}>
          <label htmlFor="context-copy-name">* New name</label>
          <input
            id="context-copy-name"
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
            data-testid="context-copy-submit"
            disabled={saving}
            onClick={() => void copyContext()}
          >
            Copy context
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="context-copy-cancel"
            disabled={saving}
            onClick={closeContextCopy}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "scheme-edit") {
    return (
      <div data-testid="scheme-editor">
        <h3>{mode.scheme?.schemeId ? "Edit location scheme" : "Add location scheme"}</h3>
        <div style={formRowStyle}>
          <label htmlFor="sch-name">* Name</label>
          <input
            id="sch-name"
            value={schName}
            onChange={(e) => {
              setSchName(e.target.value);
              setDirty(true);
            }}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="sch-gen">* Generator</label>
          <input
            id="sch-gen"
            value={schGen}
            onChange={(e) => {
              setSchGen(e.target.value);
              setDirty(true);
            }}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="sch-desc">Description</label>
          <input
            id="sch-desc"
            value={schDesc}
            onChange={(e) => {
              setSchDesc(e.target.value);
              setDirty(true);
            }}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="sch-ctype">Content type id</label>
          <input
            id="sch-ctype"
            value={schCtype}
            onChange={(e) => setSchCtype(e.target.value)}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="sch-tpl">Template id</label>
          <input
            id="sch-tpl"
            value={schTemplate}
            onChange={(e) => setSchTemplate(e.target.value)}
          />
        </div>
        <h4>Parameters (legacy schemes)</h4>
        <ul style={listStyle}>
          {params.map((p, i) => (
            <li key={`${p.name}-${i}`} style={listItemStyle}>
              <span>
                {p.name} ({p.type}): {p.value}
              </span>
              <button
                type="button"
                style={buttonStyle}
                onClick={() => setParams((prev) => prev.filter((_, j) => j !== i))}
              >
                Remove
              </button>
            </li>
          ))}
        </ul>
        <div style={toolbarStyle}>
          <input
            placeholder="name"
            value={paramName}
            onChange={(e) => setParamName(e.target.value)}
          />
          <select value={paramType} onChange={(e) => setParamType(e.target.value)}>
            <option value="String">String</option>
            <option value="BackendColumn">BackendColumn</option>
          </select>
          <input
            placeholder="value"
            value={paramValue}
            onChange={(e) => setParamValue(e.target.value)}
          />
          <button type="button" style={buttonStyle} onClick={addParam}>
            Add param
          </button>
        </div>
        <div style={{ marginTop: 12 }}>
          <button
            type="button"
            style={buttonStyle}
            onClick={() => setShowBrowser((v) => !v)}
          >
            {showBrowser ? "Hide" : "Show"} site root / path browser
          </button>
          {showBrowser && (
            <SiteRootBrowser
              rootPath="//Sites"
              onSelectPath={(p) => setParamValue(p)}
            />
          )}
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
            data-testid="location-scheme-save"
            disabled={saving}
            onClick={() => void saveScheme()}
          >
            {message(MSG.PUBLISH_SAVE)}
          </button>
          <button type="button" style={buttonStyle} onClick={closeSchemeEditor}>
            {message(MSG.PUBLISH_BACK)}
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "scheme-rename") {
    const source = mode.source;
    return (
      <div data-testid="scheme-rename">
        <h3>Rename location scheme</h3>
        <p>
          Generator:{" "}
          <span data-testid="scheme-rename-generator">{source.generator ?? ""}</span>
        </p>
        <p>
          Description:{" "}
          <span data-testid="scheme-rename-description">{source.description ?? ""}</span>
        </p>
        <p>
          Content type:{" "}
          <span data-testid="scheme-rename-content-type">
            {source.contentTypeId != null ? String(source.contentTypeId) : ""}
          </span>
        </p>
        <p>
          Template:{" "}
          <span data-testid="scheme-rename-template">
            {source.templateId != null ? String(source.templateId) : ""}
          </span>
        </p>
        <p data-testid="scheme-rename-parameters-note">
          Generator, description, content type, template, and parameters stay on this
          scheme.
        </p>
        <ul data-testid="scheme-rename-parameters" style={listStyle}>
          {(source.parameters ?? []).map((p, i) => (
            <li key={`${p.name}-${i}`} data-testid="scheme-rename-parameter">
              {p.name}: {p.value}
            </li>
          ))}
        </ul>
        <div style={formRowStyle}>
          <label htmlFor="scheme-rename-name">* Name</label>
          <input
            id="scheme-rename-name"
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
            data-testid="location-scheme-rename-submit"
            disabled={saving}
            onClick={() => void renameScheme()}
          >
            Rename scheme
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="location-scheme-rename-cancel"
            disabled={saving}
            onClick={closeSchemeRename}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "scheme-generator") {
    const source = mode.source;
    return (
      <div data-testid="scheme-generator">
        <h3>Location scheme generator</h3>
        <p>
          Name: <span data-testid="scheme-generator-name">{source.name ?? ""}</span>
        </p>
        <p>
          Description:{" "}
          <span data-testid="scheme-generator-description">{source.description ?? ""}</span>
        </p>
        <p>
          Content type:{" "}
          <span data-testid="scheme-generator-content-type">
            {source.contentTypeId != null ? String(source.contentTypeId) : ""}
          </span>
        </p>
        <p>
          Template:{" "}
          <span data-testid="scheme-generator-template">
            {source.templateId != null ? String(source.templateId) : ""}
          </span>
        </p>
        <p data-testid="scheme-generator-parameters-note">
          Name, description, content type, template, and parameters stay on this scheme.
        </p>
        <ul data-testid="scheme-generator-parameters" style={listStyle}>
          {(source.parameters ?? []).map((p, i) => (
            <li key={`${p.name}-${i}`} data-testid="scheme-generator-parameter">
              {p.name}: {p.value}
            </li>
          ))}
        </ul>
        <div style={formRowStyle}>
          <label htmlFor="scheme-generator-input">* Generator</label>
          <input
            id="scheme-generator-input"
            value={schemeGenerator}
            onChange={(e) => {
              setSchemeGenerator(e.target.value);
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
            data-testid="location-scheme-generator-save"
            disabled={saving}
            onClick={() => void saveSchemeGenerator()}
          >
            Save generator
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="location-scheme-generator-cancel"
            disabled={saving}
            onClick={closeSchemeGenerator}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "scheme-describe") {
    const source = mode.source;
    return (
      <div data-testid="scheme-description">
        <h3>Location scheme description</h3>
        <p>
          Name: <span data-testid="scheme-description-name">{source.name ?? ""}</span>
        </p>
        <p>
          Generator:{" "}
          <span data-testid="scheme-description-generator">{source.generator ?? ""}</span>
        </p>
        <p>
          Content type:{" "}
          <span data-testid="scheme-description-content-type">
            {source.contentTypeId != null ? String(source.contentTypeId) : ""}
          </span>
        </p>
        <p>
          Template:{" "}
          <span data-testid="scheme-description-template">
            {source.templateId != null ? String(source.templateId) : ""}
          </span>
        </p>
        <p data-testid="scheme-description-parameters-note">
          Name, generator, content type, template, and parameters stay on this scheme.
        </p>
        <ul data-testid="scheme-description-parameters" style={listStyle}>
          {(source.parameters ?? []).map((p, i) => (
            <li key={`${p.name}-${i}`} data-testid="scheme-description-parameter">
              {p.name}: {p.value}
            </li>
          ))}
        </ul>
        <div style={formRowStyle}>
          <label htmlFor="scheme-description-input">Description</label>
          <input
            id="scheme-description-input"
            value={schemeDescription}
            onChange={(e) => {
              setSchemeDescription(e.target.value);
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
            data-testid="location-scheme-description-save"
            disabled={saving}
            onClick={() => void saveSchemeDescription()}
          >
            Save description
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="location-scheme-description-cancel"
            disabled={saving}
            onClick={closeSchemeDescribe}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "scheme-content-type") {
    const source = mode.source;
    return (
      <div data-testid="scheme-content-type">
        <h3>Location scheme content type</h3>
        <p>
          Name: <span data-testid="scheme-content-type-name">{source.name ?? ""}</span>
        </p>
        <p>
          Generator:{" "}
          <span data-testid="scheme-content-type-generator">{source.generator ?? ""}</span>
        </p>
        <p>
          Description:{" "}
          <span data-testid="scheme-content-type-description">
            {source.description ?? ""}
          </span>
        </p>
        <p>
          Template:{" "}
          <span data-testid="scheme-content-type-template">
            {source.templateId != null ? String(source.templateId) : ""}
          </span>
        </p>
        <p data-testid="scheme-content-type-parameters-note">
          Name, generator, description, template, and parameters stay on this scheme.
        </p>
        <ul data-testid="scheme-content-type-parameters" style={listStyle}>
          {(source.parameters ?? []).map((p, i) => (
            <li key={`${p.name}-${i}`} data-testid="scheme-content-type-parameter">
              {p.name}: {p.value}
            </li>
          ))}
        </ul>
        <div style={formRowStyle}>
          <label htmlFor="scheme-content-type-input">* Content type id</label>
          <input
            id="scheme-content-type-input"
            value={schemeContentType}
            onChange={(e) => {
              setSchemeContentType(e.target.value);
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
            data-testid="location-scheme-content-type-save"
            disabled={saving}
            onClick={() => void saveSchemeContentType()}
          >
            Save content type
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="location-scheme-content-type-cancel"
            disabled={saving}
            onClick={closeSchemeContentType}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "scheme-template") {
    const source = mode.source;
    return (
      <div data-testid="scheme-template">
        <h3>Location scheme template</h3>
        <p>
          Name: <span data-testid="scheme-template-name">{source.name ?? ""}</span>
        </p>
        <p>
          Generator:{" "}
          <span data-testid="scheme-template-generator">{source.generator ?? ""}</span>
        </p>
        <p>
          Description:{" "}
          <span data-testid="scheme-template-description">{source.description ?? ""}</span>
        </p>
        <p>
          Content type:{" "}
          <span data-testid="scheme-template-content-type">
            {source.contentTypeId != null ? String(source.contentTypeId) : ""}
          </span>
        </p>
        <p data-testid="scheme-template-parameters-note">
          Name, generator, description, content type, and parameters stay on this scheme.
        </p>
        <ul data-testid="scheme-template-parameters" style={listStyle}>
          {(source.parameters ?? []).map((p, i) => (
            <li key={`${p.name}-${i}`} data-testid="scheme-template-parameter">
              {p.name}: {p.value}
            </li>
          ))}
        </ul>
        <div style={formRowStyle}>
          <label htmlFor="scheme-template-input">* Template id</label>
          <input
            id="scheme-template-input"
            value={schemeTemplate}
            onChange={(e) => {
              setSchemeTemplate(e.target.value);
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
            data-testid="location-scheme-template-save"
            disabled={saving}
            onClick={() => void saveSchemeTemplate()}
          >
            Save template
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="location-scheme-template-cancel"
            disabled={saving}
            onClick={closeSchemeTemplate}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "scheme-add-parameter") {
    const source = mode.source;
    return (
      <div data-testid="scheme-add-parameter">
        <h3>Add location scheme parameter</h3>
        <p>
          Name: <span data-testid="scheme-add-parameter-name">{source.name ?? ""}</span>
        </p>
        <p>
          Generator:{" "}
          <span data-testid="scheme-add-parameter-generator">
            {source.generator ?? ""}
          </span>
        </p>
        <p>
          Description:{" "}
          <span data-testid="scheme-add-parameter-description">
            {source.description ?? ""}
          </span>
        </p>
        <p>
          Content type:{" "}
          <span data-testid="scheme-add-parameter-content-type">
            {source.contentTypeId != null ? String(source.contentTypeId) : ""}
          </span>
        </p>
        <p>
          Template:{" "}
          <span data-testid="scheme-add-parameter-template">
            {source.templateId != null ? String(source.templateId) : ""}
          </span>
        </p>
        <p data-testid="scheme-add-parameter-fields-note">
          Name, generator, description, content type, and template stay on this scheme.
        </p>
        <ul data-testid="scheme-add-parameter-existing" style={listStyle}>
          {(source.parameters ?? []).map((p, i) => (
            <li key={`${p.name}-${i}`} data-testid="scheme-add-parameter-existing-row">
              {p.name} ({p.type ?? ""}): {p.value}
            </li>
          ))}
        </ul>
        <div style={formRowStyle}>
          <label htmlFor="scheme-add-parameter-name-input">* Name</label>
          <input
            id="scheme-add-parameter-name-input"
            value={paramAddName}
            onChange={(e) => {
              setParamAddName(e.target.value);
              setDirty(true);
            }}
          />
        </div>
        <div style={formRowStyle}>
          <label htmlFor="scheme-add-parameter-type-input">* Type</label>
          <select
            id="scheme-add-parameter-type-input"
            value={paramAddType}
            onChange={(e) => {
              setParamAddType(e.target.value);
              setDirty(true);
            }}
          >
            <option value="String">String</option>
            <option value="BackendColumn">BackendColumn</option>
          </select>
        </div>
        <div style={formRowStyle}>
          <label htmlFor="scheme-add-parameter-value-input">* Value</label>
          <input
            id="scheme-add-parameter-value-input"
            value={paramAddValue}
            onChange={(e) => {
              setParamAddValue(e.target.value);
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
            data-testid="location-scheme-add-parameter-save"
            disabled={saving}
            onClick={() => void saveSchemeAddParameter()}
          >
            Add parameter
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="location-scheme-add-parameter-cancel"
            disabled={saving}
            onClick={closeSchemeAddParameter}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "scheme-parameter-value") {
    const source = mode.source;
    const targetName = (mode.parameter.name ?? "").trim();
    const others = (source.parameters ?? []).filter(
      (p) => (p.name ?? "").trim() !== targetName,
    );
    return (
      <div data-testid="scheme-parameter-value">
        <h3>Change location scheme parameter value</h3>
        <p>
          Name: <span data-testid="scheme-parameter-value-name">{source.name ?? ""}</span>
        </p>
        <p>
          Generator:{" "}
          <span data-testid="scheme-parameter-value-generator">{source.generator ?? ""}</span>
        </p>
        <p>
          Description:{" "}
          <span data-testid="scheme-parameter-value-description">
            {source.description ?? ""}
          </span>
        </p>
        <p>
          Content type:{" "}
          <span data-testid="scheme-parameter-value-content-type">
            {source.contentTypeId != null ? String(source.contentTypeId) : ""}
          </span>
        </p>
        <p>
          Template:{" "}
          <span data-testid="scheme-parameter-value-template">
            {source.templateId != null ? String(source.templateId) : ""}
          </span>
        </p>
        <p data-testid="scheme-parameter-value-fields-note">
          Name, generator, description, content type, and template stay on this scheme.
          This parameter name, type, and sequence stay. Other parameters stay.
        </p>
        <p data-testid="scheme-parameter-value-target">
          <span data-testid="scheme-parameter-value-parameter-name">{targetName}</span>
          {" ("}
          <span data-testid="scheme-parameter-value-type">{mode.parameter.type ?? ""}</span>
          {") #"}
          <span data-testid="scheme-parameter-value-sequence">
            {mode.parameter.sequence != null ? String(mode.parameter.sequence) : ""}
          </span>
          {": "}
          <span data-testid="scheme-parameter-value-current">{mode.parameter.value ?? ""}</span>
        </p>
        <ul data-testid="scheme-parameter-value-others" style={listStyle}>
          {others.map((p, i) => (
            <li key={`${p.name}-${i}`} data-testid="scheme-parameter-value-other">
              {p.name} ({p.type ?? ""}): {p.value}
            </li>
          ))}
        </ul>
        <div style={formRowStyle}>
          <label htmlFor="scheme-parameter-value-input">* Value</label>
          <input
            id="scheme-parameter-value-input"
            value={paramValueDraft}
            onChange={(e) => {
              setParamValueDraft(e.target.value);
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
            data-testid="location-scheme-parameter-value-save"
            disabled={saving}
            onClick={() => void saveSchemeParameterValue()}
          >
            Save value
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="location-scheme-parameter-value-cancel"
            disabled={saving}
            onClick={closeSchemeParameterValue}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "scheme-parameter-type") {
    const source = mode.source;
    const targetName = (mode.parameter.name ?? "").trim();
    const storedType = (mode.parameter.type ?? "").trim();
    const typeChoices = locationSchemeParameterTypeChoices(storedType);
    const others = (source.parameters ?? []).filter(
      (p) => (p.name ?? "").trim() !== targetName,
    );
    return (
      <div data-testid="scheme-parameter-type">
        <h3>Change location scheme parameter type</h3>
        <p>
          Name: <span data-testid="scheme-parameter-type-name">{source.name ?? ""}</span>
        </p>
        <p>
          Generator:{" "}
          <span data-testid="scheme-parameter-type-generator">{source.generator ?? ""}</span>
        </p>
        <p>
          Description:{" "}
          <span data-testid="scheme-parameter-type-description">
            {source.description ?? ""}
          </span>
        </p>
        <p>
          Content type:{" "}
          <span data-testid="scheme-parameter-type-content-type">
            {source.contentTypeId != null ? String(source.contentTypeId) : ""}
          </span>
        </p>
        <p>
          Template:{" "}
          <span data-testid="scheme-parameter-type-template">
            {source.templateId != null ? String(source.templateId) : ""}
          </span>
        </p>
        <p data-testid="scheme-parameter-type-fields-note">
          Name, generator, description, content type, and template stay on this scheme.
          This parameter name, value, and sequence stay. Other parameters stay.
        </p>
        <p data-testid="scheme-parameter-type-target">
          <span data-testid="scheme-parameter-type-parameter-name">{targetName}</span>
          {" ("}
          <span data-testid="scheme-parameter-type-current">{storedType}</span>
          {") #"}
          <span data-testid="scheme-parameter-type-sequence">
            {mode.parameter.sequence != null ? String(mode.parameter.sequence) : ""}
          </span>
          {": "}
          <span data-testid="scheme-parameter-type-value">{mode.parameter.value ?? ""}</span>
        </p>
        <ul data-testid="scheme-parameter-type-others" style={listStyle}>
          {others.map((p, i) => (
            <li key={`${p.name}-${i}`} data-testid="scheme-parameter-type-other">
              {p.name} ({p.type ?? ""}): {p.value}
            </li>
          ))}
        </ul>
        <div style={formRowStyle}>
          <label htmlFor="scheme-parameter-type-input">* Type</label>
          <select
            id="scheme-parameter-type-input"
            value={paramTypeDraft}
            onChange={(e) => {
              setParamTypeDraft(e.target.value);
              setDirty(true);
            }}
          >
            <option value="" />
            {typeChoices.map((choice) => (
              <option key={choice} value={choice}>
                {choice}
              </option>
            ))}
          </select>
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
            data-testid="location-scheme-parameter-type-save"
            disabled={saving}
            onClick={() => void saveSchemeParameterType()}
          >
            Save type
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="location-scheme-parameter-type-cancel"
            disabled={saving}
            onClick={closeSchemeParameterType}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "scheme-parameter-sequence") {
    const source = mode.source;
    const targetName = (mode.parameter.name ?? "").trim();
    const storedSequence =
      mode.parameter.sequence != null ? String(mode.parameter.sequence) : "";
    const others = (source.parameters ?? []).filter(
      (p) => (p.name ?? "").trim() !== targetName,
    );
    return (
      <div data-testid="scheme-parameter-sequence">
        <h3>Change location scheme parameter sequence</h3>
        <p>
          Name:{" "}
          <span data-testid="scheme-parameter-sequence-name">{source.name ?? ""}</span>
        </p>
        <p>
          Generator:{" "}
          <span data-testid="scheme-parameter-sequence-generator">
            {source.generator ?? ""}
          </span>
        </p>
        <p>
          Description:{" "}
          <span data-testid="scheme-parameter-sequence-description">
            {source.description ?? ""}
          </span>
        </p>
        <p>
          Content type:{" "}
          <span data-testid="scheme-parameter-sequence-content-type">
            {source.contentTypeId != null ? String(source.contentTypeId) : ""}
          </span>
        </p>
        <p>
          Template:{" "}
          <span data-testid="scheme-parameter-sequence-template">
            {source.templateId != null ? String(source.templateId) : ""}
          </span>
        </p>
        <p data-testid="scheme-parameter-sequence-fields-note">
          Name, generator, description, content type, and template stay on this scheme.
          This parameter name, type, and value stay. Other parameters stay, including
          their sequences.
        </p>
        <p data-testid="scheme-parameter-sequence-target">
          <span data-testid="scheme-parameter-sequence-parameter-name">{targetName}</span>
          {" ("}
          <span data-testid="scheme-parameter-sequence-type">
            {mode.parameter.type ?? ""}
          </span>
          {") #"}
          <span data-testid="scheme-parameter-sequence-current">{storedSequence}</span>
          {": "}
          <span data-testid="scheme-parameter-sequence-value">
            {mode.parameter.value ?? ""}
          </span>
        </p>
        <ul data-testid="scheme-parameter-sequence-others" style={listStyle}>
          {others.map((p, i) => (
            <li key={`${p.name}-${i}`} data-testid="scheme-parameter-sequence-other">
              {p.name} ({p.type ?? ""}) #{p.sequence != null ? String(p.sequence) : ""}:{" "}
              {p.value}
            </li>
          ))}
        </ul>
        <div style={formRowStyle}>
          <label htmlFor="scheme-parameter-sequence-input">* Sequence</label>
          <input
            id="scheme-parameter-sequence-input"
            value={paramSequenceDraft}
            onChange={(e) => {
              setParamSequenceDraft(e.target.value);
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
            data-testid="location-scheme-parameter-sequence-save"
            disabled={saving}
            onClick={() => void saveSchemeParameterSequence()}
          >
            Save sequence
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="location-scheme-parameter-sequence-cancel"
            disabled={saving}
            onClick={closeSchemeParameterSequence}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "scheme-parameter-name") {
    const source = mode.source;
    const targetName = (mode.parameter.name ?? "").trim();
    const others = (source.parameters ?? []).filter(
      (p) => (p.name ?? "").trim() !== targetName,
    );
    return (
      <div data-testid="scheme-parameter-name">
        <h3>Rename location scheme parameter</h3>
        <p>
          Name: <span data-testid="scheme-parameter-name-name">{source.name ?? ""}</span>
        </p>
        <p>
          Generator:{" "}
          <span data-testid="scheme-parameter-name-generator">{source.generator ?? ""}</span>
        </p>
        <p>
          Description:{" "}
          <span data-testid="scheme-parameter-name-description">
            {source.description ?? ""}
          </span>
        </p>
        <p>
          Content type:{" "}
          <span data-testid="scheme-parameter-name-content-type">
            {source.contentTypeId != null ? String(source.contentTypeId) : ""}
          </span>
        </p>
        <p>
          Template:{" "}
          <span data-testid="scheme-parameter-name-template">
            {source.templateId != null ? String(source.templateId) : ""}
          </span>
        </p>
        <p data-testid="scheme-parameter-name-fields-note">
          Name, generator, description, content type, and template stay on this scheme.
          This parameter type, value, and sequence stay. Other parameters stay.
        </p>
        <p data-testid="scheme-parameter-name-target">
          <span data-testid="scheme-parameter-name-current">{targetName}</span>
          {" ("}
          <span data-testid="scheme-parameter-name-type">{mode.parameter.type ?? ""}</span>
          {") #"}
          <span data-testid="scheme-parameter-name-sequence">
            {mode.parameter.sequence != null ? String(mode.parameter.sequence) : ""}
          </span>
          {": "}
          <span data-testid="scheme-parameter-name-value">{mode.parameter.value ?? ""}</span>
        </p>
        <ul data-testid="scheme-parameter-name-others" style={listStyle}>
          {others.map((p, i) => (
            <li key={`${p.name}-${i}`} data-testid="scheme-parameter-name-other">
              {p.name} ({p.type ?? ""}) #{p.sequence != null ? String(p.sequence) : ""}:{" "}
              {p.value}
            </li>
          ))}
        </ul>
        <div style={formRowStyle}>
          <label htmlFor="scheme-parameter-name-input">* Name</label>
          <input
            id="scheme-parameter-name-input"
            value={paramNameDraft}
            onChange={(e) => {
              setParamNameDraft(e.target.value);
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
            data-testid="location-scheme-parameter-name-save"
            disabled={saving}
            onClick={() => void saveSchemeParameterName()}
          >
            Save name
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="location-scheme-parameter-name-cancel"
            disabled={saving}
            onClick={closeSchemeParameterName}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "scheme-remove-parameter") {
    const source = mode.source;
    const targetName = (mode.parameter.name ?? "").trim();
    const others = (source.parameters ?? []).filter(
      (p) => (p.name ?? "").trim() !== targetName,
    );
    return (
      <div data-testid="scheme-remove-parameter">
        <h3>Remove location scheme parameter</h3>
        <p>
          Name: <span data-testid="scheme-remove-parameter-name">{source.name ?? ""}</span>
        </p>
        <p>
          Generator:{" "}
          <span data-testid="scheme-remove-parameter-generator">
            {source.generator ?? ""}
          </span>
        </p>
        <p>
          Description:{" "}
          <span data-testid="scheme-remove-parameter-description">
            {source.description ?? ""}
          </span>
        </p>
        <p>
          Content type:{" "}
          <span data-testid="scheme-remove-parameter-content-type">
            {source.contentTypeId != null ? String(source.contentTypeId) : ""}
          </span>
        </p>
        <p>
          Template:{" "}
          <span data-testid="scheme-remove-parameter-template">
            {source.templateId != null ? String(source.templateId) : ""}
          </span>
        </p>
        <p data-testid="scheme-remove-parameter-fields-note">
          Name, generator, description, content type, and template stay on this scheme.
          Other parameters stay. Removing the last parameter does not delete the scheme.
        </p>
        <p data-testid="scheme-remove-parameter-target">
          {targetName} ({mode.parameter.type ?? ""}): {mode.parameter.value ?? ""}
        </p>
        <ul data-testid="scheme-remove-parameter-others" style={listStyle}>
          {others.map((p, i) => (
            <li key={`${p.name}-${i}`} data-testid="scheme-remove-parameter-other">
              {p.name} ({p.type ?? ""}): {p.value}
            </li>
          ))}
        </ul>
        {error && (
          <p style={errorStyle} role="alert">
            {error}
          </p>
        )}
        <div style={toolbarStyle}>
          <button
            type="button"
            style={primaryButtonStyle}
            data-testid="location-scheme-remove-parameter-confirm"
            disabled={saving}
            onClick={() => void saveSchemeRemoveParameter()}
          >
            Remove parameter
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="location-scheme-remove-parameter-cancel"
            disabled={saving}
            onClick={closeSchemeRemoveParameter}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  if (mode.kind === "scheme-copy") {
    return (
      <div data-testid="scheme-copy">
        <h3>Copy location scheme</h3>
        <p>
          Source: {mode.source.name ?? mode.source.schemeId}
        </p>
        <div style={formRowStyle}>
          <label htmlFor="scheme-copy-name">* New name</label>
          <input
            id="scheme-copy-name"
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
            data-testid="location-scheme-copy-submit"
            disabled={saving}
            onClick={() => void copyScheme()}
          >
            Copy scheme
          </button>
          <button
            type="button"
            style={buttonStyle}
            data-testid="location-scheme-copy-cancel"
            disabled={saving}
            onClick={closeSchemeCopy}
          >
            Cancel
          </button>
        </div>
      </div>
    );
  }

  return (
    <div data-testid="contexts-panel">
      {loading && <p>{message(MSG.PUBLISH_LOADING)}</p>}
      {error && (
        <p style={errorStyle} role="alert">
          {error}
        </p>
      )}
      <div style={toolbarStyle}>
        <label>
          Context{" "}
          <select
            value={selected}
            onChange={(e) => setSelected(e.target.value)}
            aria-label="Publishing context"
          >
            {contexts.map((c) => (
              <option key={c.contextId} value={c.contextId}>
                {c.name}
              </option>
            ))}
          </select>
        </label>
        <button
          type="button"
          style={buttonStyle}
          data-testid="design-add-context"
          onClick={() => openContextEdit(null)}
        >
          Add context
        </button>
        {selected && (
          <>
            <button
              type="button"
              style={buttonStyle}
              onClick={() => {
                const c =
                  contexts.find((x) => String(x.contextId ?? "") === selected) ?? null;
                openContextEdit(c);
              }}
            >
              Edit context
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="context-rename"
              disabled={saving}
              onClick={openContextRename}
            >
              Rename context
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="context-describe"
              disabled={saving}
              onClick={openContextDescribe}
            >
              Description
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="context-copy"
              disabled={saving}
              onClick={openContextCopy}
            >
              Copy context
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="context-delete"
              disabled={saving}
              onClick={() => void removeContext(selected)}
            >
              Delete context
            </button>
            <button
              type="button"
              style={buttonStyle}
              data-testid="design-add-location-scheme"
              onClick={() => void openSchemeEdit(null, selected)}
            >
              Add scheme
            </button>
          </>
        )}
      </div>
      {!loading && contexts.length === 0 && (
        <p style={emptyStyle}>No publishing contexts.</p>
      )}
      {selected && (
        <p>
          Description:{" "}
          <span data-testid={`context-description-${selected}`}>
            {contexts.find((row) => String(row.contextId ?? "") === selected)?.description ??
              ""}
          </span>
        </p>
      )}
      <h4>Location schemes</h4>
      {schemes.length === 0 ? (
        <p style={emptyStyle}>No schemes for this context.</p>
      ) : (
        <ul style={listStyle}>
          {schemes.map((s) => (
            <li key={s.schemeId ?? s.name} style={listItemStyle}>
              <button
                type="button"
                style={buttonStyle}
                onClick={() => void openSchemeEdit(s, selected)}
              >
                {s.name}
              </button>
              <span style={{ color: "#666" }}>
                {normalizeSchemeType(s.schemeType, s.generator)}
                {s.generator ? (
                  <>
                    {" · "}
                    <span data-testid={`scheme-list-generator-${s.schemeId}`}>
                      {s.generator}
                    </span>
                  </>
                ) : null}
                {s.schemeId ? (
                  <>
                    {" · "}
                    <span data-testid={`scheme-list-description-${s.schemeId}`}>
                      {s.description ?? ""}
                    </span>
                    {" · "}
                    <span data-testid={`scheme-list-content-type-${s.schemeId}`}>
                      {s.contentTypeId != null ? String(s.contentTypeId) : ""}
                    </span>
                    {" · "}
                    <span data-testid={`scheme-list-template-${s.schemeId}`}>
                      {s.templateId != null ? String(s.templateId) : ""}
                    </span>
                  </>
                ) : null}
              </span>
              {s.schemeId && (s.parameters ?? []).length > 0 ? (
                <div data-testid={`scheme-list-parameters-${s.schemeId}`}>
                  {(s.parameters ?? []).map((p, i) => (
                    <span key={`${p.name}-${i}`} data-testid="scheme-list-parameter">
                      {p.name} ({p.type ?? ""}): {p.value}{" "}
                      #
                      <span data-testid="scheme-list-parameter-sequence">
                        {p.sequence != null ? String(p.sequence) : ""}
                      </span>{" "}
                      <button
                        type="button"
                        style={buttonStyle}
                        data-testid="location-scheme-parameter-value"
                        disabled={saving}
                        onClick={() => void openSchemeParameterValue(s, p)}
                      >
                        Value
                      </button>
                      <button
                        type="button"
                        style={buttonStyle}
                        data-testid="location-scheme-parameter-type"
                        disabled={saving}
                        onClick={() => void openSchemeParameterType(s, p)}
                      >
                        Type
                      </button>
                      <button
                        type="button"
                        style={buttonStyle}
                        data-testid="location-scheme-parameter-sequence"
                        disabled={saving}
                        onClick={() => void openSchemeParameterSequence(s, p)}
                      >
                        Sequence
                      </button>
                      <button
                        type="button"
                        style={buttonStyle}
                        data-testid="location-scheme-parameter-name"
                        disabled={saving}
                        onClick={() => void openSchemeParameterName(s, p)}
                      >
                        Name
                      </button>
                      <button
                        type="button"
                        style={buttonStyle}
                        data-testid="location-scheme-remove-parameter"
                        disabled={saving}
                        onClick={() => void openSchemeRemoveParameter(s, p)}
                      >
                        Remove
                      </button>
                    </span>
                  ))}
                </div>
              ) : null}
              {s.schemeId && (
                <>
                  <button
                    type="button"
                    style={buttonStyle}
                    data-testid="location-scheme-rename"
                    disabled={saving}
                    onClick={() => void openSchemeRename(s)}
                  >
                    Rename
                  </button>
                  <button
                    type="button"
                    style={buttonStyle}
                    data-testid="location-scheme-generator"
                    disabled={saving}
                    onClick={() => void openSchemeGenerator(s)}
                  >
                    Generator
                  </button>
                  <button
                    type="button"
                    style={buttonStyle}
                    data-testid="location-scheme-description"
                    disabled={saving}
                    onClick={() => void openSchemeDescribe(s)}
                  >
                    Description
                  </button>
                  <button
                    type="button"
                    style={buttonStyle}
                    data-testid="location-scheme-content-type"
                    disabled={saving}
                    onClick={() => void openSchemeContentType(s)}
                  >
                    Content type
                  </button>
                  <button
                    type="button"
                    style={buttonStyle}
                    data-testid="location-scheme-template"
                    disabled={saving}
                    onClick={() => void openSchemeTemplate(s)}
                  >
                    Template
                  </button>
                  <button
                    type="button"
                    style={buttonStyle}
                    data-testid="location-scheme-add-parameter"
                    disabled={saving}
                    onClick={() => void openSchemeAddParameter(s)}
                  >
                    Add parameter
                  </button>
                  <button
                    type="button"
                    style={buttonStyle}
                    data-testid="location-scheme-copy"
                    onClick={() => openSchemeCopy(s)}
                  >
                    Copy
                  </button>
                  <button
                    type="button"
                    style={buttonStyle}
                    data-testid="location-scheme-delete"
                    disabled={saving}
                    onClick={() => void removeScheme(s.schemeId!)}
                  >
                    Delete
                  </button>
                </>
              )}
            </li>
          ))}
        </ul>
      )}
    </div>
  );
}
