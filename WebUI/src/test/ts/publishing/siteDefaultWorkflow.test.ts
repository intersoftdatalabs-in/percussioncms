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
  resolveCatalogWorkflow,
  siteDefaultWorkflowHttpFailure,
  workflowNamesMatch,
} from "@/publishing/siteDefaultWorkflow";

const catalog = ["Standard", "Local Content"];

describe("siteDefaultWorkflow", () => {
  it("accepts a catalog name and rejects an empty or unknown choice", () => {
    expect(resolveCatalogWorkflow("  standard ", catalog)).toBe("Standard");
    expect(resolveCatalogWorkflow("   ", catalog)).toBe("empty");
    expect(resolveCatalogWorkflow("Missing", catalog)).toBe("unknown");
  });

  it("treats the same catalog name as unchanged", () => {
    expect(workflowNamesMatch("Standard", " standard ")).toBe(true);
    expect(workflowNamesMatch("Standard", "Local Content")).toBe(false);
  });

  it("maps only 400, 403, and 409 to a failed save", () => {
    expect(siteDefaultWorkflowHttpFailure(400)).toBe("bad_request");
    expect(siteDefaultWorkflowHttpFailure(403)).toBe("forbidden");
    expect(siteDefaultWorkflowHttpFailure(409)).toBe("conflict");
    expect(siteDefaultWorkflowHttpFailure(404)).toBeNull();
    expect(siteDefaultWorkflowHttpFailure(500)).toBeNull();
  });
});
