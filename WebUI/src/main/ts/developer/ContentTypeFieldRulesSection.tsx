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

import React, { useEffect, useImperativeHandle, useRef, useState } from "react";
import {
  emptyFieldRuleExpressionTexts,
  fieldRuleExpressionTextsEqual,
  fieldRuleExpressionsToTexts,
  getContentTypeFieldRuleExpressions,
  replaceContentTypeFieldRuleExpressions,
  textsToFieldRuleExpressions,
  type FieldRuleExpressionTexts,
} from "../api/developer/contentTypeFieldRules";
import type { ContentTypeFieldSummary } from "../api/developer/types";
import { isApiError } from "../api/client";
import { catalogColors } from "./catalogStyles";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";

const inputStyle: React.CSSProperties = {
  padding: "8px",
  border: `1px solid ${catalogColors.softBorder}`,
  borderRadius: "4px",
  font: "inherit",
  width: "100%",
  boxSizing: "border-box",
};

const textareaStyle: React.CSSProperties = {
  ...inputStyle,
  fontFamily: "monospace",
  minHeight: "4.5rem",
  resize: "vertical",
};

export type ContentTypeFieldRulesHandle = {
  isDirty: () => boolean;
  save: () => Promise<void>;
};

function fieldNames(fields: ContentTypeFieldSummary[]): string[] {
  const names: string[] = [];
  for (const f of fields) {
    const n = (f.name || "").trim();
    if (n && !names.includes(n)) {
      names.push(n);
    }
  }
  return names;
}

function FieldRuleTextarea({
  id,
  testId,
  label,
  hint,
  value,
  canEdit,
  onChange,
}: {
  id: string;
  testId: string;
  label: string;
  hint: string;
  value: string;
  canEdit: boolean;
  onChange: (next: string) => void;
}): React.ReactElement {
  return (
    <div style={{ marginTop: "12px" }}>
      <label htmlFor={id} style={{ display: "block", marginBottom: 4 }}>
        {label}
      </label>
      <p style={{ color: catalogColors.muted, fontSize: "0.8rem", margin: "0 0 6px" }}>{hint}</p>
      <textarea
        id={id}
        data-testid={testId}
        style={textareaStyle}
        value={value}
        disabled={!canEdit}
        readOnly={!canEdit}
        aria-disabled={canEdit ? undefined : true}
        onChange={(e) => {
          if (!canEdit) {
            return;
          }
          onChange(e.target.value);
        }}
      />
    </div>
  );
}

export const ContentTypeFieldRulesSection = React.forwardRef<
  ContentTypeFieldRulesHandle,
  {
    idOrName: string;
    fields: ContentTypeFieldSummary[];
    canEdit: boolean;
    onDirtyChange?: (dirty: boolean) => void;
    onLockLost?: () => void;
  }
