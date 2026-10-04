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
import { copyContentList } from "@/api/publishing/designApi";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    post: vi.fn(),
  };
});

const postMock = vi.mocked(client.post);

describe("copyContentList", () => {
  beforeEach(() => {
    postMock.mockReset();
  });

  it("unwraps a contentList root and posts the copy request", async () => {
    postMock.mockResolvedValue({
      contentList: {
        contentListId: "88",
        name: "NightCl copy",
        description: "kept",
        listType: "modern",
        generator: "sys_Search",
      },
    });
    const copied = await copyContentList({
      sourceContentListId: "5",
      newName: "NightCl copy",
    });
    expect(postMock).toHaveBeenCalledWith(
      expect.stringMatching(/\/sitemanage\/publishingdesign\/contentlists\/copy$/),
      {
        copyContentListRequest: {
          sourceContentListId: "5",
          newName: "NightCl copy",
        },
      },
    );
    expect(copied).toEqual({
      contentListId: "88",
      name: "NightCl copy",
      description: "kept",
      listType: "modern",
      generator: "sys_Search",
      url: undefined,
    });
  });

  it("accepts a numeric content list id from Jackson", async () => {
    postMock.mockResolvedValue({
      contentListId: 12,
      name: "Flat",
    });
    await expect(
      copyContentList({ sourceContentListId: "1", newName: "Flat" }),
    ).resolves.toMatchObject({ contentListId: "12", name: "Flat" });
  });
});
