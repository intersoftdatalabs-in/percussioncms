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
import { TINYMCE_CORPUS } from "./tinymceCorpus";

describe("TipTap HTML round-trip (#5650 corpus, #5651 parity schema)", () => {
  for (const entry of TINYMCE_CORPUS) {
    it(`preserves TinyMCE content: ${entry.name}`, () => {
      expect(normalizeHtmlForRoundTrip(roundTripHtmlFragment(entry.tinymce))).toBe(
        normalizeHtmlForRoundTrip(entry.tinymce),
      );
    });
  }

  it("serializes an empty field as an empty paragraph", () => {
    expect(roundTripHtmlFragment("")).toBe("<p></p>");
  });

  it("normalizes known TinyMCE/TipTap representation pairs", () => {
    expect(normalizeHtmlForRoundTrip("<p><b>B</b> <i>I</i></p>")).toBe(
      normalizeHtmlForRoundTrip("<p><strong>B</strong> <em>I</em></p>"),
    );
    expect(normalizeHtmlForRoundTrip("<pre><code>x</code></pre>")).toBe(
      normalizeHtmlForRoundTrip("<pre>x</pre>"),
    );
    expect(normalizeHtmlForRoundTrip("<p><br></p>")).toBe(
      normalizeHtmlForRoundTrip("<p></p>"),
    );
  });
});
