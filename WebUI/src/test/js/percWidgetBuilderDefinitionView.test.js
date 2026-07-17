/**
 * Copyright 1999-2026 Percussion Software, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * Regression tests for WebUI/src/main/webapp/cm/widgetbuilder/js/views/PercWidgetBuilderDefinitionView.js
 *
 * Closes GitHub CodeQL alerts (js/xss-through-dom) flagged on:
 * - `$el.find("input[name=...]").parent().append('<label ...>' + this.message + '</label>')`
 * in `showErrors()` — attacker-controlled validation messages are interpolated
 * into an HTML string and appended to the DOM.
 *
 * Pre-fix code constructs an HTML string with `this.message` (from Backbone
 * model validation) appended directly, so a message containing
 * `<script>...</script>` or `<img onerror=...>` produces live DOM elements
 * inside the widget builder form.
 *
 * Test strategy (Constitution III fail-then-pass):
 * - Instantiate a WidgetDefinitionGeneralView, seed its template into DOM,
 * call render(), then call showErrors() with an XSS payload.
 * - Assert that no live <script> / <img> elements are produced and that
 * the message is present as inert text.
 *
 * Test strategy (Constitution III fail-then-pass):
 * - Load source via `readFileSync` to ensure test runs against the
 * checked-in code (same as PercUserView / PercListEditorWidget tests).
 */

import { readFileSync } from "fs";
import { resolve, dirname } from "path";
import { fileURLToPath } from "url";
import jquery from "jquery";
import { beforeEach, afterEach, describe, it, expect, vi } from "vitest";

const __dirname = dirname(fileURLToPath(import.meta.url));
const SRC_PATH = resolve(
 __dirname,
 "../../main/webapp/cm/widgetbuilder/js/views/PercWidgetBuilderDefinitionView.js"
);

let $;
let WidgetBuilderApp;

function seedDom() {
 document.body.innerHTML = `
 <script type="text/template" id="perc-widget-general-tab-template">
 <form name="perc-widget-general-tab-form">
 <input name="widgetname" value="">
 <input name="label" value="">
 <textarea name="description"></textarea>
 <input name="prefix" value="">
 <input name="author" value="">
 <input name="publisherUrl" value="">
 <input name="version" value="">
 <input name="widgetTrayCustomizedIconPath" value="">
 <input name="toolTipMessage" value="">
 <input type="checkbox" name="responsive">
 </form>
 </script>
 `;
}

function loadSource() {
 let jq = jquery(globalThis.window);
 if (typeof jq !== "function") {
 jq = typeof jquery === "function" ? jquery : jquery.fn || jquery;
 if (!jq.fn && jq.prototype) jq.fn = jq.prototype;
 }
 if (!jq.fn) throw new Error("jquery has no .fn");
 $ = jq;
 globalThis.jQuery = $;
 globalThis.$ = $;
 globalThis.I18N = { message: (key) => `[${key}]` };

 // Backbone / Underscore globals (the source depends on them)
  globalThis._ = { template: (str) => {
 const fn = new Function("obj", 'with(obj||{}){return `' + str.replace(/<%=\s*(\w+)\s*%>/g, "${$1}") + '`}');
 return (data) => fn(data);
 }};

 // Minimal Backbone.View stand-in with a working `.extend()` (the real
 // Backbone library isn't a test dependency; the source under test only
 // relies on `extend`, `initialize`, `this.el`/`this.$el`, and `this.model`).
 function View(options) {
 this.el = document.createElement(this.tagName || "div");
 this.$el = $(this.el);
 this.model = options && options.model ? options.model : { toJSON: () => ({}) };
 if (this.initialize) this.initialize(options);
 }
 View.extend = function (protoProps) {
 const Parent = this;
 function Child(options) {
 Parent.call(this, options);
 }
 Child.prototype = Object.create(Parent.prototype);
 Object.assign(Child.prototype, protoProps);
 Child.extend = Parent.extend;
 return Child;
 };
 globalThis.Backbone = { View: View };
 globalThis.WidgetBuilderApp = {
 dirtyController: { setDirty: () => {} },
 saveOnDirty: false,
 };
 WidgetBuilderApp = globalThis.WidgetBuilderApp;
}

beforeEach(() => {
 seedDom();
 loadSource();
 const src = readFileSync(SRC_PATH, "utf8");
 // eslint-disable-next-line no-eval
 eval(src);
});

afterEach(() => {
 document.body.innerHTML = "";
 $(document.body).empty();
});

describe("PercWidgetBuilderDefinitionView XSS regression", () => {
 it("should not produce live script elements from validation messages", () => {
 const model = {
 toJSON: () => ({
 widgetname: "TestWidget",
 label: "Test Label",
 description: "desc",
 prefix: "",
 author: "admin",
 publisherUrl: "",
 version: "1.0",
 widgetTrayCustomizedIconPath: "",
 toolTipMessage: "tip",
 responsive: false,
 }),
 };

 const view = new WidgetBuilderApp.WidgetDefinitionGeneralView({ model });
 view.render();
 document.body.appendChild(view.el);

 const xssPayload = "<script>alert('XSS')</script>";
 view.showErrors([{ name: "widgetname", message: xssPayload }]);

 const scripts = view.el.querySelectorAll("script");
 expect(scripts.length).toBe(0);
 });

 it("should not produce live img elements from validation messages", () => {
 const model = {
 toJSON: () => ({
 widgetname: "TestWidget",
 label: "Test Label",
 description: "desc",
 prefix: "",
 author: "admin",
 publisherUrl: "",
 version: "1.0",
 widgetTrayCustomizedIconPath: "",
 toolTipMessage: "tip",
 responsive: false,
 }),
 };

 const view = new WidgetBuilderApp.WidgetDefinitionGeneralView({ model });
 view.render();
 document.body.appendChild(view.el);

 const xssPayload = "<img src=x onerror=alert('XSS')>";
 view.showErrors([{ name: "widgetname", message: xssPayload }]);

 const imgs = view.el.querySelectorAll("img");
 expect(imgs.length).toBe(0);
 });

 it("should render validation message as text, not as HTML", () => {
 const model = {
 toJSON: () => ({
 widgetname: "TestWidget",
 label: "Test Label",
 description: "desc",
 prefix: "",
 author: "admin",
 publisherUrl: "",
 version: "1.0",
 widgetTrayCustomizedIconPath: "",
 toolTipMessage: "tip",
 responsive: false,
 }),
 };

 const view = new WidgetBuilderApp.WidgetDefinitionGeneralView({ model });
 view.render();
 document.body.appendChild(view.el);

 const payload = "<b>bold</b>";
 view.showErrors([{ name: "widgetname", message: payload }]);

 const errorLabels = view.el.querySelectorAll(".perc_field_error");
 expect(errorLabels.length).toBe(1);
 expect(errorLabels[0].textContent).toBe(payload);
 expect(errorLabels[0].innerHTML).not.toContain("<b>");
 });
});
