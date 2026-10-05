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
import { reorderEditionContentList } from "@/api/publishing/designApi";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    put: vi.fn(),
  };
});

const putMock = vi.mocked(client.put);

describe("reorderEditionContentList", () => {
  beforeEach(() => {
    putMock.mockReset();
    putMock.mockResolvedValue(undefined);
  });

  it("puts the editionContentList root with the target sequence", async () => {
    await reorderEditionContentList("7", "6", 0);
    expect(putMock).toHaveBeenCalledWith(
      expect.stringMatching(
        /\/sitemanage\/publishingdesign\/editions\/7\/contentlists\/6\/sequence$/,
      ),
      {
        editionContentList: {
          contentListId: "6",
          sequence: 0,
        },
      },
    );
  });

  it("encodes the edition and content list ids", async () => {
    await reorderEditionContentList("a/b", "c d", 1);
    expect(putMock).toHaveBeenCalledWith(
      expect.stringMatching(
        /\/editions\/a%2Fb\/contentlists\/c%20d\/sequence$/,
      ),
      {
        editionContentList: {
          contentListId: "c d",
          sequence: 1,
        },
      },
    );
  });
});
