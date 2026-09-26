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
  isJobRetryable,
  jobRetryKind,
  jobServerName,
  mapJobRetryError,
  mapJobRetryResponse,
} from "@/publishing/jobRetry";

describe("jobRetry", () => {
  it("offers retry only for failed jobs that name a site and server", () => {
    expect(
      isJobRetryable({
        jobId: 1,
        status: "Failed",
        siteName: "FastForward",
        pubServerName: "Production",
      }),
    ).toBe(true);
    expect(
      isJobRetryable({
        jobId: 2,
        status: "Completed with failures",
        siteName: "FastForward",
        serverName: "Staging",
      }),
    ).toBe(true);
    expect(
      isJobRetryable({
        jobId: 3,
        status: "Running",
        siteName: "FastForward",
        serverName: "Production",
      }),
    ).toBe(false);
    expect(
      isJobRetryable({
        jobId: 4,
        status: "Completed",
        siteName: "FastForward",
        serverName: "Production",
      }),
    ).toBe(false);
    expect(
      isJobRetryable({
        jobId: 5,
        status: "Failed",
        siteName: " ",
        serverName: "Production",
      }),
    ).toBe(false);
    expect(
      isJobRetryable({
        jobId: 6,
        status: "Failed",
        siteName: "FastForward",
      }),
    ).toBe(false);
  });

  it("reads the server from pubServerName when serverName is absent", () => {
    expect(jobServerName({ pubServerName: "Production" })).toBe("Production");
    expect(jobServerName({ serverName: "Staging", pubServerName: "Other" })).toBe(
      "Staging",
    );
  });

  it("uses incremental only when the payload says so", () => {
    expect(jobRetryKind({ publishKind: "incremental" })).toBe("incremental");
    expect(jobRetryKind({ editionName: "Production_INCREMENTAL" })).toBe(
      "incremental",
    );
    expect(jobRetryKind({ publishKind: "full" })).toBe("full");
    expect(jobRetryKind({})).toBe("full");
  });

  it("treats application FORBIDDEN and BADCONFIG as errors", () => {
    expect(mapJobRetryResponse({ status: "FORBIDDEN" })).toMatch(/Forbidden/i);
    expect(mapJobRetryResponse({ status: "BADCONFIG", warningMessage: "no host" })).toBe(
      "no host",
    );
    expect(mapJobRetryResponse({ status: "Queuing content", jobid: 9 })).toBeNull();
    expect(mapJobRetryError({ status: 403, body: "FORBIDDEN" })).toMatch(/Forbidden/i);
  });
});
