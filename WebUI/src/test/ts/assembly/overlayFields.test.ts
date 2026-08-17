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
  mergeOverlayEdits,
  parseAaFieldObjectId,
  persistOverlayEdits,
  readOverlayEdits,
  scalarOverlayFields,
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
  it("keeps scalar text and drops rich / binary kinds", () => {
    const rows = scalarOverlayFields(payload, [
      { name: "sys_title", label: "Title", control: "sys_EditBox" },
      { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
      { name: "description", label: "Body", control: "sys_tinymce" },
    ]);
    expect(rows.map((r) => r.name)).toEqual(["sys_title", "displaytitle"]);
    expect(rows.find((r) => r.name === "displaytitle")?.label).toBe(
      "Display title",
    );
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
    edited.textContent = "Updated";
    expect(readOverlayEdits(root, "42")).toEqual([
      { contentId: "42", name: "displaytitle", value: "Updated" },
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
});
