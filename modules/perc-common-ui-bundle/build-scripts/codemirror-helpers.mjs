/**
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
 * codemirror-helpers.mjs
 *
 * Shared helpers for the npm-managed CodeMirror 5 bundle (issue #5784).
 *
 * The classic template editor (`editTemplate_js.jsp`) and the `sys_TextArea`
 * code-editor control (`sys_Templates.xsl`) consume the bundle through the
 * browser global `CodeMirror`. The UMD wrapper inside
 * `codemirror/lib/codemirror.js` assigns to its bundler module record instead
 * of the browser global when bundled, so the entry exposes the shared
 * instance explicitly. The import is unwrapped defensively: depending on the
 * esbuild CJS/ESM interop path the constructor may sit one namespace level
 * deep.
 *
 * Kept in a separate module (rather than inline in the entry) so the
 * unwrapping contract is unit-tested by
 * `src/test/js/codemirror-bundle.test.js`.
 */

/**
 * Unwraps the CodeMirror 5 constructor out of an esbuild module record.
 *
 * @param {*} moduleRecord the `import * as …` namespace (or any level of
 *   `{ default: … }` nesting around the constructor).
 * @returns {Function|*} the constructor function when found, otherwise the
 *   deepest unwrapped candidate.
 */
export function unwrapCodeMirror(moduleRecord) {
  let candidate =
    moduleRecord && moduleRecord.default !== undefined
      ? moduleRecord.default
      : moduleRecord;
  while (
    candidate &&
    typeof candidate === "object" &&
    typeof candidate.default === "function"
  ) {
    candidate = candidate.default;
  }
  return candidate;
}

/**
 * Markers the built `codemirror.bundle.js` must contain. They prove the
 * htmlmixed mode plus the search/matchbrackets addons the classic consumers
 * rely on survived tree-shaking/minification. `openDialog` pins the dialog
 * addon the find/replace commands (`find`, `replace`, `replaceAll`) render
 * through (issue #6465: template editor source-view find/replace).
 * `CodeMirror-lint-markers` pins the lint addon gutter the template editor
 * source-view validation renders through, and `percValidateTemplateHtml`
 * pins the dependency-free tag-balance validator the entry registers as the
 * htmlmixed/xml/html lint helper (issue #6466: syntax validation markers
 * before save). `percCodeEditorPrefs` pins the persisted dark-mode +
 * font-size preference namespace the template editor source-view toolbar
 * reads and writes (issue #6467), and `material-darker` pins the dark theme
 * the preference namespace can switch the editor to.
 */
export const CODEMIRROR_BUNDLE_MARKERS = [
  "htmlmixed",
  "searchcursor",
  "matchBrackets",
  "openDialog",
  "CodeMirror-lint-markers",
  "percValidateTemplateHtml",
  "percCodeEditorPrefs",
  "material-darker",
];

/**
 * The CodeMirror major line this bundle tracks. Pinned to 5 until issue
 * #5701 lands the CodeMirror 6 replacement; the vitest suite fails the build
 * if the npm dependency drifts off this line.
 */
export const CODEMIRROR_PINNED_MAJOR = 5;

/**
 * HTML void elements: never need a closing tag, so an opener without one is
 * not unbalanced (issue #6466).
 */
const PERC_VOID_ELEMENTS = new Set([
  "area",
  "base",
  "br",
  "col",
  "embed",
  "hr",
  "img",
  "input",
  "link",
  "meta",
  "param",
  "source",
  "track",
  "wbr",
]);

/**
 * Upper bound on annotations per validation pass. The template code-insert
 * fields are small snippets; beyond this the gutter is noise, so scanning
 * stops early instead of flooding the editor.
 */
const PERC_MAX_HTML_LINT_ANNOTATIONS = 100;

