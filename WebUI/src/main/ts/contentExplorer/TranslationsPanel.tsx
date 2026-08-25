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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Explorer P-Trans panel (#2430 / #2411 slice C).
 *
 * <p>Shows the selected content item's current locale and related translation
 * variants (GET public REST), and lets an authorized user create new locale
 * variants (POST). In-flight queue / session content-locale are not exposed
 * by the REST façade — the panel states that explicitly rather than inventing
 * status UI.</p>
 */

import React, { useCallback, useEffect, useMemo, useState } from "react";
import { listLocales } from "../api/developer/localesApi";
import type { LocaleSummary } from "../api/developer/types";
import {
  availableTargetLocales,
  createTranslations,
  listItemTranslationVariants,
  TranslationAuthError,
  type CreateTranslationsResult,
  type ItemTranslationVariants,
  type TranslationLocaleOption,
  type TranslationVariant,
} from "../api/contentExplorer/translationsApi";
import { formatApiError } from "../api/client";
import { message } from "../i18n/message";
import { parseExplorerContentId } from "./menuCatalogLoad";
import { EXPLORER_MSG } from "./messages";

export interface TranslationsPanelProps {
  /** Legacy content id of the selected item (string form from path DTOs). */
  itemId: string;
  /** Optional display name for chrome (not required for REST). */
  itemLabel?: string;
  /** Test seam: override GET variants. */
  loadVariants?: (itemId: string) => Promise<ItemTranslationVariants>;
  /** Test seam: override locale catalog for create-variant targets. */
  loadLocaleCatalog?: () => Promise<LocaleSummary[]>;
  /** Test seam: override POST create-variant. */
  createVariants?: (body: {
    itemIds: number[];
    locales?: string[];
  }) => Promise<CreateTranslationsResult>;
  /** Fired after a successful create so the host can refresh lists. */
  onCreated?: (result: CreateTranslationsResult) => void;
  ariaLabel?: string;
  className?: string;
}

type PanelState =
  | { kind: "loading" }
  | { kind: "ok"; data: ItemTranslationVariants }
  | { kind: "auth" }
  | { kind: "error"; message: string };

async function defaultLoadVariants(
  itemId: string,
): Promise<ItemTranslationVariants> {
  return listItemTranslationVariants(itemId);
}

async function defaultLoadLocaleCatalog(): Promise<LocaleSummary[]> {
  return listLocales();
}

async function defaultCreateVariants(body: {
  itemIds: number[];
  locales?: string[];
}): Promise<CreateTranslationsResult> {
  return createTranslations(body);
}

/**
 * Resolve Explorer row id to a legacy content id for create-variant POST
 * ({@code itemIds: number[]}).
 *
 * <p>List rows are usually Percussion GUIDs ({@code 16777215-101-551}); {@link
 * Number}({@code itemId}) is NaN for those and must not be used (#3545 /
 * parent #2649). Folders/sites with no content id still return null.
 *
 * <p>Do <strong>not</strong> use this stripped id for variants GET (#3703):
 * {@code GET …/translations/551} 404s while the full GUID succeeds.
 */
function resolveTranslationsContentId(itemId: string): number | null {
  return parseExplorerContentId(itemId);
}

/**
 * Path key for {@code GET /rest/content-explorer/translations/{itemId}}.
 *
 * <p>REST accepts a hyphenated GUID or a bare numeric content id. Prefer the
 * raw Explorer row id so GUID selections are not reduced to the last
 * segment (#3703 / parent #2649).
 */
export function translationsVariantsItemKey(itemId: string): string {
  return itemId.trim();
}

function roleLabel(role: string | null | undefined): string {
  if (role === "source") {
    return message(EXPLORER_MSG.TRANSLATIONS_ROLE_SOURCE);
  }
  if (role === "translation") {
    return message(EXPLORER_MSG.TRANSLATIONS_ROLE_TRANSLATION);
  }
  return role ?? "—";
}

export function TranslationsPanel(
  props: TranslationsPanelProps,
): React.JSX.Element {
  const {
    itemId,
    itemLabel,
    loadVariants = defaultLoadVariants,
    loadLocaleCatalog = defaultLoadLocaleCatalog,
    createVariants = defaultCreateVariants,
    onCreated,
    ariaLabel,
    className,
  } = props;

  const [state, setState] = useState<PanelState>({ kind: "loading" });
  const [catalog, setCatalog] = useState<LocaleSummary[]>([]);
  const [selectedLocales, setSelectedLocales] = useState<ReadonlySet<string>>(
    () => new Set(),
  );
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);
  const [createForbidden, setCreateForbidden] = useState(false);
  const [createSuccess, setCreateSuccess] = useState<string | null>(null);
  const [reloadToken, setReloadToken] = useState(0);

  useEffect(() => {
    let alive = true;
    if (!itemId) {
      setState({ kind: "auth" });
      return;
    }
    // GET uses the raw row id (full GUID or numeric). Do not strip the last
    // GUID segment — that 404s (#3703). Parse-null still GETs the raw id
    // (unit tests / direct mounts). Create-variant POST uses numeric ids.
    const variantsKey = translationsVariantsItemKey(itemId);
    setState({ kind: "loading" });
    setCreateError(null);
    setCreateSuccess(null);
    loadVariants(variantsKey)
      .then((data) => {
        if (!alive) return;
        setState({ kind: "ok", data });
      })
      .catch((err: unknown) => {
        if (!alive) return;
        if (
          err instanceof TranslationAuthError ||
          (err &&
            typeof err === "object" &&
            "status" in err &&
            (err as { status: number }).status === 403)
        ) {
          setState({ kind: "auth" });
          return;
        }
        setState({
          kind: "error",
          message: formatApiError(err, message(EXPLORER_MSG.TRANSLATIONS_ERROR)),
        });
      });
    return () => {
      alive = false;
    };
  }, [itemId, loadVariants, reloadToken]);

  useEffect(() => {
    let alive = true;
    loadLocaleCatalog()
      .then((rows) => {
        if (!alive) return;
        setCatalog(rows ?? []);
      })
      .catch(() => {
        if (!alive) return;
        // Non-fatal: create form can still POST without a catalog (all auto).
        setCatalog([]);
      });
    return () => {
      alive = false;
    };
  }, [loadLocaleCatalog]);

  const variants: TranslationVariant[] =
    state.kind === "ok" ? (state.data.variants ?? []) : [];
  const currentLocale =
    state.kind === "ok" ? (state.data.locale ?? null) : null;

  const targets: TranslationLocaleOption[] = useMemo(
    () => availableTargetLocales(catalog, variants, currentLocale),
    [catalog, variants, currentLocale],
  );

  const toggleLocale = useCallback((lang: string, next: boolean) => {
    setSelectedLocales((prev) => {
      const copy = new Set(prev);
      if (next) copy.add(lang);
      else copy.delete(lang);
      return copy;
    });
  }, []);

  const handleCreate = useCallback(async () => {
    const numericId = resolveTranslationsContentId(itemId);
    if (numericId == null) {
      setCreateError(message(EXPLORER_MSG.TRANSLATIONS_INVALID_ITEM));
      return;
    }
    if (selectedLocales.size === 0) {
      setCreateError(message(EXPLORER_MSG.TRANSLATIONS_SELECT_LOCALE));
      return;
    }
    setCreating(true);
    setCreateError(null);
    setCreateSuccess(null);
    setCreateForbidden(false);
    try {
      const result = await createVariants({
        itemIds: [numericId],
        locales: Array.from(selectedLocales),
      });
      const count = result.created?.length ?? 0;
      setCreateSuccess(
        count === 1
          ? message(EXPLORER_MSG.TRANSLATIONS_CREATE_SUCCESS_SINGULAR)
          : message(EXPLORER_MSG.TRANSLATIONS_CREATE_SUCCESS_PLURAL).replace(
              "{count}",
              String(count),
            ),
      );
      setSelectedLocales(new Set());
      setReloadToken((n) => n + 1);
      onCreated?.(result);
    } catch (err: unknown) {
      if (
        err instanceof TranslationAuthError ||
        (err &&
          typeof err === "object" &&
          "status" in err &&
          (err as { status: number }).status === 403)
      ) {
        setCreateForbidden(true);
        setCreateError(message(EXPLORER_MSG.PERMISSION_DENIED));
        return;
      }
      setCreateError(
        formatApiError(err, message(EXPLORER_MSG.TRANSLATIONS_CREATE_ERROR)),
      );
    } finally {
      setCreating(false);
    }
  }, [itemId, selectedLocales, createVariants, onCreated]);

  const regionLabel = ariaLabel ?? message(EXPLORER_MSG.TRANSLATIONS_TITLE);

  if (state.kind === "loading") {
    return (
      <section
        role="region"
        aria-label={regionLabel}
        data-testid="translations-panel"
        data-testid-state="loading"
        className={className}
        style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
      >
        <p aria-live="polite">{message(EXPLORER_MSG.TRANSLATIONS_LOADING)}</p>
      </section>
    );
  }

  if (state.kind === "auth") {
    return (
      <section
        role="region"
        aria-label={regionLabel}
        data-testid="translations-panel"
        data-testid-state="auth"
        className={className}
        style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
      >
        <p role="status">{message(EXPLORER_MSG.PERMISSION_DENIED)}</p>
      </section>
    );
  }

  if (state.kind === "error") {
    return (
      <section
        role="region"
        aria-label={regionLabel}
        data-testid="translations-panel"
        data-testid-state="error"
        className={className}
        style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
      >
        <p role="alert">{state.message}</p>
      </section>
    );
  }

  return (
    <section
      role="region"
      aria-label={regionLabel}
      data-testid="translations-panel"
      data-testid-state="ok"
      className={className}
      style={{ border: "1px solid #ccc", padding: 12, background: "#fff" }}
    >
      <header style={{ marginBottom: 8 }}>
        <h2
          style={{ margin: 0, fontSize: "1rem" }}
          data-testid="translations-panel-title"
        >
          {message(EXPLORER_MSG.TRANSLATIONS_TITLE)}
          {itemLabel ? (
            <span style={{ fontWeight: 400, marginLeft: 8, color: "#555" }}>
              — {itemLabel}
            </span>
          ) : null}
        </h2>
      </header>

      <div
        data-testid="translations-current-locale"
        style={{ marginBottom: 12 }}
      >
        <strong>{message(EXPLORER_MSG.TRANSLATIONS_CURRENT_LOCALE)}: </strong>
        <span data-testid="translations-current-locale-value">
          {currentLocale && currentLocale.trim()
            ? currentLocale
            : message(EXPLORER_MSG.TRANSLATIONS_LOCALE_UNKNOWN)}
        </span>
      </div>

      <div data-testid="translations-variants" style={{ marginBottom: 16 }}>
        <h3 style={{ margin: "0 0 6px", fontSize: "0.95rem" }}>
          {message(EXPLORER_MSG.TRANSLATIONS_VARIANTS_HEADING)}
        </h3>
        {variants.length === 0 ? (
          <p data-testid="translations-variants-empty" role="status">
            {message(EXPLORER_MSG.TRANSLATIONS_VARIANTS_EMPTY)}
          </p>
        ) : (
          <table
            data-testid="translations-variants-table"
            style={{ width: "100%", borderCollapse: "collapse", fontSize: 13 }}
          >
            <thead>
              <tr>
                <th scope="col" style={{ textAlign: "left", padding: 4 }}>
                  {message(EXPLORER_MSG.TRANSLATIONS_COL_LOCALE)}
                </th>
                <th scope="col" style={{ textAlign: "left", padding: 4 }}>
                  {message(EXPLORER_MSG.TRANSLATIONS_COL_ROLE)}
                </th>
                <th scope="col" style={{ textAlign: "left", padding: 4 }}>
                  {message(EXPLORER_MSG.TRANSLATIONS_COL_CONTENT_ID)}
                </th>
              </tr>
            </thead>
            <tbody>
              {variants.map((v) => (
                <tr
                  key={`${v.contentId}-${v.locale ?? ""}-${v.role ?? ""}`}
                  data-testid={`translations-variant-row-${v.contentId}`}
                >
                  <td style={{ padding: 4 }}>{v.locale ?? "—"}</td>
                  <td style={{ padding: 4 }}>{roleLabel(v.role)}</td>
                  <td style={{ padding: 4 }}>{v.contentId}</td>
                </tr>
              ))}
            </tbody>
          </table>
        )}
      </div>

      <div
        data-testid="translations-create"
        style={{
          borderTop: "1px solid #eee",
          paddingTop: 12,
          marginBottom: 12,
        }}
      >
        <h3 style={{ margin: "0 0 6px", fontSize: "0.95rem" }}>
          {message(EXPLORER_MSG.TRANSLATIONS_CREATE_HEADING)}
        </h3>
        {createForbidden ? (
          <p role="status" data-testid="translations-create-forbidden">
            {message(EXPLORER_MSG.PERMISSION_DENIED)}
          </p>
        ) : targets.length === 0 ? (
          <p data-testid="translations-create-no-targets" role="status">
            {message(EXPLORER_MSG.TRANSLATIONS_NO_TARGET_LOCALES)}
          </p>
        ) : (
          <>
            <fieldset
              data-testid="translations-locale-options"
              style={{ border: "none", margin: 0, padding: 0 }}
            >
              <legend style={{ fontSize: 13, marginBottom: 6 }}>
                {message(EXPLORER_MSG.TRANSLATIONS_TARGET_LOCALES)}
              </legend>
              <ul
                style={{
                  listStyle: "none",
                  margin: 0,
                  padding: 0,
                  display: "flex",
                  flexWrap: "wrap",
                  gap: 8,
                }}
              >
                {targets.map((t) => {
                  const checked = selectedLocales.has(t.languageString);
                  return (
                    <li key={t.languageString}>
                      <label
                        style={{
                          display: "inline-flex",
                          gap: 4,
                          alignItems: "center",
                          fontSize: 13,
                        }}
                      >
                        <input
                          type="checkbox"
                          data-testid={`translations-locale-option-${t.languageString}`}
                          checked={checked}
                          disabled={creating}
                          onChange={(e) =>
                            toggleLocale(t.languageString, e.target.checked)
                          }
                        />
                        <span>
                          {t.label}
                          {t.label !== t.languageString
                            ? ` (${t.languageString})`
                            : ""}
                        </span>
                      </label>
                    </li>
                  );
                })}
              </ul>
            </fieldset>
            <button
              type="button"
              data-testid="translations-create-submit"
              disabled={creating || selectedLocales.size === 0}
              onClick={() => {
                void handleCreate();
              }}
              style={{ marginTop: 10 }}
            >
              {creating
                ? message(EXPLORER_MSG.TRANSLATIONS_CREATING)
                : message(EXPLORER_MSG.TRANSLATIONS_CREATE_ACTION)}
            </button>
          </>
        )}
        {createError ? (
          <p role="alert" data-testid="translations-create-error">
            {createError}
          </p>
        ) : null}
        {createSuccess ? (
          <p role="status" data-testid="translations-create-success">
            {createSuccess}
          </p>
        ) : null}
      </div>

      <p
        data-testid="translations-inflight-note"
        style={{ fontSize: 12, color: "#666", margin: 0 }}
      >
        {message(EXPLORER_MSG.TRANSLATIONS_INFLIGHT_OUT)}
      </p>
    </section>
  );
}

export default TranslationsPanel;
