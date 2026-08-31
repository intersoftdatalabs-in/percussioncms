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
  SLOT_DETAIL_ROOT,
  createSlot,
  deleteSlot,
  getSlotDetail,
  isSlotCreateReady,
  isValidSlotName,
  isValidSlotType,
  normalizeSlotAssociations,
  normalizeSlotDesignGaps,
  unwrapSlotDetail,
} from "../../../../main/ts/api/developer/assemblyApi";
import { PATHS } from "../../../../main/ts/api/paths";

const assoc = {
  contentTypeGuid: { stringValue: "0-2-301" },
  templateGuid: { stringValue: "0-10-1" },
};

describe("unwrapSlotDetail / Jackson list fields (#3554)", () => {
  it("unwraps SlotDetail root and keeps real arrays", () => {
    const flat = unwrapSlotDetail({
      [SLOT_DETAIL_ROOT]: {
        name: "rffList",
        associations: [assoc],
        designGaps: [{ code: "SLOT_CREATE_DELETE", message: "Create / delete not supported" }],
      },
    });
    expect(flat.name).toBe("rffList");
    expect(Array.isArray(flat.associations)).toBe(true);
    expect(flat.associations).toHaveLength(1);
    expect(Array.isArray(flat.designGaps)).toBe(true);
    expect(flat.designGaps?.[0]).toEqual({
      code: "SLOT_CREATE_DELETE",
      message: "Create / delete not supported",
    });
  });

  it("normalizes Jackson empty-collection beans to []", () => {
    const flat = unwrapSlotDetail({
      name: "sys_AutoIndex",
      associations: { empty: false },
      designGaps: { empty: true },
    });
    expect(flat.associations).toEqual([]);
    expect(flat.designGaps).toEqual([]);
  });

  it("unwraps JAXB single-item association and gap envelopes", () => {
    const flat = unwrapSlotDetail({
      name: "rffCalendar",
      associations: { SlotAssociation: assoc },
      designGaps: {
        DesignGap: { code: "SLOT_ASSOC_GUIDS_ONLY", message: "Guids only" },
      },
    });
    expect(flat.associations).toEqual([assoc]);
    expect(flat.designGaps).toEqual([{ code: "SLOT_ASSOC_GUIDS_ONLY", message: "Guids only" }]);
  });

  it("unwraps JAXB multi-item envelopes", () => {
    const second = {
      contentTypeGuid: { stringValue: "0-2-302" },
      templateGuid: { stringValue: "0-10-2" },
    };
    const flat = unwrapSlotDetail({
      name: "rffList",
      associations: { SlotAssociation: [assoc, second] },
      designGaps: {
        DesignGap: [
          { code: "A", message: "one" },
          { code: "B", message: "two" },
        ],
      },
    });
    expect(flat.associations).toEqual([assoc, second]);
    expect(flat.designGaps).toHaveLength(2);
  });

  it("flattens JAXB finderArguments {entry:[{key,value}]} to a string map", () => {
    const flat = unwrapSlotDetail({
      name: "rffAutoPressReleases2007",
      associations: assoc,
      finderArguments: {
        entry: [
          { key: "template", value: "rffSnDateAndTitleLink" },
          { key: "type", value: "sql" },
        ],
      },
      designGaps: [{ code: "SLOT_CREATE_DELETE", message: "Create / delete not supported" }],
    });
    expect(flat.finderArguments).toEqual({
      template: "rffSnDateAndTitleLink",
      type: "sql",
    });
    expect(flat.associations).toEqual([assoc]);
  });

  it("wraps a lone association object and a lone {code,message} gap", () => {
    const flat = unwrapSlotDetail({
      name: "rffContacts",
      associations: assoc,
      designGaps: { code: "SLOT_CREATE_DELETE", message: "Create / delete not supported" },
    });
    expect(flat.associations).toEqual([assoc]);
    expect(flat.designGaps).toEqual([
      { code: "SLOT_CREATE_DELETE", message: "Create / delete not supported" },
    ]);
  });

  it("returns empty object for unrelated envelopes", () => {
    expect(unwrapSlotDetail({ Error: { message: "x" } })).toEqual({});
    expect(unwrapSlotDetail(null)).toEqual({});
  });
});

describe("normalizeSlotAssociations / normalizeSlotDesignGaps", () => {
  it("leaves real arrays unchanged", () => {
    expect(normalizeSlotAssociations([assoc])).toEqual([assoc]);
    expect(normalizeSlotDesignGaps([{ code: "X", message: "m" }])).toEqual([
      { code: "X", message: "m" },
    ]);
  });

  it("maps nullish and primitives to []", () => {
    expect(normalizeSlotAssociations(undefined)).toEqual([]);
    expect(normalizeSlotAssociations("nope")).toEqual([]);
    expect(normalizeSlotDesignGaps(null)).toEqual([]);
    expect(normalizeSlotDesignGaps(12)).toEqual([]);
  });
});

