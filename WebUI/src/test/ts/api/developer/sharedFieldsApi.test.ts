/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  addSharedField,
  createSharedFieldGroup,
  deleteSharedField,
  deleteSharedFieldGroup,
  getSharedFieldControlProperties,
  isSharedFieldAddReady,
  isSharedFieldGroupWriteReady,
  isValidFilename,
  isValidGroupName,
  isValidSharedFieldName,
  listSharedFieldGroups,
  normalizeGroupName,
  replaceSharedFieldControlProperties,
  unwrapSharedFieldControlProperties,
  unwrapSharedFieldGroupDetail,
  updateSharedFieldGroup,
  wrapSharedFieldControlPropertiesForWire,
  wrapSharedFieldForWire,
  wrapSharedFieldGroupDetailForWire,
} from "../../../../main/ts/api/developer/sharedFieldsApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("shared field group name validation", () => {
  it("trims names", () => {
    expect(normalizeGroupName("  custom  ")).toBe("custom");
    expect(normalizeGroupName("")).toBe("");
    expect(normalizeGroupName(null)).toBe("");
  });

  it("accepts REST-safe names and rejects junk", () => {
    expect(isValidGroupName("customShared")).toBe(true);
    expect(isValidGroupName("qa4029")).toBe(true);
    expect(isValidGroupName("  shared  ")).toBe(true);
    expect(isValidGroupName("")).toBe(false);
    expect(isValidGroupName("has space")).toBe(false);
    expect(isValidGroupName("wild*card")).toBe(false);
    expect(isValidGroupName("../x")).toBe(false);
    expect(isValidGroupName("a/b")).toBe(false);
    expect(isValidGroupName("a\\b")).toBe(false);
  });

  it("accepts blank filename and .xml names", () => {
    expect(isValidFilename("")).toBe(true);
    expect(isValidFilename("  ")).toBe(true);
    expect(isValidFilename("custom.xml")).toBe(true);
    expect(isValidFilename("custom")).toBe(true);
    expect(isValidFilename("has space.xml")).toBe(false);
    expect(isValidFilename("foo.txt")).toBe(false);
    expect(isValidFilename("../x.xml")).toBe(false);
    expect(isValidFilename("foo.bar.xml")).toBe(true);
    expect(isValidFilename("foo.bar")).toBe(false);
    expect(isValidGroupName("foo.bar")).toBe(true);
  });

  it("disables write until the group name is valid", () => {
    expect(isSharedFieldGroupWriteReady({ name: "" })).toBe(false);
    expect(isSharedFieldGroupWriteReady({ name: "has space" })).toBe(false);
    expect(isSharedFieldGroupWriteReady({ name: "custom" })).toBe(true);
    expect(isSharedFieldGroupWriteReady({ name: "custom", filename: "custom.xml" })).toBe(
      true,
    );
    expect(isSharedFieldGroupWriteReady({ name: "custom", filename: "bad.txt" })).toBe(
      false,
    );
  });
});

describe("shared nested field name validation", () => {
  it("accepts letter-start word names and rejects junk", () => {
    expect(isValidSharedFieldName("rx_note")).toBe(true);
    expect(isValidSharedFieldName("qa4439f")).toBe(true);
    expect(isValidSharedFieldName("A")).toBe(true);
    expect(isValidSharedFieldName("")).toBe(false);
    expect(isValidSharedFieldName("1bad")).toBe(false);
    expect(isValidSharedFieldName("has space")).toBe(false);
    expect(isValidSharedFieldName("bad-name")).toBe(false);
    expect(isValidSharedFieldName("a".repeat(51))).toBe(false);
    expect(isValidSharedFieldName(" rx_note")).toBe(false);
    expect(isValidSharedFieldName("rx_note ")).toBe(false);
    expect(isValidSharedFieldName("../x")).toBe(false);
  });

  it("disables add until the name is valid", () => {
    expect(isSharedFieldAddReady("")).toBe(false);
    expect(isSharedFieldAddReady("1x")).toBe(false);
    expect(isSharedFieldAddReady("qa_field")).toBe(true);
  });
});

