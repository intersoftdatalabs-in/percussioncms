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
import { unwrapViewDef, unwrapViewDefList } from "../../../../main/ts/api/developer/viewsApi";

describe("unwrapViewDef (#3380)", () => {
  it("unwraps Jackson ViewDef root envelope so guid is reachable", () => {
    const unwrapped = unwrapViewDef({
      ViewDef: {
        name: "Inbox",
        id: 3,
        guid: { stringValue: "0-18-3", type: 18, uuid: 3 },
      },
    });
    expect(unwrapped.name).toBe("Inbox");
    expect(unwrapped.guid?.stringValue).toBe("0-18-3");
    expect(unwrapped.guidString).toBe("0-18-3");
  });

  it("synthesizes 0-18-{id} when Guid is omitted", () => {
    const unwrapped = unwrapViewDef({ name: "Inbox", id: 11 });
    expect(unwrapped.guid?.stringValue).toBe("0-18-11");
    expect(unwrapped.guidString).toBe("0-18-11");
  });

  it("uses nested Guid envelope when stringValue is nested", () => {
    const unwrapped = unwrapViewDef({
      name: "Inbox",
      guid: { Guid: { stringValue: "0-18-4", type: 18, uuid: 4 } },
    });
    expect(unwrapped.guidString).toBe("0-18-4");
  });

  it("returns empty object for null payload", () => {
    expect(unwrapViewDef(null)).toEqual({});
  });
});

describe("unwrapViewDefList", () => {
  it("normalizes each list row GUID", () => {
    const list = unwrapViewDefList({
      ViewDef: [
        { name: "Inbox", id: 3 },
        { name: "Outbox", guidString: "0-18-4" },
      ],
    });
    expect(list).toHaveLength(2);
    expect(list[0].guidString).toBe("0-18-3");
    expect(list[1].guidString).toBe("0-18-4");
  });
});
