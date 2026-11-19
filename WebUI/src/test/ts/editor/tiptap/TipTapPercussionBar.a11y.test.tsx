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
 * Percussion plugin-bar keyboard + screen-reader evidence (#6589).
 *
 * <p>Proves the WCAG 2.2 AA keyboard bar for the second editor toolbar: a
 * single tab stop (roving tabindex over the plugin bar's own slot, so Tab
 * enters each editor toolbar exactly once), WAI-APG Left/Right/Home/End
 * movement, and the dialog contract on all six plugin dialogs (inline link,
 * inline image, inline template, insert HTML, global variable, source view):
 * each opens as a labelled {@code role="dialog"}, moves focus into its
 * first field, and closes on Escape returning focus to the invoking button.
 * The axe gate pins zero serious/critical violations on the bar with a
 * dialog open; Playwright
 * ({@code editor-host-tiptap-a11y.spec.js}) carries the same bar against
 * the live H2 CMS.</p>
 */

import { Editor } from "@tiptap/core";
import { act, cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { createTipTapEditorExtensions } from "../../../../main/ts/editor/tiptap/tiptapSetup";
import { TipTapPercussionBar } from "../../../../main/ts/editor/tiptap/TipTapPercussionBar";
import { renderA11yGate } from "../../contentExplorer/a11y";

function makeEditor(html = "<p>Hello</p>"): Editor {
  return new Editor({
    extensions: createTipTapEditorExtensions(),
    content: html,
  });
}

function percBar(container: HTMLElement): HTMLElement {
  const bar = container.querySelector<HTMLElement>(
    '[data-testid="editor-tiptap-perc-text"]',
  );
  if (!bar) {
    throw new Error("perc bar not rendered");
  }
  return bar;
}

/** The six dialog panels: trigger test id, first-field test id, dialog id. */
const DIALOGS = [
  {
    panel: "inline-link",
    trigger: "tiptap-perc-inline-link-text",
    firstField: "tiptap-perc-link-url-text",
    dialogId: "tiptap-perc-dialog-inline-link-text",
    label: "Insert inline link",
  },
  {
    panel: "inline-image",
    trigger: "tiptap-perc-inline-image-text",
    firstField: "tiptap-perc-image-src-text",
    dialogId: "tiptap-perc-dialog-inline-image-text",
    label: "Insert inline image",
  },
  {
    panel: "inline-template",
    trigger: "tiptap-perc-inline-template-text",
    firstField: "tiptap-perc-template-path-text",
    dialogId: "tiptap-perc-dialog-inline-template-text",
    label: "Insert inline template",
  },
  {
    panel: "insert-html",
    trigger: "tiptap-perc-insert-html-text",
    firstField: "tiptap-perc-html-text",
    dialogId: "tiptap-perc-dialog-insert-html-text",
    label: "Insert HTML fragment",
  },
  {
    panel: "global-variable",
    trigger: "tiptap-perc-global-variable-text",
    firstField: "tiptap-perc-variable-name-text",
    dialogId: "tiptap-perc-dialog-global-variable-text",
    label: "Insert global variable",
  },
  {
    panel: "source",
    trigger: "tiptap-perc-source-text",
    firstField: "tiptap-perc-source-content-text",
    dialogId: "tiptap-perc-dialog-source-text",
    label: "View HTML source",
  },
] as const;

describe("TipTapPercussionBar a11y evidence (#6589)", () => {
  afterEach(() => {
    cleanup();
  });

  it("passes the axe gate with no dialog open", async () => {
    const editor = makeEditor();
    try {
      const { container } = render(
        <TipTapPercussionBar editor={editor} name="text" />,
      );
      await renderA11yGate(container);
    } finally {
      editor.destroy();
    }
  });

  it("passes the axe gate with a dialog open", async () => {
    const editor = makeEditor();
    try {
      const { container } = render(
        <TipTapPercussionBar editor={editor} name="text" />,
      );
      fireEvent.click(screen.getByTestId("tiptap-perc-inline-link-text"));
      await renderA11yGate(percBar(container));
    } finally {
      editor.destroy();
    }
  });

  it("keeps exactly one tab stop (roving tabindex)", () => {
    const editor = makeEditor();
    try {
      const { container } = render(
        <TipTapPercussionBar editor={editor} name="text" />,
      );
      const buttons = Array.from(
        percBar(container).querySelectorAll<HTMLElement>(
          'button[data-testid^="tiptap-perc-"]',
        ),
      );
      expect(buttons).toHaveLength(7);
      const zeroStops = buttons.filter(
        (el) => el.getAttribute("tabindex") === "0",
      );
      expect(zeroStops.map((el) => el.getAttribute("data-testid"))).toEqual([
        "tiptap-perc-inline-link-text",
      ]);
    } finally {
      editor.destroy();
    }
  });

  it("moves focus with arrows and Home/End", () => {
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      const first = screen.getByTestId("tiptap-perc-inline-link-text");
      act(() => {
        first.focus();
      });
      expect(document.activeElement).toBe(first);
      fireEvent.keyDown(document.activeElement!, { key: "ArrowRight" });
      expect(document.activeElement).toBe(
        screen.getByTestId("tiptap-perc-inline-image-text"),
      );
      fireEvent.keyDown(document.activeElement!, { key: "ArrowLeft" });
      expect(document.activeElement).toBe(first);
      fireEvent.keyDown(document.activeElement!, { key: "End" });
      expect(document.activeElement).toBe(
        screen.getByTestId("tiptap-perc-source-text"),
      );
      fireEvent.keyDown(document.activeElement!, { key: "Home" });
      expect(document.activeElement).toBe(first);
    } finally {
      editor.destroy();
    }
  });

  it("adopts the roving slot when a control takes focus directly", () => {
    const editor = makeEditor();
    try {
      const { container } = render(
        <TipTapPercussionBar editor={editor} name="text" />,
      );
      const source = screen.getByTestId("tiptap-perc-source-text");
      act(() => {
        source.focus();
      });
      const zeroStops = Array.from(
        percBar(container).querySelectorAll<HTMLElement>("button"),
      ).filter((el) => el.getAttribute("tabindex") === "0");
      expect(zeroStops).toEqual([source]);
    } finally {
      editor.destroy();
    }
  });

  it.each(DIALOGS)(
    "opens the $panel dialog with focus-in and Escape focus-return",
    ({ trigger, firstField, dialogId, label }) => {
      const editor = makeEditor();
      try {
        render(<TipTapPercussionBar editor={editor} name="text" />);
        const triggerButton = screen.getByTestId(trigger);
        fireEvent.click(triggerButton);
        const dialog = screen.getByRole("dialog");
        expect(dialog.getAttribute("id")).toBe(dialogId);
        expect(dialog.getAttribute("aria-label")).toBe(label);
        expect(triggerButton.getAttribute("aria-expanded")).toBe("true");
        expect(triggerButton.getAttribute("aria-controls")).toBe(dialogId);
        // Focus-in: the first dialog field takes focus on open.
        expect(document.activeElement).toBe(screen.getByTestId(firstField));
        // Escape closes the dialog and returns focus to the trigger.
        fireEvent.keyDown(document.activeElement!, { key: "Escape" });
        expect(screen.queryByRole("dialog")).toBeNull();
        expect(triggerButton.getAttribute("aria-expanded")).toBe("false");
        expect(document.activeElement).toBe(triggerButton);
      } finally {
        editor.destroy();
      }
    },
  );

  it("leaves text caret keys alone inside a dialog field", () => {
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-perc-insert-html-text"));
      const area = screen.getByTestId("tiptap-perc-html-text");
      expect(document.activeElement).toBe(area);
      fireEvent.keyDown(area, { key: "ArrowLeft" });
      // Caret keys in the dialog never move toolbar focus.
      expect(document.activeElement).toBe(area);
      expect(screen.getByRole("dialog")).toBeTruthy();
    } finally {
      editor.destroy();
    }
  });

  it("Browse sub-dialog moves focus in; Escape closes only the picker (#6658)", async () => {
    vi.spyOn(global, "fetch").mockImplementation(() =>
      Promise.resolve(
        new Response(
          JSON.stringify({
            PathItem: [
              { id: "77", name: "About", path: "/Sites/About", type: "page" },
            ],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      ),
    );
    const editor = makeEditor();
    try {
      const { container } = render(
        <TipTapPercussionBar editor={editor} name="text" />,
      );
      fireEvent.click(screen.getByTestId("tiptap-perc-inline-link-text"));
      const browse = screen.getByTestId("tiptap-perc-link-browse-text");
      fireEvent.click(browse);
      await waitFor(() => {
        expect(screen.getByTestId("tiptap-content-picker-text")).toBeTruthy();
      });
      // Focus-in: the first picker field takes focus on open.
      expect(document.activeElement).toBe(
        screen.getByTestId("tiptap-content-picker-path-text"),
      );
      // Two nested dialogs are open; the widget stays axe-green.
      expect(screen.getAllByRole("dialog")).toHaveLength(2);
      await renderA11yGate(percBar(container));
      // Escape closes only the picker and returns focus to Browse.
      fireEvent.keyDown(document.activeElement!, { key: "Escape" });
      expect(screen.queryByTestId("tiptap-content-picker-text")).toBeNull();
      // The host plugin dialog stays open (single dialog again).
      expect(screen.getAllByRole("dialog")).toHaveLength(1);
      expect(screen.getByRole("dialog").getAttribute("id")).toBe(
        "tiptap-perc-dialog-inline-link-text",
      );
      expect(document.activeElement).toBe(browse);
    } finally {
      vi.restoreAllMocks();
      editor.destroy();
    }
  });
});
