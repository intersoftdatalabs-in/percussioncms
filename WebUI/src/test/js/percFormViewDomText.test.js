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
    "../../../../modules/perc-packages/src/main/resources/Packages/perc.widget.form/SupportFile-rx_resources/widgets/form/js/PercFormView.js"
  );

  it("escapes saveToUrl and other metadata fields before HTML concat", () => {
    const src = readFileSync(srcPath, "utf8");
    expect(src).toMatch(
      /var\s+safeSaveToUrl\s*=\s*\$\("<div\/>"\)\.text\(saveToUrl/
    );
    expect(src).toMatch(
      /var\s+safeSuccessUrl\s*=\s*\$\("<div\/>"\)\.text\(successUrl/
    );
    expect(src).toMatch(
      /var\s+safeErrorUrl\s*=\s*\$\("<div\/>"\)\.text\(errorUrl/
    );
    expect(src).toMatch(/var\s+safeMailTo\s*=\s*\$\("<div\/>"\)\.text\(mailTo/);
    expect(src).toMatch(
      /var\s+safeMailSubject\s*=\s*\$\("<div\/>"\)\.text\(mailSubject/
    );
    // Must not concat the raw DOM value into the metadata HTML template.
    expect(src).not.toMatch(
      /save-to-url-text-readonly['"]?\s*>\s*['"]\s*\+\s*saveToUrl\s*\+/
    );
    expect(src).toMatch(
      /save-to-url-text-readonly['"]?\s*>\s*['"]\s*\+\s*safeSaveToUrl\s*\+/
    );
  });
});
