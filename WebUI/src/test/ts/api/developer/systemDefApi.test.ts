/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  addSystemDefField,
  deleteSystemDefField,
  getSystemDef,
  isSystemDefFieldAddReady,
  isValidSystemDefFieldName,
  unwrapSystemDefDetail,
  updateSystemDef,
  wrapSystemDefDetailForWire,
  wrapSystemDefFieldForWire,
} from "../../../../main/ts/api/developer/systemDefApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("system def field name validation", () => {
  it("accepts letter-start word names and rejects junk", () => {
    expect(isValidSystemDefFieldName("sys_title")).toBe(true);
    expect(isValidSystemDefFieldName("qa4030ab")).toBe(true);
    expect(isValidSystemDefFieldName("A")).toBe(true);
    expect(isValidSystemDefFieldName("")).toBe(false);
    expect(isValidSystemDefFieldName("1bad")).toBe(false);
    expect(isValidSystemDefFieldName("has space")).toBe(false);
    expect(isValidSystemDefFieldName("bad-name")).toBe(false);
    expect(isValidSystemDefFieldName("a".repeat(51))).toBe(false);
  });

  it("disables add until the name is valid", () => {
    expect(isSystemDefFieldAddReady("")).toBe(false);
    expect(isSystemDefFieldAddReady("1x")).toBe(false);
    expect(isSystemDefFieldAddReady("qa_field")).toBe(true);
  });
});

describe("system def wire wrap", () => {
  it("wraps PUT under SystemDefDetail root", () => {
    expect(
      wrapSystemDefDetailForWire({
        fields: [{ name: "sys_title", searchable: true, occurrence: "required" }],
      }),
    ).toEqual({
      SystemDefDetail: {
        fields: [{ name: "sys_title", searchable: true, occurrence: "required" }],
      },
    });
  });

  it("wraps POST under SystemDefField root", () => {
    expect(wrapSystemDefFieldForWire({ name: "qa_note", dataType: "text" })).toEqual({
      SystemDefField: { name: "qa_note", dataType: "text" },
    });
  });

  it("unwraps SystemDefDetail envelope, JAXB one-item fields, and flat bodies", () => {
    expect(
      unwrapSystemDefDetail({
        SystemDefDetail: {
          fieldCount: 1,
          fields: { SystemDefField: { name: "sys_title" } },
          designGaps: "control",
        },
      }),
    ).toEqual({
      fieldCount: 1,
      cacheTimeoutMinutes: undefined,
      fields: [{ name: "sys_title" }],
      designGaps: ["control"],
    });
    expect(unwrapSystemDefDetail({ fieldCount: 0, fields: [], designGaps: [] })).toEqual({
      fieldCount: 0,
      cacheTimeoutMinutes: undefined,
      fields: [],
      designGaps: [],
    });
    expect(unwrapSystemDefDetail(null)).toEqual({ fields: [] });
  });
});

describe("systemDefApi write paths", () => {
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

  it("GETs and unwraps /services/systemdef", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        SystemDefDetail: { fieldCount: 1, fields: [{ name: "sys_title" }] },
      }),
    );
    const detail = await getSystemDef();
    expect(detail.fields?.[0]?.name).toBe("sys_title");
    expect(String(fetchMock.mock.calls[0][0])).toContain(PATHS.SYSTEM_DEF);
  });

  it("PUTs wrapped body to /services/systemdef", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ fields: [{ name: "sys_title", searchable: false }] }),
    );
    const saved = await updateSystemDef({
      fields: [{ name: "sys_title", searchable: false, occurrence: "optional" }],
    });
    expect(saved.fields?.[0]?.searchable).toBe(false);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({
      SystemDefDetail: {
        fields: [{ name: "sys_title", searchable: false, occurrence: "optional" }],
      },
    });
  });

  it("POSTs wrapped add body to /services/systemdef/fields", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ fields: [{ name: "qa_note" }] }));
    const saved = await addSystemDefField({ name: "qa_note", dataType: "text" });
    expect(saved.fields?.[0]?.name).toBe("qa_note");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.SYSTEM_DEF}/fields`);
    expect(JSON.parse(String(init.body))).toEqual({
      SystemDefField: { name: "qa_note", dataType: "text" },
    });
  });

  it("DELETEs /services/systemdef/fields/{fieldName}", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await deleteSystemDefField("qa_note");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.SYSTEM_DEF}/fields/qa_note`);
  });
});
