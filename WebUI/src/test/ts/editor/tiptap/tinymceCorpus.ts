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
 * TinyMCE 6.8.6 output corpus for the TipTap spike round-trip test (#5650).
 *
 * <p>Each entry is representative stored HTML the React editor host persists
 * today (paragraphs, headings, inline marks, lists, links, images, quotes,
 * code, rules, tables, alignment, entities, TinyMCE bookkeeping attributes).
 * The round-trip test asserts parsing through the spike TipTap schema and
 * re-serializing yields the same normalized document: no content drift.</p>
 *
 * <p>Deliberately excluded (later slices, not the spike): styled
 * {@code <span>} runs and font colors (plugin rewrite, #5652), anchor-only
 * {@code <a name>} links (no href survives the Link mark), ordered-list
 * {@code start} offsets, and script/event markup (still rejected by
 * {@code htmlField.ts}, never normalized).</p>
 */

export interface TinymceCorpusEntry {
  /** Short label used as the Vitest case name. */
  name: string;
  /** Stored HTML as TinyMCE 6.8.6 serializes it. */
  tinymce: string;
}

export const TINYMCE_CORPUS: readonly TinymceCorpusEntry[] = [
  { name: "paragraph", tinymce: "<p>Hello world</p>" },
  { name: "multiple paragraphs", tinymce: "<p>One</p><p>Two</p><p>Three</p>" },
  {
    name: "inline marks",
    tinymce:
      "<p><strong>Bold</strong> <em>italic</em> <u>underline</u> <s>strike</s> plain</p>",
  },
  {
    name: "legacy tag aliases",
    tinymce: "<p><b>B</b> <i>I</i> <strike>S</strike></p>",
  },
  { name: "headings", tinymce: "<h1>Title</h1><h2>Section</h2><h3>Detail</h3>" },
  {
    name: "bullet and ordered lists",
    tinymce:
      "<ul><li>One</li><li>Two</li></ul><ol><li>First</li><li>Second</li></ol>",
  },
  {
    name: "nested list",
    tinymce:
      "<ul><li>One<ul><li>Nested</li><li>Deeper</li></ul></li><li>Two</li></ul>",
  },
  {
    name: "link",
    tinymce: '<p>See <a href="https://example.com/page">Example</a> now.</p>',
  },
  {
    name: "link with target",
    tinymce:
      '<p><a href="https://example.com" target="_blank" rel="noopener">New tab</a></p>',
  },
  {
    name: "image",
    tinymce:
      '<p><img src="https://example.com/i.png" alt="A picture"></p>',
  },
  {
    name: "blockquote",
    tinymce: "<blockquote><p>Quoted text</p></blockquote>",
  },
  {
    name: "inline code and code block",
    tinymce: "<p>Run <code>npm test</code> first.</p><pre>const a = 1;</pre>",
  },
  {
    name: "horizontal rule",
    tinymce: "<p>Before</p><hr><p>After</p>",
  },
  {
    name: "table",
    tinymce:
      "<table><thead><tr><th>Name</th><th>Value</th></tr></thead>" +
      "<tbody><tr><td>Alpha</td><td>1</td></tr><tr><td>Beta</td><td>2</td></tr></tbody></table>",
  },
  {
    name: "legacy presentational table attributes",
    tinymce:
      '<table border="1" cellpadding="2" cellspacing="0"><tr><td>Cell</td></tr></table>',
  },
  {
    name: "text alignment",
    tinymce:
      '<p style="text-align: center;">Center</p>' +
      '<h2 style="text-align: right;">Right</h2>' +
      "<p>Left</p>",
  },
  {
    name: "entities",
    tinymce: "<p>Fish &amp; chips &lt;3 &quot;yum&quot;</p>",
  },
  {
    name: "non-breaking space",
    tinymce: "<p>Hello&nbsp;world</p>",
  },
  { name: "line break", tinymce: "<p>One<br>Two</p>" },
  {
    name: "TinyMCE bookkeeping attributes",
    tinymce:
      '<p data-mce-bogus="all">Kept</p>' +
      '<p><a href="https://example.com" data-mce-href="https://example.com">Link</a></p>',
  },
  {
    name: "bare div wrapper",
    tinymce: "<div><p>Wrapped</p><p>Twice</p></div>",
  },
  {
    name: "bare span run",
    tinymce: "<p>Hello <span>world</span></p>",
  },
  {
    name: "empty paragraph forms",
    tinymce: "<p>Text</p><p><br></p><p>More</p>",
  },
  {
    name: "mixed article",
    tinymce:
      "<h1>News</h1><p>By <strong>Ed</strong> on <em>Monday</em>.</p>" +
      "<ul><li><a href=\"https://example.com/a\">Item one</a></li><li>Item two</li></ul>" +
      "<blockquote><p>Notable.</p></blockquote><p>End.</p>",
  },
];
