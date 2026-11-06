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
 * TipTap formatting toolbar for HTML fields (#5622 slice #5651).
 *
 * <p>Ports the {@code rx_default_config.json} toolbar to the bundled TipTap
 * editor: undo/redo, bold/italic/underline, text style (paragraph/headings),
 * alignment, bullet/numbered lists with indent/outdent, link/unlink with an
 * inline address editor, inline code/code block, and table editing (insert,
 * row/column add/remove, delete). Every control drives a StarterKit or
 * configured extension command — no custom plugins (slice #5652) — and
 * reports through the widget {@code onChange} HTML contract, so the existing
 * {@code PUT /services/itemmanagement/item/fields/{id}} persist path is
 * reused unchanged.</p>
 *
 * <p>Buttons keep editor focus (and the selection) with a mousedown guard:
 * without it the click would move focus to the button and list/link/table
 * commands would apply to a collapsed caret instead of the selection.</p>
 */

import type { ChainedCommands, Editor } from "@tiptap/core";
import React, { useState } from "react";
import { message } from "../../i18n/message";
import { EDITOR_MSG } from "../messages";
import styles from "../EditorHost.module.css";
import { isPastedLinkHrefSafe } from "./tiptapWordPaste";

export interface TipTapToolbarProps {
  /** Mounted editor. Null (pre-mount) renders disabled controls. */
  editor: Editor | null;
  /** Field name, used to scope test ids. */
  name: string;
}

type HeadingValue = "p" | "h1" | "h2" | "h3";

function headingValue(editor: Editor | null): HeadingValue {
  if (!editor) {
    return "p";
  }
  if (editor.isActive("heading", { level: 1 })) {
    return "h1";
  }
  if (editor.isActive("heading", { level: 2 })) {
    return "h2";
  }
  if (editor.isActive("heading", { level: 3 })) {
    return "h3";
  }
  return "p";
}

/** Probe a command without running it (no focus steal, headless-safe). */
function canRun(
  editor: Editor | null,
  check: (chain: ChainedCommands) => boolean,
): boolean {
  if (!editor) {
    return false;
  }
  try {
    return check(editor.can().chain());
  } catch {
    return false;
  }
}

function runCommand(
  editor: Editor | null,
  apply: (chain: ChainedCommands) => unknown,
): void {
  if (!editor) {
    return;
  }
  apply(editor.chain().focus());
}

interface ToolButtonProps {
  action: string;
  name: string;
  label: string;
  visible: React.ReactNode;
  pressed?: boolean;
  disabled?: boolean;
  onPress: () => void;
}

function ToolButton({
  action,
  name,
  label,
  visible,
  pressed = false,
  disabled = false,
  onPress,
}: ToolButtonProps): React.ReactElement {
  return (
    <button
      type="button"
      className={`${styles.tiptapTool} ${pressed ? styles.tiptapToolActive : ""}`}
      data-testid={`tiptap-${action}-${name}`}
      aria-pressed={pressed}
      disabled={disabled}
      // Keep the editor selection: focusing the button would collapse it.
      onMouseDown={(e) => e.preventDefault()}
      onClick={onPress}
    >
      {/* Glyph is visual only; the accessible name comes from the
          screen-reader text so axe label-in-name always passes. */}
      <span aria-hidden="true">{visible}</span>
      <span className={styles.tiptapSrOnly}>{label}</span>
    </button>
  );
}

export function TipTapToolbar({ editor, name }: TipTapToolbarProps): React.ReactElement {
  const [linkOpen, setLinkOpen] = useState(false);
  const [linkUrl, setLinkUrl] = useState("");
  const [linkError, setLinkError] = useState(false);

  const openLinkEditor = (): void => {
    const current =
      (editor?.getAttributes("link").href as string | undefined) ?? "";
    setLinkUrl(current);
    setLinkError(false);
    setLinkOpen(true);
  };

  const applyLink = (): void => {
    if (!editor) {
      return;
    }
    const href = linkUrl.trim();
    if (!href) {
      // Empty address removes the link, like the TinyMCE link dialog.
      editor.chain().focus().unsetLink().run();
      setLinkOpen(false);
      return;
    }
    if (!isPastedLinkHrefSafe(href)) {
      setLinkError(true);
      return;
    }
    editor.chain().focus().setLink({ href }).run();
    setLinkOpen(false);
  };

  const applyStyle = (value: HeadingValue): void => {
    if (!editor || value === headingValue(editor)) {
      return;
    }
    if (value === "p") {
      editor.chain().focus().setParagraph().run();
      return;
    }
    editor
      .chain()
      .focus()
      .toggleHeading({ level: Number(value.slice(1)) as 1 | 2 | 3 })
      .run();
  };

  const applyAlign = (align: "left" | "center" | "right" | "justify"): void => {
    runCommand(editor, (c) => c.setTextAlign(align).run());
  };

  const insertParityTable = (): void => {
    runCommand(editor, (c) =>
      c.insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run(),
    );
  };

  const styleValue = headingValue(editor);
  const linkActive = editor?.isActive("link") ?? false;

  return (
    <div
      role="toolbar"
      aria-label={message(EDITOR_MSG.TIPTAP_TOOLBAR)}
      data-testid={`editor-tiptap-toolbar-${name}`}
      className={styles.tiptapToolbar}
    >
      <span role="group" className={styles.tiptapToolGroup}>
        <ToolButton
          action="undo"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_UNDO)}
          visible="↶"
          disabled={!canRun(editor, (c) => c.undo().run())}
          onPress={() => runCommand(editor, (c) => c.undo().run())}
        />
        <ToolButton
          action="redo"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_REDO)}
          visible="↷"
          disabled={!canRun(editor, (c) => c.redo().run())}
          onPress={() => runCommand(editor, (c) => c.redo().run())}
        />
      </span>
      <span role="group" className={styles.tiptapToolGroup}>
        <ToolButton
          action="bold"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_BOLD)}
          visible={<strong>B</strong>}
          pressed={editor?.isActive("bold") ?? false}
          disabled={!canRun(editor, (c) => c.toggleBold().run())}
          onPress={() => runCommand(editor, (c) => c.toggleBold().run())}
        />
        <ToolButton
          action="italic"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_ITALIC)}
          visible={<em>I</em>}
          pressed={editor?.isActive("italic") ?? false}
          disabled={!canRun(editor, (c) => c.toggleItalic().run())}
          onPress={() => runCommand(editor, (c) => c.toggleItalic().run())}
        />
        <ToolButton
          action="underline"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_UNDERLINE)}
          visible={<u>U</u>}
          pressed={editor?.isActive("underline") ?? false}
          disabled={!canRun(editor, (c) => c.toggleUnderline().run())}
          onPress={() => runCommand(editor, (c) => c.toggleUnderline().run())}
        />
      </span>
      <span role="group" className={styles.tiptapToolGroup}>
        <select
          data-testid={`tiptap-style-${name}`}
          aria-label={message(EDITOR_MSG.TIPTAP_STYLE)}
          className={styles.tiptapStyleSelect}
          value={styleValue}
          disabled={!editor}
          onMouseDown={(e) => e.preventDefault()}
          onChange={(e) => applyStyle(e.target.value as HeadingValue)}
        >
          <option value="p">{message(EDITOR_MSG.TIPTAP_STYLE_PARAGRAPH)}</option>
          <option value="h1">H1</option>
          <option value="h2">H2</option>
          <option value="h3">H3</option>
        </select>
      </span>
      <span role="group" className={styles.tiptapToolGroup}>
        <ToolButton
          action="align-left"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_ALIGN_LEFT)}
          visible="≡‹"
          pressed={editor?.isActive({ textAlign: "left" }) ?? false}
          disabled={!canRun(editor, (c) => c.setTextAlign("left").run())}
          onPress={() => applyAlign("left")}
        />
        <ToolButton
          action="align-center"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_ALIGN_CENTER)}
          visible="≡"
          pressed={editor?.isActive({ textAlign: "center" }) ?? false}
          disabled={!canRun(editor, (c) => c.setTextAlign("center").run())}
          onPress={() => applyAlign("center")}
        />
        <ToolButton
          action="align-right"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_ALIGN_RIGHT)}
          visible="≡›"
          pressed={editor?.isActive({ textAlign: "right" }) ?? false}
          disabled={!canRun(editor, (c) => c.setTextAlign("right").run())}
          onPress={() => applyAlign("right")}
        />
        <ToolButton
          action="align-justify"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_ALIGN_JUSTIFY)}
          visible="≣"
          pressed={editor?.isActive({ textAlign: "justify" }) ?? false}
          disabled={!canRun(editor, (c) => c.setTextAlign("justify").run())}
          onPress={() => applyAlign("justify")}
        />
      </span>
      <span role="group" className={styles.tiptapToolGroup}>
        <ToolButton
          action="bullist"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_BULLET_LIST)}
          visible="•☰"
          pressed={editor?.isActive("bulletList") ?? false}
          disabled={!canRun(editor, (c) => c.toggleBulletList().run())}
          onPress={() => runCommand(editor, (c) => c.toggleBulletList().run())}
        />
        <ToolButton
          action="numlist"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_NUMBERED_LIST)}
          visible="1.☰"
          pressed={editor?.isActive("orderedList") ?? false}
          disabled={!canRun(editor, (c) => c.toggleOrderedList().run())}
          onPress={() => runCommand(editor, (c) => c.toggleOrderedList().run())}
        />
        <ToolButton
          action="outdent"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_OUTDENT)}
          visible="⇤"
          disabled={!canRun(editor, (c) => c.liftListItem("listItem").run())}
          onPress={() => runCommand(editor, (c) => c.liftListItem("listItem").run())}
        />
        <ToolButton
          action="indent"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_INDENT)}
          visible="⇥"
          disabled={!canRun(editor, (c) => c.sinkListItem("listItem").run())}
          onPress={() => runCommand(editor, (c) => c.sinkListItem("listItem").run())}
        />
      </span>
      <span role="group" className={styles.tiptapToolGroup}>
        <ToolButton
          action="link"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_LINK)}
          visible="🔗"
          pressed={linkActive}
          disabled={!editor}
          onPress={openLinkEditor}
        />
        <ToolButton
          action="unlink"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_UNLINK)}
          visible="⛓✕"
          disabled={!linkActive}
          onPress={() => runCommand(editor, (c) => c.unsetLink().run())}
        />
      </span>
      <span role="group" className={styles.tiptapToolGroup}>
        <ToolButton
          action="code-inline"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_CODE_INLINE)}
          visible="‹›"
          pressed={editor?.isActive("code") ?? false}
          disabled={!canRun(editor, (c) => c.toggleCode().run())}
          onPress={() => runCommand(editor, (c) => c.toggleCode().run())}
        />
        <ToolButton
          action="code-block"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_CODE_BLOCK)}
          visible="{;}"
          pressed={editor?.isActive("codeBlock") ?? false}
          disabled={!canRun(editor, (c) => c.toggleCodeBlock().run())}
          onPress={() => runCommand(editor, (c) => c.toggleCodeBlock().run())}
        />
      </span>
      <span role="group" className={styles.tiptapToolGroup}>
        <ToolButton
          action="table-insert"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_TABLE_INSERT)}
          visible="⊞"
          disabled={!canRun(editor, (c) =>
            c.insertTable({ rows: 3, cols: 3, withHeaderRow: true }).run(),
          )}
          onPress={insertParityTable}
        />
        <ToolButton
          action="table-add-row"
          name={name}
          label={message(EDITOR_MSG.TABLE_ADD_ROW)}
          visible="⊞+R"
          disabled={!canRun(editor, (c) => c.addRowAfter().run())}
          onPress={() => runCommand(editor, (c) => c.addRowAfter().run())}
        />
        <ToolButton
          action="table-remove-row"
          name={name}
          label={message(EDITOR_MSG.TABLE_REMOVE_ROW)}
          visible="⊞−R"
          disabled={!canRun(editor, (c) => c.deleteRow().run())}
          onPress={() => runCommand(editor, (c) => c.deleteRow().run())}
        />
        <ToolButton
          action="table-add-col"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_TABLE_ADD_COLUMN)}
          visible="⊞+C"
          disabled={!canRun(editor, (c) => c.addColumnAfter().run())}
          onPress={() => runCommand(editor, (c) => c.addColumnAfter().run())}
        />
        <ToolButton
          action="table-remove-col"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_TABLE_REMOVE_COLUMN)}
          visible="⊞−C"
          disabled={!canRun(editor, (c) => c.deleteColumn().run())}
          onPress={() => runCommand(editor, (c) => c.deleteColumn().run())}
        />
        <ToolButton
          action="table-delete"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_TABLE_DELETE)}
          visible="⊞✕"
          disabled={!canRun(editor, (c) => c.deleteTable().run())}
          onPress={() => runCommand(editor, (c) => c.deleteTable().run())}
        />
      </span>
      {linkOpen ? (
        <span role="group" className={styles.tiptapLinkEditor}>
          <input
            data-testid={`tiptap-link-url-${name}`}
            aria-label={message(EDITOR_MSG.TIPTAP_LINK_URL)}
            className={styles.tiptapLinkInput}
            value={linkUrl}
            placeholder="https://…"
            onChange={(e) => {
              setLinkUrl(e.target.value);
              setLinkError(false);
            }}
            onKeyDown={(e) => {
              if (e.key === "Enter") {
                e.preventDefault();
                applyLink();
              } else if (e.key === "Escape") {
                setLinkOpen(false);
              }
            }}
          />
          <button
            type="button"
            data-testid={`tiptap-link-apply-${name}`}
            className={styles.tiptapLinkApply}
            onMouseDown={(e) => e.preventDefault()}
            onClick={applyLink}
          >
            {message(EDITOR_MSG.TIPTAP_LINK_APPLY)}
          </button>
          <button
            type="button"
            data-testid={`tiptap-link-close-${name}`}
            className={styles.tiptapLinkApply}
            onMouseDown={(e) => e.preventDefault()}
            onClick={() => setLinkOpen(false)}
          >
            <span aria-hidden="true">✕</span>
            <span className={styles.tiptapSrOnly}>
              {message(EDITOR_MSG.TIPTAP_LINK_CLOSE)}
            </span>
          </button>
          {linkError ? (
            <span role="alert" className={styles.tiptapLinkError}>
              {message(EDITOR_MSG.TIPTAP_LINK_UNSAFE)}
            </span>
          ) : null}
        </span>
      ) : null}
    </div>
  );
}

/**
 * Shared toolbar button for the Percussion plugin bar (#5652): the same
 * mousedown selection guard, test-id shape, and screen-reader naming as the
 * parity toolbar.
 */
export { ToolButton as TipTapToolButton };
export type { ToolButtonProps as TipTapToolButtonProps };
