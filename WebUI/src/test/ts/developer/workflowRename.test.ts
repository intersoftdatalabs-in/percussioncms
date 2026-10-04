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
  canOfferWorkflowRename,
  isPackagedWorkflowName,
  isWorkflowRenameReady,
} from "../../../main/ts/developer/workflowRename";

describe("workflow rename policy", () => {
  it("treats stock names as packaged", () => {
    expect(isPackagedWorkflowName("Default Workflow")).toBe(true);
    expect(isPackagedWorkflowName("simple workflow")).toBe(true);
    expect(isPackagedWorkflowName("Local Content")).toBe(true);
    expect(isPackagedWorkflowName("LocalContent")).toBe(true);
    expect(isPackagedWorkflowName("Nightly QA")).toBe(false);
  });

  it("does not offer rename for packaged or system-default workflows", () => {
    expect(canOfferWorkflowRename({ name: "Simple Workflow", defaultWorkflow: false })).toBe(
      false,
    );
    expect(canOfferWorkflowRename({ name: "Nightly QA", defaultWorkflow: true })).toBe(false);
    expect(canOfferWorkflowRename({ name: "Nightly QA", defaultWorkflow: false })).toBe(true);
  });

  it("requires a different legal name before confirm", () => {
    expect(isWorkflowRenameReady("Nightly QA", "Nightly QA")).toBe(false);
    expect(isWorkflowRenameReady("Nightly QA", "  Nightly QA  ")).toBe(false);
    expect(isWorkflowRenameReady("Nightly QA", "nightly qa")).toBe(true);
    expect(isWorkflowRenameReady("Nightly QA", "Nightly QA 2")).toBe(true);
    expect(isWorkflowRenameReady("Nightly QA", "Bad!")).toBe(false);
    expect(isWorkflowRenameReady("Nightly QA", "Bad*Name")).toBe(false);
    expect(isWorkflowRenameReady("Nightly QA", "N".repeat(51))).toBe(false);
    expect(isWorkflowRenameReady("Nightly QA", "  ")).toBe(false);
  });
});
