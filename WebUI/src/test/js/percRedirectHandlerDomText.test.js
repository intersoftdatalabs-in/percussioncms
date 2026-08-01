/**
 * Regression: legacy PercRedirectHandler must not concatenate paths into HTML
 * (CodeQL js/xss-through-dom #1583 — "DOM text reinterpreted as HTML").
 */
import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { describe, it, expect } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));

describe("PercRedirectHandler createDialogHtml (DOM text as HTML)", () => {
  const paths = [
    resolve(__dirname, "../../main/webapp/cm/plugins/PercRedirectHandler.js"),
    resolve(
      __dirname,
      "../../main/webapp/cm/app/js/legacy/plugins/PercRedirectHandler.js",
    ),
  ];

  for (const srcPath of paths) {
    it(`${srcPath
      .split("/")
      .slice(-3)
      .join("/")} uses .text(getRelativePath(...)) not HTML concat`, () => {
      const src = readFileSync(srcPath, "utf8");
      expect(src).not.toMatch(
        /readonlyinput['"]?\s*>\s*['"]\s*\+\s*getRelativePath/,
      );
      expect(src).toMatch(/\.text\(\s*getRelativePath\(/);
    });
  }
});
