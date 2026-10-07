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
  scalarOverlayFields,
  singleLineText,
  stripLeftoverAaChrome,
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
