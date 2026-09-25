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

import { afterEach, describe, expect, it, vi } from "vitest";
import {
  canPublishFromEditor,
  canTakedownFromEditor,
  canViewPublishHistoryFromEditor,
  formatEditorTakedownConfirm,
  publishEditorItem,
  resolveEditorPublishKind,
  takedownEditorItem,
} from "../../../main/ts/editor/editorPublish";

afterEach(() => {
  vi.restoreAllMocks();
});

describe("resolveEditorPublishKind", () => {
  it("classifies percPage as page when an id is present", () => {
    expect(resolveEditorPublishKind("percPage", { id: "42" })).toBe("page");
  });

  it("classifies percRichText and percImageAsset as assets", () => {
    expect(resolveEditorPublishKind("percRichText", { id: "9" })).toBe("asset");
    expect(resolveEditorPublishKind("percImageAsset", { id: "9" })).toBe("asset");
  });

  it("classifies percBlogPost as a page", () => {
    expect(resolveEditorPublishKind("percBlogPost", { id: "7" })).toBe("page");
  });

  it("uses allowed templates as a page hint for custom types", () => {
    expect(
      resolveEditorPublishKind("acmeLanding", {
        id: "3",
        allowedTemplateCount: 2,
      }),
    ).toBe("page");
  });

  it("returns none without an id, content type, or for templates", () => {
    expect(resolveEditorPublishKind("percPage", { id: "" })).toBe("none");
    expect(resolveEditorPublishKind("", { id: "42" })).toBe("none");
    expect(resolveEditorPublishKind("percTemplate", { id: "42" })).toBe("none");
  });
});

describe("canPublishFromEditor", () => {
  it("is true only in edit mode for page or asset", () => {
    expect(canPublishFromEditor("edit", "page")).toBe(true);
    expect(canPublishFromEditor("edit", "asset")).toBe(true);
    expect(canPublishFromEditor("view", "page")).toBe(false);
    expect(canPublishFromEditor("promote", "asset")).toBe(false);
    expect(canPublishFromEditor("edit", "none")).toBe(false);
  });
});

describe("publishEditorItem", () => {
  it("GETs sitemanage publish/page for a page", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    expect(await publishEditorItem("42", "page")).toBe(true);
    const url = String(global.fetch.mock.calls[0]?.[0] ?? "");
    expect(url).toContain("sitemanage/publish/page/42");
    expect(url).not.toContain("demandpublishing");
  });

  it("GETs sitemanage publish/resource for an asset", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    expect(await publishEditorItem("99", "asset")).toBe(true);
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "sitemanage/publish/resource/99",
    );
  });

  it("returns false without an id or for none", async () => {
    const fetchSpy = vi.spyOn(global, "fetch");
    expect(await publishEditorItem("", "page")).toBe(false);
    expect(await publishEditorItem("42", "none")).toBe(false);
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("throws on HTTP 200 unwrapped FORBIDDEN instead of returning true", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ status: "FORBIDDEN" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(publishEditorItem("42", "page")).rejects.toThrow("FORBIDDEN");
  });

  it("throws on wrapped SitePublishResponse BADCONFIG warning", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          SitePublishResponse: {
            status: "BADCONFIG",
            warningMessage:
              "Could not connect to publishing server, please check publishing server configuration.",
          },
        }),
        {
          status: 200,
          headers: { "Content-Type": "application/json" },
        },
      ),
    );
    await expect(publishEditorItem("42", "page")).rejects.toThrow(
      "Could not connect to publishing server",
    );
  });
});

describe("canViewPublishHistoryFromEditor", () => {
  it("is true in edit and view for a page or asset, not promote or none", () => {
    expect(canViewPublishHistoryFromEditor("edit", "page")).toBe(true);
    expect(canViewPublishHistoryFromEditor("view", "asset")).toBe(true);
    expect(canViewPublishHistoryFromEditor("promote", "page")).toBe(false);
    expect(canViewPublishHistoryFromEditor("edit", "none")).toBe(false);
    expect(canViewPublishHistoryFromEditor("view", "none")).toBe(false);
  });
});

describe("canTakedownFromEditor", () => {
  it("matches publish eligibility: edit mode page or asset only", () => {
    expect(canTakedownFromEditor("edit", "page")).toBe(true);
    expect(canTakedownFromEditor("edit", "asset")).toBe(true);
    expect(canTakedownFromEditor("view", "page")).toBe(false);
    expect(canTakedownFromEditor("edit", "none")).toBe(false);
  });

  it("treats folders and missing ids as not takedown-eligible", () => {
    expect(resolveEditorPublishKind("Folder", { id: "8" })).toBe("none");
    expect(
      canTakedownFromEditor("edit", resolveEditorPublishKind("Folder", { id: "8" })),
    ).toBe(false);
    expect(
      canTakedownFromEditor("edit", resolveEditorPublishKind("percPage", { id: "" })),
    ).toBe(false);
  });
});

describe("takedownEditorItem", () => {
  it("GETs sitemanage takedown/page when there are no linked pages", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    expect(await takedownEditorItem("42", "page")).toBe(true);
    const req = global.fetch.mock.calls[0];
    expect(String(req?.[0] ?? "")).toContain(
      "sitemanage/publish/takedown/page/42",
    );
    expect(req?.[1]?.method ?? "GET").toBe("GET");
  });

  it("GETs sitemanage takedown/resource for an asset", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    expect(await takedownEditorItem("99", "asset")).toBe(true);
    expect(String(global.fetch.mock.calls[0]?.[0] ?? "")).toContain(
      "sitemanage/publish/takedown/resource/99",
    );
  });

  it("PUTs the linked-page list when it is non-empty", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const linked = [
      { id: "7", pagePath: "/Sites/Demo/Home", relationshipId: "rel-1" },
    ];
    expect(await takedownEditorItem("42", "page", linked)).toBe(true);
    const req = global.fetch.mock.calls[0];
    expect(req?.[1]?.method).toBe("PUT");
    expect(String(req?.[1]?.body ?? "")).toContain("/Sites/Demo/Home");
  });

  it("returns false without an id or for none", async () => {
    const fetchSpy = vi.spyOn(global, "fetch");
    expect(await takedownEditorItem("", "page")).toBe(false);
    expect(await takedownEditorItem("42", "none")).toBe(false);
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("throws on HTTP 200 FORBIDDEN instead of returning true", async () => {
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(JSON.stringify({ status: "FORBIDDEN" }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    await expect(takedownEditorItem("42", "page")).rejects.toThrow("FORBIDDEN");
  });
});

describe("formatEditorTakedownConfirm", () => {
  it("lists linked page paths and stays plain when there are none", () => {
    expect(formatEditorTakedownConfirm([])).toMatch(/unpublish/i);
    expect(formatEditorTakedownConfirm([])).not.toMatch(/Sites\/Demo/);
    expect(
      formatEditorTakedownConfirm([{ pagePath: "/Sites/Demo/Home" }]),
    ).toContain("/Sites/Demo/Home");
  });
});
