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

import { beforeEach, describe, expect, it, vi } from "vitest";
import { stopRuntimeJob } from "@/api/publishing/runtimeApi";
import * as client from "@/api/client";
import { SERVICES_ROOT } from "@/api/paths";
import { stopPublishing } from "@/api/publishing/serversApi";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    post: vi.fn(),
  };
});

vi.mock("@/api/publishing/serversApi", () => ({
  stopPublishing: vi.fn(),
}));

const designStop = `${SERVICES_ROOT}/sitemanage/publishingdesign/runtime/jobs/99/stop`;

describe("stopRuntimeJob", () => {
  beforeEach(() => {
    vi.mocked(client.post).mockReset();
    vi.mocked(stopPublishing).mockReset();
  });

  it("returns the design stop response without calling stopPublishing", async () => {
    vi.mocked(client.post).mockResolvedValue({ jobId: 99, status: "cancelled" });
    await expect(stopRuntimeJob(99)).resolves.toEqual({
      jobId: 99,
      status: "cancelled",
    });
    expect(client.post).toHaveBeenCalledWith(designStop);
    expect(stopPublishing).not.toHaveBeenCalled();
  });

  it("falls back to stopPublishing when the design stop fails", async () => {
    vi.mocked(client.post).mockRejectedValue({
      status: 404,
      body: { message: "missing" },
    });
    vi.mocked(stopPublishing).mockResolvedValue({});
    await expect(stopRuntimeJob(99)).resolves.toEqual({
      jobId: 99,
      status: "cancelled",
    });
    expect(stopPublishing).toHaveBeenCalledWith(99);
  });

  it("rethrows the design stop error when the ops fallback also fails", async () => {
    const primary = {
      status: 409,
      statusText: "Conflict",
      body: { message: "edition job 99 is not running" },
    };
    vi.mocked(client.post).mockRejectedValue(primary);
    vi.mocked(stopPublishing).mockRejectedValue({
      status: 500,
      body: { message: "ops stop failed" },
    });
    await expect(stopRuntimeJob(99)).rejects.toBe(primary);
  });
});
