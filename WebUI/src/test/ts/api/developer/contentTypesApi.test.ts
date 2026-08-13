/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  unwrapContentTypeDetail,
  unwrapContentTypeList,
} from "../../../../main/ts/api/developer/contentTypesApi";

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
