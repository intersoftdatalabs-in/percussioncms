/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import type { CommunitySummary } from "../../../main/ts/api/developer/types";
import {
  communityOpenKey,
  communityRowKey,
  filterCommunityCatalog,
  patchCommunityByName,
  sortCommunityCatalog,
} from "../../../main/ts/developer/communityCatalog";

function community(overrides: Partial<CommunitySummary> = {}): CommunitySummary {
  return { id: 7, name: "DefaultComm", label: "Default Community", ...overrides };
}

describe("communityCatalog", () => {
  it("prefers name, then id, then guid for the open key", () => {
    expect(communityOpenKey(community())).toBe("DefaultComm");
    expect(communityOpenKey(community({ name: undefined }))).toBe("7");
    expect(
      communityOpenKey(
        community({ name: undefined, id: undefined, guid: { stringValue: "0-1-7", longValue: 7 } }),
      ),
    ).toBe("0-1-7");
    expect(communityOpenKey({})).toBeNull();
  });

  it("builds stable row keys with a positional fallback", () => {
    expect(communityRowKey(community(), 3)).toBe("7");
    expect(communityRowKey(community({ id: undefined, guid: undefined }), 3)).toBe("DefaultComm");
    expect(communityRowKey({}, 3)).toBe("comm-3");
  });

  it("sorts by label with name fallback and tolerates null", () => {
    expect(sortCommunityCatalog(null)).toEqual([]);
    const sorted = sortCommunityCatalog([
      community({ name: "Zulu", label: undefined }),
      community({ name: "alpha", label: "Beta" }),
      community({ name: "Aaron", label: "Aaron" }),
    ]);
    expect(sorted.map((c) => c.name)).toEqual(["Aaron", "alpha", "Zulu"]);
    expect(sorted[1].label).toBe("Beta");
  });

  it("filters across label, name, description, and id", () => {
    const items = [
      community(),
      community({ id: 8, name: "NoirComm", label: "Noir Community", description: "Second" }),
    ];
    expect(filterCommunityCatalog(items, "noir").map((c) => c.name)).toEqual(["NoirComm"]);
    expect(filterCommunityCatalog(items, "second").map((c) => c.name)).toEqual(["NoirComm"]);
    expect(filterCommunityCatalog(items, "8").map((c) => c.name)).toEqual(["NoirComm"]);
    expect(filterCommunityCatalog(items, "zzz")).toEqual([]);
    expect(filterCommunityCatalog(items, "")).toHaveLength(2);
  });

  it("patches matching entries by name, case-insensitively", () => {
    const items = [community(), community({ id: 8, name: "NoirComm", label: "Noir Community" })];
    const patched = patchCommunityByName(items, "  defaultcomm ", {
      name: "Enterprise",
      label: "Enterprise",
    });
    expect(patched?.[0]).toMatchObject({ name: "Enterprise", label: "Enterprise" });
    expect(patched?.[1]).toMatchObject({ name: "NoirComm" });
    // Source array untouched.
    expect(items[0].name).toBe("DefaultComm");
  });

  it("leaves the catalog reference alone on blank or unmatched keys", () => {
    const items = [community()];
    expect(patchCommunityByName(items, "   ", { name: "X" })).toBe(items);
    expect(patchCommunityByName(items, "ghost", { name: "X" })).toBe(items);
    expect(patchCommunityByName(null, "DefaultComm", { name: "X" })).toBeNull();
  });
});
