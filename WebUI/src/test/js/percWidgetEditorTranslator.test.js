/**
 * Widget TipTap editor (issue #5901): translator contract, editor lifecycle,
 * TinyMCE facade, and packaging cutover assertions for perc.widget.form +
 * perc.widget.commentForm.
 *
 * Behavioral coverage runs the real `PercWidgetEditor.js` controller in
 * jsdom against a stubbed `__PercTipTap` vendor (the committed minified
 * TipTap bundle is shape-asserted separately and exercised for real in a
 * Chromium harness; see the PR body).
 */
import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import { describe, it, expect, beforeEach } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const PACKAGES = resolve(
  __dirname,
  "../../../../modules/perc-packages/src/main/resources/Packages",
);
const WIDGET_EDITOR_SRC = resolve(
  __dirname,
  "../../../../modules/perc-packages/widget-editor/src",
);
const FORM_JS = `${PACKAGES}/perc.widget.form/SupportFile-rx_resources/widgets/form/js`;
const COMMENTS_JS = `${PACKAGES}/perc.widget.commentForm/SupportFile-rx_resources/widgets/commentsForm/js`;

// ---------------------------------------------------------------------------
// Vendor stub (consumed TipTap surface only)
// ---------------------------------------------------------------------------

class FakeEditor {
  constructor(opts) {
    this.options = { element: opts.element };
    this._html = opts.content || "";
    this._handlers = {};
    this._active = {};
    this._calls = [];
    this._supported = new Set([
      "toggleBold",
      "toggleItalic",
      "toggleUnderline",
      "undo",
      "redo",
      "setParagraph",
      "toggleHeading",
      "clearNodes",
      "unsetAllMarks",
      "insertContent",
      "setPercSiteClass",
      "unsetPercSiteClass",
    ]);
    this._selectedText = "";
    this.destroyed = false;
    this._onUpdate =
      opts && typeof opts.onUpdate === "function" ? opts.onUpdate : null;
    this.extensionNames = (opts.extensions || []).map((e) =>
      e && e.name ? e.name : "?",
    );
    FakeEditor.instances.push(this);
  }

  getHTML() {
    return this._html;
  }

  get commands() {
    const self = this;
    return {
      setContent(html) {
        self._html = html || "";
      },
      focus() {},
    };
  }

  chain() {
    const self = this;
    const api = { focus: () => api };
    const verbs = [
      "toggleBold",
      "toggleItalic",
      "toggleUnderline",
      "undo",
      "redo",
      "setParagraph",
      "clearNodes",
      "unsetAllMarks",
      "toggleHeading",
      "setPercSiteClass",
      "unsetPercSiteClass",
    ];
    for (const verb of verbs) {
      api[verb] = (arg) => {
        api._last = [verb, arg];
        return api;
      };
    }
    api.insertContent = (html) => {
      api._last = ["insertContent", html];
      return api;
    };
    api.run = () => {
      const [verb, arg] = api._last || [];
      self._calls.push([verb, arg]);
      if (verb === "setParagraph") self._html = `<p>${self._html}</p>`;
      else if (verb === "toggleHeading")
        self._html = `<h${arg.level}>${self._html}</h${arg.level}>`;
      else if (verb === "insertContent") self._html += arg;
      else if (verb === "clearNodes") self._html = self._html;
      if (self._onUpdate) self._onUpdate();
      return true;
    };
    return api;
  }

  can() {
    const self = this;
    const verbs = [
      "toggleBold",
      "toggleItalic",
      "toggleUnderline",
      "undo",
      "redo",
      "setParagraph",
      "toggleHeading",
      "clearNodes",
      "unsetAllMarks",
      "insertContent",
      "setPercSiteClass",
      "unsetPercSiteClass",
    ];
    const cmdMap = { focus: () => cmdMap };
    for (const verb of verbs) {
      cmdMap[verb] = () => ({ run: () => self._supported.has(verb) });
    }
    return { chain: () => cmdMap };
  }

  on(evt, fn) {
    (this._handlers[evt] = this._handlers[evt] || []).push(fn);
  }

  fire(evt, arg) {
    for (const fn of this._handlers[evt] || []) fn(arg);
  }

  isActive(cmd) {
    return !!this._active[cmd];
  }

  destroy() {
    this.destroyed = true;
  }

