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
 * template in an iframe with a light overlay. Slot arrange and
 * contenteditable field editing are later slices (996 + 995).
 */

import React, { useEffect, useMemo, useState } from "react";
import { useSearchParams } from "react-router";
import {
  findAllowedTemplateMenus,
  mapActionMenusToMenuActions,
} from "../api/contentExplorer/actionMenuApi";
import {
  fetchPreviewLocation,
  type PreviewLocation,
} from "../api/contentExplorer/assemblyApi";
import type { MenuAction } from "../api/contentExplorer/types";
import { parseTemplateIdFromAction } from "../contentExplorer/actionDispatch";
import { message } from "../i18n/message";
import { parsePositiveInt, withCmsContextPrefix } from "./assemblyHostUrl";
import styles from "./AssemblyHost.module.css";
import { ASSEMBLY_MSG } from "./messages";

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
}: AssemblyHostProps = {}): React.ReactElement {
  const [params] = useSearchParams();
  const contentId = parsePositiveInt(params.get("contentId"));
  const requestedTemplateId = parsePositiveInt(params.get("templateId"));

  const [templates, setTemplates] = useState<AssemblyTemplateOption[]>([]);
  const [templateId, setTemplateId] = useState<number | null>(
    requestedTemplateId,
  );
  const [previewHref, setPreviewHref] = useState<string | null>(null);
  const [errorKey, setErrorKey] = useState<string | null>(
    contentId == null ? ASSEMBLY_MSG.MISSING_ITEM : null,
  );
  const [loading, setLoading] = useState(contentId != null);

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
        const nextId =
          (requestedTemplateId != null &&
          (options.length === 0 ||
            options.some((o) => o.id === requestedTemplateId))
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
        if (requestedTemplateId != null) {
          setTemplateId(requestedTemplateId);
        } else {
          setPreviewHref(null);
          setErrorKey(ASSEMBLY_MSG.NO_TEMPLATE);
          setLoading(false);
        }
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
        ) : selectedLabel || templateId != null ? (
          <span className={styles.meta} data-testid="assembly-template-id">
            <span className={styles.label}>{message(ASSEMBLY_MSG.TEMPLATE_LABEL)}</span>
            {selectedLabel ?? templateId}
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
