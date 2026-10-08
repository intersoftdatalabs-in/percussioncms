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

import { describe, expect, it, vi } from "vitest";
import type { ItemEditorFields } from "../../../main/ts/editor/itemFieldsApi";
import {
  applyFieldOverlay,
  changedOverlayEdits,
  mergeOverlayEdits,
  overlayEditKey,
  parseAaFieldObjectId,
  persistOverlayEdits,
  longTextValue,
  readOverlayEdits,
  restoreOverlayValues,
  assemblyDatetimeValue,
  blankRequiredDateFieldNames,
  blankRequiredDatetimeFieldNames,
  blankRequiredNumberFieldNames,
  blankRequiredTextFieldNames,
  calendarDateText,
  htmlContainsNul,
  longTextContainsNul,
  nulHtmlFieldNames,
  nulLongTextFieldNames,
  nulSingleLineTextFieldNames,
  singleLineTextContainsNul,
  datetimeText,
  decimalNumberText,
  invalidChangedDateFieldNames,
  invalidChangedDatetimeFieldNames,
  invalidChangedNumberFieldNames,
  markAssemblyFieldErrors,
  scalarOverlayFields,
  singleLineText,
  stripLeftoverAaChrome,
  wholeNumberText,
} from "../../../main/ts/assembly/overlayFields";

const payload: ItemEditorFields = {
  contentId: "42",
  contentType: "percPage",
  name: "Home",
  checkoutUser: "admin",
  fields: [
    { name: "sys_title", value: "Home" },
    { name: "displaytitle", value: "Welcome" },
    { name: "description", value: "About the site" },
  ],
};

function aaId(contentId: string, field: string): string {
  return JSON.stringify([
    3,
    Number(contentId),
    7,
    0,
    0,
    0,
    0,
    1,
    0,
    0,
    0,
    field,
    Number(contentId),
    field,
    0,
  ]);
}

