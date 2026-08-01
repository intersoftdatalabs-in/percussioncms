/**
 * Behavioral tests for imageSlider safeThumbnailSrc (js/xss-through-dom residual).
 * Extracts the production function from both package copies and exercises rejection paths.
 */
import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));

const PACKAGE_COPIES = [
  resolve(
    __dirname,
    "../../../../modules/perc-packages/src/main/resources/Packages/perc.widget.imageSlider/SupportFile-rx_resources/widgets/percImageSlider/js/percImageSlider.js",
  ),
  resolve(
    __dirname,
    "../../../../modules/perc-packages/src/main/resources/Packages/perc.widget.imageSlider/sys__UserDependency--rx_resources/widgets/percImageSlider/js/percImageSlider.js",
  ),
];

/**
 * Load the production `safeThumbnailSrc` implementation from source so tests
 * exercise the real helper body (not a reimplemented twin).
 */
function loadSafeThumbnailSrc(srcPath) {
  // Normalize CRLF so the function-body regex is portable on Windows checkouts.
  const src = readFileSync(srcPath, "utf8").replace(/\r\n/g, "\n");
  const marker = "function safeThumbnailSrc(path)";
  const start = src.indexOf(marker);
  if (start < 0) {
    throw new Error("safeThumbnailSrc not found in " + srcPath);
  }
  // Function body ends at the blank line / next top-level comment before $(document)
  const after = src.slice(start);
  const endMatch = after.match(
    /^function safeThumbnailSrc\(path\) \{[\s\S]*?\n  \}\n/,
  );
  if (!endMatch) {
    throw new Error("Could not parse safeThumbnailSrc body from " + srcPath);
  }
  // eslint-disable-next-line no-new-func
  return new Function(
    "return (" + endMatch[0].trim().replace(/;$/, "") + ")",
  )();
}

describe.each(
  PACKAGE_COPIES.map((p) => [p.split(/[/\\]/).slice(-5).join("/"), p]),
)("safeThumbnailSrc (%s)", (_label, srcPath) => {
    const safeThumbnailSrc = loadSafeThumbnailSrc(srcPath);

    it("returns empty for null/undefined/blank", () => {
      expect(safeThumbnailSrc(null)).toBe("");
      expect(safeThumbnailSrc(undefined)).toBe("");
      expect(safeThumbnailSrc("")).toBe("");
      expect(safeThumbnailSrc("   ")).toBe("");
    });

    it("passes through relative CMS thumbnail paths", () => {
      expect(safeThumbnailSrc("/Assets/uploads/img.jpg")).toBe(
        "/Assets/uploads/img.jpg",
      );
      expect(safeThumbnailSrc("rx_resources/images/t.png")).toBe(
        "rx_resources/images/t.png",
      );
    });

    it("rejects javascript: data: and vbscript: schemes (case-insensitive)", () => {
      expect(safeThumbnailSrc("javascript:alert(1)")).toBe("");
      expect(safeThumbnailSrc("JAVASCRIPT:alert(1)")).toBe("");
      expect(safeThumbnailSrc("  javascript:alert(1)  ")).toBe("");
      expect(safeThumbnailSrc("data:text/html,<script>")).toBe("");
      expect(safeThumbnailSrc("DATA:image/png;base64,xx")).toBe("");
      expect(safeThumbnailSrc("vbscript:msgbox")).toBe("");
      expect(safeThumbnailSrc("VbScRiPt:msgbox")).toBe("");
    });

    it("does not reject paths that merely contain scheme-like substrings later", () => {
      // Only scheme *prefix* is blocked; middle occurrences remain (product contract).
      expect(safeThumbnailSrc("/path/data:not-a-scheme.jpg")).toBe(
        "/path/data:not-a-scheme.jpg",
      );
    });
});
