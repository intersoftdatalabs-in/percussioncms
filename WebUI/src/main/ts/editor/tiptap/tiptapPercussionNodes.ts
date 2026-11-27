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
 * TipTap ports of the first-party TinyMCE plugins (#5622 slice #5652).
 *
 * <p>{@code PercLink} and {@code PercImage} extend the parity-slice
 * {@code Link}/{@code Image} marks with the CMS inline attributes
 * {@code percadvlink}, {@code percadvimage}, and {@code rxinline} persist
 * ({@code rxinlineslot}, {@code inlinetype}, {@code sys_dependentid},
 * {@code sys_dependentvariantid}, {@code rxselectedtext},
 * {@code data-jcrpath}, plus link {@code title} and image
 * {@code width}/{@code height}). Ordinary links and images serialize exactly
 * as before — CMS attributes render only when set — so the parity corpus
 * stays zero-drift and already-stored inline content round-trips.</p>
 *
 * <p>{@code PercGlobalVariable} is the {@code percglobalvariables} marker:
 * stored as {@code <span class="perc-global-variable-marker" title="NAME">}
 * (the save-time form), parsed from the stored span and from the legacy
 * TinyMCE edit-time {@code <img>} marker. {@code PercMoreLink} is the
 * {@code percmorelink} marker: stored as an empty
 * {@code <span class="perc-blog-more-link">} (self-closed on disk; see
 * {@code preparePercussionHtml}), parsed from the span and the legacy edit
 * {@code <img>}, shown in the editor as an uneditable "More" badge through a
 * node view while serializing back to the bare stored span.</p>
 *
 * <p>{@code PercInlineVariant} and {@code PercBlockVariant} are the stored
 * template-variant wrappers ({@code span.rx_inlinevariant} /
 * {@code div.rx_ephox_inlinevariant}): unlike the classic edit-time span the
 * retired {@code editorinline.js} consumed, the stored wrappers carry the
 * inline-template relationship ({@code rxinlineslot} /
 * {@code inlinetype="rxvariant"} / {@code sys_dependentid}, moved to the
 * block {@code div} by {@code PSSingleValueBuilder}), so the schema keeps
 * them — with their relationship attributes — instead of flattening them.
 * Dropping the wrapper would drop the relationship and
 * {@code PSInlineLinkField} would delete it on save. Publish-time stripping
 * ({@code PSDivTagCleanup}) is unchanged: it applies to the published
 * output, never to the stored edit-time value.</p>
 *
 * <p>MIT TipTap 3 / ProseMirror only; no classic-frame services.</p>
 */

import { Link } from "@tiptap/extension-link";
import { Image } from "@tiptap/extension-image";
import { Node, mergeAttributes } from "@tiptap/core";
import type { ViewMutationRecord } from "@tiptap/pm/view";
import {
  PERC_BLOCK_VARIANT_CLASS,
  PERC_CONTENT_EDITABLE_ATTR,
  PERC_DEPENDENT_ID_ATTR,
  PERC_DEPENDENT_VARIANT_ATTR,
  PERC_GLOBAL_VARIABLE_CLASS,
  PERC_INLINE_SLOT_ATTR,
  PERC_INLINE_TYPE_ATTR,
  PERC_INLINE_VARIANT_CLASS,
  PERC_JCR_PATH_ATTR,
  PERC_MORE_LINK_CLASS,
  PERC_RELATIONSHIP_ATTR,
  PERC_SELECTED_TEXT_ATTR,
  isCsrfTokenOnlyAddressRewrite,
  parseGlobalVariableMarker,
  readGlobalVariableData,
  stripCsrfGuardTokenParam,
} from "./tiptapPercussion";

declare module "@tiptap/core" {
  interface Commands<ReturnType> {
    percGlobalVariable: {
      insertPercGlobalVariable: (name: string, value: string) => ReturnType;
    };
    percMoreLink: {
      insertPercMoreLink: () => ReturnType;
    };
  }
}

/** CMS attribute passthrough: parse when present, omit when empty. */
function percAttribute(name: string): Record<string, unknown> {
  return {
    default: null,
    parseHTML: (el: HTMLElement) => el.getAttribute(name),
    renderHTML: (attrs: Record<string, unknown>) => {
      const value = attrs[name];
      if (typeof value !== "string" || !value) {
        return {};
      }
      return { [name]: value };
    },
  };
}

/**
 * Parity {@code Link} mark plus the CMS inline attributes. Attribute order
 * follows the TinyMCE dialog serialization (slot, type, ids) so stored
 * snapshots compare stable.
 */
export const PercLink = Link.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      // Canonicalize the address on parse (#7360): the live cell's CSRFGuard
      // rewrites relative hrefs in the DOM, and editor state must never
      // absorb the transport token (it would persist a session token into
      // stored content). Rendering stays the parent default (state is
      // token-free).
      href: {
        default: null,
        parseHTML: (element: HTMLElement) =>
          stripCsrfGuardTokenParam(element.getAttribute("href")),
      },
      title: {
        default: null,
        parseHTML: (el: HTMLElement) => el.getAttribute("title"),
        renderHTML: (attrs: Record<string, unknown>) =>
          typeof attrs["title"] === "string" && attrs["title"]
            ? { title: attrs["title"] }
            : {},
      },
      [PERC_INLINE_SLOT_ATTR]: percAttribute(PERC_INLINE_SLOT_ATTR),
      [PERC_INLINE_TYPE_ATTR]: percAttribute(PERC_INLINE_TYPE_ATTR),
      [PERC_DEPENDENT_ID_ATTR]: percAttribute(PERC_DEPENDENT_ID_ATTR),
      [PERC_DEPENDENT_VARIANT_ATTR]: percAttribute(PERC_DEPENDENT_VARIANT_ATTR),
      [PERC_RELATIONSHIP_ATTR]: percAttribute(PERC_RELATIONSHIP_ATTR),
      [PERC_SELECTED_TEXT_ATTR]: percAttribute(PERC_SELECTED_TEXT_ATTR),
      [PERC_JCR_PATH_ATTR]: percAttribute(PERC_JCR_PATH_ATTR),
    };
  },

  /**
   * External DOM-attribute reconciliation guard (#7360).
   *
   * <p>The live cell's CSRFGuard rewrites relative {@code href}s in place on
   * every anchor the editor (re-)renders. ProseMirror's DOM observer treats
   * each rewrite as an external change: the range is marked dirty and the
   * same state re-renders the anchor again, which the guard rewrites again —
   * a multi-kHz node re-creation storm that wedges the renderer (trusted
   * clicks hang; the inline-link dialog Apply times the Playwright suite
   * out) with no console or server errors. The parser already reuses state
   * attributes for described nodes, so reconciling these rewrites can never
   * converge: the only correct response is to ignore them.</p>
   *
   * <p>This mark view renders the anchor exactly like the default mark
   * serializer and ignores only token-only {@code href} rewrites (no
   * canonical address change). Genuine address changes still take the
   * normal reconciliation path.</p>
   */
  addMarkView() {
    return ({ HTMLAttributes }) => {
      const dom = document.createElement("a");
      for (const [key, value] of Object.entries(HTMLAttributes ?? {})) {
        if (value === null || value === undefined || value === false) {
          continue;
        }
        dom.setAttribute(key, String(value));
      }
      return {
        dom,
        contentDOM: dom,
        ignoreMutation: (mutation: ViewMutationRecord) =>
          isCsrfTokenOnlyAddressRewrite(mutation),
      };
    };
  },
});