describe("getSlotDetail unwraps non-array list fields (#3554)", () => {
  const fetchMock = vi.fn();

  beforeEach(() => {
    fetchMock.mockReset();
    vi.stubGlobal("fetch", fetchMock);
  });

  afterEach(() => {
    vi.unstubAllGlobals();
  });

  it("GET SlotDetail with {empty:false} associations does not throw", async () => {
    fetchMock.mockResolvedValue(
      new Response(
        JSON.stringify({
          SlotDetail: {
            name: "sys_AutoIndex",
            label: "Auto Index",
            associations: { empty: false },
            designGaps: [{ code: "SLOT_CREATE_DELETE", message: "Create / delete not supported" }],
          },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const d = await getSlotDetail("sys_AutoIndex");
    expect(d.name).toBe("sys_AutoIndex");
    expect(Array.isArray(d.associations)).toBe(true);
    expect(d.associations).toEqual([]);
    expect(d.designGaps).toEqual([
      { code: "SLOT_CREATE_DELETE", message: "Create / delete not supported" },
    ]);
    const url = String(fetchMock.mock.calls[0]?.[0]);
    expect(url).toContain(`${PATHS.SLOTS}/`);
    expect(url).toContain(encodeURIComponent("sys_AutoIndex"));
  });
});

describe("slot create validation (AS-01)", () => {
  it("rejects blank, whitespace, and wildcard names", () => {
    expect(isValidSlotName("")).toBe(false);
    expect(isValidSlotName("  ")).toBe(false);
    expect(isValidSlotName("my slot")).toBe(false);
    expect(isValidSlotName("foo*")).toBe(false);
    expect(isValidSlotName("qa4056slot")).toBe(true);
  });

  it("accepts empty, REGULAR, and INLINE slotType; rejects others", () => {
    expect(isValidSlotType("")).toBe(true);
    expect(isValidSlotType("REGULAR")).toBe(true);
    expect(isValidSlotType("inline")).toBe(true);
    expect(isValidSlotType("WIDGET")).toBe(false);
    expect(isSlotCreateReady({ name: "ok", slotType: "WIDGET" })).toBe(false);
    expect(isSlotCreateReady({ name: "ok", slotType: "REGULAR" })).toBe(true);
  });
});

describe("createSlot / deleteSlot (AS-01)", () => {
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
      statusText: status === 200 ? "OK" : "Error",
      headers: { "Content-Type": "application/json" },
    });
  }

  it("createSlot wraps request and unwraps response", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({
        SlotDetail: { name: "qaSlot", label: "QA", slotType: "REGULAR" },
      }),
    );
    const out = await createSlot({
      name: "qaSlot",
      label: "QA",
      slotType: "REGULAR",
    });
    expect(out.name).toBe("qaSlot");
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(JSON.parse(String(init.body))).toEqual({
      SlotDetail: { name: "qaSlot", label: "QA", slotType: "REGULAR" },
    });
    expect(String(fetchMock.mock.calls[0]?.[0])).toContain(PATHS.SLOTS);
  });

  it("createSlot invalid name is 400", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({ message: "name cannot contain whitespace" }, 400),
    );
    await expect(createSlot({ name: "my slot" })).rejects.toMatchObject({
      status: 400,
    });
  });

  it("createSlot invalid slotType is 400", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({ message: "slotType must be REGULAR or INLINE" }, 400),
    );
    await expect(
      createSlot({ name: "qaSlot", slotType: "WIDGET" }),
    ).rejects.toMatchObject({ status: 400 });
  });

  it("createSlot duplicate is 409", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({ message: "Slot already exists: qaSlot" }, 409),
    );
    await expect(createSlot({ name: "qaSlot" })).rejects.toMatchObject({
      status: 409,
    });
  });

  it("createSlot non-Admin is 403", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({ message: "Admin role required" }, 403),
    );
    await expect(createSlot({ name: "qaSlot" })).rejects.toMatchObject({
      status: 403,
    });
  });

  it("deleteSlot sends DELETE to slots/{idOrName}", async () => {
    fetchMock.mockResolvedValue(new Response(null, { status: 204 }));
    await deleteSlot("qaSlot");
    const url = String(fetchMock.mock.calls[0]?.[0]);
    expect(url).toContain(`${PATHS.SLOTS}/`);
    expect(url).toContain(encodeURIComponent("qaSlot"));
    const init = fetchMock.mock.calls[0]?.[1] as RequestInit;
    expect(init.method).toBe("DELETE");
  });

  it("deleteSlot system slot is 409", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({ message: "System slots cannot be deleted" }, 409),
    );
    await expect(deleteSlot("sys_inline_link")).rejects.toMatchObject({
      status: 409,
    });
  });

  it("deleteSlot non-Admin is 403", async () => {
    fetchMock.mockResolvedValue(
      jsonResponse({ message: "Admin role required" }, 403),
    );
    await expect(deleteSlot("qaSlot")).rejects.toMatchObject({ status: 403 });
  });
});