function isPercTagNameChar(code) {
  return !(
    code === 32 || // space
    code === 9 || // tab
    code === 10 || // LF
    code === 13 || // CR
    code === 12 || // FF
    code === 47 || // /
    code === 62 || // >
    code === 34 || // "
    code === 39 || // '
    code === 61 // =
  );
}

function createPercHtmlScanner(text) {
  return {
    source: String(text == null ? "" : text),
    index: 0,
    line: 0,
    ch: 0,
  };
}

function scannerPosition(scanner) {
  return { line: scanner.line, ch: scanner.ch };
}

// Advances past one character, folding CRLF into a single line break so
// Windows-edited templates report the same lines as Unix ones.
function advancePercScanner(scanner) {
  const source = scanner.source;
  if (source[scanner.index] === "\r" && source[scanner.index + 1] === "\n") {
    scanner.index += 2;
  } else {
    scanner.index += 1;
  }
  if (source[scanner.index - 1] === "\n" || source[scanner.index - 1] === "\r") {
    scanner.line += 1;
    scanner.ch = 0;
  } else {
    scanner.ch += 1;
  }
}

function advancePercScannerTo(scanner, target) {
  while (scanner.index < target) {
    advancePercScanner(scanner);
  }
}

// Scans a tag starting just after `<name`, stopping after the `>` that
// ends it (quote- and paren-aware). Returns the index just past `>`, or
// -1 when the tag never terminates.
// Advances one character of a tag-end scan. Returns true when `code`
// terminates the tag.
function stepPercTagEndScan(state, code) {
  if (state.quote !== 0) {
    if (code === state.quote) {
      state.quote = 0;
    }
    return false;
  }
  if (code === 34 || code === 39) {
    state.quote = code;
    return false;
  }
  if (code === 40) {
    state.depth += 1;
    return false;
  }
  if (code === 41 && state.depth > 0) {
    state.depth -= 1;
    return false;
  }
  return code === 62 && state.depth === 0;
}

function scanPercTagEnd(source, from) {
  const state = { quote: 0, depth: 0 };
  let cursor = from;
  while (cursor < source.length) {
    if (stepPercTagEndScan(state, source.charCodeAt(cursor))) {
      return cursor + 1;
    }
    cursor += 1;
  }
  return -1;
}

function parsePercTagName(source, from) {
  let cursor = from;
  while (
    cursor < source.length &&
    isPercTagNameChar(source.charCodeAt(cursor))
  ) {
    cursor += 1;
  }
  return { name: source.slice(from, cursor), end: cursor };
}

function pushPercAnnotation(annotations, annotation) {
  if (annotations.length < PERC_MAX_HTML_LINT_ANNOTATIONS) {
    annotations.push(annotation);
  }
}

function percAnnotationsCapped(annotations) {
  return annotations.length >= PERC_MAX_HTML_LINT_ANNOTATIONS;
}

// Consumes `<!-- ... -->`. Returns true when scanning must stop (an
// unterminated comment swallows the rest of the input).
function consumePercComment(scanner, annotations) {
  const source = scanner.source;
  const start = scannerPosition(scanner);
  const end = source.indexOf("-->", scanner.index + 4);
  if (end === -1) {
    advancePercScannerTo(scanner, source.length);
    pushPercAnnotation(annotations, {
      from: start,
      to: scannerPosition(scanner),
      message: "Unclosed HTML comment: no --> terminator.",
      severity: "error",
    });
    return true;
  }
  advancePercScannerTo(scanner, end + 3);
  return false;
}

// Consumes `<![CDATA[ ... ]]>`. Returns true when scanning must stop.
function consumePercCdata(scanner, annotations) {
  const source = scanner.source;
  const start = scannerPosition(scanner);
  const end = source.indexOf("]]>", scanner.index + 9);
  if (end === -1) {
    advancePercScannerTo(scanner, source.length);
    pushPercAnnotation(annotations, {
      from: start,
      to: scannerPosition(scanner),
      message: "Unclosed CDATA section: no ]]> terminator.",
      severity: "error",
    });
    return true;
  }
  advancePercScannerTo(scanner, end + 3);
  return false;
}

