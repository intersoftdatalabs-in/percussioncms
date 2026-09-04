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

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  VIEW_DESIGN_GAPS,
  VIEW_TYPE_CUSTOM,
  VIEW_TYPE_STANDARD,
  createView,
  deleteView,
  getViewDetail,
  isCustomViewType,
  isInboxViewName,
  isPackagedCxViewName,
  isProtectedViewWrite,
  isValidViewName,
  isValidViewUrl,
  isViewWriteReady,
  normalizeViewName,
  normalizeViewUrl,
  saveView,
  unwrapViewDef,
  unwrapViewDefList,
  withoutStaleViewWriteGap,
  wrapViewDefForWire,
} from "../../../../main/ts/api/developer/viewsApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("unwrapViewDef (#3380)", () => {
  it("unwraps Jackson ViewDef root envelope so guid is reachable", () => {
    const unwrapped = unwrapViewDef({
      ViewDef: {
        name: "Inbox",
        id: 3,
        guid: { stringValue: "0-18-3", type: 18, uuid: 3 },
      },
    });
    expect(unwrapped.name).toBe("Inbox");
    expect(unwrapped.guid?.stringValue).toBe("0-18-3");
    expect(unwrapped.guidString).toBe("0-18-3");
  });

  it("synthesizes 0-18-{id} when Guid is omitted", () => {
    const unwrapped = unwrapViewDef({ name: "Inbox", id: 11 });
    expect(unwrapped.guid?.stringValue).toBe("0-18-11");
    expect(unwrapped.guidString).toBe("0-18-11");
  });

  it("uses nested Guid envelope when stringValue is nested", () => {
    const unwrapped = unwrapViewDef({
      name: "Inbox",
      guid: { Guid: { stringValue: "0-18-4", type: 18, uuid: 4 } },
    });
    expect(unwrapped.guidString).toBe("0-18-4");
  });

  it("returns empty object for null payload", () => {
    expect(unwrapViewDef(null)).toEqual({});
  });
});

describe("unwrapViewDefList", () => {
  it("normalizes each list row GUID", () => {
    const list = unwrapViewDefList({
      ViewDef: [
        { name: "Inbox", id: 3 },
        { name: "Outbox", guidString: "0-18-4" },
      ],
    });
    expect(list).toHaveLength(2);
    expect(list[0].guidString).toBe("0-18-3");
    expect(list[1].guidString).toBe("0-18-4");
  });
});

describe("view name validation", () => {
  it("trims names", () => {
    expect(normalizeViewName("  MyView  ")).toBe("MyView");
    expect(normalizeViewName("")).toBe("");
    expect(normalizeViewName(null)).toBe("");
  });

  it("accepts REST-safe create names and rejects junk", () => {
    expect(isValidViewName("MyView")).toBe(true);
    expect(isValidViewName("qa4085")).toBe(true);
    expect(isValidViewName("  StandardOne  ")).toBe(true);
    expect(isValidViewName("")).toBe(false);
    expect(isValidViewName("has space")).toBe(false);
    expect(isValidViewName("wild*card")).toBe(false);
    expect(isValidViewName("pct%name")).toBe(false);
    expect(isValidViewName("../x")).toBe(false);
    expect(isValidViewName("a/b")).toBe(false);
    expect(isValidViewName("a\\b")).toBe(false);
  });

  it("disables write until the view name is valid on create", () => {
    expect(isViewWriteReady({ isNew: true, name: "" })).toBe(false);
    expect(isViewWriteReady({ isNew: true, name: "has space" })).toBe(false);
    expect(isViewWriteReady({ isNew: true, name: "MyView" })).toBe(true);
    expect(isViewWriteReady({ isNew: false, name: "MyView" })).toBe(true);
    expect(isViewWriteReady({ isNew: false, name: "" })).toBe(false);
  });

  it("requires a classic relative URL for CustomView writes", () => {
    expect(
      isViewWriteReady({
        isNew: true,
        name: "MyCustom",
        type: VIEW_TYPE_CUSTOM,
        url: "",
      }),
    ).toBe(false);
    expect(
      isViewWriteReady({
        isNew: true,
        name: "MyCustom",
        type: VIEW_TYPE_CUSTOM,
        url: "../sys_cxViews/myapp.xml",
      }),
    ).toBe(true);
    expect(
      isViewWriteReady({
        isNew: false,
        name: "MyCustom",
        type: "custom",
        url: "sys_cxViews/myapp.xml",
      }),
    ).toBe(true);
    expect(isViewWriteReady({ isNew: true, name: "MyView", type: VIEW_TYPE_STANDARD })).toBe(
      true,
    );
  });
});