describe("scalarOverlayFields", () => {
  it("keeps text, long text, and HTML, and drops binary kinds", () => {
    const rows = scalarOverlayFields(
      {
        ...payload,
        fields: [
          ...payload.fields,
          { name: "notes", value: "A long note" },
          { name: "photo", value: "" },
        ],
      },
      [
        { name: "sys_title", label: "Title", control: "sys_EditBox" },
        { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
        { name: "description", label: "Body", control: "sys_tinymce" },
        { name: "notes", label: "Notes", control: "sys_TextArea" },
        { name: "photo", label: "Photo", control: "sys_file" },
      ],
    );
    expect(rows.map((r) => r.name)).toEqual([
      "sys_title",
      "displaytitle",
      "description",
      "notes",
    ]);
    expect(rows.find((r) => r.name === "displaytitle")?.label).toBe(
      "Display title",
    );
    expect(rows.find((r) => r.name === "description")?.kind).toBe("html");
    expect(rows.find((r) => r.name === "notes")?.kind).toBe("longtext");
  });

  it("omits a read-only HTML field so the overlay cannot write it", () => {
    const rows = scalarOverlayFields(
      {
        ...payload,
        fields: [...payload.fields, { name: "body", value: "<p>Old</p>" }],
      },
      [
        { name: "body", label: "Body", control: "sys_tinymce", readOnly: true },
        { name: "displaytitle", control: "sys_EditBox" },
      ],
    );
    expect(rows.map((r) => r.name)).toEqual([
      "sys_title",
      "displaytitle",
      "description",
    ]);
    expect(rows.some((r) => r.name === "body")).toBe(false);
  });

  it("keeps a link field and omits a read-only link", () => {
    const rows = scalarOverlayFields(
      {
        ...payload,
        fields: [
          ...payload.fields,
          { name: "pagelink", value: "//Sites/Example/index" },
          { name: "lockedlink", value: "594" },
          { name: "photo", value: "" },
        ],
      },
      [
        { name: "pagelink", label: "Page link", control: "sys_PageLink" },
        {
          name: "lockedlink",
          label: "Locked",
          control: "sys_ManagedLink",
          readOnly: true,
        },
        { name: "photo", control: "sys_file" },
        { name: "displaytitle", control: "sys_EditBox" },
        { name: "description", control: "sys_tinymce" },
      ],
    );
    expect(rows.find((r) => r.name === "pagelink")?.kind).toBe("link");
    expect(rows.find((r) => r.name === "description")?.kind).toBe("html");
    expect(rows.find((r) => r.name === "displaytitle")?.kind).toBe("text");
    expect(rows.some((r) => r.name === "lockedlink")).toBe(false);
    expect(rows.some((r) => r.name === "photo")).toBe(false);
  });

  it("treats maxtext as long text and omits a read-only long-text field", () => {
    const rows = scalarOverlayFields(
      {
        ...payload,
        fields: [
          { name: "notes", value: "A long note" },
          { name: "bodycopy", value: "Locked copy" },
          { name: "displaytitle", value: "Welcome" },
        ],
      },
      [
        {
          name: "notes",
          label: "Notes",
          control: "sys_EditBox",
          dataType: "maxtext",
        },
        {
          name: "bodycopy",
          label: "Body copy",
          control: "sys_TextArea",
          readOnly: true,
        },
        { name: "displaytitle", control: "sys_EditBox" },
      ],
    );
    expect(rows.find((r) => r.name === "notes")?.kind).toBe("longtext");
    expect(rows.some((r) => r.name === "bodycopy")).toBe(false);
    expect(rows.find((r) => r.name === "displaytitle")?.kind).toBe("text");
  });

  it("copies the content-type required flag onto single-line text", () => {
    const rows = scalarOverlayFields(
      {
        ...payload,
        fields: [
          { name: "displaytitle", value: "Welcome" },
          { name: "summary", value: "Optional" },
          { name: "notes", value: "A long note" },
        ],
      },
      [
        { name: "displaytitle", control: "sys_EditBox", required: true },
        { name: "summary", control: "sys_EditBox", required: false },
        { name: "notes", control: "sys_TextArea", required: true },
      ],
    );
    expect(rows.find((r) => r.name === "displaytitle")?.required).toBe(true);
    expect(rows.find((r) => r.name === "summary")?.required).toBe(false);
    expect(rows.find((r) => r.name === "notes")?.required).toBe(true);
    expect(rows.find((r) => r.name === "notes")?.kind).toBe("longtext");
  });
});

describe("parseAaFieldObjectId", () => {
  it("reads content id and field name from a PSAAObjectId array", () => {
    expect(parseAaFieldObjectId(aaId("42", "displaytitle"))).toEqual({
      contentId: "42",
      fieldName: "displaytitle",
    });
  });

  it("decodes HTML-quoted ids", () => {
    const raw = `[3,7,1,0,0,0,0,1,0,0,0,&quot;sys_title&quot;,7,&quot;Title&quot;,0]`;
    expect(parseAaFieldObjectId(raw)).toEqual({
      contentId: "7",
      fieldName: "sys_title",
    });
  });

  it("returns null for junk", () => {
    expect(parseAaFieldObjectId("field-displaytitle")).toBeNull();
    expect(parseAaFieldObjectId("")).toBeNull();
  });
});

describe("applyFieldOverlay", () => {
  it("makes known AA field wrappers contenteditable and strips leftover AA chrome", () => {
    const root = document.createElement("div");
    root.innerHTML = `
      <a href="javascript:void(0)"><img class="PsAaObjectImage" src="field_0.gif" /></a>
      <div class="PsAaField" id='${aaId("42", "displaytitle")}'
           onclick="return ps.aa.controller.fieldEdit.editField(this, event);">Welcome</div>
      <span>Other</span>
    `;
    const fields = scalarOverlayFields(payload, [
      { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
    ]);
    const hits = applyFieldOverlay(root, fields, "42");
    expect(hits).toHaveLength(1);
    expect(hits[0]?.name).toBe("displaytitle");
    expect(root.querySelector("img.PsAaObjectImage")).toBeNull();
    const edited = root.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    expect(edited.contentEditable).toMatch(/true/i);
    expect(edited.getAttribute("onclick")).toBeNull();
    expect(edited.getAttribute("data-assembly-value")).toBe("text");
    edited.textContent = "Updated";
    expect(readOverlayEdits(root, "42")).toEqual([
      { contentId: "42", name: "displaytitle", value: "Updated" },
    ]);
  });

  it("saves a single-line text field as one line and keeps long-text breaks", () => {
    const root = document.createElement("div");
    root.innerHTML = `
      <h1 data-perc-field="displaytitle">Welcome</h1>
      <p data-perc-field="notes">A long note</p>
    `;
    const fields = scalarOverlayFields(
      {
        ...payload,
        fields: [
          { name: "displaytitle", value: "Welcome" },
          { name: "notes", value: "A long note" },
        ],
      },
      [
        { name: "displaytitle", control: "sys_EditBox" },
        { name: "notes", control: "sys_TextArea" },
      ],
    );
    applyFieldOverlay(root, fields, "42");
    const title = root.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    const notes = root.querySelector(
      '[data-testid="assembly-inline-field-notes"]',
    ) as HTMLElement;
    expect(title.getAttribute("data-assembly-value")).toBe("text");
    expect(notes.getAttribute("data-assembly-value")).toBe("longtext");
    expect(notes.style.whiteSpace).toBe("pre-wrap");
    title.textContent = "Updated\nwelcome";
    notes.textContent = "Line one\nLine two";
    const edits = readOverlayEdits(root, "42");
    expect(edits.find((edit) => edit.name === "displaytitle")?.value).toBe(
      "Updated welcome",
    );
    expect(edits.find((edit) => edit.name === "notes")?.value).toBe(
      "Line one\nLine two",
    );
    const enter = new KeyboardEvent("keydown", { key: "Enter", bubbles: true, cancelable: true });
    title.dispatchEvent(enter);
    expect(enter.defaultPrevented).toBe(true);
    expect(singleLineText("  a\r\n\nb  ")).toBe("a b");
    expect(singleLineText("bad\u0000value")).toBe("bad\u0000value");
    expect(longTextValue("  a\r\nb  ")).toBe("a\nb");
  });

  it("keeps long-text breaks from br and block elements", () => {
    const root = document.createElement("div");
    root.innerHTML = `<p data-perc-field="notes">A long note</p>`;
    const fields = scalarOverlayFields(
      {
        ...payload,
        fields: [{ name: "notes", value: "A long note" }],
      },
      [{ name: "notes", control: "sys_TextArea" }],
    );
    applyFieldOverlay(root, fields, "42");
    const notes = root.querySelector(
      '[data-testid="assembly-inline-field-notes"]',
    ) as HTMLElement;
    notes.innerHTML = "Line one<br>Line two";
    expect(readOverlayEdits(root, "42")).toEqual([
      { contentId: "42", name: "notes", value: "Line one\nLine two" },
    ]);
    notes.innerHTML = "<div>Line one</div><div>Line two</div>";
    expect(readOverlayEdits(root, "42")[0]?.value).toBe("Line one\nLine two");
    restoreOverlayValues(root, fields);
    expect(notes.textContent).toBe("A long note");
    expect(notes.style.whiteSpace).toBe("pre-wrap");
  });

  it("reads a long-text textarea without collapsing lines", () => {
    const root = document.createElement("div");
    const area = document.createElement("textarea");
    area.setAttribute("data-assembly-field", "notes");
    area.setAttribute("data-assembly-content-id", "42");
    area.setAttribute("data-assembly-value", "longtext");
    area.value = "  Line one\r\nLine two  ";
    root.appendChild(area);
    expect(readOverlayEdits(root, "42")).toEqual([
      { contentId: "42", name: "notes", value: "Line one\nLine two" },
    ]);
  });

  it("drops unchanged fields so one text edit does not rewrite the rest", () => {
    const baseline = new Map<string, string>([
      [overlayEditKey({ contentId: "42", name: "displaytitle" }), "Welcome"],
      [overlayEditKey({ contentId: "42", name: "notes" }), "A long note"],
      [overlayEditKey({ contentId: "42", name: "description" }), "<p>About the site</p>"],
      [overlayEditKey({ contentId: "42", name: "pagelink" }), "//Sites/Example/index"],
    ]);
    const changed = changedOverlayEdits(
      [
        { contentId: "42", name: "displaytitle", value: "Updated welcome" },
        { contentId: "42", name: "notes", value: "A long note" },
        { contentId: "42", name: "description", value: "<p>About the site</p>" },
        {
          contentId: "42",
          name: "pagelink",
          value: "//Sites/Example/index",
          dataType: "link",
        },
      ],
      baseline,
    );
    expect(changed).toEqual([
      { contentId: "42", name: "displaytitle", value: "Updated welcome" },
    ]);
  });

  it("maps data-perc-field markers without leftover AA wrappers", () => {
    const root = document.createElement("div");
    root.innerHTML = `<h1 data-perc-field="sys_title">Home</h1>`;
    const fields = scalarOverlayFields(payload, []);
    const hits = applyFieldOverlay(root, fields, "42");
    expect(hits[0]?.source).toBe("marker");
    expect(
      root.querySelector('[data-testid="assembly-inline-field-sys_title"]'),
    ).toBeTruthy();
  });

  it("maps a unique assembled text value when markers are absent", () => {
    const root = document.createElement("div");
    root.innerHTML = `<p>Welcome</p><p>footer</p>`;
    const fields = scalarOverlayFields(payload, [
      { name: "displaytitle", control: "sys_EditBox" },
    ]);
    const hits = applyFieldOverlay(root, fields, "42");
    expect(hits[0]?.source).toBe("value");
    expect(hits[0]?.name).toBe("displaytitle");
  });

  it("does not guess when the same value appears twice", () => {
    const root = document.createElement("div");
    root.innerHTML = `<p>Welcome</p><h2>Welcome</h2>`;
    const fields = scalarOverlayFields(payload, [
      { name: "displaytitle", control: "sys_EditBox" },
    ]);
    expect(applyFieldOverlay(root, fields, "42")).toEqual([]);
  });

  it("saves assembled HTML markup instead of stripped text", () => {
    const root = document.createElement("div");
    root.innerHTML = `<div class="PsAaField" id='${aaId("42", "description")}'><p>About the site</p></div>`;
    const htmlPayload: ItemEditorFields = {
      ...payload,
      fields: payload.fields.map((field) =>
        field.name === "description"
          ? { name: field.name, value: "<p>About the site</p>" }
          : field,
      ),
    };
    const fields = scalarOverlayFields(htmlPayload, [
      { name: "description", label: "Body", control: "sys_tinymce" },
    ]);
    const hits = applyFieldOverlay(root, fields, "42");
    expect(hits).toHaveLength(1);
    expect(hits[0]?.name).toBe("description");
    const edited = root.querySelector(
      '[data-testid="assembly-inline-field-description"]',
    ) as HTMLElement;
    expect(edited.getAttribute("data-assembly-value")).toBe("html");
    edited.innerHTML = "<p>Updated body</p>";
    expect(readOverlayEdits(root, "42")).toEqual([
      { contentId: "42", name: "description", value: "<p>Updated body</p>" },
    ]);
    restoreOverlayValues(root, fields);
    expect(edited.innerHTML.trim()).toBe("<p>About the site</p>");
  });

  it("keeps a NUL from an HTML text node so the save gate can see it", () => {
    const root = document.createElement("div");
    root.innerHTML = `<div class="PsAaField" id='${aaId("42", "description")}'><p>About the site</p></div>`;
    const htmlPayload: ItemEditorFields = {
      ...payload,
      fields: payload.fields.map((field) =>
        field.name === "description"
          ? { name: field.name, value: "<p>About the site</p>" }
          : field,
      ),
    };
    const fields = scalarOverlayFields(htmlPayload, [
      { name: "description", label: "Body", control: "sys_tinymce" },
    ]);
    applyFieldOverlay(root, fields, "42");
    const edited = root.querySelector(
      '[data-testid="assembly-inline-field-description"]',
    ) as HTMLElement;
    edited.appendChild(document.createTextNode("bad\u0000"));
    const value = readOverlayEdits(root, "42")[0]?.value ?? "";
    expect(htmlContainsNul(value)).toBe(true);
    expect(value).toContain("<p>About the site</p>");
  });

  it("maps a unique HTML block by markup when markers are absent", () => {
    const root = document.createElement("div");
    root.innerHTML = `<section><p>About the site</p></section><p>footer</p>`;
    const htmlPayload: ItemEditorFields = {
      ...payload,
      fields: payload.fields.map((field) =>
        field.name === "description"
          ? { name: field.name, value: "<p>About the site</p>" }
          : field,
      ),
    };
    const fields = scalarOverlayFields(htmlPayload, [
      { name: "description", control: "sys_tinymce" },
    ]);
    const hits = applyFieldOverlay(root, fields, "42");
    expect(hits).toHaveLength(1);
    expect(hits[0]?.source).toBe("value");
    expect(hits[0]?.element.tagName).toBe("SECTION");
  });

  it("clears a previous HTML marker when the field is no longer editable", () => {
    const root = document.createElement("div");
    root.innerHTML = `<div data-perc-field="description"><p>About the site</p></div>`;
    const htmlPayload: ItemEditorFields = {
      ...payload,
      fields: [{ name: "description", value: "<p>About the site</p>" }],
    };
    const editable = scalarOverlayFields(htmlPayload, [
      { name: "description", control: "sys_tinymce" },
    ]);
    applyFieldOverlay(root, editable, "42");
    expect(
      root.querySelector('[data-testid="assembly-inline-field-description"]'),
    ).toBeTruthy();
    const readOnly = scalarOverlayFields(htmlPayload, [
      { name: "description", control: "sys_tinymce", readOnly: true },
    ]);
    expect(applyFieldOverlay(root, readOnly, "42")).toEqual([]);
    expect(
      root.querySelector('[data-testid="assembly-inline-field-description"]'),
    ).toBeNull();
    expect(root.querySelector("[data-assembly-field]")).toBeNull();
  });

  it("edits one assembled link from an anchor href and does not save the label", () => {
    const root = document.createElement("div");
    root.innerHTML = `<a href="//Sites/Example/index">Example</a><p>Welcome</p>`;
    const linkPayload: ItemEditorFields = {
      ...payload,
      fields: [
        ...payload.fields,
        { name: "pagelink", value: "//Sites/Example/index" },
      ],
    };
    const fields = scalarOverlayFields(linkPayload, [
      { name: "pagelink", label: "Page link", control: "sys_PageLink" },
      { name: "displaytitle", control: "sys_EditBox" },
    ]);
    const hits = applyFieldOverlay(root, fields, "42");
    expect(hits.find((h) => h.name === "pagelink")?.source).toBe("value");
    const input = root.querySelector(
      '[data-testid="assembly-inline-field-pagelink"]',
    ) as HTMLInputElement;
    expect(input.tagName).toBe("INPUT");
    expect(input.value).toBe("//Sites/Example/index");
    expect(input.getAttribute("data-assembly-value")).toBe("link");
    const anchor = root.querySelector("a") as HTMLAnchorElement;
    expect(anchor.getAttribute("data-assembly-field")).toBeNull();
    expect(anchor.contentEditable).not.toMatch(/true/i);
    input.value = "//Sites/Example/about";
    expect(readOverlayEdits(root, "42")).toEqual(
      expect.arrayContaining([
        {
          contentId: "42",
          name: "pagelink",
          value: "//Sites/Example/about",
          dataType: "link",
        },
      ]),
    );
    restoreOverlayValues(root, fields);
    expect(input.value).toBe("//Sites/Example/index");
  });

  it("places a link input on a PsAaField wrapper", () => {
    const root = document.createElement("div");
    root.innerHTML = `<div class="PsAaField" id='${aaId("42", "pagelink")}'>//Sites/Example/index</div>`;
    const linkPayload: ItemEditorFields = {
      ...payload,
      fields: [{ name: "pagelink", value: "//Sites/Example/index" }],
    };
    const fields = scalarOverlayFields(linkPayload, [
      { name: "pagelink", label: "Page link", control: "sys_PageLink" },
    ]);
    const hits = applyFieldOverlay(root, fields, "42");
    expect(hits).toHaveLength(1);
    expect(hits[0]?.source).toBe("aa-object-id");
    const input = root.querySelector(
      '[data-testid="assembly-inline-field-pagelink"]',
    ) as HTMLInputElement;
    expect(input.value).toBe("//Sites/Example/index");
    expect(input.parentElement?.classList.contains("PsAaField")).toBe(true);
  });

  it("does not guess when the same link href appears twice", () => {
    const root = document.createElement("div");
    root.innerHTML = `<a href="//Sites/Example/index">A</a><a href="//Sites/Example/index">B</a>`;
    const fields = scalarOverlayFields(
      {
        ...payload,
        fields: [{ name: "pagelink", value: "//Sites/Example/index" }],
      },
      [{ name: "pagelink", control: "sys_PageLink" }],
    );
    expect(applyFieldOverlay(root, fields, "42")).toEqual([]);
    expect(root.querySelector("input")).toBeNull();
  });

  it("clears a previous link input when the field is no longer editable", () => {
    const root = document.createElement("div");
    root.innerHTML = `<a data-perc-field="pagelink" href="//Sites/Example/index">Example</a>`;
    const linkPayload: ItemEditorFields = {
      ...payload,
      fields: [{ name: "pagelink", value: "//Sites/Example/index" }],
    };
    const editable = scalarOverlayFields(linkPayload, [
      { name: "pagelink", control: "sys_PageLink" },
    ]);
    applyFieldOverlay(root, editable, "42");
    expect(
      root.querySelector('[data-testid="assembly-inline-field-pagelink"]'),
    ).toBeTruthy();
    const readOnly = scalarOverlayFields(linkPayload, [
      { name: "pagelink", control: "sys_PageLink", readOnly: true },
    ]);
    expect(applyFieldOverlay(root, readOnly, "42")).toEqual([]);
    expect(root.querySelector("input")).toBeNull();
    expect(root.querySelector("[data-assembly-field]")).toBeNull();
  });

  it("does not guess when the same HTML block appears twice", () => {
    const root = document.createElement("div");
    root.innerHTML = `<section><p>About the site</p></section><div><p>About the site</p></div>`;
    const htmlPayload: ItemEditorFields = {
      ...payload,
      fields: [{ name: "description", value: "<p>About the site</p>" }],
    };
    const fields = scalarOverlayFields(htmlPayload, [
      { name: "description", control: "sys_tinymce" },
    ]);
    expect(applyFieldOverlay(root, fields, "42")).toEqual([]);
  });
});

describe("stripLeftoverAaChrome", () => {
  it("counts removed field images", () => {
    const root = document.createElement("div");
    root.innerHTML = `<img class="PsAaObjectImage" /><span>ok</span>`;
    expect(stripLeftoverAaChrome(root)).toBe(1);
    expect(root.querySelector("img")).toBeNull();
  });
});

describe("mergeOverlayEdits / persistOverlayEdits", () => {
  it("merges only the edited owner fields", () => {
    const next = mergeOverlayEdits(payload, [
      { contentId: "42", name: "displaytitle", value: "Updated" },
      { contentId: "99", name: "sys_title", value: "Snippet" },
    ]);
    expect(next.fields.find((f) => f.name === "displaytitle")?.value).toBe(
      "Updated",
    );
    expect(next.fields.find((f) => f.name === "sys_title")?.value).toBe("Home");
  });

  it("saves owner and snippet items through itemmanagement", async () => {
    const saveFields = vi.fn(async (id: string, body: ItemEditorFields) => body);
    const loadFields = vi.fn(async () => ({
      contentId: "99",
      contentType: "percRichText",
      name: "Snippet",
      checkoutUser: "admin",
      fields: [{ name: "sys_title", value: "Old snippet" }],
    }));
    const checkout = vi.fn(async () => undefined);
    await persistOverlayEdits({
      ownerId: "42",
      ownerPayload: payload,
      edits: [
        { contentId: "42", name: "displaytitle", value: "Updated" },
        { contentId: "99", name: "sys_title", value: "Snippet title" },
      ],
      loadFields,
      saveFields,
      checkout,
    });
    expect(checkout).toHaveBeenCalledWith("99");
    expect(saveFields).toHaveBeenCalledTimes(2);
    const ownerCall = saveFields.mock.calls.find((c) => c[0] === "42");
    expect(
      ownerCall?.[1].fields.find((f) => f.name === "displaytitle")?.value,
    ).toBe("Updated");
    const snippetCall = saveFields.mock.calls.find((c) => c[0] === "99");
    expect(snippetCall?.[1].fields[0]?.value).toBe("Snippet title");
  });

  it("persists HTML markup through the same item field save", async () => {
    const htmlPayload: ItemEditorFields = {
      ...payload,
      fields: [
        { name: "sys_title", value: "Home" },
        { name: "description", value: "<p>About the site</p>" },
      ],
    };
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => body);
    await persistOverlayEdits({
      ownerId: "42",
      ownerPayload: htmlPayload,
      edits: [
        { contentId: "42", name: "description", value: "<p>Updated body</p>" },
      ],
      loadFields: vi.fn(),
      saveFields,
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "description")?.value).toBe(
      "<p>Updated body</p>",
    );
    expect(saved.fields.find((f) => f.name === "sys_title")?.value).toBe("Home");
  });

  it("persists one link and does not stamp dataType onto text or HTML", async () => {
    const mixed: ItemEditorFields = {
      ...payload,
      fields: [
        { name: "displaytitle", value: "Welcome" },
        { name: "description", value: "<p>About the site</p>" },
        { name: "pagelink", value: "//Sites/Example/index" },
      ],
    };
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => body);
    await persistOverlayEdits({
      ownerId: "42",
      ownerPayload: mixed,
      edits: [
        {
          contentId: "42",
          name: "pagelink",
          value: "//Sites/Example/about",
          dataType: "link",
        },
      ],
      loadFields: vi.fn(),
      saveFields,
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "pagelink")).toEqual({
      name: "pagelink",
      value: "//Sites/Example/about",
      dataType: "link",
    });
    expect(saved.fields.find((f) => f.name === "displaytitle")).toEqual({
      name: "displaytitle",
      value: "Welcome",
    });
    expect(saved.fields.find((f) => f.name === "description")?.value).toBe(
      "<p>About the site</p>",
    );
    expect(saved.fields.find((f) => f.name === "description")?.dataType).toBeUndefined();
  });
});

