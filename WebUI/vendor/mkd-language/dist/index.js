// src/submit/http.ts
var HttpSubmissionClient = class {
  constructor(options) {
    const url = options.postUrl?.trim();
    if (!url) {
      throw new Error("@mkd/language: HttpSubmissionClient requires postUrl");
    }
    this.postUrl = url;
    this.headers = options.headers;
    this.credentials = options.credentials ?? "same-origin";
    this.fetchImpl = options.fetchImpl ?? (typeof fetch !== "undefined" ? fetch.bind(globalThis) : (() => {
      throw new Error("@mkd/language: fetch is not available");
    }));
  }
  async submit(payload) {
    const headers = new Headers(
      typeof this.headers === "function" ? this.headers() : this.headers
    );
    if (!headers.has("Content-Type")) {
      headers.set("Content-Type", "application/json");
    }
    if (!headers.has("Accept")) {
      headers.set("Accept", "application/json");
    }
    const res = await this.fetchImpl(this.postUrl, {
      method: "POST",
      headers,
      credentials: this.credentials,
      body: JSON.stringify(payload)
    });
    if (!res.ok) {
      let detail = "";
      try {
        detail = (await res.text()).slice(0, 200);
      } catch {
      }
      const suffix = detail ? `: ${detail}` : "";
      throw new Error(
        `@mkd/language: POST ${this.postUrl} failed (${res.status} ${res.statusText})${suffix}`
      );
    }
  }
};

// src/submit/noop.ts
var NoopSubmissionClient = class {
  constructor(debug = false) {
    this.debug = debug;
  }
  async submit(payload) {
    if (this.debug) {
      console.debug("[@mkd/language]", payload);
    }
  }
};

// src/util/dom.ts
var BOUND_ATTR = "data-mkd-lang-bound";
var TRIGGER_ATTR = "data-mkd-lang-trigger";
var IGNORE_ATTR = "data-mkd-lang-ignore";
var IGNORE_CLASS = "mkd-lang-ignore";
var POPOVER_ROOT_ATTR = "data-mkd-lang-popover";
var STYLE_ID = "mkd-lang-styles";
function isTrigger(el) {
  return el.hasAttribute(TRIGGER_ATTR);
}
function isBound(el) {
  return el.getAttribute(BOUND_ATTR) === "1";
}
function markBound(el) {
  el.setAttribute(BOUND_ATTR, "1");
}
function unmarkBound(el) {
  el.removeAttribute(BOUND_ATTR);
}
function findTrigger(el) {
  return el.querySelector(`button[${TRIGGER_ATTR}]`);
}
function shouldIgnore(el, respectIgnore) {
  if (!respectIgnore) return false;
  if (el.closest(`[${IGNORE_ATTR}]`) || el.closest(`.${IGNORE_CLASS}`)) {
    return true;
  }
  if (el.closest(`[${POPOVER_ROOT_ATTR}]`)) return true;
  if (isTrigger(el)) return true;
  return false;
}
function isLibraryUi(el) {
  return isTrigger(el) || el.hasAttribute(POPOVER_ROOT_ATTR) || !!el.closest(`[${POPOVER_ROOT_ATTR}]`);
}
function setText(el, text) {
  el.textContent = text;
}

// src/i18n/catalogs/en.ts
var en = {
  "trigger.ariaLabel": "Suggest a translation correction",
  "dialog.title": "Suggest a better wording",
  "tab.text": "Text",
  "tab.aria": "Aria",
  "field.currentText": "What it says now",
  "field.proposedText": "What it should say",
  "field.messageId": "Message id",
  "field.messageIdMissing": "Not provided",
  "field.locale": "Locale",
  "field.notes": "Notes",
  "field.email": "Your email",
  "field.ariaLabelCurrent": "aria-label (now)",
  "field.ariaLabelProposed": "What aria-label should say",
  "field.ariaLabelledby": "aria-labelledby (now)",
  "field.ariaLabelledbyHelp": "This control is named via aria-labelledby. Correct the referenced elements, or describe the issue in Notes on the Text tab.",
  "field.title": "title attribute (now)",
  "field.ariaEmpty": "No ARIA name attributes found on this element.",
  "action.submit": "Submit",
  "action.cancel": "Cancel",
  "action.submitting": "Submitting\u2026",
  "error.localeRequired": "Locale is required. Configure locale when initializing @mkd/language.",
  "error.noChange": "Change the text or aria-label, or add notes explaining why.",
  "error.proposedRequired": "Enter what the text should say.",
  "error.emailRequired": "Email address is required.",
  "error.emailInvalid": "Enter a valid email address.",
  "error.submitFailed": "Could not submit your correction. Please try again.",
  "status.success": "Thanks \u2014 your correction was submitted.",
  "status.failure": "Submission failed.",
  "footer.privacy": "Privacy",
  "footer.accessibility": "Accessibility",
  "footer.terms": "Terms",
  "footer.copyright": "Copyright (c) 2026 Monkeyking.dev"
};

// src/i18n/catalogs/es.ts
var es = {
  "trigger.ariaLabel": "Sugerir una correcci\xF3n de traducci\xF3n",
  "dialog.title": "Sugerir un mejor texto",
  "tab.text": "Texto",
  "tab.aria": "Aria",
  "field.currentText": "Lo que dice ahora",
  "field.proposedText": "Lo que deber\xEDa decir",
  "field.messageId": "Id de mensaje",
  "field.messageIdMissing": "No proporcionado",
  "field.locale": "Configuraci\xF3n regional",
  "field.notes": "Notas",
  "field.email": "Su correo electr\xF3nico",
  "field.ariaLabelCurrent": "aria-label (ahora)",
  "field.ariaLabelProposed": "Lo que deber\xEDa decir aria-label",
  "field.ariaLabelledby": "aria-labelledby (ahora)",
  "field.ariaLabelledbyHelp": "Este control se nombra con aria-labelledby. Corrija los elementos referenciados, o describa el problema en Notas en la pesta\xF1a Texto.",
  "field.title": "atributo title (ahora)",
  "field.ariaEmpty": "No se encontraron atributos de nombre ARIA en este elemento.",
  "action.submit": "Enviar",
  "action.cancel": "Cancelar",
  "action.submitting": "Enviando\u2026",
  "error.localeRequired": "La configuraci\xF3n regional es obligatoria. Configure locale al inicializar @mkd/language.",
  "error.noChange": "Cambie el texto o aria-label, o a\xF1ada notas que lo expliquen.",
  "error.proposedRequired": "Indique lo que deber\xEDa decir el texto.",
  "error.emailRequired": "El correo electr\xF3nico es obligatorio.",
  "error.emailInvalid": "Introduzca un correo electr\xF3nico v\xE1lido.",
  "error.submitFailed": "No se pudo enviar la correcci\xF3n. Int\xE9ntelo de nuevo.",
  "status.success": "Gracias \u2014 se envi\xF3 su correcci\xF3n.",
  "status.failure": "Error al enviar.",
  "footer.privacy": "Privacidad",
  "footer.accessibility": "Accesibilidad",
  "footer.terms": "T\xE9rminos",
  "footer.copyright": "Copyright (c) 2026 Monkeyking.dev"
};

