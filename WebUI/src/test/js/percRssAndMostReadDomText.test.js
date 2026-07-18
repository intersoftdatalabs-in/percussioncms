/**
 * Source-pattern + behavioral guards for DOM text reinterpreted as HTML
 * (js/xss-through-dom #986/#987/#992/#993).
 */
import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { describe, it, expect, beforeEach } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, "../../../../");

describe("PercRssView description sink", () => {
  const src = readFileSync(
    resolve(
      ROOT,
      "modules/perc-common-ui-bundle/src/main/js/views/PercRssView.js"
    ),
    "utf8"
  );

  it("does not call .html(item.description) or .html(description.text())", () => {
    expect(src).not.toMatch(/\.html\(\s*item\.description\s*\)/);
    expect(src).not.toMatch(/\.html\(\s*description\.text\(\)\s*\)/);
  });

  it("escapes description via .text(...).html() before string embed", () => {
    expect(src).toMatch(/\$\("<div\/>"\)\.text\(descText\)\.html\(\)/);
  });
});

describe("PercMostReadBlogPostsView heading tag whitelist", () => {
  const src = readFileSync(
    resolve(
      ROOT,
      "modules/perc-common-ui-bundle/src/main/js/views/PercMostReadBlogPostsView.js"
    ),
    "utf8"
  );

  it("defines safeHeadingTag allow-list", () => {
    expect(src).toMatch(/function safeHeadingTag/);
    expect(src).toMatch(/\^\(h\[1-6\]\|div\|p\|span\)\$/);
  });

  it("rejects script-like headingStyle tokens (unit of safeHeadingTag)", () => {
    // Mirror production allow-list for a pure unit assertion.
    function safeHeadingTag(name, fallback) {
      var n = String(name || fallback || "h2").toLowerCase();
      return /^(h[1-6]|div|p|span)$/.test(n) ? n : fallback || "h2";
    }
    expect(safeHeadingTag("script", "h2")).toBe("h2");
    expect(safeHeadingTag("h3", "h2")).toBe("h3");
    expect(safeHeadingTag('img onerror=x', "h3")).toBe("h3");
  });
});
