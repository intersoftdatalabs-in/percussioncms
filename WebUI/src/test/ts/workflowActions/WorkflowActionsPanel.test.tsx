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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import React from "react";
import { render, screen, waitFor, fireEvent } from "@testing-library/react";
import { describe, expect, it, vi, beforeEach } from "vitest";
import { WorkflowActionsPanel } from "../../../main/ts/workflowActions/WorkflowActionsPanel";
import * as client from "../../../main/ts/api/client";

vi.mock("../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

describe("WorkflowActionsPanel", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("renders state and actions for checked out item", async () => {
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (url.includes("checkOut")) {
        return {
          itemName: "test-page",
          checkOutUser: "admin",
          currentUser: "admin",
          assignmentType: "ADMIN",
        };
      }
      if (url.includes("getTransitions")) {
        return {
          itemId: "123",
          stateName: "Review",
          transitionTriggers: ["Approve", "Reject"],
        };
      }
      return {};
    });

    render(<WorkflowActionsPanel itemId="123" />);

    await waitFor(() => {
      expect(screen.getByTestId("workflow-state-name").textContent).toBe("Review");
    });
    expect(screen.getByTestId("checkin-button")).toBeTruthy();
    expect(screen.getByTestId("transition-button-Approve")).toBeTruthy();
  });

  it("renders force check out for item checked out by another user", async () => {
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (url.includes("checkOut")) {
        return {
          itemName: "test-page",
          checkOutUser: "otheruser",
          currentUser: "admin",
          assignmentType: "ADMIN",
        };
      }
      if (url.includes("getTransitions")) {
        return {
          itemId: "123",
          stateName: "Draft",
          transitionTriggers: ["Submit"],
        };
      }
      return {};
    });

    render(<WorkflowActionsPanel itemId="123" />);

    await waitFor(() => {
      expect(screen.getByTestId("force-checkout-button")).toBeTruthy();
    });
  });
});
