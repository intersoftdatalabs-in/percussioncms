/*
 * Copyright 1999-2026 Percussion Software, Inc.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  copyTextToClipboard,
  copyTextViaExecCommand,
  highlightTemplateSource,
  lineNumberGutterWidth,
  lineNumbersForSource,
  splitSourceLines,
  tokenizeTemplateLine,
} from "../../../main/ts/developer/templateSourceViewer";

describe("splitSourceLines", () => {
  it("returns a single empty line for empty source", () => {
    expect(splitSourceLines("")).toEqual([""]);
  });

  it("splits LF and CRLF and keeps empty lines", () => {
    expect(splitSourceLines("a\nb\nc")).toEqual(["a", "b", "c"]);
    expect(splitSourceLines("a\r\nb\r\n")).toEqual(["a", "b", ""]);
    expect(splitSourceLines("solo")).toEqual(["solo"]);
  });

  it("handles lone CR", () => {
    expect(splitSourceLines("x\ry")).toEqual(["x", "y"]);
  });
});

describe("lineNumbersForSource / lineNumberGutterWidth", () => {
  it("numbers from 1", () => {
    expect(lineNumbersForSource("a\nb\nc")).toEqual([1, 2, 3]);
    expect(lineNumbersForSource("")).toEqual([1]);
  });

  it("gutter width scales with line count", () => {
    expect(lineNumberGutterWidth(1)).toBe(1);
    expect(lineNumberGutterWidth(9)).toBe(1);
    expect(lineNumberGutterWidth(10)).toBe(2);
    expect(lineNumberGutterWidth(100)).toBe(3);
    expect(lineNumberGutterWidth(0)).toBe(1);
  });
});

describe("tokenizeTemplateLine / highlightTemplateSource", () => {
  it("tokenizes HTML tags and attributes", () => {
    const tokens = tokenizeTemplateLine('<div class="main">');
    const kinds = tokens.map((t) => t.kind);
    expect(kinds).toContain("tag");
    expect(kinds).toContain("attr");
    expect(kinds).toContain("string");
    expect(tokens.map((t) => t.text).join("")).toBe('<div class="main">');
  });

  it("tokenizes Velocity directives and variables", () => {
    const tokens = tokenizeTemplateLine("#if($foo.bar) $!{x}");
    const kinds = tokens.map((t) => t.kind);
    expect(kinds).toContain("directive");
    expect(kinds).toContain("variable");
    expect(tokens.some((t) => t.text === "#if")).toBe(true);
    expect(tokens.some((t) => t.text === "$foo.bar")).toBe(true);
    expect(tokens.some((t) => t.text === "$!{x}")).toBe(true);
  });

  it("tokenizes HTML comments and ## comments", () => {
    expect(tokenizeTemplateLine("<!-- c -->")[0]?.kind).toBe("comment");
    expect(tokenizeTemplateLine("## note")[0]?.kind).toBe("comment");
  });

  it("returns plain empty token for blank line", () => {
    expect(tokenizeTemplateLine("")).toEqual([{ kind: "plain", text: "" }]);
  });

  it("highlights multi-line source with 1-based line numbers", () => {
    const lines = highlightTemplateSource("<html>\n#set($a = 1)\n");
    expect(lines).toHaveLength(3);
    expect(lines[0]?.lineNumber).toBe(1);
    expect(lines[1]?.tokens.some((t) => t.kind === "directive")).toBe(true);
    expect(lines[2]?.lineNumber).toBe(3);
  });
});

describe("copyTextToClipboard / copyTextViaExecCommand", () => {
  afterEach(() => {
    vi.unstubAllGlobals();
    vi.restoreAllMocks();
  });

  /** jsdom has no document.execCommand — install a stub before spying. */
  function stubExecCommand(result: boolean): ReturnType<typeof vi.fn> {
    const exec = vi.fn().mockReturnValue(result);
    Object.defineProperty(document, "execCommand", {
      configurable: true,
      writable: true,
      value: exec,
    });
    return exec;
  }

  it("uses navigator.clipboard.writeText when available", async () => {
    const writeText = vi.fn().mockResolvedValue(undefined);
    vi.stubGlobal("navigator", { clipboard: { writeText } });
    await expect(copyTextToClipboard("hello")).resolves.toBe(true);
    expect(writeText).toHaveBeenCalledWith("hello");
  });

  it("falls back when Clipboard API rejects", async () => {
    const writeText = vi.fn().mockRejectedValue(new Error("denied"));
    vi.stubGlobal("navigator", { clipboard: { writeText } });
    const exec = stubExecCommand(true);
    await expect(copyTextToClipboard("x")).resolves.toBe(true);
    expect(exec).toHaveBeenCalledWith("copy");
  });

  it("copyTextViaExecCommand returns false when execCommand fails", () => {
    stubExecCommand(false);
    expect(copyTextViaExecCommand("nope")).toBe(false);
  });

  it("copyTextViaExecCommand returns true when execCommand succeeds", () => {
    stubExecCommand(true);
    expect(copyTextViaExecCommand("ok")).toBe(true);
  });
});