describe("custom view URL validation", () => {
  it("detects CustomView type aliases", () => {
    expect(isCustomViewType(VIEW_TYPE_CUSTOM)).toBe(true);
    expect(isCustomViewType("custom")).toBe(true);
    expect(isCustomViewType(VIEW_TYPE_STANDARD)).toBe(false);
    expect(isCustomViewType("")).toBe(false);
  });

  it("accepts classic relative URLs and rejects schemes / traversal", () => {
    expect(normalizeViewUrl("  ../sys_cxViews/myapp.xml  ")).toBe("../sys_cxViews/myapp.xml");
    expect(isValidViewUrl("../sys_cxViews/myapp.xml")).toBe(true);
    expect(isValidViewUrl("sys_cxViews/myapp.xml")).toBe(true);
    expect(isValidViewUrl("")).toBe(false);
    expect(isValidViewUrl("https://example.com/x")).toBe(false);
    expect(isValidViewUrl("//host/path")).toBe(false);
    expect(isValidViewUrl("../../etc/passwd")).toBe(false);
    expect(isValidViewUrl("has space")).toBe(false);
    expect(isValidViewUrl("<enter url>")).toBe(false);
  });
});

describe("protected view write", () => {
  it("treats Inbox-family names as protected", () => {
    expect(isInboxViewName("Inbox")).toBe(true);
    expect(isInboxViewName("inbox")).toBe(true);
    expect(isInboxViewName("//Views//MyContent/Inbox")).toBe(true);
    expect(isInboxViewName("MyView")).toBe(false);
  });

  it("protects packaged sys_cxViews keys but not user custom URL views", () => {
    expect(isPackagedCxViewName("Inbox")).toBe(true);
    expect(isPackagedCxViewName("Outbox")).toBe(true);
    expect(isPackagedCxViewName("Checked Out By Me")).toBe(true);
    expect(isPackagedCxViewName("MyCustom")).toBe(false);
    expect(isProtectedViewWrite({ name: "Inbox" })).toBe(true);
    expect(isProtectedViewWrite({ name: "Outbox", customView: true })).toBe(true);
    expect(isProtectedViewWrite({ name: "Recent", url: "../sys_cxViews/recent.xml" })).toBe(true);
    expect(isProtectedViewWrite({ name: "MyView" })).toBe(false);
    expect(
      isProtectedViewWrite({
        name: "MyCustom",
        customView: true,
        url: "../sys_cxViews/myapp.xml",
      }),
    ).toBe(false);
  });
});

describe("view wire wrap", () => {
  it("wraps POST/PUT under ViewDef root", () => {
    expect(
      wrapViewDefForWire({
        name: "MyView",
        label: "My View",
        description: "Created via REST",
        type: "View",
        displayFormatId: "1",
      }),
    ).toEqual({
      ViewDef: {
        name: "MyView",
        label: "My View",
        description: "Created via REST",
        type: "View",
        displayFormatId: "1",
      },
    });
  });

  it("drops the create/update/delete and field-criterion gaps from VIEW_DESIGN_GAPS", () => {
    expect(VIEW_DESIGN_GAPS.some((g) => /create/i.test(g))).toBe(false);
    expect(VIEW_DESIGN_GAPS.some((g) => /field criterion/i.test(g))).toBe(false);
    expect(VIEW_DESIGN_GAPS.some((g) => /Inbox-family/i.test(g))).toBe(true);
    expect(VIEW_DESIGN_GAPS.some((g) => /packaged sys_cxViews/i.test(g))).toBe(true);
  });

  it("filters a stale REST write, field-criterion, and pre-UI-07 custom URL gap on GET detail", () => {
    expect(
      withoutStaleViewWriteGap([
        "View create / update / delete not supported via this API",
        "View field criterion editing not supported via this API",
        "Inbox-family and custom URL views cannot be updated or deleted via this API",
        "Inbox-family and packaged sys_cxViews views cannot be updated or deleted via this API",
      ]),
    ).toEqual([
      "Inbox-family and packaged sys_cxViews views cannot be updated or deleted via this API",
    ]);
  });

  it("does not drop a similar substring that is not the exact stale gap", () => {
    expect(
      withoutStaleViewWriteGap([
        "View create / update / delete must run in sequence",
        "Inbox-family and packaged sys_cxViews views cannot be updated or deleted via this API",
      ]),
    ).toEqual([
      "View create / update / delete must run in sequence",
      "Inbox-family and packaged sys_cxViews views cannot be updated or deleted via this API",
    ]);
  });

  it("wraps custom URL create body with url and customView", () => {
    expect(
      wrapViewDefForWire({
        name: "MyCustom",
        label: "My Custom",
        type: VIEW_TYPE_CUSTOM,
        customView: true,
        url: "../sys_cxViews/myapp.xml",
      }),
    ).toEqual({
      ViewDef: {
        name: "MyCustom",
        label: "My Custom",
        type: VIEW_TYPE_CUSTOM,
        customView: true,
        url: "../sys_cxViews/myapp.xml",
      },
    });
  });
});

