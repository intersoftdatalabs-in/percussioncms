/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, describe, expect, it } from "vitest";
import {
  ensureModernStyles,
  MODERN_UI_CSS_HREF,
  MODERN_UI_CSS_ID,
} from "@/ensureModernStyles";

describe("ensureModernStyles", () => {
  afterEach(() => {
    document.getElementById(MODERN_UI_CSS_ID)?.remove();
    document
      .querySelectorAll(`link[href="${MODERN_UI_CSS_HREF}"]`)
      .forEach((n) => n.remove());
  });

  it("injects a stylesheet link for the stable modern CSS asset", () => {
    ensureModernStyles();
    const link = document.getElementById(MODERN_UI_CSS_ID) as HTMLLinkElement | null;
    expect(link).not.toBeNull();
    expect(link?.rel).toBe("stylesheet");
    expect(link?.href).toContain(MODERN_UI_CSS_HREF);
  });

  it("is idempotent", () => {
    ensureModernStyles();
    ensureModernStyles();
    expect(
      document.querySelectorAll(`link[href*="perc-modern-ui.css"]`).length,
    ).toBe(1);
  });
});
