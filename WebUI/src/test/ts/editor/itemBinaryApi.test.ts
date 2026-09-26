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
  binaryContentUrl,
  filenameFromContentDisposition,
  resolveDownloadFilename,
  unwrapBinaryMeta,
} from "../../../main/ts/editor/itemBinaryApi";

describe("unwrapBinaryMeta", () => {
  it("reads wrapped and flat payloads", () => {
    expect(
      unwrapBinaryMeta({
        ItemEditorBinaryMeta: {
          contentId: "42",
          field: "img",
          filename: "a.png",
          present: true,
        },
      }),
    ).toEqual({
      contentId: "42",
      field: "img",
      filename: "a.png",
      contentType: "",
      present: true,
    });
  });

  it("prefers the stored download name and points at /content", () => {
    expect(binaryContentUrl("42", "img")).toMatch(/\/binary\/42\/img\/content$/);
    expect(filenameFromContentDisposition('attachment; filename="note.txt"')).toBe(
      "note.txt",
    );
    expect(resolveDownloadFilename("", "stored.pdf", "item_file_attachment")).toBe(
      "stored.pdf",
    );
    expect(resolveDownloadFilename("", "", "img")).toBe("img.bin");
  });
});