describe("blankRequiredTextFieldNames", () => {
  const fields = [
    { name: "displaytitle", kind: "text" as const, required: true, value: "Welcome" },
    { name: "summary", kind: "text" as const, required: false, value: "Optional" },
    { name: "notes", kind: "longtext" as const, required: true, value: "A long note" },
  ];

  it("names a blank or whitespace required text field and ignores other kinds", () => {
    expect(
      blankRequiredTextFieldNames(fields, [
        { name: "displaytitle", value: "" },
        { name: "summary", value: "" },
        { name: "notes", value: "   " },
      ]),
    ).toEqual(["displaytitle"]);
    expect(
      blankRequiredTextFieldNames(fields, [
        { name: "displaytitle", value: "   " },
      ]),
    ).toEqual(["displaytitle"]);
    expect(
      blankRequiredTextFieldNames(fields, [
        { name: "displaytitle", value: "Updated welcome" },
      ]),
    ).toEqual([]);
  });

  it("marks only the named overlay control invalid", () => {
    const root = document.createElement("div");
    root.innerHTML = `
      <input data-assembly-field="displaytitle" value="Welcome" />
      <h1 data-assembly-field="displaytitle">Welcome</h1>
      <p data-assembly-field="notes">A long note</p>
    `;
    markAssemblyFieldErrors(root, { displaytitle: "This field is required." });
    expect(
      root.querySelector("input")?.getAttribute("aria-invalid"),
    ).toBe("true");
    expect(
      root.querySelector("h1")?.hasAttribute("aria-invalid"),
    ).toBe(false);
    expect(
      root.querySelector('[data-assembly-field="notes"]')?.hasAttribute("aria-invalid"),
    ).toBe(false);
    markAssemblyFieldErrors(root, {});
    expect(
      root.querySelector('[data-assembly-field="displaytitle"]')?.hasAttribute("aria-invalid"),
    ).toBe(false);
  });
});

