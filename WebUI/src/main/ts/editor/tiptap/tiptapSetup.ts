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
 * Shared TipTap extension set for #5622 (spike #5650, parity #5651, Percussion
 * plugins #5652).
 *
 * <p>MIT-licensed TipTap 3 / ProseMirror extensions only. This set backs the
 * React editor host toolbar at {@code rx_default_config.json} parity (lists,
 * link, code, tables, plus the underline/align/image output the CMS already
 * persists), the Percussion plugin ports (CMS inline-link/image/template
 * attributes, the global-variable marker, the blog more-link marker), and
 * parsing already-stored TinyMCE 6.8.6 HTML without content drift (see
 * {@code tiptapRoundTrip}).</p>
 *
 * <p>TipTap 3 ships named exports only (no default exports); the imports
 * below use the v3 form.</p>
 */

import type { AnyExtension } from "@tiptap/core";
import { Table } from "@tiptap/extension-table";
import { TableCell } from "@tiptap/extension-table-cell";
import { TableHeader } from "@tiptap/extension-table-header";
import { TableRow } from "@tiptap/extension-table-row";
import { TextAlign } from "@tiptap/extension-text-align";
import { Underline } from "@tiptap/extension-underline";
import { StarterKit } from "@tiptap/starter-kit";
import {
  PercGlobalVariable,
  PercImage,
  PercLink,
  PercMoreLink,
} from "./tiptapPercussionNodes";

/**
 * Build the toolbar-parity extension set.
 *
 * <p>StarterKit covers paragraphs, headings, bold/italic/strike, lists,
 * blockquote, code, hard breaks, rules, and history (undo/redo). The extra
 * marks/extensions cover the remaining TinyMCE toolbar/config output the
 * React host persists today: underline (current toolbar), links (current
 * toolbar/plugins, plus CMS inline attributes), images (plus CMS inline
 * attributes), tables, paragraph/heading alignment
 * ({@code rx_default_config.json} align buttons), the global-variable marker,
 * and the blog more-link marker (plugin ports, #5652).</p>
 *
 * @returns a fresh extension list per call (extensions hold config state).
 */
export function createTipTapEditorExtensions(): AnyExtension[] {
  return [
    StarterKit,
    Underline,
    PercLink.configure({
      openOnClick: false,
      autolink: true,
      // Null out the extension's default target/rel (options deep-merge, so
      // omitting them would keep _blank/noopener): the editor must not
      // silently mutate stored links on save. Explicit stored target/rel
      // still round-trips through the mark attributes.
      HTMLAttributes: { target: null, rel: null },
    }),
    // Inline images match TinyMCE's stored form (images inside paragraphs);
    // block images would otherwise split paragraphs on parse.
    PercImage.configure({ inline: true }),
    Table.configure({ resizable: false }),
    TableRow,
    TableHeader,
    TableCell,
    TextAlign.configure({ types: ["heading", "paragraph"] }),
    // Percussion plugin ports (#5652): marker nodes with no parity-slice
    // equivalent. They serialize to the same stored markup the TinyMCE
    // plugins persist.
    PercGlobalVariable,
    PercMoreLink,
  ];
}
