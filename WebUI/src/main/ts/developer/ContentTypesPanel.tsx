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

import React, { useCallback, useEffect, useLayoutEffect, useRef, useState } from "react";
import { resolveContentTypeObjectGuid } from "../api/displayFormatGuid";
import {
  asContentTypeText,
  contentTypeSelectionKey,
  copyContentType,
  isValidContentTypeName,
  listContentTypes,
  unwrapContentTypeList,
} from "../api/developer/contentTypesApi";
import type { ContentTypeDetail, ContentTypeSummary } from "../api/developer/types";
import { CatalogHint, CatalogStatus, SimpleCatalogTable } from "./CatalogTable";
import { catalogColors, monoCell, mutedCell, openButtonStyle } from "./catalogStyles";
import { ContentTypeCreatePanel } from "./ContentTypeCreatePanel";
import { ContentTypeDetailPanel } from "./ContentTypeDetailPanel";
import { ContentTypeImportWizard } from "./ContentTypeImportWizard";
import { DeveloperSectionErrorBoundary } from "./DeveloperSectionErrorBoundary";
import { panelErrMsg } from "./errors";
import { DEV_MSG } from "./messages";
import {
  applyObjectSorter,
  loadObjectSorterPreference,
  type ObjectSorterRow,
} from "./objectSorter";

/** Catalog cell / sort keys — JAXB wraps unwrap in {@link asContentTypeText}. */
function asCatalogText(value: unknown): string {
  return asContentTypeText(value);
}

/**
 * Catalog rows must always be a real array. listContentTypes already unwraps;
 * re-apply here so a missed envelope cannot {@code [...obj].sort} into
 * DeveloperSectionErrorBoundary (#3706).
 */
function asContentTypeCatalog(raw: unknown): ContentTypeSummary[] {
  return unwrapContentTypeList(raw);
}

function displayId(ct: ContentTypeSummary): string {
  return resolveContentTypeObjectGuid(ct) || "—";
}

function selectionKey(ct: ContentTypeSummary): string | null {
  return contentTypeSelectionKey(ct);
}

function catalogSorterRow(ct: ContentTypeSummary): ObjectSorterRow {
  const name = asCatalogText(ct.name);
  const label = asCatalogText(ct.label);
  return {
    id: selectionKey(ct) || name,
    name,
    label,
  };
}

type SelectedContentType = {
  idOrName: string;
  catalogGuid?: string;
};

/**
 * P0.1 list + P0.2 detail + CD-01 catalog create / lock-held delete.
 */