/** Parity {@code Image} node plus the CMS inline attributes. */
export const PercImage = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
      // Same CSRFGuard canonicalization as the link href (#7360): a
      // tokenized DOM src must parse back to the tokenless state address.
      src: {
        default: null,
        parseHTML: (element: HTMLElement) =>
          stripCsrfGuardTokenParam(element.getAttribute("src")),
      },
      width: {
        default: null,
        parseHTML: (el: HTMLElement) => el.getAttribute("width"),
        renderHTML: (attrs: Record<string, unknown>) =>
          typeof attrs["width"] === "string" && attrs["width"]
            ? { width: attrs["width"] }
            : {},
      },
      height: {
        default: null,
        parseHTML: (el: HTMLElement) => el.getAttribute("height"),
        renderHTML: (attrs: Record<string, unknown>) =>
          typeof attrs["height"] === "string" && attrs["height"]
            ? { height: attrs["height"] }
            : {},
      },
      [PERC_INLINE_SLOT_ATTR]: percAttribute(PERC_INLINE_SLOT_ATTR),
      [PERC_INLINE_TYPE_ATTR]: percAttribute(PERC_INLINE_TYPE_ATTR),
      [PERC_DEPENDENT_ID_ATTR]: percAttribute(PERC_DEPENDENT_ID_ATTR),
      [PERC_DEPENDENT_VARIANT_ATTR]: percAttribute(PERC_DEPENDENT_VARIANT_ATTR),
      [PERC_RELATIONSHIP_ATTR]: percAttribute(PERC_RELATIONSHIP_ATTR),
      [PERC_JCR_PATH_ATTR]: percAttribute(PERC_JCR_PATH_ATTR),
    };
  },
});

