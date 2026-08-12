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
import { changeMyPassword } from "../../../main/ts/api/user/userPasswordApi";
import { PATHS } from "../../../main/ts/api/paths";
import * as client from "../../../main/ts/api/client";

describe("userPasswordApi.changeMyPassword", () => {
  beforeEach(() => {
    vi.restoreAllMocks();
  });

  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("PUTs User-rooted body to changepw with name, password, email, roles", async () => {
    const putSpy = vi.spyOn(client, "put").mockResolvedValueOnce(undefined);
    await changeMyPassword({
      name: "Admin",
      password: "secret1",
      email: "admin@example.com",
      roles: ["Admin", "Editor"],
    });
    expect(putSpy).toHaveBeenCalledTimes(1);
    expect(putSpy.mock.calls[0][0]).toBe(PATHS.USER_CHANGE_PW);
    const body = putSpy.mock.calls[0][1] as {
      User: { name: string; password: string; email: string; roles: string[] };
    };
    expect(body).toEqual({
      User: {
        name: "Admin",
        password: "secret1",
        email: "admin@example.com",
        roles: ["Admin", "Editor"],
      },
    });
  });

  it("trims name and defaults email/roles when omitted", async () => {
    const putSpy = vi.spyOn(client, "put").mockResolvedValueOnce(undefined);
    await changeMyPassword({ name: "  ed  ", password: "abcdef" });
    const body = putSpy.mock.calls[0][1] as {
      User: { name: string; password: string; email: string; roles: string[] };
    };
    expect(body.User.name).toBe("ed");
    expect(body.User.password).toBe("abcdef");
    expect(body.User.email).toBe("");
    expect(body.User.roles).toEqual([]);
  });

  it("rejects blank name before calling put", async () => {
    const putSpy = vi.spyOn(client, "put");
    await expect(
      changeMyPassword({ name: "   ", password: "abcdef" }),
    ).rejects.toThrow(/User name is required/i);
    expect(putSpy).not.toHaveBeenCalled();
  });

  it("rejects empty password before calling put", async () => {
    const putSpy = vi.spyOn(client, "put");
    await expect(
      changeMyPassword({ name: "Admin", password: "" }),
    ).rejects.toThrow(/Password is required/i);
    expect(putSpy).not.toHaveBeenCalled();
  });
});
