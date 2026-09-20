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
import { stopPublishing } from "@/api/publishing/serversApi";
import * as client from "@/api/client";
import { PATHS } from "@/api/paths";

vi.mock("@/api/client", async (importOriginal) => {
  const actual = await importOriginal<typeof import("@/api/client")>();
  return {
    ...actual,
    post: vi.fn().mockResolvedValue({}),
  };
});

describe("stopPublishing", () => {
  beforeEach(() => {
    vi.mocked(client.post).mockClear();
  });

  it("POSTs stopPublishing/{jobId}", async () => {
    await stopPublishing(4615);
    expect(client.post).toHaveBeenCalledWith(
      `${PATHS.PUB_SERVERS}stopPublishing/4615`,
      {},
    );
  });
});
