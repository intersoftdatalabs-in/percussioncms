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
import { CategoriesSection } from "../../../main/ts/workflowAdmin/category/CategoriesSection";
import * as client from "../../../main/ts/api/client";

vi.mock("../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

describe("CategoriesSection", () => {
  beforeEach(() => {
    vi.resetAllMocks();
  });

  it("renders category tree after fetching successfully", async () => {
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (url.includes("category/all")) {
        return {
          title: "Root Tree",
          topLevelNodes: [
            {
              id: "1",
              title: "Default Category",
              selectable: true,
              childNodes: [],
            },
            {
              id: "2",
              title: "System Category",
              selectable: false,
              childNodes: [],
            },
          ],
        };
      }
      if (url.includes("lockinfo")) {
        return {
          userName: "",
          sessionId: "",
          sitename: "",
        };
      }
      return {};
    });

    render(<CategoriesSection />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-categories-section")).toBeTruthy();
    });
    expect(screen.getByText("Default Category")).toBeTruthy();
    expect(screen.getByText("System Category")).toBeTruthy();
    // System node has locked by default if no createdBy
    expect(screen.getByTestId("lock-indicator-2")).toBeTruthy();
  });

  it("renders empty tree when topLevelNodes is missing (#3202)", async () => {
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (url.includes("category/all")) {
        return { title: "Categories" };
      }
      return { userName: "", sessionId: "", sitename: "" };
    });

    render(<CategoriesSection />);

    await waitFor(() => {
      expect(screen.getByTestId("perc-categories-section")).toBeTruthy();
    });
    expect(screen.queryByTestId("route-error")).toBeNull();
  });

  it("acquires lock when lock tab button clicked", async () => {
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (url.includes("category/all")) {
        return {
          title: "Root Tree",
          topLevelNodes: [],
        };
      }
      if (url.includes("lockinfo")) {
        return {
          userName: "",
          sessionId: "",
          sitename: "",
        };
      }
      return {};
    });

    render(<CategoriesSection />);

    await waitFor(() => {
      expect(screen.getByTestId("acquire-lock-btn")).toBeTruthy();
    });

    vi.mocked(client.post).mockResolvedValue({});
    vi.mocked(client.get).mockImplementation(async (url: string) => {
      if (url.includes("category/all")) {
        return { title: "Root Tree", topLevelNodes: [] };
      }
      if (url.includes("lockinfo")) {
        return {
          userName: "admin",
          sessionId: "session-123",
          sitename: "",
        };
      }
      return {};
    });

    const lockBtn = screen.getByTestId("acquire-lock-btn");
    fireEvent.click(lockBtn);

    await waitFor(() => {
      expect(screen.getByTestId("lock-banner")).toBeTruthy();
    });
  });
});
