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

import { describe, expect, it } from "vitest";
import {
  isPastedLinkHrefSafe,
  looksLikeWordHtml,
  markTipTapWordPasteCapable,
  sanitizeWordPastedHtml,
  TIPTAP_WORD_PASTE_WINDOW_KEY,
} from "../../../../main/ts/editor/tiptap/tiptapWordPaste";

const WORD_DOC = `<html xmlns:o="urn:schemas-microsoft-com:office:office" xmlns:w="urn:schemas-microsoft-com:office:word">
<head><style><!-- p.MsoNormal { mso-style-name: Normal; } --></style>
<xml><w:WordDocument><w:View>Normal</w:View></w:WordDocument></xml></head>
<body>
<!--[if !supportLists]><span>balloon</span><![endif]-->
<p class="MsoNormal" style="mso-margin-top-alt: 0cm; text-align: center;"><b>Hi</b><o:p></o:p></p>
<p class="MsoNormal">Visit <a href="https://example.com/x" target="_blank">us</a><o:p></o:p></p>
<p class="MsoNormal">Bad <a href="javascript:alert(1)">link</a><o:p></o:p></p>
<table class="MsoTable" border="1" cellpadding="0"><tbody><tr><th>Head</th><td>Cell</td></tr></tbody></table>
<p class="MsoListParagraph" style="text-indent: -18pt;"><span>·</span> Item one<o:p></o:p></p>
</body></html>`;

