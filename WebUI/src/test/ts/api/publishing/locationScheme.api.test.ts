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
  createScheme,
  getScheme,
  unwrapLocationScheme,
  unwrapLocationSchemeList,
  wrapLocationScheme,
} from "@/api/publishing/designApi";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    get: vi.fn(),
    post: vi.fn(),
  };
});

const getMock = vi.mocked(client.get);
const postMock = vi.mocked(client.post);

describe("location scheme wire shape", () => {
  beforeEach(() => {
    getMock.mockReset();
    postMock.mockReset();
  });

  it("posts a locationScheme root and unwraps the created scheme", async () => {
    postMock.mockResolvedValue({
      locationScheme: {
        schemeId: 12,
        name: "Article copy",
        generator: "gen",
        parameters: {
          schemeParameter: {
            name: "path",
            type: "String",
            value: "$sys.site.path",
            sequence: 0,
          },
        },
      },
    });
    const created = await createScheme("3", {
      name: "Article copy",
      generator: "gen",
      parameters: [{ name: "path", type: "String", value: "$sys.site.path", sequence: 0 }],
    });
    expect(postMock).toHaveBeenCalledWith(
      expect.stringMatching(/\/contexts\/3\/schemes$/),
      {
        locationScheme: {
          name: "Article copy",
          description: undefined,
          contextId: undefined,
          generator: "gen",
          contentTypeId: undefined,
          templateId: undefined,
          schemeParameter: [
            { name: "path", type: "String", value: "$sys.site.path", sequence: 0 },
          ],
        },
      },
    );
    expect(wrapLocationScheme({ name: "Article copy", generator: "gen" }).locationScheme.name).toBe(
      "Article copy",
    );
    expect(created.schemeId).toBe("12");
    expect(created.parameters?.[0]?.value).toBe("$sys.site.path");
  });

  it("unwraps a single-scheme GET", async () => {
    getMock.mockResolvedValue({
      locationScheme: { schemeId: "11", name: "Article", generator: "gen" },
    });
    await expect(getScheme("11")).resolves.toMatchObject({
      schemeId: "11",
      name: "Article",
      generator: "gen",
    });
  });

  it("unwraps a wrapped scheme list", () => {
    expect(
      unwrapLocationSchemeList({
        locationScheme: [{ schemeId: 11, name: "Article" }],
      }),
    ).toEqual([
      {
        schemeId: "11",
        name: "Article",
        description: undefined,
        contextId: undefined,
        generator: undefined,
        contentTypeId: undefined,
        templateId: undefined,
        schemeType: undefined,
        parameters: undefined,
      },
    ]);
  });

  it("reads a flat summary unchanged", () => {
    expect(unwrapLocationScheme({ schemeId: "4", name: "File" }).name).toBe("File");
  });
});
