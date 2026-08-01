/**
 * Source-pattern + behavioral guards for DOM text reinterpreted as HTML
 * (js/xss-through-dom #986/#987/#992/#993).
 */
import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const ROOT = resolve(__dirname, "../../../../");

describe("PercRssView description sink", () => {
  const src = readFileSync(
    resolve(
      ROOT,
      "modules/perc-common-ui-bundle/src/main/js/views/PercRssView.js",
    ),
    "utf8",
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
      "modules/perc-common-ui-bundle/src/main/js/views/PercMostReadBlogPostsView.js",
    ),
    "utf8",
  );

  it("defines safeHeadingTag allow-list", () => {
    expect(src).toMatch(/function safeHeadingTag/);
    expect(src).toMatch(/\^\(h\[1-6\]\|div\|p\|span\)\$/);
  });

  it("rejects script-like headingStyle tokens and unvalidated fallbacks", () => {
    // Mirror production allow-list (both name and fallback must match).
    function safeHeadingTag(name, fallback) {
      var allowed = /^(h[1-6]|div|p|span)$/;
      var n = String(name || "").toLowerCase();
      if (allowed.test(n)) {
        return n;
      }
      var fb = String(fallback || "").toLowerCase();
      if (allowed.test(fb)) {
        return fb;
      }
      return "h2";
    }
    expect(safeHeadingTag("script", "h2")).toBe("h2");
    expect(safeHeadingTag("h3", "h2")).toBe("h3");
    expect(safeHeadingTag("img onerror=x", "h3")).toBe("h3");
    // Kilo WARNING: bad fallback must not be returned when name fails allow-list
    expect(safeHeadingTag("script", "img")).toBe("h2");
    expect(safeHeadingTag("script", "script")).toBe("h2");
    expect(safeHeadingTag(null, "div")).toBe("div");
  });

  it("production source validates fallback before return", () => {
    // Ensure the helper no longer returns raw fallback without allow-list check.
    expect(src).toMatch(/allowed\.test\(fb\)/);
    expect(src).not.toMatch(/: fallback \|\| ["']h2["']/);
  });
});
