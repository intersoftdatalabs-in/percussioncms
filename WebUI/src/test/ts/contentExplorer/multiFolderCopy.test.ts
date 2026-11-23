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
import type { PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import {
  fallbackLabelFromKey,
  message,
} from "../../../main/ts/i18n/message";
import {
  copyCheckedItemsToFolder,
  formatMultiFolderCopyStatus,
  multiCopyOutcome,
  partitionMultiFolderCopy,
} from "../../../main/ts/contentExplorer/multiFolderCopy";

const page = (name: string, path: string): PSPathItem => ({
  id: name,
  name,
  path,
  type: "percPage",
  category: "page",
  leaf: true,
});

const folder = (name: string, path: string): PSPathItem => ({
  id: name,
  name,
  path: path.endsWith("/") ? path : `${path}/`,
  type: "folder",
  category: "folder",
  leaf: false,
});

const failCopy = async (): Promise<never> => {
  throw new Error("must not be called");
};

describe("multi-select copy to one folder (#6255)", () => {
  it("partitions folders to the folder copy call and pages to the item call", () => {
    const parts = partitionMultiFolderCopy([
      folder("Section", "/Assets/Section"),
      page("Home", "/Assets/Section/Home"),
      page("Logo", "/Assets/Section/Logo"),
    ]);
    expect(parts.folders.map((item) => item.name)).toEqual(["Section"]);
    expect(parts.copyable.map((item) => item.name)).toEqual(["Home", "Logo"]);
    expect(parts.missingPath).toEqual([]);
  });

  it("treats rows with no path as failures, not copies", () => {
    const parts = partitionMultiFolderCopy([
      { ...page("NoPath", ""), path: "" },
      { ...folder("NoPathFolder", ""), path: "" },
    ]);
    expect(parts.copyable).toEqual([]);
    expect(parts.folders).toEqual([]);
    expect(parts.missingPath.map((item) => item.id)).toEqual([
      "NoPath",
      "NoPathFolder",
    ]);
  });

  it("one destination receives every item and every folder tree", async () => {
    const itemCalls: Array<[string, string]> = [];
    const folderCalls: Array<[string, string]> = [];
    const result = await copyCheckedItemsToFolder(
      [
        page("Home", "/Assets/Home"),
        folder("Section", "/Assets/Section"),
        page("Logo", "/Assets/Logo"),
      ],
      "/Assets/Dest",
      async (source, target) => {
        itemCalls.push([source, target]);
      },
      async (source, target) => {
        folderCalls.push([source, target]);
        return "Section";
      },
    );
    expect(itemCalls).toEqual([
      ["/Assets/Home", "/Assets/Dest"],
      ["/Assets/Logo", "/Assets/Dest"],
    ]);
    expect(folderCalls).toEqual([["/Assets/Section/", "/Assets/Dest"]]);
    expect(result.copiedNames).toEqual(["Home", "Logo", "Section"]);
    expect(result.failures).toEqual([]);
    expect(result.fullSuccess).toBe(true);
    expect(multiCopyOutcome(result)).toBe("success");
    expect(formatMultiFolderCopyStatus(result, "/Assets/Dest")).toContain(
      "3",
    );
    expect(formatMultiFolderCopyStatus(result, "/Assets/Dest")).not.toContain(
      message(EXPLORER_MSG.MULTI_COPY_PARTIAL).slice(0, 12),
    );
  });

  it("names the created copy when the server uniquifies a conflict", async () => {
    const result = await copyCheckedItemsToFolder(
      [folder("Section", "/Assets/Section")],
      "/Assets/Dest",
      failCopy,
      async () => {
        // Server 200 with getUniqueFolderName: the copy lands uniquified.
        return "Section_1";
      },
    );
    expect(result.copiedNames).toEqual(["Section_1"]);
    expect(result.fullSuccess).toBe(true);
    expect(formatMultiFolderCopyStatus(result, "/Assets/Dest")).toContain(
      "Section_1",
    );
  });

  it("falls back to the source name when the response carries no created name", async () => {
    const result = await copyCheckedItemsToFolder(
      [folder("Section", "/Assets/Section")],
      "/Assets/Dest",
      failCopy,
      async () => null,
    );
    expect(result.copiedNames).toEqual(["Section"]);
    expect(result.fullSuccess).toBe(true);
  });

  it("names per-folder 404 with the folder message and stays partial", async () => {
    const copied: string[] = [];
    const result = await copyCheckedItemsToFolder(
      [
        folder("Gone", "/Assets/Gone"),
        page("Ok", "/Assets/Ok"),
        page("Missing", "/Assets/Missing"),
      ],
      "/Assets/Dest",
      async (source) => {
        if (source.endsWith("Missing")) {
          throw { status: 404, statusText: "Not Found", body: {} };
        }
        copied.push(source);
      },
      async (source) => {
        if (source.includes("Gone")) {
          throw { status: 404, statusText: "Not Found", body: {} };
        }
        copied.push(source);
        return null;
      },
    );
    expect(copied).toEqual(["/Assets/Ok"]);
    expect(result.copiedNames).toEqual(["Ok"]);
    expect(result.failures.map((failure) => failure.name)).toEqual([
      "Missing",
      "Gone",
    ]);
    expect(result.fullSuccess).toBe(false);
    expect(multiCopyOutcome(result)).toBe("partial");
    const text = formatMultiFolderCopyStatus(result, "/Assets/Dest");
    expect(text).toContain("Gone");
    expect(text).toContain("Missing");
    expect(text).toContain("were not rolled back");
    expect(text).not.toBe(
      message(EXPLORER_MSG.MULTI_COPY_SUCCESS)
        .replace("{count}", "1")
        .replace("{path}", "/Assets/Dest"),
    );
  });

  it("names per-folder 403 and 409 without claiming success", async () => {
    const result = await copyCheckedItemsToFolder(
      [folder("Locked", "/Assets/Locked")],
      "/Assets/Dest",
      failCopy,
      async (): Promise<string | null> => {
        throw { status: 403, statusText: "Forbidden", body: {} };
      },
    );
    expect(result.copiedNames).toEqual([]);
    expect(result.failures.map((failure) => failure.name)).toEqual(["Locked"]);
    expect(result.fullSuccess).toBe(false);
    expect(multiCopyOutcome(result)).toBe("none");
    const text = formatMultiFolderCopyStatus(result, "/Assets/Dest");
    expect(text).toContain("Locked");
    expect(text).toContain(message(EXPLORER_MSG.MULTI_COPY_NONE));
  });

  it("does not report success when nothing copied", async () => {
    const result = await copyCheckedItemsToFolder(
      [{ ...page("NoPath", ""), path: "" }],
      "/Assets/Dest",
      failCopy,
      failCopy,
    );
    expect(result.fullSuccess).toBe(false);
    expect(multiCopyOutcome(result)).toBe("none");
    expect(formatMultiFolderCopyStatus(result, "/Assets/Dest")).toContain(
      "NoPath",
    );
    expect(formatMultiFolderCopyStatus(result, "/Assets/Dest")).toContain(
      message(EXPLORER_MSG.MULTI_COPY_NONE),
    );
  });

  it("falls back to the catalog unnamed item for a nameless row", async () => {
    const prior = (window as unknown as { I18N?: unknown }).I18N;
    // Simulate a non-English session catalog: a hardcoded "(unnamed)"
    // literal would ignore this stub and fail the assertions below.
    (window as unknown as { I18N: { message: (k: string) => string } }).I18N = {
      message: (key: string) =>
        key === EXPLORER_MSG.MULTI_UNNAMED_ITEM
          ? "[article-sans-nom]"
          : fallbackLabelFromKey(key),
    };
    try {
      const result = await copyCheckedItemsToFolder(
        [{ ...page("", ""), id: "blank" }],
        "/Assets/Dest",
        failCopy,
        failCopy,
      );
      // Resolved through EXPLORER_MSG so session locales apply, not a
      // hardcoded English literal.
      expect(result.failures.map((failure) => failure.name)).toEqual([
        "[article-sans-nom]",
      ]);
      expect(formatMultiFolderCopyStatus(result, "/Assets/Dest")).toContain(
        "[article-sans-nom]",
      );
    } finally {
      (window as unknown as { I18N?: unknown }).I18N = prior;
    }
  });
});