// src/i18n/resolve.ts
var CATALOGS = {
  en,
  es
};
var RTL_LANGS = /* @__PURE__ */ new Set(["ar", "he", "fa", "ur"]);
function resolveLocaleValue(value) {
  if (value == null) return void 0;
  if (typeof value === "function") {
    const v = value();
    return v?.trim() || void 0;
  }
  return value.trim() || void 0;
}
function resolveUiLocale(uiLocale, contentLocale) {
  const candidates = [
    resolveLocaleValue(uiLocale),
    resolveLocaleValue(contentLocale),
    typeof navigator !== "undefined" ? navigator.language : void 0,
    "en"
  ].filter(Boolean);
  for (const tag of candidates) {
    const normalized = tag.toLowerCase();
    if (CATALOGS[normalized]) return normalized;
    const lang = normalized.split("-")[0];
    if (CATALOGS[lang]) return lang;
  }
  return "en";
}
function isRtlLocale(locale) {
  const lang = locale.toLowerCase().split("-")[0];
  return RTL_LANGS.has(lang);
}
function getCatalog(uiLocale) {
  const normalized = uiLocale.toLowerCase();
  if (CATALOGS[normalized]) return CATALOGS[normalized];
  const lang = normalized.split("-")[0];
  if (CATALOGS[lang]) return CATALOGS[lang];
  return en;
}
function t(key, uiLocale, overrides) {
  if (overrides?.[key]) return overrides[key];
  const catalog = getCatalog(uiLocale);
  return catalog[key] ?? en[key] ?? key;
}
function registerCatalog(locale, catalog) {
  CATALOGS[locale.toLowerCase()] = catalog;
}
function builtInLocales() {
  return ["en", "es"];
}

// src/i18n/types.ts
var UI_MESSAGE_IDS = {
  "trigger.ariaLabel": "mkd.language.ui.trigger.ariaLabel",
  "dialog.title": "mkd.language.ui.dialog.title",
  "tab.text": "mkd.language.ui.tab.text",
  "tab.aria": "mkd.language.ui.tab.aria",
  "field.currentText": "mkd.language.ui.field.currentText",
  "field.proposedText": "mkd.language.ui.field.proposedText",
  "field.messageId": "mkd.language.ui.field.messageId",
  "field.messageIdMissing": "mkd.language.ui.field.messageIdMissing",
  "field.locale": "mkd.language.ui.field.locale",
  "field.notes": "mkd.language.ui.field.notes",
  "field.email": "mkd.language.ui.field.email",
  "field.ariaLabelCurrent": "mkd.language.ui.field.ariaLabelCurrent",
  "field.ariaLabelProposed": "mkd.language.ui.field.ariaLabelProposed",
  "field.ariaLabelledby": "mkd.language.ui.field.ariaLabelledby",
  "field.ariaLabelledbyHelp": "mkd.language.ui.field.ariaLabelledbyHelp",
  "field.title": "mkd.language.ui.field.title",
  "field.ariaEmpty": "mkd.language.ui.field.ariaEmpty",
  "action.submit": "mkd.language.ui.action.submit",
  "action.cancel": "mkd.language.ui.action.cancel",
  "action.submitting": "mkd.language.ui.action.submitting",
  "error.localeRequired": "mkd.language.ui.error.localeRequired",
  "error.noChange": "mkd.language.ui.error.noChange",
  "error.proposedRequired": "mkd.language.ui.error.proposedRequired",
  "error.emailRequired": "mkd.language.ui.error.emailRequired",
  "error.emailInvalid": "mkd.language.ui.error.emailInvalid",
  "error.submitFailed": "mkd.language.ui.error.submitFailed",
  "status.success": "mkd.language.ui.status.success",
  "status.failure": "mkd.language.ui.status.failure",
  "footer.privacy": "mkd.language.ui.footer.privacy",
  "footer.accessibility": "mkd.language.ui.footer.accessibility",
  "footer.terms": "mkd.language.ui.footer.terms",
  "footer.copyright": "mkd.language.ui.footer.copyright"
};
var FOOTER_LINKS = {
  privacy: "https://monkeyking.dev/privacy",
  accessibility: "https://monkeyking.dev/accessibility",
  terms: "https://monkeyking.dev/terms"
};

// src/ui/trigger.ts
var MARK_SVG = `<svg viewBox="0 0 16 16" aria-hidden="true" focusable="false" xmlns="http://www.w3.org/2000/svg"><path fill="currentColor" d="M8 1.5c.4 0 .8.2 1 .5l1.2 2.1 2.3.4c.8.1 1.1 1 .5 1.6l-1.7 1.7.4 2.4c.1.8-.7 1.4-1.4 1L8 10.2l-2.1 1.1c-.7.4-1.5-.2-1.4-1l.4-2.4-1.7-1.7c-.6-.6-.3-1.5.5-1.6l2.3-.4L7 2c.2-.3.6-.5 1-.5zm0 2.2L7.1 5.3c-.1.2-.3.4-.5.4l-1.7.3 1.2 1.2c.2.2.3.4.2.6l-.3 1.7 1.5-.8c.2-.1.4-.1.6 0l1.5.8-.3-1.7c0-.2.1-.5.2-.6l1.2-1.2-1.7-.3c-.2 0-.4-.2-.5-.4L8 3.7z"/></svg>`;
function createTrigger(config, onActivate) {
  const btn = document.createElement("button");
  btn.type = "button";
  btn.className = "mkd-lang-icon";
  btn.setAttribute(TRIGGER_ATTR, "1");
  btn.setAttribute("aria-haspopup", "dialog");
  btn.setAttribute("aria-expanded", "false");
  btn.setAttribute("data-i18n-key", UI_MESSAGE_IDS["trigger.ariaLabel"]);
  const uiLocale = resolveUiLocale(config.uiLocale, config.locale);
  btn.setAttribute("aria-label", t("trigger.ariaLabel", uiLocale, config.messages));
  btn.insertAdjacentHTML("afterbegin", MARK_SVG);
  const handle = (e) => {
    e.preventDefault();
    e.stopPropagation();
    onActivate();
  };
  btn.addEventListener("click", handle);
  btn.addEventListener("pointerdown", (e) => e.stopPropagation());
  return btn;
}
function setTriggerExpanded(trigger, expanded) {
  trigger.setAttribute("aria-expanded", expanded ? "true" : "false");
}