// Consumes a doctype or other `<! ... >` declaration (opaque, never
// balanced-checked). Returns true only when it never terminates.
function consumePercDeclaration(scanner) {
  const end = scanPercTagEnd(scanner.source, scanner.index + 2);
  if (end === -1) {
    return true;
  }
  advancePercScannerTo(scanner, end);
  return false;
}

// Consumes a `<? ... ?>` processing instruction. Returns true only when it
// never terminates.
function consumePercProcessingInstruction(scanner) {
  const source = scanner.source;
  const close = source.indexOf("?>", scanner.index + 2);
  if (close !== -1) {
    advancePercScannerTo(scanner, close + 2);
    return false;
  }
  const end = scanPercTagEnd(source, scanner.index + 2);
  if (end === -1) {
    return true;
  }
  advancePercScannerTo(scanner, end);
  return false;
}

function findPercOpenTagDepth(stack, lowerName) {
  for (let level = stack.length - 1; level >= 0; level -= 1) {
    if (stack[level].name === lowerName) {
      return level;
    }
  }
  return -1;
}

function matchPercCloseTag(stack, annotations, name, closeStart, closeEnd) {
  const depth = findPercOpenTagDepth(stack, name.toLowerCase());
  if (depth === -1) {
    pushPercAnnotation(annotations, {
      from: closeStart,
      to: closeEnd,
      message: `Stray closing tag </${name}> with no matching open tag.`,
      severity: "error",
    });
    return;
  }
  if (stack.length - 1 === depth) {
    stack.pop();
    return;
  }
  const expected = stack[stack.length - 1];
  while (stack.length - 1 > depth && !percAnnotationsCapped(annotations)) {
    const unclosed = stack.pop();
    pushPercAnnotation(annotations, {
      from: unclosed.start,
      to: unclosed.end,
      message: `Unclosed <${unclosed.rawName}> tag: closed by </${name}> before it was closed.`,
      severity: "warning",
    });
  }
  stack.pop();
  pushPercAnnotation(annotations, {
    from: closeStart,
    to: closeEnd,
    message: `Mismatched closing tag </${name}>: <${expected.rawName}> was still open.`,
    severity: "error",
  });
}

// Consumes a `</name>` close tag. Returns true when scanning must stop.
function consumePercCloseTag(scanner, stack, annotations) {
  const source = scanner.source;
  const tagStart = scannerPosition(scanner);
  let cursor = scanner.index + 2;
  while (cursor < source.length && /\s/.test(source[cursor])) {
    cursor += 1;
  }
  const parsed = parsePercTagName(source, cursor);
  if (parsed.name === "") {
    advancePercScanner(scanner);
    return false;
  }
  const tagEnd = scanPercTagEnd(source, parsed.end);
  if (tagEnd === -1) {
    advancePercScannerTo(scanner, source.length);
    pushPercAnnotation(annotations, {
      from: tagStart,
      to: scannerPosition(scanner),
      message: `Unclosed tag </${parsed.name}>: no > terminator.`,
      severity: "error",
    });
    return true;
  }
  advancePercScannerTo(scanner, tagEnd);
  matchPercCloseTag(
    stack,
    annotations,
    parsed.name,
    tagStart,
    scannerPosition(scanner),
  );
  return percAnnotationsCapped(annotations);
}

function isPercSelfClosingTag(source, tagOpenIndex, tagEnd) {
  let before = tagEnd - 2;
  while (before > tagOpenIndex && /\s/.test(source[before])) {
    before -= 1;
  }
  return source[before] === "/";
}

