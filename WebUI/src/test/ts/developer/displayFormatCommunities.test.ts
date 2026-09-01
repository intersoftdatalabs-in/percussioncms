/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  communityMatchesKey,
  communityWireKey,
  isAllCommunities,
  normalizeAllowedCommunities,
  selectedKeysFromMap,
  toAllowedCommunitiesWriteBody,
} from "../../../main/ts/developer/displayFormatCommunities";

const defaultCommunity = {
  id: 10,
  name: "Default",
  label: "Default",
  guid: { stringValue: "0-10-10", uuid: 10 },
};

describe("normalizeAllowedCommunities", () => {
  it("treats missing and empty as all communities", () => {
    expect(normalizeAllowedCommunities(undefined)).toEqual({});
    expect(normalizeAllowedCommunities(null)).toEqual({});
    expect(normalizeAllowedCommunities({})).toEqual({});
    expect(isAllCommunities(normalizeAllowedCommunities({}))).toBe(true);
  });

  it("keeps GUID-string keys from a JSON map", () => {
    expect(normalizeAllowedCommunities({ "0-10-10": "Default" })).toEqual({
      "0-10-10": "Default",
    });
    expect(isAllCommunities({ "0-10-10": "Default" })).toBe(false);
  });

  it("unwraps a JAXB entry list", () => {
    expect(
      normalizeAllowedCommunities({
        entry: [{ key: { stringValue: "0-10-10" }, value: "Default" }],
      }),
    ).toEqual({ "0-10-10": "Default" });
  });

  it("unwraps REST guid/name rows", () => {
    expect(
      normalizeAllowedCommunities([{ guid: "0-10-10", name: "Default" }]),
    ).toEqual({ "0-10-10": "Default" });
  });

  it("unwraps a single JAXB community object (one-element list)", () => {
    expect(normalizeAllowedCommunities({ guid: "0-10-10", name: "Default" })).toEqual({
      "0-10-10": "Default",
    });
  });
});

describe("community matching and write body", () => {
  it("matches catalog rows by guid, uuid, id, or name", () => {
    expect(communityMatchesKey(defaultCommunity, "0-10-10")).toBe(true);
    expect(communityMatchesKey(defaultCommunity, "10")).toBe(true);
    expect(communityMatchesKey(defaultCommunity, "Default")).toBe(true);
    expect(communityMatchesKey(defaultCommunity, "missing")).toBe(false);
  });

  it("maps GET keys onto catalog wire keys", () => {
    const keys = selectedKeysFromMap({ "10": "Default" }, [defaultCommunity]);
    expect([...keys]).toEqual(["0-10-10"]);
  });

  it("empty or all writes [] — not a third none state", () => {
    expect(toAllowedCommunitiesWriteBody(true, [defaultCommunity], new Set())).toEqual([]);
    expect(toAllowedCommunitiesWriteBody(false, [defaultCommunity], new Set())).toEqual([]);
  });

  it("writes selected community GUID to name", () => {
    const key = communityWireKey(defaultCommunity);
    expect(
      toAllowedCommunitiesWriteBody(false, [defaultCommunity], new Set([key])),
    ).toEqual([{ guid: "0-10-10", name: "Default" }]);
  });
});
