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
import { describe, expect, it } from "vitest";
import {
  PERC_INLINE_TYPE_LINK,
  buildGlobalVariableHtml,
  buildPercInlineImageHtml,
  buildPercInlineLinkHtml,
  escapePercAttr,
  isMoreLinkElement,
  parseGlobalVariableMarker,
  preparePercussionHtml,
  readGlobalVariableData,
} from "../../../../main/ts/editor/tiptap/tiptapPercussion";
import { roundTripHtmlFragment } from "../../../../main/ts/editor/tiptap/tiptapRoundTrip";
import { createTipTapEditorExtensions } from "../../../../main/ts/editor/tiptap/tiptapSetup";

function makeEditor(html = "<p>Hello</p>"): Editor {
  return new Editor({
    extensions: createTipTapEditorExtensions(),
    content: html,
  });
}

function parseFirstElement(html: string): Element {
  const doc = new DOMParser().parseFromString(html, "text/html");
  const el = doc.body.firstElementChild;
  if (!el) {
    throw new Error(`no element in ${html}`);
  }
  return el;
}

describe("tiptapPercussion stored-markup helpers", () => {
  it("escapes attribute values", () => {
    expect(escapePercAttr('a"b&c<d>e')).toBe("a&quot;b&amp;c&lt;d&gt;e");
    expect(escapePercAttr(null)).toBe("");
  });

  it("builds an inline link with the CMS slot attributes", () => {
    expect(
      buildPercInlineLinkHtml({
        href: "/Rhythmyx/Sites/a.html",
        text: "About",
        title: "About",
        target: "_blank",
        slot: "103",
        selectedText: "About",
      }),
    ).toBe(
      '<a href="/Rhythmyx/Sites/a.html" title="About" target="_blank" ' +
        'rxinlineslot="103" inlinetype="rxhyperlink" rxselectedtext="About">About</a>',
    );
  });

  it("omits empty CMS attributes on ordinary links", () => {
    expect(
      buildPercInlineLinkHtml({ href: "https://example.com", text: "X" }),
    ).toBe(
      '<a href="https://example.com" inlinetype="rxhyperlink" ' +
        'rxselectedtext="X">X</a>',
    );
  });

  it("builds an inline image with dimensions and alignment", () => {
    expect(
      buildPercInlineImageHtml({
        src: "/Rhythmyx/Assets/i.png",
        alt: "Pic",
        width: "320",
        height: "200",
        align: "left",
        slot: "104",
        dependentId: "9",
      }),
    ).toBe(
      '<img src="/Rhythmyx/Assets/i.png" alt="Pic" width="320" height="200" ' +
        'style="float:left;" rxinlineslot="104" inlinetype="rximage" ' +
        'sys_dependentid="9">',
    );
  });

  it("forces an empty alt for decorative images", () => {
    const html = buildPercInlineImageHtml({
      src: "/Rhythmyx/Assets/i.png",
      alt: "Pic",
      title: "T",
      decorative: true,
    });
    expect(html).toContain('alt=""');
    expect(html).not.toContain("Pic");
    expect(html).not.toContain('title="T"');
  });

  it("builds the stored global-variable span", () => {
    expect(buildGlobalVariableHtml("company_name", "Acme")).toBe(
      '<span class="perc-global-variable-marker" title="company_name">Acme</span>',
    );
  });

  it("parses stored and legacy global-variable markers", () => {
    expect(
      parseGlobalVariableMarker(
        parseFirstElement(
          '<span class="perc-global-variable-marker" title="c">Acme</span>',
        ),
      ),
    ).toEqual({ name: "c", value: "Acme" });
    expect(parseGlobalVariableMarker(parseFirstElement("<p>x</p>"))).toBeNull();
    expect(
      parseGlobalVariableMarker(
        parseFirstElement('<span class="perc-global-variable-marker">No title</span>'),
      ),
    ).toBeNull();
  });

  it("detects more-link markers", () => {
    expect(
      isMoreLinkElement(
        parseFirstElement('<span class="perc-blog-more-link"></span>'),
      ),
    ).toBe(true);
    expect(isMoreLinkElement(parseFirstElement("<p>x</p>"))).toBe(false);
    expect(isMoreLinkElement(null)).toBe(false);
  });

  it("prepares self-closed more-link spans before parsing", () => {
    expect(preparePercussionHtml("")).toBe("");
    expect(
      preparePercussionHtml('<p>A<span class="perc-blog-more-link"/>B</p>'),
    ).toBe('<p>A<span class="perc-blog-more-link"></span>B</p>');
    expect(preparePercussionHtml("<p>Plain</p>")).toBe("<p>Plain</p>");
  });

  it("reads the CMS global-variable table when provided", () => {
    expect(readGlobalVariableData()).toBeNull();
    (window as unknown as Record<string, unknown>)["PercGlobalVariablesData"] = {
      company_name: "Acme",
      count: 3,
    };
    try {
      expect(readGlobalVariableData()).toEqual({ company_name: "Acme" });
    } finally {
      delete (window as unknown as Record<string, unknown>)[
        "PercGlobalVariablesData"
      ];
    }
  });
});

