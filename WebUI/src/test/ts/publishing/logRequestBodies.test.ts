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
  buildLogDetailsRequestBody,
  buildLogListRequestBody,
  buildPurgeRequestBody,
} from "@/publishing/logRequestBodies";

describe("logRequestBodies (Minuet / DTO parity — B1/B2)", () => {
  it("builds purge body with jobids and SitePublishPurgeRequest root", () => {
    const body = buildPurgeRequestBody(["101", 202]);
    expect(body).toEqual({
      SitePublishPurgeRequest: {
        jobids: [101, 202],
      },
    });
  });

  it("builds log details body with jobid and SitePublishLogDetailsRequest root", () => {
    const body = buildLogDetailsRequestBody("42");
    expect(body).toEqual({
      SitePublishLogDetailsRequest: {
        jobid: 42,
      },
    });
  });

  it("builds log list body under SitePublishLogRequest root", () => {
    const body = buildLogListRequestBody({
      siteId: "7",
      days: 5,
      maxcount: 20,
    });
    expect(body).toEqual({
      SitePublishLogRequest: {
        siteId: "7",
        days: 5,
        maxcount: 20,
      },
    });
  });

  it("rejects non-numeric job ids", () => {
    expect(() => buildPurgeRequestBody(["abc"])).toThrow(/Invalid publish job id/);
    expect(() => buildLogDetailsRequestBody("x")).toThrow(/Invalid publish job id/);
  });
});
