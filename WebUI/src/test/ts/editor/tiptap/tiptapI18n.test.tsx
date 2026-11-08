/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * TipTap editor strings follow the session locale (#5622 slice #5732).
 *
 * <p>Toolbar ({@code TipTapToolbar}) and Percussion plugin dialogs
 * ({@code TipTapPercussionBar}) resolve every user-visible string through the
 * TMX {@code message()} catalog. With no catalog they render the English
 * fallback (never a raw key, never blank); with a session-locale catalog
 * (stubbed {@code window.I18N}, standing in for
 * {@code tmx.jsp?mode=js&prefix=perc.ui.&sys_lang=...}) they render the
 * locale strings — including the dialog option labels that used to be
 * hardcoded English (link target, image width/height/alignment).</p>
 */

import { Editor } from "@tiptap/core";
import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it } from "vitest";
import { EDITOR_MSG } from "../../../../main/ts/editor/messages";
import { TipTapPercussionBar } from "../../../../main/ts/editor/tiptap/TipTapPercussionBar";
import { TipTapToolbar } from "../../../../main/ts/editor/tiptap/TipTapToolbar";
import {
  __resetMessageTrackingForTests,
  message,
} from "../../../../main/ts/i18n/message";
import { createTipTapEditorExtensions } from "../../../../main/ts/editor/tiptap/tiptapSetup";

type I18NStub = { message: (key: string) => string };

function setCatalog(translations: Record<string, string>): void {
  __resetMessageTrackingForTests();
  (window as unknown as { I18N: I18NStub }).I18N = {
    message: (key: string) => translations[key] ?? key,
  };
}

function clearCatalog(): void {
  delete (window as unknown as { I18N?: unknown }).I18N;
  __resetMessageTrackingForTests();
}

