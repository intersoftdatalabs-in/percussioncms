/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { afterEach, describe, expect, it, vi } from "vitest";
import * as client from "../../../../main/ts/api/client";
import { PATHS } from "../../../../main/ts/api/paths";
import {
  CONTROL_DEF_ROOT,
  CONTROL_DESIGN_GAPS,
  createControl,
  deleteControl,
  isControlCreateReady,
  isControlSaveReady,
  isSystemControl,
  isValidControlName,
  normalizeControlName,
  unwrapControlDef,
  updateControl,
  withoutStaleControlWriteGap,
  wrapControlCreateForWire,
} from "../../../../main/ts/api/developer/controlsApi";

describe("control name validation", () => {
  it("trims and rejects blank, spaces, wildcards, and unsafe keys", () => {
    expect(normalizeControlName("  myCtl  ")).toBe("myCtl");
    expect(isValidControlName("myUserControl")).toBe(true);
    expect(isValidControlName("sys_EditBox")).toBe(true);
    expect(isValidControlName("my-ctl.v1")).toBe(true);
    expect(isValidControlName("  ")).toBe(false);
    expect(isValidControlName("has space")).toBe(false);
    expect(isValidControlName("My*Control")).toBe(false);
    expect(isValidControlName("My%Control")).toBe(false);
    expect(isValidControlName("bad/name")).toBe(false);
    expect(isValidControlName("a".repeat(101))).toBe(false);
  });

  it("create-ready requires a valid name; optional metadata may be blank", () => {
    expect(isControlCreateReady({ name: "qaCtl" })).toBe(true);
    expect(isControlCreateReady({ name: "bad name" })).toBe(false);
    expect(isControlCreateReady({ name: "qaCtl", dimension: "table" })).toBe(true);
    expect(isControlCreateReady({ name: "qaCtl", dimension: "wide" })).toBe(false);
    expect(isControlCreateReady({ name: "qaCtl", choiceSet: "required" })).toBe(true);
    expect(isControlCreateReady({ name: "qaCtl", choiceSet: "maybe" })).toBe(false);
  });

  it("save-ready allows blank dimension/choiceSet and rejects invalid values", () => {
    expect(isControlSaveReady({})).toBe(true);
    expect(isControlSaveReady({ dimension: "array", choiceSet: "optional" })).toBe(true);
    expect(isControlSaveReady({ dimension: "wide" })).toBe(false);
    expect(isControlSaveReady({ choiceSet: "maybe" })).toBe(false);
  });

  it("treats scope system as packaged (409) and other scopes as user", () => {
    expect(isSystemControl("system")).toBe(true);
    expect(isSystemControl(" SYSTEM ")).toBe(true);
    expect(isSystemControl("user")).toBe(false);
    expect(isSystemControl(null)).toBe(false);
  });
});

describe("unwrapControlDef / wrapControlCreateForWire", () => {
  it("unwraps Jackson ControlDef root envelope", () => {
    const unwrapped = unwrapControlDef({
      ControlDef: { name: "myUserControl", scope: "user" },
    });
    expect(unwrapped.name).toBe("myUserControl");
    expect(unwrapped.scope).toBe("user");
  });

  it("accepts a flat ControlDef", () => {
    expect(unwrapControlDef({ name: "sys_EditBox" }).name).toBe("sys_EditBox");
  });

  it("throws when name is missing", () => {
    expect(() => unwrapControlDef({})).toThrow(/missing name/);
    expect(() => unwrapControlDef(null)).toThrow(/empty response/);
  });

  it("wraps POST body under ControlDef root", () => {
    expect(wrapControlCreateForWire({ name: "qaCtl" })).toEqual({
      [CONTROL_DEF_ROOT]: { name: "qaCtl" },
    });
  });
});

describe("withoutStaleControlWriteGap", () => {
  it("drops the pre-create write-gap string", () => {
    expect(
      withoutStaleControlWriteGap([
        "User control create / edit / delete not supported via this API",
        "User control edit / delete not supported via this API",
        "System controls are read-only packaged defaults",
      ]),
    ).toEqual(["System controls are read-only packaged defaults"]);
  });
});

describe("createControl", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("POSTs a wrapped body and unwraps the response", async () => {
    const spy = vi.spyOn(client, "post").mockResolvedValue({
      ControlDef: { name: "qaCtl", scope: "user" },
    });
    const saved = await createControl({
      name: "qaCtl",
      displayName: "QA",
    });
    expect(spy).toHaveBeenCalledWith(PATHS.CE_CONTROLS, {
      ControlDef: { name: "qaCtl", displayName: "QA" },
    });
    expect(saved.name).toBe("qaCtl");
    expect(saved.designGaps).toEqual(CONTROL_DESIGN_GAPS);
  });
});

describe("updateControl / deleteControl", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("PUTs a wrapped body and unwraps the response", async () => {
    const spy = vi.spyOn(client, "put").mockResolvedValue({
      ControlDef: { name: "qaCtl", displayName: "QA", scope: "user" },
    });
    const saved = await updateControl("qaCtl", {
      name: "qaCtl",
      displayName: "QA",
    });
    expect(spy).toHaveBeenCalledWith(`${PATHS.CE_CONTROLS}/qaCtl`, {
      ControlDef: { name: "qaCtl", displayName: "QA" },
    });
    expect(saved.name).toBe("qaCtl");
    expect(saved.designGaps).toEqual(CONTROL_DESIGN_GAPS);
  });

  it("DELETEs by encoded name", async () => {
    const spy = vi.spyOn(client, "del").mockResolvedValue(undefined);
    await deleteControl("qaCtl");
    expect(spy).toHaveBeenCalledWith(`${PATHS.CE_CONTROLS}/qaCtl`);
  });
});
