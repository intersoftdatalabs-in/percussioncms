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
 *       same attributes the advanced dialogs persist, plus a React
 *       content-picker lookup (browse/search CMS content, #5900) that
 *       resolves the managed-link reference through the render-link
 *       preview — manual URL entry stays as fallback;</li>
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
 *
 * <p>Dialog contract (#6589, same as the parity toolbar in #6588): each of
 * the six dialogs is a labelled {@code role="dialog"} — opening moves focus
 * into its first field and Escape closes it returning focus to the invoking
 * button. The Browse content picker nested in the link/image dialogs
 * follows the same contract one level deeper (#6658): focus moves into
 * the picker on open and Escape closes only the picker, returning focus
 * to the Browse button. The bar keeps a single tab stop via roving tabindex
 * (WAI-APG toolbar pattern over its own slot), so Tab enters each editor
 * toolbar exactly once.</p>
 */

import type { Editor } from "@tiptap/core";
import React, { useEffect, useRef, useState } from "react";
import { message } from "../../i18n/message";
import { EDITOR_MSG } from "../messages";
import styles from "../EditorHost.module.css";
import { fetchInlineRenderLink } from "../../api/contentExplorer/renderLinkApi";
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
import {
  resolveManagedImageRef,
  resolveManagedLinkRef,
  type ManagedImageRef,
  type ManagedLinkRef,
} from "./tiptapContentPicker";
import { ContentPickerDialog, type ContentPickerSelection } from "./ContentPickerDialog";
import { TipTapRovingContext, TipTapToolButton } from "./TipTapToolbar";
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

/**
 * Plugin-bar control order for roving tabindex (#6589, WAI-APG toolbar
 * pattern). Every entry maps to the {@code tiptap-<action>-<name>} test id;
 * the more-link action participates as a plain control (no dialog).
 */
const PERC_CONTROL_ORDER = [
  "perc-inline-link",
  "perc-inline-image",
  "perc-inline-template",
  "perc-insert-html",
  "perc-global-variable",
  "perc-more-link",
  "perc-source",
] as const;

type PercControlAction = (typeof PERC_CONTROL_ORDER)[number];

/** Stable dialog element id for a panel, wired to its trigger's aria-controls. */
function percDialogId(panel: PercPanel, name: string): string {
  return `tiptap-perc-dialog-${panel}-${name}`;
}

export function TipTapPercussionBar({
  editor,
  name,
}: TipTapPercussionBarProps): React.ReactElement {
  const [panel, setPanel] = useState<PercPanel | null>(null);
  const percBarRef = useRef<HTMLDivElement | null>(null);
  /**
   * Roving-tabindex slot (#6589): exactly one plugin-bar control is in the
   * Tab order at a time, mirroring the parity toolbar contract so Tab enters
   * each editor toolbar exactly once.
   */
  const [percRoving, setPercRoving] = useState<string>("perc-inline-link");
  /** Invoking trigger per panel, for Escape focus-return (dialog contract). */
  const triggerEls = useRef(new Map<PercPanel, HTMLButtonElement | null>());

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

  /** Close the open dialog returning focus to its invoking button. */
  const closePanelAndRefocus = (key: PercPanel): void => {
    setPanel(null);
    triggerEls.current.get(key)?.focus();
  };

  const focusPercControl = (action: PercControlAction): void => {
    const root = percBarRef.current;
    if (!root) {
      return;
    }
    const el = root.querySelector<HTMLElement>(
      `[data-testid="tiptap-${action}-${CSS.escape(name)}"]`,
    );
    el?.focus();
  };

  /**
   * WAI-APG toolbar keys (#6589): Left/Right move across enabled plugin-bar
   * controls (disabled controls are skipped), Home/End jump to the
   * first/last enabled control. Keys inside an open dialog keep their native
   * behavior (text caret keys in dialog fields never move toolbar focus).
   */
  const onPercToolbarKeyDown = (e: React.KeyboardEvent): void => {
    if (
      e.key !== "ArrowRight" &&
      e.key !== "ArrowLeft" &&
      e.key !== "Home" &&
      e.key !== "End"
    ) {
      return;
    }
    const root = percBarRef.current;
    if (!root) {
      return;
    }
    const target = e.target as HTMLElement | null;
    if (target && target.closest('[role="dialog"]') !== null) {
      return;
    }
    const enabled = PERC_CONTROL_ORDER.filter((action) => {
      const el = root.querySelector<HTMLButtonElement>(
        `[data-testid="tiptap-${action}-${CSS.escape(name)}"]`,
      );
      return el !== null && !el.disabled;
    });
    if (enabled.length === 0) {
      return;
    }
    const active = document.activeElement;
    let index = enabled.findIndex((action) => {
      const el = root.querySelector<HTMLElement>(
        `[data-testid="tiptap-${action}-${CSS.escape(name)}"]`,
      );
      return el === active;
    });
    if (e.key === "Home") {
      index = 0;
    } else if (e.key === "End") {
      index = enabled.length - 1;
    } else if (index === -1) {
      index = e.key === "ArrowLeft" ? enabled.length - 1 : 0;
    } else {
      index =
        (index + (e.key === "ArrowRight" ? 1 : -1) + enabled.length) %
        enabled.length;
    }
    e.preventDefault();
    const next = enabled[index];
    setPercRoving(next);
    focusPercControl(next);
  };

  /** Callback ref storing the invoking trigger for a panel's focus-return. */
  const triggerRefFor =
    (key: PercPanel): React.RefCallback<HTMLButtonElement> =>
    (el) => {
      triggerEls.current.set(key, el);
    };

  return (
    <TipTapRovingContext.Provider
      value={{ roving: percRoving, adopt: setPercRoving }}
    >
    <div
      ref={percBarRef}
      role="toolbar"
      aria-label={message(EDITOR_MSG.TIPTAP_PERC_GROUP)}
      data-testid={`editor-tiptap-perc-${name}`}
      className={styles.tiptapToolbar}
      onKeyDown={onPercToolbarKeyDown}
    >
      <span role="group" className={styles.tiptapToolGroup}>
        <TipTapToolButton
          action="perc-inline-link"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_PERC_INLINE_LINK)}
          visible="🔗+"
          pressed={panel === "inline-link"}
          disabled={!editor}
          buttonRef={triggerRefFor("inline-link")}
          ariaExpanded={panel === "inline-link"}
          ariaControls={percDialogId("inline-link", name)}
          onPress={() => openPanel("inline-link")}
        />
        <TipTapToolButton
          action="perc-inline-image"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_PERC_INLINE_IMAGE)}
          visible="🖼+"
          pressed={panel === "inline-image"}
          disabled={!editor}
          buttonRef={triggerRefFor("inline-image")}
          ariaExpanded={panel === "inline-image"}
          ariaControls={percDialogId("inline-image", name)}
          onPress={() => openPanel("inline-image")}
        />
        <TipTapToolButton
          action="perc-inline-template"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_PERC_INLINE_TEMPLATE)}
          visible="▦+"
          pressed={panel === "inline-template"}
          disabled={!editor}
          buttonRef={triggerRefFor("inline-template")}
          ariaExpanded={panel === "inline-template"}
          ariaControls={percDialogId("inline-template", name)}
          onPress={() => openPanel("inline-template")}
        />
        <TipTapToolButton
          action="perc-insert-html"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_PERC_INSERT_HTML)}
          visible="‹+›"
          pressed={panel === "insert-html"}
          disabled={!editor}
          buttonRef={triggerRefFor("insert-html")}
          ariaExpanded={panel === "insert-html"}
          ariaControls={percDialogId("insert-html", name)}
          onPress={() => openPanel("insert-html")}
        />
        <TipTapToolButton
          action="perc-global-variable"
          name={name}
          label={message(EDITOR_MSG.TIPTAP_PERC_GLOBAL_VARIABLE)}
          visible="$x"
          pressed={panel === "global-variable"}
          disabled={!editor}
          buttonRef={triggerRefFor("global-variable")}
          ariaExpanded={panel === "global-variable"}
          ariaControls={percDialogId("global-variable", name)}
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
          buttonRef={triggerRefFor("source")}
          ariaExpanded={panel === "source"}
          ariaControls={percDialogId("source", name)}
          onPress={() => openPanel("source")}
        />
      </span>
      {panel === "inline-link" ? (
        <InlineLinkPanel editor={editor} name={name} dialogId={percDialogId("inline-link", name)} onClose={() => closePanelAndRefocus("inline-link")} />
      ) : null}
      {panel === "inline-image" ? (
        <InlineImagePanel editor={editor} name={name} dialogId={percDialogId("inline-image", name)} onClose={() => closePanelAndRefocus("inline-image")} />
      ) : null}
      {panel === "inline-template" ? (
        <InlineTemplatePanel editor={editor} name={name} dialogId={percDialogId("inline-template", name)} onClose={() => closePanelAndRefocus("inline-template")} />
      ) : null}
      {panel === "insert-html" ? (
        <InsertHtmlPanel editor={editor} name={name} dialogId={percDialogId("insert-html", name)} onClose={() => closePanelAndRefocus("insert-html")} />
      ) : null}
      {panel === "global-variable" ? (
        <GlobalVariablePanel editor={editor} name={name} dialogId={percDialogId("global-variable", name)} onClose={() => closePanelAndRefocus("global-variable")} />
      ) : null}
      {panel === "source" ? (
        <SourcePanel editor={editor} name={name} dialogId={percDialogId("source", name)} onClose={() => closePanelAndRefocus("source")} />
      ) : null}
    </div>
    </TipTapRovingContext.Provider>
  );
}