/**
 * Global-variable marker node ({@code percglobalvariables} port). Atom and
 * selectable so backspace removes the whole marker; serializes to the stored
 * span form with the resolved value text.
 */
export const PercGlobalVariable = Node.create({
  name: "percGlobalVariable",
  inline: true,
  group: "inline",
  atom: true,
  selectable: true,

  addAttributes() {
    return {
      name: { default: "" },
      value: { default: "" },
    };
  },

  parseHTML() {
    return [
      {
        tag: `span.${PERC_GLOBAL_VARIABLE_CLASS}`,
        getAttrs: (el: HTMLElement | string) => {
          if (typeof el === "string") {
            return false;
          }
          const parsed = parseGlobalVariableMarker(el);
          return parsed ? { name: parsed.name, value: parsed.value } : false;
        },
      },
      {
        tag: `img.${PERC_GLOBAL_VARIABLE_CLASS}`,
        getAttrs: (el: HTMLElement | string) => {
          if (typeof el === "string") {
            return false;
          }
          const name = el.getAttribute("title") ?? "";
          if (!name) {
            return false;
          }
          return { name, value: readGlobalVariableData()?.[name] ?? "" };
        },
      },
    ];
  },

  renderHTML({ node }) {
    const name = String(node.attrs["name"] ?? "");
    const value = String(node.attrs["value"] ?? "");
    return [
      "span",
      mergeAttributes({ class: PERC_GLOBAL_VARIABLE_CLASS, title: name }),
      value,
    ];
  },

  addNodeView() {
    return ({ node }: { node: { attrs: Record<string, unknown> } }) => {
      const name = String(node.attrs["name"] ?? "");
      const dom = document.createElement("span");
      dom.className = PERC_GLOBAL_VARIABLE_CLASS;
      // Edit surface shows the variable name (TinyMCE showed an image
      // placeholder); the document keeps the stored span, so an unknown or
      // empty resolved value still renders a visible badge.
      dom.textContent = name ? `$${name}` : "";
      dom.setAttribute("title", name);
      dom.setAttribute("contenteditable", "false");
      return { dom };
    };
  },

  addCommands() {
    return {
      insertPercGlobalVariable:
        (name: string, value: string) =>
        ({ commands }) => {
          if (!name.trim()) {
            return false;
          }
          return commands.insertContent({
            type: this.name,
            attrs: { name: name.trim(), value },
          });
        },
    };
  },
});

/**
 * Blog more-link marker node ({@code percmorelink} port). Atom with no
 * content: the edit surface shows an uneditable badge through the node view
 * while the document keeps (and serializes) the bare stored span.
 */
