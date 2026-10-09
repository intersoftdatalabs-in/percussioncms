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

import { cleanup, fireEvent, render, screen, waitFor } from "@testing-library/react";
import React from "react";
import { MemoryRouter, Route, Routes } from "react-router";
import { afterEach, describe, expect, it, vi } from "vitest";
import {
  AssemblyHost,
  runSlotDialogWork,
} from "../../../main/ts/assembly/AssemblyHost";
import { AppRoutes } from "../../../main/ts/app/routes";
import type { MenuAction } from "../../../main/ts/api/contentExplorer/types";
import type { ItemEditorFields } from "../../../main/ts/editor/itemFieldsApi";

function renderHost(
  search: string,
  props: React.ComponentProps<typeof AssemblyHost> = {},
): void {
  render(
    <MemoryRouter initialEntries={[`/assembly${search}`]}>
      <Routes>
        <Route path="/assembly" element={<AssemblyHost {...props} />} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("AssemblyHost", () => {
  afterEach(() => {
    cleanup();
    vi.restoreAllMocks();
  });

  it("asks for an item when contentId is missing", () => {
    renderHost("");
    expect(screen.getByTestId("assembly-overlay")).toBeTruthy();
    expect(screen.getByTestId("assembly-error").textContent).toMatch(
      /content item/i,
    );
    expect(screen.queryByTestId("assembly-preview-frame")).toBeNull();
  });

  it("loads the assembled preview for a page/snippet template", async () => {
    const fetchPreview = vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
      contentId: 42,
      templateId: 7,
      revision: 1,
    });
    const loadTemplates = vi.fn().mockResolvedValue([
      {
        name: "rffPgGeneric",
        label: "Generic Page",
        url: "../assembler/render?sys_template=7",
        sortRank: 0,
        menuType: "MENUITEM",
      } satisfies MenuAction,
    ]);
    renderHost("?contentId=42&templateId=7", { fetchPreview, loadTemplates });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-preview-frame")).toBeTruthy();
    });
    expect(fetchPreview).toHaveBeenCalledWith(42, 7);
    expect(screen.getByTestId("assembly-content-id").textContent).toContain("42");
    const frame = screen.getByTestId("assembly-preview-frame");
    expect(frame.getAttribute("src")).toContain("/assembler/render");
    expect(frame.getAttribute("src")).toContain("sys_template=7");
  });

  it("picks the first AA template when the query omits templateId", async () => {
    const fetchPreview = vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_contentid=9&sys_template=3",
      contentId: 9,
      templateId: 3,
      revision: 1,
    });
    const loadTemplates = vi.fn().mockResolvedValue([
      {
        name: "rffSnTitle",
        label: "Title snippet",
        sortRank: 0,
        menuType: "MENUITEM",
        parameters: [{ name: "sys_template", value: "3" }],
      } satisfies MenuAction,
    ]);
    renderHost("?contentId=9", { fetchPreview, loadTemplates });
    await waitFor(() => {
      expect(fetchPreview).toHaveBeenCalledWith(9, 3);
    });
    expect(screen.getByTestId("assembly-template-select")).toBeTruthy();
  });

  it("shows an error when template load fails even with a requested templateId", async () => {
    const fetchPreview = vi.fn();
    const loadTemplates = vi.fn().mockRejectedValue(new Error("catalog down"));
    renderHost("?contentId=42&templateId=7", { fetchPreview, loadTemplates });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-error").textContent).toMatch(
        /no page or snippet template/i,
      );
    });
    expect(fetchPreview).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-preview-frame")).toBeNull();
  });

  it("does not silently fall back when requestedTemplateId is not in options", async () => {
    const fetchPreview = vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_contentid=42&sys_template=3",
      contentId: 42,
      templateId: 3,
      revision: 1,
    });
    const loadTemplates = vi.fn().mockResolvedValue([
      {
        name: "rffSnTitle",
        label: "Title snippet",
        sortRank: 0,
        menuType: "MENUITEM",
        parameters: [{ name: "sys_template", value: "3" }],
      } satisfies MenuAction,
    ]);
    renderHost("?contentId=42&templateId=99", { fetchPreview, loadTemplates });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-error").textContent).toMatch(
        /not in the available list/i,
      );
    });
    expect(fetchPreview).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-preview-frame")).toBeNull();
    expect(screen.getByTestId("assembly-requested-template").textContent).toContain(
      "99",
    );
  });

  it("AppRoutes mounts assembly outside AppLayout", () => {
    render(
      <MemoryRouter initialEntries={["/assembly"]}>
        <AppRoutes />
      </MemoryRouter>,
    );
    expect(screen.getByTestId("assembly-host")).toBeTruthy();
    expect(screen.queryByTestId("perc-spa-app")).toBeNull();
  });

  it("renders slot add/create/arrange once a slot is selected", async () => {
    const fetchPreview = vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
      contentId: 42,
      templateId: 7,
      revision: 1,
    });
    const loadTemplates = vi.fn().mockResolvedValue([
      {
        name: "rffPgGeneric",
        label: "Generic Page",
        url: "../assembler/render?sys_template=7",
        sortRank: 0,
        menuType: "MENUITEM",
      } satisfies MenuAction,
    ]);
    const loadCanvas = vi.fn().mockResolvedValue({
      ownerId: 42,
      templateId: 7,
      slots: [
        {
          slotId: 3,
          name: "sidebar",
          label: "Sidebar",
          items: [
            {
              relationshipId: 88,
              ownerId: 42,
              dependentId: 7,
              slotId: 3,
              templateId: 4,
              sortRank: 0,
            },
          ],
        },
      ],
    });
    const removeSlotRel = vi.fn().mockResolvedValue(undefined);
    const moveSlotRel = vi.fn().mockResolvedValue(undefined);
    vi.spyOn(window, "confirm").mockReturnValue(true);
    renderHost("?contentId=42&templateId=7", {
      fetchPreview,
      loadTemplates,
      loadCanvas,
      removeSlotRel,
      moveSlotRel,
    });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-3")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("assembly-slot-3"));
    expect(screen.getByTestId("assembly-slot-add")).not.toHaveProperty(
      "disabled",
      true,
    );
    expect(screen.getByTestId("assembly-slot-move-up")).toHaveProperty(
      "disabled",
      true,
    );
    fireEvent.click(screen.getByTestId("assembly-slot-item-88"));
    expect(screen.getByTestId("assembly-slot-move-up")).not.toHaveProperty(
      "disabled",
      true,
    );
    fireEvent.click(screen.getByTestId("assembly-slot-move-up"));
    await waitFor(() => {
      expect(moveSlotRel).toHaveBeenCalledWith(88, "UP");
    });
    fireEvent.click(screen.getByTestId("assembly-slot-move-down"));
    await waitFor(() => {
      expect(moveSlotRel).toHaveBeenCalledWith(88, "DOWN");
    });
    fireEvent.click(screen.getByTestId("assembly-slot-remove"));
    await waitFor(() => {
      expect(removeSlotRel).toHaveBeenCalledWith(88);
    });
  });

  it("runSlotDialogWork reports failures instead of swallowing them", async () => {
    const onFail = vi.fn();
    await runSlotDialogWork(async () => {
      throw new Error("add failed");
    }, onFail);
    expect(onFail).toHaveBeenCalledTimes(1);
    const ok = vi.fn();
    await runSlotDialogWork(async () => undefined, ok);
    expect(ok).not.toHaveBeenCalled();
  });

  it("shows a slot notice when create or change dialog apply rejects", async () => {
    const fetchPreview = vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
      contentId: 42,
      templateId: 7,
      revision: 1,
    });
    const loadTemplates = vi.fn().mockResolvedValue([
      {
        name: "rffPgGeneric",
        label: "Generic Page",
        url: "../assembler/render?sys_template=7",
        sortRank: 0,
        menuType: "MENUITEM",
      } satisfies MenuAction,
    ]);
    const loadCanvas = vi.fn().mockResolvedValue({
      ownerId: 42,
      templateId: 7,
      slots: [
        {
          slotId: 3,
          name: "sidebar",
          label: "Sidebar",
          items: [
            {
              relationshipId: 88,
              ownerId: 42,
              dependentId: 7,
              slotId: 3,
              templateId: 4,
              sortRank: 0,
            },
          ],
        },
      ],
    });
    const loadAllowedTypes = vi.fn().mockResolvedValue([
      { id: 1, name: "percRichText", label: "Rich Text" },
    ]);
    const loadAllowedTemplates = vi.fn().mockResolvedValue([
      { id: 4, name: "rffSnTitle", label: "Title" },
    ]);
    const createItem = vi.fn().mockRejectedValue(new Error("create failed"));
    const changeSlotTemplate = vi.fn().mockRejectedValue(new Error("change failed"));
    renderHost("?contentId=42&templateId=7", {
      fetchPreview,
      loadTemplates,
      loadCanvas,
      loadAllowedTypes,
      loadAllowedTemplates,
      createItem,
      changeSlotTemplate,
    });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-3")).toBeTruthy();
    });
    fireEvent.click(screen.getByTestId("assembly-slot-3"));
    fireEvent.click(screen.getByTestId("assembly-slot-create"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-create-dialog")).toBeTruthy();
      expect(
        (screen.getByTestId("assembly-slot-create-type") as HTMLSelectElement)
          .value,
      ).toBe("percRichText");
    });
    fireEvent.change(screen.getByTestId("assembly-slot-create-folder"), {
      target: { value: "/Sites/Demo" },
    });
    fireEvent.click(screen.getByTestId("assembly-slot-create-apply"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-notice").textContent).toMatch(
        /could not update the slot/i,
      );
    });
    expect(screen.queryByTestId("assembly-slot-create-dialog")).toBeNull();

    fireEvent.click(screen.getByTestId("assembly-slot-item-88"));
    fireEvent.click(screen.getByTestId("assembly-slot-change"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-change-dialog")).toBeTruthy();
      expect(
        (screen.getByTestId("assembly-slot-change-template") as HTMLSelectElement)
          .value,
      ).toBe("4");
    });
    fireEvent.click(screen.getByTestId("assembly-slot-change-apply"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-slot-notice").textContent).toMatch(
        /could not update the slot/i,
      );
    });
    expect(screen.queryByTestId("assembly-slot-change-dialog")).toBeNull();
    expect(changeSlotTemplate).toHaveBeenCalledWith(88, 3, 4);
  });

  const pageFields: ItemEditorFields = {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [
      { name: "sys_title", value: "Home" },
      { name: "displaytitle", value: "Welcome" },
    ],
  };

  it("edits known scalar fields on the assembled preview and saves via itemmanagement", async () => {
    const previewDoc = document.implementation.createHTMLDocument("preview");
    previewDoc.body.innerHTML = `
      <a href="javascript:void(0)"><img class="PsAaObjectImage" src="field.gif" /></a>
      <div class="PsAaField" id='[3,42,7,0,0,0,0,1,0,0,0,"displaytitle",42,"Display",0]'>Welcome</div>
    `;
    const fetchPreview = vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
      contentId: 42,
      templateId: 7,
      revision: 1,
    });
    const loadTemplates = vi.fn().mockResolvedValue([
      {
        name: "rffPgGeneric",
        label: "Generic Page",
        url: "../assembler/render?sys_template=7",
        sortRank: 0,
        menuType: "MENUITEM",
      } satisfies MenuAction,
    ]);
    const checkout = vi.fn().mockResolvedValue(undefined);
    const loadFields = vi.fn().mockResolvedValue(pageFields);
    const saveFields = vi.fn().mockResolvedValue({
      ...pageFields,
      fields: [
        { name: "sys_title", value: "Home" },
        { name: "displaytitle", value: "Updated inline" },
      ],
    });
    const loadType = vi.fn().mockResolvedValue({
      fields: [
        { name: "sys_title", label: "Title", control: "sys_EditBox" },
        { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
      ],
    });
    renderHost("?contentId=42&templateId=7", {
      fetchPreview,
      loadTemplates,
      checkout,
      loadFields,
      saveFields,
      loadType,
      getPreviewDocument: () => previewDoc,
    });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-chip-displaytitle")).toBeTruthy();
    });
    expect(checkout).toHaveBeenCalledWith("42");
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-displaytitle"]'),
      ).toBeTruthy();
    });
    expect(previewDoc.querySelector("img.PsAaObjectImage")).toBeNull();
    const inline = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    inline.textContent = "Updated inline";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "displaytitle")?.value).toBe(
      "Updated inline",
    );
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(
      /saved/i,
    );
  });

  it("falls back to the overlay field strip when the assembled page has no markers", async () => {
    const fetchPreview = vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
      contentId: 42,
      templateId: 7,
      revision: 1,
    });
    const loadTemplates = vi.fn().mockResolvedValue([
      {
        name: "rffPgGeneric",
        label: "Generic Page",
        url: "../assembler/render?sys_template=7",
        sortRank: 0,
        menuType: "MENUITEM",
      } satisfies MenuAction,
    ]);
    const saveFields = vi.fn().mockResolvedValue(pageFields);
    renderHost("?contentId=42&templateId=7", {
      fetchPreview,
      loadTemplates,
      checkout: vi.fn().mockResolvedValue(undefined),
      loadFields: vi.fn().mockResolvedValue(pageFields),
      saveFields,
      loadType: vi.fn().mockResolvedValue({
        fields: [{ name: "sys_title", label: "Title", control: "sys_EditBox" }],
      }),
      getPreviewDocument: () => document.implementation.createHTMLDocument("empty"),
    });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-overlay-field-sys_title")).toBeTruthy();
    });
    const titleInput = screen.getByTestId(
      "assembly-overlay-field-sys_title",
    ) as HTMLInputElement;
    expect(titleInput.tagName).toBe("INPUT");
    titleInput.value = "Renamed";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "sys_title")?.value).toBe("Renamed");
  });

  const htmlFields: ItemEditorFields = {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [
      { name: "displaytitle", value: "Welcome" },
      { name: "description", value: "<p>About the site</p>" },
    ],
  };

  const htmlSchema = {
    fields: [
      { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
      { name: "description", label: "Body", control: "sys_tinymce" },
    ],
  };

  function htmlPreviewDoc(): Document {
    const previewDoc = document.implementation.createHTMLDocument("preview");
    previewDoc.body.innerHTML = `
      <div class="PsAaField" id='[3,42,7,0,0,0,0,1,0,0,0,"description",42,"Body",0]'><p>About the site</p></div>
      <h1 data-perc-field="displaytitle">Welcome</h1>
    `;
    return previewDoc;
  }

  function renderHtmlHost(
    previewDoc: Document,
    saveFields: ReturnType<typeof vi.fn>,
    loadFields: ReturnType<typeof vi.fn> = vi.fn().mockResolvedValue(htmlFields),
    schema: { fields: Array<Record<string, unknown>> } = htmlSchema,
  ): void {
    renderHost("?contentId=42&templateId=7", {
      fetchPreview: vi.fn().mockResolvedValue({
        previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
        contentId: 42,
        templateId: 7,
        revision: 1,
      }),
      loadTemplates: vi.fn().mockResolvedValue([
        {
          name: "rffPgGeneric",
          label: "Generic Page",
          url: "../assembler/render?sys_template=7",
          sortRank: 0,
          menuType: "MENUITEM",
        } satisfies MenuAction,
      ]),
      checkout: vi.fn().mockResolvedValue(undefined),
      loadFields,
      saveFields,
      loadType: vi.fn().mockResolvedValue(schema),
      getPreviewDocument: () => previewDoc,
    });
  }

  it("saves one assembled HTML field through itemmanagement without stripping markup", async () => {
    const previewDoc = htmlPreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderHtmlHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-description"]'),
      ).toBeTruthy();
    });
    const inline = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-description"]',
    ) as HTMLElement;
    inline.innerHTML = "<p>Updated body</p>";
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "description")?.value).toBe(
      "<p>Updated body</p>",
    );
    expect(saved.fields.find((f) => f.name === "displaytitle")?.value).toBe(
      "Welcome",
    );
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
  });

  it("reloads the assembly host with the HTML that was saved", async () => {
    const savedHtml = "<p>Updated body</p>";
    const savedPayload: ItemEditorFields = {
      ...htmlFields,
      fields: [
        { name: "displaytitle", value: "Welcome" },
        { name: "description", value: savedHtml },
      ],
    };
    const previewDoc = document.implementation.createHTMLDocument("preview");
    previewDoc.body.innerHTML = `
      <div class="PsAaField" id='[3,42,7,0,0,0,0,1,0,0,0,"description",42,"Body",0]'>${savedHtml}</div>
    `;
    renderHtmlHost(
      previewDoc,
      vi.fn().mockResolvedValue(savedPayload),
      vi.fn().mockResolvedValue(savedPayload),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-description"]'),
      ).toBeTruthy();
    });
    const inline = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-description"]',
    ) as HTMLElement;
    expect(inline.innerHTML.trim()).toBe(savedHtml);
  });

  it("does not write a read-only HTML field", async () => {
    const previewDoc = htmlPreviewDoc();
    const saveFields = vi.fn().mockResolvedValue(htmlFields);
    renderHtmlHost(previewDoc, saveFields, vi.fn().mockResolvedValue(htmlFields), {
      fields: [
        { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
        {
          name: "description",
          label: "Body",
          control: "sys_tinymce",
          readOnly: true,
        },
      ],
    });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-inline-displaytitle")).toBeTruthy();
    });
    expect(screen.queryByTestId("assembly-field-chip-description")).toBeNull();
    expect(
      previewDoc.querySelector('[data-testid="assembly-inline-field-description"]'),
    ).toBeNull();
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    title.textContent = "Renamed";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "description")?.value).toBe(
      "<p>About the site</p>",
    );
    expect(saved.fields.find((f) => f.name === "displaytitle")?.value).toBe(
      "Renamed",
    );
  });

  it.each([400, 403, 409])(
    "HTTP %s leaves the previous HTML in place",
    async (status) => {
      const previewDoc = htmlPreviewDoc();
      const saveFields = vi.fn().mockRejectedValue({ status });
      renderHtmlHost(previewDoc, saveFields);
      await waitFor(() => {
        expect(
          previewDoc.querySelector('[data-testid="assembly-inline-field-description"]'),
        ).toBeTruthy();
      });
      const inline = previewDoc.querySelector(
        '[data-testid="assembly-inline-field-description"]',
      ) as HTMLElement;
      inline.innerHTML = "<p>Should not stick</p>";
      fireEvent.click(screen.getByTestId("assembly-field-save"));
      await waitFor(() => {
        expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(
          /could not save/i,
        );
      });
      expect(inline.innerHTML.trim()).toBe("<p>About the site</p>");
      expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
        /fields saved/i,
      );
    },
  );

  it("edits HTML from the overlay strip when the page has no marker and still saves markup", async () => {
    const previewDoc = document.implementation.createHTMLDocument("empty");
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderHtmlHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(screen.getByTestId("assembly-overlay-field-description")).toBeTruthy();
    });
    const area = screen.getByTestId(
      "assembly-overlay-field-description",
    ) as HTMLTextAreaElement;
    expect(area.tagName).toBe("TEXTAREA");
    area.value = "<p>From the strip</p>";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "description")?.value).toBe(
      "<p>From the strip</p>",
    );
  });

  const PREV_HTML = "<p>About the site</p>";

  function descriptionPayload(html: string): ItemEditorFields {
    return {
      ...htmlFields,
      fields: htmlFields.fields.map((field) =>
        field.name === "description" ? { ...field, value: html } : field,
      ),
    };
  }

  it("refuses an HTML NUL before save and reloads the previous markup", async () => {
    const previewDoc = htmlPreviewDoc();
    const saveFields = vi.fn();
    renderHtmlHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(descriptionPayload(PREV_HTML)),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-description"]'),
      ).toBeTruthy();
    });
    const body = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-description"]',
    ) as HTMLElement;
    expect(body.innerHTML.trim()).toBe(PREV_HTML);
    expect(body.getAttribute("data-assembly-value")).toBe("html");
    body.appendChild(previewDoc.createTextNode("bad\u0000"));
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-description").textContent).toMatch(
        /HTML contains a character that cannot be saved/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(
      /HTML contains a character that cannot be saved/i,
    );
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
    expect(screen.queryByTestId("assembly-field-error-displaytitle")).toBeNull();
    expect(body.hasAttribute("aria-invalid")).toBe(false);
    expect(body.textContent).toContain("\u0000");
    cleanup();
    const reloaded = htmlPreviewDoc();
    renderHtmlHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue(descriptionPayload(PREV_HTML)),
    );
    await waitFor(() => {
      const live = reloaded.querySelector(
        '[data-testid="assembly-inline-field-description"]',
      ) as HTMLElement | null;
      expect(live?.innerHTML.trim()).toBe(PREV_HTML);
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
  });

  it("does not write when Cancel leaves an HTML NUL edit", async () => {
    const previewDoc = htmlPreviewDoc();
    const saveFields = vi.fn();
    renderHtmlHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(descriptionPayload(PREV_HTML)),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-description"]'),
      ).toBeTruthy();
    });
    const body = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-description"]',
    ) as HTMLElement;
    body.appendChild(previewDoc.createTextNode("bad\u0000"));
    fireEvent.click(screen.getByTestId("assembly-field-cancel"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
    expect(body.innerHTML.trim()).toBe(PREV_HTML);
    expect(body.textContent).not.toContain("\u0000");
    expect(screen.queryByTestId("assembly-field-error-description")).toBeNull();
  });

  it("still saves ordinary HTML without a NUL", async () => {
    let htmlValue = PREV_HTML;
    const next = "<p>Updated body</p>";
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => {
      htmlValue = body.fields.find((field) => field.name === "description")?.value ?? htmlValue;
      return descriptionPayload(htmlValue);
    });
    const previewDoc = htmlPreviewDoc();
    renderHtmlHost(
      previewDoc,
      saveFields,
      vi.fn(async () => descriptionPayload(htmlValue)),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-description"]'),
      ).toBeTruthy();
    });
    const body = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-description"]',
    ) as HTMLElement;
    body.innerHTML = next;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    });
    const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(sent.fields.find((field) => field.name === "description")?.value).toBe(next);
    expect(String(sent.fields.find((field) => field.name === "description")?.value)).not.toContain(
      "\u0000",
    );
    expect(sent.fields.find((field) => field.name === "displaytitle")?.value).toBe("Welcome");
    expect(screen.queryByTestId("assembly-field-error-description")).toBeNull();
    cleanup();
    const reloaded = document.implementation.createHTMLDocument("preview");
    reloaded.body.innerHTML = `
      <div class="PsAaField" id='[3,42,7,0,0,0,0,1,0,0,0,"description",42,"Body",0]'>${next}</div>
      <h1 data-perc-field="displaytitle">Welcome</h1>
    `;
    renderHtmlHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue(descriptionPayload(next)),
    );
    await waitFor(() => {
      const live = reloaded.querySelector(
        '[data-testid="assembly-inline-field-description"]',
      ) as HTMLElement | null;
      expect(live?.innerHTML.trim()).toBe(next);
    });
  });

  it("refuses an HTML NUL on the overlay strip", async () => {
    const previewDoc = document.implementation.createHTMLDocument("empty");
    const saveFields = vi.fn();
    renderHtmlHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(descriptionPayload(PREV_HTML)),
    );
    await waitFor(() => {
      expect(screen.getByTestId("assembly-overlay-field-description")).toBeTruthy();
    });
    const area = screen.getByTestId(
      "assembly-overlay-field-description",
    ) as HTMLTextAreaElement;
    expect(area.value).toBe(PREV_HTML);
    area.value = "<p>bad\u0000value</p>";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-description").textContent).toMatch(
        /HTML contains a character that cannot be saved/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(area.getAttribute("aria-invalid")).toBe("true");
    expect(area.value).toContain("\u0000");
    expect(area.value).toContain("<p>");
  });

  const OLD_LINK = "//Sites/Example/index";
  const NEW_LINK = "//Sites/Example/about";

  const linkFields: ItemEditorFields = {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [
      { name: "displaytitle", value: "Welcome" },
      { name: "description", value: "<p>About the site</p>" },
      { name: "pagelink", value: OLD_LINK },
    ],
  };

  const linkSchema = {
    fields: [
      { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
      { name: "description", label: "Body", control: "sys_tinymce" },
      { name: "pagelink", label: "Page link", control: "sys_PageLink" },
    ],
  };

  function linkPreviewDoc(link = OLD_LINK): Document {
    const previewDoc = document.implementation.createHTMLDocument("preview");
    previewDoc.body.innerHTML = `
      <h1 data-perc-field="displaytitle">Welcome</h1>
      <div class="PsAaField" id='[3,42,7,0,0,0,0,1,0,0,0,"description",42,"Body",0]'><p>About the site</p></div>
      <a data-perc-field="pagelink" href="${link}">Example</a>
    `;
    return previewDoc;
  }

  function renderLinkHost(
    previewDoc: Document,
    saveFields: ReturnType<typeof vi.fn>,
    loadFields: ReturnType<typeof vi.fn> = vi.fn().mockResolvedValue(linkFields),
    schema: { fields: Array<Record<string, unknown>> } = linkSchema,
  ): void {
    renderHost("?contentId=42&templateId=7", {
      fetchPreview: vi.fn().mockResolvedValue({
        previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
        contentId: 42,
        templateId: 7,
        revision: 1,
      }),
      loadTemplates: vi.fn().mockResolvedValue([
        {
          name: "rffPgGeneric",
          label: "Generic Page",
          url: "../assembler/render?sys_template=7",
          sortRank: 0,
          menuType: "MENUITEM",
        } satisfies MenuAction,
      ]),
      checkout: vi.fn().mockResolvedValue(undefined),
      loadFields,
      saveFields,
      loadType: vi.fn().mockResolvedValue(schema),
      getPreviewDocument: () => previewDoc,
    });
  }

  it("saves one assembled link and does not write before Save", async () => {
    const previewDoc = linkPreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderLinkHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-pagelink"]'),
      ).toBeTruthy();
    });
    const input = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-pagelink"]',
    ) as HTMLInputElement;
    expect(input.value).toBe(OLD_LINK);
    input.value = NEW_LINK;
    expect(saveFields).not.toHaveBeenCalled();
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    title.textContent = "Renamed";
    const html = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-description"]',
    ) as HTMLElement;
    html.innerHTML = "<p>Updated body</p>";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "pagelink")).toEqual({
      name: "pagelink",
      value: NEW_LINK,
      dataType: "link",
    });
    expect(saved.fields.find((f) => f.name === "displaytitle")?.value).toBe("Renamed");
    expect(saved.fields.find((f) => f.name === "description")?.value).toBe(
      "<p>Updated body</p>",
    );
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    expect(screen.getByTestId("assembly-field-inline-pagelink")).toBeTruthy();
  });

  it("reloads the assembly host with the link that was saved", async () => {
    const savedPayload: ItemEditorFields = {
      ...linkFields,
      fields: linkFields.fields.map((field) =>
        field.name === "pagelink" ? { ...field, value: NEW_LINK } : field,
      ),
    };
    const previewDoc = linkPreviewDoc(NEW_LINK);
    renderLinkHost(
      previewDoc,
      vi.fn().mockResolvedValue(savedPayload),
      vi.fn().mockResolvedValue(savedPayload),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-pagelink"]'),
      ).toBeTruthy();
    });
    const input = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-pagelink"]',
    ) as HTMLInputElement;
    expect(input.value).toBe(NEW_LINK);
    expect(previewDoc.querySelector("a")?.getAttribute("href")).toBe(NEW_LINK);
  });

  it("does not write a read-only link field", async () => {
    const previewDoc = linkPreviewDoc();
    const saveFields = vi.fn().mockResolvedValue(linkFields);
    renderLinkHost(previewDoc, saveFields, vi.fn().mockResolvedValue(linkFields), {
      fields: [
        { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
        { name: "description", label: "Body", control: "sys_tinymce" },
        {
          name: "pagelink",
          label: "Page link",
          control: "sys_PageLink",
          readOnly: true,
        },
      ],
    });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-inline-displaytitle")).toBeTruthy();
    });
    expect(screen.queryByTestId("assembly-field-chip-pagelink")).toBeNull();
    expect(
      previewDoc.querySelector('[data-testid="assembly-inline-field-pagelink"]'),
    ).toBeNull();
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    title.textContent = "Renamed";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "pagelink")?.value).toBe(OLD_LINK);
    expect(saved.fields.find((f) => f.name === "pagelink")?.dataType).toBeUndefined();
    expect(saved.fields.find((f) => f.name === "displaytitle")?.value).toBe("Renamed");
    expect(saved.fields.find((f) => f.name === "description")?.value).toBe(
      "<p>About the site</p>",
    );
  });

  it.each([400, 403, 409])(
    "HTTP %s leaves the previous link in place",
    async (status) => {
      const previewDoc = linkPreviewDoc();
      const saveFields = vi.fn().mockRejectedValue({ status });
      renderLinkHost(previewDoc, saveFields);
      await waitFor(() => {
        expect(
          previewDoc.querySelector('[data-testid="assembly-inline-field-pagelink"]'),
        ).toBeTruthy();
      });
      const input = previewDoc.querySelector(
        '[data-testid="assembly-inline-field-pagelink"]',
      ) as HTMLInputElement;
      input.value = NEW_LINK;
      fireEvent.click(screen.getByTestId("assembly-field-save"));
      // A late preview load repaints the link input. Assert the connected node,
      // not the input that existed before Save.
      await waitFor(() => {
        expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(
          /could not save/i,
        );
        const live = previewDoc.querySelector(
          '[data-testid="assembly-inline-field-pagelink"]',
        ) as HTMLInputElement | null;
        expect(live?.isConnected).toBe(true);
        expect(live?.value).toBe(OLD_LINK);
      });
      expect(saveFields).toHaveBeenCalled();
      const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
      expect(sent.fields.find((field) => field.name === "pagelink")?.value).toBe(
        NEW_LINK,
      );
      expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
        /fields saved/i,
      );
    },
  );

  function linkPayload(link: string): ItemEditorFields {
    return {
      ...linkFields,
      fields: linkFields.fields.map((field) =>
        field.name === "pagelink" ? { ...field, value: link } : field,
      ),
    };
  }

  it("refuses a link NUL before save and reloads the previous link", async () => {
    const previewDoc = linkPreviewDoc();
    const saveFields = vi.fn();
    renderLinkHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(linkPayload(OLD_LINK)),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-pagelink"]'),
      ).toBeTruthy();
    });
    const input = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-pagelink"]',
    ) as HTMLInputElement;
    expect(input.value).toBe(OLD_LINK);
    expect(input.getAttribute("data-assembly-value")).toBe("link");
    input.value = "594\u0000";
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-pagelink").textContent).toMatch(
        /link contains a character that cannot be saved/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(
      /link contains a character that cannot be saved/i,
    );
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
      /HTML contains a character/i,
    );
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
    expect(screen.queryByTestId("assembly-field-error-description")).toBeNull();
    expect(screen.queryByTestId("assembly-field-error-displaytitle")).toBeNull();
    expect(input.getAttribute("aria-invalid")).toBe("true");
    expect(input.value).toContain("\u0000");
    cleanup();
    const reloaded = linkPreviewDoc();
    renderLinkHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue(linkPayload(OLD_LINK)),
    );
    await waitFor(() => {
      const live = reloaded.querySelector(
        '[data-testid="assembly-inline-field-pagelink"]',
      ) as HTMLInputElement | null;
      expect(live?.value).toBe(OLD_LINK);
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
  });

  it("does not write when Cancel leaves a link NUL edit", async () => {
    const previewDoc = linkPreviewDoc();
    const saveFields = vi.fn();
    renderLinkHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(linkPayload(OLD_LINK)),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-pagelink"]'),
      ).toBeTruthy();
    });
    const input = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-pagelink"]',
    ) as HTMLInputElement;
    input.value = `${OLD_LINK}\u0000`;
    fireEvent.click(screen.getByTestId("assembly-field-cancel"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
    expect(input.value).toBe(OLD_LINK);
    expect(input.value).not.toContain("\u0000");
    expect(screen.queryByTestId("assembly-field-error-pagelink")).toBeNull();
  });

  it.each([
    ["content id", "594"],
    ["GUID", "0-101-594"],
    ["folder path", "/Sites/Example/index"],
  ])("still saves a %s without a NUL", async (_label, next) => {
    let linkValue = OLD_LINK;
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => {
      linkValue = body.fields.find((field) => field.name === "pagelink")?.value ?? linkValue;
      return linkPayload(linkValue);
    });
    const previewDoc = linkPreviewDoc();
    renderLinkHost(
      previewDoc,
      saveFields,
      vi.fn(async () => linkPayload(linkValue)),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-pagelink"]'),
      ).toBeTruthy();
    });
    const input = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-pagelink"]',
    ) as HTMLInputElement;
    input.value = next;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    });
    const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(sent.fields.find((field) => field.name === "pagelink")).toEqual({
      name: "pagelink",
      value: next,
      dataType: "link",
    });
    expect(String(sent.fields.find((field) => field.name === "pagelink")?.value)).not.toContain(
      "\u0000",
    );
    expect(sent.fields.find((field) => field.name === "description")?.value).toBe(
      "<p>About the site</p>",
    );
    expect(screen.queryByTestId("assembly-field-error-pagelink")).toBeNull();
    expect(screen.queryByTestId("assembly-field-error-description")).toBeNull();
    cleanup();
    const reloaded = linkPreviewDoc(next);
    renderLinkHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue(linkPayload(next)),
    );
    await waitFor(() => {
      const live = reloaded.querySelector(
        '[data-testid="assembly-inline-field-pagelink"]',
      ) as HTMLInputElement | null;
      expect(live?.value).toBe(next);
    });
  });

  it("keeps the HTML NUL gate when the link has no NUL", async () => {
    const previewDoc = linkPreviewDoc();
    const saveFields = vi.fn();
    renderLinkHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(linkPayload(OLD_LINK)),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-description"]'),
      ).toBeTruthy();
    });
    const body = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-description"]',
    ) as HTMLElement;
    body.appendChild(previewDoc.createTextNode("bad\u0000"));
    const input = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-pagelink"]',
    ) as HTMLInputElement;
    expect(input.value).toBe(OLD_LINK);
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-description").textContent).toMatch(
        /HTML contains a character that cannot be saved/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-error-pagelink")).toBeNull();
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(
      /HTML contains a character that cannot be saved/i,
    );
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
      /link contains a character/i,
    );
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
  });

  it("refuses a link NUL on the overlay strip", async () => {
    const previewDoc = document.implementation.createHTMLDocument("empty");
    const saveFields = vi.fn();
    renderLinkHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(linkPayload(OLD_LINK)),
    );
    await waitFor(() => {
      expect(screen.getByTestId("assembly-overlay-field-pagelink")).toBeTruthy();
    });
    const input = screen.getByTestId(
      "assembly-overlay-field-pagelink",
    ) as HTMLInputElement;
    expect(input.value).toBe(OLD_LINK);
    input.value = "0-101-594\u0000";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-pagelink").textContent).toMatch(
        /link contains a character that cannot be saved/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(screen.queryByTestId("assembly-field-error-description")).toBeNull();
    expect(input.getAttribute("aria-invalid")).toBe("true");
    expect(input.value).toContain("\u0000");
  });

  it("edits a link from the overlay strip when the page has no marker", async () => {
    const previewDoc = document.implementation.createHTMLDocument("empty");
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderLinkHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(screen.getByTestId("assembly-overlay-field-pagelink")).toBeTruthy();
    });
    const input = screen.getByTestId(
      "assembly-overlay-field-pagelink",
    ) as HTMLInputElement;
    expect(input.tagName).toBe("INPUT");
    expect(input.value).toBe(OLD_LINK);
    input.value = NEW_LINK;
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "pagelink")).toEqual({
      name: "pagelink",
      value: NEW_LINK,
      dataType: "link",
    });
  });

  const OLD_TEXT = "Welcome";
  const NEW_TEXT = "Updated welcome";
  const LONG_NOTE = "A long note";
  const BODY_HTML = "<p>About the site</p>";

  const textFields: ItemEditorFields = {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [
      { name: "displaytitle", value: OLD_TEXT },
      { name: "notes", value: LONG_NOTE },
      { name: "description", value: BODY_HTML },
      { name: "pagelink", value: "//Sites/Example/index" },
    ],
  };

  const textSchema = {
    fields: [
      { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
      { name: "notes", label: "Notes", control: "sys_TextArea" },
      { name: "description", label: "Body", control: "sys_tinymce" },
      { name: "pagelink", label: "Page link", control: "sys_PageLink" },
    ],
  };

  function textPreviewDoc(title = OLD_TEXT, notes = LONG_NOTE): Document {
    const previewDoc = document.implementation.createHTMLDocument("preview");
    previewDoc.body.innerHTML = `
      <h1 data-perc-field="displaytitle">${title}</h1>
      <p data-perc-field="notes">${notes}</p>
      <div class="PsAaField" id='[3,42,7,0,0,0,0,1,0,0,0,"description",42,"Body",0]'>${BODY_HTML}</div>
      <a data-perc-field="pagelink" href="//Sites/Example/index">Example</a>
    `;
    return previewDoc;
  }

  function renderTextHost(
    previewDoc: Document,
    saveFields: ReturnType<typeof vi.fn>,
    loadFields: ReturnType<typeof vi.fn> = vi.fn().mockResolvedValue(textFields),
    schema: { fields: Array<Record<string, unknown>> } = textSchema,
  ): void {
    renderHost("?contentId=42&templateId=7", {
      fetchPreview: vi.fn().mockResolvedValue({
        previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
        contentId: 42,
        templateId: 7,
        revision: 1,
      }),
      loadTemplates: vi.fn().mockResolvedValue([
        {
          name: "rffPgGeneric",
          label: "Generic Page",
          url: "../assembler/render?sys_template=7",
          sortRank: 0,
          menuType: "MENUITEM",
        } satisfies MenuAction,
      ]),
      checkout: vi.fn().mockResolvedValue(undefined),
      loadFields,
      saveFields,
      loadType: vi.fn().mockResolvedValue(schema),
      getPreviewDocument: () => previewDoc,
    });
  }

  it("saves one single-line text field and leaves the other fields unchanged", async () => {
    const previewDoc = textPreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderTextHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-displaytitle"]'),
      ).toBeTruthy();
    });
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    expect(title.getAttribute("data-assembly-value")).toBe("text");
    title.textContent = "Updated\nwelcome";
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "displaytitle")).toEqual({
      name: "displaytitle",
      value: NEW_TEXT,
    });
    expect(saved.fields.find((f) => f.name === "notes")).toEqual({
      name: "notes",
      value: LONG_NOTE,
    });
    expect(saved.fields.find((f) => f.name === "description")).toEqual({
      name: "description",
      value: BODY_HTML,
    });
    expect(saved.fields.find((f) => f.name === "pagelink")).toEqual({
      name: "pagelink",
      value: "//Sites/Example/index",
    });
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    expect(title.textContent).toBe(NEW_TEXT);
  });

  it("reloads the assembly host with the single-line text that was saved", async () => {
    const savedPayload: ItemEditorFields = {
      ...textFields,
      fields: textFields.fields.map((field) =>
        field.name === "displaytitle" ? { ...field, value: NEW_TEXT } : field,
      ),
    };
    const previewDoc = textPreviewDoc(NEW_TEXT);
    renderTextHost(
      previewDoc,
      vi.fn().mockResolvedValue(savedPayload),
      vi.fn().mockResolvedValue(savedPayload),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-displaytitle"]'),
      ).toBeTruthy();
    });
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    expect(title.textContent).toBe(NEW_TEXT);
    expect(
      (previewDoc.querySelector('[data-testid="assembly-inline-field-notes"]') as HTMLElement)
        .textContent,
    ).toBe(LONG_NOTE);
  });

  it("does not write a read-only single-line text field", async () => {
    const previewDoc = textPreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderTextHost(previewDoc, saveFields, vi.fn().mockResolvedValue(textFields), {
      fields: [
        {
          name: "displaytitle",
          label: "Display title",
          control: "sys_EditBox",
          readOnly: true,
        },
        { name: "notes", label: "Notes", control: "sys_TextArea" },
        { name: "description", label: "Body", control: "sys_tinymce" },
        { name: "pagelink", label: "Page link", control: "sys_PageLink" },
      ],
    });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-inline-notes")).toBeTruthy();
    });
    expect(screen.queryByTestId("assembly-field-chip-displaytitle")).toBeNull();
    expect(
      previewDoc.querySelector('[data-testid="assembly-inline-field-displaytitle"]'),
    ).toBeNull();
    const notes = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-notes"]',
    ) as HTMLElement;
    notes.textContent = "Updated note";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "displaytitle")?.value).toBe(OLD_TEXT);
    expect(saved.fields.find((f) => f.name === "notes")?.value).toBe("Updated note");
    expect(saved.fields.find((f) => f.name === "description")?.value).toBe(BODY_HTML);
    expect(saved.fields.find((f) => f.name === "pagelink")?.value).toBe(
      "//Sites/Example/index",
    );
    expect(saved.fields.find((f) => f.name === "pagelink")?.dataType).toBeUndefined();
  });

  it.each([400, 403, 409])(
    "HTTP %s leaves the previous single-line text in place",
    async (status) => {
      const previewDoc = textPreviewDoc();
      const saveFields = vi.fn().mockRejectedValue({ status });
      renderTextHost(previewDoc, saveFields);
      await waitFor(() => {
        expect(
          previewDoc.querySelector('[data-testid="assembly-inline-field-displaytitle"]'),
        ).toBeTruthy();
      });
      const title = previewDoc.querySelector(
        '[data-testid="assembly-inline-field-displaytitle"]',
      ) as HTMLElement;
      title.textContent = NEW_TEXT;
      fireEvent.click(screen.getByTestId("assembly-field-save"));
      await waitFor(() => {
        expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(
          /could not save/i,
        );
        const live = previewDoc.querySelector(
          '[data-testid="assembly-inline-field-displaytitle"]',
        ) as HTMLElement | null;
        expect(live?.textContent).toBe(OLD_TEXT);
      });
      expect(saveFields).toHaveBeenCalled();
      const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
      expect(sent.fields.find((field) => field.name === "displaytitle")?.value).toBe(
        NEW_TEXT,
      );
      expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
        /fields saved/i,
      );
    },
  );

  const NEW_NOTE = "Line one\nLine two";

  it("saves one long-text field and leaves the other fields unchanged", async () => {
    const previewDoc = textPreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderTextHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-notes"]'),
      ).toBeTruthy();
    });
    const notes = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-notes"]',
    ) as HTMLElement;
    expect(notes.getAttribute("data-assembly-value")).toBe("longtext");
    expect(notes.style.whiteSpace).toBe("pre-wrap");
    notes.innerHTML = "Line one<br>Line two";
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "notes")).toEqual({
      name: "notes",
      value: NEW_NOTE,
    });
    expect(saved.fields.find((f) => f.name === "displaytitle")).toEqual({
      name: "displaytitle",
      value: OLD_TEXT,
    });
    expect(saved.fields.find((f) => f.name === "description")).toEqual({
      name: "description",
      value: BODY_HTML,
    });
    expect(saved.fields.find((f) => f.name === "pagelink")).toEqual({
      name: "pagelink",
      value: "//Sites/Example/index",
    });
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    expect(notes.textContent).toBe(NEW_NOTE);
    expect(notes.style.whiteSpace).toBe("pre-wrap");
  });

  it("does not write a long-text edit that is left unsaved", async () => {
    const previewDoc = textPreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderTextHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-notes"]'),
      ).toBeTruthy();
    });
    const notes = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-notes"]',
    ) as HTMLElement;
    notes.textContent = NEW_NOTE;
    fireEvent.click(screen.getByTestId("assembly-close"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(notes.textContent).toBe(NEW_NOTE);
  });

  it("reloads the assembly host with the long text that was saved", async () => {
    const savedPayload: ItemEditorFields = {
      ...textFields,
      fields: textFields.fields.map((field) =>
        field.name === "notes" ? { ...field, value: NEW_NOTE } : field,
      ),
    };
    const previewDoc = textPreviewDoc(OLD_TEXT, NEW_NOTE);
    renderTextHost(
      previewDoc,
      vi.fn().mockResolvedValue(savedPayload),
      vi.fn().mockResolvedValue(savedPayload),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-notes"]'),
      ).toBeTruthy();
    });
    const notes = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-notes"]',
    ) as HTMLElement;
    expect(notes.textContent).toBe(NEW_NOTE);
    expect(notes.getAttribute("data-assembly-value")).toBe("longtext");
    expect(
      (previewDoc.querySelector(
        '[data-testid="assembly-inline-field-displaytitle"]',
      ) as HTMLElement).textContent,
    ).toBe(OLD_TEXT);
  });

  it("does not write a read-only long-text field", async () => {
    const previewDoc = textPreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderTextHost(previewDoc, saveFields, vi.fn().mockResolvedValue(textFields), {
      fields: [
        { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
        { name: "notes", label: "Notes", control: "sys_TextArea", readOnly: true },
        { name: "description", label: "Body", control: "sys_tinymce" },
        { name: "pagelink", label: "Page link", control: "sys_PageLink" },
      ],
    });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-inline-displaytitle")).toBeTruthy();
    });
    expect(screen.queryByTestId("assembly-field-chip-notes")).toBeNull();
    expect(
      previewDoc.querySelector('[data-testid="assembly-inline-field-notes"]'),
    ).toBeNull();
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    title.textContent = NEW_TEXT;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "notes")?.value).toBe(LONG_NOTE);
    expect(saved.fields.find((f) => f.name === "displaytitle")?.value).toBe(NEW_TEXT);
    expect(saved.fields.find((f) => f.name === "description")?.value).toBe(BODY_HTML);
    expect(saved.fields.find((f) => f.name === "pagelink")?.value).toBe(
      "//Sites/Example/index",
    );
  });

  it.each([400, 403, 409])(
    "HTTP %s leaves the previous long text in place",
    async (status) => {
      const previewDoc = textPreviewDoc();
      const saveFields = vi.fn().mockRejectedValue({ status });
      renderTextHost(previewDoc, saveFields);
      await waitFor(() => {
        expect(
          previewDoc.querySelector('[data-testid="assembly-inline-field-notes"]'),
        ).toBeTruthy();
      });
      const notes = previewDoc.querySelector(
        '[data-testid="assembly-inline-field-notes"]',
      ) as HTMLElement;
      notes.textContent = NEW_NOTE;
      fireEvent.click(screen.getByTestId("assembly-field-save"));
      await waitFor(() => {
        expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(
          /could not save/i,
        );
        const live = previewDoc.querySelector(
          '[data-testid="assembly-inline-field-notes"]',
        ) as HTMLElement | null;
        expect(live?.textContent).toBe(LONG_NOTE);
      });
      expect(saveFields).toHaveBeenCalled();
      const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
      expect(sent.fields.find((field) => field.name === "notes")?.value).toBe(NEW_NOTE);
      expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
        /fields saved/i,
      );
    },
  );

  it("saves long text from the overlay strip when the page has no node", async () => {
    const previewDoc = document.implementation.createHTMLDocument("empty");
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderTextHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(screen.getByTestId("assembly-overlay-field-notes")).toBeTruthy();
    });
    const area = screen.getByTestId("assembly-overlay-field-notes") as HTMLTextAreaElement;
    expect(area.tagName).toBe("TEXTAREA");
    expect(area.getAttribute("data-assembly-value")).toBe("longtext");
    expect(area.value).toBe(LONG_NOTE);
    area.value = NEW_NOTE;
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((f) => f.name === "notes")).toEqual({
      name: "notes",
      value: NEW_NOTE,
    });
    expect(saved.fields.find((f) => f.name === "displaytitle")?.value).toBe(OLD_TEXT);
    expect(area.value).toBe(NEW_NOTE);
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
  });

  const PREV_NOTE = "Line one\nLine two";

  function notesPayload(notes: string): ItemEditorFields {
    return {
      ...textFields,
      fields: textFields.fields.map((field) =>
        field.name === "notes" ? { ...field, value: notes } : field,
      ),
    };
  }

  it("refuses a long-text NUL before save and reloads the previous text including line breaks", async () => {
    const previewDoc = textPreviewDoc(OLD_TEXT, PREV_NOTE);
    const saveFields = vi.fn();
    renderTextHost(previewDoc, saveFields, vi.fn().mockResolvedValue(notesPayload(PREV_NOTE)));
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-notes"]'),
      ).toBeTruthy();
    });
    const notes = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-notes"]',
    ) as HTMLElement;
    expect(notes.textContent).toBe(PREV_NOTE);
    expect(notes.getAttribute("data-assembly-value")).toBe("longtext");
    notes.textContent = "Line one\nbad\u0000value";
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-notes").textContent).toMatch(
        /long text contains a character that cannot be saved/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(
      /long text contains a character that cannot be saved/i,
    );
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
    expect(screen.queryByTestId("assembly-field-error-displaytitle")).toBeNull();
    expect(notes.hasAttribute("aria-invalid")).toBe(false);
    expect(notes.textContent).toContain("\u0000");
    cleanup();
    const reloaded = textPreviewDoc(OLD_TEXT, PREV_NOTE);
    renderTextHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue(notesPayload(PREV_NOTE)),
    );
    await waitFor(() => {
      const live = reloaded.querySelector(
        '[data-testid="assembly-inline-field-notes"]',
      ) as HTMLElement | null;
      expect(live?.textContent).toBe(PREV_NOTE);
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
  });

  it("does not write when Cancel leaves a long-text NUL edit", async () => {
    const previewDoc = textPreviewDoc(OLD_TEXT, PREV_NOTE);
    const saveFields = vi.fn();
    renderTextHost(previewDoc, saveFields, vi.fn().mockResolvedValue(notesPayload(PREV_NOTE)));
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-notes"]'),
      ).toBeTruthy();
    });
    const notes = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-notes"]',
    ) as HTMLElement;
    notes.textContent = "Line one\nbad\u0000value";
    fireEvent.click(screen.getByTestId("assembly-field-cancel"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
    expect(notes.textContent).toBe(PREV_NOTE);
    expect(notes.textContent).toContain("\n");
    expect(notes.textContent).not.toContain("\u0000");
    expect(screen.queryByTestId("assembly-field-error-notes")).toBeNull();
  });

  it("still saves a multiline long-text value without a NUL", async () => {
    let notesValue = PREV_NOTE;
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => {
      notesValue = body.fields.find((field) => field.name === "notes")?.value ?? notesValue;
      return notesPayload(notesValue);
    });
    const previewDoc = textPreviewDoc(OLD_TEXT, PREV_NOTE);
    renderTextHost(
      previewDoc,
      saveFields,
      vi.fn(async () => notesPayload(notesValue)),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-notes"]'),
      ).toBeTruthy();
    });
    const notes = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-notes"]',
    ) as HTMLElement;
    const next = "Updated line\nsecond line";
    notes.innerHTML = "Updated line<br>second line";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    });
    const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(sent.fields.find((field) => field.name === "notes")?.value).toBe(next);
    expect(String(sent.fields.find((field) => field.name === "notes")?.value)).not.toContain(
      "\u0000",
    );
    expect(sent.fields.find((field) => field.name === "displaytitle")?.value).toBe(OLD_TEXT);
    expect(screen.queryByTestId("assembly-field-error-notes")).toBeNull();
    cleanup();
    const reloaded = textPreviewDoc(OLD_TEXT, next);
    renderTextHost(reloaded, saveFields, vi.fn().mockResolvedValue(notesPayload(next)));
    await waitFor(() => {
      const live = reloaded.querySelector(
        '[data-testid="assembly-inline-field-notes"]',
      ) as HTMLElement | null;
      expect(live?.textContent).toBe(next);
    });
  });

  it("HTTP 400 on a long-text save does not claim success and keeps line breaks", async () => {
    const previewDoc = textPreviewDoc(OLD_TEXT, PREV_NOTE);
    const saveFields = vi.fn().mockRejectedValue({ status: 400 });
    renderTextHost(previewDoc, saveFields, vi.fn().mockResolvedValue(notesPayload(PREV_NOTE)));
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-notes"]'),
      ).toBeTruthy();
    });
    const notes = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-notes"]',
    ) as HTMLElement;
    notes.innerHTML = "Updated line<br>second line";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/could not save/i);
      const live = previewDoc.querySelector(
        '[data-testid="assembly-inline-field-notes"]',
      ) as HTMLElement | null;
      expect(live?.textContent).toBe(PREV_NOTE);
    });
    expect(saveFields).toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
    expect(screen.queryByTestId("assembly-field-error-notes")).toBeNull();
  });

  it("refuses a long-text NUL on the overlay strip", async () => {
    const previewDoc = document.implementation.createHTMLDocument("empty");
    const saveFields = vi.fn();
    renderTextHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(notesPayload(PREV_NOTE)),
    );
    await waitFor(() => {
      expect(screen.getByTestId("assembly-overlay-field-notes")).toBeTruthy();
    });
    const area = screen.getByTestId("assembly-overlay-field-notes") as HTMLTextAreaElement;
    expect(area.value).toBe(PREV_NOTE);
    area.value = "Line one\nbad\u0000value";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-notes").textContent).toMatch(
        /long text contains a character that cannot be saved/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(area.getAttribute("aria-invalid")).toBe("true");
    expect(area.value).toContain("\u0000");
    expect(area.value).toContain("\n");
  });

  function requiredTextSchema(required = true) {
    return {
      fields: textSchema.fields.map((field) =>
        field.name === "displaytitle" ? { ...field, required } : field,
      ),
    };
  }

  it("does not save a blank required text field and reloads the previous text", async () => {
    const previewDoc = textPreviewDoc();
    const saveFields = vi.fn();
    renderTextHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(textFields),
      requiredTextSchema(),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-displaytitle"]'),
      ).toBeTruthy();
    });
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    expect(title.hasAttribute("aria-required")).toBe(false);
    expect(title.getAttribute("data-assembly-required")).toBe("true");
    expect(
      screen.getByTestId("assembly-field-chip-displaytitle").getAttribute("data-required"),
    ).toBe("true");
    title.textContent = "";
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-displaytitle").textContent).toMatch(
        /required/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/required/i);
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
    expect(title.hasAttribute("aria-invalid")).toBe(false);
    expect(title.textContent).toBe("");
    cleanup();
    const reloaded = textPreviewDoc();
    renderTextHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue(textFields),
      requiredTextSchema(),
    );
    await waitFor(() => {
      const live = reloaded.querySelector(
        '[data-testid="assembly-inline-field-displaytitle"]',
      ) as HTMLElement | null;
      expect(live?.textContent).toBe(OLD_TEXT);
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
  });

  it("does not save whitespace-only required text and keeps the previous value", async () => {
    const previewDoc = textPreviewDoc();
    const saveFields = vi.fn();
    renderTextHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(textFields),
      requiredTextSchema(),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-displaytitle"]'),
      ).toBeTruthy();
    });
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    title.textContent = "  \n  ";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-displaytitle").textContent).toMatch(
        /required/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    cleanup();
    const reloaded = textPreviewDoc();
    renderTextHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue(textFields),
      requiredTextSchema(),
    );
    await waitFor(() => {
      const live = reloaded.querySelector(
        '[data-testid="assembly-inline-field-displaytitle"]',
      ) as HTMLElement | null;
      expect(live?.textContent).toBe(OLD_TEXT);
    });
  });

  it("does not write when Close leaves a blank required text edit", async () => {
    const previewDoc = textPreviewDoc();
    const saveFields = vi.fn();
    renderTextHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(textFields),
      requiredTextSchema(),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-displaytitle"]'),
      ).toBeTruthy();
    });
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    title.textContent = "";
    fireEvent.click(screen.getByTestId("assembly-close"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
    expect(title.textContent).toBe("");
    expect(
      screen.getByTestId("assembly-field-chip-displaytitle").getAttribute("data-required"),
    ).toBe("true");
  });

  it("still saves a non-blank required text value", async () => {
    let titleValue = OLD_TEXT;
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => {
      titleValue = body.fields.find((field) => field.name === "displaytitle")?.value ?? titleValue;
      return {
        ...textFields,
        fields: textFields.fields.map((field) =>
          field.name === "displaytitle" ? { ...field, value: titleValue } : field,
        ),
      };
    });
    const previewDoc = textPreviewDoc();
    renderTextHost(
      previewDoc,
      saveFields,
      vi.fn(async () => ({
        ...textFields,
        fields: textFields.fields.map((field) =>
          field.name === "displaytitle" ? { ...field, value: titleValue } : field,
        ),
      })),
      requiredTextSchema(),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-displaytitle"]'),
      ).toBeTruthy();
    });
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    title.textContent = NEW_TEXT;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    });
    const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(sent.fields.find((field) => field.name === "displaytitle")?.value).toBe(NEW_TEXT);
    expect(sent.fields.find((field) => field.name === "notes")?.value).toBe(LONG_NOTE);
    expect(screen.queryByTestId("assembly-field-error-displaytitle")).toBeNull();
    cleanup();
    const reloaded = textPreviewDoc(NEW_TEXT);
    renderTextHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue({
        ...textFields,
        fields: textFields.fields.map((field) =>
          field.name === "displaytitle" ? { ...field, value: NEW_TEXT } : field,
        ),
      }),
      requiredTextSchema(),
    );
    await waitFor(() => {
      const live = reloaded.querySelector(
        '[data-testid="assembly-inline-field-displaytitle"]',
      ) as HTMLElement | null;
      expect(live?.textContent).toBe(NEW_TEXT);
    });
  });

  it("still clears an optional single-line text field", async () => {
    const previewDoc = textPreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderTextHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-displaytitle"]'),
      ).toBeTruthy();
    });
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    expect(title.hasAttribute("aria-required")).toBe(false);
    expect(
      screen.getByTestId("assembly-field-chip-displaytitle").getAttribute("data-required"),
    ).toBe("false");
    title.textContent = "   ";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "displaytitle")?.value).toBe("");
    expect(saved.fields.find((field) => field.name === "notes")?.value).toBe(LONG_NOTE);
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    expect(screen.queryByTestId("assembly-field-error-displaytitle")).toBeNull();
  });

  it("HTTP 400 on a required text field does not claim success", async () => {
    const previewDoc = textPreviewDoc();
    const saveFields = vi.fn().mockRejectedValue({ status: 400 });
    renderTextHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(textFields),
      requiredTextSchema(),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-displaytitle"]'),
      ).toBeTruthy();
    });
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    title.textContent = NEW_TEXT;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/could not save/i);
      const live = previewDoc.querySelector(
        '[data-testid="assembly-inline-field-displaytitle"]',
      ) as HTMLElement | null;
      expect(live?.textContent).toBe(OLD_TEXT);
    });
    expect(saveFields).toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
    expect(screen.queryByTestId("assembly-field-error-displaytitle")).toBeNull();
  });

  it("refuses a blank required text field on the overlay strip", async () => {
    const previewDoc = document.implementation.createHTMLDocument("empty");
    const saveFields = vi.fn();
    renderTextHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(textFields),
      requiredTextSchema(),
    );
    await waitFor(() => {
      expect(screen.getByTestId("assembly-overlay-field-displaytitle")).toBeTruthy();
    });
    const input = screen.getByTestId(
      "assembly-overlay-field-displaytitle",
    ) as HTMLInputElement;
    expect(input.getAttribute("aria-required")).toBe("true");
    input.value = "   ";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-displaytitle").textContent).toMatch(
        /required/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(input.getAttribute("aria-invalid")).toBe("true");
  });

  it("refuses a single-line NUL before save and reloads the previous text", async () => {
    const previewDoc = textPreviewDoc();
    const saveFields = vi.fn();
    renderTextHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-displaytitle"]'),
      ).toBeTruthy();
    });
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    expect(title.getAttribute("data-assembly-value")).toBe("text");
    title.textContent = "bad\u0000value";
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-displaytitle").textContent).toMatch(
        /character that cannot be saved/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(
      /character that cannot be saved/i,
    );
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
    expect(title.hasAttribute("aria-invalid")).toBe(false);
    expect(title.textContent).toBe("bad\u0000value");
    cleanup();
    const reloaded = textPreviewDoc();
    renderTextHost(reloaded, saveFields);
    await waitFor(() => {
      const live = reloaded.querySelector(
        '[data-testid="assembly-inline-field-displaytitle"]',
      ) as HTMLElement | null;
      expect(live?.textContent).toBe(OLD_TEXT);
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
  });

  it("does not write when Cancel leaves a single-line NUL edit", async () => {
    const previewDoc = textPreviewDoc();
    const saveFields = vi.fn();
    renderTextHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-displaytitle"]'),
      ).toBeTruthy();
    });
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    title.textContent = "bad\u0000value";
    fireEvent.click(screen.getByTestId("assembly-field-cancel"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
    expect(title.textContent).toBe(OLD_TEXT);
    expect(screen.queryByTestId("assembly-field-error-displaytitle")).toBeNull();
  });

  it("still saves an ordinary single-line value", async () => {
    let titleValue = OLD_TEXT;
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => {
      titleValue = body.fields.find((field) => field.name === "displaytitle")?.value ?? titleValue;
      return {
        ...textFields,
        fields: textFields.fields.map((field) =>
          field.name === "displaytitle" ? { ...field, value: titleValue } : field,
        ),
      };
    });
    const previewDoc = textPreviewDoc();
    renderTextHost(
      previewDoc,
      saveFields,
      vi.fn(async () => ({
        ...textFields,
        fields: textFields.fields.map((field) =>
          field.name === "displaytitle" ? { ...field, value: titleValue } : field,
        ),
      })),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-displaytitle"]'),
      ).toBeTruthy();
    });
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    title.textContent = NEW_TEXT;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    });
    const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(sent.fields.find((field) => field.name === "displaytitle")?.value).toBe(NEW_TEXT);
    expect(String(sent.fields.find((field) => field.name === "displaytitle")?.value)).not.toContain(
      "\u0000",
    );
    expect(sent.fields.find((field) => field.name === "notes")?.value).toBe(LONG_NOTE);
    expect(screen.queryByTestId("assembly-field-error-displaytitle")).toBeNull();
    cleanup();
    const reloaded = textPreviewDoc(NEW_TEXT);
    renderTextHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue({
        ...textFields,
        fields: textFields.fields.map((field) =>
          field.name === "displaytitle" ? { ...field, value: NEW_TEXT } : field,
        ),
      }),
    );
    await waitFor(() => {
      const live = reloaded.querySelector(
        '[data-testid="assembly-inline-field-displaytitle"]',
      ) as HTMLElement | null;
      expect(live?.textContent).toBe(NEW_TEXT);
    });
  });

  it("HTTP 400 on a single-line text save does not claim success", async () => {
    const previewDoc = textPreviewDoc();
    const saveFields = vi.fn().mockRejectedValue({ status: 400 });
    renderTextHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-displaytitle"]'),
      ).toBeTruthy();
    });
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    title.textContent = NEW_TEXT;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/could not save/i);
      const live = previewDoc.querySelector(
        '[data-testid="assembly-inline-field-displaytitle"]',
      ) as HTMLElement | null;
      expect(live?.textContent).toBe(OLD_TEXT);
    });
    expect(saveFields).toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
    expect(screen.queryByTestId("assembly-field-error-displaytitle")).toBeNull();
  });

  it("refuses a single-line NUL on the overlay strip", async () => {
    const previewDoc = document.implementation.createHTMLDocument("empty");
    const saveFields = vi.fn();
    renderTextHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(screen.getByTestId("assembly-overlay-field-displaytitle")).toBeTruthy();
    });
    const input = screen.getByTestId(
      "assembly-overlay-field-displaytitle",
    ) as HTMLInputElement;
    expect(input.getAttribute("data-assembly-value")).toBe("text");
    input.value = "bad\u0000value";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-displaytitle").textContent).toMatch(
        /character that cannot be saved/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(input.getAttribute("aria-invalid")).toBe("true");
    expect(input.value).toBe("bad\u0000value");
  });

  const OLD_QTY = "12";
  const NEW_QTY = "27";

  const numberFields: ItemEditorFields = {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [
      { name: "qty", value: OLD_QTY },
      { name: "displaytitle", value: OLD_TEXT },
      { name: "notes", value: LONG_NOTE },
    ],
  };

  const numberSchema = {
    fields: [
      { name: "qty", label: "Quantity", control: "sys_Number", dataType: "integer" },
      { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
      { name: "notes", label: "Notes", control: "sys_TextArea" },
    ],
  };

  function numberPreviewDoc(qty = OLD_QTY): Document {
    const previewDoc = document.implementation.createHTMLDocument("preview");
    previewDoc.body.innerHTML = `
      <span data-perc-field="qty">${qty}</span>
      <h1 data-perc-field="displaytitle">${OLD_TEXT}</h1>
      <p data-perc-field="notes">${LONG_NOTE}</p>
    `;
    return previewDoc;
  }

  function renderNumberHost(
    previewDoc: Document,
    saveFields: ReturnType<typeof vi.fn>,
    loadFields: ReturnType<typeof vi.fn> = vi.fn().mockResolvedValue(numberFields),
    schema: { fields: Array<Record<string, unknown>> } = numberSchema,
  ): void {
    renderHost("?contentId=42&templateId=7", {
      fetchPreview: vi.fn().mockResolvedValue({
        previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
        contentId: 42,
        templateId: 7,
        revision: 1,
      }),
      loadTemplates: vi.fn().mockResolvedValue([
        {
          name: "rffPgGeneric",
          label: "Generic Page",
          url: "../assembler/render?sys_template=7",
          sortRank: 0,
          menuType: "MENUITEM",
        } satisfies MenuAction,
      ]),
      checkout: vi.fn().mockResolvedValue(undefined),
      loadFields,
      saveFields,
      loadType: vi.fn().mockResolvedValue(schema),
      getPreviewDocument: () => previewDoc,
    });
  }

  it("saves one whole number and leaves the other fields unchanged", async () => {
    const previewDoc = numberPreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderNumberHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-qty"]'),
      ).toBeTruthy();
    });
    const qty = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-qty"]',
    ) as HTMLElement;
    expect(qty.getAttribute("data-assembly-value")).toBe("number");
    expect(qty.textContent).toBe(OLD_QTY);
    qty.textContent = ` ${NEW_QTY} `;
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "qty")).toEqual({
      name: "qty",
      value: NEW_QTY,
      dataType: "integer",
    });
    expect(saved.fields.find((field) => field.name === "displaytitle")).toEqual({
      name: "displaytitle",
      value: OLD_TEXT,
    });
    expect(saved.fields.find((field) => field.name === "notes")).toEqual({
      name: "notes",
      value: LONG_NOTE,
    });
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    expect(qty.textContent).toBe(NEW_QTY);
  });

  it("reloads the assembly host with the whole number that was saved", async () => {
    const savedPayload: ItemEditorFields = {
      ...numberFields,
      fields: numberFields.fields.map((field) =>
        field.name === "qty" ? { ...field, value: NEW_QTY } : field,
      ),
    };
    const previewDoc = numberPreviewDoc(NEW_QTY);
    renderNumberHost(
      previewDoc,
      vi.fn().mockResolvedValue(savedPayload),
      vi.fn().mockResolvedValue(savedPayload),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-qty"]'),
      ).toBeTruthy();
    });
    const qty = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-qty"]',
    ) as HTMLElement;
    expect(qty.textContent).toBe(NEW_QTY);
    expect(
      (previewDoc.querySelector(
        '[data-testid="assembly-inline-field-displaytitle"]',
      ) as HTMLElement).textContent,
    ).toBe(OLD_TEXT);
  });

  it("does not write a number edit that is left unsaved", async () => {
    const previewDoc = numberPreviewDoc();
    const saveFields = vi.fn();
    renderNumberHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-qty"]'),
      ).toBeTruthy();
    });
    const qty = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-qty"]',
    ) as HTMLElement;
    qty.textContent = NEW_QTY;
    fireEvent.click(screen.getByTestId("assembly-close"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
    expect(qty.textContent).toBe(NEW_QTY);
  });

  it.each(["12.5", "abc", ""])(
    "does not save %j and leaves the previous number",
    async (next) => {
      const previewDoc = numberPreviewDoc();
      const saveFields = vi.fn();
      renderNumberHost(previewDoc, saveFields);
      await waitFor(() => {
        expect(
          previewDoc.querySelector('[data-testid="assembly-inline-field-qty"]'),
        ).toBeTruthy();
      });
      const qty = previewDoc.querySelector(
        '[data-testid="assembly-inline-field-qty"]',
      ) as HTMLElement;
      qty.textContent = next;
      fireEvent.click(screen.getByTestId("assembly-field-save"));
      await waitFor(() => {
        expect(screen.getByTestId("assembly-field-error-qty").textContent).toMatch(
          /whole number/i,
        );
      });
      expect(saveFields).not.toHaveBeenCalled();
      expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/whole number/i);
      expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
        /fields saved/i,
      );
      expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
      expect(qty.textContent).toBe(OLD_QTY);
    },
  );

  it("does not write a read-only number field", async () => {
    const previewDoc = numberPreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderNumberHost(previewDoc, saveFields, vi.fn().mockResolvedValue(numberFields), {
      fields: [
        {
          name: "qty",
          label: "Quantity",
          control: "sys_Number",
          dataType: "integer",
          readOnly: true,
        },
        { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
        { name: "notes", label: "Notes", control: "sys_TextArea" },
      ],
    });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-inline-displaytitle")).toBeTruthy();
    });
    expect(screen.queryByTestId("assembly-field-chip-qty")).toBeNull();
    expect(previewDoc.querySelector('[data-testid="assembly-inline-field-qty"]')).toBeNull();
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    title.textContent = NEW_TEXT;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "qty")?.value).toBe(OLD_QTY);
    expect(saved.fields.find((field) => field.name === "qty")?.dataType).toBeUndefined();
    expect(saved.fields.find((field) => field.name === "displaytitle")?.value).toBe(NEW_TEXT);
    expect(saved.fields.find((field) => field.name === "notes")?.value).toBe(LONG_NOTE);
  });

  it.each([400, 403, 409])(
    "HTTP %s leaves the previous number in place",
    async (status) => {
      const previewDoc = numberPreviewDoc();
      const saveFields = vi.fn().mockRejectedValue({ status });
      renderNumberHost(previewDoc, saveFields);
      await waitFor(() => {
        expect(
          previewDoc.querySelector('[data-testid="assembly-inline-field-qty"]'),
        ).toBeTruthy();
      });
      const qty = previewDoc.querySelector(
        '[data-testid="assembly-inline-field-qty"]',
      ) as HTMLElement;
      qty.textContent = NEW_QTY;
      fireEvent.click(screen.getByTestId("assembly-field-save"));
      await waitFor(() => {
        expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(
          /could not save/i,
        );
        const live = previewDoc.querySelector(
          '[data-testid="assembly-inline-field-qty"]',
        ) as HTMLElement | null;
        expect(live?.textContent).toBe(OLD_QTY);
      });
      expect(saveFields).toHaveBeenCalled();
      const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
      expect(sent.fields.find((field) => field.name === "qty")?.value).toBe(NEW_QTY);
      expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
        /fields saved/i,
      );
    },
  );

  it("saves a whole number from the overlay strip when the page has no node", async () => {
    const previewDoc = document.implementation.createHTMLDocument("empty");
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderNumberHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(screen.getByTestId("assembly-overlay-field-qty")).toBeTruthy();
    });
    const input = screen.getByTestId("assembly-overlay-field-qty") as HTMLInputElement;
    expect(input.tagName).toBe("INPUT");
    expect(input.getAttribute("data-assembly-value")).toBe("number");
    expect(input.value).toBe(OLD_QTY);
    input.value = NEW_QTY;
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "qty")).toEqual({
      name: "qty",
      value: NEW_QTY,
      dataType: "integer",
    });
    expect(saved.fields.find((field) => field.name === "displaytitle")?.value).toBe(OLD_TEXT);
    expect(input.value).toBe(NEW_QTY);
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
  });

  function requiredNumberSchema(required = true) {
    return {
      fields: numberSchema.fields.map((field) =>
        field.name === "qty" ? { ...field, required } : field,
      ),
    };
  }

  it("does not save a blank required number and reloads the previous number", async () => {
    const previewDoc = numberPreviewDoc();
    const saveFields = vi.fn();
    renderNumberHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(numberFields),
      requiredNumberSchema(),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-qty"]'),
      ).toBeTruthy();
    });
    const qty = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-qty"]',
    ) as HTMLElement;
    expect(qty.hasAttribute("aria-required")).toBe(false);
    expect(qty.getAttribute("data-assembly-required")).toBe("true");
    expect(screen.getByTestId("assembly-field-chip-qty").getAttribute("data-required")).toBe(
      "true",
    );
    qty.textContent = "";
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-qty").textContent).toMatch(/required/i);
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/required/i);
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/whole number/i);
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
    expect(qty.hasAttribute("aria-invalid")).toBe(false);
    expect(qty.textContent).toBe("");
    cleanup();
    const reloaded = numberPreviewDoc();
    renderNumberHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue(numberFields),
      requiredNumberSchema(),
    );
    await waitFor(() => {
      const live = reloaded.querySelector(
        '[data-testid="assembly-inline-field-qty"]',
      ) as HTMLElement | null;
      expect(live?.textContent).toBe(OLD_QTY);
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
  });

  it("does not save whitespace-only required number and keeps the previous value", async () => {
    const previewDoc = numberPreviewDoc();
    const saveFields = vi.fn();
    renderNumberHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(numberFields),
      requiredNumberSchema(),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-qty"]'),
      ).toBeTruthy();
    });
    const qty = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-qty"]',
    ) as HTMLElement;
    qty.textContent = "  \n  ";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-qty").textContent).toMatch(/required/i);
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/whole number/i);
    cleanup();
    const reloaded = numberPreviewDoc();
    renderNumberHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue(numberFields),
      requiredNumberSchema(),
    );
    await waitFor(() => {
      const live = reloaded.querySelector(
        '[data-testid="assembly-inline-field-qty"]',
      ) as HTMLElement | null;
      expect(live?.textContent).toBe(OLD_QTY);
    });
  });

  it("does not write when Cancel leaves a blank required number edit", async () => {
    const previewDoc = numberPreviewDoc();
    const saveFields = vi.fn();
    renderNumberHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(numberFields),
      requiredNumberSchema(),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-qty"]'),
      ).toBeTruthy();
    });
    const qty = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-qty"]',
    ) as HTMLElement;
    qty.textContent = "";
    fireEvent.click(screen.getByTestId("assembly-field-cancel"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
    expect(qty.textContent).toBe(OLD_QTY);
    expect(screen.queryByTestId("assembly-field-error-qty")).toBeNull();
  });

  it("still saves a non-blank required whole number", async () => {
    let qtyValue = OLD_QTY;
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => {
      qtyValue = body.fields.find((field) => field.name === "qty")?.value ?? qtyValue;
      return {
        ...numberFields,
        fields: numberFields.fields.map((field) =>
          field.name === "qty" ? { ...field, value: qtyValue } : field,
        ),
      };
    });
    const previewDoc = numberPreviewDoc();
    renderNumberHost(
      previewDoc,
      saveFields,
      vi.fn(async () => ({
        ...numberFields,
        fields: numberFields.fields.map((field) =>
          field.name === "qty" ? { ...field, value: qtyValue } : field,
        ),
      })),
      requiredNumberSchema(),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-qty"]'),
      ).toBeTruthy();
    });
    const qty = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-qty"]',
    ) as HTMLElement;
    qty.textContent = NEW_QTY;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    });
    const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(sent.fields.find((field) => field.name === "qty")).toEqual({
      name: "qty",
      value: NEW_QTY,
      dataType: "integer",
    });
    expect(sent.fields.find((field) => field.name === "displaytitle")?.value).toBe(OLD_TEXT);
    expect(sent.fields.find((field) => field.name === "notes")?.value).toBe(LONG_NOTE);
    expect(screen.queryByTestId("assembly-field-error-qty")).toBeNull();
    cleanup();
    const reloaded = numberPreviewDoc(NEW_QTY);
    renderNumberHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue({
        ...numberFields,
        fields: numberFields.fields.map((field) =>
          field.name === "qty" ? { ...field, value: NEW_QTY } : field,
        ),
      }),
      requiredNumberSchema(),
    );
    await waitFor(() => {
      const live = reloaded.querySelector(
        '[data-testid="assembly-inline-field-qty"]',
      ) as HTMLElement | null;
      expect(live?.textContent).toBe(NEW_QTY);
    });
  });

  it.each([400, 403, 409])(
    "HTTP %s on a required number does not claim success",
    async (status) => {
      const previewDoc = numberPreviewDoc();
      const saveFields = vi.fn().mockRejectedValue({ status });
      renderNumberHost(
        previewDoc,
        saveFields,
        vi.fn().mockResolvedValue(numberFields),
        requiredNumberSchema(),
      );
      await waitFor(() => {
        expect(
          previewDoc.querySelector('[data-testid="assembly-inline-field-qty"]'),
        ).toBeTruthy();
      });
      const qty = previewDoc.querySelector(
        '[data-testid="assembly-inline-field-qty"]',
      ) as HTMLElement;
      qty.textContent = NEW_QTY;
      fireEvent.click(screen.getByTestId("assembly-field-save"));
      await waitFor(() => {
        expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(
          /could not save/i,
        );
        const live = previewDoc.querySelector(
          '[data-testid="assembly-inline-field-qty"]',
        ) as HTMLElement | null;
        expect(live?.textContent).toBe(OLD_QTY);
      });
      expect(saveFields).toHaveBeenCalled();
      expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
        /fields saved/i,
      );
      expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
      expect(screen.queryByTestId("assembly-field-error-qty")).toBeNull();
    },
  );

  it("refuses a blank required number on the overlay strip", async () => {
    const previewDoc = document.implementation.createHTMLDocument("empty");
    const saveFields = vi.fn();
    renderNumberHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(numberFields),
      requiredNumberSchema(),
    );
    await waitFor(() => {
      expect(screen.getByTestId("assembly-overlay-field-qty")).toBeTruthy();
    });
    const input = screen.getByTestId("assembly-overlay-field-qty") as HTMLInputElement;
    expect(input.getAttribute("aria-required")).toBe("true");
    expect(input.getAttribute("data-assembly-required")).toBe("true");
    input.value = "   ";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-qty").textContent).toMatch(/required/i);
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/whole number/i);
    expect(input.getAttribute("aria-invalid")).toBe("true");
    expect(input.value).toBe("   ");
  });

  const OLD_RATE = "1";
  const NEW_RATE = "1.5";

  const decimalFields: ItemEditorFields = {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [
      { name: "rate", value: OLD_RATE },
      { name: "qty", value: OLD_QTY },
      { name: "displaytitle", value: OLD_TEXT },
      { name: "notes", value: LONG_NOTE },
    ],
  };

  const decimalSchema = {
    fields: [
      { name: "rate", label: "Rate", control: "sys_Number", dataType: "float" },
      { name: "qty", label: "Quantity", control: "sys_Number", dataType: "integer" },
      { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
      { name: "notes", label: "Notes", control: "sys_TextArea" },
    ],
  };

  function decimalPreviewDoc(rate = OLD_RATE): Document {
    const previewDoc = document.implementation.createHTMLDocument("preview");
    previewDoc.body.innerHTML = `
      <span data-perc-field="rate">${rate}</span>
      <span data-perc-field="qty">${OLD_QTY}</span>
      <h1 data-perc-field="displaytitle">${OLD_TEXT}</h1>
      <p data-perc-field="notes">${LONG_NOTE}</p>
    `;
    return previewDoc;
  }

  function renderDecimalHost(
    previewDoc: Document,
    saveFields: ReturnType<typeof vi.fn>,
    loadFields: ReturnType<typeof vi.fn> = vi.fn().mockResolvedValue(decimalFields),
    schema: { fields: Array<Record<string, unknown>> } = decimalSchema,
  ): void {
    renderHost("?contentId=42&templateId=7", {
      fetchPreview: vi.fn().mockResolvedValue({
        previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
        contentId: 42,
        templateId: 7,
        revision: 1,
      }),
      loadTemplates: vi.fn().mockResolvedValue([
        {
          name: "rffPgGeneric",
          label: "Generic Page",
          url: "../assembler/render?sys_template=7",
          sortRank: 0,
          menuType: "MENUITEM",
        } satisfies MenuAction,
      ]),
      checkout: vi.fn().mockResolvedValue(undefined),
      loadFields,
      saveFields,
      loadType: vi.fn().mockResolvedValue(schema),
      getPreviewDocument: () => previewDoc,
    });
  }

  it("saves 1.5 on a float field and leaves the whole number", async () => {
    const previewDoc = decimalPreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderDecimalHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-rate"]'),
      ).toBeTruthy();
    });
    const rate = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-rate"]',
    ) as HTMLElement;
    expect(rate.getAttribute("data-assembly-number")).toBe("float");
    expect(rate.textContent).toBe(OLD_RATE);
    rate.textContent = ` ${NEW_RATE} `;
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "rate")).toEqual({
      name: "rate",
      value: NEW_RATE,
      dataType: "float",
    });
    expect(saved.fields.find((field) => field.name === "qty")).toEqual({
      name: "qty",
      value: OLD_QTY,
    });
    expect(saved.fields.find((field) => field.name === "displaytitle")?.value).toBe(OLD_TEXT);
    expect(saved.fields.find((field) => field.name === "notes")?.value).toBe(LONG_NOTE);
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    expect(rate.textContent).toBe(NEW_RATE);
  });

  it("reloads the assembly host with the decimal that was saved", async () => {
    const savedPayload: ItemEditorFields = {
      ...decimalFields,
      fields: decimalFields.fields.map((field) =>
        field.name === "rate" ? { ...field, value: NEW_RATE } : field,
      ),
    };
    const previewDoc = decimalPreviewDoc(NEW_RATE);
    renderDecimalHost(
      previewDoc,
      vi.fn().mockResolvedValue(savedPayload),
      vi.fn().mockResolvedValue(savedPayload),
    );
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-rate"]'),
      ).toBeTruthy();
    });
    expect(
      (
        previewDoc.querySelector(
          '[data-testid="assembly-inline-field-rate"]',
        ) as HTMLElement
      ).textContent,
    ).toBe(NEW_RATE);
    expect(
      (
        previewDoc.querySelector(
          '[data-testid="assembly-inline-field-qty"]',
        ) as HTMLElement
      ).textContent,
    ).toBe(OLD_QTY);
  });

  it("does not save a decimal on an integer field and keeps the previous whole number", async () => {
    const previewDoc = decimalPreviewDoc();
    const saveFields = vi.fn();
    renderDecimalHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-qty"]'),
      ).toBeTruthy();
    });
    const qty = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-qty"]',
    ) as HTMLElement;
    const rate = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-rate"]',
    ) as HTMLElement;
    qty.textContent = NEW_RATE;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-qty").textContent).toMatch(
        /whole number/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/whole number/i);
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(qty.textContent).toBe(OLD_QTY);
    expect(rate.textContent).toBe(OLD_RATE);
  });

  it("does not write when Cancel leaves an unsaved decimal", async () => {
    const previewDoc = decimalPreviewDoc();
    const saveFields = vi.fn();
    renderDecimalHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-rate"]'),
      ).toBeTruthy();
    });
    const rate = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-rate"]',
    ) as HTMLElement;
    rate.textContent = NEW_RATE;
    fireEvent.click(screen.getByTestId("assembly-field-cancel"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
    expect(rate.textContent).toBe(OLD_RATE);
  });

  it("does not save a non-numeric float and leaves the previous number", async () => {
    const previewDoc = decimalPreviewDoc();
    const saveFields = vi.fn();
    renderDecimalHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(
        previewDoc.querySelector('[data-testid="assembly-inline-field-rate"]'),
      ).toBeTruthy();
    });
    const rate = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-rate"]',
    ) as HTMLElement;
    rate.textContent = "abc";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-rate").textContent).toMatch(
        /enter a number/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/enter a number/i);
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/whole number/i);
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
    expect(rate.textContent).toBe(OLD_RATE);
  });

  it("does not write a read-only float field", async () => {
    const previewDoc = decimalPreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderDecimalHost(previewDoc, saveFields, vi.fn().mockResolvedValue(decimalFields), {
      fields: decimalSchema.fields.map((field) =>
        field.name === "rate" ? { ...field, readOnly: true } : field,
      ),
    });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-inline-qty")).toBeTruthy();
    });
    expect(screen.queryByTestId("assembly-field-chip-rate")).toBeNull();
    expect(previewDoc.querySelector('[data-testid="assembly-inline-field-rate"]')).toBeNull();
    const qty = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-qty"]',
    ) as HTMLElement;
    qty.textContent = NEW_QTY;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "rate")).toEqual({
      name: "rate",
      value: OLD_RATE,
    });
    expect(saved.fields.find((field) => field.name === "qty")).toEqual({
      name: "qty",
      value: NEW_QTY,
      dataType: "integer",
    });
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
  });

  it.each([400, 403, 409])(
    "HTTP %s on a decimal save does not claim success",
    async (status) => {
      const previewDoc = decimalPreviewDoc();
      const saveFields = vi.fn().mockRejectedValue({ status });
      renderDecimalHost(previewDoc, saveFields);
      await waitFor(() => {
        expect(
          previewDoc.querySelector('[data-testid="assembly-inline-field-rate"]'),
        ).toBeTruthy();
      });
      const rate = previewDoc.querySelector(
        '[data-testid="assembly-inline-field-rate"]',
      ) as HTMLElement;
      rate.textContent = NEW_RATE;
      fireEvent.click(screen.getByTestId("assembly-field-save"));
      await waitFor(() => {
        expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(
          /could not save/i,
        );
        const live = previewDoc.querySelector(
          '[data-testid="assembly-inline-field-rate"]',
        ) as HTMLElement | null;
        expect(live?.textContent).toBe(OLD_RATE);
      });
      expect(saveFields).toHaveBeenCalled();
      const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
      expect(sent.fields.find((field) => field.name === "rate")?.value).toBe(NEW_RATE);
      expect(sent.fields.find((field) => field.name === "rate")?.dataType).toBe("float");
      expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
        /fields saved/i,
      );
      expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
    },
  );

  it("saves a decimal from the overlay strip when the page has no node", async () => {
    const previewDoc = document.implementation.createHTMLDocument("empty");
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderDecimalHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(screen.getByTestId("assembly-overlay-field-rate")).toBeTruthy();
    });
    const input = screen.getByTestId("assembly-overlay-field-rate") as HTMLInputElement;
    expect(input.getAttribute("inputmode")).toBe("decimal");
    expect(input.getAttribute("data-assembly-number")).toBe("float");
    expect(input.value).toBe(OLD_RATE);
    input.value = NEW_RATE;
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "rate")).toEqual({
      name: "rate",
      value: NEW_RATE,
      dataType: "float",
    });
    expect(saved.fields.find((field) => field.name === "qty")?.value).toBe(OLD_QTY);
    expect(input.value).toBe(NEW_RATE);
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
  });

  const OLD_DATE = "2026-10-07";
  const NEW_DATE = "2026-11-02";

  const dateFields: ItemEditorFields = {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [
      { name: "event_on", value: OLD_DATE },
      { name: "displaytitle", value: OLD_TEXT },
      { name: "notes", value: LONG_NOTE },
    ],
  };

  const dateSchema = {
    fields: [
      {
        name: "event_on",
        label: "Event on",
        control: "sys_CalendarSimple",
        dataType: "date",
      },
      { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
      { name: "notes", label: "Notes", control: "sys_TextArea" },
    ],
  };

  function datePreviewDoc(eventOn = OLD_DATE): Document {
    const previewDoc = document.implementation.createHTMLDocument("preview");
    previewDoc.body.innerHTML = `
      <span data-perc-field="event_on">${eventOn}</span>
      <h1 data-perc-field="displaytitle">${OLD_TEXT}</h1>
      <p data-perc-field="notes">${LONG_NOTE}</p>
    `;
    return previewDoc;
  }

  function renderDateHost(
    previewDoc: Document,
    saveFields: ReturnType<typeof vi.fn>,
    loadFields: ReturnType<typeof vi.fn> = vi.fn().mockResolvedValue(dateFields),
    schema: { fields: Array<Record<string, unknown>> } = dateSchema,
  ): void {
    renderHost("?contentId=42&templateId=7", {
      fetchPreview: vi.fn().mockResolvedValue({
        previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
        contentId: 42,
        templateId: 7,
        revision: 1,
      }),
      loadTemplates: vi.fn().mockResolvedValue([
        {
          name: "rffPgGeneric",
          label: "Generic Page",
          url: "../assembler/render?sys_template=7",
          sortRank: 0,
          menuType: "MENUITEM",
        } satisfies MenuAction,
      ]),
      checkout: vi.fn().mockResolvedValue(undefined),
      loadFields,
      saveFields,
      loadType: vi.fn().mockResolvedValue(schema),
      getPreviewDocument: () => previewDoc,
    });
  }

  function dateInput(previewDoc: Document): HTMLInputElement {
    return previewDoc.querySelector(
      '[data-testid="assembly-inline-field-event_on"]',
    ) as HTMLInputElement;
  }

  it("saves one calendar date and leaves the other fields unchanged", async () => {
    const previewDoc = datePreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderDateHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(dateInput(previewDoc)).toBeTruthy();
    });
    const eventOn = dateInput(previewDoc);
    expect(eventOn.type).toBe("date");
    expect(eventOn.getAttribute("data-assembly-value")).toBe("date");
    expect(eventOn.value).toBe(OLD_DATE);
    eventOn.value = NEW_DATE;
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "event_on")).toEqual({
      name: "event_on",
      value: NEW_DATE,
      dataType: "date",
    });
    expect(saved.fields.find((field) => field.name === "displaytitle")).toEqual({
      name: "displaytitle",
      value: OLD_TEXT,
    });
    expect(saved.fields.find((field) => field.name === "notes")).toEqual({
      name: "notes",
      value: LONG_NOTE,
    });
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    expect(dateInput(previewDoc).value).toBe(NEW_DATE);
  });

  it("reloads the assembly host with the calendar date that was saved", async () => {
    const savedPayload: ItemEditorFields = {
      ...dateFields,
      fields: dateFields.fields.map((field) =>
        field.name === "event_on" ? { ...field, value: NEW_DATE } : field,
      ),
    };
    const previewDoc = datePreviewDoc(NEW_DATE);
    renderDateHost(
      previewDoc,
      vi.fn().mockResolvedValue(savedPayload),
      vi.fn().mockResolvedValue(savedPayload),
    );
    await waitFor(() => {
      expect(dateInput(previewDoc)).toBeTruthy();
    });
    expect(dateInput(previewDoc).value).toBe(NEW_DATE);
    expect(
      (previewDoc.querySelector(
        '[data-testid="assembly-inline-field-displaytitle"]',
      ) as HTMLElement).textContent,
    ).toBe(OLD_TEXT);
  });

  it("Cancel does not write a date edit", async () => {
    const previewDoc = datePreviewDoc();
    const saveFields = vi.fn();
    renderDateHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(dateInput(previewDoc)).toBeTruthy();
    });
    dateInput(previewDoc).value = NEW_DATE;
    fireEvent.click(screen.getByTestId("assembly-field-cancel"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
    expect(dateInput(previewDoc).value).toBe(OLD_DATE);
  });

  it.each(["", "2026-02-31", "2026-10-07 15:30:00"])(
    "does not save %j and leaves the previous date",
    async (next) => {
      const previewDoc = datePreviewDoc();
      const saveFields = vi.fn();
      renderDateHost(previewDoc, saveFields);
      await waitFor(() => {
        expect(dateInput(previewDoc)).toBeTruthy();
      });
      dateInput(previewDoc).value = next;
      fireEvent.click(screen.getByTestId("assembly-field-save"));
      await waitFor(() => {
        expect(screen.getByTestId("assembly-field-error-event_on").textContent).toMatch(
          /calendar date/i,
        );
      });
      expect(saveFields).not.toHaveBeenCalled();
      expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/calendar date/i);
      expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
        /fields saved/i,
      );
      expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
      expect(dateInput(previewDoc).value).toBe(OLD_DATE);
    },
  );

  it("does not write a read-only date field", async () => {
    const previewDoc = datePreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderDateHost(previewDoc, saveFields, vi.fn().mockResolvedValue(dateFields), {
      fields: [
        {
          name: "event_on",
          label: "Event on",
          control: "sys_CalendarSimple",
          dataType: "date",
          readOnly: true,
        },
        { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
        { name: "notes", label: "Notes", control: "sys_TextArea" },
      ],
    });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-inline-displaytitle")).toBeTruthy();
    });
    expect(screen.queryByTestId("assembly-field-chip-event_on")).toBeNull();
    expect(previewDoc.querySelector('[data-testid="assembly-inline-field-event_on"]')).toBeNull();
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    title.textContent = NEW_TEXT;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "event_on")?.value).toBe(OLD_DATE);
    expect(saved.fields.find((field) => field.name === "event_on")?.dataType).toBeUndefined();
    expect(saved.fields.find((field) => field.name === "displaytitle")?.value).toBe(NEW_TEXT);
    expect(saved.fields.find((field) => field.name === "notes")?.value).toBe(LONG_NOTE);
  });

  it("still edits a calendar date when the item also has a datetime", async () => {
    const previewDoc = datePreviewDoc();
    previewDoc.body.insertAdjacentHTML(
      "beforeend",
      `<span data-perc-field="event_at">2026-10-07 15:30:00</span>`,
    );
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderDateHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue({
        ...dateFields,
        fields: [
          ...dateFields.fields,
          { name: "event_at", value: "2026-10-07 15:30:00" },
        ],
      }),
      {
        fields: [
          ...dateSchema.fields,
          {
            name: "event_at",
            label: "Event at",
            control: "sys_CalendarSimple",
            dataType: "datetime",
          },
        ],
      },
    );
    await waitFor(() => {
      expect(dateInput(previewDoc)).toBeTruthy();
    });
    const eventAt = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-event_at"]',
    ) as HTMLInputElement;
    expect(eventAt.type).toBe("datetime-local");
    expect(eventAt.value).toBe("2026-10-07T15:30");
    expect(screen.getByTestId("assembly-field-inline-event_at")).toBeTruthy();
    dateInput(previewDoc).value = NEW_DATE;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "event_on")?.value).toBe(NEW_DATE);
    expect(saved.fields.find((field) => field.name === "event_at")).toEqual({
      name: "event_at",
      value: "2026-10-07 15:30:00",
    });
  });

  it.each([400, 403, 409])(
    "HTTP %s leaves the previous date in place",
    async (status) => {
      const previewDoc = datePreviewDoc();
      const saveFields = vi.fn().mockRejectedValue({ status });
      renderDateHost(previewDoc, saveFields);
      await waitFor(() => {
        expect(dateInput(previewDoc)).toBeTruthy();
      });
      dateInput(previewDoc).value = NEW_DATE;
      fireEvent.click(screen.getByTestId("assembly-field-save"));
      await waitFor(() => {
        expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(
          /could not save/i,
        );
        expect(dateInput(previewDoc).value).toBe(OLD_DATE);
      });
      expect(saveFields).toHaveBeenCalled();
      const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
      expect(sent.fields.find((field) => field.name === "event_on")?.value).toBe(NEW_DATE);
      expect(sent.fields.find((field) => field.name === "event_on")?.dataType).toBe("date");
      expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
        /fields saved/i,
      );
    },
  );

  it("saves a calendar date from the overlay strip when the page has no node", async () => {
    const previewDoc = document.implementation.createHTMLDocument("empty");
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderDateHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(screen.getByTestId("assembly-overlay-field-event_on")).toBeTruthy();
    });
    const input = screen.getByTestId("assembly-overlay-field-event_on") as HTMLInputElement;
    expect(input.tagName).toBe("INPUT");
    expect(input.type).toBe("date");
    expect(input.getAttribute("data-assembly-value")).toBe("date");
    expect(input.value).toBe(OLD_DATE);
    input.value = NEW_DATE;
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "event_on")).toEqual({
      name: "event_on",
      value: NEW_DATE,
      dataType: "date",
    });
    expect(saved.fields.find((field) => field.name === "displaytitle")?.value).toBe(OLD_TEXT);
    expect(input.value).toBe(NEW_DATE);
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
  });

  function requiredDateSchema(required = true) {
    return {
      fields: dateSchema.fields.map((field) =>
        field.name === "event_on" ? { ...field, required } : field,
      ),
    };
  }

  it("does not save a blank required date and reloads the previous date", async () => {
    const previewDoc = datePreviewDoc();
    const saveFields = vi.fn();
    renderDateHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(dateFields),
      requiredDateSchema(),
    );
    await waitFor(() => {
      expect(dateInput(previewDoc)).toBeTruthy();
    });
    const eventOn = dateInput(previewDoc);
    expect(eventOn.getAttribute("aria-required")).toBe("true");
    expect(eventOn.getAttribute("data-assembly-required")).toBe("true");
    expect(
      screen.getByTestId("assembly-field-chip-event_on").getAttribute("data-required"),
    ).toBe("true");
    eventOn.value = "";
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-event_on").textContent).toMatch(
        /required/i,
      );
      expect(dateInput(previewDoc).getAttribute("aria-invalid")).toBe("true");
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/required/i);
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
      /calendar date/i,
    );
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
      /fields saved/i,
    );
    expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
    expect(dateInput(previewDoc).value).toBe("");
    cleanup();
    const reloaded = datePreviewDoc();
    renderDateHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue(dateFields),
      requiredDateSchema(),
    );
    await waitFor(() => {
      expect(dateInput(reloaded).value).toBe(OLD_DATE);
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
  });

  it("does not save whitespace-only required date and keeps the previous value", async () => {
    const previewDoc = datePreviewDoc();
    const saveFields = vi.fn();
    renderDateHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(dateFields),
      requiredDateSchema(),
    );
    await waitFor(() => {
      expect(dateInput(previewDoc)).toBeTruthy();
    });
    dateInput(previewDoc).value = "  \n  ";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-event_on").textContent).toMatch(
        /required/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
      /fields saved/i,
    );
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
      /calendar date/i,
    );
    cleanup();
    const reloaded = datePreviewDoc();
    renderDateHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue(dateFields),
      requiredDateSchema(),
    );
    await waitFor(() => {
      expect(dateInput(reloaded).value).toBe(OLD_DATE);
    });
  });

  it("does not write when Cancel leaves a blank required date edit", async () => {
    const previewDoc = datePreviewDoc();
    const saveFields = vi.fn();
    renderDateHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(dateFields),
      requiredDateSchema(),
    );
    await waitFor(() => {
      expect(dateInput(previewDoc)).toBeTruthy();
    });
    dateInput(previewDoc).value = "";
    fireEvent.click(screen.getByTestId("assembly-field-cancel"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
    expect(dateInput(previewDoc).value).toBe(OLD_DATE);
    expect(screen.queryByTestId("assembly-field-error-event_on")).toBeNull();
  });

  it("still saves a non-blank required calendar date", async () => {
    let eventOnValue = OLD_DATE;
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => {
      eventOnValue =
        body.fields.find((field) => field.name === "event_on")?.value ?? eventOnValue;
      return {
        ...dateFields,
        fields: dateFields.fields.map((field) =>
          field.name === "event_on" ? { ...field, value: eventOnValue } : field,
        ),
      };
    });
    const previewDoc = datePreviewDoc();
    renderDateHost(
      previewDoc,
      saveFields,
      vi.fn(async () => ({
        ...dateFields,
        fields: dateFields.fields.map((field) =>
          field.name === "event_on" ? { ...field, value: eventOnValue } : field,
        ),
      })),
      requiredDateSchema(),
    );
    await waitFor(() => {
      expect(dateInput(previewDoc)).toBeTruthy();
    });
    dateInput(previewDoc).value = NEW_DATE;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    });
    const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(sent.fields.find((field) => field.name === "event_on")).toEqual({
      name: "event_on",
      value: NEW_DATE,
      dataType: "date",
    });
    expect(sent.fields.find((field) => field.name === "displaytitle")?.value).toBe(OLD_TEXT);
    expect(sent.fields.find((field) => field.name === "notes")?.value).toBe(LONG_NOTE);
    expect(screen.queryByTestId("assembly-field-error-event_on")).toBeNull();
    cleanup();
    const reloaded = datePreviewDoc(NEW_DATE);
    renderDateHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue({
        ...dateFields,
        fields: dateFields.fields.map((field) =>
          field.name === "event_on" ? { ...field, value: NEW_DATE } : field,
        ),
      }),
      requiredDateSchema(),
    );
    await waitFor(() => {
      expect(dateInput(reloaded).value).toBe(NEW_DATE);
    });
  });

  it.each([400, 403, 409])(
    "HTTP %s on a required date does not claim success",
    async (status) => {
      const previewDoc = datePreviewDoc();
      const saveFields = vi.fn().mockRejectedValue({ status });
      renderDateHost(
        previewDoc,
        saveFields,
        vi.fn().mockResolvedValue(dateFields),
        requiredDateSchema(),
      );
      await waitFor(() => {
        expect(dateInput(previewDoc)).toBeTruthy();
      });
      dateInput(previewDoc).value = NEW_DATE;
      fireEvent.click(screen.getByTestId("assembly-field-save"));
      await waitFor(() => {
        expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(
          /could not save/i,
        );
        expect(dateInput(previewDoc).value).toBe(OLD_DATE);
      });
      expect(saveFields).toHaveBeenCalled();
      expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
        /fields saved/i,
      );
      expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
      expect(screen.queryByTestId("assembly-field-error-event_on")).toBeNull();
    },
  );

  it("refuses a blank required date on the overlay strip", async () => {
    const previewDoc = document.implementation.createHTMLDocument("empty");
    const saveFields = vi.fn();
    renderDateHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(dateFields),
      requiredDateSchema(),
    );
    await waitFor(() => {
      expect(screen.getByTestId("assembly-overlay-field-event_on")).toBeTruthy();
    });
    const input = screen.getByTestId("assembly-overlay-field-event_on") as HTMLInputElement;
    expect(input.getAttribute("aria-required")).toBe("true");
    expect(input.getAttribute("data-assembly-required")).toBe("true");
    input.value = "   ";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-event_on").textContent).toMatch(
        /required/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
      /fields saved/i,
    );
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
      /calendar date/i,
    );
    expect(input.getAttribute("aria-invalid")).toBe("true");
  });

  it("still refuses an optional blank date instead of clearing it", async () => {
    const previewDoc = datePreviewDoc();
    const saveFields = vi.fn();
    renderDateHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(dateFields),
      requiredDateSchema(false),
    );
    await waitFor(() => {
      expect(dateInput(previewDoc)).toBeTruthy();
    });
    dateInput(previewDoc).value = "";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-event_on").textContent).toMatch(
        /calendar date/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/required/i);
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(
      /fields saved/i,
    );
    expect(dateInput(previewDoc).value).toBe(OLD_DATE);
  });
});