describe("shared field group detail wire wrap", () => {
  it("wraps POST/PUT under SharedFieldGroupDetail root", () => {
    expect(wrapSharedFieldGroupDetailForWire({ name: "custom", filename: "custom.xml" })).toEqual({
      SharedFieldGroupDetail: { name: "custom", filename: "custom.xml" },
    });
  });

  it("unwraps SharedFieldGroupDetail envelope and flat bodies", () => {
    expect(
      unwrapSharedFieldGroupDetail({
        SharedFieldGroupDetail: { name: "shared", filename: "shared.xml" },
      }),
    ).toEqual({ name: "shared", filename: "shared.xml" });
    expect(unwrapSharedFieldGroupDetail({ name: "shared", filename: "shared.xml" })).toEqual({
      name: "shared",
      filename: "shared.xml",
    });
    expect(unwrapSharedFieldGroupDetail(null)).toEqual({});
  });

  it("unwraps JAXB one-item fields envelopes", () => {
    expect(
      unwrapSharedFieldGroupDetail({
        SharedFieldGroupDetail: {
          name: "shared",
          fields: { SharedField: { name: "rx_note", dataType: "text" } },
        },
      }).fields,
    ).toEqual([{ name: "rx_note", dataType: "text" }]);
  });

  it("wraps POST field under SharedField root", () => {
    expect(wrapSharedFieldForWire({ name: "rx_note", dataType: "text" })).toEqual({
      SharedField: { name: "rx_note", dataType: "text" },
    });
  });

  it("wraps PUT control properties under SharedFieldControlProperties root", () => {
    expect(
      wrapSharedFieldControlPropertiesForWire({
        properties: [{ name: "height", value: "200" }],
      }),
    ).toEqual({
      SharedFieldControlProperties: {
        properties: [{ name: "height", value: "200" }],
      },
    });
    expect(
      wrapSharedFieldControlPropertiesForWire({
        properties: [],
        choices: { type: "none" },
      }),
    ).toEqual({
      SharedFieldControlProperties: {
        properties: [],
        choices: { type: "none" },
      },
    });
  });

  it("unwraps SharedFieldControlProperties envelope and flat bodies", () => {
    expect(
      unwrapSharedFieldControlProperties({
        SharedFieldControlProperties: {
          fieldName: "rx_note",
          control: "sys_EditBox",
          properties: [{ name: "height", value: "200" }],
        },
      }),
    ).toEqual({
      fieldName: "rx_note",
      control: "sys_EditBox",
      properties: [{ name: "height", value: "200" }],
    });
    expect(unwrapSharedFieldControlProperties({ properties: [] })).toEqual({
      properties: [],
    });
    expect(unwrapSharedFieldControlProperties(null)).toEqual({ properties: [] });
  });
});

describe("sharedFieldsApi write paths", () => {
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

  it("POSTs create body to /services/sharedfields", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ name: "custom", filename: "custom.xml" }),
    );
    const saved = await createSharedFieldGroup({ name: "custom" });
    expect(saved.name).toBe("custom");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toContain(PATHS.SHARED_FIELDS);
    expect(JSON.parse(String(init.body))).toEqual({
      SharedFieldGroupDetail: { name: "custom" },
    });
  });

  it("PUTs update body to /services/sharedfields/{name}", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ name: "custom", filename: "custom.xml" }),
    );
    const saved = await updateSharedFieldGroup("custom", {
      name: "custom",
      filename: "custom.xml",
    });
    expect(saved.filename).toBe("custom.xml");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.SHARED_FIELDS}/custom`);
  });

  it("DELETEs /services/sharedfields/{name}", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await deleteSharedFieldGroup("custom");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.SHARED_FIELDS}/custom`);
  });

  it("lists groups from GET /services/sharedfields", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ SharedFieldGroupSummary: [{ name: "shared", filename: "shared.xml" }] }),
    );
    const list = await listSharedFieldGroups();
    expect(list).toEqual([{ name: "shared", filename: "shared.xml" }]);
  });

  it("unwraps GET /services/sharedfields/{name} Jackson root", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        SharedFieldGroupDetail: { name: "shared", filename: "shared.xml", fields: [] },
      }),
    );
    const { getSharedFieldGroupDetail } = await import(
      "../../../../main/ts/api/developer/sharedFieldsApi"
    );
    const detail = await getSharedFieldGroupDetail("shared");
    expect(detail.name).toBe("shared");
    expect(detail.filename).toBe("shared.xml");
  });

  it("POSTs wrapped add-field body", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        SharedFieldGroupDetail: {
          name: "shared",
          fields: [{ name: "rx_note", dataType: "text" }],
        },
      }),
    );
    const saved = await addSharedField("shared", { name: "rx_note", dataType: "text" });
    expect(saved.fields?.[0]?.name).toBe("rx_note");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.SHARED_FIELDS}/shared/fields`);
    expect(JSON.parse(String(init.body))).toEqual({
      SharedField: { name: "rx_note", dataType: "text" },
    });
  });

  it("DELETEs nested field", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await deleteSharedField("shared", "rx_note");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.SHARED_FIELDS}/shared/fields/rx_note`,
    );
  });

  it("GETs wrapped control properties", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        SharedFieldControlProperties: {
          fieldName: "rx_note",
          properties: [{ name: "height", value: "200" }],
        },
      }),
    );
    const out = await getSharedFieldControlProperties("shared", "rx_note");
    expect(out.properties?.[0]?.value).toBe("200");
    expect(String(fetchMock.mock.calls[0][0])).toContain(
      `${PATHS.SHARED_FIELDS}/shared/fields/rx_note/controlProperties`,
    );
  });

  it("PUTs wrapped control properties omitting choices when unchanged", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({
        properties: [{ name: "width", value: "640" }],
      }),
    );
    const saved = await replaceSharedFieldControlProperties("shared", "rx_note", {
      properties: [{ name: "width", value: "640" }],
    });
    expect(saved.properties?.[0]?.value).toBe("640");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(JSON.parse(String(init.body))).toEqual({
      SharedFieldControlProperties: {
        properties: [{ name: "width", value: "640" }],
      },
    });
  });
});
