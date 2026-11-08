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
 * Percussion plugin toolbar for the TipTap editor host (#5622 slice #5652).
 *
 * <p>Self-contained React ports of the first-party TinyMCE plugins whose
 * classic dialogs cannot run in the React host (they depend on the
 * ContentEditor frame services):</p>
 * <ul>
 *   <li>{@code rxinline} — inline link / inline image / inline template
 *       dialogs emitting the same stored attribute shape
 *       ({@code rxinlineslot}, {@code inlinetype}, {@code sys_dependentid},
 *       {@code sys_dependentvariantid}, {@code rxselectedtext});</li>
 *   <li>{@code percadvlink} / {@code percadvimage} — URL/title/target and
 *       src/alt/title/dimension/alignment/decorative controls writing the
 *       same attributes the advanced dialogs persist (the CMS content-picker
 *       lookup stays a classic-dialog concern, out of slice scope);</li>
 *   <li>{@code percglobalvariables} — variable picker (CMS table when the
 *       hosting frame provides {@code PercGlobalVariablesData}, manual name
 *       entry otherwise) inserting the stored marker span;</li>
 *   <li>{@code percmorelink} — one-click more-link marker insert;</li>
 *   <li>{@code rxinserthtml} — raw HTML fragment insert dialog;</li>
 *   <li>CodeMirror source view — raw HTML source panel writing back through
 *       the schema (plain textarea; no new npm dependency).</li>
 * </ul>
 *
 * <p>Every insert flows through TipTap commands, so the widget
 * {@code onChange} HTML contract and the existing
 * {@code PUT /services/itemmanagement/item/fields/{id}} persist path are
 * reused unchanged. The {@code Ctrl+Shift+L/T/I/H} shortcuts mirror the
 * TinyMCE plugins and are scoped to the editor surface.</p>
 */

import type { Editor } from "@tiptap/core";
import React, { useEffect, useState } from "react";
import { message } from "../../i18n/message";
import { EDITOR_MSG } from "../messages";
import styles from "../EditorHost.module.css";
import {
  PERC_DEFAULT_IMAGE_SLOT,
  PERC_DEFAULT_LINK_SLOT,
  PERC_DEFAULT_VARIANT_SLOT,
  PERC_INLINE_TYPE_LINK,
  PERC_INLINE_TYPE_TEMPLATE,
  buildPercInlineImageHtml,
  buildPercInlineLinkHtml,
  readGlobalVariableData,
} from "./tiptapPercussion";
import { TipTapToolButton } from "./TipTapToolbar";
import { isPastedLinkHrefSafe } from "./tiptapWordPaste";

export interface TipTapPercussionBarProps {
  /** Mounted editor. Null (pre-mount) renders disabled controls. */
  editor: Editor | null;
  /** Field name, used to scope test ids. */
  name: string;
}

type PercPanel =
  | "inline-link"
  | "inline-image"
  | "inline-template"
  | "insert-html"
  | "global-variable"
  | "source";

/** Classic-plugin shortcut map, scoped to the editor surface. */
const SHORTCUT_PANELS: Record<string, PercPanel> = {
  l: "inline-link",
  t: "inline-template",
  i: "inline-image",
  h: "insert-html",
};

export function TipTapPercussionBar({
  editor,
  name,
}: TipTapPercussionBarProps): React.ReactElement {
  const [panel, setPanel] = useState<PercPanel | null>(null);

  useEffect(() => {
    const dom = editor?.view.dom;
    if (!dom) {
      return undefined;
    }
    const onKeyDown = (event: KeyboardEvent): void => {
      if (!event.ctrlKey || !event.shiftKey || event.altKey || event.metaKey) {
        return;
      }
      const target = SHORTCUT_PANELS[event.key.toLowerCase()];
      if (!target) {
        return;
      }
      event.preventDefault();
      setPanel(target);
    };
    dom.addEventListener("keydown", onKeyDown);
    return () => {
      dom.removeEventListener("keydown", onKeyDown);
    };
  }, [editor]);

  const openPanel = (next: PercPanel): void => {
    setPanel((current) => (current === next ? null : next));
  };

  return (
    <div
      role="toolbar"
      aria-label={message(EDITOR_MSG.TIPTAP_PERC_GROUP)}
      data-testid={`editor-tiptap-perc-${name}`}
      className={styles.tiptapToolbar}
    >
      <span role="group" className={styles.tiptapToolGroup}>
        <TipTapToolButton
          action="perc-inline-link"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_PERC_INLINE_LINK)}
          visible="🔗+"
          pressed={panel === "inline-link"}
          disabled={!editor}
          onPress={() => openPanel("inline-link")}
        />
        <TipTapToolButton
          action="perc-inline-image"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_PERC_INLINE_IMAGE)}
          visible="🖼+"
          pressed={panel === "inline-image"}
          disabled={!editor}
          onPress={() => openPanel("inline-image")}
        />
        <TipTapToolButton
          action="perc-inline-template"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_PERC_INLINE_TEMPLATE)}
          visible="▦+"
          pressed={panel === "inline-template"}
          disabled={!editor}
          onPress={() => openPanel("inline-template")}
        />
        <TipTapToolButton
          action="perc-insert-html"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_PERC_INSERT_HTML)}
          visible="‹+›"
          pressed={panel === "insert-html"}
          disabled={!editor}
          onPress={() => openPanel("insert-html")}
        />
        <TipTapToolButton
          action="perc-global-variable"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_PERC_GLOBAL_VARIABLE)}
          visible="$x"
          pressed={panel === "global-variable"}
          disabled={!editor}
          onPress={() => openPanel("global-variable")}
        />
        <TipTapToolButton
          action="perc-more-link"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_PERC_MORE_LINK)}
          visible="⋯»"
          disabled={!editor}
          onPress={() => {
            editor?.chain().focus().insertPercMoreLink().run();
          }}
        />
        <TipTapToolButton
          action="perc-source"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_PERC_SOURCE)}
          visible="{…}"
          pressed={panel === "source"}
          disabled={!editor}
          onPress={() => openPanel("source")}
        />
      </span>
      {panel === "inline-link" ? (
        <InlineLinkPanel editor={editor} name={name} onClose={() => setPanel(null)} />
      ) : null}
      {panel === "inline-image" ? (
        <InlineImagePanel editor={editor} name={name} onClose={() => setPanel(null)} />
      ) : null}
      {panel === "inline-template" ? (
        <InlineTemplatePanel editor={editor} name={name} onClose={() => setPanel(null)} />
      ) : null}
      {panel === "insert-html" ? (
        <InsertHtmlPanel editor={editor} name={name} onClose={() => setPanel(null)} />
      ) : null}
      {panel === "global-variable" ? (
        <GlobalVariablePanel editor={editor} name={name} onClose={() => setPanel(null)} />
      ) : null}
      {panel === "source" ? (
        <SourcePanel editor={editor} name={name} onClose={() => setPanel(null)} />
      ) : null}
    </div>
  );
}

