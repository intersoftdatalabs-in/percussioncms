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
import * as client from "@/api/client";
import {
  listSiteProperties,
  putSiteProperty,
  unwrapSiteProperty,
  unwrapSitePropertyList,
  wrapSiteProperty,
} from "@/api/publishing/designApi";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    get: vi.fn(),
    put: vi.fn(),
  };
});

const getMock = vi.mocked(client.get);
const putMock = vi.mocked(client.put);

describe("site property wire shape", () => {
  beforeEach(() => {
    getMock.mockReset();
    putMock.mockReset();
  });

  it("puts a siteProperty root and unwraps the saved property", async () => {
    putMock.mockResolvedValue({
      siteProperty: { name: "nightVar", contextId: "3", value: "kept-value" },
    });
    const saved = await putSiteProperty("42", {
      name: "nightVar",
      contextId: "3",
      value: "kept-value",
    });
    expect(putMock).toHaveBeenCalledWith(
      expect.stringMatching(/\/sites\/42\/properties$/),
      {
        siteProperty: {
          name: "nightVar",
          contextId: "3",
          value: "kept-value",
        },
      },
    );
    expect(wrapSiteProperty({ name: "nightVar", value: "kept-value" }).siteProperty.name).toBe(
      "nightVar",
    );
    expect(saved).toEqual({
      name: "nightVar",
      contextId: "3",
      value: "kept-value",
    });
  });

  it("unwraps a wrapped property list and a single property document", async () => {
    getMock.mockResolvedValueOnce({
      siteProperty: [
        { name: "kept", contextId: 3, value: "old" },
        { name: "nightVar", contextId: "3", value: "new" },
      ],
    });
    await expect(listSiteProperties("42", "3")).resolves.toEqual([
      { name: "kept", contextId: "3", value: "old" },
      { name: "nightVar", contextId: "3", value: "new" },
    ]);

    expect(
      unwrapSitePropertyList({
        siteProperty: { name: "only", contextId: "1", value: "one" },
      }),
    ).toEqual([{ name: "only", contextId: "1", value: "one" }]);
  });

  it("puts updateValue on a value change and still uses the siteProperty root", async () => {
    putMock.mockResolvedValue({
      siteProperty: { name: "kept", contextId: "3", value: "next" },
    });
    await putSiteProperty("42", {
      name: "kept",
      contextId: "3",
      value: "next",
      updateValue: true,
    });
    expect(putMock).toHaveBeenCalledWith(
      expect.stringMatching(/\/sites\/42\/properties$/),
      {
        siteProperty: {
          name: "kept",
          contextId: "3",
          value: "next",
          updateValue: true,
        },
      },
    );
    expect(wrapSiteProperty({ name: "kept", value: "old" }).siteProperty).toEqual({
      name: "kept",
      contextId: undefined,
      value: "old",
    });
  });

  it("reads a flat property unchanged", () => {
    expect(unwrapSiteProperty({ name: "kept", contextId: "3", value: "old" })).toEqual({
      name: "kept",
      contextId: "3",
      value: "old",
    });
  });
});
