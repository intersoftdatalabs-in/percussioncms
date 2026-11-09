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

import React, { useEffect, useRef } from "react";
import { withCmsContextPrefix } from "../../assembly/assemblyHostUrl";
import { message } from "../../i18n/message";
import { EDITOR_MSG } from "../messages";
import { TipTapFieldWidget, type HtmlEditorEngine } from "./TipTapFieldWidget";
import styles from "../EditorHost.module.css";

export const TINYMCE_SCRIPT_PATH =
  "/sys_resources/tinymce/js/tinymce/tinymce.min.js";

interface TinyMceEditor {
  getContent(): string;
  setContent(html: string): void;
  remove(): void;
  setMode(mode: string): void;
  on(event: string, handler: () => void): void;
}

interface TinyMceStatic {
  init(opts: Record<string, unknown>): Promise<TinyMceEditor[]> | void;
  get(id: string): TinyMceEditor | undefined;
}

declare global {
  interface Window {
    tinymce?: TinyMceStatic;
  }
}

export type { HtmlEditorEngine } from "./TipTapFieldWidget";

export interface HtmlFieldWidgetProps {
  name: string;
  value: string;
  readOnly: boolean;
  invalid?: boolean;
  onChange: (value: string) => void;
  loadScript?: (src: string) => Promise<void>;
  /**
   * Engine seam for #5622/#5758. {@code "tiptap"} (default) renders the
   * bundled TipTap editor with the parity toolbar (#5651); {@code "tinymce"}
   * keeps the legacy script-loaded editor as a one-release escape hatch
   * ({@code ?engine=tinymce}).
   */
  engine?: HtmlEditorEngine;
}

let tinymceLoad: Promise<void> | null = null;

function watchExistingTinyMceScriptTag(
  existing: Element,
  resolve: () => void,
  reject: (reason: Error) => void,
): void {
  existing.addEventListener("load", () => resolve());
  existing.addEventListener("error", () => reject(new Error("tinymce")));
}

function injectTinyMceScriptTag(
  src: string,
  resolve: () => void,
  reject: (reason: Error) => void,
): void {
  const script = document.createElement("script");
  script.src = src;
  script.async = true;
  script.onload = () => resolve();
  script.onerror = () => reject(new Error("tinymce"));
  document.head.appendChild(script);
}

export function defaultLoadTinyMceScript(src: string): Promise<void> {
  if (typeof window === "undefined") {
    return Promise.resolve();
  }
  if (window.tinymce) {
    return Promise.resolve();
  }
  if (tinymceLoad) {
    return tinymceLoad;
  }
  tinymceLoad = new Promise((resolve, reject) => {
    const existing = document.querySelector(`script[src="${src}"]`);
    if (existing) {
      watchExistingTinyMceScriptTag(existing, resolve, reject);
      return;
    }
    injectTinyMceScriptTag(src, resolve, reject);
  });
  return tinymceLoad;
}

const TINYMCE_PLUGINS = "lists link code";
const TINYMCE_TOOLBAR =
  "undo redo | bold italic underline | bullist numlist | link | code";
const TINYMCE_HEIGHT = 280;

function buildTinyMceInitOptions(
  target: HTMLTextAreaElement,
  onEditor: (ed: TinyMceEditor) => void,
  onContentChange: (html: string) => void,
): Record<string, unknown> {
  return {
    target,
    menubar: false,
    branding: false,
    statusbar: false,
    height: TINYMCE_HEIGHT,
    plugins: TINYMCE_PLUGINS,
    toolbar: TINYMCE_TOOLBAR,
    convert_urls: false,
    setup: (ed: TinyMceEditor) => {
      onEditor(ed);
      ed.on("change", () => {
        onContentChange(ed.getContent());
      });
    },
  };
}

function isThenable(value: unknown): value is PromiseLike<unknown> {
  return (
    typeof value === "object" &&
    value !== null &&
    typeof (value as { then?: unknown }).then === "function"
  );
}

