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
  normalizeHtmlForRoundTrip,
  roundTripHtmlFragment,
} from "../../../../main/ts/editor/tiptap/tiptapRoundTrip";

/**
 * Template-content parity for the TipTap migration (#7048, #5622 slice 3/3).
 *
 * <p>Each entry is a whole stored field value in the shape produced by the
 * template-configured {@code sys_tinymce} content editors audited for this
 * slice (FastForward shared {@code callout}/{@code body}, press-release
 * {@code pr_summary}, sample article/brief {@code body}): TinyMCE-era markup
 * combined with the Percussion stored constructs those fields carry in real
 * content — managed inline links ({@code rxinlineslot}/
 * {@code sys_dependentid}), inline images, global-variable markers,
 * blog more-link markers, and inline/block template-variant wrappers.</p>
 *
 * <p>Zero drift is asserted as
 * {@code normalize(roundTrip(stored)) === normalize(stored)}: the same
 * canonicalization the spike corpus (#5650) and the plugin rewrite (#5652)
 * use. Two template-variant wrappers are accepted representation drift with
 * content preserved: the {@code rx_ephox_inlinevariant} block {@code div}
 * (stripped at publish by {@code PSDivTagCleanup}) and the
 * {@code rx_inlinevariant} inline {@code span} (a classic edit-time
 * affordance consumed only by the retired classic {@code editorinline.js});
 * the link text and marker images inside them round-trip intact.</p>
 */

interface TemplateFieldEntry {
  /** Template config + field the stored value is shaped after. */
  name: string;
  /** Stored HTML as TinyMCE-era template fields persist it. */
  stored: string;
}

const TEMPLATE_FIELDS: readonly TemplateFieldEntry[] = [
  {
    name: "FastForward shared callout with marks and ordinary link",
    stored:
      "<p><strong>Callout:</strong> Read <em>the release notes</em> and see " +
      '<a href="https://example.com/notes">what changed</a>.</p>',
  },
  {
    name: "FastForward shared body with headings, list, table and image",
    stored:
      "<h2>Highlights</h2>" +
      "<p>Ship <strong>TipTap</strong> with <em>zero drift</em>.</p>" +
      "<ul><li>First</li><li>Second</li></ul>" +
      "<table><thead><tr><th>Name</th><th>Value</th></tr></thead>" +
      "<tbody><tr><td>Engine</td><td>TipTap</td></tr></tbody></table>" +
      '<p><img src="/Rhythmyx/Assets/i.png" alt="Pic" title="T" width="320" ' +
      'height="200" rxinlineslot="104" inlinetype="rximage" ' +
      'sys_dependentid="9" sys_dependentvariantid="8"></p>',
  },
  {
    name: "press-release summary with managed inline link and variable",
    stored:
      "<p>Summary for " +
      '<span class="perc-global-variable-marker" title="company_name">Acme</span>: ' +
      'see <a href="/Rhythmyx/Sites/a.html" title="About" rxinlineslot="103" ' +
      'inlinetype="rxhyperlink" sys_dependentid="101" sys_dependentvariantid="201" ' +
      'rxselectedtext="About">About</a>.</p>',
  },
  {
    name: "article body with more-link teaser split",
    stored:
      "<p>Teaser<span " +
      'class="perc-blog-more-link"></span> continued after the fold.</p>' +
      "<p>Second paragraph stays intact.</p>",
  },
  {
    name: "brief body with legacy tag aliases and entities",
    stored:
      "<p><b>Bold</b> and <i>italic</i> with &amp; entities.</p>" +
      "<p>Before</p><hr><p>After</p>",
  },
];

describe("template-content TipTap parity (#7048)", () => {
  for (const entry of TEMPLATE_FIELDS) {
    it(`preserves template field content: ${entry.name}`, () => {
      expect(normalizeHtmlForRoundTrip(roundTripHtmlFragment(entry.stored))).toBe(
        normalizeHtmlForRoundTrip(entry.stored),
      );
    });
  }

  it("preserves inline-variant link content while normalizing the edit-time wrapper", () => {
    const stored =
      '<p><span class="rx_inlinevariant">' +
      '<img alt="@inlinemarker@" width="9" height="8" src="../sys_resources/images/inlinestart.png"/>' +
      '<a href="/Sites/Enterprise/about">About</a>' +
      '<img alt="@inlinemarker@" width="9" height="8" src="../sys_resources/images/inlineend.png"/>' +
      "</span> continues.</p>";
    const out = roundTripHtmlFragment(stored);
    // Link text and both marker images survive; only the classic
    // edit-time span wrapper is normalized away.
    expect(out).toContain('href="/Sites/Enterprise/about"');
    expect(out).toContain("About</a>");
    expect(out).toContain("inlinestart.png");
    expect(out).toContain("inlineend.png");
    expect(out).not.toContain("rx_inlinevariant");
  });

  it("preserves block-variant text while normalizing the publish-stripped wrapper", () => {
    const stored =
      '<div class="rx_ephox_inlinevariant"><p>Block variant text</p></div>';
    const out = roundTripHtmlFragment(stored);
    expect(out).toContain("Block variant text");
    // PSDivTagCleanup strips this wrapper div at publish, so the editor
    // normalizing it away matches the published form.
    expect(out).not.toContain("rx_ephox_inlinevariant");
  });
});