describe("nulSingleLineTextFieldNames", () => {
  const fields = [
    { name: "displaytitle", kind: "text" as const, value: "Welcome" },
    { name: "summary", kind: "text" as const, value: "Optional" },
    { name: "notes", kind: "longtext" as const, value: "A long note" },
    { name: "description", kind: "html" as const, value: "<p>About</p>" },
    { name: "pagelink", kind: "link" as const, value: "//Sites/Example/index" },
  ];

  it("names a single-line value that contains a NUL and ignores other kinds", () => {
    expect(singleLineTextContainsNul("Updated welcome")).toBe(false);
    expect(singleLineTextContainsNul("")).toBe(false);
    expect(singleLineTextContainsNul("bad\u0000value")).toBe(true);
    expect(
      nulSingleLineTextFieldNames(fields, [
        { name: "displaytitle", value: "bad\u0000value" },
        { name: "summary", value: "ok" },
        { name: "notes", value: "x\u0000y" },
        { name: "description", value: "z\u0000" },
        { name: "pagelink", value: "//Sites/\u0000" },
      ]),
    ).toEqual(["displaytitle"]);
    expect(
      nulSingleLineTextFieldNames(fields, [
        { name: "displaytitle", value: "Updated welcome" },
      ]),
    ).toEqual([]);
  });

  it("uses the loaded value when the field was not edited", () => {
    expect(
      nulSingleLineTextFieldNames(
        [{ name: "displaytitle", kind: "text", value: "bad\u0000stored" }],
        [],
      ),
    ).toEqual(["displaytitle"]);
  });
});