>(function ContentTypeFieldRulesSection(
  { idOrName, fields, canEdit, onDirtyChange, onLockLost },
  ref,
): React.ReactElement {
  const names = fieldNames(fields);
  const catalogKey = names.join("|");
  const [selected, setSelected] = useState(names[0] || "");
  const [drafts, setDrafts] = useState<Record<string, FieldRuleExpressionTexts>>({});
  const [loaded, setLoaded] = useState<Record<string, FieldRuleExpressionTexts>>({});
  const [busyField, setBusyField] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const onDirtyChangeRef = useRef(onDirtyChange);
  const onLockLostRef = useRef(onLockLost);

  useEffect(() => {
    onDirtyChangeRef.current = onDirtyChange;
  }, [onDirtyChange]);

  useEffect(() => {
    onLockLostRef.current = onLockLost;
  }, [onLockLost]);

  useEffect(() => {
    setDrafts({});
    setLoaded({});
    setError(null);
    setBusyField(null);
    setSelected(names[0] || "");
  }, [idOrName, catalogKey]);

  useEffect(() => {
    if (!selected) {
      return;
    }
    if (loaded[selected] != null) {
      return;
    }
    const fieldName = selected;
    let cancelled = false;
    setBusyField(fieldName);
    setError(null);
    getContentTypeFieldRuleExpressions(idOrName, fieldName)
      .then((env) => {
        if (cancelled) return;
        const texts = fieldRuleExpressionsToTexts(env);
        setLoaded((prev) => ({ ...prev, [fieldName]: texts }));
        setDrafts((prev) => (prev[fieldName] != null ? prev : { ...prev, [fieldName]: texts }));
      })
      .catch((err: unknown) => {
        if (cancelled) return;
        setError(panelErrMsg(err, DEV_MSG.CT_FR_LOAD_ERROR));
        const empty = emptyFieldRuleExpressionTexts();
        setLoaded((prev) => ({ ...prev, [fieldName]: empty }));
        setDrafts((prev) => (prev[fieldName] != null ? prev : { ...prev, [fieldName]: empty }));
      })
      .finally(() => {
        if (!cancelled) {
          setBusyField(null);
        }
      });
    return () => {
      cancelled = true;
    };
  }, [idOrName, selected, loaded[selected]]);

  const dirtyFields = names.filter((n) => {
    const d = drafts[n];
    const l = loaded[n];
    if (d == null || l == null) {
      return false;
    }
    return !fieldRuleExpressionTextsEqual(d, l);
  });
  const dirty = dirtyFields.length > 0;

  useEffect(() => {
    onDirtyChangeRef.current?.(dirty);
  }, [dirty]);

  const current = drafts[selected] ?? emptyFieldRuleExpressionTexts();

  function patchDraft(patch: Partial<FieldRuleExpressionTexts>) {
    if (!canEdit || !selected) {
      return;
    }
    setError(null);
    setDrafts((prev) => ({
      ...prev,
      [selected]: { ...(prev[selected] ?? emptyFieldRuleExpressionTexts()), ...patch },
    }));
  }

  async function save(): Promise<void> {
    if (!dirty) {
      return;
    }
    for (const fieldName of dirtyFields) {
      const texts = drafts[fieldName];
      if (!texts) {
        continue;
      }
      let body;
      try {
        body = textsToFieldRuleExpressions(fieldName, texts);
      } catch (err: unknown) {
        const msg = err instanceof Error ? err.message : String(err);
        setError(`${DEV_MSG.CT_FR_PARSE_ERROR} ${msg}`);
        throw err;
      }
      try {
        const saved = await replaceContentTypeFieldRuleExpressions(idOrName, fieldName, body);
        const nextTexts = fieldRuleExpressionsToTexts(saved);
        setLoaded((prev) => ({ ...prev, [fieldName]: nextTexts }));
        setDrafts((prev) => ({ ...prev, [fieldName]: nextTexts }));
      } catch (err: unknown) {
        if (isApiError(err) && err.status === 409) {
          onLockLostRef.current?.();
        }
        setError(panelErrMsg(err, DEV_MSG.CT_FR_SAVE_ERROR));
        throw err;
      }
    }
  }

  useImperativeHandle(
    ref,
    () => ({
      isDirty: () => dirty,
      save,
    }),
    // save closes over dirtyFields/drafts; rebind when those change
    // eslint-disable-next-line react-hooks/exhaustive-deps
    [dirty, dirtyFields.join("|"), drafts, idOrName],
  );

  return (
    <section style={{ marginBottom: "16px" }} data-testid="developer-ct-field-rule-expressions">
      <h3 style={{ fontSize: "1rem" }}>{DEV_MSG.CT_FIELD_RULES}</h3>
      <p style={{ color: catalogColors.muted, fontSize: "0.9rem" }}>{DEV_MSG.CT_FIELD_RULES_HINT}</p>
      {error ? (
        <div role="alert" data-testid="developer-ct-fr-error" style={{ color: catalogColors.error }}>
          {error}
        </div>
      ) : null}
      {names.length === 0 ? (
        <p style={{ color: catalogColors.empty }} data-testid="developer-ct-fr-empty">
          {DEV_MSG.CT_NONE}
        </p>
      ) : (
        <>
          <div style={{ marginTop: "8px", maxWidth: "24rem" }}>
            <label htmlFor="ct-fr-field" style={{ display: "block", marginBottom: 4 }}>
              {DEV_MSG.CT_FR_FIELD}
            </label>
            <select
              id="ct-fr-field"
              data-testid="developer-ct-fr-field"
              style={inputStyle}
              value={selected}
              disabled={names.length === 0}
              onChange={(e) => {
                setError(null);
                setSelected(e.target.value);
              }}
            >
              {names.map((n) => (
                <option key={n} value={n}>
                  {n}
                </option>
              ))}
            </select>
          </div>
          {busyField === selected ? (
            <div data-testid="developer-ct-fr-loading" style={{ marginTop: "8px" }}>
              {DEV_MSG.CT_FR_LOADING}
            </div>
          ) : null}
          <FieldRuleTextarea
            id="ct-fr-validation"
            testId="developer-ct-fr-validation"
            label={DEV_MSG.CT_FR_VALIDATION}
            hint={DEV_MSG.CT_FR_VALIDATION_HINT}
            value={current.validation}
            canEdit={canEdit}
            onChange={(validation) => patchDraft({ validation })}
          />
          <FieldRuleTextarea
            id="ct-fr-visibility"
            testId="developer-ct-fr-visibility"
            label={DEV_MSG.CT_FR_VISIBILITY}
            hint={DEV_MSG.CT_FR_VISIBILITY_HINT}
            value={current.visibility}
            canEdit={canEdit}
            onChange={(visibility) => patchDraft({ visibility })}
          />
          <FieldRuleTextarea
            id="ct-fr-input"
            testId="developer-ct-fr-input"
            label={DEV_MSG.CT_FR_INPUT}
            hint={DEV_MSG.CT_FR_TRANSLATION_HINT}
            value={current.inputTranslation}
            canEdit={canEdit}
            onChange={(inputTranslation) => patchDraft({ inputTranslation })}
          />
          <FieldRuleTextarea
            id="ct-fr-output"
            testId="developer-ct-fr-output"
            label={DEV_MSG.CT_FR_OUTPUT}
            hint={DEV_MSG.CT_FR_TRANSLATION_HINT}
            value={current.outputTranslation}
            canEdit={canEdit}
            onChange={(outputTranslation) => patchDraft({ outputTranslation })}
          />
        </>
      )}
    </section>
  );
});