describe("tiptapWordPaste", () => {
  it("detects Word/Office clipboard HTML", () => {
    expect(looksLikeWordHtml(WORD_DOC)).toBe(true);
    expect(looksLikeWordHtml('<p class="MsoNormal">x</p>')).toBe(true);
    expect(looksLikeWordHtml("<p>Hello <strong>world</strong></p>")).toBe(false);
    expect(looksLikeWordHtml("")).toBe(false);
  });

  it("passes ordinary HTML through byte-identical", () => {
    const clean =
      '<p>Hello <strong>world</strong></p><ul><li><p>one</p></li></ul><table><tr><td>x</td></tr></table>';
    expect(sanitizeWordPastedHtml(clean)).toBe(clean);
    expect(sanitizeWordPastedHtml("")).toBe("");
  });

  it("strips Word metadata but keeps contracted content", () => {
    const out = sanitizeWordPastedHtml(WORD_DOC);
    expect(out).not.toContain("mso-");
    expect(out).not.toContain("Mso");
    expect(out).not.toContain("o:p");
    expect(out).not.toContain("<style");
    expect(out).not.toContain("<!--");
    expect(out).not.toContain("target=");
    expect(out).toContain("<b>Hi</b>");
    expect(out).toContain('href="https://example.com/x"');
    // Centered Word paragraph keeps the one alignment form the schema stores.
    expect(out).toContain("text-align: center");
  });

  it("drops unsafe pasted link targets but keeps the link text", () => {
    const out = sanitizeWordPastedHtml(WORD_DOC);
    expect(out).not.toContain("javascript:");
    expect(out).toContain(">link</a>");
    expect(out).not.toMatch(/<a[^>]*href="javascript/);
  });

  it("keeps contracted tables with header cells as td", () => {
    const out = sanitizeWordPastedHtml(WORD_DOC);
    expect(out).toContain("<table>");
    expect(out).toContain("<td>Head</td>");
    expect(out).toContain("<td>Cell</td>");
    expect(out).not.toContain("<th");
    expect(out).not.toContain("border=");
  });

  it("keeps pasted data images (paste_data_images) with src only", () => {
    const word =
      '<p class="MsoNormal"><img src="data:image/png;base64,AAA" alt="x" width="3" style="mso-x: 1"></p>';
    const out = sanitizeWordPastedHtml(word);
    expect(out).toContain('src="data:image/png;base64,AAA"');
    expect(out).toContain('alt="x"');
    expect(out).not.toContain("width=");
    expect(out).not.toContain("mso-");
  });

  it("blocks active href schemes for pasted and toolbar links", () => {
    expect(isPastedLinkHrefSafe("https://example.com/a?b=c")).toBe(true);
    expect(isPastedLinkHrefSafe("/Sites/Home/page")).toBe(true);
    expect(isPastedLinkHrefSafe("#anchor")).toBe(true);
    expect(isPastedLinkHrefSafe("mailto:a@b.c")).toBe(true);
    expect(isPastedLinkHrefSafe("  https://example.com  ")).toBe(true);
    expect(isPastedLinkHrefSafe("javascript:alert(1)")).toBe(false);
    expect(isPastedLinkHrefSafe("  JaVaScRiPt:alert(1)")).toBe(false);
    expect(isPastedLinkHrefSafe("data:text/html,<p>x</p>")).toBe(false);
    expect(isPastedLinkHrefSafe("vbscript:msgbox(1)")).toBe(false);
    expect(isPastedLinkHrefSafe("")).toBe(false);
    expect(isPastedLinkHrefSafe("https://example.com/\u0000x")).toBe(false);
  });

  it("rebuilds Word bullet paragraphs into a real list", () => {
    const word =
      '<html xmlns:o="urn:schemas-microsoft-com:office:office"><body>' +
      '<p class="MsoNormal">Intro<o:p></o:p></p>' +
      '<p class="MsoListParagraph" style="text-indent:-18pt;mso-list:l0 level1 lfo1;">' +
      '<span style="font-family:Symbol;">\u00b7<span>&nbsp;&nbsp;</span></span>Item one<o:p></o:p></p>' +
      '<p class="MsoListParagraph"><span>\u00b7</span> Item <b>two</b><o:p></o:p></p>' +
      '<p class="MsoNormal">After<o:p></o:p></p>' +
      "</body></html>";
    const out = sanitizeWordPastedHtml(word);
    expect(out).toContain("<ul>");
    expect(out).toContain("<li>Item one</li>");
    expect(out).toContain("<li>Item <b>two</b></li>");
    expect(out).not.toContain("MsoListParagraph");
    expect(out).not.toContain("mso-list");
    expect(out).not.toContain("\u00b7");
    expect(out).toContain("<p>Intro</p>");
    expect(out).toContain("<p>After</p>");
  });

  it("rebuilds Word numbered paragraphs into an ordered list", () => {
    const word =
      '<p class="MsoListParagraph" style="mso-list:l1 level1 lfo1;">1. First<o:p></o:p></p>' +
      '<p class="MsoListParagraph">2. Second<o:p></o:p></p>' +
      '<p class="MsoListParagraph">a) Third<o:p></o:p></p>';
    const out = sanitizeWordPastedHtml(word);
    expect(out).toContain("<ol>");
    expect(out).toContain("<li>First</li>");
    expect(out).toContain("<li>Second</li>");
    expect(out).toContain("<li>Third</li>");
    expect(out).not.toContain("<ul>");
  });

  it("nests deeper mso-list levels inside their parent item", () => {
    const word =
      '<p class="MsoListParagraph" style="mso-list:l0 level1 lfo1;"><span>\u00b7</span> Top<o:p></o:p></p>' +
      '<p class="MsoListParagraph" style="mso-list:l0 level2 lfo1;"><span>o</span> Nested<o:p></o:p></p>' +
      '<p class="MsoListParagraph" style="mso-list:l0 level1 lfo1;"><span>\u00b7</span> Back<o:p></o:p></p>';
    const out = sanitizeWordPastedHtml(word);
    expect(out).toContain("<li>Top<ul>");
    expect(out).toContain("<li>Nested</li>");
    expect(out).toContain("<li>Back</li>");
  });

  it("splits list runs on non-list content and ordered/bullet changes", () => {
    const word =
      '<p class="MsoListParagraph"><span>\u00b7</span> One<o:p></o:p></p>' +
      '<p class="MsoNormal">Break<o:p></o:p></p>' +
      '<p class="MsoListParagraph">1. Two<o:p></o:p></p>' +
      '<p class="MsoListParagraph"><span>\u00b7</span> Three<o:p></o:p></p>';
    const out = sanitizeWordPastedHtml(word);
    expect(out).toContain("<p>Break</p>");
    expect(out).toContain("<ul><li>One</li></ul>");
    expect(out).toContain("<ol><li>Two</li></ol>");
    expect(out).toContain("<ul><li>Three</li></ul>");
  });

  it("publishes the Word-paste capability marker on module load (#5945)", () => {
    // Live Playwright specs read this marker fail-fast so a stale SPA
    // bundle (pre-#5933 rebuild) is reported as stale, not as a
    // paste-path regression.
    const scope = (globalThis as unknown as Record<string, unknown>);
    expect(scope[TIPTAP_WORD_PASTE_WINDOW_KEY]).toEqual({ lists: true });
  });

  it("marks an explicit scope and refuses unusable scopes", () => {
    const scope: Record<string, unknown> = {};
    expect(markTipTapWordPasteCapable(scope)).toBe(true);
    expect(scope[TIPTAP_WORD_PASTE_WINDOW_KEY]).toEqual({ lists: true });
    expect(markTipTapWordPasteCapable(null)).toBe(false);
    expect(markTipTapWordPasteCapable(undefined)).toBe(true);
  });
});
