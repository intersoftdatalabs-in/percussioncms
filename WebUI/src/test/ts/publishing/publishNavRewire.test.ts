/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */
import { readFileSync } from "node:fs";
import { resolve } from "node:path";
import { describe, expect, it } from "vitest";

describe("publish nav rewire (US8)", () => {
  it("index.jsp maps publish to publishModern.jsp", () => {
    // vite root is frontend/; module webapp is three levels up from frontend
    const indexPath = resolve(
      __dirname,
      "../../../main/webapp/cm/app/index.jsp",
    );
    const text = readFileSync(indexPath, "utf8");
    expect(text).toContain('views.put("publish", "publishModern.jsp")');
    expect(text).not.toMatch(/views\.put\("publish",\s*"publish\.jsp"\)/);
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
