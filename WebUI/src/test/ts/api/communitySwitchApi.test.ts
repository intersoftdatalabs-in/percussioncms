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

import { beforeEach, describe, expect, it, vi } from "vitest";

const post = vi.fn();

vi.mock("../../../main/ts/api/client", () => ({
  post: (...args: unknown[]) => post(...args),
}));

import {
  allowedSessionCommunities,
  communitySwitchUrl,
  switchSessionCommunity,
} from "../../../main/ts/api/user/communitySwitchApi";

describe("communitySwitchApi", () => {
  beforeEach(() => {
    post.mockReset();
    post.mockResolvedValue({ Status: { message: "OK", statusCode: 200 } });
  });

  it("builds a path-encoded switch URL", () => {
    const url = communitySwitchUrl("Enterprise Investments");
    expect(url).toContain("/communities/switch/");
    expect(url).toContain(encodeURIComponent("Enterprise Investments"));
    expect(url).not.toContain("Enterprise Investments");
  });

  it("rejects a blank community name", () => {
    expect(() => communitySwitchUrl("  ")).toThrow(/required/i);
  });

  it("dedupes membership names and ignores blanks", () => {
    expect(
      allowedSessionCommunities(["Default", " Default ", "", "Corporate", "Default"]),
    ).toEqual(["Default", "Corporate"]);
    expect(allowedSessionCommunities(null)).toEqual([]);
  });

  it("POSTs switch without a body", async () => {
    await switchSessionCommunity("Corporate");
    expect(post).toHaveBeenCalledTimes(1);
    expect(post).toHaveBeenCalledWith(
      expect.stringMatching(/\/communities\/switch\/Corporate$/),
    );
    expect(post.mock.calls[0]).toHaveLength(1);
  });
});
