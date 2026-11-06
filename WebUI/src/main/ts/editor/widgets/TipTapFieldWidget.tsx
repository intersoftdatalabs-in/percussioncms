/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * TipTap 3 / ProseMirror host for HTML fields (#5622 slices #5650/#5651).
 *
 * <p>Toolbar-parity scope: a {@code rx_default_config.json} toolbar (lists,
 * link, code, tables, plus undo/redo, inline styles, alignment) over the
 * shared extension set, reporting HTML strings through the same
 * {@code onChange} contract as the TinyMCE widget, so the existing
 * {@code PUT /services/itemmanagement/item/fields/{id}} persist path is
 * reused unchanged. Pasted Word/Office HTML is reduced to the
 * {@code paste_word_valid_elements} contract before parsing. The Percussion
 * plugin rewrite is slice #5652 — no custom plugins live here.</p>
 *
 * <p>Implementation note: the editor is managed directly through
 * {@code @tiptap/core} ({@code Editor} lifecycle in an effect) rather than
 * {@code @tiptap/react}. The repository Vitest harness resolves a second
 * React instance for the {@code @tiptap/react} hook chain, which breaks
 * {@code useEditor} under test while the production bundle (single React)
 * is unaffected. Driving {@code Editor} directly keeps the parity slice
 * testable in this harness; revisit when the plugin rewrite lands.</p>
 */

import { Editor } from "@tiptap/core";
import React, { useEffect, useRef, useState } from "react";
import { message } from "../../i18n/message";
import { EDITOR_MSG } from "../messages";
import { createTipTapEditorExtensions } from "../tiptap/tiptapSetup";
import { sanitizeWordPastedHtml } from "../tiptap/tiptapWordPaste";
import { TipTapToolbar } from "../tiptap/TipTapToolbar";
import styles from "../EditorHost.module.css";

/** Which rich-text engine backs an HTML field. TinyMCE stays the default. */
export type HtmlEditorEngine = "tinymce" | "tiptap";

export interface TipTapFieldWidgetProps {
  name: string;
  value: string;
  readOnly: boolean;
  invalid?: boolean;
  onChange: (value: string) => void;
}

export function TipTapFieldWidget({
  name,
  value,
  readOnly,
  invalid = false,
  onChange,
}: TipTapFieldWidgetProps): React.ReactElement {
  const mountRef = useRef<HTMLDivElement | null>(null);
  const editorRef = useRef<Editor | null>(null);
  const [toolbarEditor, setToolbarEditor] = useState<Editor | null>(null);
  // Toolbar active/disabled states read the editor on render; bump after
  // every transaction so buttons track selection and history depth.
  const [, setToolbarTick] = useState(0);
  const onChangeRef = useRef(onChange);
  onChangeRef.current = onChange;
  const initialRef = useRef<string | null>(null);
  if (initialRef.current === null) {
    initialRef.current = value;
  }
  const readOnlyRef = useRef(readOnly);
  readOnlyRef.current = readOnly;

  useEffect(() => {
    const mount = mountRef.current;
    if (!mount) {
      return undefined;
    }
    const editor = new Editor({
      element: mount,
      extensions: createTipTapEditorExtensions(),
      content: initialRef.current ?? "",
      editable: !readOnlyRef.current,
      onUpdate: ({ editor: updated }) => {
        onChangeRef.current(updated.getHTML());
      },
      editorProps: {
        // Word/Office pastes reduce to the paste_word_valid_elements
        // contract before parsing; ordinary HTML passes through unchanged.
        transformPastedHTML: (html) => sanitizeWordPastedHtml(html),
        attributes: {
          class: `${styles.textarea} ${styles.tiptap} tiptap-editor`,
          "data-testid": `editor-field-${name}`,
          "data-editor-kind": "html",
          "data-editor-engine": "tiptap",
          "aria-label": message(EDITOR_MSG.HTML_LABEL),
        },
      },
    });
    editorRef.current = editor;
    setToolbarEditor(editor);
    const bumpToolbar = (): void => {
      setToolbarTick((tick) => tick + 1);
    };
    editor.on("transaction", bumpToolbar);
    return () => {
      editor.off("transaction", bumpToolbar);
      editor.destroy();
      editorRef.current = null;
      setToolbarEditor(null);
    };
  }, [name]);

  useSyncTipTapEditor(editorRef, { value, readOnly, invalid });

  return (
    <div
      data-testid={`editor-tiptap-${name}`}
      className={`${readOnly ? styles.readonly : ""} ${styles.tiptapHost}`}
      // The field row renders inside an implicit <label> (EditorHost). A div
      // is not labelable, so the label forwards editor clicks to the first
      // labelable control in the row — the Clear button (#5650: clicking the
      // surface wiped the field). Cancelling the click default stops the
      // forwarding; ProseMirror selection/focus runs on mousedown and is
      // unaffected. The textarea widget never hit this: it is labelable and
      // first, so forwarding focuses it harmlessly.
      onClick={(e) => e.preventDefault()}
    >
      {!readOnly && toolbarEditor ? (
        <TipTapToolbar editor={toolbarEditor} name={name} />
      ) : null}
      <div ref={mountRef} />
    </div>
  );
}

/**
 * Mirror controlled props into a mounted parity editor. `setContent` stays
 * silent (no onUpdate echo), so external saves never loop back through
 * {@code onChange}; the editable/aria flags track their props directly.
 */
function useSyncTipTapEditor(
  editorRef: React.RefObject<Editor | null>,
  state: { value: string; readOnly: boolean; invalid: boolean },
): void {
  const { value, readOnly, invalid } = state;

  useEffect(() => {
    editorRef.current?.setEditable(!readOnly);
  }, [editorRef, readOnly]);

  useEffect(() => {
    const editor = editorRef.current;
    if (editor && editor.getHTML() !== value) {
      editor.commands.setContent(value);
    }
  }, [editorRef, value]);

  useEffect(() => {
    const dom = editorRef.current?.view.dom;
    if (!dom) {
      return;
    }
    if (readOnly) {
      dom.setAttribute("readonly", "");
    } else {
      dom.removeAttribute("readonly");
    }
    if (invalid) {
      dom.setAttribute("aria-invalid", "true");
    } else {
      dom.removeAttribute("aria-invalid");
    }
  }, [editorRef, readOnly, invalid]);
}
