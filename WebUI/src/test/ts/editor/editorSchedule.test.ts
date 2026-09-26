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
import { SessionRedirectError } from "../../../main/ts/api/client";
import { editorScheduleFailureMessage } from "../../../main/ts/editor/editorSchedule";

describe("editorScheduleFailureMessage", () => {
  it("does not claim success for HTTP 400 or 403", () => {
    expect(
      editorScheduleFailureMessage({
        status: 400,
        statusText: "Bad Request",
        body: { message: "dates overlap" },
      }),
    ).toMatch(/not valid|dates overlap/i);
    expect(
      editorScheduleFailureMessage({
        status: 403,
        statusText: "Forbidden",
        body: {},
      }),
    ).toMatch(/not allowed/i);
  });

  it("maps application-level FORBIDDEN and INVALID", () => {
    expect(editorScheduleFailureMessage(new Error("FORBIDDEN"))).toMatch(
      /not allowed/i,
    );
    expect(editorScheduleFailureMessage(new Error("INVALID range"))).toMatch(
      /not valid/i,
    );
  });

  it("stays silent when the session is already redirecting", () => {
    expect(editorScheduleFailureMessage(new SessionRedirectError())).toBe("");
  });
});