describe("viewsApi write paths", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  function jsonResponse(body: unknown, status = 200): Response {
    return new Response(JSON.stringify(body), {
      status,
      headers: { "Content-Type": "application/json" },
    });
  }

  it("POSTs create body to /services/views", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ name: "MyView", label: "My View", type: "View" }),
    );
    const saved = await createView({
      name: "MyView",
      label: "My View",
      type: "View",
    });
    expect(saved.name).toBe("MyView");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toContain(PATHS.VIEWS);
    expect(JSON.parse(String(init.body))).toEqual({
      ViewDef: { name: "MyView", label: "My View", type: "View" },
    });
  });

  it("POSTs custom URL create with url and customView", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        name: "MyCustom",
        label: "My Custom",
        type: VIEW_TYPE_CUSTOM,
        customView: true,
        url: "../sys_cxViews/myapp.xml",
      }),
    );
    const saved = await createView({
      name: "MyCustom",
      label: "My Custom",
      type: VIEW_TYPE_CUSTOM,
      customView: true,
      url: "../sys_cxViews/myapp.xml",
    });
    expect(saved.customView).toBe(true);
    expect(saved.url).toBe("../sys_cxViews/myapp.xml");
    expect(JSON.parse(String((fetchMock.mock.calls[0][1] as RequestInit).body))).toEqual({
      ViewDef: {
        name: "MyCustom",
        label: "My Custom",
        type: VIEW_TYPE_CUSTOM,
        customView: true,
        url: "../sys_cxViews/myapp.xml",
      },
    });
  });

  it("PUTs save body to /services/views/{idOrName}", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ name: "MyView", label: "Updated" }));
    const saved = await saveView("MyView", { name: "MyView", label: "Updated" });
    expect(saved.label).toBe("Updated");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.VIEWS}/MyView`);
  });

  it("DELETEs /services/views/{idOrName}", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await deleteView("MyView");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.VIEWS}/MyView`);
  });

  it("unwraps GET /services/views/{idOrName} Jackson root", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ ViewDef: { name: "MyView", label: "My View", fields: [] } }),
    );
    const detail = await getViewDetail("MyView");
    expect(detail.name).toBe("MyView");
    expect(detail.label).toBe("My View");
  });

  it("surfaces 400 invalid name", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ message: "name cannot contain whitespace" }, 400),
    );
    await expect(createView({ name: "bad name" })).rejects.toMatchObject({
      status: 400,
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("surfaces 409 duplicate name", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ message: "View already exists: MyView" }, 409),
    );
    await expect(createView({ name: "MyView" })).rejects.toMatchObject({
      status: 409,
    });
  });

  it("surfaces 404 missing view", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "View not found" }, 404));
    await expect(getViewDetail("missing")).rejects.toMatchObject({ status: 404 });
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "View not found" }, 404));
    await expect(deleteView("missing")).rejects.toMatchObject({ status: 404 });
  });

  it("surfaces 403 non-Admin create", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Admin role required" }, 403));
    await expect(createView({ name: "MyView" })).rejects.toMatchObject({
      status: 403,
    });
  });
});