export function ContentTypesPanel(): React.ReactElement {
  const [items, setItems] = useState<ContentTypeSummary[] | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [selected, setSelected] = useState<SelectedContentType | "new" | null>(null);
  const [copyOf, setCopyOf] = useState<string | null>(null);
  const [copyName, setCopyName] = useState("");
  const [copyReview, setCopyReview] = useState<string | null>(null);
  const [copyError, setCopyError] = useState<string | null>(null);
  const [copying, setCopying] = useState(false);
  const mountedRef = useRef(true);
  /** Copy source whose form just closed; drives #7344 focus return. */
  const prevCopyOfRef = useRef<string | null>(null);
  /** Copy-name input; drives #7354 deterministic autofocus. */
  const copyNameRef = useRef<HTMLInputElement | null>(null);

  // #7354: mount-aware ref callback — focuses every newly mounted name
  // input, including remounts where the layout-effect deps below are
  // unchanged (e.g. Back from a detail/Create panel opened while the copy
  // form was up: copyOf/copyReview/copying never change, so the effect
  // below does not re-run but a fresh input mounts).
  const focusCopyNameRef = useCallback((el: HTMLInputElement | null) => {
    copyNameRef.current = el;
    if (el && !el.disabled && typeof el.focus === "function") {
      el.focus();
    }
  }, []);

  /**
   * Find the catalog Copy button for a content type by its selection key
   * (mirrors the RolesPanel #6905 row lookup; avoids CSS.escape in jsdom).
   */
  function findCopyButton(name: string): HTMLElement | null {
    const btn = Array.from(
      document.querySelectorAll('button[data-testid="developer-ct-copy"]'),
    ).find((el) => el.getAttribute("data-ct-name") === name);
    return btn instanceof HTMLElement ? btn : null;
  }

  useEffect(() => {
    mountedRef.current = true;
    return () => {
      mountedRef.current = false;
    };
  }, []);

  const reload = useCallback((opts?: { showLoading?: boolean }) => {
    if (!mountedRef.current) {
      return Promise.resolve();
    }
    if (opts?.showLoading) {
      setItems(null);
    }
    setError(null);
    return listContentTypes()
      .then((list) => {
        if (!mountedRef.current) return;
        setItems(asContentTypeCatalog(list));
      })
      .catch((err: unknown) => {
        if (!mountedRef.current) return;
        // Session redirect navigates away; still leave an error so UI does not hang
        // if navigation is delayed or blocked. Leave items null so the failure
        // reads as a named load error (with Retry) instead of an empty catalog.
        setError(panelErrMsg(err, DEV_MSG.CT_ERROR));
      });
  }, []);

  useEffect(() => {
    void reload({ showLoading: true });
  }, [reload]);

  function openContentType(ct: ContentTypeSummary) {
    const idOrName = selectionKey(ct);
    if (!idOrName) return;
    setSelected({
      idOrName,
      catalogGuid: resolveContentTypeObjectGuid(ct),
    });
  }

  function handleDeleted(): void {
    setSelected(null);
    void reload();
  }

  function resetCopy(): void {
    setCopyOf(null);
    setCopyName("");
    setCopyReview(null);
    setCopyError(null);
  }

  // #7344: when the copy form closes (Escape or Cancel) the name input
  // unmounts and focus would fall back to <body>. Return focus to the
  // invoking catalog Copy button so keyboard and screen-reader users keep
  // their place (mirrors #6905 / #6733). On a successful copy the catalog
  // unmounts into the detail view, so the lookup no-ops there. Layout
  // effect so focus lands before paint.
  useLayoutEffect(() => {
    if (prevCopyOfRef.current != null && copyOf == null) {
      const btn = findCopyButton(prevCopyOfRef.current);
      if (btn && !btn.hasAttribute("disabled") && typeof btn.focus === "function") {
        btn.focus();
      }
    }
    prevCopyOfRef.current = copyOf;
  }, [copyOf]);

  // #7354: React's autoFocus attribute fires during commit and loses to
  // the invoking click's default focus under load (cycle-verify: focus
  // stayed inactive 3/3 runs on tip although the input mounted). Focus the
  // name input explicitly in a layout effect whenever the name step is
  // shown — on open and on Back from the confirm step — so focus lands
  // after commit instead of racing it. Guards mirror useDialogEscape's
  // focusSafely (attached, enabled, focusable). The #7344 close effect
  // above only runs when copyOf becomes null, so the two never fight.
  useLayoutEffect(() => {
    if (copyOf != null && copyReview == null && !copying) {
      const el = copyNameRef.current;
      if (
        el &&
        document.contains(el) &&
        !el.disabled &&
        typeof el.focus === "function"
      ) {
        el.focus();
      }
    }
  }, [copyOf, copyReview, copying]);

  // #7344: Escape cancels the copy form without posting, reusing the
  // existing resetCopy (typed name discarded, catalog row unchanged).
  function handleCopyNameKeyDown(e: React.KeyboardEvent<HTMLInputElement>): void {
    if (e.key === "Escape" && !copying) {
      resetCopy();
    }
  }

  /**
   * Step 1 of clone: a valid new name advances to the deep-copy confirm step.
   * Nothing is posted until the confirm step submits.
   */
  function requestCopyReview(): void {
    if (!copyOf || !isValidContentTypeName(copyName) || copying) {
      return;
    }
    setCopyError(null);
    setCopyReview(copyName.trim());
  }

  async function submitCopy(): Promise<void> {
    const target = copyReview ?? copyName.trim();
    if (!copyOf || !isValidContentTypeName(target) || copying) {
      return;
    }
    setCopying(true);
    setCopyError(null);
    try {
      const detail = await copyContentType(copyOf, target);
      if (!mountedRef.current) return;
      resetCopy();
      handleCreated(detail);
    } catch (err: unknown) {
      if (!mountedRef.current) return;
      // Back to the name step with the typed name preserved; the catalog row
      // is unchanged because the POST never created the type.
      setCopyReview(null);
      setCopyError(panelErrMsg(err, DEV_MSG.CT_COPY_ERROR));
    } finally {
      if (mountedRef.current) {
        setCopying(false);
      }
    }
  }

  function handleCreated(detail: ContentTypeDetail): void {
    const idOrName = asContentTypeText(detail.name);
    void reload();
    if (idOrName) {
      setSelected({
        idOrName,
        catalogGuid: resolveContentTypeObjectGuid(detail),
      });
    } else {
      setSelected(null);
    }
  }

  if (selected === "new") {
    return (
      <ContentTypeCreatePanel
        onBack={() => setSelected(null)}
        onCreated={handleCreated}
      />
    );
  }

  if (selected) {
    return (
      <DeveloperSectionErrorBoundary
        label={DEV_MSG.TAB_CONTENT_TYPES}
        testId="developer-ct-detail-error"
      >
        <ContentTypeDetailPanel
          idOrName={selected.idOrName}
          catalogGuid={selected.catalogGuid}
          onBack={() => setSelected(null)}
          onDeleted={handleDeleted}
          onRenamed={() => {
            void reload();
          }}
          onSaved={() => {
            void reload();
          }}
        />
      </DeveloperSectionErrorBoundary>
    );
  }

  const sorted =
    items && items.length > 0
      ? applyObjectSorter(items, catalogSorterRow, loadObjectSorterPreference())
      : [];

  return (
    <div data-testid="developer-ct-panel">
      <div
        style={{
          display: "flex",
          justifyContent: "space-between",
          alignItems: "center",
          marginBottom: "12px",
          gap: "12px",
          flexWrap: "wrap",
        }}
      >
        <CatalogHint>{DEV_MSG.CT_HINT}</CatalogHint>
        <button
          type="button"
          data-testid="developer-ct-new"
          onClick={() => setSelected("new")}
          style={{
            padding: "8px 14px",
            background: catalogColors.accent,
            color: "#fff",
            border: "none",
            borderRadius: "4px",
            cursor: "pointer",
          }}
        >
          {DEV_MSG.CT_NEW}
        </button>
      </div>
      <ContentTypeImportWizard
        onImported={() => void reload()}
        existingNames={(items ?? []).map((ct) => asCatalogText(ct.name)).filter((n) => n.length > 0)}
      />
      {copyOf ? (
        copyReview != null ? (
          <div
            data-testid="developer-ct-copy-confirm"
            style={{
              display: "flex",
              gap: "8px",
              alignItems: "center",
              flexWrap: "wrap",
              marginBottom: "12px",
            }}
          >
            <span>
              {DEV_MSG.CT_COPY_CONFIRM_TITLE}: <code>{copyOf}</code> →{" "}
              <code>{copyReview}</code>?
            </span>
            <span data-testid="developer-ct-copy-scope" style={mutedCell}>
              {DEV_MSG.CT_COPY_SCOPE}
            </span>
            <button
              type="button"
              data-testid="developer-ct-copy-submit"
              disabled={copying}
              onClick={() => void submitCopy()}
            >
              {copying ? DEV_MSG.CT_COPYING : DEV_MSG.CT_COPY_SUBMIT}
            </button>
            <button
              type="button"
              data-testid="developer-ct-copy-back"
              disabled={copying}
              onClick={() => setCopyReview(null)}
            >
              {DEV_MSG.CT_COPY_BACK}
            </button>
            <button
              type="button"
              data-testid="developer-ct-copy-cancel"
              disabled={copying}
              onClick={resetCopy}
            >
              {DEV_MSG.CT_COPY_CANCEL}
            </button>
            {copyError ? (
              <span data-testid="developer-ct-copy-error" role="alert">
                {copyError}
              </span>
            ) : null}
          </div>
        ) : (
        <form
          data-testid="developer-ct-copy-form"
          onSubmit={(event) => {
            event.preventDefault();
            requestCopyReview();
          }}
          style={{
            display: "flex",
            gap: "8px",
            alignItems: "center",
            flexWrap: "wrap",
            marginBottom: "12px",
          }}
        >
          <span>
            {DEV_MSG.CT_COPY_TITLE}: <code>{copyOf}</code>
          </span>
          <label>
            {DEV_MSG.CT_COPY_NAME}{" "}
            <input
              data-testid="developer-ct-copy-name"
              ref={focusCopyNameRef}
              value={copyName}
              onChange={(event) => setCopyName(event.target.value)}
              disabled={copying}
              onKeyDown={handleCopyNameKeyDown}
            />
          </label>
          <button
            type="submit"
            data-testid="developer-ct-copy-review"
            disabled={copying || !isValidContentTypeName(copyName)}
          >
            {DEV_MSG.CT_COPY_REVIEW}
          </button>
          <button
            type="button"
            data-testid="developer-ct-copy-cancel"
            disabled={copying}
            onClick={resetCopy}
          >
            {DEV_MSG.CT_COPY_CANCEL}
          </button>
          {copyError ? (
            <span data-testid="developer-ct-copy-error" role="alert">
              {copyError}
            </span>
          ) : (
            <span style={mutedCell}>{DEV_MSG.CT_COPY_HINT}</span>
          )}
          <span data-testid="developer-ct-copy-scope" style={mutedCell}>
            {DEV_MSG.CT_COPY_SCOPE}
          </span>
        </form>
        )
      ) : null}
      {error ? (
        <>
          <CatalogStatus testId="developer-ct-error" error>
            {error}
          </CatalogStatus>
          <button
            type="button"
            data-testid="developer-ct-retry"
            onClick={() => reload({ showLoading: true })}
            style={{
              padding: "8px 14px",
              background: catalogColors.accent,
              color: "#fff",
              border: "none",
              borderRadius: "4px",
              cursor: "pointer",
            }}
          >
            {DEV_MSG.CT_RETRY}
          </button>
        </>
      ) : items == null ? (
        <CatalogStatus testId="developer-ct-loading">{DEV_MSG.CT_LOADING}</CatalogStatus>
      ) : items.length === 0 ? (
        <>
          <CatalogStatus testId="developer-ct-empty">{DEV_MSG.CT_EMPTY}</CatalogStatus>
          <div data-testid="developer-ct-empty-hint">{DEV_MSG.CT_EMPTY_HINT}</div>
          <button
            type="button"
            data-testid="developer-ct-empty-new"
            onClick={() => setSelected("new")}
            style={{
              padding: "8px 14px",
              background: catalogColors.accent,
              color: "#fff",
              border: "none",
              borderRadius: "4px",
              cursor: "pointer",
              marginTop: "8px",
            }}
          >
            {DEV_MSG.CT_NEW}
          </button>
        </>
      ) : (
      <SimpleCatalogTable
        tableTestId="developer-ct-table"
        rowTestId="developer-ct-row"
        columns={[
          DEV_MSG.CT_COL_LABEL,
          DEV_MSG.CT_COL_NAME,
          DEV_MSG.CT_COL_ID,
          DEV_MSG.CT_COL_DESCRIPTION,
          DEV_MSG.CT_COPY,
        ]}
        rows={sorted.map((ct) => {
          const resolved = resolveContentTypeObjectGuid(ct);
          const label = asCatalogText(ct.label);
          const name = asCatalogText(ct.name);
          const key =
            resolved ||
            name ||
            `${label || "ct"}-${displayId(ct)}`;
          const openKey = selectionKey(ct);
          const interactive = !!openKey;
          return {
            key,
            dataAttrs: openKey ? { "data-ct-name": openKey } : undefined,
            cells: [
              interactive ? (
                <button
                  key="open"
                  type="button"
                  style={openButtonStyle}
                  data-testid="developer-ct-open"
                  data-ct-name={openKey}
                  aria-label={`Open ${label || name || openKey}`}
                  onClick={() => openContentType(ct)}
                >
                  {label || name || "—"}
                </button>
              ) : (
                <span key="lbl" style={mutedCell}>
                  {label || "—"}
                </span>
              ),
              <span key="n" style={monoCell}>
                {name || "—"}
              </span>,
              <span key="i" style={monoCell}>
                {displayId(ct)}
              </span>,
              <span
                key="d"
                style={mutedCell}
                data-testid="developer-ct-catalog-description"
                data-ct-name={openKey || ""}
                data-ct-description={asCatalogText(ct.description)}
              >
                {asCatalogText(ct.description)}
              </span>,
              interactive ? (
                <button
                  key="copy"
                  type="button"
                  data-testid="developer-ct-copy"
                  data-ct-name={openKey}
                  onClick={() => {
                    setCopyOf(openKey);
                    setCopyName("");
                    setCopyReview(null);
                    setCopyError(null);
                  }}
                >
                  {DEV_MSG.CT_COPY}
                </button>
              ) : (
                <span key="copy-off">—</span>
              ),
            ],
          };
        })}
      />
      )}
    </div>
  );
}
