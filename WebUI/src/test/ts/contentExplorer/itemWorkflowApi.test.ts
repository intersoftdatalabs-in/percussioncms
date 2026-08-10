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

import { beforeEach, describe, expect, it, vi } from "vitest";
import {
  getItemWorkflowTransitions,
  transitionItem,
} from "../../../main/ts/api/contentExplorer/itemWorkflowApi";
import * as client from "../../../main/ts/api/client";
import { PATHS } from "../../../main/ts/api/paths";

vi.mock("../../../main/ts/api/client", () => ({
  get: vi.fn(),
  post: vi.fn(),
  put: vi.fn(),
  del: vi.fn(),
}));

describe("itemWorkflowApi (#2732)", () => {
  beforeEach(() => {
    vi.mocked(client.get).mockReset();
  });

  it("getItemWorkflowTransitions calls getTransitions path with encoded id", async () => {
    vi.mocked(client.get).mockResolvedValue({
      itemId: "101-1",
      stateName: "Draft",
      transitionTriggers: ["Submit"],
    });
    const result = await getItemWorkflowTransitions("101-1");
    expect(client.get).toHaveBeenCalledWith(
      `${PATHS.ITEM_WORKFLOW_TRANSITIONS}${encodeURIComponent("101-1")}`,
    );
    expect(result.transitionTriggers).toEqual(["Submit"]);
  });

  it("getItemWorkflowTransitions returns empty triggers for blank id without fetch", async () => {
    const result = await getItemWorkflowTransitions("  ");
    expect(client.get).not.toHaveBeenCalled();
    expect(result.transitionTriggers).toEqual([]);
  });

  it("transitionItem fetches trigger via transitionWithComments path", async () => {
    vi.mocked(client.get).mockResolvedValue({ itemId: "55" });
    await transitionItem("55", "Submit");
    expect(client.get).toHaveBeenCalledWith(
      `${PATHS.ITEM_WORKFLOW_TRANSITION_WITH_COMMENTS}${encodeURIComponent("55")}/${encodeURIComponent("Submit")}`,
    );
  });

  it("transitionItem appends comment query when provided", async () => {
    vi.mocked(client.get).mockResolvedValue({ itemId: "55" });
    await transitionItem("55", "Approve", "looks good");
    const url = vi.mocked(client.get).mock.calls[0]?.[0] as string;
    expect(url).toContain("comment=");
    expect(url).toContain(encodeURIComponent("looks good"));
  });

  it("transitionItem rejects missing id or trigger", async () => {
    await expect(transitionItem("", "Submit")).rejects.toThrow(/requires/);
    await expect(transitionItem("1", "")).rejects.toThrow(/requires/);
    expect(client.get).not.toHaveBeenCalled();
  });
});
