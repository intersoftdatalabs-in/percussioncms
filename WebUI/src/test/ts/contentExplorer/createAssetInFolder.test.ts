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

import { describe, expect, it } from "vitest";
import {
  assetContentTypeChoices,
  validateCreateAssetFields,
} from "../../../main/ts/contentExplorer/createAssetInFolder";

describe("create asset in selected folder (#4970)", () => {
  it("keeps asset types and drops page types", () => {
    expect(
      assetContentTypeChoices([
        { id: "percImage", name: "Image", label: "Image", contentTypeName: "percImageAsset" },
        { id: "percPage", name: "Page", contentTypeName: "percPage" },
        { id: "percFile", name: "File", label: "File", contentTypeName: "percFileAsset" },
        { id: "dup", name: "Image again", contentTypeName: "percImageAsset" },
        { id: "", name: "blank" },
      ]).map((t) => t.contentType),
    ).toEqual(["percImageAsset", "percFileAsset"]);
  });

  it("rejects a blank name, a path, and a missing or page type", () => {
    expect(validateCreateAssetFields("  ", "percFileAsset")).toBe("missing_name");
    expect(validateCreateAssetFields("a/b", "percFileAsset")).toBe("invalid_name");
    expect(validateCreateAssetFields("logo", "  ")).toBe("missing_type");
    expect(validateCreateAssetFields("logo", "percPage")).toBe("missing_type");
    expect(validateCreateAssetFields("logo", "percFileAsset")).toBeNull();
  });
});
