/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */
import { afterEach, describe, expect, it } from "vitest";
import {
  __resetMessageTrackingForTests,
  fallbackLabelFromKey,
  getTrackedMessageId,
  message,
} from "../../../main/ts/i18n/message";

describe("message / TMX fallback", () => {
  afterEach(() => {
    delete (window as { I18N?: unknown }).I18N;
    __resetMessageTrackingForTests();
  });

  it("fallbackLabelFromKey uses text after @", () => {
    expect(fallbackLabelFromKey("perc.ui.home.modern@Home")).toBe("Home");
    expect(fallbackLabelFromKey("perc.ui.home@My Recent")).toBe("My Recent");
    expect(fallbackLabelFromKey("no-at-sign")).toBe("no-at-sign");
  });

  it("message falls back when I18N missing", () => {
    expect(message("perc.ui.home.modern@Gadgets")).toBe("Gadgets");
  });

  it("fallbackLabelFromKey interpolates positional args (#7106)", () => {
    expect(
      fallbackLabelFromKey("perc.ui.publish.modern@Unknown file driver: {0}", [
        "WEIRD",
      ]),
    ).toBe("Unknown file driver: WEIRD");
    expect(
      fallbackLabelFromKey("perc.ui.publish.modern@{0} of {1}", ["3", "9"]),
    ).toBe("3 of 9");
    expect(
      fallbackLabelFromKey("perc.ui.publish.modern@Hi {1}", ["only-zero"]),
    ).toBe("Hi {1}");
    expect(fallbackLabelFromKey("perc.ui.home.modern@Home")).toBe("Home");
  });

  it("message interpolates args when I18N echoes the key (double-fault, #7106)", () => {
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) => k,
    };
    expect(
      message("perc.ui.publish.modern@Unknown file driver: {0}", ["WEIRD"]),
    ).toBe("Unknown file driver: WEIRD");
  });

  it("message interpolates args when I18N returns the server-trimmed label (#7106)", () => {
    // Real tmx.jsp behavior for a missing TU: ___psxTrimKey returns the text
    // after @ with NO arg substitution (not a full-key echo).
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (k: string) => k.slice(k.lastIndexOf("@") + 1),
    };
    expect(
      message("perc.ui.publish.modern@Unknown file driver: {0}", ["WEIRD"]),
    ).toBe("Unknown file driver: WEIRD");
  });

  it("message does not double-substitute a genuine translation (#7106)", () => {
    // Server substitutes args when the TU exists; resolveMessage must pass it through.
    (window as unknown as { I18N: { message: () => string } }).I18N = {
      message: () => "Unknown file driver: WEIRD",
    };
    expect(
      message("perc.ui.publish.modern@Unknown file driver: {0}", ["WEIRD"]),
    ).toBe("Unknown file driver: WEIRD");
  });
  it("message interpolates args when I18N is missing (#7106)", () => {
    expect(
      message("perc.ui.publish.modern@Unknown file driver: {0}", ["WEIRD"]),
    ).toBe("Unknown file driver: WEIRD");
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

  it("tracks key for @mkd/language getMessageId after message()", () => {
    message("perc.ui.navMenu.home@Home");
    document.body.innerHTML = `<button type="button">Home</button>`;
    const btn = document.querySelector("button")!;
    expect(getTrackedMessageId(btn)).toBe("perc.ui.navMenu.home@Home");
  });
});

