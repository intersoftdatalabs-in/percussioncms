/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

describe("logout / modern CSS host contract", () => {
  it("rxlogout.jsp links stable perc-modern-ui.css and loads TMX with perc.ui. prefix", () => {
    const path = resolve(__dirname, "../../../main/webapp/rxlogout.jsp");
    const text = readFileSync(path, "utf8");
    expect(text).toContain('href="/cm/modern/assets/perc-modern-ui.css"');
    expect(text).toContain("perc-modern-ui.js");
    expect(text).toContain("tmx/tmx.jsp");
    expect(text).toContain("prefix=perc.ui.");
    expect(text).toContain("sys_lang=");
    expect(text).toContain('id="perc-logout-root"');
    expect(text).toContain('id="perc-logout-bootstrap"');
    // Defensive logo cap if CSS fails
    expect(text).toMatch(/max-height:\s*48px/);
    // No legacy jQuery host
    expect(text).not.toContain("jquery.min.js");
    expect(text).not.toContain("jquery-migrate");
  });

  it("logout reuses login card logo size caps", () => {
    const path = resolve(
      __dirname,
      "../../../main/ts/login/LoginPage.module.css",
    );
    const text = readFileSync(path, "utf8");
    expect(text).toMatch(/max-height:\s*48px/);
    expect(text).toMatch(/max-width:\s*min\(220px/);
    expect(text).toContain("object-fit: contain");
  });

  it("modern bundle entry boots logout root", () => {
    const path = resolve(__dirname, "../../../main/ts/index.ts");
    const text = readFileSync(path, "utf8");
    expect(text).toContain("perc-logout-root");
    expect(text).toContain("bootLogout");
    expect(text).toContain('from "./logout"');
  });
});
