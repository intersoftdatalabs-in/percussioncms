/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */
import { afterEach, describe, expect, it } from "vitest";
import { fallbackLabelFromKey, message } from "../../../main/ts/i18n/message";

describe("message / TMX fallback", () => {
  afterEach(() => {
    delete (window as { I18N?: unknown }).I18N;
  });

  it("fallbackLabelFromKey uses text after @", () => {
    expect(fallbackLabelFromKey("perc.ui.home.modern@Home")).toBe("Home");
    expect(fallbackLabelFromKey("perc.ui.home@My Recent")).toBe("My Recent");
    expect(fallbackLabelFromKey("no-at-sign")).toBe("no-at-sign");
  });

  it("message falls back when I18N missing", () => {
    expect(message("perc.ui.home.modern@Gadgets")).toBe("Gadgets");
  });

  it("message uses I18N when it returns a real translation", () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: () => "Translated",
    };
    expect(message("perc.ui.home.modern@Home")).toBe("Translated");
  });

  it("message falls back when I18N echoes the key", () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) => k,
    };
    expect(message("perc.ui.home.modern@Home")).toBe("Home");
  });

  it("message falls back when I18N returns empty string", () => {
    (window as unknown as { I18N: { message: () => string } }).I18N = {
      message: () => "",
    };
    expect(message("perc.ui.home.modern@Home")).toBe("Home");
  });
});

