/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  unwrapContentTypeDetail,
  unwrapContentTypeList,
  updateContentTypeDetail,
} from "../../../../main/ts/api/developer/contentTypesApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("unwrapContentTypeList", () => {
  it("returns bare arrays", () => {
    expect(unwrapContentTypeList([{ name: "a" }])).toEqual([{ name: "a" }]);
  });

  it("unwraps ContentType envelope", () => {
    expect(
      unwrapContentTypeList({
        ContentType: [{ name: "page", label: "Page" }],
      }),
    ).toEqual([{ name: "page", label: "Page" }]);
  });

  it("unwraps single ContentType object", () => {
    expect(unwrapContentTypeList({ ContentType: { name: "only" } })).toEqual([
      { name: "only" },
    ]);
  });

  it("handles null and empty", () => {
    expect(unwrapContentTypeList(null)).toEqual([]);
    expect(unwrapContentTypeList({})).toEqual([]);
  });

  it("fills guid.stringValue from host/type/uuid on list rows (#3319)", () => {
    expect(
      unwrapContentTypeList([
        { name: "percPage", guid: { hostId: 0, type: 2, uuid: 301 } },
      ]),
    ).toEqual([
      {
        name: "percPage",
        guid: { hostId: 0, type: 2, uuid: 301, stringValue: "0-2-301" },
        guidString: "0-2-301",
      },
    ]);
  });
});

describe("unwrapContentTypeDetail (#3319)", () => {
  it("unwraps Jackson ContentTypeDetail root so guid is reachable", () => {
    const flat = unwrapContentTypeDetail({
      ContentTypeDetail: {
        name: "percPage",
        guid: { stringValue: "0-2-301", type: 2, uuid: 301 },
        fields: [{ name: "sys_title" }],
      },
    });
    expect(flat.name).toBe("percPage");
    expect(flat.guid?.stringValue).toBe("0-2-301");
    expect(flat.guidString).toBe("0-2-301");
    expect(flat.fields).toHaveLength(1);
  });

  it("synthesizes guid.stringValue from numeric parts under root wrap", () => {
    const flat = unwrapContentTypeDetail({
      ContentTypeDetail: {
        name: "percPage",
        guid: { hostId: 0, type: 2, uuid: 301 },
      },
    });
    expect(flat.guid?.stringValue).toBe("0-2-301");
    expect(flat.guidString).toBe("0-2-301");
  });

  it("accepts flat payload and copies guidString", () => {
    const flat = unwrapContentTypeDetail({
      name: "percPage",
      guidString: "0-2-8",
    });
    expect(flat.guid?.stringValue).toBe("0-2-8");
    expect(flat.guidString).toBe("0-2-8");
  });

  it("returns empty object for unrelated envelopes", () => {
    expect(unwrapContentTypeDetail({ Error: { message: "x" } })).toEqual({});
    expect(unwrapContentTypeDetail(null)).toEqual({});
  });
});

describe("updateContentTypeDetail lock-save-unlock wrap", () => {
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

  it("POSTs lock, PUTs save, then POSTs unlock", async () => {
    fetchMock
      .mockResolvedValueOnce(jsonResponse({ session: "s", locker: "Admin", remainingTime: 30 }))
      .mockResolvedValueOnce(
        jsonResponse({
          ContentTypeDetail: { name: "percPage", label: "Page", description: "updated" },
        }),
      )
      .mockResolvedValueOnce(new Response(null, { status: 204 }));

    const saved = await updateContentTypeDetail("percPage", { description: "updated" });
    expect(saved.description).toBe("updated");
    expect(fetchMock).toHaveBeenCalledTimes(3);
    const methods = fetchMock.mock.calls.map((c) => (c[1] as RequestInit).method);
    expect(methods).toEqual(["POST", "PUT", "POST"]);
    const urls = fetchMock.mock.calls.map((c) => String(c[0]));
    expect(urls[0]).toContain(`${PATHS.CONTENT_TYPES}/percPage/lock`);
    expect(urls[1]).toContain(`${PATHS.CONTENT_TYPES}/percPage`);
    expect(urls[2]).toContain(`${PATHS.CONTENT_TYPES}/percPage/unlock`);
  });
});
