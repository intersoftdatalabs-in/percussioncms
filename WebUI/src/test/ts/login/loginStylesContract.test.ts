/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

describe("login / modern CSS host contract", () => {
  it("rxlogin.jsp links stable perc-modern-ui.css and loads TMX with perc.ui. prefix", () => {
    const path = resolve(__dirname, "../../../main/webapp/rxlogin.jsp");
    const text = readFileSync(path, "utf8");
    expect(text).toContain('href="/cm/modern/assets/perc-modern-ui.css"');
    expect(text).toContain("perc-modern-ui.js");
    // Login form chrome needs I18N.message from tmx.jsp (initial session locale).
    expect(text).toContain("tmx/tmx.jsp");
    expect(text).toContain("prefix=perc.ui.");
    expect(text).toContain("sys_lang=");
    // Defensive logo cap if CSS fails
    expect(text).toMatch(/max-height:\s*48px/);
  });

  it("spa.jsp hosts link stable perc-modern-ui.css and TMX (both trees)", () => {
    for (const rel of [
      "../../../main/webapp/cm/app/spa.jsp",
      "../../../main/webapp/cm/pages/app/spa.jsp",
    ]) {
      const text = readFileSync(resolve(__dirname, rel), "utf8");
      expect(text).toContain('href="/cm/modern/assets/perc-modern-ui.css"');
      // Home / SPA chrome need I18N.message from tmx.jsp (session locale)
      expect(text).toContain("tmx/tmx.jsp");
      expect(text).toContain("prefix=perc.ui.");
    }
  });

  it("vite emits stable CSS name (cssCodeSplit false)", () => {
    const path = resolve(__dirname, "../../../main/frontend/vite.config.ts");
    const text = readFileSync(path, "utf8");
    expect(text).toContain("cssCodeSplit: false");
    expect(text).toContain("perc-modern-ui.css");
  });

  it("login logo CSS caps size", () => {
    const path = resolve(
      __dirname,
      "../../../main/ts/login/LoginPage.module.css",
    );
    const text = readFileSync(path, "utf8");
    expect(text).toMatch(/max-height:\s*48px/);
    expect(text).toMatch(/max-width:\s*min\(220px/);
    expect(text).toContain("object-fit: contain");
  });
});
