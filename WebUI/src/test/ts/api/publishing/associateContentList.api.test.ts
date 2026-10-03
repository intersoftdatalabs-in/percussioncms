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
import { associateContentList } from "@/api/publishing/designApi";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    post: vi.fn(),
  };
});

const postMock = vi.mocked(client.post);

describe("associateContentList", () => {
  beforeEach(() => {
    postMock.mockReset();
  });

  it("posts the editionContentList root and unwraps the content list", async () => {
    postMock.mockResolvedValue({
      contentList: {
        contentListId: "5",
        name: "Home Pages",
        listType: "modern",
      },
    });
    const associated = await associateContentList("7", {
      contentListId: "5",
      deliveryContextId: "9",
    });
    expect(postMock).toHaveBeenCalledWith(
      expect.stringMatching(
        /\/sitemanage\/publishingdesign\/editions\/7\/contentlists$/,
      ),
      {
        editionContentList: {
          contentListId: "5",
          deliveryContextId: "9",
        },
      },
    );
    expect(associated).toMatchObject({
      contentListId: "5",
      name: "Home Pages",
      listType: "modern",
    });
  });

  it("stringifies a numeric contentListId inside the JAXB root", async () => {
    postMock.mockResolvedValue({
      contentList: {
        contentListId: 1013,
        name: "NightAssoc",
        listType: "legacy",
        description: "",
        generator: "",
      },
    });
    await expect(
      associateContentList("7", {
        contentListId: "1013",
        deliveryContextId: "1",
      }),
    ).resolves.toEqual({
      contentListId: "1013",
      name: "NightAssoc",
      listType: "legacy",
      description: "",
      generator: "",
      url: undefined,
    });
  });

  it("accepts an already-flat content list summary", async () => {
    postMock.mockResolvedValue({
      contentListId: "5",
      name: "Flat",
    });
    await expect(
      associateContentList(7, {
        contentListId: "5",
        deliveryContextId: "9",
      }),
    ).resolves.toMatchObject({ name: "Flat", contentListId: "5" });
  });
});
