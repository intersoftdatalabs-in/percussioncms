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

import { describe, expect, it, vi } from "vitest";
import {
  replaceContentTypePickerSession,
  settleContentTypePickerSession,
} from "../../../main/ts/contentExplorer/contentTypePickerSession";

const T = [{ name: "percFile", label: "File" }];

describe("contentTypePickerSession", () => {
  it("cancels the previous waiter when a new session opens", () => {
    const first = vi.fn();
    const second = vi.fn();
    const previous = { types: T, resolve: first };
    const next = { types: T, resolve: second };
    const opened = replaceContentTypePickerSession(previous, next);
    expect(opened).toBe(next);
    expect(first).toHaveBeenCalledWith(null);
    expect(second).not.toHaveBeenCalled();
  });

  it("settles the open session and is a no-op when already cleared", () => {
    const resolve = vi.fn();
    const session = { types: T, resolve };
    expect(settleContentTypePickerSession(session, "rffEvent")).toBeNull();
    expect(resolve).toHaveBeenCalledWith("rffEvent");
    expect(settleContentTypePickerSession(null, "rffEvent")).toBeNull();
    expect(resolve).toHaveBeenCalledTimes(1);
  });
});
