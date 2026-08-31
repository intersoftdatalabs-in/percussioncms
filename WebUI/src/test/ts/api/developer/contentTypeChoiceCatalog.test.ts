/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  choiceCatalogPayloadError,
  choiceCatalogsEqual,
  cloneChoiceCatalog,
  isChoiceCatalogNone,
  parseChoiceCatalog,
  toChoiceCatalogPayload,
} from "../../../../main/ts/api/developer/contentTypeChoiceCatalog";

describe("contentTypeChoiceCatalog helpers (CD-07)", () => {
  it("treats null, empty, and type none as a cleared catalog", () => {
    expect(isChoiceCatalogNone(null)).toBe(true);
    expect(isChoiceCatalogNone({ type: "none" })).toBe(true);
    expect(isChoiceCatalogNone({ type: "  " })).toBe(true);
    expect(isChoiceCatalogNone({ type: "local", entries: [{ value: "a" }] })).toBe(false);
    expect(choiceCatalogsEqual(null, { type: "none" })).toBe(true);
    expect(choiceCatalogsEqual({ type: "local" }, { type: "none" })).toBe(false);
  });

  it("clones catalogs without sharing nested arrays", () => {
    const src = {
      type: "local",
      entries: [{ value: "open", label: "Open" }],
      nullEntry: { value: "", label: "None" },
    };
    const cloned = cloneChoiceCatalog(src);
    expect(cloned).toEqual({
      type: "local",
      entries: [{ value: "open", label: "Open" }],
      nullEntry: { value: "", label: "None" },
    });
    if (cloned?.entries) {
      cloned.entries[0].value = "closed";
    }
    expect(src.entries[0].value).toBe("open");
  });

  it("parses JAXB entry envelopes and numeric globalId", () => {
    expect(
      parseChoiceCatalog({
        type: "global",
        globalId: "42",
        sortOrder: "ascending",
      }),
    ).toEqual({ type: "global", globalId: 42, sortOrder: "ascending" });
    expect(
      parseChoiceCatalog({
        type: "local",
        entries: { ContentTypeChoiceEntry: { value: "open", label: "Open" } },
      }),
    ).toEqual({
      type: "local",
      entries: [{ value: "open", label: "Open" }],
    });
  });

  it("omits choices extras from a type-none PUT payload", () => {
    expect(
      toChoiceCatalogPayload({
        type: "none",
        entries: [{ value: "stale" }],
      }),
    ).toEqual({ type: "none" });
  });

  it("builds a local PUT payload with null-entry and default-selected", () => {
    expect(
      toChoiceCatalogPayload({
        type: "local",
        sortOrder: "user",
        entries: [
          { value: " open ", label: " Open " },
          { value: "  ", label: "skip" },
        ],
        nullEntry: { value: "", label: "None", includeWhen: "always", sortOrder: "first" },
        defaultSelected: [{ type: "nullEntry" }, { type: "text", text: "open" }],
      }),
    ).toEqual({
      type: "local",
      sortOrder: "user",
      entries: [{ value: "open", label: "Open" }],
      nullEntry: { value: "", label: "None", includeWhen: "always", sortOrder: "first" },
      defaultSelected: [{ type: "nullEntry" }, { type: "text", text: "open" }],
    });
  });

  it("reports missing required fields before PUT", () => {
    expect(choiceCatalogPayloadError(null)).toBeNull();
    expect(choiceCatalogPayloadError({ type: "local", entries: [] })).toBe("local-entries");
    expect(choiceCatalogPayloadError({ type: "global" })).toBe("global-id");
    expect(choiceCatalogPayloadError({ type: "lookup" })).toBe("lookup-href");
    expect(choiceCatalogPayloadError({ type: "tableinfo", table: { tableName: "T" } })).toBe(
      "table",
    );
    expect(
      choiceCatalogPayloadError({
        type: "local",
        entries: [{ value: "open", label: "Open" }],
      }),
    ).toBeNull();
  });
});