describe("nulLongTextFieldNames", () => {
  const fields = [
    { name: "displaytitle", kind: "text" as const, value: "Welcome" },
    { name: "summary", kind: "text" as const, value: "Optional" },
    { name: "notes", kind: "longtext" as const, value: "Line one\nLine two" },
    { name: "description", kind: "html" as const, value: "<p>About</p>" },
    { name: "pagelink", kind: "link" as const, value: "//Sites/Example/index" },
  ];

  it("names a long-text NUL and ignores line breaks and other kinds", () => {
    expect(longTextContainsNul("Line one\nLine two")).toBe(false);
    expect(longTextContainsNul("")).toBe(false);
    expect(longTextContainsNul("Line one\nbad\u0000value")).toBe(true);
    expect(
      nulLongTextFieldNames(fields, [
        { name: "displaytitle", value: "bad\u0000value" },
        { name: "notes", value: "Line one\nbad\u0000value" },
        { name: "description", value: "z\u0000" },
        { name: "pagelink", value: "//Sites/\u0000" },
      ]),
    ).toEqual(["notes"]);
    expect(
      nulLongTextFieldNames(fields, [
        { name: "notes", value: "Line one\nLine two" },
      ]),
    ).toEqual([]);
    expect(
      nulSingleLineTextFieldNames(fields, [
        { name: "notes", value: "Line one\nbad\u0000value" },
      ]),
    ).toEqual([]);
  });

  it("uses the loaded long text when the field was not edited", () => {
    expect(
      nulLongTextFieldNames(
        [{ name: "notes", kind: "longtext", value: "Line one\nbad\u0000stored" }],
        [],
      ),
    ).toEqual(["notes"]);
  });
});

describe("nulHtmlFieldNames", () => {
  const fields = [
    { name: "displaytitle", kind: "text" as const, value: "Welcome" },
    { name: "summary", kind: "text" as const, value: "Optional" },
    { name: "notes", kind: "longtext" as const, value: "Line one\nLine two" },
    { name: "description", kind: "html" as const, value: "<p>About</p>" },
    { name: "pagelink", kind: "link" as const, value: "//Sites/Example/index" },
  ];

  it("names an HTML NUL and ignores ordinary markup and other kinds", () => {
    expect(htmlContainsNul("<p>About</p>")).toBe(false);
    expect(htmlContainsNul("")).toBe(false);
    expect(htmlContainsNul("<p>bad\u0000value</p>")).toBe(true);
    expect(
      nulHtmlFieldNames(fields, [
        { name: "displaytitle", value: "bad\u0000value" },
        { name: "notes", value: "Line one\nbad\u0000value" },
        { name: "description", value: "<p>bad\u0000value</p>" },
        { name: "pagelink", value: "//Sites/\u0000" },
      ]),
    ).toEqual(["description"]);
    expect(
      nulHtmlFieldNames(fields, [
        { name: "description", value: "<p>Updated body</p>" },
      ]),
    ).toEqual([]);
    expect(
      nulLongTextFieldNames(fields, [
        { name: "description", value: "<p>bad\u0000value</p>" },
      ]),
    ).toEqual([]);
    expect(
      nulSingleLineTextFieldNames(fields, [
        { name: "description", value: "<p>bad\u0000value</p>" },
      ]),
    ).toEqual([]);
  });

  it("uses the loaded HTML when the field was not edited", () => {
    expect(
      nulHtmlFieldNames(
        [{ name: "description", kind: "html", value: "<p>bad\u0000stored</p>" }],
        [],
      ),
    ).toEqual(["description"]);
  });
});

