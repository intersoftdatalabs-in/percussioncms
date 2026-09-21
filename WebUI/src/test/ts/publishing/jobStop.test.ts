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
import { isJobStoppable, mapJobStopError } from "@/publishing/jobStop";

describe("isJobStoppable", () => {
  it("allows stop for a running job with an id", () => {
    expect(
      isJobStoppable({ jobId: 4615, status: "Running", isStopping: false }),
    ).toBe(true);
  });

  it("forbids stop when not running", () => {
    expect(isJobStoppable({ jobId: 1, status: "Completed" })).toBe(false);
    expect(isJobStoppable({ jobId: 1, status: "Failed" })).toBe(false);
  });

  it("forbids stop while already stopping or missing id", () => {
    expect(
      isJobStoppable({ jobId: 1, status: "running", isStopping: true }),
    ).toBe(false);
    expect(isJobStoppable({ status: "running" })).toBe(false);
    expect(isJobStoppable({ jobId: "", status: "running" })).toBe(false);
  });
});

describe("mapJobStopError", () => {
  it("maps 403/404/409 to failure chrome, not success", () => {
    expect(
      mapJobStopError({ status: 403, statusText: "Forbidden", body: {} }),
    ).toMatch(/Forbidden|403/i);
    expect(
      mapJobStopError({ status: 404, statusText: "Not Found", body: {} }),
    ).toMatch(/not found|404/i);
    expect(
      mapJobStopError({ status: 409, statusText: "Conflict", body: {} }),
    ).toMatch(/cannot be stopped|409|Conflict/i);
  });

  it("uses body message when present", () => {
    expect(
      mapJobStopError({
        status: 409,
        statusText: "Conflict",
        body: { message: "Job already complete" },
      }),
    ).toBe("Job already complete");
  });
});
