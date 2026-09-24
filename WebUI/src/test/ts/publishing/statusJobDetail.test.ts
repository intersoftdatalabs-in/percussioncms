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
import { statusJobDetail } from "@/publishing/statusJobDetail";

describe("statusJobDetail (#4789)", () => {
  it("copies job id, site, edition, and status", () => {
    const detail = statusJobDetail({
      jobId: 27,
      siteName: "FastForward",
      editionName: "Nightly Full",
      status: "Running",
    });
    expect(detail).toMatchObject({
      jobId: "27",
      site: "FastForward",
      editionName: "Nightly Full",
      status: "Running",
      errorText: "",
      showError: false,
    });
  });

  it("shows error text only when the job failed and the API sent it", () => {
    expect(
      statusJobDetail({
        jobId: 1,
        status: "Failed",
        errorMessage: "disk full",
      }).errorText,
    ).toBe("disk full");
    expect(
      statusJobDetail({
        jobId: 2,
        status: "Completed with failures",
        error: "partial",
      }).errorText,
    ).toBe("partial");
    expect(
      statusJobDetail({
        jobId: 3,
        status: "Running",
        errorMessage: "ignored",
      }).errorText,
    ).toBe("");
  });

  it("renders missing optional fields as empty without throwing", () => {
    const detail = statusJobDetail({
      jobId: 9,
      editionName: { unexpected: true } as unknown as string,
      errorMessage: null as unknown as string,
    });
    expect(detail.site).toBe("");
    expect(detail.editionName).toBe("");
    expect(detail.status).toBe("");
    expect(detail.errorText).toBe("");
    expect(detail.showError).toBe(false);
    expect(statusJobDetail(undefined).jobId).toBe("");
  });
});
