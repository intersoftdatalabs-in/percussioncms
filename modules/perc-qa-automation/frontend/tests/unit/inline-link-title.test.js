/**
 * Unit tests for inline-link title helpers (#2243 / parent #946 slice 4).
 * No live CMS, no host install required.
 *
 * Run: npm run test:unit  (from frontend/)
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  normalizeInlineLinkTitleField,
  buildRenderLinkPreviewUrl,
  resolveInlineLinkTitle,
  pathItemsWithIds,
  extractTitleFromPreviewBody,
  inlineLinkTitleFixturesSkipReason,
  PAGE_DEFAULT_TITLE_FIELD,
  DISPLAYTITLE_FIELD,
  SLICE_ISSUE,
  PARENT_ISSUE,
  REPO_ISSUES,
} = require("../helpers/inline-link-title");

describe("normalizeInlineLinkTitleField", () => {
  it("returns empty for null/undefined/whitespace", () => {
    assert.equal(normalizeInlineLinkTitleField(null), "");
    assert.equal(normalizeInlineLinkTitleField(undefined), "");
    assert.equal(normalizeInlineLinkTitleField("  "), "");
  });

  it("trims control setting values", () => {
    assert.equal(normalizeInlineLinkTitleField("  page_title "), "page_title");
  });
});

describe("buildRenderLinkPreviewUrl", () => {
  const base = "http://127.0.0.1:9993";

  it("omits titleField when blank", () => {
    assert.equal(
      buildRenderLinkPreviewUrl(base, "42"),
      `${base}/Rhythmyx/services/pagemanagement/renderlink/preview/42/default`,
    );
    assert.equal(
      buildRenderLinkPreviewUrl(base + "/", "42", "  "),
      `${base}/Rhythmyx/services/pagemanagement/renderlink/preview/42/default`,
    );
  });

  it("appends encoded titleField for custom field", () => {
    assert.equal(
      buildRenderLinkPreviewUrl(base, "99", "page_title"),
      `${base}/Rhythmyx/services/pagemanagement/renderlink/preview/99/default?titleField=page_title`,
    );
    assert.equal(
      buildRenderLinkPreviewUrl(base, "1", "a b"),
      `${base}/Rhythmyx/services/pagemanagement/renderlink/preview/1/default?titleField=a%20b`,
    );
  });
});

describe("resolveInlineLinkTitle (fallback peer)", () => {
  it("blank config → type default only", () => {
    assert.equal(
      resolveInlineLinkTitle(null, { pagetitle: "X" }, "Nav"),
      "Nav",
    );
    assert.equal(
      resolveInlineLinkTitle("", { displaytitle: "D" }, "Nav"),
      "Nav",
    );
  });

  it("custom field hit", () => {
    assert.equal(
      resolveInlineLinkTitle(
        "pagetitle",
        { pagetitle: "Custom", displaytitle: "Disp" },
        "Nav",
      ),
      "Custom",
    );
  });

  it("missing custom → displaytitle → type default", () => {
    assert.equal(
      resolveInlineLinkTitle("missing", { displaytitle: "Disp" }, "Nav"),
      "Disp",
    );
    assert.equal(
      resolveInlineLinkTitle("missing", {}, "resource_link_title value"),
      "resource_link_title value",
    );
  });

  it("constants document page default and displaytitle", () => {
    assert.equal(PAGE_DEFAULT_TITLE_FIELD, "resource_link_title");
    assert.equal(DISPLAYTITLE_FIELD, "displaytitle");
  });
});

describe("pathItemsWithIds / extractTitleFromPreviewBody", () => {
  it("reads PathItem id/name", () => {
    const items = pathItemsWithIds({
      PathItem: [{ id: "16777215-101-1", name: "Home" }, { name: "no-id" }],
    });
    assert.equal(items.length, 1);
    assert.equal(items[0].id, "16777215-101-1");
    assert.equal(items[0].name, "Home");
  });

  it("extracts InlineRenderLink.title", () => {
    assert.equal(
      extractTitleFromPreviewBody({
        InlineRenderLink: { title: " Link Title ", url: "/x" },
      }),
      "Link Title",
    );
    assert.equal(extractTitleFromPreviewBody({ title: "T" }), "T");
    assert.equal(extractTitleFromPreviewBody(null), null);
  });
});

describe("skip-with-BUG reason", () => {
  it("embeds durable issue URLs", () => {
    const reason = inlineLinkTitleFixturesSkipReason();
    assert.match(reason, /^BUG:/);
    assert.match(reason, new RegExp(`#${SLICE_ISSUE}`));
    assert.match(reason, new RegExp(`#${PARENT_ISSUE}`));
    assert.match(reason, new RegExp(`${REPO_ISSUES}/${SLICE_ISSUE}`));
  });
});
