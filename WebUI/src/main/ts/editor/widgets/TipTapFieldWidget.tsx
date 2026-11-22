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
 * TipTap 3 / ProseMirror host for HTML fields (#5622 slices #5650/#5651, plugins #5652).
 *
 * <p>Toolbar-parity scope: a {@code rx_default_config} toolbar (lists,
 * link, code, tables, plus undo/redo, inline styles, alignment) over the
 * shared extension set, reporting HTML strings through the same
 * {@code onChange} contract as the TinyMCE widget, so the existing
 * {@code PUT /services/itemmanagement/item/fields/{id}} persist path is
 * reused unchanged. Pasted Word/Office HTML is reduced to the
 * {@code paste_word_valid_elements} contract before parsing. The Percussion
 * plugin ports ({@code TipTapPercussionBar}: inline link/image/template,
 * insert HTML, global variables, more link, source view) serialize to the
 * same stored markup the TinyMCE plugins persist.</p>
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
import React, { useCallback, useEffect, useMemo, useRef, useState } from "react";
import { message } from "../../i18n/message";
import { EDITOR_MSG } from "../messages";
import { preparePercussionHtml } from "../tiptap/tiptapPercussion";
import { roundTripHtmlFragment } from "../tiptap/tiptapRoundTrip";
import { createTipTapEditorExtensions } from "../tiptap/tiptapSetup";
import { setTipTapContentWithoutHistory } from "../tiptap/tiptapHistorySync";
import { sanitizeWordPastedHtml } from "../tiptap/tiptapWordPaste";
import { TipTapPercussionBar } from "../tiptap/TipTapPercussionBar";
import { TipTapToolbar } from "../tiptap/TipTapToolbar";
import styles from "../EditorHost.module.css";

export interface TipTapFieldWidgetProps {
  name: string;
  value: string;
  readOnly: boolean;
  invalid?: boolean;
  /**
   * DOM id of the row's inline error text (#6933). Set only while the row
   * carries an error so {@code aria-describedby} never dangles.
   */
  describedById?: string;
  onChange: (value: string) => void;
}

export function TipTapFieldWidget({
  name,
  value,
  readOnly,
  invalid = false,
  describedById,
  onChange,
}: TipTapFieldWidgetProps): React.ReactElement {
  // #7046 (slice 1/3): read-only rows render a static schema-normalized
  // view instead of mounting an interactive ProseMirror instance. View mode
  // (and checked-out-to-other / schema-readonly rows) can never edit, so a
  // live editor there only costs lifecycle work and a focusable widget that
  // swallows arrow keys. Splitting keeps every hook call unconditional
  // inside its own component; flipping readOnly unmounts one side and mounts
  // the other (the editor destroy runs in the editable unmount cleanup).
  if (readOnly) {
    return (
      <ReadOnlyTipTapView
        name={name}
        value={value}
        invalid={invalid}
        describedById={describedById}
      />
    );
  }
  return (
    <EditableTipTapField
      name={name}
      value={value}
      invalid={invalid}
      describedById={describedById}
      onChange={onChange}
    />
  );
}

/**
 * Static read-only render of a stored HTML value (#7046).
 *
 * <p>Parity by construction: the markup is exactly what the editing view
 * serializes for the same stored value ({@code preparePercussionHtml} plus
 * the shared extension set via {@code roundTripHtmlFragment}), so View mode
 * shows what Edit mode edits — TinyMCE-era aliases, percussion markers,
 * lists, and tables included. ProseMirror drops refused markup
 * (script/event, NUL-adjacent) at parse time, so this render is no more
 * permissive than the editor surface it mirrors.</p>
 *
 * <p>The full row contract is preserved: the same {@code editor-field-}
 * testid, {@code data-editor-kind/engine} markers, editor classes,
 * {@code contenteditable="false"}, {@code readonly}, and aria wiring — only
 * the live ProseMirror view (and its key handling) is gone, announced via
 * {@code role="textbox"} + {@code aria-readonly} so screen readers hear the
 * same read-only text control as before.</p>
 */
function ReadOnlyTipTapView({
  name,
  value,
  invalid = false,
  describedById,
}: {
  name: string;
  value: string;
  invalid?: boolean;
  describedById?: string;
}): React.ReactElement {
  const html = useMemo(() => roundTripHtmlFragment(value ?? ""), [value]);
  const staticRef = useRef<HTMLDivElement | null>(null);
  // `readonly` is not a typed div prop; the editor host contract predates
  // this view (`readonly=""` asserted by editor-host-html-tiptap.spec.js),
  // so it is set imperatively like the editable sync effect does.
  useEffect(() => {
    staticRef.current?.setAttribute("readonly", "");
  }, []);
  return (
    <div
      data-testid={`editor-tiptap-${name}`}
      className={`${styles.readonly} ${styles.tiptapHost}`}
      // Same label-click guard as the editable host: the row renders inside
      // an implicit <label> and a plain div is not labelable.
      onClick={(e) => e.preventDefault()}
    >
      <div
        ref={staticRef}
        data-testid={`editor-field-${name}`}
        data-editor-kind="html"
        data-editor-engine="tiptap"
        className={`${styles.textarea} ${styles.tiptap} tiptap-editor`}
        contentEditable={false}
        role="textbox"
        aria-readonly="true"
        aria-multiline="true"
        aria-label={message(EDITOR_MSG.HTML_LABEL)}
        aria-invalid={invalid ? "true" : undefined}
        aria-describedby={describedById}
        dangerouslySetInnerHTML={{ __html: html }}
      />
    </div>
  );
}

function EditableTipTapField({
  name,
  value,
  invalid = false,
  describedById,
  onChange,
}: {
  name: string;
  value: string;
  invalid?: boolean;
  describedById?: string;
  onChange: (value: string) => void;
}): React.ReactElement {
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

  useEffect(() => {
    const mount = mountRef.current;
    if (!mount) {
      return undefined;
    }
    const editor = new Editor({
      element: mount,
      extensions: createTipTapEditorExtensions(),
      // Stored Percussion marker shorthands are prepared before parsing,
      // mirroring the TinyMCE plugins' BeforeSetContent handlers.
      content: preparePercussionHtml(initialRef.current ?? ""),
      // This component only mounts for editable rows (read-only rows render
      // ReadOnlyTipTapView); the editable flag never flips at runtime.
      editable: true,
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

  // Stable identity: the sync effect must re-run only when `value` changes.
  // An inline closure would re-run the effect on every render, and content
  // that never round-trips through the editor (stored NUL #5696, refused
  // script markup) would sync -> tick -> render -> sync forever.
  // Stable identity: the sync effect must re-run only when `value` changes.
  // An inline closure would re-run the effect on every render, and content
  // that never round-trips through the editor (stored NUL #5696, refused
  // script markup) would sync -> tick -> render -> sync forever.
  const bumpToolbarTick = useCallback(() => {
    setToolbarTick((tick) => tick + 1);
  }, []);
  useSyncTipTapEditor(
    editorRef,
    { value, invalid, describedById },
    bumpToolbarTick,
  );

  return (
    <div
      data-testid={`editor-tiptap-${name}`}
      className={styles.tiptapHost}
      // The field row renders inside an implicit <label> (EditorHost). A div
      // is not labelable, so the label forwards editor clicks to the first
      // labelable control in the row — the Clear button (#5650: clicking the
      // surface wiped the field). Cancelling the click default stops the
      // forwarding; ProseMirror selection/focus runs on mousedown and is
      // unaffected. The textarea widget never hit this: it is labelable and
      // first, so forwarding focuses it harmlessly.
      onClick={(e) => e.preventDefault()}
    >
      {toolbarEditor ? (
        <TipTapToolbar editor={toolbarEditor} name={name} />
      ) : null}
      {toolbarEditor ? (
        <TipTapPercussionBar editor={toolbarEditor} name={name} />
      ) : null}
      <div ref={mountRef} />
    </div>
  );
}

/**
 * Mirror controlled props into a mounted parity editor. External syncs stay
 * silent (no onUpdate echo, so external saves never loop back through
 * {@code onChange}) and history-free (#6262: a programmatic setContent used
 * to push an undo step, so Ctrl+Z after revert/restore resurrected discarded
 * edits). Read-only rows never mount here (see ReadOnlyTipTapView), so the
 * editable/aria flags below are the always-editable path.
 */
function useSyncTipTapEditor(
  editorRef: React.RefObject<Editor | null>,
  state: {
    value: string;
    invalid: boolean;
    describedById?: string;
  },
  onSync?: () => void,
): void {
  const { value, invalid, describedById } = state;

  useEffect(() => {
    const editor = editorRef.current;
    const prepared = preparePercussionHtml(value);
    if (editor && editor.getHTML() !== prepared) {
      // Silent and history-free by contract (#6262): external saves install
      // a fresh editor state (no onUpdate echo — the option defaults to noisy
      // in TipTap v3 setContent, which also has no addToHistory option), and
      // the undo/redo stacks reset so Ctrl+Z after revert/restore cannot
      // resurrect discarded edits. A stored NUL stays in the row value so
      // the save-side HTML gate (#5696) can refuse it instead of being
      // normalized away here.
      setTipTapContentWithoutHistory(editor, prepared);
      // The state swap bypasses dispatch, so the toolbar bump listener
      // misses it: refresh the disabled state explicitly.
      onSync?.();
    }
  }, [editorRef, onSync, value]);

  useEffect(() => {
    const dom = editorRef.current?.view.dom;
    if (!dom) {
      return;
    }
    // Always-editable path (read-only rows render ReadOnlyTipTapView and
    // carry their own `readonly` marker): keep the stale-attribute cleanup
    // so a remount never inherits a readonly flag from a recycled node.
    dom.removeAttribute("readonly");
    if (invalid) {
      dom.setAttribute("aria-invalid", "true");
    } else {
      dom.removeAttribute("aria-invalid");
    }
    if (describedById) {
      dom.setAttribute("aria-describedby", describedById);
    } else {
      dom.removeAttribute("aria-describedby");
    }
  }, [editorRef, invalid, describedById]);
}
