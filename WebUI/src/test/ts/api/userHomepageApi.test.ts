/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import {
  asPlainHomepageString,
  canonicalizeHomepageType,
  getMyHomepageOverride,
  HOMEPAGE_TYPES,
  setMyHomepageOverride,
} from "../../../main/ts/api/user/userHomepageApi";
import * as client from "../../../main/ts/api/client";

describe("userHomepageApi", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("asPlainHomepageString unwraps quoted JSON and object wrappers", () => {
    expect(asPlainHomepageString(null)).toBe("");
    expect(asPlainHomepageString("Editor")).toBe("Editor");
    expect(asPlainHomepageString('"Editor"')).toBe("Editor");
    expect(asPlainHomepageString({ homepage: "Home" })).toBe("Home");
    expect(asPlainHomepageString({ value: "Architecture" })).toBe("Architecture");
  });

  it("canonicalizeHomepageType maps aliases to product types", () => {
    expect(canonicalizeHomepageType("")).toBe("");
    expect(canonicalizeHomepageType("Editor")).toBe(HOMEPAGE_TYPES.EDITOR);
    expect(canonicalizeHomepageType("editor")).toBe(HOMEPAGE_TYPES.EDITOR);
    expect(canonicalizeHomepageType("arch")).toBe(HOMEPAGE_TYPES.ARCHITECTURE);
    expect(canonicalizeHomepageType("explorer")).toBe(HOMEPAGE_TYPES.EXPLORER);
    expect(canonicalizeHomepageType("Explorer")).toBe(HOMEPAGE_TYPES.EXPLORER);
    expect(canonicalizeHomepageType("developer")).toBe(HOMEPAGE_TYPES.DEVELOPER);
    expect(canonicalizeHomepageType('"Home"')).toBe(HOMEPAGE_TYPES.HOME);
    expect(canonicalizeHomepageType("explorer")).toBe(HOMEPAGE_TYPES.EXPLORER);
    expect(canonicalizeHomepageType("developer")).toBe(HOMEPAGE_TYPES.DEVELOPER);
  });

  it("getMyHomepageOverride treats 404 as empty override", async () => {
    vi.spyOn(client, "get").mockRejectedValueOnce({
      status: 404,
      statusText: "Not Found",
      body: null,
    });
    await expect(getMyHomepageOverride()).resolves.toBe("");
  });

  it("getMyHomepageOverride canonicalizes a quoted JSON body", async () => {
    vi.spyOn(client, "get").mockResolvedValueOnce('"Editor"');
    await expect(getMyHomepageOverride()).resolves.toBe(HOMEPAGE_TYPES.EDITOR);
  });

  it("setMyHomepageOverride PUTs text/plain and returns canonical type", async () => {
    const putSpy = vi.spyOn(client, "putPlainText").mockResolvedValueOnce("Editor");
    await expect(setMyHomepageOverride("editor")).resolves.toBe(
      HOMEPAGE_TYPES.EDITOR,
    );
    expect(putSpy).toHaveBeenCalled();
    expect(putSpy.mock.calls[0][1]).toBe(HOMEPAGE_TYPES.EDITOR);
  });

  it("setMyHomepageOverride blank value DELETEs instead of PUT", async () => {
    const delSpy = vi.spyOn(client, "del").mockResolvedValueOnce(undefined);
    const putSpy = vi.spyOn(client, "putPlainText");
    await expect(setMyHomepageOverride("")).resolves.toBe("");
    expect(delSpy).toHaveBeenCalled();
    expect(putSpy).not.toHaveBeenCalled();
  });
});