  get state() {
    const self = this;
    return {
      selection: { from: 0, to: 0 },
      doc: { textBetween: () => self._selectedText },
    };
  }
}
FakeEditor.instances = [];

function fakeVendor() {
  return {
    Editor: FakeEditor,
    Mark: { create: (spec) => ({ name: spec.name, __fakeMark: true }) },
    StarterKit: { configure: () => ({ name: "starterKit" }) },
    Underline: { name: "underline" },
    version: "3.33.0",
    engine: "tiptap",
  };
}

function loadController() {
  delete window.PercWidgetEditor;
  delete window.tinymce;
  delete window.__percPendingEditorSettings;
  delete window.__percDefaultEditorSettings;
  delete window.__PercTipTap;
  FakeEditor.instances = [];
  const src = readFileSync(
    resolve(WIDGET_EDITOR_SRC, "PercWidgetEditor.js"),
    "utf8",
  );
  window.eval(src);
  return window.PercWidgetEditor;
}

function addTextarea(id, value) {
  const ta = document.createElement("textarea");
  ta.id = id;
  ta.value = value || "";
  document.body.appendChild(ta);
  return ta;
}

beforeEach(() => {
  document.body.innerHTML = "";
  document.head
    .querySelectorAll("[data-perc-widget-editor],[data-perc-widget-editor-css]")
    .forEach((n) => n.remove());
});

// ---------------------------------------------------------------------------
// Translator (pure)
// ---------------------------------------------------------------------------

describe("PercWidgetEditor.translateConfig", () => {
  it("parses the shipped toolbar string into the subset", () => {
    const api = loadController();
    const t = api.translateConfig({
      toolbar: ["bold italic underline undo redo styleselect "],
    });
    expect(t.buttons).toEqual({
      bold: true,
      italic: true,
      underline: true,
      undo: true,
      redo: true,
      styleselect: true,
    });
  });

  it("accepts a plain toolbar string and drops unknown tokens", () => {
    const api = loadController();
    const t = api.translateConfig({
      toolbar: "bold forecolor | italic pastetext underline",
      content_css: "/sites/site1/css/main.css",
      auto_focus: "elm1",
      branding: false,
    });
    expect(t.buttons).toEqual({ bold: true, italic: true, underline: true });
    expect(t.contentCss).toBe("/sites/site1/css/main.css");
    expect(t.autoFocus).toBe("elm1");
    expect(t.branding).toBe(false);
  });

  it("falls back to the shipped subset on missing/empty toolbar", () => {
    const api = loadController();
    expect(Object.keys(api.translateConfig({}).buttons).sort()).toEqual(
      ["bold", "italic", "redo", "styleselect", "underline", "undo"].sort(),
    );
    expect(Object.keys(api.translateConfig(null).buttons)).toHaveLength(6);
  });

  it("keeps the setup callback verbatim", () => {
    const api = loadController();
    const setup = () => {};
    expect(api.translateConfig({ setup }).setup).toBe(setup);
    expect(api.translateConfig({}).setup).toBeNull();
  });
});

describe("PercWidgetEditor URL/CSS helpers", () => {
  it("sanitizeCssUrl rejects javascript: and data: URLs", () => {
    const api = loadController();
    expect(api.sanitizeCssUrl("javascript:alert(1)")).toBe("");
    expect(api.sanitizeCssUrl("  JaVaScRiPt:alert(1)")).toBe("");
    expect(api.sanitizeCssUrl("data:text/css,body{}")).toBe("");
    expect(api.sanitizeCssUrl("")).toBe("");
    expect(api.sanitizeCssUrl(null)).toBe("");
    expect(api.sanitizeCssUrl("/sites/s1/css/main.css")).toBe(
      "/sites/s1/css/main.css",
    );
    expect(api.sanitizeCssUrl("https://cdn.example/css/a.css")).toBe(
      "https://cdn.example/css/a.css",
    );
  });

  it("parseClassSelectors harvests classes, skips pseudos/comments", () => {
    const api = loadController();
    const classes = api.parseClassSelectors(
      "/* .ignored-in-comment */\n" +
        ".hero, p.lead { color: red; }\n" +
        "a:hover { text-decoration: none; }\n" +
        "@media x { .in-media { margin: 0; } }\n" +
        ".hero { font-weight: bold; }",
    );
    expect(classes).toContain("hero");
    expect(classes).toContain("lead");
    expect(classes).toContain("in-media");
    expect(classes).not.toContain("ignored-in-comment");
    expect(classes.filter((c) => c === "hero")).toHaveLength(1);
  });
});

