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

import { render, screen } from "@testing-library/react";
import { afterEach, describe, expect, it, vi } from "vitest";
import type {
  MenuAction,
  PSPathItem,
} from "../../../main/ts/api/contentExplorer/types";

const findChildren = vi.hoisted(() => vi.fn());

vi.mock("../../../main/ts/api/contentExplorer/pathApi", async (importOriginal) => {
  const actual =
    await importOriginal<typeof import("../../../main/ts/api/contentExplorer/pathApi")>();
  return { ...actual, findChildren };
});

import {
  filterToolbarActions,
  isToolbarCheckoutHidden,
  isToolbarClearScheduleHidden,
  isToolbarForceCheckinHidden,
  isToolbarPublishingHistoryHidden,
  isToolbarPublishNowHidden,
  isToolbarScheduleHidden,
  isToolbarStageHidden,
  isToolbarTakedownHidden,
} from "../../../main/ts/contentExplorer/actionEnablement";
import { dispatchAction } from "../../../main/ts/contentExplorer/actionDispatch";
import {
  buildBulkStageRows,
  partitionStageSelection,
  publishSelectedFolder,
  publishSelectedItem,
  publishSelectedItems,
  removeFromStagingSelectedItem,
  resolvePublishKind,
  stageSelectedItem,
  takedownSelectedItem,
} from "../../../main/ts/contentExplorer/itemPublish";
import {
  publishableScheduleTargets,
} from "../../../main/ts/contentExplorer/itemScheduleDates";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";
import { message } from "../../../main/ts/i18n/message";
import { BulkStageDialog } from "../../../main/ts/contentExplorer/BulkStageDialog";
import type { BulkStageReportInput } from "../../../main/ts/contentExplorer/itemPublish";
import { renderA11yGate } from "./a11y";

/**
 * All-items submit for generic custom-type Items (#6580 reversal, parent
 * #4530).
 *
 * Owner direction: Folder Publish Now submits EVERY child with an id (like
 * the ondemand/publish-now generator, which takes any numeric content
 * ids); a single custom-type row is never hidden — Pages, Assets, and
 * Items are all 1st-class entities, so the client submits and the engine
 * accepts or rejects with a named error. {@link resolvePublishKind} stays
 * an endpoint-affinity classifier (generic rows are still {@code "none"})
 * but nothing in the publish family gates availability on it.
 */

function generic(overrides: Partial<PSPathItem> = {}): PSPathItem {
  return {
    id: "g-9",
    path: "/ContentLib/PromoSnippet",
    name: "PromoSnippet",
    title: "Promo snippet",
    type: "MyCustomArticle",
    leaf: true,
    accessLevel: "WRITE",
    ...overrides,
  } as PSPathItem;
}

function page(overrides: Partial<PSPathItem> = {}): PSPathItem {
  return {
    id: "42",
    path: "/Sites/Demo/Home",
    name: "Home",
    type: "percPage",
    leaf: true,
    ...overrides,
  };
}

function toolbarAction(name: string): MenuAction {
  return { name, label: name, sortRank: 1, menuType: "MENUITEM" };
}

const TOOLBAR_FAMILY = [
  "publish_now",
  "Take_Down",
  "Stage",
  "Remove_from_Staging",
  "Schedule",
  "Clear_Scheduled_Dates",
  "Publishing_History",
  "Check_Out",
  "Check_In",
  "Force_Checkin",
];

/** Fresh JSON `{}` response per fetch — Response bodies are one-shot. */
function mockOkJson(): void {
  vi.spyOn(global, "fetch").mockImplementation(
    async () =>
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
  );
}

afterEach(() => {
  findChildren.mockReset();
  vi.restoreAllMocks();
});

