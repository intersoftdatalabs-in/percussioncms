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
  folderRenameFieldReason,
  isValidExplorerFolderName,
} from "../../../main/ts/contentExplorer/folderName";

describe("isValidExplorerFolderName (#4637)", () => {
  it("accepts a single segment", () => {
    expect(isValidExplorerFolderName("qa4637")).toBe(true);
  });

  it("rejects separators and hops", () => {
    expect(isValidExplorerFolderName("a/b")).toBe(false);
    expect(isValidExplorerFolderName("a\\b")).toBe(false);
    expect(isValidExplorerFolderName("..")).toBe(false);
    expect(isValidExplorerFolderName(".")).toBe(false);
    expect(isValidExplorerFolderName("  ")).toBe(false);
  });
});

describe("folderRenameFieldReason (#5478)", () => {
  it("refuses a NUL before blank, separators, unchanged, or collision", () => {
    expect(folderRenameFieldReason("Foo", "New\u0000Name")).toBe("nul");
    expect(folderRenameFieldReason("Foo", " \u0000 ")).toBe("nul");
    expect(folderRenameFieldReason("Foo", "a/b\u0000")).toBe("nul");
    expect(folderRenameFieldReason("Foo", "Foo\u0000")).toBe("nul");
    expect(folderRenameFieldReason("Foo", "taken\u0000", ["taken"])).toBe("nul");
  });

  it("still refuses blank, separators, collision, and an unchanged name", () => {
    expect(folderRenameFieldReason("Foo", "   ")).toBe("blank");
    expect(folderRenameFieldReason("Foo", "a/b")).toBe("invalid");
    expect(folderRenameFieldReason("Foo", "a\\b")).toBe("invalid");
    expect(folderRenameFieldReason("Foo", "taken", ["Foo", "Taken"])).toBe(
      "collision",
    );
    expect(folderRenameFieldReason("Foo", "foo")).toBe("unchanged");
    expect(folderRenameFieldReason("Foo", "Renamed", ["Taken"])).toBeNull();
  });
});
