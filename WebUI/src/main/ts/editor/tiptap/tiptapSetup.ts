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
 * Shared TipTap extension set for the #5622 spike-validation slice (#5650).
 *
 * <p>MIT-licensed TipTap 3 / ProseMirror extensions only. This set exists to
 * prove the Spike criterion: TipTap parses the HTML the CMS already persists
 * (TinyMCE 6.8.6 output) and re-serializes it without content drift. It is
 * not the parity toolbar (slice #5651) and not the Percussion plugin rewrite
 * (slice #5652): no toolbar chrome and no custom plugins live here.</p>
 */

import type { AnyExtension } from "@tiptap/core";
import { Image } from "@tiptap/extension-image";
import { Link } from "@tiptap/extension-link";
import { Table } from "@tiptap/extension-table";
import { TableCell } from "@tiptap/extension-table-cell";
import { TableHeader } from "@tiptap/extension-table-header";
import { TableRow } from "@tiptap/extension-table-row";
import { TextAlign } from "@tiptap/extension-text-align";
import { Underline } from "@tiptap/extension-underline";
import { StarterKit } from "@tiptap/starter-kit";

/**
 * Build the spike-validation extension set.
 *
 * <p>StarterKit covers paragraphs, headings, bold/italic/strike, lists,
 * blockquote, code, hard breaks, and rules. The extra marks/extensions cover
 * the remaining TinyMCE toolbar/config output the React host persists today:
 * underline (current toolbar), links (current toolbar/plugins), images,
 * tables, and paragraph/heading alignment ({@code rx_default_config.json}
 * align buttons).</p>
 *
 * @returns a fresh extension list per call (extensions hold config state).
 */
export function createSpikeEditorExtensions(): AnyExtension[] {
  return [
    StarterKit,
    Underline,
    Link.configure({
      openOnClick: false,
      autolink: true,
      // Null out the extension's default target/rel (options deep-merge, so
      // omitting them would keep _blank/noopener): the spike must not
      // silently mutate stored links on save. Explicit stored target/rel
      // still round-trips through the mark attributes.
      HTMLAttributes: { target: null, rel: null },
    }),
    // Inline images match TinyMCE's stored form (images inside paragraphs);
    // block images would otherwise split paragraphs on parse.
    Image.configure({ inline: true }),
    Table.configure({ resizable: false }),
    TableRow,
    TableHeader,
    TableCell,
    TextAlign.configure({ types: ["heading", "paragraph"] }),
  ];
}