interface PanelProps {
  editor: Editor | null;
  name: string;
  /** Stable dialog element id, wired to the invoking trigger's aria-controls. */
  dialogId: string;
  onClose: () => void;
}

function PanelShell({
  name,
  dialogId,
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
  const dialogRef = useRef<HTMLSpanElement | null>(null);
  // Opening a dialog moves focus into its first field (dialog contract,
  // same as the parity link editor in #6588).
  useEffect(() => {
    const first = dialogRef.current?.querySelector<HTMLElement>(
      "input, select, textarea",
    );
    first?.focus();
  }, []);
  return (
    <span
      ref={dialogRef}
      role="dialog"
      aria-label={label}
      id={dialogId}
      data-testid={`tiptap-perc-dialog-${name}`}
      // Escape anywhere in the dialog closes it; the parent onClose returns
      // focus to the invoking trigger button (dialog contract).
      onKeyDown={(e) => {
        if (e.key === "Escape") {
          e.stopPropagation();
          onClose();
        }
      }}
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

function InlineLinkPanel({ editor, name, dialogId, onClose }: PanelProps): React.ReactElement {
  const [href, setHref] = useState("");
  const [text, setText] = useState("");
  const [title, setTitle] = useState("");
  const [target, setTarget] = useState("");
  const [error, setError] = useState(false);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [resolving, setResolving] = useState(false);
  const [resolveFailed, setResolveFailed] = useState(false);
  const [managed, setManaged] = useState<ManagedLinkRef | null>(null);
  const canApply = href.trim().length > 0 && !error && !resolving;
  /** Browse trigger, for picker Escape focus-return (dialog contract). */
  const browseButtonRef = useRef<HTMLButtonElement | null>(null);

  /** Close the picker returning focus to the Browse button (#6658). */
  const closePickerAndRefocus = (): void => {
    setPickerOpen(false);
    browseButtonRef.current?.focus();
  };

  async function pickContent(selection: ContentPickerSelection): Promise<void> {
    setPickerOpen(false);
    setResolving(true);
    setResolveFailed(false);
    setError(false);
    try {
      const preview = await fetchInlineRenderLink(selection.contentId);
      const ref = resolveManagedLinkRef(preview, selection.path);
      if (!ref) {
        setResolveFailed(true);
        return;
      }
      setHref(ref.href);
      setManaged(ref);
      if (!title.trim() && ref.title) {
        setTitle(ref.title);
      }
      if (!text.trim()) {
        setText(selection.name);
      }
    } catch {
      setResolveFailed(true);
    } finally {
      setResolving(false);
    }
  }

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
          dependentId: managed?.dependentId,
          dependentVariantId: managed?.dependentVariantId,
          jcrPath: managed?.jcrPath,
        }),
      )
      .run();
    onClose();
  };

  return (
    <PanelShell
      editor={editor}
      name={name}
      dialogId={dialogId}
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
          // Manual edits after a pick revert to an external/manual link:
          // the managed reference only applies to the picked address.
          setManaged(null);
          setError(false);
          setResolveFailed(false);
        }}
      />
      <button
        type="button"
        ref={browseButtonRef}
        data-testid={`tiptap-perc-link-browse-${name}`}
        className={styles.tiptapLinkApply}
        disabled={resolving}
        onMouseDown={(e) => e.preventDefault()}
        onClick={() => setPickerOpen((open) => !open)}
      >
        {message(EDITOR_MSG.TIPTAP_PERC_BROWSE)}
      </button>
      {pickerOpen ? (
        <ContentPickerDialog
          name={name}
          mode="link"
          onPick={(selection) => void pickContent(selection)}
          onClose={closePickerAndRefocus}
        />
      ) : null}
      {resolving ? (
        <div
          className={styles.status}
          data-testid={`tiptap-perc-link-resolving-${name}`}
        >
          {message(EDITOR_MSG.TIPTAP_PERC_PICK_LOADING)}
        </div>
      ) : null}
      {resolveFailed ? (
        <span
          role="alert"
          className={styles.tiptapLinkError}
          data-testid={`tiptap-perc-link-resolve-error-${name}`}
        >
          {message(EDITOR_MSG.TIPTAP_PERC_PICK_RESOLVE_FAILED)}
        </span>
      ) : null}
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

