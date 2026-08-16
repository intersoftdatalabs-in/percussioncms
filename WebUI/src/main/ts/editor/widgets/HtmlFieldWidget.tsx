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

export interface HtmlFieldWidgetProps {
  name: string;
  value: string;
  readOnly: boolean;
  onChange: (value: string) => void;
  loadScript?: (src: string) => Promise<void>;
}

let tinymceLoad: Promise<void> | null = null;

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
      existing.addEventListener("load", () => resolve());
      existing.addEventListener("error", () => reject(new Error("tinymce")));
      return;
    }
    const script = document.createElement("script");
    script.src = src;
    script.async = true;
    script.onload = () => resolve();
    script.onerror = () => reject(new Error("tinymce"));
    document.head.appendChild(script);
  });
  return tinymceLoad;
}

export function HtmlFieldWidget({
  name,
  value,
  readOnly,
  onChange,
  loadScript = defaultLoadTinyMceScript,
}: HtmlFieldWidgetProps): React.ReactElement {
  const areaRef = useRef<HTMLTextAreaElement | null>(null);
  const editorRef = useRef<TinyMceEditor | null>(null);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;

  useEffect(() => {
    const area = areaRef.current;
    if (!area || readOnly) {
      return;
    }
    let cancelled = false;
    const src = withCmsContextPrefix(TINYMCE_SCRIPT_PATH);
    void loadScript(src)
      .then(async () => {
        const tinymce = window.tinymce;
        if (cancelled || !tinymce || !areaRef.current) {
          return;
        }
        const result = tinymce.init({
          target: areaRef.current,
          menubar: false,
          branding: false,
          statusbar: false,
          height: 280,
          plugins: "lists link code",
          toolbar: "undo redo | bold italic underline | bullist numlist | link | code",
          convert_urls: false,
          setup: (ed: TinyMceEditor) => {
            editorRef.current = ed;
            ed.on("change", () => {
              onChangeRef.current(ed.getContent());
            });
          },
        });
        if (result && typeof (result as Promise<unknown>).then === "function") {
          await result;
        }
      })
      .catch(() => {
        /* textarea fallback */
      });
    return () => {
      cancelled = true;
      editorRef.current?.remove();
      editorRef.current = null;
    };
  }, [loadScript, readOnly, name]);

  useEffect(() => {
    const ed = editorRef.current;
    if (ed && ed.getContent() !== value) {
      ed.setContent(value);
    }
  }, [value]);

  return (
    <textarea
      ref={areaRef}
      className={`${styles.textarea} ${styles.html} ${readOnly ? styles.readonly : ""}`}
      data-testid={`editor-field-${name}`}
      data-editor-kind="html"
      name={name}
      value={value}
      readOnly={readOnly}
      aria-label={message(EDITOR_MSG.HTML_LABEL)}
      onChange={(e) => onChange(e.target.value)}
    />
  );
}
