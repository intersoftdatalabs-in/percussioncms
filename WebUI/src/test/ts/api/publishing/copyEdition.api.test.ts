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
import { copyEdition } from "@/api/publishing/designApi";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    post: vi.fn(),
  };
});

const postMock = vi.mocked(client.post);

describe("copyEdition", () => {
  beforeEach(() => {
    postMock.mockReset();
  });

  it("unwraps a JAXB edition root and posts the copy request", async () => {
    postMock.mockResolvedValue({
      edition: { editionId: "88", name: "NightCopy", siteId: "9" },
    });
    const copied = await copyEdition({
      sourceEditionId: "7",
      targetSiteId: "9",
      newName: "NightCopy",
      copyContentLists: true,
    });
    expect(postMock).toHaveBeenCalledWith(
      expect.stringMatching(/\/sitemanage\/publishingdesign\/editions\/copy$/),
      {
        sourceEditionId: "7",
        targetSiteId: "9",
        newName: "NightCopy",
        copyContentLists: true,
      },
    );
    expect(copied).toEqual({
      editionId: "88",
      name: "NightCopy",
      siteId: "9",
    });
  });

  it("accepts an already-flat edition summary", async () => {
    postMock.mockResolvedValue({
      editionId: "12",
      name: "Flat",
      siteId: "1",
    });
    await expect(
      copyEdition({ sourceEditionId: "1", targetSiteId: "1" }),
    ).resolves.toMatchObject({ name: "Flat", editionId: "12" });
  });
});