// Consumes a `<script>` / `<style>` element: the tags participate in
// balancing, but the element content is skipped with a raw scan for the
// matching closer so JS/CSS quotes and comparisons cannot desynchronize
// the parser. Returns true when scanning must stop.
function consumePercOpaqueElement(
  scanner,
  source,
  stack,
  annotations,
  parsed,
  tagRange,
) {
  const lower = parsed.name.toLowerCase();
  const closer = new RegExp(`</${lower}\\s*>`, "gi");
  closer.lastIndex = scanner.index;
  const found = closer.exec(source);
  stack.push({
    name: lower,
    rawName: parsed.name,
    start: tagRange.start,
    end: tagRange.end,
  });
  if (!found) {
    pushPercAnnotation(annotations, {
      from: tagRange.start,
      to: tagRange.end,
      message: `Unclosed <${parsed.name}> tag: no matching </${parsed.name}> found.`,
      severity: "warning",
    });
    stack.pop();
    return true;
  }
  advancePercScannerTo(scanner, found.index);
  return percAnnotationsCapped(annotations);
}

// Consumes a `<name ...>` open tag. Returns true when scanning must stop.
function consumePercOpenTag(scanner, stack, annotations) {
  const source = scanner.source;
  const tagStart = scannerPosition(scanner);
  const parsed = parsePercTagName(source, scanner.index + 1);
  const tagEnd = scanPercTagEnd(source, parsed.end);
  if (tagEnd === -1) {
    advancePercScannerTo(scanner, source.length);
    pushPercAnnotation(annotations, {
      from: tagStart,
      to: scannerPosition(scanner),
      message: `Unclosed <${parsed.name}> tag: no > terminator.`,
      severity: "warning",
    });
    return true;
  }
  const lower = parsed.name.toLowerCase();
  const selfClosing = isPercSelfClosingTag(source, scanner.index, tagEnd);
  advancePercScannerTo(scanner, tagEnd);
  const tagRange = { start: tagStart, end: scannerPosition(scanner) };
  if (selfClosing || PERC_VOID_ELEMENTS.has(lower)) {
    return percAnnotationsCapped(annotations);
  }
  if (lower === "script" || lower === "style") {
    return consumePercOpaqueElement(
      scanner,
      source,
      stack,
      annotations,
      parsed,
      tagRange,
    );
  }
  stack.push({
    name: lower,
    rawName: parsed.name,
    start: tagRange.start,
    end: tagRange.end,
  });
  return percAnnotationsCapped(annotations);
}

function consumePercBangConstruct(scanner, annotations) {
  const source = scanner.source;
  if (source.startsWith("<!--", scanner.index)) {
    return consumePercComment(scanner, annotations);
  }
  if (source.startsWith("<![CDATA[", scanner.index)) {
    return consumePercCdata(scanner, annotations);
  }
  return consumePercDeclaration(scanner);
}

// Consumes one `<...>` construct. Returns true when scanning must stop.
function consumePercTagConstruct(scanner, stack, annotations) {
  const source = scanner.source;
  const second = source[scanner.index + 1];
  if (second === "!") {
    return consumePercBangConstruct(scanner, annotations);
  }
  if (second === "?") {
    return consumePercProcessingInstruction(scanner);
  }
  if (second === "/") {
    return consumePercCloseTag(scanner, stack, annotations);
  }
  if (!/[A-Za-z]/.test(second || "")) {
    advancePercScanner(scanner);
    return false;
  }
  return consumePercOpenTag(scanner, stack, annotations);
}

function flushPercUnclosedTags(stack, annotations) {
  while (stack.length > 0 && !percAnnotationsCapped(annotations)) {
    const unclosed = stack.pop();
    pushPercAnnotation(annotations, {
      from: unclosed.start,
      to: unclosed.end,
      message: `Unclosed <${unclosed.rawName}> tag: no matching closing tag before the end of the content.`,
      severity: "warning",
    });
  }
}

