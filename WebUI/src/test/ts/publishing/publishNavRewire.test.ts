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
