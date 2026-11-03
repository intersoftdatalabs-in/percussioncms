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
  collectDatetimeNulFieldErrors,
  datetimeContainsNul,
  fromWidgetValue,
  isInvalidEditorDate,
  toWidgetValue,
} from "../../../main/ts/editor/dateField";

describe("toWidgetValue / fromWidgetValue", () => {
  it("round-trips date-only CMS values", () => {
    expect(toWidgetValue("date", "2026-09-18")).toBe("2026-09-18");
    expect(fromWidgetValue("date", "2026-09-18")).toBe("2026-09-18");
  });

  it("round-trips datetime CMS values through datetime-local", () => {
    expect(toWidgetValue("datetime", "2026-09-18 14:30:00")).toBe("2026-09-18T14:30");
    expect(fromWidgetValue("datetime", "2026-09-18T14:30")).toBe("2026-09-18 14:30:00");
  });

  it("rejects impossible calendar days", () => {
    expect(toWidgetValue("date", "2026-02-31")).toBe("");
    expect(isInvalidEditorDate("date", "2026-02-31")).toBe(true);
    expect(isInvalidEditorDate("date", "")).toBe(false);
    expect(isInvalidEditorDate("date", "not-a-date")).toBe(true);
  });
});

describe("datetimeContainsNul", () => {
  it("rejects an embedded NUL and accepts a valid datetime or empty clear", () => {
    expect(datetimeContainsNul("2026-09-18 14:30:00")).toBe(false);
    expect(datetimeContainsNul("2026-09-18T14:30")).toBe(false);
    expect(datetimeContainsNul("")).toBe(false);
    expect(datetimeContainsNul("   ")).toBe(false);
    expect(datetimeContainsNul("2026-09-18 14:30:00\u0000")).toBe(true);
    expect(datetimeContainsNul("\u0000")).toBe(true);
    expect(datetimeContainsNul("2026-09-18T14:30\u0000")).toBe(true);
  });
});

describe("collectDatetimeNulFieldErrors", () => {
  it("flags only datetime rows and leaves date NUL and empty required for other gates", () => {
    expect(
      collectDatetimeNulFieldErrors(
        [
          { name: "event_at", kind: "datetime", value: "2026-01-01 09:00:00\u0000" },
          { name: "ok_at", kind: "datetime", value: "2026-09-18 14:30:00" },
          { name: "empty_at", kind: "datetime", value: "" },
          { name: "event_on", kind: "date", value: "2026-01-01\u0000" },
          { name: "title", kind: "text", value: "z\u0000" },
        ],
        "cannot save",
      ),
    ).toEqual({ event_at: "cannot save" });
  });
});