describe("whole number overlay fields", () => {
  const numberPayload: ItemEditorFields = {
    ...payload,
    fields: [
      { name: "qty", value: "12" },
      { name: "rate", value: "1.5" },
      { name: "lockedqty", value: "4" },
      { name: "displaytitle", value: "Welcome" },
    ],
  };

  it("keeps a whole-number field and a float and omits a read-only number", () => {
    const rows = scalarOverlayFields(numberPayload, [
      { name: "qty", label: "Quantity", control: "sys_Number", dataType: "integer" },
      { name: "rate", label: "Rate", control: "sys_Number", dataType: "float" },
      {
        name: "lockedqty",
        label: "Locked quantity",
        control: "sys_Number",
        dataType: "number",
        readOnly: true,
      },
      { name: "displaytitle", control: "sys_EditBox" },
    ]);
    expect(rows.find((row) => row.name === "qty")).toMatchObject({
      kind: "number",
      value: "12",
      label: "Quantity",
      numericFloat: false,
    });
    expect(rows.find((row) => row.name === "rate")).toMatchObject({
      kind: "number",
      value: "1.5",
      label: "Rate",
      numericFloat: true,
    });
    expect(rows.some((row) => row.name === "lockedqty")).toBe(false);
    expect(rows.find((row) => row.name === "displaytitle")?.kind).toBe("text");
  });

  it("reads one assembled whole number and restores the previous value", () => {
    const root = document.createElement("div");
    root.innerHTML = `<span data-perc-field="qty">12</span><h1 data-perc-field="displaytitle">Welcome</h1>`;
    const fields = scalarOverlayFields(numberPayload, [
      { name: "qty", label: "Quantity", control: "sys_Number", dataType: "integer" },
      { name: "displaytitle", control: "sys_EditBox" },
    ]);
    const hits = applyFieldOverlay(root, fields, "42");
    expect(hits.find((hit) => hit.name === "qty")?.source).toBe("marker");
    const qty = root.querySelector(
      '[data-testid="assembly-inline-field-qty"]',
    ) as HTMLElement;
    expect(qty.getAttribute("data-assembly-value")).toBe("number");
    qty.textContent = " 27 ";
    expect(readOverlayEdits(root, "42")).toEqual(
      expect.arrayContaining([
        { contentId: "42", name: "qty", value: "27", dataType: "integer" },
        { contentId: "42", name: "displaytitle", value: "Welcome" },
      ]),
    );
    expect(wholeNumberText("27")).toBe("27");
    expect(wholeNumberText("12.5")).toBeNull();
    expect(wholeNumberText("abc")).toBeNull();
    expect(wholeNumberText("")).toBeNull();
    restoreOverlayValues(root, fields);
    expect(qty.textContent).toBe("12");
  });

  it("names a changed decimal or non-numeric number and ignores an unchanged blank", () => {
    const fields = [
      { name: "qty", kind: "number" as const, value: "12" },
      { name: "optional", kind: "number" as const, value: "" },
      { name: "displaytitle", kind: "text" as const, value: "Welcome" },
    ];
    const baseline = new Map<string, string>([
      ["42\nqty", "12"],
      ["42\noptional", ""],
      ["42\ndisplaytitle", "Welcome"],
    ]);
    expect(
      invalidChangedNumberFieldNames(
        fields,
        [
          { contentId: "42", name: "qty", value: "12.5" },
          { contentId: "42", name: "optional", value: "" },
          { contentId: "42", name: "displaytitle", value: "nope" },
        ],
        baseline,
      ),
    ).toEqual(["qty"]);
    expect(
      invalidChangedNumberFieldNames(
        fields,
        [{ contentId: "42", name: "qty", value: "abc" }],
        baseline,
      ),
    ).toEqual(["qty"]);
    expect(
      invalidChangedNumberFieldNames(
        fields,
        [{ contentId: "42", name: "qty", value: "27" }],
        baseline,
      ),
    ).toEqual([]);
    expect(
      invalidChangedNumberFieldNames(
        fields,
        [{ contentId: "42", name: "qty", value: "" }],
        baseline,
      ),
    ).toEqual(["qty"]);
    expect(
      invalidChangedNumberFieldNames(
        [{ name: "qty", kind: "number", value: "12", required: true }],
        [{ contentId: "42", name: "qty", value: "   " }],
        baseline,
      ),
    ).toEqual([]);
    expect(
      invalidChangedNumberFieldNames(
        [{ name: "qty", kind: "number", value: "12", required: true }],
        [{ contentId: "42", name: "qty", value: "12.5" }],
        baseline,
      ),
    ).toEqual(["qty"]);
  });

  it("names a blank or whitespace required number and ignores optional and other kinds", () => {
    const fields = [
      { name: "qty", kind: "number" as const, required: true, value: "12" },
      { name: "optional", kind: "number" as const, required: false, value: "4" },
      { name: "displaytitle", kind: "text" as const, required: true, value: "Welcome" },
    ];
    expect(
      blankRequiredNumberFieldNames(fields, [
        { name: "qty", value: "" },
        { name: "optional", value: "" },
        { name: "displaytitle", value: "   " },
      ]),
    ).toEqual(["qty"]);
    expect(
      blankRequiredNumberFieldNames(fields, [{ name: "qty", value: "  \n  " }]),
    ).toEqual(["qty"]);
    expect(
      blankRequiredNumberFieldNames(fields, [{ name: "qty", value: "27" }]),
    ).toEqual([]);
    expect(
      blankRequiredNumberFieldNames(fields, [{ name: "qty", value: "12.5" }]),
    ).toEqual([]);
  });

  it("marks a required whole number on the assembled node", () => {
    const root = document.createElement("div");
    root.innerHTML = `<span data-perc-field="qty">12</span>`;
    const fields = scalarOverlayFields(
      {
        ...numberPayload,
        fields: [{ name: "qty", value: "12" }],
      },
      [
        {
          name: "qty",
          label: "Quantity",
          control: "sys_Number",
          dataType: "integer",
          required: true,
        },
      ],
    );
    expect(fields[0]?.required).toBe(true);
    applyFieldOverlay(root, fields, "42");
    const qty = root.querySelector(
      '[data-testid="assembly-inline-field-qty"]',
    ) as HTMLElement;
    expect(qty.getAttribute("data-assembly-required")).toBe("true");
    expect(qty.hasAttribute("aria-required")).toBe(false);
  });

  it("persists one whole number with dataType integer and leaves other fields", async () => {
    const mixed: ItemEditorFields = {
      ...payload,
      fields: [
        { name: "qty", value: "12" },
        { name: "displaytitle", value: "Welcome" },
        { name: "description", value: "<p>About the site</p>" },
      ],
    };
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => body);
    await persistOverlayEdits({
      ownerId: "42",
      ownerPayload: mixed,
      edits: [
        { contentId: "42", name: "qty", value: "27", dataType: "integer" },
      ],
      loadFields: vi.fn(),
      saveFields,
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "qty")).toEqual({
      name: "qty",
      value: "27",
      dataType: "integer",
    });
    expect(saved.fields.find((field) => field.name === "displaytitle")).toEqual({
      name: "displaytitle",
      value: "Welcome",
    });
    expect(saved.fields.find((field) => field.name === "description")?.dataType).toBeUndefined();
  });

  it("reads one assembled decimal and restores the previous float", () => {
    const root = document.createElement("div");
    root.innerHTML = `<span data-perc-field="rate">1</span><span data-perc-field="qty">12</span>`;
    const fields = scalarOverlayFields(
      {
        ...numberPayload,
        fields: [
          { name: "rate", value: "1" },
          { name: "qty", value: "12" },
        ],
      },
      [
        { name: "rate", label: "Rate", control: "sys_Number", dataType: "float" },
        { name: "qty", label: "Quantity", control: "sys_Number", dataType: "integer" },
      ],
    );
    applyFieldOverlay(root, fields, "42");
    const rate = root.querySelector(
      '[data-testid="assembly-inline-field-rate"]',
    ) as HTMLElement;
    const qty = root.querySelector(
      '[data-testid="assembly-inline-field-qty"]',
    ) as HTMLElement;
    expect(rate.getAttribute("data-assembly-number")).toBe("float");
    expect(qty.hasAttribute("data-assembly-number")).toBe(false);
    rate.textContent = " 1.5 ";
    qty.textContent = "1.5";
    expect(readOverlayEdits(root, "42")).toEqual(
      expect.arrayContaining([
        { contentId: "42", name: "rate", value: "1.5", dataType: "float" },
        { contentId: "42", name: "qty", value: "1.5", dataType: "integer" },
      ]),
    );
    expect(decimalNumberText("1.5")).toBe("1.5");
    expect(decimalNumberText("abc")).toBeNull();
    expect(decimalNumberText("")).toBeNull();
    expect(wholeNumberText("1.5")).toBeNull();
    const baseline = new Map<string, string>([
      ["42\nrate", "1"],
      ["42\nqty", "12"],
    ]);
    expect(
      invalidChangedNumberFieldNames(
        fields,
        [
          { contentId: "42", name: "rate", value: "1.5" },
          { contentId: "42", name: "qty", value: "1.5" },
        ],
        baseline,
      ),
    ).toEqual(["qty"]);
    expect(
      invalidChangedNumberFieldNames(
        fields,
        [{ contentId: "42", name: "rate", value: "abc" }],
        baseline,
      ),
    ).toEqual(["rate"]);
    restoreOverlayValues(root, fields);
    expect(rate.textContent).toBe("1");
    expect(qty.textContent).toBe("12");
  });

  it("persists one decimal with dataType float and leaves the whole number", async () => {
    const mixed: ItemEditorFields = {
      ...payload,
      fields: [
        { name: "rate", value: "1" },
        { name: "qty", value: "12" },
        { name: "displaytitle", value: "Welcome" },
      ],
    };
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => body);
    await persistOverlayEdits({
      ownerId: "42",
      ownerPayload: mixed,
      edits: [{ contentId: "42", name: "rate", value: "1.5", dataType: "float" }],
      loadFields: vi.fn(),
      saveFields,
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "rate")).toEqual({
      name: "rate",
      value: "1.5",
      dataType: "float",
    });
    expect(saved.fields.find((field) => field.name === "qty")).toEqual({
      name: "qty",
      value: "12",
    });
    expect(saved.fields.find((field) => field.name === "displaytitle")?.dataType).toBeUndefined();
  });
});