interface PanelProps {
  editor: Editor | null;
  name: string;
  onClose: () => void;
}

function PanelShell({
  name,
  label,
  onClose,
  onApply,
  canApply,
  children,
}: PanelProps & {
  label: string;
  onApply: () => void;
  canApply: boolean;
  children: React.ReactNode;
}): React.ReactElement {
  return (
    <span
      role="group"
      aria-label={label}
      data-testid={`tiptap-perc-dialog-${name}`}
      className={`${styles.tiptapLinkEditor} ${styles.tiptapPercDialog}`}
    >
      {children}
      <button
        type="button"
        data-testid={`tiptap-perc-apply-${name}`}
        className={styles.tiptapLinkApply}
        disabled={!canApply}
        onMouseDown={(e) => e.preventDefault()}
        onClick={onApply}
      >
        {message(EDITOR_MSG.TIPTAP_PERC_APPLY)}
      </button>
      <button
        type="button"
        data-testid={`tiptap-perc-close-${name}`}
        className={styles.tiptapLinkApply}
        onMouseDown={(e) => e.preventDefault()}
        onClick={onClose}
      >
        <span aria-hidden="true">✕</span>
        <span className={styles.tiptapSrOnly}>
          {message(EDITOR_MSG.TIPTAP_PERC_CLOSE)}
        </span>
      </button>
    </span>
  );
}

