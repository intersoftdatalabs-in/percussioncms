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
  CONNECTION_TEST_FAILURE,
  CONNECTION_TEST_SUCCESS,
  connectionTestHttpFailure,
  isConnectionTestSuccess,
} from "@/publishing/deliveryServerConnectionTest";

describe("deliveryServerConnectionTest", () => {
  it("reads the connected flag and never reports success without proof", () => {
    expect(isConnectionTestSuccess({ connected: true })).toBe(true);
    expect(isConnectionTestSuccess({ connected: false })).toBe(false);
    expect(isConnectionTestSuccess(true)).toBe(true);
    expect(isConnectionTestSuccess(false)).toBe(false);
    expect(isConnectionTestSuccess({})).toBe(false);
    expect(isConnectionTestSuccess(null)).toBe(false);
    expect(isConnectionTestSuccess(undefined)).toBe(false);
    expect(isConnectionTestSuccess("ok")).toBe(false);
    expect(isConnectionTestSuccess({ connected: "yes" })).toBe(false);
  });

  it("exposes fixed success and failure notice copy", () => {
    expect(CONNECTION_TEST_SUCCESS).toMatch(/succeeded/i);
    expect(CONNECTION_TEST_FAILURE).toMatch(/failed/i);
  });

  it("maps 400, 403, and 409 to the save-error path", () => {
    expect(connectionTestHttpFailure(400)).toBe("bad_request");
    expect(connectionTestHttpFailure(403)).toBe("forbidden");
    expect(connectionTestHttpFailure(409)).toBe("conflict");
    expect(connectionTestHttpFailure(200)).toBeNull();
    expect(connectionTestHttpFailure(404)).toBeNull();
    expect(connectionTestHttpFailure(500)).toBeNull();
  });
});
