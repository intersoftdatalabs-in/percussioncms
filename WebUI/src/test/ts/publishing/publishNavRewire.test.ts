/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */
import { existsSync, readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

describe("publish nav rewire (US8) — SPA cutover", () => {
  it("index.jsp maps publish to spa.jsp?entry=publish (not *Modern.jsp)", () => {
    // vite root is frontend/; module webapp is three levels up from frontend
    const indexPath = resolve(
      __dirname,
      "../../../main/webapp/cm/app/index.jsp",
    );
    const text = readFileSync(indexPath, "utf8");
    expect(text).toContain("buildSpaEntryRedirect");
    expect(text).toContain("/cm/app/spa.jsp?");
    expect(text).toContain('"publish"');
    expect(text).not.toMatch(
      /legacyViews\.put\("publish",\s*"publishModern\.jsp"\)/,
    );
    expect(text).not.toMatch(/views\.put\("publish",\s*"publish\.jsp"\)/);
  });

  it("war/app/publish.jsp redirects to modern shell (Erlang S1)", () => {
    const warPublish = resolve(__dirname, "../../../../war/app/publish.jsp");
    const text = readFileSync(warPublish, "utf8");
    expect(text).toContain("301");
    expect(text).toContain("/cm/app/");
    expect(text).toContain("view=publish");
    expect(text).not.toContain("PercPublishMinuetView");
  });

  it("publishModern.jsp product host is removed (PR-8)", () => {
    const modern = resolve(
      __dirname,
      "../../../main/webapp/cm/app/publishModern.jsp",
    );
    expect(existsSync(modern)).toBe(false);
  });
});

describe("Minuet pack retirement (US8 / B3)", () => {
  it("static-bundles perc_publish pack does not list deleted Minuet exclusive views", () => {
    const packPath = resolve(
      __dirname,
      "../../../main/resources/minify/static-bundles.json",
    );
    const json = JSON.parse(readFileSync(packPath, "utf8")) as {
      bundles?: Array<{ name?: string; files?: string[] }>;
    };
    const pack = (json.bundles ?? []).find(
      (b) => b.name === "jslibMin/perc_publish.packed.js",
    );
    expect(pack).toBeDefined();
    const files = pack?.files ?? [];
    for (const banned of [
      "views/PercPublishMinuetView.js",
      "views/PercPublishStatusMinuetView.js",
      "views/PercPublishLogsMinuetView.js",
    ]) {
      expect(files).not.toContain(banned);
    }
    // Shared publisher service retained for item publish-now
    expect(files.some((f) => f.includes("PercPublisherService"))).toBe(true);
  });
});