describe("Percussion plugin nodes (#5652)", () => {
  it("round-trips stored inline-link markup without drift", () => {
    const stored =
      '<p>See <a href="/Rhythmyx/Sites/a.html" title="About" rxinlineslot="103" ' +
      'inlinetype="rxhyperlink" sys_dependentid="101" sys_dependentvariantid="201" ' +
      'rxselectedtext="About">About</a> now.</p>';
    expect(roundTripHtmlFragment(stored)).toBe(stored);
  });

  it("round-trips stored inline-image markup without drift", () => {
    const stored =
      '<p><img src="/Rhythmyx/Assets/i.png" alt="Pic" title="T" width="320" ' +
      'height="200" rxinlineslot="104" inlinetype="rximage" ' +
      'sys_dependentid="9" sys_dependentvariantid="8"></p>';
    expect(roundTripHtmlFragment(stored)).toBe(stored);
  });

  it("round-trips the stored global-variable span without drift", () => {
    const stored =
      '<p>Welcome <span class="perc-global-variable-marker" ' +
      'title="company_name">Acme</span>!</p>';
    expect(roundTripHtmlFragment(stored)).toBe(stored);
  });

  it("round-trips stored and self-closed more-link spans to the same document", () => {
    const stored = '<p>Teaser<span class="perc-blog-more-link"></span> continued</p>';
    expect(roundTripHtmlFragment(stored)).toBe(stored);
    expect(
      roundTripHtmlFragment('<p>Teaser<span class="perc-blog-more-link"/> continued</p>'),
    ).toBe(stored);
  });

  it("parses the legacy TinyMCE edit-time markers", () => {
    const legacy =
      '<p><span class="perc-global-variable-marker" title="c">Acme</span>' +
      '<span class="perc-blog-more-link"></span></p>';
    const html = roundTripHtmlFragment(legacy);
    expect(html).toContain('title="c"');
    expect(html).toContain("perc-blog-more-link");
  });

  it("inserts a more-link marker through the command", () => {
    const editor = makeEditor();
    try {
      editor.chain().focus().insertPercMoreLink().run();
      expect(editor.getHTML()).toContain(
        '<span class="perc-blog-more-link"></span>',
      );
    } finally {
      editor.destroy();
    }
  });

  it("inserts a global variable through the command and rejects blanks", () => {
    const editor = makeEditor();
    try {
      expect(
        editor.chain().focus().insertPercGlobalVariable("c", "Acme").run(),
      ).toBe(true);
      expect(editor.getHTML()).toContain(
        '<span class="perc-global-variable-marker" title="c">Acme</span>',
      );
      expect(
        editor.chain().focus().insertPercGlobalVariable("  ", "Acme").run(),
      ).toBe(false);
    } finally {
      editor.destroy();
    }
  });

  it("keeps ordinary links free of CMS attributes", () => {
    const editor = makeEditor(
      '<p><a href="https://example.com">Plain</a></p>',
    );
    try {
      const html = editor.getHTML();
      expect(html).toContain('href="https://example.com"');
      expect(html).not.toContain(PERC_INLINE_TYPE_LINK);
    } finally {
      editor.destroy();
    }
  });
});
