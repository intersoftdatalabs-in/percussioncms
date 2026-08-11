/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  designGapCode,
  designGapKey,
  formatDesignGap,
} from "../../../../main/ts/api/developer/designGaps";

describe("designGaps helpers (REST-GAPS-01)", () => {
  it("formatDesignGap prefers message over code", () => {
    expect(formatDesignGap({ code: "CT_ITEM_EXITS", message: "Item exits not exposed" })).toBe(
      "Item exits not exposed",
    );
  });

  it("formatDesignGap falls back to code when message empty", () => {
    expect(formatDesignGap({ code: "CT_ITEM_EXITS", message: "  " })).toBe("CT_ITEM_EXITS");
    expect(formatDesignGap({ code: "ONLY" })).toBe("ONLY");
  });

  it("formatDesignGap accepts legacy free-text strings", () => {
    expect(formatDesignGap("legacy gap text")).toBe("legacy gap text");
  });

  it("formatDesignGap handles nullish", () => {
    expect(formatDesignGap(null)).toBe("");
    expect(formatDesignGap(undefined)).toBe("");
  });

  it("designGapKey uses code then message then index", () => {
    expect(designGapKey({ code: "A", message: "m" }, 0)).toBe("A");
    expect(designGapKey({ message: "only-msg" }, 1)).toBe("only-msg");
    expect(designGapKey({}, 2)).toBe("2");
    expect(designGapKey("legacy", 3)).toBe("legacy");
  });

  it("designGapCode returns code only for structured gaps", () => {
    expect(designGapCode({ code: "X", message: "m" })).toBe("X");
    expect(designGapCode("legacy")).toBeUndefined();
    expect(designGapCode({ message: "no-code" })).toBeUndefined();
  });
});
