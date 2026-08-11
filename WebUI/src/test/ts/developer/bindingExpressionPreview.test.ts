/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  EXPRESSION_CLAMP_MAX_WIDTH_PX,
  EXPRESSION_COLLAPSED_MAX_HEIGHT_PX,
  EXPRESSION_EXPANDED_MAX_HEIGHT_PX,
  EXPRESSION_LONG_CHAR_THRESHOLD,
  EXPRESSION_LONG_LINE_THRESHOLD,
  bindingExpressionEditorStyle,
  bindingExpressionLineCount,
  isLongBindingExpression,
  normalizeBindingExpression,
} from "../../../main/ts/developer/bindingExpressionPreview";

describe("normalizeBindingExpression", () => {
  it("maps null and undefined to empty string", () => {
    expect(normalizeBindingExpression(null)).toBe("");
    expect(normalizeBindingExpression(undefined)).toBe("");
  });

  it("returns string content as-is", () => {
    expect(normalizeBindingExpression("$sys.item")).toBe("$sys.item");
  });
});

describe("bindingExpressionLineCount", () => {
  it("counts empty as one line", () => {
    expect(bindingExpressionLineCount("")).toBe(1);
  });

  it("counts LF CRLF and lone CR", () => {
    expect(bindingExpressionLineCount("a\nb\nc")).toBe(3);
    expect(bindingExpressionLineCount("a\r\nb")).toBe(2);
    expect(bindingExpressionLineCount("a\rb")).toBe(2);
  });
});

describe("isLongBindingExpression", () => {
  it("is false for short single-line expressions", () => {
    expect(isLongBindingExpression("$sys.item")).toBe(false);
    expect(isLongBindingExpression("1")).toBe(false);
    expect(isLongBindingExpression("")).toBe(false);
    expect(isLongBindingExpression(null)).toBe(false);
  });

  it("is true when longer than the char threshold", () => {
    const long = "x".repeat(EXPRESSION_LONG_CHAR_THRESHOLD + 1);
    expect(isLongBindingExpression(long)).toBe(true);
    expect(
      isLongBindingExpression("x".repeat(EXPRESSION_LONG_CHAR_THRESHOLD)),
    ).toBe(false);
  });

  it("is true when line count exceeds the line threshold", () => {
    // EXPRESSION_LONG_LINE_THRESHOLD default is 2 → 3+ lines is long
    expect(isLongBindingExpression("a\nb")).toBe(false);
    expect(isLongBindingExpression("a\nb\nc")).toBe(true);
    expect(EXPRESSION_LONG_LINE_THRESHOLD).toBe(2);
  });

  it("honors custom thresholds", () => {
    expect(isLongBindingExpression("abc", { maxChars: 2 })).toBe(true);
    expect(isLongBindingExpression("a\nb\nc", { maxLines: 5 })).toBe(false);
  });
});

describe("bindingExpressionEditorStyle", () => {
  it("clamps width and collapses height when not expanded", () => {
    const s = bindingExpressionEditorStyle(false);
    expect(s.maxWidth).toBe(EXPRESSION_CLAMP_MAX_WIDTH_PX);
    expect(s.maxHeight).toBe(EXPRESSION_COLLAPSED_MAX_HEIGHT_PX);
    expect(s.overflow).toBe("hidden");
    expect(s.resize).toBe("none");
    expect(s.whiteSpace).toBe("pre-wrap");
    expect(s.overflowWrap).toBe("anywhere");
  });

  it("raises max height and allows scroll when expanded", () => {
    const s = bindingExpressionEditorStyle(true);
    expect(s.maxHeight).toBe(EXPRESSION_EXPANDED_MAX_HEIGHT_PX);
    expect(s.overflow).toBe("auto");
    expect(s.resize).toBe("vertical");
  });
});
