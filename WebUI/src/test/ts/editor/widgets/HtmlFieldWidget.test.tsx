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

import { cleanup, fireEvent, render, screen } from "@testing-library/react";
import React from "react";
import { afterEach, describe, expect, it, vi } from "vitest";
import { HtmlFieldWidget } from "../../../../main/ts/editor/widgets/HtmlFieldWidget";

interface FakeTinyMceEditor {
  content: string;
  removed: boolean;
  handlers: Record<string, () => void>;
  getContent(): string;
  setContent(html: string): void;
  remove(): void;
  setMode(mode: string): void;
  on(event: string, handler: () => void): void;
}

function createFakeTinyMceEditor(initial: string): FakeTinyMceEditor {
  return {
    content: initial,
    removed: false,
    handlers: {},
    getContent() {
      return this.content;
    },
    setContent(html: string) {
      this.content = html;
    },
    remove() {
      this.removed = true;
    },
    setMode() {
      /* parity stub */
    },
    on(event: string, handler: () => void) {
      this.handlers[event] = handler;
    },
  };
}

function installFakeTinyMce(initial: string): {
  initOpts: Record<string, unknown>[];
  editors: FakeTinyMceEditor[];
} {
  const installed: {
    initOpts: Record<string, unknown>[];
    editors: FakeTinyMceEditor[];
  } = { initOpts: [], editors: [] };
  const fake = {
    init(opts: Record<string, unknown>) {
      installed.initOpts.push(opts);
      const ed = createFakeTinyMceEditor(initial);
      installed.editors.push(ed);
      (opts["setup"] as (ed: FakeTinyMceEditor) => void)(ed);
      return Promise.resolve(installed.editors);
    },
    get() {
      return undefined;
    },
  };
  (window as unknown as { tinymce?: object }).tinymce = fake;
  return installed;
}

function clearFakeTinyMce(): void {
  delete (window as unknown as { tinymce?: object }).tinymce;
}

function fireEditorChange(ed: FakeTinyMceEditor): void {
  const handler = ed.handlers["change"];
  if (handler) {
    handler();
  }
}

describe("HtmlFieldWidget", () => {
  afterEach(() => {
    cleanup();
    clearFakeTinyMce();
  });

  it("falls back to a textarea and reports edits", () => {
    const onChange = vi.fn();
    render(
      <HtmlFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={false}
        onChange={onChange}
        engine="tinymce"
        loadScript={async () => {
          /* do not attach TinyMCE in unit tests */
        }}
      />,
    );
    const area = screen.getByTestId("editor-field-text") as HTMLTextAreaElement;
    expect(area.value).toBe("<p>Hi</p>");
    expect(area.getAttribute("data-editor-kind")).toBe("html");
    fireEvent.change(area, { target: { value: "<p>Bye</p>" } });
    expect(onChange).toHaveBeenCalledWith("<p>Bye</p>");
  });

  it("is read-only in view mode", () => {
    render(
      <HtmlFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={true}
        onChange={vi.fn()}
        engine="tinymce"
      />,
    );
    const area = screen.getByTestId("editor-field-text") as HTMLTextAreaElement;
    expect(area.readOnly).toBe(true);
  });

  it("marks the textarea invalid when the editor refuses the HTML", () => {
    render(
      <HtmlFieldWidget
        name="text"
        value={'<script>alert(1)</script>'}
        readOnly={false}
        invalid
        onChange={vi.fn()}
        engine="tinymce"
        loadScript={async () => {
          /* do not attach TinyMCE in unit tests */
        }}
      />,
    );
    const area = screen.getByTestId("editor-field-text") as HTMLTextAreaElement;
    expect(area.getAttribute("aria-invalid")).toBe("true");
  });

  it("renders TipTap by default with no engine flag (#5758)", async () => {
    render(
      <HtmlFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={false}
        onChange={vi.fn()}
      />,
    );
    const editable = await screen.findByTestId("editor-field-text");
    expect(editable.getAttribute("data-editor-kind")).toBe("html");
    expect(editable.getAttribute("data-editor-engine")).toBe("tiptap");
  });

  it("initializes TinyMCE against the textarea and forwards editor changes (#5795)", async () => {
    const installed = installFakeTinyMce("<p>Hi</p>");
    const onChange = vi.fn();
    render(
      <HtmlFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={false}
        onChange={onChange}
        engine="tinymce"
        loadScript={async () => {
          /* fake TinyMCE is pre-installed above */
        }}
      />,
    );
    const area = await screen.findByTestId("editor-field-text");
    expect(installed.initOpts).toHaveLength(1);
    expect(installed.initOpts[0]?.["target"]).toBe(area);
    const ed = installed.editors[0];
    if (!ed) {
      throw new Error("expected TinyMCE init to create an editor");
    }
    ed.content = "<p>Edited in editor</p>";
    fireEditorChange(ed);
    expect(onChange).toHaveBeenCalledWith("<p>Edited in editor</p>");
  });

  it("syncs external value changes into the TinyMCE editor (#5795)", async () => {
    const installed = installFakeTinyMce("<p>One</p>");
    const loadScript = async () => {
      /* fake TinyMCE is pre-installed above */
    };
    const { rerender } = render(
      <HtmlFieldWidget
        name="text"
        value="<p>One</p>"
        readOnly={false}
        onChange={vi.fn()}
        engine="tinymce"
        loadScript={loadScript}
      />,
    );
    await screen.findByTestId("editor-field-text");
    const ed = installed.editors[0];
    if (!ed) {
      throw new Error("expected TinyMCE init to create an editor");
    }
    rerender(
      <HtmlFieldWidget
        name="text"
        value="<p>Two</p>"
        readOnly={false}
        onChange={vi.fn()}
        engine="tinymce"
        loadScript={loadScript}
      />,
    );
    expect(ed.content).toBe("<p>Two</p>");
  });

  it("falls back to the textarea when the TinyMCE script fails to load (#5795)", async () => {
    const onChange = vi.fn();
    render(
      <HtmlFieldWidget
        name="text"
        value="<p>Hi</p>"
        readOnly={false}
        onChange={onChange}
        engine="tinymce"
        loadScript={async () => {
          throw new Error("script unavailable");
        }}
      />,
    );
    const area = (await screen.findByTestId(
      "editor-field-text",
    )) as HTMLTextAreaElement;
    fireEvent.change(area, { target: { value: "<p>Typed</p>" } });
    expect(onChange).toHaveBeenCalledWith("<p>Typed</p>");
  });
});