describe("publish all-items submit (#6580 reversal)", () => {
  it("resolvePublishKind stays an endpoint classifier; none is never an availability gate", () => {
    // Generic custom-type rows keep no endpoint affinity…
    expect(resolvePublishKind(generic())).toBe("none");
    // …while site rows (even custom types under /Sites) keep the page
    // heuristic and asset rows keep the asset kind.
    expect(
      resolvePublishKind(generic({ path: "/Sites/Demo/PromoSnippet" })),
    ).toBe("page");
    expect(resolvePublishKind(page())).toBe("page");
    expect(
      resolvePublishKind(
        generic({
          id: "99",
          path: "/Assets/logo.png",
          name: "logo",
          type: "percImageAsset",
        }),
      ),
    ).toBe("asset");
  });

  it("single-item publish/stage/takedown/unstage submit generics via the page endpoint", async () => {
    mockOkJson();
    expect(await publishSelectedItem(generic())).toBe(true);
    expect(await stageSelectedItem(generic())).toBe(true);
    expect(await takedownSelectedItem(generic())).toBe(true);
    expect(await removeFromStagingSelectedItem(generic())).toBe(true);
    const urls = vi
      .mocked(global.fetch)
      .mock.calls.map((call) => String(call[0]));
    expect(urls.some((url) => url.includes("/publish/page/g-9"))).toBe(true);
    expect(urls.some((url) => url.includes("/page/staging/g-9"))).toBe(true);
    expect(urls.some((url) => url.includes("/takedown/page/g-9"))).toBe(true);
  });

  it("folder publish submits every child with an id; only folders and id-less rows skip", async () => {
    findChildren.mockResolvedValue([
      page({ id: "42", name: "Home" }),
      generic({ id: "g-9", name: "PromoSnippet" }),
      {
        id: "8",
        name: "Nested",
        path: "/Sites/Demo/News/Nested",
        type: "folder",
        leaf: false,
      } as PSPathItem,
      generic({ id: "", name: "Orphan" }),
    ]);
    mockOkJson();
    const folder = {
      id: "7",
      name: "News",
      path: "/Sites/Demo/News",
      type: "folder",
      leaf: false,
    } as PSPathItem;
    const result = await publishSelectedFolder(folder);
    expect(result.publishedIds).toEqual(["42", "g-9"]);
    expect(result.skippedFolders).toEqual(["Nested"]);
    expect(result.failures).toEqual([
      expect.objectContaining({ name: "Orphan", message: "missing content id" }),
    ]);
    const urls = String(
      (global.fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls.map(
        (call) => String(call[0]),
      ),
    );
    expect(urls).toContain("g-9");
  });

  it("engine rejections become named failures, not pre-skips", async () => {
    findChildren.mockResolvedValue([generic()]);
    vi.spyOn(global, "fetch").mockResolvedValue(
      new Response("denied", { status: 403, statusText: "Forbidden" }),
    );
    const folder = {
      id: "7",
      name: "News",
      path: "/Sites/Demo/News",
      type: "folder",
      leaf: false,
    } as PSPathItem;
    const result = await publishSelectedFolder(folder);
    expect(result.publishedIds).toEqual([]);
    expect(result.failures).toEqual([
      expect.objectContaining({
        id: "g-9",
        name: "PromoSnippet",
        status: 403,
      }),
    ]);
  });

  it("multi-select and schedule targets include generic rows", async () => {
    mockOkJson();
    const multi = await publishSelectedItems([page(), generic()]);
    expect(multi.publishedIds).toEqual(["42", "g-9"]);
    expect(multi.failures).toEqual([]);

    const plan = partitionStageSelection([page(), generic()]);
    expect(plan.eligible.map((row) => row.id)).toEqual(["42", "g-9"]);
    expect(plan.skippedFolders).toEqual([]);
    expect("skippedOther" in plan).toBe(false);

    expect(
      publishableScheduleTargets([page(), generic()]).map((row) => row.id),
    ).toEqual(["42", "g-9"]);
  });

  it("toolbar shows the publish family for a generic selection", () => {
    const actions = TOOLBAR_FAMILY.map(toolbarAction);
    const visible = filterToolbarActions(actions, undefined, generic(), true);
    expect(visible.map((row) => row.name).sort()).toEqual(
      [...TOOLBAR_FAMILY].sort(),
    );
    const helpers = [
      isToolbarPublishNowHidden,
      isToolbarTakedownHidden,
      isToolbarStageHidden,
      isToolbarScheduleHidden,
      isToolbarClearScheduleHidden,
      isToolbarPublishingHistoryHidden,
      isToolbarCheckoutHidden,
      isToolbarForceCheckinHidden,
    ];
    const names = [
      "publish_now",
      "Take_Down",
      "Stage",
      "Schedule",
      "Clear_Scheduled_Dates",
      "Publishing_History",
      "Check_Out",
      "Force_Checkin",
    ];
    helpers.forEach((helper, index) => {
      expect(helper(toolbarAction(names[index]), generic())).toBe(false);
      expect(helper(toolbarAction(names[index]), undefined)).toBe(true);
    });
  });

  it("dispatch submits history and checkout for generics instead of unavailable", async () => {
    mockOkJson();
    const onShowPublishingHistory = vi.fn();
    const history = await dispatchAction(
      toolbarAction("Publishing_History"),
      { item: generic(), onShowPublishingHistory },
    );
    expect(history.kind).not.toBe("unavailable");
    expect(onShowPublishingHistory).toHaveBeenCalledWith(generic());

    const checkout = await dispatchAction(toolbarAction("Check_Out"), {
      item: generic(),
    });
    expect(checkout.kind).not.toBe("unavailable");

    const scheduled = await dispatchAction(toolbarAction("Schedule"), {
      item: generic(),
      confirm: () => true,
      pickScheduleDates: async () => ({
        itemId: "g-9",
        startDate: "09/18/2026 09:00 am",
        endDate: "",
        comments: "",
      }),
    });
    expect(scheduled.kind).not.toBe("unavailable");
  });

  it("folder confirm copy is type-neutral and names no second-class rows", () => {
    expect(message(EXPLORER_MSG.CONFIRM_PUBLISH_FOLDER)).toMatch(
      /Publish the items in this folder now/i,
    );
    expect(message(EXPLORER_MSG.CONFIRM_PUBLISH_FOLDER)).toMatch(
      /Nested folders are not included/i,
    );
    expect(message(EXPLORER_MSG.CONFIRM_PUBLISH_FOLDER)).not.toMatch(
      /Other item types/i,
    );
    expect(message(EXPLORER_MSG.CONFIRM_STAGE_FOLDER)).not.toMatch(
      /Other item types/i,
    );
    expect(message(EXPLORER_MSG.CONFIRM_UNSTAGE_FOLDER)).not.toMatch(
      /Other item types/i,
    );
    expect(message(EXPLORER_MSG.PUBLISH_FOLDER_EMPTY)).toMatch(
      /no items to publish/i,
    );
    expect(
      (EXPLORER_MSG as Record<string, string>).PUBLISH_SKIPPED_OTHER,
    ).toBeUndefined();
    expect(
      (EXPLORER_MSG as Record<string, string>).STAGE_SKIPPED_OTHER,
    ).toBeUndefined();
    expect(
      (EXPLORER_MSG as Record<string, string>).UNSTAGE_SKIPPED_OTHER,
    ).toBeUndefined();
  });

  it("bulk stage report treats a generic row as first-class (a11y gated)", async () => {
    const rows = buildBulkStageRows([page(), generic()], {
      okIds: ["42", "g-9"],
      skippedFolders: [],
      failures: [],
    } satisfies BulkStageReportInput);
    expect(rows).toHaveLength(2);
    expect(rows[1]).toMatchObject({ name: "PromoSnippet", outcome: "ok" });
    const runBatch = vi.fn();
    const onDone = vi.fn();
    const { container } = render(
      <BulkStageDialog
        mode="stage"
        items={[page(), generic()]}
        runBatch={runBatch}
        onDone={onDone}
      />,
    );
    expect(
      screen.queryByTestId("explorer-bulk-stage-skipped-other"),
    ).toBeNull();
    expect(screen.getByTestId("explorer-bulk-stage-items")).toHaveTextContent(
      /PromoSnippet/,
    );
    await renderA11yGate(container);
  });
});
