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
  createContext,
  listContexts,
  unwrapContext,
  updateContext,
  wrapContext,
} from "@/api/publishing/designApi";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    get: vi.fn(),
    post: vi.fn(),
    put: vi.fn(),
  };
});

const getMock = vi.mocked(client.get);
const postMock = vi.mocked(client.post);
const putMock = vi.mocked(client.put);

describe("publishing context wire shape", () => {
  beforeEach(() => {
    getMock.mockReset();
    postMock.mockReset();
    putMock.mockReset();
  });

  it("lists wrapped contexts with string ids so edit can match the select", async () => {
    getMock.mockResolvedValue({
      context: [
        {
          contextId: 1012,
          name: "CtxDst",
          description: "Night copy source",
          defaultSchemeId: 11,
        },
      ],
    });
    await expect(listContexts()).resolves.toEqual([
      {
        contextId: "1012",
        name: "CtxDst",
        description: "Night copy source",
        defaultSchemeId: "11",
      },
    ]);
  });

  it("posts a context root without a scheme id and unwraps the created row", async () => {
    postMock.mockResolvedValue({
      context: {
        contextId: 12,
        name: "Publish copy",
        description: "Public site",
      },
    });
    const created = await createContext({
      name: "Publish copy",
      description: "Public site",
    });
    expect(postMock).toHaveBeenCalledWith(
      expect.stringMatching(/\/publishingdesign\/contexts$/),
      {
        context: {
          name: "Publish copy",
          description: "Public site",
        },
      },
    );
    expect(created).toEqual({
      contextId: "12",
      name: "Publish copy",
      description: "Public site",
    });
    expect(created).not.toHaveProperty("defaultSchemeId");
  });

  it("puts a context root on update", async () => {
    putMock.mockResolvedValue({
      context: { contextId: "3", name: "Publish", description: "edited" },
    });
    const updated = await updateContext("3", { name: "Publish", description: "edited" });
    expect(putMock).toHaveBeenCalledWith(
      expect.stringMatching(/\/publishingdesign\/contexts\/3$/),
      { context: { name: "Publish", description: "edited" } },
    );
    expect(updated.contextId).toBe("3");
    expect(updated.description).toBe("edited");
  });

  it("wrapContext omits id and default scheme when the copy body does not have them", () => {
    expect(wrapContext({ name: "Publish copy", description: "Public site" })).toEqual({
      context: { name: "Publish copy", description: "Public site" },
    });
  });

  it("unwrapContext accepts an already-flat summary", () => {
    expect(unwrapContext({ contextId: "4", name: "Preview" })).toEqual({
      contextId: "4",
      name: "Preview",
    });
  });
});