/**
 * Dependency-free HTML tag-balance validator for the classic template editor
 * source-view (issue #6466: syntax validation markers before save).
 *
 * (Behavior contract — see the original JSDoc in git history for the full
 * rationale. The scan is split into single-purpose module helpers above so
 * each unit stays under the complexity gate; the orchestrator below only
 * drives the loop.)
 *
 * Positions are 0-based `{ line, ch }` pairs in CodeMirror lint-annotation
 * shape (`from`/`to`/`message`/`severity`), sorted by position.
 *
 * @param {string} text the editor content to validate.
 * @returns {Array<{from:{line:number,ch:number},to:{line:number,ch:number},message:string,severity:string}>}
 *   the structural findings, empty when the markup is balanced.
 */
export function findPercTemplateHtmlIssues(text) {
  const scanner = createPercHtmlScanner(text);
  const stack = [];
  const annotations = [];
  while (
    scanner.index < scanner.source.length &&
    !percAnnotationsCapped(annotations)
  ) {
    if (scanner.source[scanner.index] !== "<") {
      advancePercScanner(scanner);
      continue;
    }
    if (consumePercTagConstruct(scanner, stack, annotations)) {
      break;
    }
  }
  flushPercUnclosedTags(stack, annotations);
  annotations.sort(
    (left, right) =>
      left.from.line - right.from.line || left.from.ch - right.from.ch,
  );
  return annotations;
}


/**
 * Mode names the template HTML lint helper is registered under. The
 * `sys_TextArea` code-editor control runs in `htmlmixed` mode, but the lint
 * addon resolves its helper through `getModeAt(Pos(0, 0))`, and
 * `htmlmixed.innerMode` at document start returns the inner `xml` mode
 * (name `xml`) — registering only `htmlmixed` would silently disable
 * validation for markup-led content. `html` covers MIME-aliased consumers.
 * The validator itself is mode-agnostic (pure text), so one function serves
 * all three names.
 */
export const PERC_TEMPLATE_HTML_LINT_MODES = ["htmlmixed", "xml", "html"];

/**
 * Registers the template HTML validator as the CodeMirror 5 `lint` helper
 * for the modes the classic template editor source-view resolves (see
 * `PERC_TEMPLATE_HTML_LINT_MODES`), so `lint: true` in `sys_Templates.xsl`
 * renders gutter markers before save (issue #6466). Returns false when the
 * constructor has no helper registry (defensive: the bundle entry still
 * assigns the global).
 *
 * @param {*} CodeMirror the CodeMirror 5 constructor (or a test double with
 *   `registerHelper` and `Pos`).
 * @returns {boolean} true when the helper was registered.
 */
export function registerPercTemplateHtmlLint(CodeMirror) {
  if (
    !CodeMirror ||
    typeof CodeMirror.registerHelper !== "function" ||
    typeof CodeMirror.Pos !== "function"
  ) {
    return false;
  }
  const getAnnotations = (text, options, cm) =>
    findPercTemplateHtmlIssues(text).map((annotation) => ({
      from: CodeMirror.Pos(annotation.from.line, annotation.from.ch),
      to: CodeMirror.Pos(annotation.to.line, annotation.to.ch),
      message: annotation.message,
      severity: annotation.severity,
    }));
  for (const mode of PERC_TEMPLATE_HTML_LINT_MODES) {
    CodeMirror.registerHelper("lint", mode, getAnnotations);
  }
  return true;
}

/**
 * localStorage key the template editor source-view persists its display
 * preferences under (issue #6467). One shared key for every `sys_TextArea`
 * code editor: the preference is the author's, not the field's.
 */
export const PERC_CODEEDITOR_PREFS_KEY = "perc.codeeditor.prefs";

/**
 * Theme names the source-view preference can switch between. `light` is the
 * CodeMirror 5 stock `default` theme; `dark` is the `material-darker` theme
 * shipped inside the npm bundle (imported by the bundle entry so no extra
 * request is needed). Only these two names are ever applied: anything else
 * read back from storage normalizes to `light`.
 */
export const PERC_CODEEDITOR_THEMES = {
  light: "default",
  dark: "material-darker",
};

