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
import { formatCopyFolderError } from "../../../main/ts/contentExplorer/copyFolderErrors";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import { message } from "../../../main/ts/i18n/message";

describe("formatCopyFolderError (#4750)", () => {
  it("maps HTTP 400 to a bad-request folder-copy message", () => {
    expect(
      formatCopyFolderError({ status: 400, statusText: "Bad Request", body: {} }),
    ).toBe(message(EXPLORER_MSG.SUBFOLDER_COPY_BAD_REQUEST));
  });

  it("maps HTTP 403 to permission denied", () => {
    expect(
      formatCopyFolderError({ status: 403, statusText: "Forbidden", body: {} }),
    ).toBe(message(EXPLORER_MSG.SUBFOLDER_COPY_FORBIDDEN));
  });

  it("maps HTTP 404 to folder not found", () => {
    expect(
      formatCopyFolderError({ status: 404, statusText: "Not Found", body: {} }),
    ).toBe(message(EXPLORER_MSG.SUBFOLDER_COPY_NOT_FOUND));
  });

  it("maps HTTP 409 to a destination conflict", () => {
    expect(
      formatCopyFolderError({ status: 409, statusText: "Conflict", body: {} }),
    ).toBe(message(EXPLORER_MSG.SUBFOLDER_COPY_CONFLICT));
  });

  it("does not treat other statuses as success", () => {
    expect(
      formatCopyFolderError({ status: 500, statusText: "Error", body: {} }),
    ).toMatch(/500/);
  });
});