function InlineImagePanel({ editor, name, dialogId, onClose }: PanelProps): React.ReactElement {
  const [src, setSrc] = useState("");
  const [alt, setAlt] = useState("");
  const [title, setTitle] = useState("");
  const [width, setWidth] = useState("");
  const [height, setHeight] = useState("");
  const [align, setAlign] = useState("");
  const [decorative, setDecorative] = useState(false);
  const [error, setError] = useState(false);
  const [pickerOpen, setPickerOpen] = useState(false);
  const [resolving, setResolving] = useState(false);
  const [resolveFailed, setResolveFailed] = useState(false);
  const [managed, setManaged] = useState<ManagedImageRef | null>(null);
  const canApply = src.trim().length > 0 && !error && !resolving;
  /** Browse trigger, for picker Escape focus-return (dialog contract). */
  const browseButtonRef = useRef<HTMLButtonElement | null>(null);

  /** Close the picker returning focus to the Browse button (#6658). */
  const closePickerAndRefocus = (): void => {
    setPickerOpen(false);
    browseButtonRef.current?.focus();
  };

  async function pickContent(selection: ContentPickerSelection): Promise<void> {
    setPickerOpen(false);
    setResolving(true);
    setResolveFailed(false);
    setError(false);
    try {
      const preview = await fetchInlineRenderLink(selection.contentId);
      const ref = resolveManagedImageRef(preview, selection.path);
      if (!ref) {
        setResolveFailed(true);
        return;
      }
      setSrc(ref.src);
      setManaged(ref);
      if (!alt.trim() && ref.alt) {
        setAlt(ref.alt);
      }
      if (!title.trim() && ref.title) {
        setTitle(ref.title);
      }
    } catch {
      setResolveFailed(true);
    } finally {
      setResolving(false);
    }
  }

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
          dependentId: managed?.dependentId,
          dependentVariantId: managed?.dependentVariantId,
          jcrPath: managed?.jcrPath,
        }),
      )
      .run();
    onClose();
  };

  return (
    <PanelShell
      editor={editor}
      name={name}
      dialogId={dialogId}
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
          // Manual edits after a pick revert to an external/manual image:
          // the managed reference only applies to the picked address.
          setManaged(null);
          setError(false);
          setResolveFailed(false);
        }}
      />
      <button
        type="button"
        ref={browseButtonRef}
        data-testid={`tiptap-perc-image-browse-${name}`}
        className={styles.tiptapLinkApply}
        disabled={resolving}
        onMouseDown={(e) => e.preventDefault()}
        onClick={() => setPickerOpen((open) => !open)}
      >
        {message(EDITOR_MSG.TIPTAP_PERC_BROWSE)}
      </button>
      {pickerOpen ? (
        <ContentPickerDialog
          name={name}
          mode="image"
          onPick={(selection) => void pickContent(selection)}
          onClose={closePickerAndRefocus}
        />
      ) : null}
      {resolving ? (
        <div
          className={styles.status}
          data-testid={`tiptap-perc-image-resolving-${name}`}
        >
          {message(EDITOR_MSG.TIPTAP_PERC_PICK_LOADING)}
        </div>
      ) : null}
      {resolveFailed ? (
        <span
          role="alert"
          className={styles.tiptapLinkError}
          data-testid={`tiptap-perc-image-resolve-error-${name}`}
        >
          {message(EDITOR_MSG.TIPTAP_PERC_PICK_RESOLVE_FAILED)}
        </span>
      ) : null}
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

function InlineTemplatePanel({ editor, name, dialogId, onClose }: PanelProps): React.ReactElement {
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
      dialogId={dialogId}
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

function InsertHtmlPanel({ editor, name, dialogId, onClose }: PanelProps): React.ReactElement {
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
      dialogId={dialogId}
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

function GlobalVariablePanel({ editor, name, dialogId, onClose }: PanelProps): React.ReactElement {
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
      dialogId={dialogId}
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

function SourcePanel({ editor, name, dialogId, onClose }: PanelProps): React.ReactElement {
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
      dialogId={dialogId}
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