/**
 * Font-size bounds (px) for the source-view preference. The editor default
 * is 13px; the steppers move 1px at a time and clamp here so the text can
 * neither vanish nor blow out the dialog layout.
 */
export const PERC_CODEEDITOR_FONT_SIZE_DEFAULT = 13;
export const PERC_CODEEDITOR_FONT_SIZE_MIN = 11;
export const PERC_CODEEDITOR_FONT_SIZE_MAX = 24;
export const PERC_CODEEDITOR_FONT_SIZE_STEP = 1;

/**
 * Normalizes a stored theme value to a theme the bundle ships. Unknown,
 * empty, or non-string values fall back to the light theme so a corrupt
 * preference can never leave the editor theme-less.
 *
 * @param {*} value the stored theme name.
 * @returns {string} `material-darker` for the dark theme, else `default`.
 */
export function normalizePercCodeEditorTheme(value) {
  if (value === PERC_CODEEDITOR_THEMES.dark) {
    return PERC_CODEEDITOR_THEMES.dark;
  }
  return PERC_CODEEDITOR_THEMES.light;
}

/**
 * Clamps a stored font size into the supported range. Non-numeric, empty,
 * or out-of-range values fall back to the editor default so a corrupt
 * preference can never shrink the text away or blow out the dialog.
 *
 * @param {*} value the stored font size (px).
 * @returns {number} an integer within [MIN, MAX].
 */
export function clampPercCodeEditorFontSize(value) {
  // Only numbers and non-blank numeric strings count: Number("") and
  // Number([]) both coerce to 0, which would silently shrink the editor to
  // the minimum instead of falling back to the default.
  if (typeof value === "string") {
    if (value.trim() === "") {
      return PERC_CODEEDITOR_FONT_SIZE_DEFAULT;
    }
  } else if (typeof value !== "number") {
    return PERC_CODEEDITOR_FONT_SIZE_DEFAULT;
  }
  const parsed = Number(value);
  if (!Number.isFinite(parsed)) {
    return PERC_CODEEDITOR_FONT_SIZE_DEFAULT;
  }
  const rounded = Math.round(parsed);
  if (rounded < PERC_CODEEDITOR_FONT_SIZE_MIN) {
    return PERC_CODEEDITOR_FONT_SIZE_MIN;
  }
  if (rounded > PERC_CODEEDITOR_FONT_SIZE_MAX) {
    return PERC_CODEEDITOR_FONT_SIZE_MAX;
  }
  return rounded;
}

/**
 * The preferences a first-time author gets: light theme, default size.
 *
 * @returns {{theme:string,fontSize:number}} a fresh defaults object.
 */
export function defaultPercCodeEditorPrefs() {
  return {
    theme: PERC_CODEEDITOR_THEMES.light,
    fontSize: PERC_CODEEDITOR_FONT_SIZE_DEFAULT,
  };
}

/**
 * Reads the persisted source-view preferences. Never throws and never
 * returns a partial object: missing storage (private mode / disabled
 * cookies), a storage read that throws, absent data, or unparseable JSON
 * all fall back to the defaults, with each field normalized independently
 * so one corrupt field does not discard the other.
 *
 * @param {*} storage a `localStorage`-like with `getItem`, or nullish.
 * @returns {{theme:string,fontSize:number}} the effective preferences.
 */
export function readPercCodeEditorPrefs(storage) {
  const prefs = defaultPercCodeEditorPrefs();
  if (!storage || typeof storage.getItem !== "function") {
    return prefs;
  }
  let raw = null;
  try {
    raw = storage.getItem(PERC_CODEEDITOR_PREFS_KEY);
  } catch (ignored) {
    return prefs;
  }
  if (raw === null || raw === undefined || raw === "") {
    return prefs;
  }
  let parsed = null;
  try {
    parsed = JSON.parse(raw);
  } catch (ignored) {
    return prefs;
  }
  if (parsed === null || typeof parsed !== "object") {
    return prefs;
  }
  return {
    theme: normalizePercCodeEditorTheme(parsed.theme),
    fontSize: clampPercCodeEditorFontSize(parsed.fontSize),
  };
}

