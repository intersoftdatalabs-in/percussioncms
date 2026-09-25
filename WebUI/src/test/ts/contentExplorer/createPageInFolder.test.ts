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
  pageContentTypeChoices,
  validateCreatePageFields,
} from "../../../main/ts/contentExplorer/createPageInFolder";

describe("create page in selected folder (#4874)", () => {
  it("keeps only page content types", () => {
    expect(
      pageContentTypeChoices([
        { name: "percImage", label: "Image" },
        { name: "percPage", label: "Page" },
        { name: "Page", label: "Page again" },
        { name: "percPage", label: "dup" },
      ]).map((t) => t.name),
    ).toEqual(["percPage", "Page"]);
  });

  it("rejects a blank name, a path, and a missing type", () => {
    expect(validateCreatePageFields("  ", "percPage")).toBe("missing_name");
    expect(validateCreatePageFields("a/b", "percPage")).toBe("invalid_name");
    expect(validateCreatePageFields("Home", "  ")).toBe("missing_type");
    expect(validateCreatePageFields("Home", "percPage")).toBeNull();
  });
});
