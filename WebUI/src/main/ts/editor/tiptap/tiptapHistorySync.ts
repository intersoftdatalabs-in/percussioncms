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
 * History-silent content sync for TipTap rich-text segments (#6262 / #4532).
 *
 * <p>TipTap v3 {@code commands.setContent} dispatches
 * {@code tr.replaceWith(...)} with no {@code addToHistory} meta, and
 * {@code prosemirror-history} records every doc-changing transaction unless
 * the meta is {@code false}. A plain programmatic sync therefore did two bad
 * things at once: it pushed an undo step for the sync itself, and it left the
 * pre-sync user steps on the stack with a base document that no longer
 * matches. After revert-all (#5889), compare-restore (#6080),
 * autosave-restore (#6076), save-reload (#5768), or retry-load, Ctrl+Z /
 * toolbar Undo resurrected discarded edits into the widget, desyncing it from
 * the EditorHost draft — the next keystroke or save then persisted edits the
 * author had explicitly discarded.</p>
 *
 * <p>This helper replays the same parse path as {@code setContent} (TipTap
 * {@code createDocument}, default parse options,
 * {@code enableContentCheck}) but installs the parsed document as a fresh
 * editor state: no {@code onUpdate} echo back through {@code onChange}
 * (state is replaced, not dispatched) and an empty undo/redo stack, so the
 * sync becomes the new undo baseline. User edits keep flowing through the
 * {@code UndoRedo} extension (toolbar buttons, Mod-z / Shift-Mod-z / Mod-y),
 * covering typing since the last sync only. The caret is preserved
 * best-effort (clamped into the new document).</p>
 */

import { createDocument, type Editor } from "@tiptap/core";
import { EditorState, TextSelection } from "@tiptap/pm/state";

/**
 * Replace the whole document without echoing through {@code onChange} and
 * without keeping any undo/redo history, mirroring
 * {@code setContent(html, { emitUpdate: false })} plus a history reset.
 *
 * @param editor mounted TipTap editor to sync.
 * @param html stored HTML to show (prepared Percussion markup).
 */
export function setTipTapContentWithoutHistory(
  editor: Editor,
  html: string,
): void {
  const { schema, plugins, selection } = editor.state;
  // createDocument always returns a full document node (same parse path as
  // commands.setContent with default parse options).
  const doc = createDocument(html ?? "", schema, undefined, {
    errorOnInvalidContent: editor.options.enableContentCheck,
  });
  const anchor = Math.min(selection.from, doc.content.size);
  const head = Math.min(selection.to, doc.content.size);
  const next = EditorState.create({
    schema,
    doc,
    plugins,
    selection: TextSelection.create(doc, anchor, head),
  });
  editor.view.updateState(next);
}