/**
 * Persists the source-view preferences. Normalizes before writing so only
 * valid values ever reach storage. Never throws: a full or unavailable
 * store reports failure through the return value instead.
 *
 * @param {*} storage a `localStorage`-like with `setItem`, or nullish.
 * @param {{theme:string,fontSize:number}} prefs the preferences to store.
 * @returns {boolean} true when the write succeeded.
 */
export function writePercCodeEditorPrefs(storage, prefs) {
  if (!storage || typeof storage.setItem !== "function") {
    return false;
  }
  const safe = prefs === null || typeof prefs !== "object" ? {} : prefs;
  try {
    storage.setItem(
      PERC_CODEEDITOR_PREFS_KEY,
      JSON.stringify({
        theme: normalizePercCodeEditorTheme(safe.theme),
        fontSize: clampPercCodeEditorFontSize(safe.fontSize),
      }),
    );
    return true;
  } catch (ignored) {
    return false;
  }
}

/**
 * Steps the editor font size by one increment, clamped to the supported
 * range. Pure: the toolbar calls this, then writes + applies the result.
 *
 * @param {*} current the current size (px); garbage means the default.
 * @param {number} delta +1 to grow, -1 to shrink (any number works).
 * @returns {number} the next size within [MIN, MAX].
 */
export function nextPercCodeEditorFontSize(current, delta) {
  const step = Number(delta);
  const base = clampPercCodeEditorFontSize(current);
  if (!Number.isFinite(step) || step === 0) {
    return base;
  }
  return clampPercCodeEditorFontSize(
    base + Math.sign(step) * PERC_CODEEDITOR_FONT_SIZE_STEP,
  );
}

/**
 * Flips the theme preference light <-> dark, keeping the font size.
 *
 * @param {{theme:string,fontSize:number}} prefs the current preferences.
 * @returns {{theme:string,fontSize:number}} the toggled preferences.
 */
export function togglePercCodeEditorTheme(prefs) {
  const safe = prefs === null || typeof prefs !== "object" ? {} : prefs;
  return {
    theme:
      normalizePercCodeEditorTheme(safe.theme) ===
      PERC_CODEEDITOR_THEMES.dark
        ? PERC_CODEEDITOR_THEMES.light
        : PERC_CODEEDITOR_THEMES.dark,
    fontSize: clampPercCodeEditorFontSize(safe.fontSize),
  };
}

/**
 * Applies preferences to a live CodeMirror 5 instance: switches the theme
 * option and sets the wrapper font size, then refreshes so the new metrics
 * take effect. Defensive: a nullish editor, a missing `setOption`, or a
 * throwing instance reports failure instead of breaking dialog init — the
 * editor stays usable on its defaults.
 *
 * @param {*} editor the CodeMirror instance (or a test double with
 *   `setOption`/`getWrapperElement`/`refresh`).
 * @param {{theme:string,fontSize:number}} prefs the preferences to apply.
 * @returns {boolean} true when the theme + size were applied.
 */
export function applyPercCodeEditorPrefs(editor, prefs) {
  if (!editor || typeof editor.setOption !== "function") {
    return false;
  }
  const safe = prefs === null || typeof prefs !== "object" ? {} : prefs;
  try {
    editor.setOption("theme", normalizePercCodeEditorTheme(safe.theme));
    const size = clampPercCodeEditorFontSize(safe.fontSize);
    if (typeof editor.getWrapperElement === "function") {
      const wrapper = editor.getWrapperElement();
      if (wrapper && wrapper.style) {
        wrapper.style.fontSize = `${size}px`;
      }
    }
    if (typeof editor.refresh === "function") {
      editor.refresh();
    }
    return true;
  } catch (ignored) {
    return false;
  }
}