describe("calendar date overlay fields", () => {
  const OLD_DATE = "2026-10-07";
  const NEW_DATE = "2026-11-02";
  const datePayload: ItemEditorFields = {
    ...payload,
    fields: [
      { name: "event_on", value: OLD_DATE },
      { name: "event_at", value: "2026-10-07 15:30:00" },
      { name: "locked_on", value: "2026-01-01" },
      { name: "displaytitle", value: "Welcome" },
    ],
  };

  it("keeps a calendar date and a datetime and omits a read-only date", () => {
    const rows = scalarOverlayFields(datePayload, [
      {
        name: "event_on",
        label: "Event on",
        control: "sys_CalendarSimple",
        dataType: "date",
      },
      {
        name: "event_at",
        label: "Event at",
        control: "sys_CalendarSimple",
        dataType: "datetime",
      },
      {
        name: "locked_on",
        label: "Locked on",
        control: "sys_CalendarSimple",
        dataType: "date",
        readOnly: true,
      },
      { name: "displaytitle", control: "sys_EditBox" },
    ]);
    expect(rows.find((row) => row.name === "event_on")).toMatchObject({
      kind: "date",
      value: OLD_DATE,
      label: "Event on",
    });
    expect(rows.find((row) => row.name === "event_at")).toMatchObject({
      kind: "datetime",
      value: "2026-10-07T15:30",
      label: "Event at",
    });
    expect(rows.some((row) => row.name === "locked_on")).toBe(false);
    expect(rows.find((row) => row.name === "displaytitle")?.kind).toBe("text");
  });

  it("normalizes a midnight datetime stored on a date field to the calendar day", () => {
    const rows = scalarOverlayFields(
      {
        ...payload,
        fields: [{ name: "event_on", value: "2026-10-07 00:00:00" }],
      },
      [{ name: "event_on", control: "sys_CalendarSimple", dataType: "date" }],
    );
    expect(rows[0]?.value).toBe(OLD_DATE);
  });

  it("reads one assembled calendar date and restores the previous value", () => {
    const root = document.createElement("div");
    root.innerHTML = `<span data-perc-field="event_on">${OLD_DATE}</span><h1 data-perc-field="displaytitle">Welcome</h1>`;
    const fields = scalarOverlayFields(datePayload, [
      { name: "event_on", label: "Event on", control: "sys_CalendarSimple", dataType: "date" },
      { name: "displaytitle", control: "sys_EditBox" },
    ]);
    const hits = applyFieldOverlay(root, fields, "42");
    expect(hits.find((hit) => hit.name === "event_on")?.source).toBe("marker");
    const eventOn = root.querySelector(
      '[data-testid="assembly-inline-field-event_on"]',
    ) as HTMLInputElement;
    expect(eventOn.tagName).toBe("INPUT");
    expect(eventOn.type).toBe("date");
    expect(eventOn.getAttribute("data-assembly-value")).toBe("date");
    expect(eventOn.value).toBe(OLD_DATE);
    eventOn.value = NEW_DATE;
    expect(readOverlayEdits(root, "42")).toEqual(
      expect.arrayContaining([
        { contentId: "42", name: "event_on", value: NEW_DATE, dataType: "date" },
        { contentId: "42", name: "displaytitle", value: "Welcome" },
      ]),
    );
    expect(calendarDateText(NEW_DATE)).toBe(NEW_DATE);
    expect(calendarDateText("")).toBeNull();
    expect(calendarDateText("2026-02-31")).toBeNull();
    expect(calendarDateText("2026-10-07 15:30:00")).toBeNull();
    restoreOverlayValues(root, fields);
    expect(eventOn.value).toBe(OLD_DATE);
  });

  it("names a blank or non-date change and ignores an unchanged blank", () => {
    const fields = [
      { name: "event_on", kind: "date" as const, value: OLD_DATE },
      { name: "optional_on", kind: "date" as const, value: "" },
      { name: "displaytitle", kind: "text" as const, value: "Welcome" },
    ];
    const baseline = new Map<string, string>([
      ["42\nevent_on", OLD_DATE],
      ["42\noptional_on", ""],
      ["42\ndisplaytitle", "Welcome"],
    ]);
    expect(
      invalidChangedDateFieldNames(
        fields,
        [
          { contentId: "42", name: "event_on", value: "" },
          { contentId: "42", name: "optional_on", value: "" },
          { contentId: "42", name: "displaytitle", value: "nope" },
        ],
        baseline,
      ),
    ).toEqual(["event_on"]);
    expect(
      invalidChangedDateFieldNames(
        fields,
        [{ contentId: "42", name: "event_on", value: "2026-10-07 15:30:00" }],
        baseline,
      ),
    ).toEqual(["event_on"]);
    expect(
      invalidChangedDateFieldNames(
        fields,
        [{ contentId: "42", name: "event_on", value: NEW_DATE }],
        baseline,
      ),
    ).toEqual([]);
    expect(
      invalidChangedDateFieldNames(
        [{ name: "event_on", kind: "date", value: OLD_DATE, required: true }],
        [{ contentId: "42", name: "event_on", value: "   " }],
        baseline,
      ),
    ).toEqual([]);
    expect(
      invalidChangedDateFieldNames(
        [{ name: "event_on", kind: "date", value: OLD_DATE, required: true }],
        [{ contentId: "42", name: "event_on", value: "2026-10-07 15:30:00" }],
        baseline,
      ),
    ).toEqual(["event_on"]);
  });

  it("names a blank or whitespace required date and ignores optional and other kinds", () => {
    const fields = [
      { name: "event_on", kind: "date" as const, required: true, value: OLD_DATE },
      { name: "optional_on", kind: "date" as const, required: false, value: OLD_DATE },
      { name: "displaytitle", kind: "text" as const, required: true, value: "Welcome" },
    ];
    expect(
      blankRequiredDateFieldNames(fields, [
        { name: "event_on", value: "" },
        { name: "optional_on", value: "" },
        { name: "displaytitle", value: "   " },
      ]),
    ).toEqual(["event_on"]);
    expect(
      blankRequiredDateFieldNames(fields, [{ name: "event_on", value: "  \n  " }]),
    ).toEqual(["event_on"]);
    expect(
      blankRequiredDateFieldNames(fields, [{ name: "event_on", value: NEW_DATE }]),
    ).toEqual([]);
    expect(
      blankRequiredDateFieldNames(fields, [{ name: "event_on", value: "2026-02-31" }]),
    ).toEqual([]);
  });

  it("marks a required calendar date on the assembled node", () => {
    const root = document.createElement("div");
    root.innerHTML = `<span data-perc-field="event_on">${OLD_DATE}</span>`;
    const fields = scalarOverlayFields(
      {
        ...datePayload,
        fields: [{ name: "event_on", value: OLD_DATE }],
      },
      [
        {
          name: "event_on",
          label: "Event on",
          control: "sys_CalendarSimple",
          dataType: "date",
          required: true,
        },
      ],
    );
    expect(fields[0]?.required).toBe(true);
    applyFieldOverlay(root, fields, "42");
    const eventOn = root.querySelector(
      '[data-testid="assembly-inline-field-event_on"]',
    ) as HTMLInputElement;
    expect(eventOn.getAttribute("aria-required")).toBe("true");
    expect(eventOn.getAttribute("data-assembly-required")).toBe("true");
  });

  it("persists one calendar date with dataType date and leaves other fields", async () => {
    const mixed: ItemEditorFields = {
      ...payload,
      fields: [
        { name: "event_on", value: OLD_DATE },
        { name: "displaytitle", value: "Welcome" },
        { name: "notes", value: "A long note" },
      ],
    };
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => body);
    await persistOverlayEdits({
      ownerId: "42",
      ownerPayload: mixed,
      edits: [{ contentId: "42", name: "event_on", value: NEW_DATE, dataType: "date" }],
      loadFields: vi.fn(),
      saveFields,
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "event_on")).toEqual({
      name: "event_on",
      value: NEW_DATE,
      dataType: "date",
    });
    expect(saved.fields.find((field) => field.name === "displaytitle")).toEqual({
      name: "displaytitle",
      value: "Welcome",
    });
    expect(saved.fields.find((field) => field.name === "notes")?.value).toBe("A long note");
    expect(saved.fields.find((field) => field.name === "notes")?.dataType).toBeUndefined();
  });
});

