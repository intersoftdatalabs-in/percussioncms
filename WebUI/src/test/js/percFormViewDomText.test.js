/**
 * Regression: PercFormView read-only metadata must HTML-escape DOM-sourced
 * fields (including saveToUrl) before string concatenation
 * (CodeQL js/xss-through-dom / Kilo review on PR #1360).
 */
import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));

describe("PercFormView metadata HTML escape (DOM text as HTML)", () => {
  const srcPath = resolve(
    __dirname,
    "../../../../modules/perc-packages/src/main/resources/Packages/perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormView.js",
  );

  it("escapes saveToUrl and other metadata fields before HTML concat", () => {
    // Normalize CRLF; production formats the jQuery .text().html() chain
    // across multiple lines (with || "" defaults).
    const src = readFileSync(srcPath, "utf8").replace(/\r\n/g, "\n");
    // $("<div/>").text(field || "").html() — field may wrap lines.
    const textEscape = (ident) =>
      new RegExp(
        String.raw`var\s+safe\w+\s*=\s*\$\("<div\/>"\)\s*\.text\(\s*${ident}\s*(?:\|\|\s*""\s*)?\)\s*\.html\(\)`,
        "m",
      );
    expect(src).toMatch(textEscape("saveToUrl"));
    expect(src).toMatch(textEscape("successUrl"));
    expect(src).toMatch(textEscape("errorUrl"));
    expect(src).toMatch(textEscape("mailTo"));
    expect(src).toMatch(textEscape("mailSubject"));
    // Must not concat the raw DOM value into the metadata HTML template.
    expect(src).not.toMatch(
      /save-to-url-text-readonly['"]?\s*>\s*['"]\s*\+\s*saveToUrl\s*\+/,
    );
    expect(src).toMatch(
      /save-to-url-text-readonly['"]?\s*>\s*['"]\s*\+\s*\n?\s*safeSaveToUrl\s*\+/,
    );
  });
});
