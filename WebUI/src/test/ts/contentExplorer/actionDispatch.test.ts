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

import { describe, expect, it, vi } from "vitest";
import type { MenuAction, PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import {
  classifyAction,
  dispatchAction,
  isContentEditorActionUrl,
  isDataFlowActionUrl,
  parseTemplateIdFromAction,
} from "../../../main/ts/contentExplorer/actionDispatch";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";

function item(overrides: Partial<PSPathItem> = {}): PSPathItem {
  return {
    name: "page",
    path: "/Sites/Demo/page",
    type: "percPage",
    id: "42",
    ...overrides,
  };
}

function action(overrides: Partial<MenuAction> = {}): MenuAction {
  return {
    name: "Edit",
    label: "Edit",
    sortRank: 0,
    menuType: "MENUITEM",
    ...overrides,
  };
}

describe("actionDispatch", () => {
  it("classifies Data Flow URLs as non-navigable", () => {
    expect(isDataFlowActionUrl("../sys_cxSupport/previewslotvariant.html")).toBe(
      true,
    );
    expect(isDataFlowActionUrl("../sys_action/checkoutedit.xml")).toBe(true);
    expect(isDataFlowActionUrl("/assembler/render?sys_template=1")).toBe(false);
  });

  it("classifies Edit as editor (not CM1 editor navigation)", () => {
    expect(classifyAction(action({ name: "Edit" }))).toBe("editor");
    expect(classifyAction(action({ name: "Quick_Edit" }))).toBe("editor");
  });

  it("classifies Item_Preview and assembler URLs as rest", () => {
    expect(classifyAction(action({ name: "Item_Preview" }))).toBe("rest");
    expect(
      classifyAction(
        action({
          name: "rffSnTitle",
          url: "../assembler/render?sys_contentid=42&sys_template=7",
        }),
      ),
    ).toBe("rest");
  });

  it("parses template id from query and parameters", () => {
    expect(
      parseTemplateIdFromAction(
        action({
          url: "../assembler/render?sys_template=9&sys_contentid=1",
        }),
      ),
    ).toBe(9);
    expect(
      parseTemplateIdFromAction(
        action({
          parameters: [{ name: "sys_variantid", value: "11" }],
        }),
      ),
    ).toBe(11);
  });

  it("dispatch Edit returns editor unavailable key", async () => {
    const result = await dispatchAction(action({ name: "Edit" }), {
      item: item(),
    });
    expect(result.kind).toBe("editor");
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_EDITOR_UNAVAILABLE);
  });

  it("dispatch Data Flow HTML does not navigate", async () => {
    const openWindow = vi.fn();
    const result = await dispatchAction(
      action({
        name: "Item_Assembly",
        url: "../sys_cxSupport/aadocactions.html",
      }),
      { item: item(), openWindow },
    );
    expect(result.kind).toBe("unavailable");
    expect(openWindow).not.toHaveBeenCalled();
  });

  it("dispatch template leaf fetches preview location and opens it", async () => {
    const openWindow = vi.fn();
    const fetchPreview = vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_template=7",
      contentId: 42,
      templateId: 7,
      revision: 1,
    });
    const result = await dispatchAction(
      action({
        name: "rffSnTitle",
        url: "../assembler/render?sys_template=7&sys_contentid=42",
      }),
      { item: item(), openWindow, fetchPreview },
    );
    expect(result.kind).toBe("rest");
    expect(fetchPreview).toHaveBeenCalledWith(42, 7);
    expect(openWindow).toHaveBeenCalled();
    const href = String(openWindow.mock.calls[0]?.[0] ?? "");
    expect(href).toContain("/assembler/render");
    expect(href).not.toContain("sys_cxSupport");
  });

  it("does not navigate Content Editor New Item URLs", async () => {
    const openWindow = vi.fn();
    expect(isContentEditorActionUrl("../rx_cePage/page.html")).toBe(true);
    const result = await dispatchAction(
      action({
        name: "percPage",
        url: "../rx_cePage/page.html?sys_folderid=1",
      }),
      { item: item(), openWindow },
    );
    expect(result.kind).toBe("editor");
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_EDITOR_UNAVAILABLE);
    expect(openWindow).not.toHaveBeenCalled();
  });

  it("parses GUID item ids for template preview", async () => {
    const openWindow = vi.fn();
    const fetchPreview = vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_template=7",
      contentId: 708,
      templateId: 7,
      revision: 1,
    });
    await dispatchAction(
      action({
        name: "rffSnTitle",
        url: "../assembler/render?sys_template=7",
      }),
      {
        item: item({ id: "1-101-708" }),
        openWindow,
        fetchPreview,
      },
    );
    expect(fetchPreview).toHaveBeenCalledWith(708, 7);
    expect(openWindow).toHaveBeenCalled();
  });

  it("purge confirms then calls onPurge", async () => {
    const onPurge = vi.fn().mockResolvedValue(undefined);
    const confirm = vi.fn().mockReturnValue(true);
    const result = await dispatchAction(action({ name: "Purge" }), {
      item: item(),
      onPurge,
      confirm,
    });
    expect(confirm).toHaveBeenCalled();
    expect(onPurge).toHaveBeenCalled();
    expect(result.refresh).toBe(true);
  });

  it("purge cancel does not call onPurge", async () => {
    const onPurge = vi.fn();
    const result = await dispatchAction(action({ name: "Purge" }), {
      item: item(),
      onPurge,
      confirm: () => false,
    });
    expect(onPurge).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
  });

  it("publish_now is unavailable without navigation", async () => {
    const openWindow = vi.fn();
    const result = await dispatchAction(action({ name: "Publish_Now" }), {
      item: item(),
      openWindow,
    });
    expect(result.kind).toBe("unavailable");
    expect(openWindow).not.toHaveBeenCalled();
  });

  it("dispatch workflow-transition runs the trigger", async () => {
    const runWorkflow = vi.fn().mockResolvedValue(undefined);
    const result = await dispatchAction(
      action({ name: "workflow-transition:Submit" }),
      { item: item(), runWorkflow },
    );
    expect(result.kind).toBe("workflow");
    expect(result.refresh).toBe(true);
    expect(runWorkflow).toHaveBeenCalledWith("42", "Submit");
  });
});
