/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Pure helpers for the Developer template source viewer (UI-SRC-01).
 *
 * Lightweight line split, clipboard copy, and token highlight for CMS
 * template source (HTML + Velocity-style directives / variables). No Monaco,
 * CodeMirror, or Prism dependency.
 */

/** Token kinds used for lightweight template-source highlighting. */
export type SourceTokenKind =
  | "plain"
  | "tag"
  | "attr"
  | "string"
  | "comment"
  | "directive"
  | "variable";

export interface SourceToken {
  kind: SourceTokenKind;
  text: string;
}

export interface HighlightedLine {
  /** 1-based line number. */
  lineNumber: number;
  tokens: SourceToken[];
}

/**
 * Split source into display lines. Empty string yields a single empty line so
 * the gutter still shows line 1. Accepts LF, CRLF, and lone CR.
 */
export function splitSourceLines(source: string): string[] {
  if (source.length === 0) {
    return [""];
  }
  return source.split(/\r\n|\n|\r/);
}

/** Digit count for a fixed-width line-number gutter. */
export function lineNumberGutterWidth(lineCount: number): number {
  const n = Math.max(1, lineCount);
  return String(n).length;
}

/**
 * Build 1-based line numbers for a source string (for gutter rendering).
 */
export function lineNumbersForSource(source: string): number[] {
  const lines = splitSourceLines(source);
  return lines.map((_, i) => i + 1);
}

/**
 * Lightweight per-line tokenizer for HTML / Velocity-ish template source.
 * Line-oriented (comments that span lines are not fully tracked) — enough for
 * a readable preview without a full parser.
 */
export function tokenizeTemplateLine(line: string): SourceToken[] {
  if (line.length === 0) {
    return [{ kind: "plain", text: "" }];
  }

  const tokens: SourceToken[] = [];
  let i = 0;

  const push = (kind: SourceTokenKind, text: string) => {
    if (text.length === 0) return;
    const last = tokens[tokens.length - 1];
    if (last && last.kind === kind) {
      last.text += text;
    } else {
      tokens.push({ kind, text });
    }
  };

  while (i < line.length) {
    // HTML comment
    if (line.startsWith("<!--", i)) {
      const end = line.indexOf("-->", i + 4);
      if (end === -1) {
        push("comment", line.slice(i));
        break;
      }
      push("comment", line.slice(i, end + 3));
      i = end + 3;
      continue;
    }

    // Velocity multi-line comment start/end markers on this line
    if (line.startsWith("#*", i)) {
      const end = line.indexOf("*#", i + 2);
      if (end === -1) {
        push("comment", line.slice(i));
        break;
      }
      push("comment", line.slice(i, end + 2));
      i = end + 2;
      continue;
    }

    // Velocity single-line comment
    if (line.startsWith("##", i)) {
      push("comment", line.slice(i));
      break;
    }

    // Velocity / JEXL-style directive: #if, #set, #foreach, #end, #else, …
    if (line[i] === "#" && i + 1 < line.length && /[a-zA-Z]/.test(line[i + 1]!)) {
      let j = i + 1;
      while (j < line.length && /[a-zA-Z0-9_]/.test(line[j]!)) {
        j++;
      }
      push("directive", line.slice(i, j));
      i = j;
      continue;
    }

    // Variables: $name, $!name, ${name}, $!{name}
    if (line[i] === "$") {
      let j = i + 1;
      if (j < line.length && line[j] === "!") j++;
      if (j < line.length && line[j] === "{") {
        const close = line.indexOf("}", j + 1);
        if (close !== -1) {
          push("variable", line.slice(i, close + 1));
          i = close + 1;
          continue;
        }
      } else if (j < line.length && /[a-zA-Z_]/.test(line[j]!)) {
        while (j < line.length && /[a-zA-Z0-9_.]/.test(line[j]!)) {
          j++;
        }
        push("variable", line.slice(i, j));
        i = j;
        continue;
      }
      push("plain", "$");
      i++;
      continue;
    }

    // HTML tag open/close
    if (line[i] === "<" && i + 1 < line.length && /[a-zA-Z/!]/.test(line[i + 1]!)) {
      const close = line.indexOf(">", i + 1);
      const tagEnd = close === -1 ? line.length : close + 1;
      const tagText = line.slice(i, tagEnd);
      // Split tag into tag name region vs quoted attrs (simple)
      tokenizeHtmlTag(tagText, push);
      i = tagEnd;
      continue;
    }

    // Quoted string outside tags (fallback)
    if (line[i] === '"' || line[i] === "'") {
      const q = line[i]!;
      let j = i + 1;
      while (j < line.length && line[j] !== q) {
        if (line[j] === "\\") j++;
        j++;
      }
      if (j < line.length) j++;
      push("string", line.slice(i, j));
      i = j;
      continue;
    }

    // Plain run until next special
    let j = i + 1;
    while (j < line.length) {
      const c = line[j]!;
      if (
        c === "<" ||
        c === "$" ||
        c === "#" ||
        c === '"' ||
        c === "'" ||
        (c === "<" && line.startsWith("<!--", j))
      ) {
        break;
      }
      // Stop before ## or #* or #directive
      if (c === "#" && j + 1 < line.length) {
        const n = line[j + 1]!;
        if (n === "#" || n === "*" || /[a-zA-Z]/.test(n)) break;
      }
      j++;
    }
    push("plain", line.slice(i, j));
    i = j;
  }

  return tokens.length > 0 ? tokens : [{ kind: "plain", text: line }];
}