function InlineLinkPanel({ editor, name, onClose }: PanelProps): React.ReactElement {
  const [href, setHref] = useState("");
  const [text, setText] = useState("");
  const [title, setTitle] = useState("");
  const [target, setTarget] = useState("");
  const [error, setError] = useState(false);
  const canApply = href.trim().length > 0 && !error;

  const apply = (): void => {
    if (!editor) {
      return;
    }
    const address = href.trim();
    if (!address || !isPastedLinkHrefSafe(address)) {
      setError(true);
      return;
    }
    const label = text.trim() || address;
    editor
      .chain()
      .focus()
      .insertContent(
        buildPercInlineLinkHtml({
          href: address,
          text: label,
          title: title.trim() || undefined,
          target: target || undefined,
          slot: PERC_DEFAULT_LINK_SLOT,
          inlineType: PERC_INLINE_TYPE_LINK,
          selectedText: label,
        }),
      )
      .run();
    onClose();
  };

  return (
    <PanelShell
      editor={editor}
      name={name}
      label={message(EDITOR_MSG.TIPTAP_PERC_INLINE_LINK)}
      onClose={onClose}
      onApply={apply}
      canApply={canApply}
    >
      <input
        data-testid={`tiptap-perc-link-url-${name}`}
        aria-label={message(EDITOR_MSG.TIPTAP_PERC_LINK_URL)}
        className={styles.tiptapLinkInput}
        value={href}
        placeholder="/Sites/…"
        onChange={(e) => {
          setHref(e.target.value);
          setError(false);
        }}
      />
      <input
        data-testid={`tiptap-perc-link-text-${name}`}
        aria-label={message(EDITOR_MSG.TIPTAP_PERC_LINK_TEXT)}
        className={styles.tiptapLinkInput}
        value={text}
        onChange={(e) => setText(e.target.value)}
      />
      <input
        data-testid={`tiptap-perc-link-title-${name}`}
        aria-label={message(EDITOR_MSG.TIPTAP_PERC_LINK_TITLE)}
        className={styles.tiptapLinkInput}
        value={title}
        onChange={(e) => setTitle(e.target.value)}
      />
      <select
        data-testid={`tiptap-perc-link-target-${name}`}
        aria-label={message(EDITOR_MSG.TIPTAP_PERC_LINK_TARGET)}
        className={styles.tiptapStyleSelect}
        value={target}
        onChange={(e) => setTarget(e.target.value)}
      >
        <option value="">{message(EDITOR_MSG.TIPTAP_PERC_LINK_TARGET_SAME)}</option>
        <option value="_blank">{message(EDITOR_MSG.TIPTAP_PERC_LINK_TARGET_BLANK)}</option>
        <option value="_parent">{message(EDITOR_MSG.TIPTAP_PERC_LINK_TARGET_PARENT)}</option>
        <option value="_top">{message(EDITOR_MSG.TIPTAP_PERC_LINK_TARGET_TOP)}</option>
      </select>
      {error ? (
        <span role="alert" className={styles.tiptapLinkError}>
          {message(EDITOR_MSG.TIPTAP_LINK_UNSAFE)}
        </span>
      ) : null}
    </PanelShell>
  );
}