describe("datetime overlay fields", () => {
  const OLD_AT = "2026-10-07 15:30:00";
  const OLD_WIDGET = "2026-10-07T15:30";
  const NEW_AT = "2026-11-02 09:05:00";
  const NEW_WIDGET = "2026-11-02T09:05";
  const datetimePayload: ItemEditorFields = {
    ...payload,
    fields: [
      { name: "event_at", value: OLD_AT },
      { name: "locked_at", value: "2026-01-01 08:00:00" },
      { name: "event_on", value: "2026-10-07" },
      { name: "displaytitle", value: "Welcome" },
    ],
  };
  const datetimeSchema = [
    {
      name: "event_at",
      label: "Event at",
      control: "sys_CalendarSimple",
      dataType: "datetime",
    },
    { name: "displaytitle", control: "sys_EditBox" },
  ];

  it("normalizes a stored datetime to the picker and omits a read-only datetime", () => {
    const rows = scalarOverlayFields(datetimePayload, [
      ...datetimeSchema,
      {
        name: "locked_at",
        label: "Locked at",
        control: "sys_CalendarSimple",
        dataType: "datetime",
        readOnly: true,
      },
      { name: "event_on", control: "sys_CalendarSimple", dataType: "date" },
    ]);
    expect(rows.find((row) => row.name === "event_at")).toMatchObject({
      kind: "datetime",
      value: OLD_WIDGET,
      label: "Event at",
      required: false,
    });
    expect(rows.some((row) => row.name === "locked_at")).toBe(false);
    expect(rows.find((row) => row.name === "event_on")?.kind).toBe("date");
  });

  it("reads one assembled datetime as CMS text and restores the picker", () => {
    const root = document.createElement("div");
    root.innerHTML = `<span data-perc-field="event_at">${OLD_AT}</span><h1 data-perc-field="displaytitle">Welcome</h1>`;
    const fields = scalarOverlayFields(datetimePayload, datetimeSchema);
    const hits = applyFieldOverlay(root, fields, "42");
    expect(hits.find((hit) => hit.name === "event_at")?.source).toBe("marker");
    const eventAt = root.querySelector(
      '[data-testid="assembly-inline-field-event_at"]',
    ) as HTMLInputElement;
    expect(eventAt.tagName).toBe("INPUT");
    expect(eventAt.type).toBe("datetime-local");
    expect(eventAt.getAttribute("data-assembly-value")).toBe("datetime");
    expect(eventAt.value).toBe(OLD_WIDGET);
    eventAt.value = NEW_WIDGET;
    expect(assemblyDatetimeValue(NEW_WIDGET)).toBe(NEW_AT);
    expect(datetimeText(NEW_AT)).toBe(NEW_AT);
    expect(datetimeText("")).toBeNull();
    expect(datetimeText("2026-02-31T09:05")).toBeNull();
    expect(datetimeText("not-a-date")).toBeNull();
    expect(readOverlayEdits(root, "42")).toEqual(
      expect.arrayContaining([
        { contentId: "42", name: "event_at", value: NEW_AT, dataType: "datetime" },
        { contentId: "42", name: "displaytitle", value: "Welcome" },
      ]),
    );
    restoreOverlayValues(root, fields);
    expect(eventAt.value).toBe(OLD_WIDGET);
  });

  it("matches a unique assembled datetime string when there is no marker", () => {
    const root = document.createElement("div");
    root.innerHTML = `<p>${OLD_AT}</p><h1>Welcome</h1>`;
    const fields = scalarOverlayFields(
      {
        ...payload,
        fields: [
          { name: "event_at", value: OLD_AT },
          { name: "displaytitle", value: "Welcome" },
        ],
      },
      datetimeSchema,
    );
    const hits = applyFieldOverlay(root, fields, "42");
    expect(hits.find((hit) => hit.name === "event_at")?.source).toBe("value");
    const eventAt = root.querySelector(
      '[data-testid="assembly-inline-field-event_at"]',
    ) as HTMLInputElement;
    expect(eventAt.value).toBe(OLD_WIDGET);
  });

  it("names an invalid datetime change and allows an optional clear", () => {
    const fields = [
      { name: "event_at", kind: "datetime" as const, value: OLD_WIDGET },
      { name: "optional_at", kind: "datetime" as const, value: "" },
      { name: "displaytitle", kind: "text" as const, value: "Welcome" },
    ];
    const baseline = new Map<string, string>([
      ["42\nevent_at", OLD_AT],
      ["42\noptional_at", ""],
      ["42\ndisplaytitle", "Welcome"],
    ]);
    expect(
      invalidChangedDatetimeFieldNames(
        fields,
        [
          { contentId: "42", name: "event_at", value: "" },
          { contentId: "42", name: "optional_at", value: "" },
          { contentId: "42", name: "displaytitle", value: "nope" },
        ],
        baseline,
      ),
    ).toEqual([]);
    expect(
      invalidChangedDatetimeFieldNames(
        fields,
        [{ contentId: "42", name: "event_at", value: "not-a-date" }],
        baseline,
      ),
    ).toEqual(["event_at"]);
    expect(
      invalidChangedDatetimeFieldNames(
        fields,
        [{ contentId: "42", name: "event_at", value: NEW_AT }],
        baseline,
      ),
    ).toEqual([]);
    expect(
      invalidChangedDatetimeFieldNames(
        [{ name: "event_at", kind: "datetime", value: OLD_WIDGET, required: true }],
        [{ contentId: "42", name: "event_at", value: "   " }],
        baseline,
      ),
    ).toEqual([]);
  });

  it("names a blank or whitespace required datetime and ignores an optional clear", () => {
    const fields = [
      { name: "event_at", kind: "datetime" as const, required: true, value: OLD_WIDGET },
      { name: "optional_at", kind: "datetime" as const, required: false, value: OLD_WIDGET },
      { name: "displaytitle", kind: "text" as const, required: true, value: "Welcome" },
    ];
    expect(
      blankRequiredDatetimeFieldNames(fields, [
        { name: "event_at", value: "" },
        { name: "optional_at", value: "" },
        { name: "displaytitle", value: "   " },
      ]),
    ).toEqual(["event_at"]);
    expect(
      blankRequiredDatetimeFieldNames(fields, [{ name: "event_at", value: "  \n  " }]),
    ).toEqual(["event_at"]);
    expect(
      blankRequiredDatetimeFieldNames(fields, [{ name: "event_at", value: NEW_AT }]),
    ).toEqual([]);
    expect(
      blankRequiredDatetimeFieldNames(fields, [{ name: "event_at", value: "not-a-date" }]),
    ).toEqual([]);
  });

  it("marks a required datetime on the assembled node", () => {
    const root = document.createElement("div");
    root.innerHTML = `<span data-perc-field="event_at">${OLD_AT}</span>`;
    const fields = scalarOverlayFields(
      {
        ...payload,
        fields: [{ name: "event_at", value: OLD_AT }],
      },
      [
        {
          name: "event_at",
          label: "Event at",
          control: "sys_CalendarSimple",
          dataType: "datetime",
          required: true,
        },
      ],
    );
    expect(fields[0]?.required).toBe(true);
    applyFieldOverlay(root, fields, "42");
    const eventAt = root.querySelector(
      '[data-testid="assembly-inline-field-event_at"]',
    ) as HTMLInputElement;
    expect(eventAt.getAttribute("aria-required")).toBe("true");
    expect(eventAt.getAttribute("data-assembly-required")).toBe("true");
  });

  it("persists one datetime with dataType datetime and leaves other fields", async () => {
    const mixed: ItemEditorFields = {
      ...payload,
      fields: [
        { name: "event_at", value: OLD_AT },
        { name: "displaytitle", value: "Welcome" },
        { name: "notes", value: "A long note" },
      ],
    };
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => body);
    await persistOverlayEdits({
      ownerId: "42",
      ownerPayload: mixed,
      edits: [{ contentId: "42", name: "event_at", value: NEW_AT, dataType: "datetime" }],
      loadFields: vi.fn(),
      saveFields,
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "event_at")).toEqual({
      name: "event_at",
      value: NEW_AT,
      dataType: "datetime",
    });
    expect(saved.fields.find((field) => field.name === "displaytitle")).toEqual({
      name: "displaytitle",
      value: "Welcome",
    });
    expect(saved.fields.find((field) => field.name === "notes")?.value).toBe("A long note");
    expect(saved.fields.find((field) => field.name === "notes")?.dataType).toBeUndefined();
  });

  it("persists an optional datetime clear as a blank value", async () => {
    const mixed: ItemEditorFields = {
      ...payload,
      fields: [
        { name: "event_at", value: OLD_AT },
        { name: "displaytitle", value: "Welcome" },
      ],
    };
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => body);
    await persistOverlayEdits({
      ownerId: "42",
      ownerPayload: mixed,
      edits: [{ contentId: "42", name: "event_at", value: "", dataType: "datetime" }],
      loadFields: vi.fn(),
      saveFields,
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "event_at")).toEqual({
      name: "event_at",
      value: "",
      dataType: "datetime",
    });
    expect(saved.fields.find((field) => field.name === "displaytitle")?.value).toBe("Welcome");
  });
});