// ---------------------------------------------------------------------------
// Lifecycle against the stubbed vendor
// ---------------------------------------------------------------------------

describe("PercWidgetEditor lifecycle", () => {
  it("addEditor builds toolbar + editor, hides textarea, focuses on auto_focus", () => {
    const api = loadController();
    window.__PercTipTap = fakeVendor();
    window.__percPendingEditorSettings = {
      toolbar: ["bold italic underline undo redo styleselect "],
      auto_focus: "elm1",
      content_css: "",
    };
    const ta = addTextarea("elm1", "<p>Text</p>");
    let focused = false;
    const origFocus = FakeEditor.prototype.commands;
    void origFocus;
    expect(api.addEditor("elm1")).toBe(true);
    expect(api.isActive("elm1")).toBe(true);
    expect(ta.classList.contains("pwe-hidden")).toBe(true);
    const bar = document.querySelector(".pwe-toolbar");
    expect(bar).not.toBeNull();
    expect(
      [...bar.querySelectorAll("button")].map((b) =>
        b.getAttribute("data-pwe-cmd"),
      ),
    ).toEqual(["bold", "italic", "underline", "undo", "redo"]);
    expect(bar.querySelector("select[data-pwe-styles]")).not.toBeNull();
    expect(document.querySelector(".pwe-editor")).not.toBeNull();
    expect(focused).toBe(false); // focus is best-effort; init must not throw
  });

  it("addEditor is a no-op without the vendor bundle (plain textarea survives)", () => {
    const api = loadController();
    const ta = addTextarea("elm1", "hello");
    expect(api.addEditor("elm1")).toBe(false);
    expect(api.isActive("elm1")).toBe(false);
    expect(document.querySelector(".pwe-toolbar")).toBeNull();
    expect(ta.classList.contains("pwe-hidden")).toBe(false);
  });

  it("getContent/setContent round-trip and removeEditor syncs the textarea", () => {
    const api = loadController();
    window.__PercTipTap = fakeVendor();
    const ta = addTextarea("elm1", "<p>Text</p>");
    api.addEditor("elm1");
    const ed = api.get("elm1");
    expect(ed).not.toBeNull();
    expect(ed.getContent()).toBe("<p>Text</p>");
    ed.setContent("<p>Changed</p>");
    expect(ed.getContent()).toBe("<p>Changed</p>");
    expect(api.removeEditor("elm1")).toBe(true);
    expect(api.isActive("elm1")).toBe(false);
    expect(ta.value).toBe("<p>Changed</p>");
    expect(ta.classList.contains("pwe-hidden")).toBe(false);
    expect(api.get("elm1")).toBeNull();
    expect(api.removeEditor("elm1")).toBe(false);
  });

  it("edits mark the textarea dirty and fire input/change", () => {
    const api = loadController();
    window.__PercTipTap = fakeVendor();
    const ta = addTextarea("elm1", "<p>Text</p>");
    const events = [];
    ta.addEventListener("input", () => events.push("input"));
    ta.addEventListener("change", () => events.push("change"));
    api.addEditor("elm1");
    const inst = FakeEditor.instances[FakeEditor.instances.length - 1];
    inst._html = "<p>Typed</p>";
    inst._onUpdate();
    expect(ta.value).toBe("<p>Typed</p>");
    expect(ta.getAttribute("data-perc-dirty")).toBe("true");
    expect(events).toEqual(["input", "change"]);
  });

  it("styleselect applies block format, site class, and clear", () => {
    const api = loadController();
    window.__PercTipTap = fakeVendor();
    window.__percPendingEditorSettings = {
      toolbar: ["styleselect"],
      content_css: "",
    };
    addTextarea("elm1", "<p>Text</p>");
    api.addEditor("elm1");
    const select = document.querySelector("select[data-pwe-styles]");
    const values = [...select.querySelectorAll("option")].map((o) => o.value);
    expect(values).toEqual([
      "",
      "block:p",
      "block:h1",
      "block:h2",
      "block:h3",
      "clear",
    ]);
    const inst = FakeEditor.instances[FakeEditor.instances.length - 1];
    select.value = "block:h2";
    select.dispatchEvent(new window.Event("change", { bubbles: true }));
    expect(inst._calls).toContainEqual(["toggleHeading", { level: 2 }]);
    // Simulate site classes asynchronously loaded into the dropdown.
    const hero = document.createElement("option");
    hero.value = "site:hero";
    hero.textContent = "hero";
    select.appendChild(hero);
    select.value = "site:hero";
    select.dispatchEvent(new window.Event("change", { bubbles: true }));
    expect(inst._calls).toContainEqual(["setPercSiteClass", "hero"]);
  });

  it("site classes from content_css populate the styles dropdown", async () => {
    const api = loadController();
    window.__PercTipTap = fakeVendor();
    window.fetch = async () => ({
      ok: true,
      text: async () => ".hero{color:red} p.lead{margin:0}",
    });
    window.__percPendingEditorSettings = {
      toolbar: ["styleselect"],
      content_css: "/sites/s1/css/main.css",
    };
    addTextarea("elm1", "x");
    api.addEditor("elm1");
    await new Promise((r) => setTimeout(r, 20));
    const opts = [
      ...document.querySelectorAll("select[data-pwe-styles] option"),
    ].map((o) => o.value);
    expect(opts).toContain("site:hero");
    expect(opts).toContain("site:lead");
    expect(
      document.querySelector('link[data-perc-widget-editor-css]'),
    ).not.toBeNull();
  });

  it("setup callback receives a facade and click wiring survives throws", () => {
    const api = loadController();
    window.__PercTipTap = fakeVendor();
    let seen = null;
    window.__percPendingEditorSettings = {
      toolbar: ["bold"],
      setup(editor) {
        seen = editor;
        editor.on("click", () => {
          throw new Error("site handler boom");
        });
      },
    };
    addTextarea("elm1", "x");
    expect(() => api.addEditor("elm1")).not.toThrow();
    expect(seen).not.toBeNull();
    expect(seen.getContent()).toBe("x");
    const mount = document.querySelector(".pwe-editor");
    expect(() =>
      mount.dispatchEvent(new window.Event("click", { bubbles: true })),
    ).not.toThrow();
  });
});

