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
import { formatMoveItemError } from "../../../main/ts/contentExplorer/moveItemErrors";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import { message } from "../../../main/ts/i18n/message";

describe("formatMoveItemError (#4601)", () => {
  it("maps HTTP 403 to permission denied", () => {
    expect(formatMoveItemError({ status: 403, statusText: "Forbidden", body: {} })).toBe(
      message(EXPLORER_MSG.PERMISSION_DENIED),
    );
  });

  it("maps HTTP 404 to move not found", () => {
    expect(formatMoveItemError({ status: 404, statusText: "Not Found", body: {} })).toBe(
      message(EXPLORER_MSG.ACTION_MOVE_NOT_FOUND),
    );
  });

  it("maps HTTP 409 to move conflict", () => {
    expect(formatMoveItemError({ status: 409, statusText: "Conflict", body: {} })).toBe(
      message(EXPLORER_MSG.ACTION_MOVE_CONFLICT),
    );
  });

  it("does not treat other statuses as success", () => {
    expect(
      formatMoveItemError({ status: 500, statusText: "Error", body: {} }),
    ).toMatch(/500/);
  });
});