interface TinyMceAttachHooks {
  isCancelled: () => boolean;
  onEditor: (ed: TinyMceEditor) => void;
  onContentChange: (html: string) => void;
}

async function attachTinyMceEditor(
  areaRef: React.RefObject<HTMLTextAreaElement | null>,
  loadScript: (src: string) => Promise<void>,
  hooks: TinyMceAttachHooks,
): Promise<void> {
  const src = withCmsContextPrefix(TINYMCE_SCRIPT_PATH);
  try {
    await loadScript(src);
  } catch {
    /* textarea fallback */
    return;
  }
  const area = areaRef.current;
  const tinymce = window.tinymce;
  if (hooks.isCancelled() || !tinymce || !area) {
    return;
  }
  const initResult = tinymce.init(
    buildTinyMceInitOptions(area, hooks.onEditor, hooks.onContentChange),
  );
  if (isThenable(initResult)) {
    await initResult;
  }
}

function useTinyMceEditor(
  areaRef: React.RefObject<HTMLTextAreaElement | null>,
  options: {
    readOnly: boolean;
    name: string;
    loadScript: (src: string) => Promise<void>;
    onContentChange: (html: string) => void;
  },
): React.RefObject<TinyMceEditor | null> {
  const { readOnly, name, loadScript, onContentChange } = options;
  const editorRef = useRef<TinyMceEditor | null>(null);
  const onChangeRef = useRef(onContentChange);
  onChangeRef.current = onContentChange;

  useEffect(() => {
    if (!areaRef.current || readOnly) {
      return;
    }
    let cancelled = false;
    void attachTinyMceEditor(areaRef, loadScript, {
      isCancelled: () => cancelled,
      onEditor: (ed: TinyMceEditor) => {
        editorRef.current = ed;
      },
      onContentChange: (html: string) => {
        onChangeRef.current(html);
      },
    });
    return () => {
      cancelled = true;
      editorRef.current?.remove();
      editorRef.current = null;
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [loadScript, readOnly, name]);

  return editorRef;
}

function useTinyMceValueSync(
  editorRef: React.RefObject<TinyMceEditor | null>,
  value: string,
): void {
  useEffect(() => {
    const ed = editorRef.current;
    if (ed && ed.getContent() !== value) {
      ed.setContent(value);
    }
  }, [editorRef, value]);
}

export function HtmlFieldWidget({
  name,
  value,
  readOnly,
  invalid = false,
  onChange,
  loadScript = defaultLoadTinyMceScript,
  engine = "tiptap",
}: HtmlFieldWidgetProps): React.ReactElement {
  if (engine === "tiptap") {
    return (
      <TipTapFieldWidget
        name={name}
        value={value}
        readOnly={readOnly}
        invalid={invalid}
        onChange={onChange}
      />
    );
  }
  return (
    <TinyMceFieldWidget
      name={name}
      value={value}
      readOnly={readOnly}
      invalid={invalid}
      onChange={onChange}
      loadScript={loadScript}
    />
  );
}

function TinyMceFieldWidget({
  name,
  value,
  readOnly,
  invalid = false,
  onChange,
  loadScript,
}: {
  name: string;
  value: string;
  readOnly: boolean;
  invalid?: boolean;
  onChange: (value: string) => void;
  loadScript: (src: string) => Promise<void>;
}): React.ReactElement {
  const areaRef = useRef<HTMLTextAreaElement | null>(null);
  const editorRef = useTinyMceEditor(areaRef, {
    readOnly,
    name,
    loadScript,
    onContentChange: onChange,
  });
  useTinyMceValueSync(editorRef, value);

  return (
    <textarea
      ref={areaRef}
      className={`${styles.textarea} ${styles.html} ${readOnly ? styles.readonly : ""}`}
      data-testid={`editor-field-${name}`}
      data-editor-kind="html"
      name={name}
      value={value}
      readOnly={readOnly}
      aria-invalid={invalid ? true : undefined}
      aria-label={message(EDITOR_MSG.HTML_LABEL)}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}
