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
 * PercWidgetEditor — vanilla (no-React) rich-text controller for the
 * delivered-site form widgets (`perc.widget.form`, `perc.widget.commentForm`).
 *
 * Replaces the frozen TinyMCE 6.8.6 client (`tinymce.EditorManager`) with a
 * TipTap-backed inline editor (see `perc-tiptap-editor.bundle.min.js`,
 * built from MIT-licensed npm `@tiptap/*`, translator input below).
 *
 * Shipped contract (issue #5901), preserved from `TinymceConfig.js`:
 * toolbar subset (bold / italic / underline / undo / redo / styleselect),
 * `styleselect` populated from the site CSS (`PercCustomStylesService`
 * semantics: class selectors harvested from the configured stylesheet),
 * `content_css` site hook, dirty tracking, `auto_focus`, branding-off
 * (TipTap ships no branding). Developer customizations of `TinymceConfig.js`
 * keep working: the translator reads the same
 * `tinymce.EditorManager.settings` shape the old file assigns.
 *
 * Backwards compatibility: when `window.tinymce` is absent this file
 * installs a minimal `tinymce.EditorManager` facade (`settings`,
 * `execCommand("mceAddEditor"|"mceRemoveEditor")`, `get(id)`) backed by the
 * TipTap engine, so previously customized site configs and any remaining
 * `tinymce.*` call sites keep functioning. New code should call
 * `window.PercWidgetEditor` directly.
 *
 * Plain script, no module system, no jQuery dependency. Safe to load in any
 * order relative to `TinymceConfig.js`: settings assigned before this file
 * loads are picked up from `window.__percPendingEditorSettings`.
 */
(function (global) {
  "use strict";

  var ENGINE_VERSION = "3.33.0";
  var STYLE_CLASSES_CAP = 100;
  var siteClassCache = {};
  var activeEditors = {};
  var baseCssInjected = false;

  function isBrowser() {
    return (
      typeof global.window !== "undefined" &&
      typeof global.window.document !== "undefined"
    );
  }

  function doc() {
    return global.window.document;
  }

  /**
   * Translate a `tinymce.EditorManager.settings`-shaped config into the
   * widget-editor contract. Pure: no DOM access, safe to unit test.
   *
   * Recognized keys: toolbar (string or string[]), content_css,
   * auto_focus, branding, menubar, setup. Unknown toolbar tokens and keys
   * are ignored so newer/future customizations never break init.
   */
  function translateConfig(settings) {
    var input = settings && typeof settings === "object" ? settings : {};
    var rawToolbar = input.toolbar;
    var tokens = [];
    if (typeof rawToolbar === "string") {
      tokens = rawToolbar.split(/[\s|,]+/);
    } else if (Object.prototype.toString.call(rawToolbar) === "[object Array]") {
      for (var i = 0; i < rawToolbar.length; i++) {
        var part = String(rawToolbar[i]).split(/[\s|,]+/);
        for (var j = 0; j < part.length; j++) {
          tokens.push(part[j]);
        }
      }
    }
    var enabled = {};
    for (var k = 0; k < tokens.length; k++) {
      var token = tokens[k].toLowerCase();
      if (
        token === "bold" ||
        token === "italic" ||
        token === "underline" ||
        token === "undo" ||
        token === "redo" ||
        token === "styleselect"
      ) {
        enabled[token] = true;
      }
    }
    // Empty/missing toolbar keeps the shipped default subset.
    if (tokens.length === 0) {
      enabled = {
        bold: true,
        italic: true,
        underline: true,
        undo: true,
        redo: true,
        styleselect: true,
      };
    }
    return {
      buttons: enabled,
      contentCss:
        typeof input.content_css === "string" ? input.content_css : "",
      autoFocus: typeof input.auto_focus === "string" ? input.auto_focus : "",
      // branding:false is the only shipped value; the engine has no branding.
      branding: input.branding === true,
      setup: typeof input.setup === "function" ? input.setup : null,
    };
  }

  function readSettings() {
    var fromFacade =
      global.tinymce &&
      global.tinymce.EditorManager &&
      global.tinymce.EditorManager.settings;
    var pending = global.__percPendingEditorSettings;
    return translateConfig(fromFacade || pending || {});
  }

  /**
   * Allow only same-origin-safe stylesheet references. Rejects javascript:
   * and data: URLs (CodeQL js/xss-through-dom: never sink config strings
   * into markup; the href is assigned as a property, never innerHTML).
   */
  function sanitizeCssUrl(url) {
    if (typeof url !== "string") return "";
    var trimmed = url.replace(/^\s+|\s+$/g, "");
    if (trimmed === "") return "";
    if (/^javascript:/i.test(trimmed) || /^data:/i.test(trimmed)) return "";
    return trimmed;
  }

  function ensureContentCssLink(href) {
    var clean = sanitizeCssUrl(href);
    if (clean === "" || !isBrowser()) return;
    var d = doc();
    var selector =
      'link[data-perc-widget-editor-css="' + clean.replace(/"/g, "") + '"]';
    if (d.head.querySelector(selector)) return;
    var link = d.createElement("link");
    link.setAttribute("rel", "stylesheet");
    link.setAttribute("type", "text/css");
    link.setAttribute("data-perc-widget-editor-css", clean.replace(/"/g, ""));
    link.href = clean;
    d.head.appendChild(link);
  }

  function ensureBaseCss() {
    if (baseCssInjected || !isBrowser()) return;
    baseCssInjected = true;
    var d = doc();
    var style = d.createElement("style");
    style.setAttribute("data-perc-widget-editor", "base");
    style.textContent =
      ".pwe-toolbar{display:flex;flex-wrap:wrap;gap:2px;align-items:center;" +
      "padding:3px;border:1px solid #ccc;border-bottom:0;background:#f5f5f5;}" +
      ".pwe-toolbar button{font-size:12px;line-height:1.4;min-width:26px;" +
      "padding:2px 6px;border:1px solid transparent;background:transparent;" +
      "cursor:pointer;color:#333;}" +
      ".pwe-toolbar button:hover{border-color:#999;background:#e6e6e6;}" +
      ".pwe-toolbar button[aria-pressed='true']{background:#d9d9d9;" +
      "border-color:#888;}" +
      ".pwe-toolbar button:disabled{opacity:.4;cursor:default;}" +
      ".pwe-toolbar select{font-size:12px;max-width:220px;margin-left:4px;}" +
      ".pwe-editor{border:1px solid #ccc;min-height:120px;padding:6px;" +
      "background:#fff;}" +
      ".pwe-editor:focus{outline:1px solid #3288b0;}" +
      ".pwe-editor p{margin:0 0 6px;}" +
      ".pwe-hidden{display:none !important;}";
    d.head.appendChild(style);
  }

  /**
   * Harvest class selectors from a site stylesheet (PercCustomStylesService
   * semantics: styleselect options derived from site CSS). Fetches as text
   * and matches selectors with regex — never touches foreign CSSOM, so
   * cross-origin stylesheets fail closed to the built-in formats.
   */
  function loadSiteClasses(contentCss, onDone) {
    var clean = sanitizeCssUrl(contentCss);
    if (clean === "") {
      onDone([]);
      return;
    }
    if (Object.prototype.hasOwnProperty.call(siteClassCache, clean)) {
      onDone(siteClassCache[clean]);
      return;
    }
    var finish = function (classes) {
      siteClassCache[clean] = classes;
      onDone(classes);
    };
    var fetchFn = global.window && global.window.fetch;
    if (typeof fetchFn !== "function") {
      finish([]);
      return;
    }
    try {
      fetchFn(clean, { credentials: "same-origin" })
        .then(function (resp) {
          if (!resp || !resp.ok) throw new Error("css fetch failed");
          return resp.text();
        })
        .then(function (text) {
          finish(parseClassSelectors(String(text || "")));
        })
        .catch(function () {
          finish([]);
        });
    } catch (e) {
      finish([]);
    }
  }

  /**
   * Extract candidate class names from raw CSS text. Pure function.
   * Matches `.name` in selector position, skips pseudo-classes,
   * numeric-prefixed tokens and file-extension lookalikes.
   */
  function parseClassSelectors(cssText) {
    var found = [];
    var seen = {};
    // Strip comments and match selector lists. The opening brace is a
    // lookahead (not consumed) so nested rules (@media) still match.
    var stripped = String(cssText).replace(/\/\*[\s\S]*?\*\//g, " ");
    var re = /(^|[{};,])\s*([^{}]+?)(?=\{)/g;
    var m;
    while ((m = re.exec(stripped)) !== null) {
      var selectorList = m[2].split(",");
      for (var i = 0; i < selectorList.length; i++) {
        var sel = selectorList[i].replace(/::?[a-zA-Z-]+(\([^)]*\))?/g, " ");
        var cm = /\.([a-zA-Z_][a-zA-Z0-9_-]*)/g;
        var c;
        while ((c = cm.exec(sel)) !== null) {
          var name = c[1];
          if (!seen[name]) {
            seen[name] = true;
            found.push(name);
            if (found.length >= STYLE_CLASSES_CAP) return found;
          }
        }
      }
    }
    return found;
  }

  function vendor() {
    return global.__PercTipTap || null;
  }

  /**
   * Probe a command without running it (no focus steal, headless-safe).
   * Mirrors the WebUI TipTapToolbar canRun idiom.
   */
  function canCommand(editor, name, arg) {
    if (!editor) return false;
    try {
      var chain = editor.can().chain().focus();
      if (typeof chain[name] !== "function") return false;
      var probe = arg === undefined ? chain[name]() : chain[name](arg);
      return !!probe.run();
    } catch (e) {
      return false;
    }
  }

  function buildSiteClassMark(Mark) {
    return Mark.create({
      name: "percSiteClass",
      attrs: {
        className: {
          default: null,
          parseHTML: function (el) {
            return el.getAttribute("class");
          },
        },
      },
      parseHTML: function () {
        return [{ tag: "span[data-perc-site-class]" }];
      },
      renderHTML: function (props) {
        var attrs = { "data-perc-site-class": "1" };
        var cls =
          props && props.HTMLAttributes && props.HTMLAttributes.className;
        if (cls) attrs["class"] = cls;
        return ["span", attrs, 0];
      },
      addCommands: function () {
        return {
          setPercSiteClass: function (className) {
            return function (ctx) {
              return ctx.commands.setMark("percSiteClass", {
                className: className,
              });
            };
          },
          unsetPercSiteClass: function () {
            return function (ctx) {
              return ctx.commands.unsetMark("percSiteClass");
            };
          },
        };
      },
    });
  }

  function createEditorInstance(textarea, translated) {
    var T = vendor();
    if (!T || !T.Editor) return null;
    var extensions = [T.StarterKit.configure({ heading: { levels: [1, 2, 3] } })];
    if (T.Underline) extensions.push(T.Underline);
    var siteClassMark = null;
    if (T.Mark && typeof T.Mark.create === "function") {
      try {
        siteClassMark = buildSiteClassMark(T.Mark);
        extensions.push(siteClassMark);
      } catch (e) {
        siteClassMark = null;
      }
    }
    var mount = doc().createElement("div");
    mount.className = "pwe-editor";
    var editor = new T.Editor({
      element: mount,
      extensions: extensions,
      content: textarea.value || "",
      onUpdate: function () {
        syncToTextarea(textarea, editor);
      },
    });
    return { editor: editor, mount: mount, hasSiteMark: !!siteClassMark };
  }

  function syncToTextarea(textarea, editor) {
    var html = "";
    try {
      html = editor.getHTML();
    } catch (e) {
      return;
    }
    if (textarea.value !== html) {
      textarea.value = html;
      textarea.setAttribute("data-perc-dirty", "true");
      var d = doc();
      var evt;
      try {
        evt = new global.window.Event("input", { bubbles: true });
      } catch (e2) {
        evt = d.createEvent("Event");
        evt.initEvent("input", true, true);
      }
      textarea.dispatchEvent(evt);
      var changeEvt;
      try {
        changeEvt = new global.window.Event("change", { bubbles: true });
      } catch (e3) {
        changeEvt = d.createEvent("Event");
        changeEvt.initEvent("change", true, true);
      }
      textarea.dispatchEvent(changeEvt);
    }
  }

  function blockLabel(value) {
    if (value === "p") return "Paragraph";
    if (value === "h1") return "Heading 1";
    if (value === "h2") return "Heading 2";
    if (value === "h3") return "Heading 3";
    return value;
  }

  function buildToolbar(textarea, editor, translated, record) {
    var d = doc();
    var bar = d.createElement("div");
    bar.className = "pwe-toolbar";
    bar.setAttribute("role", "toolbar");
    bar.setAttribute("aria-label", "Rich text");

    var buttons = [];
    if (translated.buttons.bold) buttons.push({ cmd: "bold", label: "B", bold: true, title: "Bold" });
    if (translated.buttons.italic)
      buttons.push({ cmd: "italic", label: "I", italic: true, title: "Italic" });
    if (translated.buttons.underline)
      buttons.push({ cmd: "underline", label: "U", underline: true, title: "Underline" });
    var supportsHistory =
      canCommand(editor, "undo") && canCommand(editor, "redo");
    if (translated.buttons.undo)
      buttons.push({ cmd: "undo", label: "\u21BA", title: "Undo", needsHistory: true });
    if (translated.buttons.redo)
      buttons.push({ cmd: "redo", label: "\u21BB", title: "Redo", needsHistory: true });

    buttons.forEach(function (spec) {
      var btn = d.createElement("button");
      btn.setAttribute("type", "button");
      btn.setAttribute("title", spec.title);
      btn.setAttribute("data-pwe-cmd", spec.cmd);
      btn.setAttribute("aria-pressed", "false");
      var inner = d.createElement("span");
      inner.textContent = spec.label;
      if (spec.bold) inner.style.fontWeight = "bold";
      if (spec.italic) inner.style.fontStyle = "italic";
      if (spec.underline) inner.style.textDecoration = "underline";
      btn.appendChild(inner);
      if (spec.needsHistory && !supportsHistory) btn.disabled = true;
      btn.addEventListener("click", function (ev) {
        ev.preventDefault();
        runCommand(editor, spec.cmd);
      });
      bar.appendChild(btn);
    });

    if (translated.buttons.styleselect) {
      var select = d.createElement("select");
      select.setAttribute("data-pwe-styles", "1");
      select.setAttribute("aria-label", "Styles");
      var placeholder = d.createElement("option");
      placeholder.value = "";
      placeholder.textContent = "Styles";
      select.appendChild(placeholder);
      ["p", "h1", "h2", "h3"].forEach(function (tag) {
        var opt = d.createElement("option");
        opt.value = "block:" + tag;
        opt.textContent = blockLabel(tag);
        select.appendChild(opt);
      });
      var clear = d.createElement("option");
      clear.value = "clear";
      clear.textContent = "Clear formatting";
      select.appendChild(clear);
      select.addEventListener("change", function () {
        applyStyleValue(editor, select.value, record);
        select.value = "";
      });
      bar.appendChild(select);
      record.styleSelect = select;
      loadSiteClasses(translated.contentCss, function (classes) {
        populateSiteClasses(select, classes);
      });
    }

    var refreshState = function () {
      var btns = bar.querySelectorAll("button[data-pwe-cmd]");
      for (var i = 0; i < btns.length; i++) {
        var cmd = btns[i].getAttribute("data-pwe-cmd");
        var on = false;
        try {
          if (cmd === "bold" || cmd === "italic" || cmd === "underline") {
            on = editor.isActive(cmd);
          }
        } catch (e) {
          on = false;
        }
        btns[i].setAttribute("aria-pressed", on ? "true" : "false");
      }
    };
    try {
      editor.on("selectionUpdate", refreshState);
      editor.on("transaction", refreshState);
    } catch (e) {
      /* older engines: state highlight is best-effort */
    }
    return bar;
  }

  function populateSiteClasses(select, classes) {
    if (!select || !select.ownerDocument) return;
    if (select.getAttribute("data-pwe-site-loaded") === "1") return;
    select.setAttribute("data-pwe-site-loaded", "1");
    if (!classes || classes.length === 0) return;
    var d = select.ownerDocument;
    var group = d.createElement("optgroup");
    group.setAttribute("label", "Site styles");
    classes.forEach(function (name) {
      var opt = d.createElement("option");
      opt.value = "site:" + name;
      opt.textContent = name;
      group.appendChild(opt);
    });
    select.appendChild(group);
  }

  function runCommand(editor, cmd) {
    // Mirrors the WebUI TipTapToolbar runCommand idiom; every failure is
    // swallowed so toolbar actions never break the host form.
    try {
      var chain = editor.chain().focus();
      if (cmd === "bold") chain.toggleBold().run();
      else if (cmd === "italic") chain.toggleItalic().run();
      else if (cmd === "underline") chain.toggleUnderline().run();
      else if (cmd === "undo") chain.undo().run();
      else if (cmd === "redo") chain.redo().run();
    } catch (e) {
      /* toolbar actions never break the host form */
    }
  }

  function applyBlockTag(editor, tag) {
    try {
      if (tag === "p") editor.chain().focus().setParagraph().run();
      else if (tag === "h1")
        editor.chain().focus().toggleHeading({ level: 1 }).run();
      else if (tag === "h2")
        editor.chain().focus().toggleHeading({ level: 2 }).run();
      else if (tag === "h3")
        editor.chain().focus().toggleHeading({ level: 3 }).run();
    } catch (e) {
      /* style application never breaks the host form */
    }
  }

  function clearFormatting(editor) {
    try {
      editor.chain().focus().clearNodes().unsetAllMarks().run();
    } catch (e) {
      /* style application never breaks the host form */
    }
  }

  function selectedText(editor) {
    try {
      var range = editor.state.selection;
      return editor.state.doc.textBetween(range.from, range.to, " ");
    } catch (e) {
      return "";
    }
  }

  function insertSiteClassSpan(editor, cls) {
    // Engine without the site-class mark: wrap selection in a classed
    // span via DOM-built markup to preserve author intent.
    try {
      var ownerDoc =
        (editor.options && editor.options.element.ownerDocument) || doc();
      var tmp = ownerDoc.createElement("span");
      tmp.setAttribute("class", cls);
      tmp.setAttribute("data-perc-site-class", "1");
      tmp.textContent = selectedText(editor);
      editor.chain().focus().insertContent(tmp.outerHTML).run();
    } catch (e) {
      /* style application never breaks the host form */
    }
  }

  function applySiteClass(editor, cls) {
    var applied = false;
    if (canCommand(editor, "setPercSiteClass", cls)) {
      try {
        editor.chain().focus().setPercSiteClass(cls).run();
        applied = true;
      } catch (e) {
        applied = false;
      }
    }
    if (!applied) insertSiteClassSpan(editor, cls);
  }

  function applyStyleValue(editor, value, record) {
    if (!value) return;
    if (value === "clear") {
      clearFormatting(editor);
    } else if (value.indexOf("block:") === 0) {
      applyBlockTag(editor, value.substring(6));
    } else if (value.indexOf("site:") === 0) {
      var cls = String(value.substring(5)).replace(/[^a-zA-Z0-9_-]/g, "");
      if (cls !== "") applySiteClass(editor, cls);
    }
    if (record && record.textarea && record.editor) {
      syncToTextarea(record.textarea, record.editor);
    }
  }

  function setupFacade(editor, textarea, translated) {
    if (!translated.setup) return;
    var facade = {
      id: textarea.id || textarea.name || "",
      on: function (eventName, fn) {
        if (typeof fn !== "function") return;
        var target = editor.options.element;
        if (eventName === "click" && target) {
          target.addEventListener("click", function (ev) {
            try {
              fn(ev);
            } catch (e) {
              /* site setup handlers must not break the editor */
            }
          });
        } else if ((eventName === "change" || eventName === "input") && target) {
          var handler = function () {
            try {
              fn({ type: eventName });
            } catch (e) {
              /* site setup handlers must not break the editor */
            }
          };
          try {
            editor.on("update", handler);
          } catch (e2) {
            target.addEventListener(eventName, handler);
          }
        }
      },
      getContent: function () {
        try {
          return editor.getHTML();
        } catch (e) {
          return textarea.value || "";
        }
      },
      setContent: function (html) {
        try {
          editor.commands.setContent(html || "");
        } catch (e) {
          textarea.value = html || "";
        }
      },
    };
    try {
      translated.setup(facade);
    } catch (e) {
      /* a throwing site setup must not break editor init */
    }
  }

  function addEditor(textareaId) {
    if (!isBrowser()) return false;
    if (!textareaId || activeEditors[textareaId]) return !!activeEditors[textareaId];
    var d = doc();
    var textarea = d.getElementById(textareaId);
    if (!textarea || textarea.tagName.toLowerCase() !== "textarea") return false;
    if (!vendor()) {
      if (typeof global.window.console !== "undefined") {
        global.window.console.warn(
          "PercWidgetEditor: TipTap vendor bundle (__PercTipTap) not loaded; leaving plain textarea.",
        );
      }
      return false;
    }
    ensureBaseCss();
    var translated = readSettings();
    ensureContentCssLink(translated.contentCss);
    var created = createEditorInstance(textarea, translated);
    if (!created) return false;
    var record = {
      textarea: textarea,
      editor: created.editor,
      mount: created.mount,
      toolbar: null,
      styleSelect: null,
      previousDisplay: textarea.style.display || "",
    };
    record.toolbar = buildToolbar(textarea, created.editor, translated, record);
    textarea.parentNode.insertBefore(record.toolbar, textarea.nextSibling);
    textarea.parentNode.insertBefore(created.mount, record.toolbar.nextSibling);
    textarea.classList.add("pwe-hidden");
    activeEditors[textareaId] = record;
    setupFacade(created.editor, textarea, translated);
    if (
      translated.autoFocus &&
      (translated.autoFocus === textareaId || translated.autoFocus === textarea.id)
    ) {
      try {
        created.editor.commands.focus();
      } catch (e) {
        /* focus is best-effort */
      }
    }
    syncToTextarea(textarea, created.editor);
    return true;
  }

  function removeEditor(textareaId) {
    var record = activeEditors[textareaId];
    if (!record) return false;
    try {
      syncToTextarea(record.textarea, record.editor);
    } catch (e) {
      /* keep tearing down */
    }
    try {
      record.editor.destroy();
    } catch (e) {
      /* keep tearing down */
    }
    try {
      if (record.toolbar && record.toolbar.parentNode)
        record.toolbar.parentNode.removeChild(record.toolbar);
      if (record.mount && record.mount.parentNode)
        record.mount.parentNode.removeChild(record.mount);
    } catch (e) {
      /* keep tearing down */
    }
    try {
      record.textarea.classList.remove("pwe-hidden");
      record.textarea.style.display = record.previousDisplay;
    } catch (e) {
      /* keep tearing down */
    }
    delete activeEditors[textareaId];
    return true;
  }

  function getEditor(textareaId) {
    var record = activeEditors[textareaId];
    if (!record) return null;
    return {
      id: textareaId,
      getContent: function () {
        try {
          return record.editor.getHTML();
        } catch (e) {
          return record.textarea.value || "";
        }
      },
      setContent: function (html) {
        try {
          record.editor.commands.setContent(html || "");
          syncToTextarea(record.textarea, record.editor);
        } catch (e) {
          record.textarea.value = html || "";
        }
      },
    };
  }

  function installTinymceFacade() {
    var existing =
      global.tinymce && global.tinymce.EditorManager;
    // A real TinyMCE (full EditorManager API) always wins over the facade.
    if (
      existing &&
      !existing.__percFacade &&
      typeof existing.execCommand === "function" &&
      typeof existing.get === "function"
    ) {
      return;
    }
    // Preserve settings assigned before this file loaded (defensive site
    // configs that bootstrap `tinymce.EditorManager = {}` themselves).
    var priorSettings =
      existing && existing.settings && typeof existing.settings === "object"
        ? existing.settings
        : null;
    if (priorSettings && !global.__percPendingEditorSettings) {
      global.__percPendingEditorSettings = priorSettings;
    }
    var facadeManager = {
      __percFacade: true,
      get settings() {
        return (
          global.__percPendingEditorSettings ||
          global.__percDefaultEditorSettings ||
          {}
        );
      },
      set settings(value) {
        global.__percPendingEditorSettings =
          value && typeof value === "object" ? value : {};
      },
      execCommand: function (cmd, ui, id) {
        if (cmd === "mceAddEditor") return addEditor(id);
        if (cmd === "mceRemoveEditor") return removeEditor(id);
        return false;
      },
      get: function (id) {
        return getEditor(id);
      },
    };
    global.__percDefaultEditorSettings = {};
    if (!global.tinymce) global.tinymce = {};
    global.tinymce.EditorManager = facadeManager;
  }

  var api = {
    engine: "tiptap",
    engineVersion: ENGINE_VERSION,
    translateConfig: translateConfig,
    parseClassSelectors: parseClassSelectors,
    sanitizeCssUrl: sanitizeCssUrl,
    addEditor: addEditor,
    removeEditor: removeEditor,
    get: getEditor,
    isActive: function (textareaId) {
      return !!activeEditors[textareaId];
    },
  };

  global.PercWidgetEditor = api;
  try {
    installTinymceFacade();
  } catch (e) {
    /* facade is best-effort; the direct API always works */
  }
})(typeof window !== "undefined" ? window : typeof globalThis !== "undefined" ? globalThis : this);
