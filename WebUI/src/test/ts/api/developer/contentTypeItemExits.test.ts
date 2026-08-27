/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 */

import { describe, expect, it } from "vitest";
import {
  addItemExit,
  cloneContentTypeItemExits,
  contentTypeItemExitsEqual,
  emptyContentTypeItemExits,
  listContainsExtension,
  removeItemExit,
  toContentTypeItemExitsPutBody,
} from "../../../../main/ts/api/developer/contentTypeItemExits";

describe("contentTypeItemExits helpers (CD-09)", () => {
  it("adds an extension with optional parameter and skips duplicates", () => {
    const empty = emptyContentTypeItemExits();
    const added = addItemExit(
      empty,
      "inputTranslations",
      "Java/global/percussion/generic/sys_ToUpperCase",
      "sys_title",
    );
    expect(added.inputTranslations).toHaveLength(1);
    expect(added.inputTranslations?.[0]).toEqual({
      extension: "Java/global/percussion/generic/sys_ToUpperCase",
      parameters: [{ value: "sys_title" }],
    });
    const dup = addItemExit(
      added,
      "inputTranslations",
      "JAVA/global/percussion/generic/sys_ToUpperCase",
    );
    expect(dup).toBe(added);
  });

  it("removes by index and ignores out-of-range", () => {
    const env = addItemExit(
      emptyContentTypeItemExits(),
      "validations",
      "Java/global/percussion/content/sys_ValidateRequiredField",
    );
    expect(removeItemExit(env, "validations", 3)).toBe(env);
    const next = removeItemExit(env, "validations", 0);
    expect(next.validations).toEqual([]);
  });

  it("compares lists and max-errors independently of designGaps", () => {
    const a = addItemExit(
      emptyContentTypeItemExits(),
      "preExits",
      "Java/global/percussion/generic/sys_ToUpperCase",
    );
    const b = cloneContentTypeItemExits(a);
    b.designGaps = [{ code: "CT_ITEM_EXIT_CONDITIONS", message: "x" }];
    expect(contentTypeItemExitsEqual(a, b)).toBe(true);
    b.maxErrorsToStopValidation = 5;
    expect(contentTypeItemExitsEqual(a, b)).toBe(false);
  });

  it("builds a PUT body with all five lists and omits designGaps", () => {
    const env = addItemExit(
      emptyContentTypeItemExits(),
      "inputTranslations",
      "Java/global/percussion/generic/sys_ToUpperCase",
      "sys_title",
    );
    env.maxErrorsToStopValidation = 10;
    env.designGaps = [{ code: "CT_ITEM_EXIT_CONDITIONS" }];
    const body = toContentTypeItemExitsPutBody(env);
    expect(body.designGaps).toBeUndefined();
    expect(body.preExits).toBeUndefined();
    expect(body.postExits).toBeUndefined();
    expect(body.maxErrorsToStopValidation).toBeUndefined();
    const withPipe = toContentTypeItemExitsPutBody(env, true, true);
    expect(withPipe.preExits).toEqual([]);
    expect(withPipe.postExits).toEqual([]);
    expect(body.inputTranslations?.[0]?.extension).toBe(
      "Java/global/percussion/generic/sys_ToUpperCase",
    );
    expect(withPipe.maxErrorsToStopValidation).toBe(10);
  });

  it("detects an extension already in a list", () => {
    const env = addItemExit(
      emptyContentTypeItemExits(),
      "outputTranslations",
      "Java/global/percussion/generic/sys_ToUpperCase",
    );
    expect(
      listContainsExtension(
        env.outputTranslations,
        "Java/global/percussion/generic/sys_ToUpperCase",
      ),
    ).toBe(true);
    expect(listContainsExtension(env.outputTranslations, "other")).toBe(false);
  });
});