/** Every key the TipTap toolbar + dialogs render. */
const TIPTAP_KEYS = [
  EDITOR_MSG.HTML_LABEL,
  EDITOR_MSG.TABLE_ADD_ROW,
  EDITOR_MSG.TABLE_REMOVE_ROW,
  EDITOR_MSG.TIPTAP_TOOLBAR,
  EDITOR_MSG.TIPTAP_UNDO,
  EDITOR_MSG.TIPTAP_REDO,
  EDITOR_MSG.TIPTAP_BOLD,
  EDITOR_MSG.TIPTAP_ITALIC,
  EDITOR_MSG.TIPTAP_UNDERLINE,
  EDITOR_MSG.TIPTAP_STYLE,
  EDITOR_MSG.TIPTAP_STYLE_PARAGRAPH,
  EDITOR_MSG.TIPTAP_ALIGN_LEFT,
  EDITOR_MSG.TIPTAP_ALIGN_CENTER,
  EDITOR_MSG.TIPTAP_ALIGN_RIGHT,
  EDITOR_MSG.TIPTAP_ALIGN_JUSTIFY,
  EDITOR_MSG.TIPTAP_BULLET_LIST,
  EDITOR_MSG.TIPTAP_NUMBERED_LIST,
  EDITOR_MSG.TIPTAP_OUTDENT,
  EDITOR_MSG.TIPTAP_INDENT,
  EDITOR_MSG.TIPTAP_LINK,
  EDITOR_MSG.TIPTAP_UNLINK,
  EDITOR_MSG.TIPTAP_LINK_URL,
  EDITOR_MSG.TIPTAP_LINK_APPLY,
  EDITOR_MSG.TIPTAP_LINK_CLOSE,
  EDITOR_MSG.TIPTAP_LINK_UNSAFE,
  EDITOR_MSG.TIPTAP_CODE_INLINE,
  EDITOR_MSG.TIPTAP_CODE_BLOCK,
  EDITOR_MSG.TIPTAP_TABLE_INSERT,
  EDITOR_MSG.TIPTAP_TABLE_ADD_COLUMN,
  EDITOR_MSG.TIPTAP_TABLE_REMOVE_COLUMN,
  EDITOR_MSG.TIPTAP_TABLE_DELETE,
  EDITOR_MSG.TIPTAP_PERC_GROUP,
  EDITOR_MSG.TIPTAP_PERC_INLINE_LINK,
  EDITOR_MSG.TIPTAP_PERC_INLINE_IMAGE,
  EDITOR_MSG.TIPTAP_PERC_INLINE_TEMPLATE,
  EDITOR_MSG.TIPTAP_PERC_INSERT_HTML,
  EDITOR_MSG.TIPTAP_PERC_GLOBAL_VARIABLE,
  EDITOR_MSG.TIPTAP_PERC_MORE_LINK,
  EDITOR_MSG.TIPTAP_PERC_SOURCE,
  EDITOR_MSG.TIPTAP_PERC_CLOSE,
  EDITOR_MSG.TIPTAP_PERC_APPLY,
  EDITOR_MSG.TIPTAP_PERC_LINK_URL,
  EDITOR_MSG.TIPTAP_PERC_LINK_TEXT,
  EDITOR_MSG.TIPTAP_PERC_LINK_TITLE,
  EDITOR_MSG.TIPTAP_PERC_LINK_TARGET,
  EDITOR_MSG.TIPTAP_PERC_LINK_TARGET_SAME,
  EDITOR_MSG.TIPTAP_PERC_LINK_TARGET_BLANK,
  EDITOR_MSG.TIPTAP_PERC_LINK_TARGET_PARENT,
  EDITOR_MSG.TIPTAP_PERC_LINK_TARGET_TOP,
  EDITOR_MSG.TIPTAP_PERC_IMAGE_SRC,
  EDITOR_MSG.TIPTAP_PERC_IMAGE_ALT,
  EDITOR_MSG.TIPTAP_PERC_IMAGE_DECORATIVE,
  EDITOR_MSG.TIPTAP_PERC_IMAGE_WIDTH,
  EDITOR_MSG.TIPTAP_PERC_IMAGE_HEIGHT,
  EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN,
  EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_NONE,
  EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_LEFT,
  EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_RIGHT,
  EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_TOP,
  EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_MIDDLE,
  EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_BOTTOM,
  EDITOR_MSG.TIPTAP_PERC_TEMPLATE_PATH,
  EDITOR_MSG.TIPTAP_PERC_TEMPLATE_TEXT,
  EDITOR_MSG.TIPTAP_PERC_HTML_CONTENT,
  EDITOR_MSG.TIPTAP_PERC_VARIABLE_NAME,
  EDITOR_MSG.TIPTAP_PERC_SOURCE_CONTENT,
  EDITOR_MSG.TIPTAP_PERC_REQUIRED,
];

function makeEditor(html = "<p>Hello</p>"): Editor {
  return new Editor({
    extensions: createTipTapEditorExtensions(),
    content: html,
  });
}

