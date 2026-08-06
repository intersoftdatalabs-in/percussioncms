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
import { buildPurgeRequestBody } from "@/publishing/logRequestBodies";
import { canPurge } from "@/publishing/sections/LogsSection";

describe("purge confirmation gate", () => {
  it("blocks purge with no selection", () => {
    expect(canPurge([])).toBe(false);
  });

  it("allows purge when ids selected", () => {
    expect(canPurge(["1", "2"])).toBe(true);
  });

  it("pairs with Minuet-shaped purge payload (jobids)", () => {
    const body = buildPurgeRequestBody(["1", "2"]);
    const root = body.SitePublishPurgeRequest as { jobids: number[] };
    expect(root.jobids).toEqual([1, 2]);
  });
});