export const PercMoreLink = Node.create({
  name: "percMoreLink",
  inline: true,
  group: "inline",
  atom: true,
  selectable: true,

  parseHTML() {
    return [
      { tag: `span.${PERC_MORE_LINK_CLASS}` },
      { tag: `img.${PERC_MORE_LINK_CLASS}` },
    ];
  },

  renderHTML() {
    return ["span", mergeAttributes({ class: PERC_MORE_LINK_CLASS })];
  },

  addNodeView() {
    return () => {
      const dom = document.createElement("span");
      dom.className = `${PERC_MORE_LINK_CLASS} perc-more-link-edit`;
      dom.textContent = "More";
      dom.setAttribute("aria-label", "More link marker");
      dom.setAttribute("contenteditable", "false");
      return { dom };
    };
  },

  addCommands() {
    return {
      insertPercMoreLink: () => ({ commands }) =>
        commands.insertContent({ type: this.name }),
    };
  },
});

/** Relationship attributes kept on template-variant wrappers. */
function variantRelationshipAttributes(): Record<string, unknown> {
  return {
    [PERC_INLINE_SLOT_ATTR]: percAttribute(PERC_INLINE_SLOT_ATTR),
    [PERC_INLINE_TYPE_ATTR]: percAttribute(PERC_INLINE_TYPE_ATTR),
    [PERC_DEPENDENT_ID_ATTR]: percAttribute(PERC_DEPENDENT_ID_ATTR),
    [PERC_DEPENDENT_VARIANT_ATTR]: percAttribute(PERC_DEPENDENT_VARIANT_ATTR),
    [PERC_RELATIONSHIP_ATTR]: percAttribute(PERC_RELATIONSHIP_ATTR),
    // #7104: the stored wrappers also carry the broken-link selected text
    // and the edit-time lock flag. Both must survive the round-trip:
    // processInlineLink falls back to rxselectedtext for broken-link
    // cleanup, and dropping contenteditable would misrepresent the stored
    // lock state on the next save.
    [PERC_SELECTED_TEXT_ATTR]: percAttribute(PERC_SELECTED_TEXT_ATTR),
    [PERC_CONTENT_EDITABLE_ATTR]: percAttribute(PERC_CONTENT_EDITABLE_ATTR),
  };
}

/**
 * Stored inline template-variant wrapper ({@code rxvariant} port). Keeps
 * the wrapper — with its relationship attributes — so the inline-template
 * relationship survives the edit round-trip; renders the stored span form.
 */
export const PercInlineVariant = Node.create({
  name: "percInlineVariant",
  inline: true,
  group: "inline",
  content: "inline*",

  addAttributes() {
    return variantRelationshipAttributes();
  },

  parseHTML() {
    return [{ tag: `span.${PERC_INLINE_VARIANT_CLASS}` }];
  },

  renderHTML({ node }) {
    const htmlAttributes: Record<string, unknown> = {
      class: PERC_INLINE_VARIANT_CLASS,
    };
    for (const attr of Object.keys(variantRelationshipAttributes())) {
      const value = node.attrs[attr];
      if (typeof value === "string" && value) {
        htmlAttributes[attr] = value;
      }
    }
    return ["span", mergeAttributes(htmlAttributes), 0];
  },
});

/**
 * Stored block template-variant wrapper ({@code rxvariant} port). Keeps the
 * wrapper div — with its relationship attributes — so the inline-template
 * relationship survives the edit round-trip; renders the stored div form.
 */
export const PercBlockVariant = Node.create({
  name: "percBlockVariant",
  group: "block",
  content: "block+",

  addAttributes() {
    return variantRelationshipAttributes();
  },

  parseHTML() {
    return [{ tag: `div.${PERC_BLOCK_VARIANT_CLASS}` }];
  },

  renderHTML({ node }) {
    const htmlAttributes: Record<string, unknown> = {
      class: PERC_BLOCK_VARIANT_CLASS,
    };
    for (const attr of Object.keys(variantRelationshipAttributes())) {
      const value = node.attrs[attr];
      if (typeof value === "string" && value) {
        htmlAttributes[attr] = value;
      }
    }
    return ["div", mergeAttributes(htmlAttributes), 0];
  },
});