describe("TipTap i18n session locale", () => {
  afterEach(() => {
    cleanup();
    clearCatalog();
  });

  it("falls back to English (never blank, never a raw key) without a catalog", () => {
    clearCatalog();
    for (const key of TIPTAP_KEYS) {
      const text = message(key);
      expect(text.trim().length).toBeGreaterThan(0);
      expect(text).not.toBe(key);
      expect(text).not.toContain("perc.ui.editor@");
    }
  });

  it("resolves toolbar + dialog strings from the session-locale catalog", () => {
    setCatalog({
      [EDITOR_MSG.TIPTAP_TOOLBAR]: "Formatierungsleiste",
      [EDITOR_MSG.TIPTAP_BOLD]: "Fett",
      [EDITOR_MSG.TIPTAP_PERC_LINK_TARGET_SAME]: "Gleiches Fenster",
      [EDITOR_MSG.TIPTAP_PERC_IMAGE_WIDTH]: "Breite",
    });
    expect(message(EDITOR_MSG.TIPTAP_TOOLBAR)).toBe("Formatierungsleiste");
    expect(message(EDITOR_MSG.TIPTAP_BOLD)).toBe("Fett");
    expect(message(EDITOR_MSG.TIPTAP_PERC_LINK_TARGET_SAME)).toBe(
      "Gleiches Fenster",
    );
    expect(message(EDITOR_MSG.TIPTAP_PERC_IMAGE_WIDTH)).toBe("Breite");
    // Untranslated keys still fall back to English, never blank.
    expect(message(EDITOR_MSG.TIPTAP_ITALIC)).toBe("Italic");
  });

  it("toolbar accessible name follows the session locale", () => {
    setCatalog({ [EDITOR_MSG.TIPTAP_TOOLBAR]: "Formatierungsleiste" });
    render(<TipTapToolbar editor={null} name="text" />);
    expect(
      screen.getByTestId("editor-tiptap-toolbar-text").getAttribute("aria-label"),
    ).toBe("Formatierungsleiste");
  });

  it("inline-link target options follow the session locale", () => {
    setCatalog({
      [EDITOR_MSG.TIPTAP_PERC_LINK_TARGET]: "Ziel",
      [EDITOR_MSG.TIPTAP_PERC_LINK_TARGET_SAME]: "Gleiches Fenster",
      [EDITOR_MSG.TIPTAP_PERC_LINK_TARGET_BLANK]: "Neues Fenster",
      [EDITOR_MSG.TIPTAP_PERC_LINK_TARGET_PARENT]: "Übergeordnetes Fenster",
      [EDITOR_MSG.TIPTAP_PERC_LINK_TARGET_TOP]: "Oberstes Fenster",
    });
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-perc-inline-link-text"));
      const select = screen.getByTestId("tiptap-perc-link-target-text");
      expect(select.getAttribute("aria-label")).toBe("Ziel");
      const options = Array.from(select.querySelectorAll("option")).map(
        (o) => o.textContent,
      );
      expect(options).toEqual([
        "Gleiches Fenster",
        "Neues Fenster",
        "Übergeordnetes Fenster",
        "Oberstes Fenster",
      ]);
    } finally {
      editor.destroy();
    }
  });

  it("inline-image dimension/alignment strings follow the session locale", () => {
    setCatalog({
      [EDITOR_MSG.TIPTAP_PERC_IMAGE_WIDTH]: "Breite",
      [EDITOR_MSG.TIPTAP_PERC_IMAGE_HEIGHT]: "Höhe",
      [EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN]: "Ausrichtung",
      [EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_NONE]: "Keine Ausrichtung",
      [EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_LEFT]: "Links",
      [EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_RIGHT]: "Rechts",
      [EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_TOP]: "Oben",
      [EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_MIDDLE]: "Mitte",
      [EDITOR_MSG.TIPTAP_PERC_IMAGE_ALIGN_BOTTOM]: "Unten",
    });
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-perc-inline-image-text"));
      expect(
        screen.getByTestId("tiptap-perc-image-width-text").getAttribute("aria-label"),
      ).toBe("Breite");
      expect(
        screen.getByTestId("tiptap-perc-image-height-text").getAttribute("aria-label"),
      ).toBe("Höhe");
      const select = screen.getByTestId("tiptap-perc-image-align-text");
      expect(select.getAttribute("aria-label")).toBe("Ausrichtung");
      const options = Array.from(select.querySelectorAll("option")).map(
        (o) => o.textContent,
      );
      expect(options).toEqual([
        "Keine Ausrichtung",
        "Links",
        "Rechts",
        "Oben",
        "Mitte",
        "Unten",
      ]);
    } finally {
      editor.destroy();
    }
  });

  it("dialogs fall back to English without a catalog", () => {
    clearCatalog();
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-perc-inline-link-text"));
      const options = Array.from(
        screen
          .getByTestId("tiptap-perc-link-target-text")
          .querySelectorAll("option"),
      ).map((o) => o.textContent);
      expect(options).toEqual([
        "Same window",
        "New window",
        "Parent window",
        "Top window",
      ]);
    } finally {
      editor.destroy();
    }
  });
});