function InlineImagePanel({ editor, name, onClose }: PanelProps): React.ReactElement {
  const [src, setSrc] = useState("");
  const [alt, setAlt] = useState("");
  const [title, setTitle] = useState("");
  const [width, setWidth] = useState("");
  const [height, setHeight] = useState("");
  const [align, setAlign] = useState("");
  const [decorative, setDecorative] = useState(false);
  const [error, setError] = useState(false);
  const canApply = src.trim().length > 0 && !error;

  const apply = (): void => {
    if (!editor) {
      return;
    }
    const address = src.trim();
    if (!address || !isPastedLinkHrefSafe(address)) {
      setError(true);
      return;
    }
    editor
      .chain()
      .focus()
      .insertContent(
        buildPercInlineImageHtml({
          src: address,
          alt: alt.trim(),
          title: title.trim() || undefined,
          width: width.trim() || undefined,
          height: height.trim() || undefined,
          align: align || undefined,
          decorative,
          slot: PERC_DEFAULT_IMAGE_SLOT,
        }),
      )
      .run();
    onClose();
  };

  return (
    <PanelShell
      editor={editor}
      name={name}
      label={message(EDITOR_MSG.TIPTAP_PERC_INLINE_IMAGE)}
      onClose={onClose}
      onApply={apply}
      canApply={canApply}
    >
      <input
        data-testid={`tiptap-perc-image-src-${name}`}
        aria-label={message(EDITOR_MSG.TIPTAP_PERC_IMAGE_SRC)}
        className={styles.tiptapLinkInput}
        value={src}
        placeholder="/Assets/…"
        onChange={(e) => {
          setSrc(e.target.value);
          setError(false);
        }}
      />
      <input
        data-testid={`tiptap-perc-image-alt-${name}`}
        aria-label={message(EDITOR_MSG.TIPTAP_PERC_IMAGE_ALT)}
        className={styles.tiptapLinkInput}
        value={alt}
        disabled={decorative}
        onChange={(e) => setAlt(e.target.value)}
      />
      <input
        data-testid={`tiptap-perc-image-title-${name}`}
        aria-label={message(EDITOR_MSG.TIPTAP_PERC_LINK_TITLE)}
        className={styles.tiptapLinkInput}
        value={title}
        disabled={decorative}
        onChange={(e) => setTitle(e.target.value)}
      />
      <input
        data-testid={`tiptap-perc-image-width-${name}`}
        aria-label={message(EDITOR_MSG.TIPTAP_PERC_IMAGE_WIDTH)}
        className={styles.tiptapLinkInput}
        value={width}
        inputMode="numeric"
        onChange={(e) => setWidth(e.target.value.replace(/[^0-9]/g, ""))}
      />
      <input
        data-testid={`tiptap-perc-image-height-${name}`}
        aria-label={message(EDITOR_MSG.TIPTAP_PERC_IMAGE_HEIGHT)}
        className={styles.tiptapLinkInput}
        value={height}
        inputMode="numeric"
        onChange={(e) => setHeight(e.target.value.replace(/[^0-9]/g, ""))}
      />
      <select
        data-testid={`tiptap-perc-image-align-${name}`}
        aria-label={message(EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN)}
        className={styles.tiptapStyleSelect}
        value={align}
        onChange={(e) => setAlign(e.target.value)}
      >
        <option value="">{message(EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_NONE)}</option>
        <option value="left">{message(EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_LEFT)}</option>
        <option value="right">{message(EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_RIGHT)}</option>
        <option value="top">{message(EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_TOP)}</option>
        <option value="middle">{message(EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_MIDDLE)}</option>
        <option value="bottom">{message(EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_BOTTOM)}</option>
      </select>
      <label className={styles.tiptapPercCheck}>
        <input
          type="checkbox"
          data-testid={`tiptap-perc-image-decorative-${name}`}
          checked={decorative}
          onChange={(e) => setDecorative(e.target.checked)}
        />
        {message(EDITOR_MSG.TIPTAP_PERC_IMAGE_DECORATIVE)}
      </label>
      {error ? (
        <span role="alert" className={styles.tiptapLinkError}>
          {message(EDITOR_MSG.TIPTAP_LINK_UNSAFE)}
        </span>
      ) : null}
    </PanelShell>
  );
}

function InlineTemplatePanel({ editor, name, onClose }: PanelProps): React.ReactElement {
  const [path, setPath] = useState("");
  const [text, setText] = useState("");
  const [error, setError] = useState(false);
  const canApply = path.trim().length > 0 && !error;

  const apply = (): void => {
    if (!editor) {
      return;
    }
    const address = path.trim();
    if (!address || !isPastedLinkHrefSafe(address)) {
      setError(true);
      return;
    }
    const label = text.trim() || address;
    editor
      .chain()
      .focus()
      .insertContent(
        buildPercInlineLinkHtml({
          href: address,
          text: label,
          slot: PERC_DEFAULT_VARIANT_SLOT,
          inlineType: PERC_INLINE_TYPE_TEMPLATE,
          selectedText: label,
        }),
      )
      .run();
    onClose();
  };

  return (
    <PanelShell
      editor={editor}
      name={name}
      label={message(EDITOR_MSG.TIPTAP_PERC_INLINE_TEMPLATE)}
      onClose={onClose}
      onApply={apply}
      canApply={canApply}
    >
      <input
        data-testid={`tiptap-perc-template-path-${name}`}
        aria-label={message(EDITOR_MSG.TIPTAP_PERC_TEMPLATE_PATH)}
        className={styles.tiptapLinkInput}
        value={path}
        placeholder="/Sites/…"
        onChange={(e) => {
          setPath(e.target.value);
          setError(false);
        }}
      />
      <input
        data-testid={`tiptap-perc-template-text-${name}`}
        aria-label={message(EDITOR_MSG.TIPTAP_PERC_TEMPLATE_TEXT)}
        className={styles.tiptapLinkInput}
        value={text}
        onChange={(e) => setText(e.target.value)}
      />
      {error ? (
        <span role="alert" className={styles.tiptapLinkError}>
          {message(EDITOR_MSG.TIPTAP_LINK_UNSAFE)}
        </span>
      ) : null}
    </PanelShell>
  );
}