describe("AssemblyHost datetime field", () => {
  const OLD_AT = "2026-10-07 15:30:00";
  const OLD_WIDGET = "2026-10-07T15:30";
  const NEW_AT = "2026-11-02 09:05:00";
  const NEW_WIDGET = "2026-11-02T09:05";
  const TITLE = "Welcome";
  const NEW_TITLE = "Updated welcome";
  const LONG_NOTE = "A long note";

  const datetimeFields: ItemEditorFields = {
    contentId: "42",
    contentType: "percPage",
    name: "Home",
    checkoutUser: "admin",
    fields: [
      { name: "event_at", value: OLD_AT },
      { name: "displaytitle", value: TITLE },
      { name: "notes", value: LONG_NOTE },
    ],
  };

  const datetimeSchema = {
    fields: [
      {
        name: "event_at",
        label: "Event at",
        control: "sys_CalendarSimple",
        dataType: "datetime",
      },
      { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
      { name: "notes", label: "Notes", control: "sys_TextArea" },
    ],
  };

  function datetimePreviewDoc(eventAt = OLD_AT): Document {
    const previewDoc = document.implementation.createHTMLDocument("preview");
    previewDoc.body.innerHTML = `
      <span data-perc-field="event_at">${eventAt}</span>
      <h1 data-perc-field="displaytitle">${TITLE}</h1>
      <p data-perc-field="notes">${LONG_NOTE}</p>
    `;
    return previewDoc;
  }

  function renderDatetimeHost(
    previewDoc: Document,
    saveFields: ReturnType<typeof vi.fn>,
    loadFields: ReturnType<typeof vi.fn> = vi.fn().mockResolvedValue(datetimeFields),
    schema: { fields: Array<Record<string, unknown>> } = datetimeSchema,
  ): void {
    renderHost("?contentId=42&templateId=7", {
      fetchPreview: vi.fn().mockResolvedValue({
        previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
        contentId: 42,
        templateId: 7,
        revision: 1,
      }),
      loadTemplates: vi.fn().mockResolvedValue([
        {
          name: "rffPgGeneric",
          label: "Generic Page",
          url: "../assembler/render?sys_template=7",
          sortRank: 0,
          menuType: "MENUITEM",
        } satisfies MenuAction,
      ]),
      checkout: vi.fn().mockResolvedValue(undefined),
      loadFields,
      saveFields,
      loadType: vi.fn().mockResolvedValue(schema),
      getPreviewDocument: () => previewDoc,
    });
  }

  function datetimeInput(previewDoc: Document): HTMLInputElement {
    return previewDoc.querySelector(
      '[data-testid="assembly-inline-field-event_at"]',
    ) as HTMLInputElement;
  }

  function requiredDatetimeSchema(required = true) {
    return {
      fields: datetimeSchema.fields.map((field) =>
        field.name === "event_at" ? { ...field, required } : field,
      ),
    };
  }

  it("saves one datetime and leaves the other fields unchanged", async () => {
    const previewDoc = datetimePreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderDatetimeHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(datetimeInput(previewDoc)).toBeTruthy();
    });
    const eventAt = datetimeInput(previewDoc);
    expect(eventAt.type).toBe("datetime-local");
    expect(eventAt.getAttribute("data-assembly-value")).toBe("datetime");
    expect(eventAt.value).toBe(OLD_WIDGET);
    eventAt.value = NEW_WIDGET;
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "event_at")).toEqual({
      name: "event_at",
      value: NEW_AT,
      dataType: "datetime",
    });
    expect(saved.fields.find((field) => field.name === "displaytitle")).toEqual({
      name: "displaytitle",
      value: TITLE,
    });
    expect(saved.fields.find((field) => field.name === "notes")).toEqual({
      name: "notes",
      value: LONG_NOTE,
    });
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    expect(datetimeInput(previewDoc).value).toBe(NEW_WIDGET);
  });

  it("reloads the assembly host with the datetime that was saved", async () => {
    const savedPayload: ItemEditorFields = {
      ...datetimeFields,
      fields: datetimeFields.fields.map((field) =>
        field.name === "event_at" ? { ...field, value: NEW_AT } : field,
      ),
    };
    const previewDoc = datetimePreviewDoc(NEW_AT);
    renderDatetimeHost(
      previewDoc,
      vi.fn().mockResolvedValue(savedPayload),
      vi.fn().mockResolvedValue(savedPayload),
    );
    await waitFor(() => {
      expect(datetimeInput(previewDoc)).toBeTruthy();
    });
    expect(datetimeInput(previewDoc).value).toBe(NEW_WIDGET);
    expect(
      (previewDoc.querySelector(
        '[data-testid="assembly-inline-field-displaytitle"]',
      ) as HTMLElement).textContent,
    ).toBe(TITLE);
  });

  it("Cancel does not write a datetime edit", async () => {
    const previewDoc = datetimePreviewDoc();
    const saveFields = vi.fn();
    renderDatetimeHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(datetimeInput(previewDoc)).toBeTruthy();
    });
    datetimeInput(previewDoc).value = NEW_WIDGET;
    fireEvent.click(screen.getByTestId("assembly-field-cancel"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
    expect(datetimeInput(previewDoc).value).toBe(OLD_WIDGET);
  });

  it("does not save an invalid datetime and leaves the previous value", async () => {
    const previewDoc = datetimePreviewDoc();
    const saveFields = vi.fn();
    renderDatetimeHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(datetimeInput(previewDoc)).toBeTruthy();
    });
    const eventAt = datetimeInput(previewDoc);
    // A native datetime-local control rejects free text. Force the string
    // through so the save gate still refuses a value that is not a date and time.
    eventAt.type = "text";
    eventAt.value = "not-a-date";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-event_at").textContent).toMatch(
        /date and time/i,
      );
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/date and time/i);
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
    expect(datetimeInput(previewDoc).value).toBe(OLD_WIDGET);
  });

  it("does not write a read-only datetime field", async () => {
    const previewDoc = datetimePreviewDoc();
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderDatetimeHost(previewDoc, saveFields, vi.fn().mockResolvedValue(datetimeFields), {
      fields: [
        {
          name: "event_at",
          label: "Event at",
          control: "sys_CalendarSimple",
          dataType: "datetime",
          readOnly: true,
        },
        { name: "displaytitle", label: "Display title", control: "sys_EditBox" },
        { name: "notes", label: "Notes", control: "sys_TextArea" },
      ],
    });
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-inline-displaytitle")).toBeTruthy();
    });
    expect(screen.queryByTestId("assembly-field-chip-event_at")).toBeNull();
    expect(previewDoc.querySelector('[data-testid="assembly-inline-field-event_at"]')).toBeNull();
    const title = previewDoc.querySelector(
      '[data-testid="assembly-inline-field-displaytitle"]',
    ) as HTMLElement;
    title.textContent = NEW_TITLE;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "event_at")?.value).toBe(OLD_AT);
    expect(saved.fields.find((field) => field.name === "event_at")?.dataType).toBeUndefined();
    expect(saved.fields.find((field) => field.name === "displaytitle")?.value).toBe(NEW_TITLE);
    expect(saved.fields.find((field) => field.name === "notes")?.value).toBe(LONG_NOTE);
  });

  it.each([400, 403, 409])("HTTP %s leaves the previous datetime in place", async (status) => {
    const previewDoc = datetimePreviewDoc();
    const saveFields = vi.fn().mockRejectedValue({ status });
    renderDatetimeHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(datetimeInput(previewDoc)).toBeTruthy();
    });
    datetimeInput(previewDoc).value = NEW_WIDGET;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/could not save/i);
      expect(datetimeInput(previewDoc).value).toBe(OLD_WIDGET);
    });
    expect(saveFields).toHaveBeenCalled();
    const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(sent.fields.find((field) => field.name === "event_at")?.value).toBe(NEW_AT);
    expect(sent.fields.find((field) => field.name === "event_at")?.dataType).toBe("datetime");
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
  });

  it("saves a datetime from the overlay strip when the page has no node", async () => {
    const previewDoc = document.implementation.createHTMLDocument("empty");
    const saveFields = vi.fn().mockImplementation(async (_id: string, body: ItemEditorFields) => body);
    renderDatetimeHost(previewDoc, saveFields);
    await waitFor(() => {
      expect(screen.getByTestId("assembly-overlay-field-event_at")).toBeTruthy();
    });
    const input = screen.getByTestId("assembly-overlay-field-event_at") as HTMLInputElement;
    expect(input.type).toBe("datetime-local");
    expect(input.getAttribute("data-assembly-value")).toBe("datetime");
    expect(input.value).toBe(OLD_WIDGET);
    input.value = NEW_WIDGET;
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(saveFields).toHaveBeenCalled();
    });
    const saved = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(saved.fields.find((field) => field.name === "event_at")).toEqual({
      name: "event_at",
      value: NEW_AT,
      dataType: "datetime",
    });
    expect(saved.fields.find((field) => field.name === "displaytitle")?.value).toBe(TITLE);
    expect(input.value).toBe(NEW_WIDGET);
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
  });

  it("does not save a blank required datetime and reloads the previous value", async () => {
    const previewDoc = datetimePreviewDoc();
    const saveFields = vi.fn();
    renderDatetimeHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(datetimeFields),
      requiredDatetimeSchema(),
    );
    await waitFor(() => {
      expect(datetimeInput(previewDoc)).toBeTruthy();
    });
    const eventAt = datetimeInput(previewDoc);
    expect(eventAt.getAttribute("aria-required")).toBe("true");
    expect(eventAt.getAttribute("data-assembly-required")).toBe("true");
    expect(screen.getByTestId("assembly-field-chip-event_at").getAttribute("data-required")).toBe(
      "true",
    );
    eventAt.value = "";
    expect(saveFields).not.toHaveBeenCalled();
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-error-event_at").textContent).toMatch(/required/i);
      expect(datetimeInput(previewDoc).getAttribute("aria-invalid")).toBe("true");
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/required/i);
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/date and time/i);
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/fields saved/i);
    expect(screen.getByTestId("assembly-field-notice").getAttribute("role")).toBe("alert");
    expect(datetimeInput(previewDoc).value).toBe("");
    cleanup();
    const reloaded = datetimePreviewDoc();
    renderDatetimeHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue(datetimeFields),
      requiredDatetimeSchema(),
    );
    await waitFor(() => {
      expect(datetimeInput(reloaded).value).toBe(OLD_WIDGET);
    });
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
  });

  it("does not write when Cancel leaves a blank required datetime edit", async () => {
    const previewDoc = datetimePreviewDoc();
    const saveFields = vi.fn();
    renderDatetimeHost(
      previewDoc,
      saveFields,
      vi.fn().mockResolvedValue(datetimeFields),
      requiredDatetimeSchema(),
    );
    await waitFor(() => {
      expect(datetimeInput(previewDoc)).toBeTruthy();
    });
    datetimeInput(previewDoc).value = "";
    fireEvent.click(screen.getByTestId("assembly-field-cancel"));
    expect(saveFields).not.toHaveBeenCalled();
    expect(screen.queryByTestId("assembly-field-notice")).toBeNull();
    expect(datetimeInput(previewDoc).value).toBe(OLD_WIDGET);
    expect(screen.queryByTestId("assembly-field-error-event_at")).toBeNull();
  });

  it("still saves a non-blank required datetime", async () => {
    let eventAtValue = OLD_AT;
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => {
      eventAtValue = body.fields.find((field) => field.name === "event_at")?.value ?? eventAtValue;
      return {
        ...datetimeFields,
        fields: datetimeFields.fields.map((field) =>
          field.name === "event_at" ? { ...field, value: eventAtValue } : field,
        ),
      };
    });
    const previewDoc = datetimePreviewDoc();
    renderDatetimeHost(
      previewDoc,
      saveFields,
      vi.fn(async () => ({
        ...datetimeFields,
        fields: datetimeFields.fields.map((field) =>
          field.name === "event_at" ? { ...field, value: eventAtValue } : field,
        ),
      })),
      requiredDatetimeSchema(),
    );
    await waitFor(() => {
      expect(datetimeInput(previewDoc)).toBeTruthy();
    });
    datetimeInput(previewDoc).value = NEW_WIDGET;
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    });
    const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(sent.fields.find((field) => field.name === "event_at")).toEqual({
      name: "event_at",
      value: NEW_AT,
      dataType: "datetime",
    });
    expect(sent.fields.find((field) => field.name === "displaytitle")?.value).toBe(TITLE);
    expect(screen.queryByTestId("assembly-field-error-event_at")).toBeNull();
    cleanup();
    const reloaded = datetimePreviewDoc(NEW_AT);
    renderDatetimeHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue({
        ...datetimeFields,
        fields: datetimeFields.fields.map((field) =>
          field.name === "event_at" ? { ...field, value: NEW_AT } : field,
        ),
      }),
      requiredDatetimeSchema(),
    );
    await waitFor(() => {
      expect(datetimeInput(reloaded).value).toBe(NEW_WIDGET);
    });
  });

  it("clears an optional datetime and reloads it empty", async () => {
    let eventAtValue = OLD_AT;
    const saveFields = vi.fn(async (_id: string, body: ItemEditorFields) => {
      eventAtValue = body.fields.find((field) => field.name === "event_at")?.value ?? eventAtValue;
      return {
        ...datetimeFields,
        fields: datetimeFields.fields.map((field) =>
          field.name === "event_at" ? { ...field, value: eventAtValue } : field,
        ),
      };
    });
    const previewDoc = datetimePreviewDoc();
    renderDatetimeHost(
      previewDoc,
      saveFields,
      vi.fn(async () => ({
        ...datetimeFields,
        fields: datetimeFields.fields.map((field) =>
          field.name === "event_at" ? { ...field, value: eventAtValue } : field,
        ),
      })),
      requiredDatetimeSchema(false),
    );
    await waitFor(() => {
      expect(datetimeInput(previewDoc)).toBeTruthy();
    });
    datetimeInput(previewDoc).value = "";
    fireEvent.click(screen.getByTestId("assembly-field-save"));
    await waitFor(() => {
      expect(screen.getByTestId("assembly-field-notice").textContent).toMatch(/saved/i);
    });
    const sent = saveFields.mock.calls[0]?.[1] as ItemEditorFields;
    expect(sent.fields.find((field) => field.name === "event_at")).toEqual({
      name: "event_at",
      value: "",
      dataType: "datetime",
    });
    expect(sent.fields.find((field) => field.name === "displaytitle")?.value).toBe(TITLE);
    expect(screen.getByTestId("assembly-field-notice").textContent).not.toMatch(/required/i);
    expect(datetimeInput(previewDoc).value).toBe("");
    cleanup();
    const reloaded = datetimePreviewDoc("");
    renderDatetimeHost(
      reloaded,
      saveFields,
      vi.fn().mockResolvedValue({
        ...datetimeFields,
        fields: datetimeFields.fields.map((field) =>
          field.name === "event_at" ? { ...field, value: "" } : field,
        ),
      }),
      requiredDatetimeSchema(false),
    );
    await waitFor(() => {
      expect(datetimeInput(reloaded).value).toBe("");
    });
  });
});
