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

const putPlainText = vi.fn();

vi.mock("../../../main/ts/api/client", () => ({
  get: vi.fn(),
  put: vi.fn(),
  putPlainText: (...args: unknown[]) => putPlainText(...args),
}));

import { updateMyDefaultCommunity } from "../../../main/ts/api/user/userProfileApi";

describe("userProfileApi default community (#3508)", () => {
  beforeEach(() => {
    putPlainText.mockReset();
  });

  it("PUTs text/plain to the current-user defaultCommunity path", async () => {
    putPlainText.mockResolvedValue({
      CurrentUser: {
        name: "Admin",
        communities: ["Default", "Corporate"],
        defaultCommunity: "Corporate",
      },
    });

    const profile = await updateMyDefaultCommunity("Corporate");

    expect(putPlainText).toHaveBeenCalledTimes(1);
    expect(putPlainText.mock.calls[0][0]).toMatch(/\/user\/user\/defaultCommunity$/);
    expect(putPlainText.mock.calls[0][1]).toBe("Corporate");
    expect(profile.defaultCommunity).toBe("Corporate");
    expect(profile.communities).toEqual(["Default", "Corporate"]);
  });

  it("trims the community name before PUT", async () => {
    putPlainText.mockResolvedValue({
      defaultCommunity: "",
      name: "Admin",
    });
    await updateMyDefaultCommunity("  ");
    expect(putPlainText.mock.calls[0][1]).toBe("");
  });
});
