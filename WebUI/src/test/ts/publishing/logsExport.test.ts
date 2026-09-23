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
  buildPublishingLogsCsv,
  PUBLISHING_LOG_CSV_HEADERS,
} from "@/publishing/logsExport";
import { filterLogEntries } from "@/publishing/logsFilter";
import type { PublishingLogEntry } from "@/publishing/types";

const LOGS: PublishingLogEntry[] = [
  {
    jobId: 10,
    siteName: "FastForward",
    serverName: "prod",
    status: "Completed",
  },
  {
    jobId: 11,
    siteName: 'Market, "East"',
    pubServerName: "stage",
    status: "Failed",
  },
];

describe("buildPublishingLogsCsv", () => {
  it("writes visible columns for the filtered rows only", () => {
    const visible = filterLogEntries(LOGS, { query: "market", status: "failed" });
    const csv = buildPublishingLogsCsv(visible).replace(/\r\n/g, "\n");
    expect(csv).toBe(
      `${PUBLISHING_LOG_CSV_HEADERS.join(",")}\n11,"Market, ""East""",stage,Failed\n`,
    );
  });

  it("downloads a header-only file when the filter matches nothing", () => {
    const visible = filterLogEntries(LOGS, { query: "no-such-site" });
    expect(visible).toEqual([]);
    const csv = buildPublishingLogsCsv(visible).replace(/\r\n/g, "\n");
    expect(csv).toBe(`${PUBLISHING_LOG_CSV_HEADERS.join(",")}\n`);
  });

  it("throws when the row list cannot be built", () => {
    expect(() =>
      buildPublishingLogsCsv(undefined as unknown as PublishingLogEntry[]),
    ).toThrow(/requires a list/);
  });
});
