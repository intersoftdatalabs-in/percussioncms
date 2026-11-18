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
  stageSelectedFolder,
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
 * Publish-family gating for generic custom-type items (#6580, parent #4530).
 *
 * Rhythmyx upgrade rows that are neither pages nor assets (custom content
 * types, snippets, legacy types) deliberately stay {@code "none"} from
 * {@link resolvePublishKind}: the demand endpoints are page|resource
 * addressed, so Publish Now, stage / remove-from-staging, Take Down,
 * schedule / clear-schedule, Publishing History, and checkout / check-in /
 * force check-in stay hidden, single-item calls return false without fetch,
 * and folder / multi-select paths report those rows as skipped-other with
 * honest copy instead of failing silently.
 */

function generic(overrides: Partial<PSPathItem> = {}): PSPathItem {
  // Generics in the #6580 sense live outside /Sites and /Assets with no
  // page/asset token. Custom-type rows under /Sites keep the page heuristic
  // (asserted below) — they are site content the page endpoint can serve.
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

afterEach(() => {
  findChildren.mockReset();
  vi.restoreAllMocks();
});

describe("generic publish gating (#6580)", () => {
  it("resolvePublishKind stays none for generic custom-type rows", () => {
    expect(resolvePublishKind(generic())).toBe("none");
    expect(
      resolvePublishKind(
        generic({ type: "rffSnippet", path: "/Snippets/promo" }),
      ),
    ).toBe("none");
    expect(
      resolvePublishKind(
        generic({ type: "UnknownThing", path: "/Misc/thing" }),
      ),
    ).toBe("none");
    expect(resolvePublishKind(generic({ id: "" }))).toBe("none");
    // Boundary: a custom-type row filed under /Sites keeps the page
    // heuristic — site content the page endpoint can serve.
    expect(
      resolvePublishKind(
        generic({ path: "/Sites/Demo/PromoSnippet" }),
      ),
    ).toBe("page");
    // Pages and assets keep their kinds.
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

  it("single-item publish/stage/takedown/unstage return false without fetch", async () => {
    const fetchSpy = vi.spyOn(global, "fetch");
    expect(await publishSelectedItem(generic())).toBe(false);
    expect(await stageSelectedItem(generic())).toBe(false);
    expect(await takedownSelectedItem(generic())).toBe(false);
    expect(await removeFromStagingSelectedItem(generic())).toBe(false);
    expect(fetchSpy).not.toHaveBeenCalled();
  });

  it("folder publish routes generic children to skippedOther, not failures", async () => {
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
    ]);
    vi.spyOn(global, "fetch").mockResolvedValue(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const folder = {
      id: "7",
      name: "News",
      path: "/Sites/Demo/News",
      type: "folder",
      leaf: false,
    } as PSPathItem;
    const result = await publishSelectedFolder(folder);
    expect(result.publishedIds).toEqual(["42"]);
    expect(result.skippedOther).toEqual(["PromoSnippet"]);
    expect(result.skippedFolders).toEqual(["Nested"]);
    expect(result.failures).toEqual([]);
    const urls = String(
      (global.fetch as unknown as { mock: { calls: unknown[][] } }).mock.calls.map(
        (call) => String(call[0]),
      ),
    );
    expect(urls).not.toContain("g-9");
  });

  it("stage folder and multi-select keep generics out of the eligible set", async () => {
    findChildren.mockResolvedValue([generic()]);
    const folder = {
      id: "7",
      name: "News",
      path: "/Sites/Demo/News",
      type: "folder",
      leaf: false,
    } as PSPathItem;
    const staged = await stageSelectedFolder(folder);
    expect(staged.stagedIds).toEqual([]);
    expect(staged.skippedOther).toEqual(["PromoSnippet"]);

    const plan = partitionStageSelection([page(), generic()]);
    expect(plan.eligible.map((row) => row.id)).toEqual(["42"]);
    expect(plan.skippedOther).toEqual(["PromoSnippet"]);

    const multi = await publishSelectedItems([generic()]);
    expect(multi.publishedIds).toEqual([]);
    expect(multi.skippedOther).toEqual(["PromoSnippet"]);
  });

  it("schedule targets exclude generic rows", () => {
    expect(publishableScheduleTargets([page(), generic()]).map((row) => row.id)).toEqual([
      "42",
    ]);
    expect(publishableScheduleTargets([generic()])).toEqual([]);
  });

  it("toolbar hides the publish family for a generic selection", () => {
    const actions = TOOLBAR_FAMILY.map(toolbarAction);
    const hidden = filterToolbarActions(actions, undefined, generic(), true);
    expect(hidden).toEqual([]);
    const visible = filterToolbarActions(actions, undefined, page(), true);
    expect(visible.length).toBeGreaterThan(0);
  });

  it("toolbar hidden helpers agree per action for generic vs page", () => {
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
      expect(helper(toolbarAction(names[index]), generic())).toBe(true);
      expect(helper(toolbarAction(names[index]), page())).toBe(false);
    });
  });

  it("dispatch keeps history/schedule/checkout unavailable for generics", async () => {
    const onShowPublishingHistory = vi.fn();
    const history = await dispatchAction(
      toolbarAction("Publishing_History"),
      { item: generic(), onShowPublishingHistory },
    );
    expect(history.kind).toBe("unavailable");
    expect(history.messageKey).toBe(EXPLORER_MSG.ACTION_UNAVAILABLE);
    expect(onShowPublishingHistory).not.toHaveBeenCalled();

    const scheduled = await dispatchAction(toolbarAction("Schedule"), {
      item: generic(),
      selectedItems: [generic()],
    });
    expect(scheduled.kind).toBe("unavailable");

    const checkout = await dispatchAction(toolbarAction("Check_Out"), {
      item: generic(),
    });
    expect(checkout.kind).toBe("unavailable");
    expect(checkout.messageKey).toBe(EXPLORER_MSG.ACTION_UNAVAILABLE);
  });

  it("folder confirm copy explicitly scopes other item types", () => {
    expect(message(EXPLORER_MSG.CONFIRM_PUBLISH_FOLDER)).toMatch(
      /Other item types and nested folders are not included/i,
    );
    expect(message(EXPLORER_MSG.CONFIRM_STAGE_FOLDER)).toMatch(
      /Other item types and nested folders are not included/i,
    );
    expect(message(EXPLORER_MSG.CONFIRM_UNSTAGE_FOLDER)).toMatch(
      /Other item types and nested folders are not included/i,
    );
    expect(message(EXPLORER_MSG.PUBLISH_FOLDER_EMPTY)).toMatch(
      /no pages or assets to publish/i,
    );
    expect(message(EXPLORER_MSG.PUBLISH_SKIPPED_OTHER)).toMatch(
      /not pages or assets/i,
    );
  });

  it("bulk stage report names a skipped generic row honestly (a11y gated)", async () => {
    const rows = buildBulkStageRows([page(), generic()], {
      okIds: ["42"],
      skippedFolders: [],
      skippedOther: ["PromoSnippet"],
      failures: [],
    } satisfies BulkStageReportInput);
    expect(rows).toHaveLength(2);
    expect(rows[1]).toMatchObject({
      name: "PromoSnippet",
      outcome: "skipped-other",
    });
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
      screen.getByTestId("explorer-bulk-stage-skipped-other"),
    ).toHaveTextContent(/PromoSnippet/);
    expect(
      screen.getByTestId("explorer-bulk-stage-skipped-other"),
    ).toHaveTextContent(/not pages or assets/i);
    await renderA11yGate(container);
  });
});
