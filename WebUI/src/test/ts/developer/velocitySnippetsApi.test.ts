/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../main/ts/api/client";
import {
  getVelocitySnippet,
  listVelocitySnippets,
  unwrapVelocitySnippets,
} from "../../../main/ts/api/developer/velocitySnippetsApi";

vi.mock("../../../main/ts/api/client", () => ({
  get: vi.fn(),
}));

const getMock = vi.mocked(client.get);

describe("unwrapVelocitySnippets", () => {
  it("accepts a bare array", () => {
    const rows = unwrapVelocitySnippets([
      {
        id: "field.field",
        title: "field",
        category: "field",
        insertText: '#field("rx:title")',
      },
    ]);
    expect(rows).toHaveLength(1);
    expect(rows[0]?.id).toBe("field.field");
    expect(rows[0]?.insertText).toContain("#field");
  });

  it("unwraps VelocitySnippet envelope and skips empty rows", () => {
    const rows = unwrapVelocitySnippets({
      VelocitySnippet: {
        id: "misc.inner",
        title: "inner",
        category: "misc",
        insertText: "#inner()",
      },
    });
    expect(rows).toEqual([
      {
        id: "misc.inner",
        title: "inner",
        category: "misc",
        insertText: "#inner()",
      },
    ]);
    expect(unwrapVelocitySnippets(null)).toEqual([]);
    expect(unwrapVelocitySnippets({})).toEqual([]);
  });
});

describe("listVelocitySnippets / getVelocitySnippet", () => {
  afterEach(() => {
    getMock.mockReset();
  });

  it("lists snippets from GET /services/velocity/snippets", async () => {
    getMock.mockResolvedValue([
      {
        id: "slot.slot_simple",
        title: "slot_simple",
        category: "slot",
        insertText: '#slot_simple("rffList")',
      },
    ]);
    const list = await listVelocitySnippets();
    expect(getMock).toHaveBeenCalled();
    expect(list[0]?.category).toBe("slot");
  });

  it("gets one snippet and unwraps root", async () => {
    getMock.mockResolvedValue({
      VelocitySnippet: {
        id: "field.displayfield",
        title: "displayfield",
        category: "field",
        insertText: '#displayfield("rx:title")',
      },
    });
    const snip = await getVelocitySnippet("field.displayfield");
    expect(snip.id).toBe("field.displayfield");
    expect(snip.insertText).toContain("displayfield");
  });
});