function InsertHtmlPanel({ editor, name, onClose }: PanelProps): React.ReactElement {
  const [fragment, setFragment] = useState("");
  const [error, setError] = useState(false);

  const apply = (): void => {
    if (!editor) {
      return;
    }
    if (!fragment.trim()) {
      setError(true);
      return;
    }
    // Raw fragment insert, like rxinserthtml: the save-side htmlField.ts
    // checks still reject script/event markup before anything persists.
    editor.chain().focus().insertContent(fragment).run();
    onClose();
  };

  return (
    <PanelShell
      editor={editor}
      name={name}
      label={message(EDITOR_MSG.TIPTAP_PERC_INSERT_HTML)}
      onClose={onClose}
      onApply={apply}
      canApply={fragment.trim().length > 0}
    >
      <textarea
        data-testid={`tiptap-perc-html-${name}`}
        aria-label={message(EDITOR_MSG.TIPTAP_PERC_HTML_CONTENT)}
        className={styles.tiptapPercTextarea}
        value={fragment}
        onChange={(e) => {
          setFragment(e.target.value);
          setError(false);
        }}
      />
      {error ? (
        <span role="alert" className={styles.tiptapLinkError}>
          {message(EDITOR_MSG.TIPTAP_PERC_REQUIRED)}
        </span>
      ) : null}
    </PanelShell>
  );
}

function GlobalVariablePanel({ editor, name, onClose }: PanelProps): React.ReactElement {
  const table = readGlobalVariableData();
  const names = table ? Object.keys(table).sort() : [];
  const [selected, setSelected] = useState(names[0] ?? "");
  const [manual, setManual] = useState("");
  const variableName = names.length > 0 ? selected : manual.trim();
  const canApply = variableName.length > 0;

  const apply = (): void => {
    if (!editor || !canApply) {
      return;
    }
    const value = table?.[variableName] ?? "";
    editor.chain().focus().insertPercGlobalVariable(variableName, value).run();
    onClose();
  };

  return (
    <PanelShell
      editor={editor}
      name={name}
      label={message(EDITOR_MSG.TIPTAP_PERC_GLOBAL_VARIABLE)}
      onClose={onClose}
      onApply={apply}
      canApply={canApply}
    >
      {names.length > 0 ? (
        <select
          data-testid={`tiptap-perc-variable-name-${name}`}
          aria-label={message(EDITOR_MSG.TIPTAP_PERC_VARIABLE_NAME)}
          className={styles.tiptapPercSelect}
          value={selected}
          onChange={(e) => setSelected(e.target.value)}
        >
          {names.map((variable) => (
            <option key={variable} value={variable}>
              {variable}
            </option>
          ))}
        </select>
      ) : (
        <input
          data-testid={`tiptap-perc-variable-name-${name}`}
          aria-label={message(EDITOR_MSG.TIPTAP_PERC_VARIABLE_NAME)}
          className={styles.tiptapLinkInput}
          value={manual}
          onChange={(e) => setManual(e.target.value)}
        />
      )}
    </PanelShell>
  );
}

function SourcePanel({ editor, name, onClose }: PanelProps): React.ReactElement {
  const [source, setSource] = useState(() => editor?.getHTML() ?? "");

  const apply = (): void => {
    if (!editor) {
      return;
    }
    // Source writes back through the schema, like the CodeMirror plugin's
    // save path: unknown markup is dropped, never persisted raw.
    editor.commands.setContent(source);
    onClose();
  };

  return (
    <PanelShell
      editor={editor}
      name={name}
      label={message(EDITOR_MSG.TIPTAP_PERC_SOURCE)}
      onClose={onClose}
      onApply={apply}
      canApply
    >
      <textarea
        data-testid={`tiptap-perc-source-content-${name}`}
        aria-label={message(EDITOR_MSG.TIPTAP_PERC_SOURCE_CONTENT)}
        className={styles.tiptapPercTextarea}
        value={source}
        spellCheck={false}
        onChange={(e) => setSource(e.target.value)}
      />
    </PanelShell>
  );
}
