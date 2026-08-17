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

import { describe, expect, it, vi, afterEach } from "vitest";
import {
  unwrapItemCreateResult,
  wrapItemCreateRequest,
  createEditorItem,
} from "../../../main/ts/editor/itemCreateApi";
import * as client from "../../../main/ts/api/client";

describe("wrapItemCreateRequest", () => {
  it("wraps bare fields under ItemCreateRequest for CXF JAXB", () => {
    expect(
      wrapItemCreateRequest({
        contentType: "percImageAsset",
        folderPath: "/Assets",
      }),
    ).toEqual({
      ItemCreateRequest: {
        contentType: "percImageAsset",
        folderPath: "/Assets",
      },
    });
  });

  it("does not double-wrap an already-enveloped payload", () => {
    expect(
      wrapItemCreateRequest({
        ItemCreateRequest: {
          contentType: "percRawHtml",
          folderPath: "/Assets",
          name: "stub",
        },
      }),
    ).toEqual({
      ItemCreateRequest: {
        contentType: "percRawHtml",
        folderPath: "/Assets",
        name: "stub",
      },
    });
  });
});

describe("unwrapItemCreateResult", () => {
  it("unwraps Jackson root and PascalCase", () => {
    expect(
      unwrapItemCreateResult({
        ItemCreateResult: { ItemId: "99", FolderPath: "//Sites/x", Name: "n", ContentType: "t" },
      }),
    ).toEqual({
      itemId: "99",
      folderPath: "//Sites/x",
      name: "n",
      contentType: "t",
    });
  });
});

describe("createEditorItem", () => {
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("POSTs the JAXB envelope and unwraps the result", async () => {
    const post = vi.spyOn(client, "post").mockResolvedValue({
      ItemCreateResult: {
        itemId: "42",
        folderPath: "//Assets",
        name: "New-percImageAsset",
        contentType: "percImageAsset",
      },
    });
    await expect(
      createEditorItem({ contentType: "percImageAsset", folderPath: "/Assets" }),
    ).resolves.toEqual({
      itemId: "42",
      folderPath: "//Assets",
      name: "New-percImageAsset",
      contentType: "percImageAsset",
    });
    expect(post).toHaveBeenCalledWith(
      expect.stringContaining("/itemmanagement/item/create"),
      {
        ItemCreateRequest: {
          contentType: "percImageAsset",
          folderPath: "/Assets",
        },
      },
    );
  });
});