// ---------------------------------------------------------------------------
// tinymce.EditorManager facade (customized-site-config compatibility)
// ---------------------------------------------------------------------------

describe("tinymce.EditorManager facade", () => {
  it("adopts settings assigned by a TinymceConfig-style file", () => {
    const api = loadController();
    window.__PercTipTap = fakeVendor();
    window.tinymce.EditorManager.settings = {
      toolbar: ["bold italic underline undo redo styleselect "],
      content_css: "",
      auto_focus: "elm1",
      setup(editor) {
        editor.on("click", () => {});
      },
    };
    addTextarea("elm1", "<p>Hi</p>");
    expect(window.tinymce.EditorManager.execCommand("mceAddEditor", true, "elm1")).toBe(
      true,
    );
    expect(window.tinymce.EditorManager.get("elm1").getContent()).toBe(
      "<p>Hi</p>",
    );
    expect(
      window.tinymce.EditorManager.execCommand("mceRemoveEditor", true, "elm1"),
    ).toBe(true);
    expect(window.tinymce.EditorManager.get("elm1")).toBeNull();
    expect(api.isActive("elm1")).toBe(false);
  });

  it("leaves a real TinyMCE EditorManager untouched", () => {
    delete window.PercWidgetEditor;
    delete window.__percPendingEditorSettings;
    delete window.__percDefaultEditorSettings;
    const real = {
      execCommand: () => "real",
      get: () => "real",
      settings: { toolbar: "real" },
    };
    window.tinymce = { EditorManager: real };
    const src = readFileSync(
      resolve(WIDGET_EDITOR_SRC, "PercWidgetEditor.js"),
      "utf8",
    );
    window.eval(src);
    expect(window.tinymce.EditorManager).toBe(real);
    expect(window.tinymce.EditorManager.execCommand()).toBe("real");
    expect(window.PercWidgetEditor.engine).toBe("tiptap");
  });
});

// ---------------------------------------------------------------------------
// Committed bundle shape (real minified TipTap, no React)
// ---------------------------------------------------------------------------