// src/scan/attach.ts
var CHROME_SELECTORS = [
  '[role="tab"]',
  "legend",
  "th",
  "h1",
  "h2",
  "h3",
  "h4",
  "h5",
  "h6"
];
function buildSelectorList(config) {
  const parts = [
    "label",
    "button",
    "a[href]",
    '[role="button"]',
    "[aria-label]",
    "[aria-labelledby]",
    `.${cssEscapeClass(config.targetClass)}`
  ];
  if (config.includeChromeSelectors) {
    parts.push(...CHROME_SELECTORS);
  }
  if (config.scanMessageIdAttr && config.messageIdAttr) {
    parts.push(`[${cssEscapeAttr(config.messageIdAttr)}]`);
  }
  parts.push(...config.includeSelectors);
  return parts.join(",");
}
function cssEscapeClass(className) {
  return cssEscapeIdent(className);
}
function cssEscapeAttr(attr) {
  return cssEscapeIdent(attr);
}
function cssEscapeIdent(value) {
  if (typeof CSS !== "undefined" && typeof CSS.escape === "function") {
    return CSS.escape(value);
  }
  return value.replace(/([ !"#$%&'()*+,./:;<=>?@[\\\]^`{|}~])/g, "\\$1");
}
function matchReasonFor(el, targetClass, messageIdAttr) {
  const tag = el.tagName.toLowerCase();
  if (tag === "label") return "label";
  if (tag === "button") return "button";
  if (tag === "a" && el.hasAttribute("href")) return "link";
  if (el.getAttribute("role") === "button") return "role-button";
  if (el.getAttribute("role") === "tab") return "chrome";
  if (tag === "legend" || tag === "th" || /^h[1-6]$/.test(tag)) return "chrome";
  if (messageIdAttr && el.hasAttribute(messageIdAttr)) return "message-id";
  if (el.classList.contains(targetClass)) return "class";
  if (el.hasAttribute("aria-label") || el.hasAttribute("aria-labelledby")) return "aria";
  return "custom";
}
function shouldSkip(el, config) {
  if (!el.isConnected) return true;
  if (isLibraryUi(el)) return true;
  if (shouldIgnore(el, config.respectIgnore)) return true;
  if (el.hasAttribute("data-mkd-lang-trigger")) return true;
  return false;
}
function scanAndAttach(config, openPopover, boundElements) {
  const selector = buildSelectorList(config);
  let nodes;
  try {
    nodes = Array.from(config.root.querySelectorAll(selector));
  } catch {
    const fallback = [
      "label",
      "button",
      "a[href]",
      '[role="button"]',
      "[aria-label]",
      "[aria-labelledby]",
      `.${cssEscapeClass(config.targetClass)}`
    ].join(",");
    nodes = Array.from(config.root.querySelectorAll(fallback));
  }
  for (const el of nodes) {
    if (shouldSkip(el, config)) continue;
    const existing = findTrigger(el);
    if (isBound(el) && existing) {
      boundElements.add(el);
      continue;
    }
    if (isBound(el) && !existing) {
      unmarkBound(el);
    }
    attachTrigger(el, config, openPopover, boundElements);
  }
}
function attachTrigger(el, config, openPopover, boundElements) {
  markBound(el);
  const trigger = createTrigger(config, () => {
    openPopover(
      el,
      trigger,
      matchReasonFor(el, config.targetClass, config.messageIdAttr)
    );
  });
  if (canHaveChildren(el)) {
    el.appendChild(trigger);
  } else if (el.parentNode) {
    el.parentNode.insertBefore(trigger, el.nextSibling);
  } else {
    unmarkBound(el);
    return;
  }
  boundElements.add(el);
}
function canHaveChildren(el) {
  const voidTags = /* @__PURE__ */ new Set([
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
    "wbr"
  ]);
  return !voidTags.has(el.tagName.toLowerCase());
}
function detachAll(boundElements) {
  for (const el of boundElements) {
    const trigger = findTrigger(el);
    trigger?.remove();
    unmarkBound(el);
    el.removeAttribute(BOUND_ATTR);
  }
  boundElements.clear();
}

// src/scan/observer.ts
function startObserver(config, openPopover, boundElements, onScan) {
  if (config.once) return null;
  if (typeof MutationObserver === "undefined") return null;
  let scheduled = false;
  const observer = new MutationObserver((mutations) => {
    const relevant = mutations.some((m) => {
      if (m.target instanceof Element && isLibraryUi(m.target)) return false;
      for (const n of Array.from(m.addedNodes)) {
        if (n instanceof Element && isLibraryUi(n)) continue;
        return true;
      }
      for (const n of Array.from(m.removedNodes)) {
        if (n instanceof Element && isLibraryUi(n)) continue;
        return true;
      }
      return m.type === "childList";
    });
    if (!relevant) return;
    if (scheduled) return;
    scheduled = true;
    queueMicrotask(() => {
      scheduled = false;
      scanAndAttach(config, openPopover, boundElements);
      onScan();
    });
  });
  const rootNode = config.root instanceof Document ? config.root.documentElement : config.root;
  observer.observe(rootNode, {
    childList: true,
    subtree: true
  });
  return observer;
}

// src/util/text.ts
function extractVisibleText(el) {
  const parts = [];
  const walk = (node) => {
    if (node.nodeType === Node.TEXT_NODE) {
      const t2 = node.textContent ?? "";
      if (t2) parts.push(t2);
      return;
    }
    if (node.nodeType !== Node.ELEMENT_NODE) return;
    const element = node;
    if (element.hasAttribute(TRIGGER_ATTR)) return;
    const tag = element.tagName.toLowerCase();
    if (tag === "script" || tag === "style" || tag === "noscript") return;
    for (const child of Array.from(element.childNodes)) {
      walk(child);
    }
  };
  walk(el);
  return parts.join("").replace(/\s+/g, " ").trim();
}
function attrOrNull(el, name) {
  const v = el.getAttribute(name);
  if (v == null) return null;
  const trimmed = v.trim();
  return trimmed.length ? trimmed : null;
}

// src/util/message-id.ts
function resolveMessageId(el, config) {
  const { messageIdAttr, messageIdAncestorWalk, getMessageId, root } = config;
  if (messageIdAttr) {
    const fromAttr = readAttrWalk(el, messageIdAttr, messageIdAncestorWalk, root);
    if (fromAttr) return fromAttr;
  }
  if (getMessageId) {
    const fromFn = getMessageId(el);
    if (fromFn?.trim()) return fromFn.trim();
  }
  return null;
}
function readAttrWalk(el, attr, walk, root) {
  let current = el;
  while (current) {
    const v = current.getAttribute(attr);
    if (v?.trim()) return v.trim();
    if (!walk) break;
    if (current === root || !current.parentElement) break;
    if (root instanceof Element && !root.contains(current.parentElement)) break;
    current = current.parentElement;
  }
  return null;
}

// src/util/validate.ts
function isValidEmail(email) {
  const s = email.trim();
  if (!s || s.length > 254) return false;
  return /^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(s);
}
function validateSubmission(payload) {
  if (!payload.locale.trim()) {
    return { ok: false, errorKey: "error.localeRequired" };
  }
  const email = (payload.email ?? "").trim();
  if (!email) {
    return { ok: false, errorKey: "error.emailRequired" };
  }
  if (!isValidEmail(email)) {
    return { ok: false, errorKey: "error.emailInvalid" };
  }
  const proposedText = payload.proposedText.trim();
  const currentText = payload.currentText.trim();
  const proposedAria = (payload.proposedAriaLabel ?? "").trim();
  const currentAria = (payload.currentAriaLabel ?? "").trim();
  const notes = payload.notes.trim();
  const textChanged = proposedText !== currentText;
  const ariaChanged = payload.proposedAriaLabel != null && proposedAria !== currentAria;
  const hasVisible = currentText.length > 0 || proposedText.length > 0;
  const hasAriaSource = currentAria.length > 0 || proposedAria.length > 0;
  if (hasVisible && !proposedText && !hasAriaSource) {
    return { ok: false, errorKey: "error.proposedRequired" };
  }
  if (!hasVisible && hasAriaSource && !proposedAria) {
    return { ok: false, errorKey: "error.proposedRequired" };
  }
  if (!textChanged && !ariaChanged && !notes) {
    return { ok: false, errorKey: "error.noChange" };
  }
  return { ok: true };
}

// src/ui/styles.ts
var CSS2 = `
.mkd-lang-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  vertical-align: super;
  margin-inline-start: 0.15em;
  padding: 0;
  width: 1.25em;
  height: 1.25em;
  min-width: 24px;
  min-height: 24px;
  border: none;
  background: transparent;
  color: inherit;
  opacity: var(--mkd-lang-icon-opacity, 0.55);
  cursor: pointer;
  line-height: 1;
  font-size: var(--mkd-lang-icon-size, 0.75em);
  border-radius: 0.25em;
  flex-shrink: 0;
}
.mkd-lang-icon:hover,
.mkd-lang-icon:focus-visible {
  opacity: 1;
  outline: 2px solid var(--mkd-lang-accent, #c9a227);
  outline-offset: 1px;
}
.mkd-lang-icon svg {
  width: 1em;
  height: 1em;
  display: block;
  pointer-events: none;
}
.mkd-lang-popover {
  position: fixed;
  z-index: var(--mkd-lang-z, 10000);
  box-sizing: border-box;
  width: min(30rem, calc(100vw - 1.5rem));
  max-height: min(42rem, calc(100vh - 2rem));
  overflow: auto;
  padding: 0.85rem 1rem 0.9rem;
  border-radius: 0.5rem;
  border: 1px solid color-mix(in srgb, var(--mkd-lang-accent, #c9a227) 45%, #444);
  background: #1a1a1c;
  color: #f3f1ea;
  font-family: system-ui, -apple-system, Segoe UI, Roboto, sans-serif;
  font-size: 0.875rem;
  line-height: 1.4;
  box-shadow: 0 8px 28px rgba(0, 0, 0, 0.35);
}
.mkd-lang-popover *,
.mkd-lang-popover *::before,
.mkd-lang-popover *::after {
  box-sizing: border-box;
}
.mkd-lang-popover__title {
  margin: 0 0 0.65rem;
  font-size: 1rem;
  font-weight: 600;
  color: var(--mkd-lang-accent, #e0c35a);
}
.mkd-lang-popover__tabs {
  display: flex;
  gap: 0.25rem;
  margin-bottom: 0.75rem;
  border-bottom: 1px solid #333;
}
.mkd-lang-popover__tab {
  appearance: none;
  border: none;
  background: transparent;
  color: #c8c5bc;
  padding: 0.4rem 0.65rem;
  cursor: pointer;
  font: inherit;
  border-bottom: 2px solid transparent;
  margin-bottom: -1px;
}
.mkd-lang-popover__tab[aria-selected="true"] {
  color: var(--mkd-lang-accent, #e0c35a);
  border-bottom-color: var(--mkd-lang-accent, #c9a227);
  font-weight: 600;
}
.mkd-lang-popover__tab:focus-visible {
  outline: 2px solid var(--mkd-lang-accent, #c9a227);
  outline-offset: 2px;
}
.mkd-lang-popover__panel[hidden] {
  display: none;
}
.mkd-lang-field {
  display: flex;
  flex-direction: column;
  gap: 0.25rem;
  margin-bottom: 0.65rem;
}
.mkd-lang-field label,
.mkd-lang-field .mkd-lang-label {
  font-size: 0.75rem;
  font-weight: 600;
  color: #c8c5bc;
}
.mkd-lang-field .mkd-lang-readonly {
  padding: 0.4rem 0.5rem;
  border-radius: 0.35rem;
  background: #121214;
  border: 1px solid #333;
  color: #e8e6df;
  white-space: pre-wrap;
  word-break: break-word;
  min-height: 1.6em;
}
.mkd-lang-field input,
.mkd-lang-field textarea {
  font: inherit;
  color: #f3f1ea;
  background: #121214;
  border: 1px solid #444;
  border-radius: 0.35rem;
  padding: 0.4rem 0.5rem;
  width: 100%;
}
.mkd-lang-field input:focus-visible,
.mkd-lang-field textarea:focus-visible {
  outline: 2px solid var(--mkd-lang-accent, #c9a227);
  outline-offset: 1px;
  border-color: var(--mkd-lang-accent, #c9a227);
}
.mkd-lang-field textarea {
  min-height: 3.5rem;
  resize: vertical;
}
.mkd-lang-help {
  font-size: 0.75rem;
  color: #a8a59c;
  margin: 0 0 0.65rem;
}
.mkd-lang-error {
  color: #ff8e8e;
  font-size: 0.8rem;
  margin: 0 0 0.5rem;
  min-height: 1.1em;
}
.mkd-lang-status {
  font-size: 0.8rem;
  margin: 0 0 0.5rem;
  min-height: 1.1em;
  color: #9ddea3;
}
.mkd-lang-actions {
  display: flex;
  justify-content: flex-end;
  gap: 0.5rem;
  margin-top: 0.35rem;
}
.mkd-lang-btn {
  appearance: none;
  font: inherit;
  font-weight: 600;
  padding: 0.4rem 0.75rem;
  border-radius: 0.35rem;
  cursor: pointer;
  border: 1px solid #555;
  background: #2a2a2e;
  color: #f3f1ea;
}
.mkd-lang-btn:focus-visible {
  outline: 2px solid var(--mkd-lang-accent, #c9a227);
  outline-offset: 2px;
}
.mkd-lang-btn--primary {
  background: var(--mkd-lang-accent, #c9a227);
  border-color: color-mix(in srgb, var(--mkd-lang-accent, #c9a227) 70%, #000);
  color: #1a1400;
}
.mkd-lang-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}
.mkd-lang-footer {
  margin-top: 0.85rem;
  padding-top: 0.65rem;
  border-top: 1px solid #333;
  display: flex;
  flex-direction: column;
  gap: 0.35rem;
  font-size: 0.7rem;
  color: #a8a59c;
}
.mkd-lang-footer__links {
  display: flex;
  flex-wrap: wrap;
  gap: 0.35rem 0.75rem;
  align-items: center;
}
.mkd-lang-footer a {
  color: #c8c5bc;
  text-decoration: underline;
  text-underline-offset: 2px;
}
.mkd-lang-footer a:hover,
.mkd-lang-footer a:focus-visible {
  color: var(--mkd-lang-accent, #e0c35a);
}
.mkd-lang-footer a:focus-visible {
  outline: 2px solid var(--mkd-lang-accent, #c9a227);
  outline-offset: 2px;
  border-radius: 0.15em;
}
.mkd-lang-footer__copy {
  margin: 0;
}
@media (prefers-reduced-motion: reduce) {
  .mkd-lang-popover {
    transition: none !important;
  }
}
`;
function ensureStyles(zIndex) {
  if (typeof document === "undefined") return;
  let style = document.getElementById(STYLE_ID);
  if (!style) {
    style = document.createElement("style");
    style.id = STYLE_ID;
    style.textContent = CSS2;
    document.head.appendChild(style);
  }
  document.documentElement.style.setProperty("--mkd-lang-z", String(zIndex));
}
function removeStyles() {
  document.getElementById(STYLE_ID)?.remove();
}

// src/ui/popover.ts
var idSeq = 0;
function uid(prefix) {
  idSeq += 1;
  return `mkd-lang-${prefix}-${idSeq}`;
}
function applyMsgId(el, key) {
  el.setAttribute("data-i18n-key", UI_MESSAGE_IDS[key]);
}
function createPopoverController(getConfig) {
  let root = null;
  let activeTrigger = null;
  let activeEl = null;
  let matchReason = "custom";
  let removeListeners = null;
  let submitting = false;
  const close = () => {
    if (removeListeners) {
      removeListeners();
      removeListeners = null;
    }
    if (activeTrigger) {
      setTriggerExpanded(activeTrigger, false);
      activeTrigger.focus();
    }
    root?.remove();
    root = null;
    activeTrigger = null;
    activeEl = null;
    submitting = false;
  };
  const open = (el, trigger, reason) => {
    const config = getConfig();
    ensureStyles(config.zIndex);
    if (root && activeTrigger === trigger) {
      close();
      return;
    }
    close();
    activeEl = el;
    activeTrigger = trigger;
    matchReason = reason;
    setTriggerExpanded(trigger, true);
    const uiLocale = resolveUiLocale(config.uiLocale, config.locale);
    const contentLocale = resolveLocaleValue(config.locale) ?? "";
    const email = config.getUserEmail?.()?.trim() || config.userEmail?.trim() || "";
    const currentText = extractVisibleText(el);
    const currentAriaLabel = attrOrNull(el, "aria-label");
    const ariaLabelledby = attrOrNull(el, "aria-labelledby");
    const currentTitle = attrOrNull(el, "title");
    const messageId = resolveMessageId(el, config);
    const titleId = uid("title");
    const tabTextId = uid("tab-text");
    const tabAriaId = uid("tab-aria");
    const panelTextId = uid("panel-text");
    const panelAriaId = uid("panel-aria");
    const errorId = uid("error");
    const statusId = uid("status");
    const proposedId = uid("proposed");
    const notesId = uid("notes");
    const emailId = uid("email");
    const ariaProposedId = uid("aria-proposed");
    const tt = (key) => t(key, uiLocale, config.messages);
    root = document.createElement("div");
    root.className = "mkd-lang-popover";
    root.setAttribute(POPOVER_ROOT_ATTR, "1");
    root.setAttribute("data-mkd-lang-ignore", "1");
    root.classList.add("mkd-lang-ignore");
    root.setAttribute("role", "dialog");
    root.setAttribute("aria-modal", "true");
    root.setAttribute("aria-labelledby", titleId);
    if (isRtlLocale(uiLocale)) {
      root.dir = "rtl";
    }
    const title = document.createElement("h2");
    title.id = titleId;
    title.className = "mkd-lang-popover__title";
    applyMsgId(title, "dialog.title");
    setText(title, tt("dialog.title"));
    root.appendChild(title);
    const tablist = document.createElement("div");
    tablist.className = "mkd-lang-popover__tabs";
    tablist.setAttribute("role", "tablist");
    tablist.setAttribute("aria-label", tt("dialog.title"));
    const tabText = document.createElement("button");
    tabText.type = "button";
    tabText.className = "mkd-lang-popover__tab";
    tabText.id = tabTextId;
    tabText.setAttribute("role", "tab");
    tabText.setAttribute("aria-selected", "true");
    tabText.setAttribute("aria-controls", panelTextId);
    tabText.tabIndex = 0;
    applyMsgId(tabText, "tab.text");
    setText(tabText, tt("tab.text"));
    const tabAria = document.createElement("button");
    tabAria.type = "button";
    tabAria.className = "mkd-lang-popover__tab";
    tabAria.id = tabAriaId;
    tabAria.setAttribute("role", "tab");
    tabAria.setAttribute("aria-selected", "false");
    tabAria.setAttribute("aria-controls", panelAriaId);
    tabAria.tabIndex = -1;
    applyMsgId(tabAria, "tab.aria");
    setText(tabAria, tt("tab.aria"));
    tablist.append(tabText, tabAria);
    root.appendChild(tablist);
    const panelText = document.createElement("div");
    panelText.id = panelTextId;
    panelText.className = "mkd-lang-popover__panel";
    panelText.setAttribute("role", "tabpanel");
    panelText.setAttribute("aria-labelledby", tabTextId);
    panelText.appendChild(
      readonlyField(tt("field.currentText"), currentText || "\u2014", uid("cur"), "field.currentText")
    );
    panelText.appendChild(
      readonlyField(
        tt("field.messageId"),
        messageId ?? tt("field.messageIdMissing"),
        uid("mid"),
        "field.messageId"
      )
    );
    panelText.appendChild(
      readonlyField(tt("field.locale"), contentLocale || "\u2014", uid("loc"), "field.locale")
    );
    const proposedField = editableField({
      label: tt("field.proposedText"),
      id: proposedId,
      multiline: true,
      value: currentText,
      msgKey: "field.proposedText"
    });
    panelText.appendChild(proposedField.wrap);
    const notesField = editableField({
      label: tt("field.notes"),
      id: notesId,
      multiline: true,
      value: "",
      msgKey: "field.notes"
    });
    panelText.appendChild(notesField.wrap);
    const emailField = editableField({
      label: tt("field.email"),
      id: emailId,
      multiline: false,
      value: email,
      inputType: "email",
      required: true,
      msgKey: "field.email"
    });
    panelText.appendChild(emailField.wrap);
    const panelAria = document.createElement("div");
    panelAria.id = panelAriaId;
    panelAria.className = "mkd-lang-popover__panel";
    panelAria.setAttribute("role", "tabpanel");
    panelAria.setAttribute("aria-labelledby", tabAriaId);
    panelAria.hidden = true;
    const hasAria = currentAriaLabel != null || ariaLabelledby != null || currentTitle != null;
    if (!hasAria) {
      const empty = document.createElement("p");
      empty.className = "mkd-lang-help";
      applyMsgId(empty, "field.ariaEmpty");
      setText(empty, tt("field.ariaEmpty"));
      panelAria.appendChild(empty);
    }
    panelAria.appendChild(
      readonlyField(
        tt("field.ariaLabelCurrent"),
        currentAriaLabel ?? "\u2014",
        uid("aria-cur"),
        "field.ariaLabelCurrent"
      )
    );
    const ariaProposedField = editableField({
      label: tt("field.ariaLabelProposed"),
      id: ariaProposedId,
      multiline: true,
      value: currentAriaLabel ?? "",
      msgKey: "field.ariaLabelProposed"
    });
    panelAria.appendChild(ariaProposedField.wrap);
    panelAria.appendChild(
      readonlyField(
        tt("field.ariaLabelledby"),
        ariaLabelledby ?? "\u2014",
        uid("aria-lb"),
        "field.ariaLabelledby"
      )
    );
    if (ariaLabelledby) {
      const help = document.createElement("p");
      help.className = "mkd-lang-help";
      applyMsgId(help, "field.ariaLabelledbyHelp");
      setText(help, tt("field.ariaLabelledbyHelp"));
      panelAria.appendChild(help);
    }
    if (currentTitle) {
      panelAria.appendChild(
        readonlyField(tt("field.title"), currentTitle, uid("title-attr"), "field.title")
      );
    }
    root.append(panelText, panelAria);
    const errorEl = document.createElement("div");
    errorEl.id = errorId;
    errorEl.className = "mkd-lang-error";
    errorEl.setAttribute("role", "alert");
    root.appendChild(errorEl);
    const statusEl = document.createElement("div");
    statusEl.id = statusId;
    statusEl.className = "mkd-lang-status";
    statusEl.setAttribute("aria-live", "polite");
    root.appendChild(statusEl);
    const actions = document.createElement("div");
    actions.className = "mkd-lang-actions";
    const cancelBtn = document.createElement("button");
    cancelBtn.type = "button";
    cancelBtn.className = "mkd-lang-btn";
    applyMsgId(cancelBtn, "action.cancel");
    setText(cancelBtn, tt("action.cancel"));
    const submitBtn = document.createElement("button");
    submitBtn.type = "button";
    submitBtn.className = "mkd-lang-btn mkd-lang-btn--primary";
    applyMsgId(submitBtn, "action.submit");
    setText(submitBtn, tt("action.submit"));
    actions.append(cancelBtn, submitBtn);
    root.appendChild(actions);
    const footer = document.createElement("footer");
    footer.className = "mkd-lang-footer";
    const links = document.createElement("div");
    links.className = "mkd-lang-footer__links";
    const linkDefs = [
      { key: "footer.privacy", href: FOOTER_LINKS.privacy },
      { key: "footer.accessibility", href: FOOTER_LINKS.accessibility },
      { key: "footer.terms", href: FOOTER_LINKS.terms }
    ];
    for (const def of linkDefs) {
      const a = document.createElement("a");
      a.href = def.href;
      a.target = "_blank";
      a.rel = "noopener noreferrer";
      applyMsgId(a, def.key);
      setText(a, tt(def.key));
      links.appendChild(a);
    }
    footer.appendChild(links);
    const copy = document.createElement("p");
    copy.className = "mkd-lang-footer__copy";
    applyMsgId(copy, "footer.copyright");
    setText(copy, tt("footer.copyright"));
    footer.appendChild(copy);
    root.appendChild(footer);
    document.body.appendChild(root);
    positionPopover(root);
    const selectTab = (which) => {
      const isText = which === "text";
      tabText.setAttribute("aria-selected", isText ? "true" : "false");
      tabAria.setAttribute("aria-selected", isText ? "false" : "true");
      tabText.tabIndex = isText ? 0 : -1;
      tabAria.tabIndex = isText ? -1 : 0;
      panelText.hidden = !isText;
      panelAria.hidden = isText;
      (isText ? tabText : tabAria).focus();
    };
    tabText.addEventListener("click", () => selectTab("text"));
    tabAria.addEventListener("click", () => selectTab("aria"));
    const onTabKeydown = (e) => {
      if (e.key !== "ArrowRight" && e.key !== "ArrowLeft" && e.key !== "Home" && e.key !== "End") {
        return;
      }
      e.preventDefault();
      const tabs = [tabText, tabAria];
      let idx = tabs.findIndex((tb) => tb.getAttribute("aria-selected") === "true");
      if (e.key === "ArrowRight") idx = (idx + 1) % tabs.length;
      if (e.key === "ArrowLeft") idx = (idx - 1 + tabs.length) % tabs.length;
      if (e.key === "Home") idx = 0;
      if (e.key === "End") idx = tabs.length - 1;
      selectTab(idx === 0 ? "text" : "aria");
    };
    tablist.addEventListener("keydown", onTabKeydown);
    cancelBtn.addEventListener("click", () => close());
    submitBtn.addEventListener("click", async () => {
      if (submitting || !activeEl) return;
      const cfg = getConfig();
      const locale = resolveLocaleValue(cfg.locale) ?? "";
      const proposedText = proposedField.control.value;
      const notes = notesField.control.value;
      const emailVal = emailField.control.value;
      const proposedAria = ariaProposedField.control.value;
      const draft = {
        locale,
        currentText,
        proposedText,
        currentAriaLabel,
        proposedAriaLabel: currentAriaLabel != null || proposedAria.trim() ? proposedAria : null,
        notes,
        email: emailVal
      };
      const result = validateSubmission(draft);
      if (!result.ok) {
        setText(errorEl, tt(result.errorKey));
        setText(statusEl, "");
        if (result.errorKey === "error.emailRequired" || result.errorKey === "error.emailInvalid") {
          emailField.control.focus();
        }
        return;
      }
      setText(errorEl, "");
      const payload = {
        currentText,
        proposedText,
        currentAriaLabel,
        proposedAriaLabel: draft.proposedAriaLabel,
        ariaLabelledby,
        currentTitle,
        messageId,
        notes,
        email: emailVal.trim(),
        locale,
        source: {
          tagName: activeEl.tagName.toLowerCase(),
          matchReason,
          elementId: activeEl.id || void 0,
          pageUrl: typeof location !== "undefined" ? location.href : ""
        },
        submittedAt: (/* @__PURE__ */ new Date()).toISOString()
      };
      submitting = true;
      submitBtn.disabled = true;
      setText(submitBtn, tt("action.submitting"));
      try {
        await cfg.client.submit(payload);
        setText(statusEl, tt("status.success"));
        window.setTimeout(() => close(), 600);
      } catch {
        setText(errorEl, tt("error.submitFailed"));
        setText(statusEl, tt("status.failure"));
        submitting = false;
        submitBtn.disabled = false;
        setText(submitBtn, tt("action.submit"));
      }
    });
    const focusables = () => Array.from(
      root.querySelectorAll(
        'button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex="-1"])'
      )
    ).filter((n) => !n.hasAttribute("disabled") && n.offsetParent !== null);
    const onKeydown = (e) => {
      if (!root) return;
      if (e.key === "Escape") {
        e.preventDefault();
        e.stopPropagation();
        close();
        return;
      }
      if (e.key === "Tab") {
        const list = focusables();
        if (!list.length) return;
        const first = list[0];
        const last = list[list.length - 1];
        if (e.shiftKey && document.activeElement === first) {
          e.preventDefault();
          last.focus();
        } else if (!e.shiftKey && document.activeElement === last) {
          e.preventDefault();
          first.focus();
        }
      }
    };
    const onPointerDown = (e) => {
      if (!root) return;
      const target = e.target;
      if (target && (root.contains(target) || activeTrigger?.contains(target))) {
        return;
      }
      close();
    };
    const onReposition = () => {
      if (root) positionPopover(root);
    };
    document.addEventListener("keydown", onKeydown, true);
    document.addEventListener("pointerdown", onPointerDown, true);
    window.addEventListener("resize", onReposition);
    window.addEventListener("scroll", onReposition, true);
    removeListeners = () => {
      document.removeEventListener("keydown", onKeydown, true);
      document.removeEventListener("pointerdown", onPointerDown, true);
      window.removeEventListener("resize", onReposition);
      window.removeEventListener("scroll", onReposition, true);
    };
    queueMicrotask(() => {
      proposedField.control.focus();
    });
  };
  return {
    open,
    close,
    destroy: () => {
      close();
    },
    isOpen: () => root != null
  };
}
function readonlyField(label, value, id, msgKey) {
  const wrap = document.createElement("div");
  wrap.className = "mkd-lang-field";
  const lab = document.createElement("div");
  lab.className = "mkd-lang-label";
  lab.id = id;
  if (msgKey) applyMsgId(lab, msgKey);
  setText(lab, label);
  const val = document.createElement("div");
  val.className = "mkd-lang-readonly";
  val.setAttribute("aria-labelledby", id);
  setText(val, value);
  wrap.append(lab, val);
  return wrap;
}
function editableField(opts) {
  const wrap = document.createElement("div");
  wrap.className = "mkd-lang-field";
  const lab = document.createElement("label");
  lab.htmlFor = opts.id;
  if (opts.msgKey) applyMsgId(lab, opts.msgKey);
  setText(lab, opts.label);
  let control;
  if (opts.multiline) {
    const ta = document.createElement("textarea");
    ta.id = opts.id;
    ta.value = opts.value;
    if (opts.required) {
      ta.required = true;
      ta.setAttribute("aria-required", "true");
    }
    control = ta;
  } else {
    const input = document.createElement("input");
    input.type = opts.inputType ?? "text";
    input.id = opts.id;
    input.value = opts.value;
    if (opts.required) {
      input.required = true;
      input.setAttribute("aria-required", "true");
    }
    if (opts.inputType === "email") {
      input.autocomplete = "email";
    }
    control = input;
  }
  wrap.append(lab, control);
  return { wrap, control };
}
function positionPopover(popover) {
  const margin = 12;
  const pop = popover.getBoundingClientRect();
  const vw = window.innerWidth;
  const vh = window.innerHeight;
  let left = (vw - pop.width) / 2;
  let top = Math.max(margin, vh * 0.12);
  if (left + pop.width > vw - margin) {
    left = Math.max(margin, vw - margin - pop.width);
  }
  if (left < margin) left = margin;
  if (top + pop.height > vh - margin) {
    top = Math.max(margin, vh - margin - pop.height);
  }
  if (top < margin) top = margin;
  popover.style.top = `${Math.round(top)}px`;
  popover.style.left = `${Math.round(left)}px`;
}

// src/init.ts
var activeHandle = null;
var activeState = null;
function resolveSubmissionClient(options, debug) {
  if (options.client) return options.client;
  const postUrl = options.postUrl?.trim();
  if (postUrl) {
    return new HttpSubmissionClient({
      postUrl,
      headers: options.postHeaders,
      credentials: options.postCredentials
    });
  }
  return new NoopSubmissionClient(debug);
}
function resolveConfig(options = {}) {
  const debug = options.debug ?? false;
  return {
    locale: options.locale,
    uiLocale: options.uiLocale,
    userEmail: options.userEmail,
    getUserEmail: options.getUserEmail,
    messageIdAttr: options.messageIdAttr === void 0 ? "data-i18n-key" : options.messageIdAttr,
    scanMessageIdAttr: options.scanMessageIdAttr ?? true,
    getMessageId: options.getMessageId,
    messageIdAncestorWalk: options.messageIdAncestorWalk ?? true,
    targetClass: options.targetClass ?? "mkd-lang-target",
    includeChromeSelectors: options.includeChromeSelectors ?? true,
    includeSelectors: options.includeSelectors ?? [],
    root: options.root ?? document.documentElement,
    client: resolveSubmissionClient(options, debug),
    postUrl: options.postUrl?.trim() || void 0,
    postHeaders: options.postHeaders,
    postCredentials: options.postCredentials,
    respectIgnore: options.respectIgnore ?? true,
    zIndex: options.zIndex ?? 1e4,
    once: options.once ?? false,
    debug,
    messages: options.messages ?? {}
  };
}
function mergeConfig(current, partial) {
  const next = { ...current };
  if (partial.locale !== void 0) next.locale = partial.locale;
  if (partial.uiLocale !== void 0) next.uiLocale = partial.uiLocale;
  if (partial.userEmail !== void 0) next.userEmail = partial.userEmail;
  if (partial.getUserEmail !== void 0) next.getUserEmail = partial.getUserEmail;
  if (partial.messageIdAttr !== void 0) next.messageIdAttr = partial.messageIdAttr;
  if (partial.scanMessageIdAttr !== void 0) {
    next.scanMessageIdAttr = partial.scanMessageIdAttr;
  }
  if (partial.getMessageId !== void 0) next.getMessageId = partial.getMessageId;
  if (partial.messageIdAncestorWalk !== void 0) {
    next.messageIdAncestorWalk = partial.messageIdAncestorWalk;
  }
  if (partial.targetClass !== void 0) next.targetClass = partial.targetClass;
  if (partial.includeChromeSelectors !== void 0) {
    next.includeChromeSelectors = partial.includeChromeSelectors;
  }
  if (partial.includeSelectors !== void 0) {
    next.includeSelectors = partial.includeSelectors;
  }
  if (partial.root !== void 0) next.root = partial.root;
  if (partial.postUrl !== void 0) next.postUrl = partial.postUrl?.trim() || void 0;
  if (partial.postHeaders !== void 0) next.postHeaders = partial.postHeaders;
  if (partial.postCredentials !== void 0) next.postCredentials = partial.postCredentials;
  if (partial.client !== void 0) {
    next.client = partial.client;
  } else if (partial.postUrl !== void 0 || partial.postHeaders !== void 0 || partial.postCredentials !== void 0 || partial.debug !== void 0) {
    next.client = resolveSubmissionClient(
      {
        client: void 0,
        postUrl: next.postUrl,
        postHeaders: next.postHeaders,
        postCredentials: next.postCredentials,
        debug: partial.debug ?? next.debug
      },
      partial.debug ?? next.debug
    );
  }
  if (partial.respectIgnore !== void 0) next.respectIgnore = partial.respectIgnore;
  if (partial.zIndex !== void 0) next.zIndex = partial.zIndex;
  if (partial.once !== void 0) next.once = partial.once;
  if (partial.debug !== void 0) next.debug = partial.debug;
  if (partial.messages !== void 0) next.messages = partial.messages;
  return next;
}
function init(options = {}) {
  if (typeof document === "undefined") {
    throw new Error("@mkd/language: init() requires a browser document");
  }
  if (activeState && activeHandle) {
    activeHandle.configure(options);
    activeHandle.rescan();
    return activeHandle;
  }
  const config = resolveConfig(options);
  ensureStyles(config.zIndex);
  const boundElements = /* @__PURE__ */ new Set();
  const popover = createPopoverController(() => activeState.config);
  const state = {
    config,
    boundElements,
    observer: null,
    popover
  };
  activeState = state;
  const rescan = () => {
    if (!activeState) return;
    scanAndAttach(activeState.config, activeState.popover.open, activeState.boundElements);
  };
  const destroy = () => {
    if (!activeState) return;
    activeState.observer?.disconnect();
    activeState.popover.destroy();
    detachAll(activeState.boundElements);
    removeStyles();
    activeState = null;
    activeHandle = null;
  };
  const configure = (partial) => {
    if (!activeState) return;
    activeState.config = mergeConfig(activeState.config, partial);
    ensureStyles(activeState.config.zIndex);
    activeState.observer?.disconnect();
    activeState.observer = startObserver(
      activeState.config,
      activeState.popover.open,
      activeState.boundElements,
      () => {
      }
    );
  };
  rescan();
  state.observer = startObserver(config, popover.open, boundElements, () => {
  });
  const handle = {
    rescan,
    destroy,
    configure
  };
  activeHandle = handle;
  return handle;
}

// src/host/createTrackedMessage.ts
function normalizeTrackedText(text) {
  return text.replace(/\s+/g, " ").trim();
}
function createTrackedMessage(resolve) {
  const byText = /* @__PURE__ */ new Map();
  function message(key, args) {
    const text = resolve(key, args);
    const normalized = normalizeTrackedText(text ?? "");
    if (normalized && key) {
      byText.set(normalized, key);
    }
    return text;
  }
  function getMessageId(el) {
    const normalized = normalizeTrackedText(extractVisibleText(el));
    if (!normalized) return void 0;
    return byText.get(normalized);
  }
  return {
    message,
    getMessageId,
    clear: () => {
      byText.clear();
    },
    size: () => byText.size
  };
}

// src/host/messageIdProps.ts
var MESSAGE_ID_ATTR = "data-i18n-key";
var TARGET_CLASS = "mkd-lang-target";
function messageIdProps(key, options = {}) {
  const attr = options.attr ?? MESSAGE_ID_ATTR;
  const props = { [attr]: key };
  if (options.markTarget) {
    const target = options.targetClass ?? TARGET_CLASS;
    props.className = [target, options.className].filter(Boolean).join(" ");
  } else if (options.className) {
    props.className = options.className;
  }
  return props;
}
export {
  FOOTER_LINKS,
  HttpSubmissionClient,
  MESSAGE_ID_ATTR,
  NoopSubmissionClient,
  TARGET_CLASS,
  UI_MESSAGE_IDS,
  builtInLocales,
  createTrackedMessage,
  init,
  isValidEmail,
  messageIdProps,
  normalizeTrackedText,
  registerCatalog,
  validateSubmission
};
