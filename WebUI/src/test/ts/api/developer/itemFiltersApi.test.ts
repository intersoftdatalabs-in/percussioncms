/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  createItemFilter,
  deleteItemFilter,
  getItemFilterDetail,
  isItemFilterWriteReady,
  isValidFilterName,
  listItemFilters,
  normalizeFilterName,
  unwrapItemFilter,
  updateItemFilter,
  wrapItemFilterForWire,
} from "../../../../main/ts/api/developer/itemFiltersApi";
import { PATHS } from "../../../../main/ts/api/paths";

describe("item filter name validation", () => {
  it("trims names", () => {
    expect(normalizeFilterName("  preview  ")).toBe("preview");
    expect(normalizeFilterName("")).toBe("");
    expect(normalizeFilterName(null)).toBe("");
  });

  it("accepts REST-safe names and rejects junk", () => {
    expect(isValidFilterName("preview")).toBe(true);
    expect(isValidFilterName("qa4060")).toBe(true);
    expect(isValidFilterName("  public  ")).toBe(true);
    expect(isValidFilterName("")).toBe(false);
    expect(isValidFilterName("has space")).toBe(false);
    expect(isValidFilterName("wild*card")).toBe(false);
    expect(isValidFilterName("pct%name")).toBe(false);
    expect(isValidFilterName("../x")).toBe(false);
    expect(isValidFilterName("a/b")).toBe(false);
    expect(isValidFilterName("a\\b")).toBe(false);
  });

  it("disables write until the filter name is valid on create", () => {
    expect(isItemFilterWriteReady({ isNew: true, name: "" })).toBe(false);
    expect(isItemFilterWriteReady({ isNew: true, name: "has space" })).toBe(false);
    expect(isItemFilterWriteReady({ isNew: true, name: "preview" })).toBe(true);
    expect(isItemFilterWriteReady({ isNew: false, name: "preview" })).toBe(true);
    expect(isItemFilterWriteReady({ isNew: false, name: "" })).toBe(false);
  });
});

describe("item filter wire wrap", () => {
  it("wraps POST/PUT under ItemFilter root", () => {
    expect(wrapItemFilterForWire({ name: "preview", description: "Public preview" })).toEqual({
      ItemFilter: { name: "preview", description: "Public preview" },
    });
  });

  it("unwraps ItemFilter envelope and flat bodies", () => {
    expect(
      unwrapItemFilter({ ItemFilter: { name: "public", description: "Public" } }),
    ).toEqual({ name: "public", description: "Public" });
    expect(unwrapItemFilter({ name: "preview" })).toEqual({ name: "preview" });
    expect(unwrapItemFilter(null)).toEqual({});
  });
});

describe("itemFiltersApi write paths", () => {
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

  it("POSTs create body to /services/itemfilters", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ name: "preview", description: "Preview" }));
    const saved = await createItemFilter({ name: "preview", description: "Preview" });
    expect(saved.name).toBe("preview");
    expect(fetchMock).toHaveBeenCalledTimes(1);
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("POST");
    expect(String(fetchMock.mock.calls[0][0])).toContain(PATHS.ITEM_FILTERS);
    expect(JSON.parse(String(init.body))).toEqual({
      ItemFilter: { name: "preview", description: "Preview" },
    });
  });

  it("PUTs update body to /services/itemfilters/{idOrName}", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ name: "preview", description: "Updated" }),
    );
    const saved = await updateItemFilter("preview", {
      name: "preview",
      description: "Updated",
    });
    expect(saved.description).toBe("Updated");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("PUT");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.ITEM_FILTERS}/preview`);
  });

  it("DELETEs /services/itemfilters/{idOrName}", async () => {
    fetchMock.mockResolvedValueOnce(new Response(null, { status: 204 }));
    await deleteItemFilter("preview");
    const init = fetchMock.mock.calls[0][1] as RequestInit;
    expect(init.method).toBe("DELETE");
    expect(String(fetchMock.mock.calls[0][0])).toContain(`${PATHS.ITEM_FILTERS}/preview`);
  });

  it("lists filters from GET /services/itemfilters", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ ItemFilter: [{ name: "public", description: "Public" }] }),
    );
    const list = await listItemFilters();
    expect(list).toEqual([{ name: "public", description: "Public" }]);
  });

  it("unwraps GET /services/itemfilters/{idOrName} Jackson root", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ ItemFilter: { name: "public", description: "Public", rules: [] } }),
    );
    const detail = await getItemFilterDetail("public");
    expect(detail.name).toBe("public");
    expect(detail.description).toBe("Public");
  });

  it("surfaces 400 invalid name", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ message: "name cannot contain whitespace" }, 400),
    );
    await expect(createItemFilter({ name: "bad name" })).rejects.toMatchObject({
      status: 400,
    });
    expect(fetchMock).toHaveBeenCalledTimes(1);
  });

  it("surfaces 409 duplicate name", async () => {
    fetchMock.mockResolvedValueOnce(
      jsonResponse({ message: "Item filter already exists: preview" }, 409),
    );
    await expect(createItemFilter({ name: "preview" })).rejects.toMatchObject({
      status: 409,
    });
  });

  it("surfaces 404 missing filter", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Item filter not found" }, 404));
    await expect(getItemFilterDetail("missing")).rejects.toMatchObject({ status: 404 });
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Item filter not found" }, 404));
    await expect(deleteItemFilter("missing")).rejects.toMatchObject({ status: 404 });
  });

  it("surfaces 403 non-Admin create", async () => {
    fetchMock.mockResolvedValueOnce(jsonResponse({ message: "Admin role required" }, 403));
    await expect(createItemFilter({ name: "preview" })).rejects.toMatchObject({
      status: 403,
    });
  });
});
