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
import { formatCreateFolderError } from "../../../main/ts/contentExplorer/createFolderErrors";

describe("formatCreateFolderError (#4637)", () => {
  it("maps 403/404/409", () => {
    expect(formatCreateFolderError({ status: 403, statusText: "x", body: {} })).toMatch(
      /permission/i,
    );
    expect(formatCreateFolderError({ status: 404, statusText: "x", body: {} })).toMatch(
      /not found/i,
    );
    expect(formatCreateFolderError({ status: 409, statusText: "x", body: {} })).toMatch(
      /Could not create/i,
    );
  });
});
