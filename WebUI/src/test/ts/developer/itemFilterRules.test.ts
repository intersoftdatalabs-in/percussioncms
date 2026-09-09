/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  cloneRules,
  emptyRule,
  rulesFingerprint,
  sanitizeRuleParams,
  toWireRules,
} from "../../../main/ts/developer/itemFilterRules";

describe("itemFilterRules", () => {
  it("clones rules and params without sharing references", () => {
    const src = [
      {
        name: "Java/global/percussion/itemfilter/sys_previewFilter",
        ruleId: { stringValue: "0-1-1" },
        params: [{ name: "sys_user", value: "Admin" }],
      },
    ];
    const cloned = cloneRules(src);
    expect(cloned).toEqual([
      {
        name: "Java/global/percussion/itemfilter/sys_previewFilter",
        ruleId: { stringValue: "0-1-1" },
        params: [{ name: "sys_user", value: "Admin" }],
      },
    ]);
    cloned[0].name = "changed";
    cloned[0].params![0].value = "other";
    expect(src[0].name).toBe("Java/global/percussion/itemfilter/sys_previewFilter");
    expect(src[0].params![0].value).toBe("Admin");
  });

  it("treats missing rules as empty", () => {
    expect(cloneRules(null)).toEqual([]);
    expect(cloneRules(undefined)).toEqual([]);
    expect(toWireRules([])).toEqual([]);
    expect(rulesFingerprint([])).toBe(rulesFingerprint(null));
  });

  it("coerces a single Jackson rule object to a one-element array", () => {
    const single = {
      name: "Java/global/percussion/itemfilter/sys_previewFilter",
      params: { name: "sys_user", value: "Admin" },
    };
    expect(cloneRules(single as never)).toEqual([
      {
        name: "Java/global/percussion/itemfilter/sys_previewFilter",
        ruleId: undefined,
        params: [{ name: "sys_user", value: "Admin" }],
      },
    ]);
  });

  it("drops blank param names on wire sanitize", () => {
    expect(
      sanitizeRuleParams([
        { name: "  ", value: "x" },
        { name: "sys_flagValues", value: "y" },
        { name: "keepEmpty", value: "" },
      ]),
    ).toEqual([
      { name: "sys_flagValues", value: "y" },
      { name: "keepEmpty", value: "" },
    ]);
  });

  it("fingerprints order-sensitive wire rules", () => {
    const a = toWireRules([
      { name: "r1", params: [{ name: "p", value: "1" }] },
      { name: "r2", params: [] },
    ]);
    const b = toWireRules([
      { name: "r2", params: [] },
      { name: "r1", params: [{ name: "p", value: "1" }] },
    ]);
    expect(rulesFingerprint(a)).not.toBe(rulesFingerprint(b));
    expect(rulesFingerprint(a)).toBe(
      rulesFingerprint([
        { name: "r1", params: [{ name: "p", value: "1" }] },
        { name: "r2", params: [] },
      ]),
    );
  });

  it("emptyRule starts with blank name and no params", () => {
    expect(emptyRule()).toEqual({ name: "", params: [] });
  });
});
