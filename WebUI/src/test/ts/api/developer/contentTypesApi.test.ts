/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  lockContentType,
  unlockContentType,
  unwrapContentTypeDetail,
  unwrapContentTypeList,
  unwrapObjectLockSummary,
  updateContentTypeDetail,
  wrapContentTypeDetailForWire,
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

describe("wrapContentTypeDetailForWire", () => {
  it("wraps a flat update under ContentTypeDetail", () => {
    expect(wrapContentTypeDetailForWire({ description: "d", enabled: true })).toEqual({
      ContentTypeDetail: { description: "d", enabled: true },
    });
  });
});

describe("unwrapObjectLockSummary", () => {
  it("unwraps Jackson ObjectLockSummary root", () => {
    expect(
      unwrapObjectLockSummary({
        ObjectLockSummary: { session: "s1", locker: "Admin", remainingTime: 30 },
      }),
    ).toEqual({ session: "s1", locker: "Admin", remainingTime: 30 });
  });

  it("accepts a flat lock body", () => {
    expect(
      unwrapObjectLockSummary({ locker: "Admin", remainingTime: 15 }),
    ).toEqual({ locker: "Admin", remainingTime: 15 });
  });

  it("returns empty object for null", () => {
    expect(unwrapObjectLockSummary(null)).toEqual({});
  });
});

describe("content type design-session lock/save/unlock client (#3744)", () => {
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

  it("POSTs lock and unwraps ObjectLockSummary", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        ObjectLockSummary: { session: "s", locker: "Admin", remainingTime: 30 },
      }),
    );
    const summary = await lockContentType("percPage");
    expect(summary.locker).toBe("Admin");
    expect(summary.remainingTime).toBe(30);
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.CONTENT_TYPES}/percPage/lock`,
    );
    expect((fetchMock.mock.calls[0][1] as RequestInit).method).toBe("POST");
  });

  it("POSTs unlock", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await unlockContentType("percPage");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.CONTENT_TYPES}/percPage/unlock`,
    );
    expect((fetchMock.mock.calls[0][1] as RequestInit).method).toBe("POST");
  });

  it("PUTs save without lock or unlock, wrapping ContentTypeDetail root", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        ContentTypeDetail: { name: "percPage", label: "Page", description: "updated" },
      }),
    );

    const saved = await updateContentTypeDetail("percPage", { description: "updated" });
    expect(saved.description).toBe("updated");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    expect((fetchMock.mock.calls[0][1] as RequestInit).method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.CONTENT_TYPES}/percPage`);
    expect(String(fetchMock.mock.calls[0][0])).not.toContain("/lock");
    expect(String(fetchMock.mock.calls[0][0])).not.toContain("/unlock");
    const sent = JSON.parse(String((fetchMock.mock.calls[0][1] as RequestInit).body));
    expect(sent).toEqual(
      wrapContentTypeDetailForWire({ description: "updated" }),
    );
    expect(sent.ContentTypeDetail.description).toBe("updated");
  });
});
