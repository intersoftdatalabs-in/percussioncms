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
 * <p>MIT TipTap 3 / ProseMirror only; no classic-frame services.</p>
 */

import { Link } from "@tiptap/extension-link";
import { Image } from "@tiptap/extension-image";
import { Node, mergeAttributes } from "@tiptap/core";
import {
  PERC_DEPENDENT_ID_ATTR,
  PERC_DEPENDENT_VARIANT_ATTR,
  PERC_GLOBAL_VARIABLE_CLASS,
  PERC_INLINE_SLOT_ATTR,
  PERC_INLINE_TYPE_ATTR,
  PERC_JCR_PATH_ATTR,
  PERC_MORE_LINK_CLASS,
  PERC_RELATIONSHIP_ATTR,
  PERC_SELECTED_TEXT_ATTR,
  parseGlobalVariableMarker,
  readGlobalVariableData,
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
});

/** Parity {@code Image} node plus the CMS inline attributes. */
export const PercImage = Image.extend({
  addAttributes() {
    return {
      ...this.parent?.(),
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