function tokenizeHtmlTag(
  tagText: string,
  push: (kind: SourceTokenKind, text: string) => void,
): void {
  // Very small tag splitter: leading <name /attrs> with quoted strings as attr values
  let i = 0;
  // opening < and name / closing </
  if (tagText[0] === "<") {
    let j = 1;
    if (j < tagText.length && tagText[j] === "/") j++;
    if (j < tagText.length && tagText[j] === "!") {
      // <!DOCTYPE or similar — treat rest as tag
      push("tag", tagText);
      return;
    }
    while (j < tagText.length && /[a-zA-Z0-9:_-]/.test(tagText[j]!)) {
      j++;
    }
    push("tag", tagText.slice(0, j));
    i = j;
  }

  while (i < tagText.length) {
    if (tagText[i] === '"' || tagText[i] === "'") {
      const q = tagText[i]!;
      let j = i + 1;
      while (j < tagText.length && tagText[j] !== q) j++;
      if (j < tagText.length) j++;
      push("string", tagText.slice(i, j));
      i = j;
      continue;
    }
    if (tagText[i] === ">" || (tagText[i] === "/" && tagText[i + 1] === ">")) {
      push("tag", tagText.slice(i));
      break;
    }
    // attribute name / punctuation
    let j = i + 1;
    while (
      j < tagText.length &&
      tagText[j] !== '"' &&
      tagText[j] !== "'" &&
      tagText[j] !== ">" &&
      !(tagText[j] === "/" && tagText[j + 1] === ">")
    ) {
      j++;
    }
    push("attr", tagText.slice(i, j));
    i = j;
  }
}

/** Highlight full source into numbered token lines. */
export function highlightTemplateSource(source: string): HighlightedLine[] {
  return splitSourceLines(source).map((line, idx) => ({
    lineNumber: idx + 1,
    tokens: tokenizeTemplateLine(line),
  }));
}

/** Default CSS color per token kind (inline-style friendly). */
export const SOURCE_TOKEN_COLORS: Readonly<Record<SourceTokenKind, string>> = {
  plain: "#1a202c",
  tag: "#2b6cb0",
  attr: "#805ad5",
  string: "#c05621",
  comment: "#718096",
  directive: "#2f855a",
  variable: "#b83280",
};

/**
 * Copy text to the system clipboard. Uses Clipboard API when available;
 * falls back to a temporary textarea + {@code document.execCommand("copy")}.
 * Returns true on success.
 */
export async function copyTextToClipboard(text: string): Promise<boolean> {
  if (typeof navigator !== "undefined" && navigator.clipboard?.writeText) {
    try {
      await navigator.clipboard.writeText(text);
      return true;
    } catch {
      // fall through to legacy path
    }
  }
  return copyTextViaExecCommand(text);
}

/**
 * Legacy clipboard path (sync). Exported for unit tests without Clipboard API.
 */
export function copyTextViaExecCommand(text: string): boolean {
  if (typeof document === "undefined") {
    return false;
  }
  const ta = document.createElement("textarea");
  ta.value = text;
  ta.setAttribute("readonly", "");
  ta.style.position = "fixed";
  ta.style.left = "-9999px";
  ta.style.top = "0";
  document.body.appendChild(ta);
  ta.focus();
  ta.select();
  let ok = false;
  try {
    ok = document.execCommand("copy");
  } catch {
    ok = false;
  }
  document.body.removeChild(ta);
  return ok;
}
