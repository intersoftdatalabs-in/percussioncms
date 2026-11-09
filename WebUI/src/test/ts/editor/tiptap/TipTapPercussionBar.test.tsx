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

import { Editor } from "@tiptap/core";
import {
  cleanup,
  fireEvent,
  render,
  screen,
  waitFor,
} from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { createTipTapEditorExtensions } from "../../../../main/ts/editor/tiptap/tiptapSetup";
import { TipTapPercussionBar } from "../../../../main/ts/editor/tiptap/TipTapPercussionBar";

function makeEditor(html = "<p>Hello</p>"): Editor {
  return new Editor({
    extensions: createTipTapEditorExtensions(),
    content: html,
  });
}

describe("TipTapPercussionBar", () => {
  afterEach(() => {
    cleanup();
  });

  it("renders the Percussion plugin buttons", () => {
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      expect(screen.getByTestId("editor-tiptap-perc-text")).toBeTruthy();
      for (const action of [
        "perc-inline-link",
        "perc-inline-image",
        "perc-inline-template",
        "perc-insert-html",
        "perc-global-variable",
        "perc-more-link",
        "perc-source",
      ]) {
        expect(screen.getByTestId(`tiptap-${action}-text`)).toBeTruthy();
      }
    } finally {
      editor.destroy();
    }
  });

  it("inserts a more-link marker on click", () => {
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-perc-more-link-text"));
      expect(editor.getHTML()).toContain(
        '<span class="perc-blog-more-link"></span>',
      );
    } finally {
      editor.destroy();
    }
  });

  it("inserts an inline link with CMS attributes through the dialog", () => {
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-perc-inline-link-text"));
      fireEvent.change(screen.getByTestId("tiptap-perc-link-url-text"), {
        target: { value: "/Rhythmyx/Sites/a.html" },
      });
      fireEvent.change(screen.getByTestId("tiptap-perc-link-text-text"), {
        target: { value: "About" },
      });
      fireEvent.click(screen.getByTestId("tiptap-perc-apply-text"));
      const html = editor.getHTML();
      expect(html).toContain('href="/Rhythmyx/Sites/a.html"');
      expect(html).toContain('rxinlineslot="103"');
      expect(html).toContain('inlinetype="rxhyperlink"');
      expect(html).toContain(">About</a>");
      // Dialog closes after a successful insert.
      expect(screen.queryByTestId("tiptap-perc-dialog-text")).toBeNull();
    } finally {
      editor.destroy();
    }
  });

  it("rejects unsafe inline-link addresses", () => {
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-perc-inline-link-text"));
      fireEvent.change(screen.getByTestId("tiptap-perc-link-url-text"), {
        target: { value: "javascript:alert(1)" },
      });
      fireEvent.click(screen.getByTestId("tiptap-perc-apply-text"));
      expect(screen.getByTestId("tiptap-perc-dialog-text")).toBeTruthy();
      expect(editor.getHTML()).not.toContain("javascript:");
    } finally {
      editor.destroy();
    }
  });

  it("inserts an inline template through the dialog", () => {
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-perc-inline-template-text"));
      fireEvent.change(screen.getByTestId("tiptap-perc-template-path-text"), {
        target: { value: "/Rhythmyx/Assets/t.html" },
      });
      fireEvent.change(screen.getByTestId("tiptap-perc-template-text-text"), {
        target: { value: "Promo" },
      });
      fireEvent.click(screen.getByTestId("tiptap-perc-apply-text"));
      const html = editor.getHTML();
      expect(html).toContain('inlinetype="rxvariant"');
      expect(html).toContain('rxinlineslot="105"');
    } finally {
      editor.destroy();
    }
  });

  it("inserts an inline image with dimensions through the dialog", () => {
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-perc-inline-image-text"));
      fireEvent.change(screen.getByTestId("tiptap-perc-image-src-text"), {
        target: { value: "/Rhythmyx/Assets/i.png" },
      });
      fireEvent.change(screen.getByTestId("tiptap-perc-image-alt-text"), {
        target: { value: "Pic" },
      });
      fireEvent.change(screen.getByTestId("tiptap-perc-image-width-text"), {
        target: { value: "320" },
      });
      fireEvent.click(screen.getByTestId("tiptap-perc-apply-text"));
      const html = editor.getHTML();
      expect(html).toContain('src="/Rhythmyx/Assets/i.png"');
      expect(html).toContain('width="320"');
      expect(html).toContain('inlinetype="rximage"');
    } finally {
      editor.destroy();
    }
  });

  it("inserts raw HTML through the fragment dialog", () => {
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-perc-insert-html-text"));
      fireEvent.change(screen.getByTestId("tiptap-perc-html-text"), {
        target: { value: "<p>Fragment</p>" },
      });
      fireEvent.click(screen.getByTestId("tiptap-perc-apply-text"));
      expect(editor.getHTML()).toContain("Fragment");
    } finally {
      editor.destroy();
    }
  });

  it("inserts a manually named global variable", () => {
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-perc-global-variable-text"));
      fireEvent.change(screen.getByTestId("tiptap-perc-variable-name-text"), {
        target: { value: "company_name" },
      });
      fireEvent.click(screen.getByTestId("tiptap-perc-apply-text"));
      expect(editor.getHTML()).toContain(
        '<span class="perc-global-variable-marker" title="company_name">',
      );
    } finally {
      editor.destroy();
    }
  });

  it("writes source edits back through the schema", () => {
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-perc-source-text"));
      const area = screen.getByTestId("tiptap-perc-source-content-text");
      expect((area as HTMLTextAreaElement).value).toContain("Hello");
      fireEvent.change(area, { target: { value: "<p>Edited</p>" } });
      fireEvent.click(screen.getByTestId("tiptap-perc-apply-text"));
      expect(editor.getHTML()).toBe("<p>Edited</p>");
    } finally {
      editor.destroy();
    }
  });

  it("picks CMS content through the link picker with managed attributes", async () => {
    vi.spyOn(global, "fetch").mockImplementation((input: Parameters<typeof fetch>[0]) => {
      const url = String(input ?? "");
      if (url.includes("renderlink/preview/")) {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              InlineRenderLink: {
                sys_dependentid: 77,
                sys_dependentvariantid: 512,
                url: "/Sites/About",
                title: "About us",
              },
            }),
            { status: 200, headers: { "Content-Type": "application/json" } },
          ),
        );
      }
      return Promise.resolve(
        new Response(
          JSON.stringify({
            PathItem: [
              { id: "77", name: "About", path: "/Sites/About", type: "page" },
            ],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      );
    });
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-perc-inline-link-text"));
      fireEvent.click(screen.getByTestId("tiptap-perc-link-browse-text"));
      await waitFor(() => {
        expect(
          screen.getByTestId("tiptap-content-picker-text"),
        ).toBeTruthy();
      });
      await waitFor(() => {
        expect(
          screen.getAllByTestId("tiptap-content-picker-row-text"),
        ).toHaveLength(1);
      });
      fireEvent.click(
        screen.getAllByTestId("tiptap-content-picker-row-text")[0],
      );
      fireEvent.click(screen.getByTestId("tiptap-content-picker-use-text"));
      await waitFor(() => {
        expect(
          screen.queryByTestId("tiptap-perc-link-resolving-text"),
        ).toBeNull();
      });
      // Pick fills the managed address, title, and link text.
      expect(
        (screen.getByTestId("tiptap-perc-link-url-text") as HTMLInputElement)
          .value,
      ).toBe("/Sites/About");
      fireEvent.click(screen.getByTestId("tiptap-perc-apply-text"));
      const html = editor.getHTML();
      expect(html).toContain('href="/Sites/About"');
      expect(html).toContain('sys_dependentid="77"');
      expect(html).toContain('sys_dependentvariantid="512"');
      expect(html).toContain('data-jcrpath="/Sites/About"');
      expect(html).toContain('rxinlineslot="103"');
      expect(html).toContain('inlinetype="rxhyperlink"');
    } finally {
      vi.restoreAllMocks();
      editor.destroy();
    }
  });

  it("picks CMS content through the image picker with managed attributes", async () => {
    vi.spyOn(global, "fetch").mockImplementation((input: Parameters<typeof fetch>[0]) => {
      const url = String(input ?? "");
      if (url.includes("renderlink/preview/")) {
        return Promise.resolve(
          new Response(
            JSON.stringify({
              InlineRenderLink: {
                sys_dependentid: 88,
                url: "/Assets/Images/logo.png",
                altText: "Company logo",
                title: "Logo",
              },
            }),
            { status: 200, headers: { "Content-Type": "application/json" } },
          ),
        );
      }
      return Promise.resolve(
        new Response(
          JSON.stringify({
            PathItem: [
              {
                id: "88",
                name: "logo.png",
                path: "/Assets/Images/logo.png",
                type: "asset",
              },
            ],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      );
    });
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-perc-inline-image-text"));
      fireEvent.click(screen.getByTestId("tiptap-perc-image-browse-text"));
      await waitFor(() => {
        expect(
          screen.getAllByTestId("tiptap-content-picker-row-text"),
        ).toHaveLength(1);
      });
      fireEvent.click(
        screen.getAllByTestId("tiptap-content-picker-row-text")[0],
      );
      fireEvent.click(screen.getByTestId("tiptap-content-picker-use-text"));
      await waitFor(() => {
        expect(
          screen.queryByTestId("tiptap-perc-image-resolving-text"),
        ).toBeNull();
      });
      expect(
        (screen.getByTestId("tiptap-perc-image-src-text") as HTMLInputElement)
          .value,
      ).toBe("/Assets/Images/logo.png");
      fireEvent.click(screen.getByTestId("tiptap-perc-apply-text"));
      const html = editor.getHTML();
      expect(html).toContain('src="/Assets/Images/logo.png"');
      expect(html).toContain('alt="Company logo"');
      expect(html).toContain('sys_dependentid="88"');
      expect(html).toContain('data-jcrpath="/Assets/Images/logo.png"');
      expect(html).toContain('inlinetype="rximage"');
    } finally {
      vi.restoreAllMocks();
      editor.destroy();
    }
  });

  it("keeps manual URL entry when the managed lookup fails", async () => {
    vi.spyOn(global, "fetch").mockImplementation((input: Parameters<typeof fetch>[0]) => {
      const url = String(input ?? "");
      if (url.includes("renderlink/preview/")) {
        return Promise.resolve(
          new Response("boom", {
            status: 500,
            headers: { "Content-Type": "application/json" },
          }),
        );
      }
      return Promise.resolve(
        new Response(
          JSON.stringify({
            PathItem: [
              { id: "77", name: "About", path: "/Sites/About", type: "page" },
            ],
          }),
          { status: 200, headers: { "Content-Type": "application/json" } },
        ),
      );
    });
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      fireEvent.click(screen.getByTestId("tiptap-perc-inline-link-text"));
      fireEvent.click(screen.getByTestId("tiptap-perc-link-browse-text"));
      await waitFor(() => {
        expect(
          screen.getAllByTestId("tiptap-content-picker-row-text"),
        ).toHaveLength(1);
      });
      fireEvent.click(
        screen.getAllByTestId("tiptap-content-picker-row-text")[0],
      );
      fireEvent.click(screen.getByTestId("tiptap-content-picker-use-text"));
      await waitFor(() => {
        expect(
          screen.getByTestId("tiptap-perc-link-resolve-error-text"),
        ).toBeTruthy();
      });
      // Manual entry still works after the failure.
      fireEvent.change(screen.getByTestId("tiptap-perc-link-url-text"), {
        target: { value: "https://example.com/manual" },
      });
      fireEvent.change(screen.getByTestId("tiptap-perc-link-text-text"), {
        target: { value: "Manual" },
      });
      fireEvent.click(screen.getByTestId("tiptap-perc-apply-text"));
      const html = editor.getHTML();
      expect(html).toContain('href="https://example.com/manual"');
      expect(html).not.toContain("sys_dependentid");
    } finally {
      vi.restoreAllMocks();
      editor.destroy();
    }
  });

  it("opens the inline-link dialog with the classic shortcut", () => {
    const editor = makeEditor();
    try {
      render(<TipTapPercussionBar editor={editor} name="text" />);
      expect(screen.queryByTestId("tiptap-perc-dialog-text")).toBeNull();
      fireEvent.keyDown(editor.view.dom, {
        key: "l",
        ctrlKey: true,
        shiftKey: true,
      });
      expect(screen.getByTestId("tiptap-perc-dialog-text")).toBeTruthy();
      expect(
        screen.getByTestId("tiptap-perc-link-url-text"),
      ).toBeTruthy();
    } finally {
      editor.destroy();
    }
  });
});