describe("committed vendor bundle", () => {
  it("exposes __PercTipTap with Editor/Mark/StarterKit/Underline", () => {
    const src = readFileSync(resolve(FORM_JS, "perc-tiptap-editor.bundle.min.js"), "utf8");
    expect(src).toMatch(/^\/\*! perc-tiptap-editor\.bundle\.min\.js/);
    window.eval(src);
    const T = window.__PercTipTap;
    expect(typeof T.Editor).toBe("function");
    expect(typeof T.Mark).toBe("function");
    expect(T.version).toBe("3.33.0");
    expect(T.engine).toBe("tiptap");
    expect(T.StarterKit).toBeTruthy();
    expect(T.Underline).toBeTruthy();
    delete window.__PercTipTap;
  });

  it("ships byte-identical copies in both widget packages", () => {
    for (const f of ["perc-tiptap-editor.bundle.min.js", "PercWidgetEditor.js"]) {
      const a = readFileSync(resolve(FORM_JS, f), "utf8");
      const b = readFileSync(resolve(COMMENTS_JS, f), "utf8");
      expect(b).toBe(a);
    }
    const src = readFileSync(resolve(WIDGET_EDITOR_SRC, "PercWidgetEditor.js"), "utf8");
    expect(readFileSync(resolve(FORM_JS, "PercWidgetEditor.js"), "utf8")).toBe(src);
  });
});

// ---------------------------------------------------------------------------
// Packaging cutover: zero /sys_resources/tinymce in both widget packages
// ---------------------------------------------------------------------------

describe("widget package cutover (no CMS-served TinyMCE)", () => {
  const xsl = {
    form: `${PACKAGES}/perc.widget.form/SupportFile-rx_resources/stylesheets/controls/formWidgetControl.xsl`,
    comments: `${PACKAGES}/perc.widget.commentForm/SupportFile-rx_resources/stylesheets/controls/commentsFormWidgetControl.xsl`,
  };

  it.each(["form", "comments"])(
    "%s control loads the TipTap bundle, not sys_resources/tinymce",
    (which) => {
      const src = readFileSync(xsl[which], "utf8");
      expect(src).not.toMatch(/sys_resources\/tinymce/);
      expect(src).toMatch(/perc-tiptap-editor\.bundle\.min\.js/);
      expect(src).toMatch(/PercWidgetEditor\.js/);
      expect(src).toMatch(/TinymceConfig\.js/);
    },
  );

  it.each(["form", "comments"])(
    "%s call sites use PercWidgetEditor (no tinymce globals)",
    (which) => {
      const dir = which === "form" ? FORM_JS : COMMENTS_JS;
      const view = which === "form" ? "PercFormView.js" : "PercCommentsFormView.js";
      const ctrl =
        which === "form" ? "PercFormController.js" : "PercCommentsFormController.js";
      for (const f of [view, ctrl]) {
        const src = readFileSync(resolve(dir, f), "utf8");
        expect(src).not.toMatch(/tinymce\.EditorManager/);
        expect(src).not.toMatch(/mceAddEditor|mceRemoveEditor/);
        expect(src).toMatch(/PercWidgetEditor/);
      }
    },
  );

  it("archive snapshots reference the bundle and drop the System tinymce deps", () => {
    for (const pkg of ["perc.widget.form", "perc.widget.commentForm"]) {
      const info = readFileSync(`${PACKAGES}/${pkg}/psx_archiveInfo.xml`, "utf8");
      const manifest = readFileSync(
        `${PACKAGES}/${pkg}/psx_archiveManifest.xml`,
        "utf8",
      );
      expect(info).not.toMatch(/sys_resources\/tinymce/);
      expect(manifest).not.toMatch(/sys_resources\/tinymce/);
      expect(info).toMatch(/perc-tiptap-editor\.bundle\.min\.js/);
      expect(info).toMatch(/PercWidgetEditor\.js/);
      expect(manifest).toMatch(/perc-tiptap-editor\.bundle\.min\.js/);
    }
  });

  it("TinymceConfig keeps the settings contract with a shippable content_css hook", () => {
    for (const dir of [FORM_JS, COMMENTS_JS]) {
      const src = readFileSync(resolve(dir, "TinymceConfig.js"), "utf8");
      expect(src).toMatch(/tinymce\.EditorManager\.settings\s*=/);
      expect(src).toMatch(/bold italic underline undo redo styleselect/);
      expect(src).toMatch(/content_css/);
      expect(src).not.toMatch(/sys_resources\/tinymce/);
    }
  });
});
