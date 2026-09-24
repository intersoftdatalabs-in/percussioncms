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

import { afterEach, describe, expect, it, vi } from "vitest";
import type { MenuAction, PSPathItem } from "../../../main/ts/api/contentExplorer/types";
import * as itemWorkflowApi from "../../../main/ts/api/contentExplorer/itemWorkflowApi";
import {
  classifyAction,
  dispatchAction,
  findMenuParentName,
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
  afterEach(() => {
    vi.restoreAllMocks();
  });

  it("classifies Data Flow URLs as non-navigable", () => {
    expect(isDataFlowActionUrl("../sys_cxSupport/previewslotvariant.html")).toBe(
      true,
    );
    expect(isDataFlowActionUrl("../sys_action/checkoutedit.xml")).toBe(true);
    expect(isDataFlowActionUrl("../sys_Compare/compare.html")).toBe(true);
    expect(isDataFlowActionUrl("/assembler/render?sys_template=1")).toBe(false);
  });

  it("keeps DCE sys_compare as unavailable (React compare is RevisionsPanel)", () => {
    expect(
      classifyAction(
        action({
          name: "Compare",
          url: "../sys_Compare/compare.html",
        }),
      ),
    ).toBe("unavailable");
  });

  it("classifies Edit as editor (not CM1 editor navigation)", () => {
    expect(classifyAction(action({ name: "Edit" }))).toBe("editor");
    expect(classifyAction(action({ name: "Quick_Edit" }))).toBe("editor");
  });

  it("classifies New Item host as rest so the type picker can run", () => {
    expect(classifyAction(action({ name: "New" }))).toBe("rest");
    expect(classifyAction(action({ name: "Create_New_Item" }))).toBe("rest");
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

  it("dispatch Edit opens the React editor host", async () => {
    const openWindow = vi.fn();
    const result = await dispatchAction(action({ name: "Edit" }), {
      item: item(),
      openWindow,
    });
    expect(result.kind).toBe("editor");
    expect(result.messageKey).toBeUndefined();
    const href = String(openWindow.mock.calls[0]?.[0] ?? "");
    expect(href).toContain("entry=editor");
    expect(href).toContain("contentId=42");
    expect(href).toContain("mode=edit");
    expect(href).not.toContain("view=editor");
  });

  it("dispatch View_Content opens the editor in view mode", async () => {
    const openWindow = vi.fn();
    const result = await dispatchAction(action({ name: "View_Content" }), {
      item: item(),
      openWindow,
    });
    expect(result.kind).toBe("editor");
    expect(String(openWindow.mock.calls[0]?.[0] ?? "")).toContain("mode=view");
  });

  it("dispatch revision_promote opens the editor promote form", async () => {
    const openWindow = vi.fn();
    const result = await dispatchAction(action({ name: "revision_promote" }), {
      item: item(),
      openWindow,
    });
    expect(result.kind).toBe("editor");
    expect(String(openWindow.mock.calls[0]?.[0] ?? "")).toContain("mode=promote");
  });

  it("dispatch Data Flow HTML does not navigate", async () => {
    const openWindow = vi.fn();
    const result = await dispatchAction(
      action({
        name: "Lifecycle_Analysis",
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

  it("creates a percPage with a picked template", async () => {
    const openWindow = vi.fn();
    const createItem = vi.fn().mockResolvedValue({
      itemId: "77",
      folderPath: "//Sites/Demo",
      name: "New-percPage.html",
      contentType: "percPage",
    });
    const loadPageTemplates = vi.fn().mockResolvedValue([
      { id: "tpl-a", name: "A" },
      { id: "tpl-b", name: "B" },
    ]);
    const pickPageTemplate = vi.fn().mockResolvedValue("tpl-b");
    const result = await dispatchAction(
      action({ name: "percPage", parentName: "New" }),
      {
        item: item({ type: "folder", path: "/Sites/Demo", id: "1" }),
        folderPath: "/Sites/Demo",
        parentName: "New",
        openWindow,
        createItem,
        loadPageTemplates,
        pickPageTemplate,
      },
    );
    expect(result.refresh).toBe(true);
    expect(createItem).toHaveBeenCalledWith({
      contentType: "percPage",
      folderPath: "/Sites/Demo",
      templateId: "tpl-b",
    });
  });

  it("does not create a percPage without templates", async () => {
    const createItem = vi.fn();
    const result = await dispatchAction(
      action({ name: "percPage", parentName: "New" }),
      {
        folderPath: "/Sites/Demo",
        parentName: "New",
        createItem,
        loadPageTemplates: async () => [],
      },
    );
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_NEEDS_TEMPLATE);
    expect(createItem).not.toHaveBeenCalled();
  });

  it("auto-selects a single percPage template", async () => {
    const createItem = vi.fn().mockResolvedValue({
      itemId: "78",
      folderPath: "//Sites/Demo",
      name: "New-percPage.html",
      contentType: "percPage",
    });
    const pickPageTemplate = vi.fn();
    const result = await dispatchAction(
      action({ name: "percPage", parentName: "New" }),
      {
        folderPath: "/Sites/Demo",
        parentName: "New",
        openWindow: vi.fn(),
        createItem,
        loadPageTemplates: async () => [{ id: "only", name: "Only" }],
        pickPageTemplate,
      },
    );
    expect(result.refresh).toBe(true);
    expect(pickPageTemplate).not.toHaveBeenCalled();
    expect(createItem).toHaveBeenCalledWith({
      contentType: "percPage",
      folderPath: "/Sites/Demo",
      templateId: "only",
    });
  });

  it("does not create a percPage when the template picker is cancelled", async () => {
    const createItem = vi.fn();
    const result = await dispatchAction(
      action({ name: "percPage", parentName: "New" }),
      {
        folderPath: "/Sites/Demo",
        parentName: "New",
        createItem,
        loadPageTemplates: async () => [
          { id: "tpl-a", name: "A" },
          { id: "tpl-b", name: "B" },
        ],
        pickPageTemplate: async () => null,
      },
    );
    expect(result.messageKey).toBeUndefined();
    expect(createItem).not.toHaveBeenCalled();
  });

  it("creates a New Item type and opens the editor", async () => {
    const openWindow = vi.fn();
    const createItem = vi.fn().mockResolvedValue({
      itemId: "99",
      folderPath: "//Sites/Demo",
      name: "New-rffEvent",
      contentType: "rffEvent",
    });
    const result = await dispatchAction(
      action({
        name: "rffEvent",
        url: "../rx_ceEvent/event.html",
        parentName: "New",
      }),
      {
        item: item({ type: "folder", path: "/Sites/Demo", id: "1" }),
        folderPath: "/Sites/Demo",
        parentName: "New",
        openWindow,
        createItem,
      },
    );
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(createItem).toHaveBeenCalledWith({
      contentType: "rffEvent",
      folderPath: "/Sites/Demo",
    });
    expect(String(openWindow.mock.calls[0]?.[0] ?? "")).toContain("entry=editor");
    expect(String(openWindow.mock.calls[0]?.[0] ?? "")).toContain("contentId=99");
  });

  it("New Item host with no types still asks to choose a type", async () => {
    const createItem = vi.fn();
    const result = await dispatchAction(action({ name: "Create_New_Item" }), {
      item: item({ type: "folder", path: "/Sites/Demo" }),
      folderPath: "/Sites/Demo",
      createItem,
      loadContentTypes: async () => [],
    });
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_NEEDS_TYPE);
    expect(createItem).not.toHaveBeenCalled();
  });

  it("New Item host opens a type picker and creates the picked type", async () => {
    const openWindow = vi.fn();
    const createItem = vi.fn().mockResolvedValue({
      itemId: "88",
      folderPath: "//Sites/Demo",
      name: "New-rffEvent",
      contentType: "rffEvent",
    });
    const pickContentType = vi.fn().mockResolvedValue("rffEvent");
    const result = await dispatchAction(action({ name: "New" }), {
      item: item({ type: "folder", path: "/Sites/Demo", id: "1" }),
      folderPath: "/Sites/Demo",
      createItem,
      openWindow,
      pickContentType,
      loadContentTypes: async () => [
        { name: "percFile", label: "File" },
        { name: "rffEvent", label: "Event" },
      ],
    });
    expect(result.messageKey).toBeUndefined();
    expect(result.refresh).toBe(true);
    expect(pickContentType).toHaveBeenCalled();
    expect(createItem).toHaveBeenCalledWith({
      contentType: "rffEvent",
      folderPath: "/Sites/Demo",
    });
    expect(String(openWindow.mock.calls[0]?.[0] ?? "")).toContain("contentId=88");
  });

  it("New Item host auto-selects a single allowed type", async () => {
    const createItem = vi.fn().mockResolvedValue({
      itemId: "89",
      folderPath: "//Sites/Demo",
      name: "New-percFile",
      contentType: "percFile",
    });
    const pickContentType = vi.fn();
    const result = await dispatchAction(action({ name: "Create_New_Item" }), {
      folderPath: "/Sites/Demo",
      createItem,
      openWindow: vi.fn(),
      pickContentType,
      loadContentTypes: async () => [{ name: "percFile", label: "File" }],
    });
    expect(result.refresh).toBe(true);
    expect(pickContentType).not.toHaveBeenCalled();
    expect(createItem).toHaveBeenCalledWith({
      contentType: "percFile",
      folderPath: "/Sites/Demo",
    });
  });

  it("New Item host uses type children without calling the catalog", async () => {
    const createItem = vi.fn().mockResolvedValue({
      itemId: "90",
      folderPath: "//Sites/Demo",
      name: "New-rffEvent",
      contentType: "rffEvent",
    });
    const loadContentTypes = vi.fn();
    const pickContentType = vi.fn().mockResolvedValue("rffEvent");
    await dispatchAction(
      action({
        name: "New",
        children: [
          { name: "percFile", label: "File", sortRank: 1, menuType: "MENUITEM" },
          { name: "rffEvent", label: "Event", sortRank: 2, menuType: "MENUITEM" },
        ],
      }),
      {
        folderPath: "/Sites/Demo",
        createItem,
        openWindow: vi.fn(),
        pickContentType,
        loadContentTypes,
      },
    );
    expect(loadContentTypes).not.toHaveBeenCalled();
    expect(createItem).toHaveBeenCalledWith({
      contentType: "rffEvent",
      folderPath: "/Sites/Demo",
    });
  });

  it("does not create when the New Item type picker is cancelled", async () => {
    const createItem = vi.fn();
    const result = await dispatchAction(action({ name: "New" }), {
      folderPath: "/Sites/Demo",
      createItem,
      pickContentType: async () => null,
      loadContentTypes: async () => [
        { name: "percFile", label: "File" },
        { name: "rffEvent", label: "Event" },
      ],
    });
    expect(result.messageKey).toBeUndefined();
    expect(createItem).not.toHaveBeenCalled();
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

  it("Translate opens the translations panel", async () => {
    const onShowTranslations = vi.fn();
    const result = await dispatchAction(action({ name: "Translate" }), {
      item: item(),
      onShowTranslations,
    });
    expect(result.kind).toBe("client");
    expect(onShowTranslations).toHaveBeenCalledTimes(1);
  });

  it("Impact Analysis opens the dependencies panel", async () => {
    const onShowDependencies = vi.fn();
    const result = await dispatchAction(
      action({ name: "Item_ViewDependents" }),
      { item: item(), onShowDependencies },
    );
    expect(result.kind).toBe("client");
    expect(onShowDependencies).toHaveBeenCalledTimes(1);
  });

  it("Copy URL writes the site preview URL", async () => {
    const writeClipboard = vi.fn().mockResolvedValue(undefined);
    const result = await dispatchAction(
      action({ name: "Copy_URL_to_Clipboard" }),
      {
        item: item({ path: "/Sites/Demo/Home" }),
        writeClipboard,
      },
    );
    expect(result.kind).toBe("client");
    expect(writeClipboard).toHaveBeenCalled();
    const written = String(writeClipboard.mock.calls[0]?.[0] ?? "");
    expect(written.toLowerCase()).toContain("/sites/demo/home");
  });

  it("classifies remaining P1 names even when the catalog still has Data Flow URLs", () => {
    expect(
      classifyAction(
        action({
          name: "Workflow_Revisions",
          url: "../sys_cxSupport/contenteditorurls.html?sys_userview=sys_Revisions",
        }),
      ),
    ).toBe("client");
    expect(
      classifyAction(
        action({
          name: "Workflow_NewVersion",
          url: "../sys_cxSupport/contenteditorurls.html?sys_command=relate",
        }),
      ),
    ).toBe("rest");
    expect(
      classifyAction(
        action({
          name: "Flush_Cache",
          url: "../sys_uiSupport/flushcache.html",
        }),
      ),
    ).toBe("rest");
    expect(
      classifyAction(
        action({
          name: "navreset",
          url: "../rxs_navSupport/navreset.html",
        }),
      ),
    ).toBe("rest");
  });

  it("Revisions without a content item asks to select one", async () => {
    const onShowRevisions = vi.fn();
    const result = await dispatchAction(action({ name: "Workflow_Revisions" }), {
      item: null,
      onShowRevisions,
    });
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_NEEDS_ITEM);
    expect(onShowRevisions).toHaveBeenCalledWith("revisions");
  });

  it("Revisions opens the revisions panel", async () => {
    const onShowRevisions = vi.fn();
    const result = await dispatchAction(action({ name: "Workflow_Revisions" }), {
      item: item(),
      onShowRevisions,
    });
    expect(result.kind).toBe("client");
    expect(onShowRevisions).toHaveBeenCalledWith("revisions");
  });

  it("Audit Trail opens the revisions panel on the audit tab", async () => {
    const onShowRevisions = vi.fn();
    const result = await dispatchAction(
      action({ name: "Workflow_AuditTrail" }),
      { item: item(), onShowRevisions },
    );
    expect(result.kind).toBe("client");
    expect(onShowRevisions).toHaveBeenCalledWith("audit");
  });

  it("Flush Cache confirms then flushes", async () => {
    const flushCache = vi.fn().mockResolvedValue(undefined);
    const confirm = vi.fn().mockReturnValue(true);
    const result = await dispatchAction(action({ name: "Flush_Cache" }), {
      item: item(),
      flushCache,
      confirm,
    });
    expect(result.kind).toBe("rest");
    expect(confirm).toHaveBeenCalled();
    expect(flushCache).toHaveBeenCalledTimes(1);
  });

  it("Flush Cache cancel does not flush", async () => {
    const flushCache = vi.fn();
    const result = await dispatchAction(action({ name: "Flush_Cache" }), {
      item: item(),
      flushCache,
      confirm: () => false,
    });
    expect(flushCache).not.toHaveBeenCalled();
    expect(result.kind).toBe("rest");
  });

  it("Nav Reset confirms then resets", async () => {
    const resetNav = vi.fn().mockResolvedValue(undefined);
    const result = await dispatchAction(action({ name: "navreset" }), {
      item: item(),
      resetNav,
      confirm: () => true,
    });
    expect(result.kind).toBe("rest");
    expect(resetNav).toHaveBeenCalledTimes(1);
  });

  it("New Copy confirms then copies", async () => {
    const createCopy = vi.fn().mockResolvedValue(undefined);
    const result = await dispatchAction(
      action({ name: "Workflow_NewVersion" }),
      { item: item(), createCopy, confirm: () => true },
    );
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(createCopy).toHaveBeenCalledWith("42");
  });

  it("Promotable Version confirms then creates", async () => {
    const createPromotable = vi.fn().mockResolvedValue(undefined);
    const result = await dispatchAction(
      action({ name: "Edit_PromotableVersion" }),
      { item: item(), createPromotable, confirm: () => true },
    );
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(createPromotable).toHaveBeenCalledWith("42");
  });

  it("New Copy cancel does not copy", async () => {
    const createCopy = vi.fn();
    const result = await dispatchAction(
      action({ name: "Workflow_NewVersion" }),
      { item: item(), createCopy, confirm: () => false },
    );
    expect(createCopy).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
  });

  it("Promotable Version cancel does not create", async () => {
    const createPromotable = vi.fn();
    const result = await dispatchAction(
      action({ name: "Edit_PromotableVersion" }),
      { item: item(), createPromotable, confirm: () => false },
    );
    expect(createPromotable).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
  });

  it("classifies Publish Now as rest even with demandpublishing URL", () => {
    expect(
      classifyAction(
        action({
          name: "Publish_Now",
          url: "../publisher/demandpublishing",
        }),
      ),
    ).toBe("rest");
  });

  it("Publish Now confirms then publishes", async () => {
    const onPublish = vi.fn().mockResolvedValue(undefined);
    const confirm = vi.fn().mockReturnValue(true);
    const openWindow = vi.fn();
    const result = await dispatchAction(
      action({ name: "Publish_Now", url: "../publisher/demandpublishing" }),
      { item: item(), onPublish, confirm, openWindow },
    );
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(confirm).toHaveBeenCalled();
    expect(onPublish).toHaveBeenCalled();
    expect(openWindow).not.toHaveBeenCalled();
  });

  it("classifies Active Assembly parents and slot actions as rest", () => {
    expect(classifyAction(action({ name: "Item_ActiveAssembly" }))).toBe("rest");
    expect(
      classifyAction(action({ name: "EnterpriseItem_ActiveAssembly" })),
    ).toBe("rest");
    expect(classifyAction(action({ name: "Item_Assembly" }))).toBe("rest");
    expect(classifyAction(action({ name: "Arrange_Remove" }))).toBe("rest");
    expect(classifyAction(action({ name: "Slot_Add" }))).toBe("rest");
    expect(classifyAction(action({ name: "AA_Table_Editor" }))).toBe(
      "unavailable",
    );
  });

  it("finds the parent menu name for a nested template child", () => {
    expect(
      findMenuParentName(
        [
          action({
            name: "Item_ActiveAssembly",
            children: [action({ name: "rffPgGeneric" })],
          }),
        ],
        "rffPgGeneric",
      ),
    ).toBe("Item_ActiveAssembly");
  });

  it("opens the assembly host for Active Assembly without fetching preview", async () => {
    const openWindow = vi.fn();
    const fetchPreview = vi.fn();
    const result = await dispatchAction(
      action({ name: "Item_ActiveAssembly" }),
      { item: item(), openWindow, fetchPreview },
    );
    expect(result.kind).toBe("rest");
    expect(fetchPreview).not.toHaveBeenCalled();
    expect(openWindow).toHaveBeenCalledTimes(1);
    const href = String(openWindow.mock.calls[0]?.[0] ?? "");
    expect(href).toContain("entry=assembly");
    expect(href).toContain("contentId=42");
    expect(href).not.toContain("templateId=");
    expect(openWindow.mock.calls[0]?.[1]).toBe("percAssembly_42");
  });

  it("opens the assembly host with a template when the AA child is invoked", async () => {
    const openWindow = vi.fn();
    const fetchPreview = vi.fn();
    const result = await dispatchAction(
      action({
        name: "rffPgGeneric",
        url: "../assembler/render?sys_template=7",
      }),
      {
        item: item(),
        parentName: "Item_ActiveAssembly",
        openWindow,
        fetchPreview,
      },
    );
    expect(result.kind).toBe("rest");
    expect(fetchPreview).not.toHaveBeenCalled();
    const href = String(openWindow.mock.calls[0]?.[0] ?? "");
    expect(href).toContain("entry=assembly");
    expect(href).toContain("contentId=42");
    expect(href).toContain("templateId=7");
  });

  it("still opens raw assembler preview for Preview template children", async () => {
    const openWindow = vi.fn();
    const fetchPreview = vi.fn().mockResolvedValue({
      previewUrl: "/assembler/render?sys_contentid=42&sys_template=7",
      contentId: 42,
      templateId: 7,
      revision: 1,
    });
    const result = await dispatchAction(
      action({
        name: "rffPgGeneric",
        url: "../assembler/render?sys_template=7",
      }),
      {
        item: item(),
        parentName: "Item_Preview",
        openWindow,
        fetchPreview,
      },
    );
    expect(result.kind).toBe("rest");
    expect(fetchPreview).toHaveBeenCalledWith(42, 7);
    expect(String(openWindow.mock.calls[0]?.[0] ?? "")).toContain(
      "/assembler/render",
    );
    expect(String(openWindow.mock.calls[0]?.[0] ?? "")).not.toContain(
      "entry=assembly",
    );
  });

  it("Publish Now cancel does not publish", async () => {
    const onPublish = vi.fn();
    const result = await dispatchAction(action({ name: "Publish_Now" }), {
      item: item(),
      onPublish,
      confirm: () => false,
    });
    expect(onPublish).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
  });

  it("multi-select Publish Now confirms once and publishes each page and asset", async () => {
    const onPublish = vi.fn().mockResolvedValue(undefined);
    const confirm = vi.fn().mockReturnValue(true);
    const folder = item({
      id: "7",
      name: "News",
      path: "/Sites/Demo/News",
      type: "folder",
      leaf: false,
    });
    const asset = item({
      id: "99",
      name: "logo",
      path: "/Assets/logo.png",
      type: "percImageAsset",
    });
    const page = item({ id: "42", name: "Home" });
    const result = await dispatchAction(action({ name: "Publish_Now" }), {
      item: page,
      selectedItems: [page, folder, asset],
      onPublish,
      confirm,
    });
    expect(confirm).toHaveBeenCalledTimes(1);
    expect(String(confirm.mock.calls[0]?.[0] ?? "")).toContain("2");
    expect(onPublish).toHaveBeenCalledTimes(2);
    expect(onPublish.mock.calls.map((call) => call[0].id)).toEqual(["42", "99"]);
    expect(result.refresh).toBe(true);
    expect(result.messageText ?? "").toMatch(/Folders are not published: News/);
  });

  it("multi-select Publish Now cancel publishes nothing", async () => {
    const onPublish = vi.fn();
    const result = await dispatchAction(action({ name: "Publish_Now" }), {
      item: item(),
      selectedItems: [item({ id: "42" }), item({ id: "43", name: "About" })],
      onPublish,
      confirm: () => false,
    });
    expect(onPublish).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
    expect(result.messageText).toBeUndefined();
  });

  it("multi-select Publish Now reports one HTTP failure without claiming full success", async () => {
    const onPublish = vi.fn(async (row: PSPathItem) => {
      if (row.id === "43") {
        throw { status: 409, statusText: "Conflict", body: "locked" };
      }
    });
    const result = await dispatchAction(action({ name: "Publish_Now" }), {
      item: item(),
      selectedItems: [
        item({ id: "42", name: "Home" }),
        item({ id: "43", name: "About" }),
      ],
      onPublish,
      confirm: () => true,
    });
    expect(onPublish).toHaveBeenCalledTimes(2);
    expect(result.refresh).toBe(true);
    expect(result.messageKey).toBe(EXPLORER_MSG.PUBLISH_BATCH_INCOMPLETE);
    expect(result.messageText ?? "").toMatch(/About \(HTTP 409\)/);
    expect(result.messageText ?? "").not.toMatch(/Home \(HTTP/);
  });

  it("multi-select of only folders does not publish", async () => {
    const onPublish = vi.fn();
    const result = await dispatchAction(action({ name: "Publish_Now" }), {
      item: item(),
      selectedItems: [
        item({
          id: "1",
          name: "Sites",
          path: "/Sites",
          type: "folder",
          leaf: false,
        }),
        item({
          id: "2",
          name: "News",
          path: "/Sites/Demo/News",
          type: "folder",
          leaf: false,
        }),
      ],
      onPublish,
      confirm: () => true,
    });
    expect(onPublish).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
    expect(result.messageText ?? "").toMatch(
      /Folders are not published: Sites, News/,
    );
  });

  it("Publish Now on a Sites folder asks for a content item and does not publish", async () => {
    const onPublish = vi.fn();
    const result = await dispatchAction(action({ name: "Publish_Now" }), {
      item: item({
        id: "1",
        name: "Sites",
        path: "/Sites",
        type: "folder",
        leaf: false,
      }),
      onPublish,
      confirm: () => true,
    });
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_NEEDS_ITEM);
    expect(result.refresh).toBeUndefined();
    expect(onPublish).not.toHaveBeenCalled();
  });

  it("classifies Check Out and Check In as rest", () => {
    expect(classifyAction(action({ name: "Check_Out" }))).toBe("rest");
    expect(classifyAction(action({ name: "Check_In" }))).toBe("rest");
  });

  it("Check Out calls workflow checkOut and refreshes", async () => {
    const checkOut = vi
      .spyOn(itemWorkflowApi, "checkOutItem")
      .mockResolvedValue(undefined);
    const result = await dispatchAction(action({ name: "Check_Out" }), {
      item: item(),
    });
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(checkOut).toHaveBeenCalledWith("42");
  });

  it("Check Out on a folder asks for a content item", async () => {
    const checkOut = vi
      .spyOn(itemWorkflowApi, "checkOutItem")
      .mockResolvedValue(undefined);
    const result = await dispatchAction(action({ name: "Check_Out" }), {
      item: item({
        id: "1",
        name: "Sites",
        path: "/Sites",
        type: "folder",
        leaf: false,
      }),
    });
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_NEEDS_ITEM);
    expect(checkOut).not.toHaveBeenCalled();
  });

  it("Check Out maps HTTP 403/409", async () => {
    vi.spyOn(itemWorkflowApi, "checkOutItem")
      .mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: {} })
      .mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: {} });
    const forbidden = await dispatchAction(action({ name: "Check_Out" }), {
      item: item(),
    });
    expect(forbidden.messageKey).toBe(EXPLORER_MSG.CHECKOUT_FORBIDDEN);
    const conflict = await dispatchAction(action({ name: "Check_Out" }), {
      item: item(),
    });
    expect(conflict.messageKey).toBe(EXPLORER_MSG.CHECKOUT_CONFLICT);
  });

  it("Check In calls workflow checkIn and refreshes", async () => {
    const checkIn = vi
      .spyOn(itemWorkflowApi, "checkInItem")
      .mockResolvedValue(undefined);
    const result = await dispatchAction(action({ name: "Check_In" }), {
      item: item(),
    });
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(checkIn).toHaveBeenCalledWith("42");
  });

  it("Check In maps HTTP 403/409", async () => {
    vi.spyOn(itemWorkflowApi, "checkInItem")
      .mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: {} })
      .mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: {} });
    const forbidden = await dispatchAction(action({ name: "Check_In" }), {
      item: item(),
    });
    expect(forbidden.messageKey).toBe(EXPLORER_MSG.CHECKIN_FORBIDDEN);
    const conflict = await dispatchAction(action({ name: "Check_In" }), {
      item: item(),
    });
    expect(conflict.messageKey).toBe(EXPLORER_MSG.CHECKIN_CONFLICT);
  });

  it("classifies Force Check-in as rest", () => {
    expect(classifyAction(action({ name: "Force_Checkin" }))).toBe("rest");
  });

  it("Force Check-in confirms then refreshes", async () => {
    const onForceCheckin = vi.fn().mockResolvedValue(undefined);
    const confirm = vi.fn().mockReturnValue(true);
    const result = await dispatchAction(action({ name: "Force_Checkin" }), {
      item: item(),
      onForceCheckin,
      confirm,
    });
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(confirm).toHaveBeenCalled();
    expect(onForceCheckin).toHaveBeenCalled();
  });

  it("Force Check-in cancel does not call REST", async () => {
    const onForceCheckin = vi.fn();
    const result = await dispatchAction(action({ name: "Force_Checkin" }), {
      item: item(),
      onForceCheckin,
      confirm: () => false,
    });
    expect(onForceCheckin).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
  });

  it("Force Check-in on a folder asks for a content item", async () => {
    const onForceCheckin = vi.fn();
    const result = await dispatchAction(action({ name: "Force_Checkin" }), {
      item: item({
        id: "1",
        name: "Sites",
        path: "/Sites",
        type: "folder",
        leaf: false,
      }),
      onForceCheckin,
      confirm: () => true,
    });
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_NEEDS_ITEM);
    expect(onForceCheckin).not.toHaveBeenCalled();
  });

  it("Force Check-in maps HTTP 403/404/409", async () => {
    const onForceCheckin = vi
      .fn()
      .mockRejectedValueOnce({ status: 403, statusText: "Forbidden", body: {} })
      .mockRejectedValueOnce({ status: 404, statusText: "Not Found", body: {} })
      .mockRejectedValueOnce({ status: 409, statusText: "Conflict", body: {} });
    const forbidden = await dispatchAction(action({ name: "Force_Checkin" }), {
      item: item(),
      onForceCheckin,
      confirm: () => true,
    });
    expect(forbidden.messageKey).toBe(EXPLORER_MSG.FORCE_CHECKIN_FORBIDDEN);
    const missing = await dispatchAction(action({ name: "Force_Checkin" }), {
      item: item(),
      onForceCheckin,
      confirm: () => true,
    });
    expect(missing.messageKey).toBe(EXPLORER_MSG.FORCE_CHECKIN_NOT_FOUND);
    const notOut = await dispatchAction(action({ name: "Force_Checkin" }), {
      item: item(),
      onForceCheckin,
      confirm: () => true,
    });
    expect(notOut.messageKey).toBe(EXPLORER_MSG.FORCE_CHECKIN_NOT_CHECKED_OUT);
  });

  it("classifies Take Down as rest", () => {
    expect(classifyAction(action({ name: "Take_Down" }))).toBe("rest");
    expect(classifyAction(action({ name: "unpublish" }))).toBe("rest");
  });

  it("Take Down confirms then unpublishes", async () => {
    const onTakedown = vi.fn().mockResolvedValue(undefined);
    const confirm = vi.fn().mockReturnValue(true);
    vi.spyOn(global, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ ArrayList: [] }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const result = await dispatchAction(action({ name: "Take_Down" }), {
      item: item(),
      onTakedown,
      confirm,
    });
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(confirm).toHaveBeenCalled();
    expect(String(confirm.mock.calls[0]?.[0] ?? "")).toMatch(/Take down/i);
    expect(onTakedown).toHaveBeenCalled();
  });

  it("Take Down cancel does not unpublish", async () => {
    const onTakedown = vi.fn();
    vi.spyOn(global, "fetch").mockResolvedValue(
      new Response("[]", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const result = await dispatchAction(action({ name: "Take_Down" }), {
      item: item(),
      onTakedown,
      confirm: () => false,
    });
    expect(onTakedown).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
  });

  it("Take Down on a Sites folder asks for a content item", async () => {
    const onTakedown = vi.fn();
    const result = await dispatchAction(action({ name: "Take_Down" }), {
      item: item({
        id: "1",
        name: "Sites",
        path: "/Sites",
        type: "folder",
        leaf: false,
      }),
      onTakedown,
      confirm: () => true,
    });
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_NEEDS_ITEM);
    expect(onTakedown).not.toHaveBeenCalled();
  });

  it("Take Down confirm lists linked page paths", async () => {
    const onTakedown = vi.fn().mockResolvedValue(undefined);
    const confirm = vi.fn().mockReturnValue(true);
    vi.spyOn(global, "fetch").mockResolvedValueOnce(
      new Response(
        JSON.stringify({
          ArrayList: [{ pagePath: "/Sites/Demo/Home" }],
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    await dispatchAction(action({ name: "Take_Down" }), {
      item: item(),
      onTakedown,
      confirm,
    });
    expect(String(confirm.mock.calls[0]?.[0] ?? "")).toContain(
      "/Sites/Demo/Home",
    );
    expect(onTakedown).toHaveBeenCalled();
  });

  it("multi-select Take Down confirms once and unpublishes each page and asset", async () => {
    const onTakedown = vi.fn().mockResolvedValue(undefined);
    const confirm = vi.fn().mockReturnValue(true);
    const folder = item({
      id: "7",
      name: "News",
      path: "/Sites/Demo/News",
      type: "folder",
      leaf: false,
    });
    const asset = item({
      id: "99",
      name: "logo",
      path: "/Assets/logo.png",
      type: "percImageAsset",
    });
    const page = item({ id: "42", name: "Home" });
    const result = await dispatchAction(action({ name: "Take_Down" }), {
      item: page,
      selectedItems: [page, folder, asset],
      onTakedown,
      confirm,
    });
    expect(confirm).toHaveBeenCalledTimes(1);
    expect(String(confirm.mock.calls[0]?.[0] ?? "")).toContain("2");
    expect(onTakedown).toHaveBeenCalledTimes(2);
    expect(onTakedown.mock.calls.map((call) => call[0].id)).toEqual([
      "42",
      "99",
    ]);
    expect(result.refresh).toBe(true);
    expect(result.messageText ?? "").toMatch(/Folders are not taken down: News/);
  });

  it("multi-select Take Down cancel takes down nothing", async () => {
    const onTakedown = vi.fn();
    const result = await dispatchAction(action({ name: "Take_Down" }), {
      item: item(),
      selectedItems: [item({ id: "42" }), item({ id: "43", name: "About" })],
      onTakedown,
      confirm: () => false,
    });
    expect(onTakedown).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
    expect(result.messageText).toBeUndefined();
  });

  it("multi-select Take Down reports one HTTP failure without claiming full success", async () => {
    const onTakedown = vi.fn(async (row: PSPathItem) => {
      if (row.id === "43") {
        throw { status: 403, statusText: "Forbidden", body: "denied" };
      }
    });
    const result = await dispatchAction(action({ name: "Take_Down" }), {
      item: item(),
      selectedItems: [
        item({ id: "42", name: "Home" }),
        item({ id: "43", name: "About" }),
      ],
      onTakedown,
      confirm: () => true,
    });
    expect(onTakedown).toHaveBeenCalledTimes(2);
    expect(result.refresh).toBe(true);
    expect(result.messageKey).toBe(EXPLORER_MSG.TAKEDOWN_BATCH_INCOMPLETE);
    expect(result.messageText ?? "").toMatch(/About \(HTTP 403\)/);
    expect(result.messageText ?? "").not.toMatch(/Home \(HTTP/);
  });

  it("multi-select of only folders does not take down", async () => {
    const onTakedown = vi.fn();
    const result = await dispatchAction(action({ name: "Take_Down" }), {
      item: item(),
      selectedItems: [
        item({
          id: "1",
          name: "Sites",
          path: "/Sites",
          type: "folder",
          leaf: false,
        }),
        item({
          id: "2",
          name: "News",
          path: "/Sites/Demo/News",
          type: "folder",
          leaf: false,
        }),
      ],
      onTakedown,
      confirm: () => true,
    });
    expect(onTakedown).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
    expect(result.messageText ?? "").toMatch(
      /Folders are not taken down: Sites, News/,
    );
  });

  it("classifies Stage and Remove from Staging as rest", () => {
    expect(classifyAction(action({ name: "Stage" }))).toBe("rest");
    expect(classifyAction(action({ name: "Remove_from_Staging" }))).toBe(
      "rest",
    );
    expect(classifyAction(action({ name: "unstage" }))).toBe("rest");
  });

  it("Stage confirms then stages", async () => {
    const onStage = vi.fn().mockResolvedValue(undefined);
    const confirm = vi.fn().mockReturnValue(true);
    const result = await dispatchAction(action({ name: "Stage" }), {
      item: item(),
      onStage,
      confirm,
    });
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(confirm).toHaveBeenCalledWith(EXPLORER_MSG.CONFIRM_STAGE);
    expect(onStage).toHaveBeenCalled();
  });

  it("Stage cancel does not stage", async () => {
    const onStage = vi.fn();
    const result = await dispatchAction(action({ name: "Stage" }), {
      item: item(),
      onStage,
      confirm: () => false,
    });
    expect(onStage).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
  });

  it("multi-select Stage confirms once and stages each page and asset", async () => {
    const onStage = vi.fn().mockResolvedValue(undefined);
    const confirm = vi.fn().mockReturnValue(true);
    const folder = item({
      id: "7",
      name: "News",
      path: "/Sites/Demo/News",
      type: "folder",
      leaf: false,
    });
    const asset = item({
      id: "99",
      name: "logo",
      path: "/Assets/logo.png",
      type: "percImageAsset",
    });
    const page = item({ id: "42", name: "Home" });
    const result = await dispatchAction(action({ name: "Stage" }), {
      item: page,
      selectedItems: [page, folder, asset],
      onStage,
      confirm,
    });
    expect(confirm).toHaveBeenCalledTimes(1);
    expect(String(confirm.mock.calls[0]?.[0] ?? "")).toContain("2");
    expect(onStage).toHaveBeenCalledTimes(2);
    expect(onStage.mock.calls.map((call) => call[0].id)).toEqual(["42", "99"]);
    expect(result.refresh).toBe(true);
    expect(result.messageText ?? "").toMatch(/Folders are not staged: News/);
  });

  it("multi-select Stage cancel stages nothing", async () => {
    const onStage = vi.fn();
    const result = await dispatchAction(action({ name: "Stage" }), {
      item: item(),
      selectedItems: [item({ id: "42" }), item({ id: "43", name: "About" })],
      onStage,
      confirm: () => false,
    });
    expect(onStage).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
    expect(result.messageText).toBeUndefined();
  });

  it("multi-select Stage reports one HTTP failure without claiming full success", async () => {
    const onStage = vi.fn(async (row: PSPathItem) => {
      if (row.id === "43") {
        throw { status: 409, statusText: "Conflict", body: "locked" };
      }
    });
    const result = await dispatchAction(action({ name: "Stage" }), {
      item: item(),
      selectedItems: [
        item({ id: "42", name: "Home" }),
        item({ id: "43", name: "About" }),
      ],
      onStage,
      confirm: () => true,
    });
    expect(onStage).toHaveBeenCalledTimes(2);
    expect(result.refresh).toBe(true);
    expect(result.messageKey).toBe(EXPLORER_MSG.STAGE_BATCH_INCOMPLETE);
    expect(result.messageText ?? "").toMatch(/About \(HTTP 409\)/);
    expect(result.messageText ?? "").not.toMatch(/Home \(HTTP/);
  });

  it("multi-select of only folders does not stage", async () => {
    const onStage = vi.fn();
    const result = await dispatchAction(action({ name: "Stage" }), {
      item: item(),
      selectedItems: [
        item({
          id: "1",
          name: "Sites",
          path: "/Sites",
          type: "folder",
          leaf: false,
        }),
        item({
          id: "2",
          name: "News",
          path: "/Sites/Demo/News",
          type: "folder",
          leaf: false,
        }),
      ],
      onStage,
      confirm: () => true,
    });
    expect(onStage).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
    expect(result.messageText ?? "").toMatch(/Folders are not staged: Sites, News/);
  });

  it("Stage on a Sites folder asks for a content item", async () => {
    const onStage = vi.fn();
    const result = await dispatchAction(action({ name: "Stage" }), {
      item: item({
        id: "1",
        name: "Sites",
        path: "/Sites",
        type: "folder",
        leaf: false,
      }),
      onStage,
      confirm: () => true,
    });
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_NEEDS_ITEM);
    expect(onStage).not.toHaveBeenCalled();
  });

  it("Stage on a template stays unavailable", async () => {
    const result = await dispatchAction(action({ name: "Stage" }), {
      item: item({
        path: "/Design/Templates/base",
        type: "percTemplate",
        category: "template",
        id: "77",
      }),
      confirm: () => true,
    });
    expect(result.kind).toBe("unavailable");
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_UNAVAILABLE);
  });

  it("Remove from Staging confirms then unstages", async () => {
    const onRemoveFromStaging = vi.fn().mockResolvedValue(undefined);
    const confirm = vi.fn().mockReturnValue(true);
    const result = await dispatchAction(
      action({ name: "Remove_from_Staging" }),
      {
        item: item(),
        onRemoveFromStaging,
        confirm,
      },
    );
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(confirm).toHaveBeenCalledWith(
      EXPLORER_MSG.CONFIRM_REMOVE_FROM_STAGING,
    );
    expect(onRemoveFromStaging).toHaveBeenCalled();
  });

  it("multi-select Remove from Staging confirms once and unstages each page and asset", async () => {
    const onRemoveFromStaging = vi.fn().mockResolvedValue(undefined);
    const confirm = vi.fn().mockReturnValue(true);
    const folder = item({
      id: "7",
      name: "News",
      path: "/Sites/Demo/News",
      type: "folder",
      leaf: false,
    });
    const asset = item({
      id: "99",
      name: "logo",
      path: "/Assets/logo.png",
      type: "percImageAsset",
    });
    const page = item({ id: "42", name: "Home" });
    const result = await dispatchAction(
      action({ name: "Remove_from_Staging" }),
      {
        item: page,
        selectedItems: [page, folder, asset],
        onRemoveFromStaging,
        confirm,
      },
    );
    expect(confirm).toHaveBeenCalledTimes(1);
    expect(String(confirm.mock.calls[0]?.[0] ?? "")).toContain("2");
    expect(onRemoveFromStaging).toHaveBeenCalledTimes(2);
    expect(onRemoveFromStaging.mock.calls.map((call) => call[0].id)).toEqual([
      "42",
      "99",
    ]);
    expect(result.refresh).toBe(true);
    expect(result.messageText ?? "").toMatch(
      /Folders are not removed from staging: News/,
    );
  });

  it("multi-select Remove from Staging cancel unstages nothing", async () => {
    const onRemoveFromStaging = vi.fn();
    const result = await dispatchAction(
      action({ name: "Remove_from_Staging" }),
      {
        item: item(),
        selectedItems: [item({ id: "42" }), item({ id: "43", name: "About" })],
        onRemoveFromStaging,
        confirm: () => false,
      },
    );
    expect(onRemoveFromStaging).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
    expect(result.messageText).toBeUndefined();
  });

  it("multi-select Remove from Staging reports one HTTP failure without claiming full success", async () => {
    const onRemoveFromStaging = vi.fn(async (row: PSPathItem) => {
      if (row.id === "43") {
        throw { status: 404, statusText: "Not Found", body: "missing" };
      }
    });
    const result = await dispatchAction(
      action({ name: "Remove_from_Staging" }),
      {
        item: item(),
        selectedItems: [
          item({ id: "42", name: "Home" }),
          item({ id: "43", name: "About" }),
        ],
        onRemoveFromStaging,
        confirm: () => true,
      },
    );
    expect(onRemoveFromStaging).toHaveBeenCalledTimes(2);
    expect(result.refresh).toBe(true);
    expect(result.messageKey).toBe(EXPLORER_MSG.UNSTAGE_BATCH_INCOMPLETE);
    expect(result.messageText ?? "").toMatch(/About \(HTTP 404\)/);
    expect(result.messageText ?? "").not.toMatch(/Home \(HTTP/);
  });

  it("multi-select of only folders does not remove from staging", async () => {
    const onRemoveFromStaging = vi.fn();
    const result = await dispatchAction(
      action({ name: "Remove_from_Staging" }),
      {
        item: item(),
        selectedItems: [
          item({
            id: "1",
            name: "Sites",
            path: "/Sites",
            type: "folder",
            leaf: false,
          }),
          item({
            id: "2",
            name: "News",
            path: "/Sites/Demo/News",
            type: "folder",
            leaf: false,
          }),
        ],
        onRemoveFromStaging,
        confirm: () => true,
      },
    );
    expect(onRemoveFromStaging).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
    expect(result.messageText ?? "").toMatch(
      /Folders are not removed from staging: Sites, News/,
    );
  });

  it("Remove from Staging cancel does not unstage", async () => {
    const onRemoveFromStaging = vi.fn();
    const result = await dispatchAction(
      action({ name: "Remove_from_Staging" }),
      {
        item: item(),
        onRemoveFromStaging,
        confirm: () => false,
      },
    );
    expect(onRemoveFromStaging).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
  });

  it("Remove from Staging on a Sites folder asks for a content item", async () => {
    const onRemoveFromStaging = vi.fn();
    const result = await dispatchAction(
      action({ name: "Remove_from_Staging" }),
      {
        item: item({
          id: "1",
          name: "Sites",
          path: "/Sites",
          type: "folder",
          leaf: false,
        }),
        onRemoveFromStaging,
        confirm: () => true,
      },
    );
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_NEEDS_ITEM);
    expect(onRemoveFromStaging).not.toHaveBeenCalled();
  });

  it("classifies Schedule as rest", () => {
    expect(classifyAction(action({ name: "Schedule" }))).toBe("rest");
    expect(classifyAction(action({ name: "schedule_dates" }))).toBe("rest");
  });

  it("Schedule confirms then saves dates", async () => {
    const onSchedule = vi.fn().mockResolvedValue(undefined);
    const confirm = vi.fn().mockReturnValue(true);
    const pickScheduleDates = vi.fn().mockResolvedValue({
      itemId: "42",
      startDate: "09/18/2026 09:00 am",
      endDate: "",
      comments: "ok",
    });
    vi.spyOn(global, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({
          ItemDates: { itemId: "42", startDate: "", endDate: "" },
        }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const result = await dispatchAction(action({ name: "Schedule" }), {
      item: item(),
      onSchedule,
      pickScheduleDates,
      confirm,
    });
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(pickScheduleDates).toHaveBeenCalled();
    expect(confirm).toHaveBeenCalledWith(EXPLORER_MSG.CONFIRM_SCHEDULE);
    expect(onSchedule).toHaveBeenCalled();
  });

  it("Schedule cancel from picker does not save", async () => {
    const onSchedule = vi.fn();
    vi.spyOn(global, "fetch").mockResolvedValue(
      new Response(
        JSON.stringify({ ItemDates: { itemId: "42" } }),
        { status: 200, headers: { "Content-Type": "application/json" } },
      ),
    );
    const result = await dispatchAction(action({ name: "Schedule" }), {
      item: item(),
      onSchedule,
      pickScheduleDates: async () => null,
      confirm: () => true,
    });
    expect(onSchedule).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
  });

  it("Schedule multi-select uses one dialog and writes every page", async () => {
    const posted: string[] = [];
    const onSchedule = vi.fn(async (row: { id?: string }) => {
      posted.push(String(row.id));
    });
    const confirm = vi.fn().mockReturnValue(true);
    const pickScheduleDates = vi.fn().mockResolvedValue({
      itemId: "42",
      startDate: "",
      endDate: "",
      comments: "",
    });
    vi.spyOn(global, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ ItemDates: { itemId: "42" } }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const folder = item({
      id: "1",
      name: "Sites",
      path: "/Sites",
      type: "folder",
      leaf: false,
    });
    const about = item({ id: "43", name: "About", path: "/Sites/Demo/About" });
    const result = await dispatchAction(action({ name: "Schedule" }), {
      item: item(),
      selectedItems: [item(), folder, about],
      onSchedule,
      pickScheduleDates,
      confirm,
    });
    expect(result.refresh).toBe(true);
    expect(result.messageText).toBeUndefined();
    expect(pickScheduleDates).toHaveBeenCalledTimes(1);
    expect(pickScheduleDates.mock.calls[0]?.[2]).toEqual({ applyCount: 2 });
    expect(confirm).toHaveBeenCalledWith(EXPLORER_MSG.CONFIRM_SCHEDULE_MULTI);
    expect(posted).toEqual(["42", "43"]);
    expect(global.fetch).toHaveBeenCalledTimes(1);
  });

  it("Schedule multi-select cancel writes nothing", async () => {
    const onSchedule = vi.fn();
    vi.spyOn(global, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ ItemDates: {} }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const result = await dispatchAction(action({ name: "Schedule" }), {
      item: item(),
      selectedItems: [item(), item({ id: "43", path: "/Sites/Demo/About" })],
      onSchedule,
      pickScheduleDates: async () => null,
      confirm: () => true,
    });
    expect(onSchedule).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
  });

  it("Schedule multi-select partial failure is not full success", async () => {
    const onSchedule = vi
      .fn()
      .mockResolvedValueOnce(undefined)
      .mockRejectedValueOnce(new Error("FORBIDDEN"));
    vi.spyOn(global, "fetch").mockResolvedValue(
      new Response(JSON.stringify({ ItemDates: {} }), {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const result = await dispatchAction(action({ name: "Schedule" }), {
      item: item(),
      selectedItems: [
        item({ id: "42", name: "Home" }),
        item({ id: "43", name: "About", path: "/Sites/Demo/About" }),
      ],
      onSchedule,
      pickScheduleDates: async () => ({
        itemId: "42",
        startDate: "",
        endDate: "",
        comments: "",
      }),
      confirm: () => true,
    });
    expect(onSchedule).toHaveBeenCalledTimes(2);
    expect(result.refresh).toBe(true);
    expect(result.messageKey).toBe(EXPLORER_MSG.SCHEDULE_PARTIAL);
    expect(result.messageText).toMatch(/About/);
    expect(result.messageText).toMatch(/FORBIDDEN/);
  });

  it("Schedule confirm cancel does not save", async () => {
    const onSchedule = vi.fn();
    vi.spyOn(global, "fetch").mockResolvedValue(
      new Response("{}", {
        status: 200,
        headers: { "Content-Type": "application/json" },
      }),
    );
    const result = await dispatchAction(action({ name: "Schedule" }), {
      item: item(),
      onSchedule,
      pickScheduleDates: async () => ({
        itemId: "42",
        startDate: "",
        endDate: "",
        comments: "",
      }),
      confirm: () => false,
    });
    expect(onSchedule).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
  });

  it("Schedule on a Sites folder asks for a content item", async () => {
    const onSchedule = vi.fn();
    const result = await dispatchAction(action({ name: "Schedule" }), {
      item: item({
        id: "1",
        name: "Sites",
        path: "/Sites",
        type: "folder",
        leaf: false,
      }),
      onSchedule,
      confirm: () => true,
    });
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_NEEDS_ITEM);
    expect(onSchedule).not.toHaveBeenCalled();
  });

  it("Schedule on a template stays unavailable", async () => {
    const result = await dispatchAction(action({ name: "Schedule" }), {
      item: item({
        path: "/Design/Templates/base",
        type: "percTemplate",
        category: "template",
        id: "77",
      }),
      confirm: () => true,
    });
    expect(result.kind).toBe("unavailable");
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_UNAVAILABLE);
  });

  it("classifies Publishing History as client", () => {
    expect(classifyAction(action({ name: "Publishing_History" }))).toBe(
      "client",
    );
  });

  it("Publishing History opens the history dialog", async () => {
    const onShowPublishingHistory = vi.fn();
    const result = await dispatchAction(
      action({ name: "Publishing_History" }),
      { item: item(), onShowPublishingHistory },
    );
    expect(result.kind).toBe("client");
    expect(onShowPublishingHistory).toHaveBeenCalled();
  });

  it("Publishing History on a Sites folder asks for a content item", async () => {
    const onShowPublishingHistory = vi.fn();
    const result = await dispatchAction(
      action({ name: "Publishing History" }),
      {
        item: item({
          path: "/Sites",
          type: "site",
          category: "SITE",
          id: "1",
        }),
        onShowPublishingHistory,
      },
    );
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_NEEDS_ITEM);
    expect(onShowPublishingHistory).not.toHaveBeenCalled();
  });

  it("Publishing History on a template stays unavailable", async () => {
    const onShowPublishingHistory = vi.fn();
    const result = await dispatchAction(
      action({ name: "pubhistory" }),
      {
        item: item({
          path: "/Design/Templates/base",
          type: "percTemplate",
          category: "template",
          id: "77",
        }),
        onShowPublishingHistory,
      },
    );
    expect(result.kind).toBe("unavailable");
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_UNAVAILABLE);
    expect(onShowPublishingHistory).not.toHaveBeenCalled();
  });

  it("dispatch workflow-transition runs the trigger", async () => {
    const runWorkflow = vi.fn().mockResolvedValue(undefined);
    const result = await dispatchAction(
      action({ name: "workflow-transition:Submit" }),
      { item: item(), runWorkflow },
    );
    expect(result.kind).toBe("workflow");
    expect(result.refresh).toBe(true);
    expect(runWorkflow).toHaveBeenCalledWith("42", "Submit", undefined);
  });

  it("comment-required workflow transition blocks a blank comment (#4723)", async () => {
    const runWorkflow = vi.fn();
    const result = await dispatchAction(
      action({ name: "workflow-transition:Reject", commentRequired: true }),
      { item: item(), runWorkflow, promptWorkflowComment: () => "  " },
    );
    expect(result.messageKey).toBe(EXPLORER_MSG.WORKFLOW_COMMENT_REQUIRED);
    expect(result.refresh).toBeUndefined();
    expect(runWorkflow).not.toHaveBeenCalled();
  });

  it("comment-required workflow transition sends the trimmed comment (#4723)", async () => {
    const runWorkflow = vi.fn().mockResolvedValue(undefined);
    const result = await dispatchAction(
      action({ name: "workflow-transition:Reject", commentRequired: true }),
      {
        item: item(),
        runWorkflow,
        promptWorkflowComment: () => " needs work ",
      },
    );
    expect(result.refresh).toBe(true);
    expect(runWorkflow).toHaveBeenCalledWith("42", "Reject", "needs work");
  });

  it("multi-select workflow transition confirms once and skips folders (#4833)", async () => {
    const runWorkflow = vi.fn().mockResolvedValue(undefined);
    const confirm = vi.fn().mockReturnValue(true);
    const page = item({ id: "42", name: "Home" });
    const asset = item({
      id: "44",
      name: "Logo",
      type: "percImageAsset",
      category: "asset",
    });
    const folder = item({
      id: "7",
      name: "News",
      type: "folder",
      category: "folder",
    });
    const result = await dispatchAction(
      action({ name: "workflow-transition:Submit" }),
      { item: page, selectedItems: [page, folder, asset], runWorkflow, confirm },
    );
    expect(confirm).toHaveBeenCalledTimes(1);
    expect(String(confirm.mock.calls[0]?.[0])).toMatch(/Apply Submit to 2 selected items/i);
    expect(runWorkflow).toHaveBeenCalledTimes(2);
    expect(runWorkflow).toHaveBeenNthCalledWith(1, "42", "Submit", undefined);
    expect(runWorkflow).toHaveBeenNthCalledWith(2, "44", "Submit", undefined);
    expect(result.refresh).toBe(true);
    expect(result.messageText).toMatch(/Folders are not transitioned: News/i);
  });

  it("multi-select workflow cancel transitions nothing (#4833)", async () => {
    const runWorkflow = vi.fn();
    const result = await dispatchAction(
      action({ name: "workflow-transition:Submit" }),
      {
        item: item(),
        selectedItems: [item({ id: "42" }), item({ id: "43", name: "About" })],
        runWorkflow,
        confirm: () => false,
      },
    );
    expect(runWorkflow).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
  });

  it("multi-select workflow names an HTTP failure and still records the other item (#4833)", async () => {
    const runWorkflow = vi.fn(async (id: string) => {
      if (id === "43") {
        throw Object.assign(new Error("denied"), { status: 403 });
      }
    });
    const result = await dispatchAction(
      action({ name: "workflow-transition:Submit" }),
      {
        item: item(),
        selectedItems: [
          item({ id: "42", name: "Home" }),
          item({ id: "43", name: "About" }),
        ],
        runWorkflow,
        confirm: () => true,
      },
    );
    expect(result.refresh).toBe(true);
    expect(result.messageKey).toBe(EXPLORER_MSG.WORKFLOW_BATCH_INCOMPLETE);
    expect(result.messageText).toMatch(/About \(HTTP 403\)/i);
    expect(result.messageText).toMatch(/Not every selected item was transitioned/i);
    expect(runWorkflow).toHaveBeenCalledTimes(2);
  });

  it("multi-select of only folders does not transition (#4833)", async () => {
    const runWorkflow = vi.fn();
    const result = await dispatchAction(
      action({ name: "workflow-transition:Submit" }),
      {
        item: item({ id: "7", type: "folder", category: "folder", name: "News" }),
        selectedItems: [
          item({ id: "7", type: "folder", category: "folder", name: "News" }),
          item({ id: "8", type: "folder", category: "folder", name: "Blog" }),
        ],
        runWorkflow,
        confirm: () => true,
      },
    );
    expect(runWorkflow).not.toHaveBeenCalled();
    expect(result.refresh).toBeUndefined();
    expect(result.messageText).toMatch(/Folders are not transitioned/i);
  });

  it("maps workflow transition HTTP 403 and 409 (#4723)", async () => {
    const forbidden = await dispatchAction(
      action({ name: "workflow-transition:Submit" }),
      {
        item: item(),
        runWorkflow: async () => {
          throw Object.assign(new Error("no"), { status: 403 });
        },
      },
    );
    expect(forbidden.messageKey).toBe(EXPLORER_MSG.WORKFLOW_TRANSITION_FORBIDDEN);
    expect(forbidden.refresh).toBeUndefined();

    const conflict = await dispatchAction(
      action({ name: "workflow-transition:Submit" }),
      {
        item: item(),
        runWorkflow: async () => {
          throw Object.assign(new Error("comment"), { status: 409 });
        },
      },
    );
    expect(conflict.messageKey).toBe(EXPLORER_MSG.WORKFLOW_TRANSITION_CONFLICT);
    expect(conflict.refresh).toBeUndefined();
  });

  it("slot add without AA slot context stays unavailable to invent", async () => {
    const addToSlot = vi.fn();
    const result = await dispatchAction(action({ name: "Slot_Add" }), {
      item: item(),
      addToSlot,
    });
    expect(result.kind).toBe("rest");
    expect(result.messageKey).toMatch(/Select a slot/i);
    expect(addToSlot).not.toHaveBeenCalled();
  });

  it("slot add picker cancel does not POST", async () => {
    const addToSlot = vi.fn();
    const result = await dispatchAction(action({ name: "Slot_Add" }), {
      item: item(),
      slot: { ownerId: 42, slotId: 3 },
      addToSlot,
      pickSlotDependent: async () => null,
    });
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBeUndefined();
    expect(result.messageKey).toBeUndefined();
    expect(addToSlot).not.toHaveBeenCalled();
  });

  it("slot add uses Content Browser pick + relationship REST", async () => {
    const addToSlot = vi.fn().mockResolvedValue({
      relationshipId: 9,
      ownerId: 42,
      dependentId: 7,
      slotId: 3,
      templateId: 4,
      sortRank: 0,
    });
    const result = await dispatchAction(action({ name: "Slot_Add" }), {
      item: item(),
      slot: { ownerId: 42, slotId: 3 },
      addToSlot,
      pickSlotDependent: async () => ({ contentId: 7, templateId: 4 }),
    });
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBe(true);
    expect(addToSlot).toHaveBeenCalledWith({
      ownerId: 42,
      dependentId: 7,
      slotId: 3,
      templateId: 4,
      folderId: undefined,
    });
  });

  it("arrange remove needs a relationship id from AA", async () => {
    const removeSlotRel = vi.fn();
    const missing = await dispatchAction(action({ name: "Arrange_Remove" }), {
      item: item(),
      slot: { ownerId: 42, slotId: 3 },
      removeSlotRel,
    });
    expect(missing.kind).toBe("rest");
    expect(missing.messageKey).toBe(EXPLORER_MSG.ACTION_NEEDS_RELATIONSHIP);
    expect(missing.messageKey).toMatch(/item in the slot/i);
    expect(removeSlotRel).not.toHaveBeenCalled();

    const ok = await dispatchAction(action({ name: "Arrange_Remove" }), {
      item: item(),
      slot: { ownerId: 42, slotId: 3, relationshipId: 88 },
      removeSlotRel,
    });
    expect(ok.refresh).toBe(true);
    expect(removeSlotRel).toHaveBeenCalledWith(88);
  });

  it("arrange without a slot stays needs-slot, not Data Flow unavailable", async () => {
    const removeSlotRel = vi.fn();
    const moveSlotRel = vi.fn();
    const changeSlotTemplate = vi.fn();
    const missing = await dispatchAction(action({ name: "Arrange_Remove" }), {
      item: item(),
      removeSlotRel,
      moveSlotRel,
      changeSlotTemplate,
    });
    expect(missing.kind).toBe("rest");
    expect(missing.messageKey).toBe(EXPLORER_MSG.ACTION_NEEDS_SLOT);
    expect(missing.messageKey).not.toBe(EXPLORER_MSG.ACTION_UNAVAILABLE);
    expect(removeSlotRel).not.toHaveBeenCalled();
    expect(moveSlotRel).not.toHaveBeenCalled();
    expect(changeSlotTemplate).not.toHaveBeenCalled();
  });

  it("arrange remove confirm cancel does not DELETE", async () => {
    const removeSlotRel = vi.fn();
    const result = await dispatchAction(action({ name: "Arrange_Remove" }), {
      item: item(),
      slot: { ownerId: 42, slotId: 3, relationshipId: 88 },
      removeSlotRel,
      confirm: () => false,
    });
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBeUndefined();
    expect(removeSlotRel).not.toHaveBeenCalled();
  });

  it("arrange move and change template-slot use relationship REST", async () => {
    const moveSlotRel = vi.fn().mockResolvedValue(undefined);
    const changeSlotTemplate = vi.fn().mockResolvedValue({
      relationshipId: 88,
      ownerId: 42,
      dependentId: 7,
      slotId: 5,
      templateId: 6,
      sortRank: 0,
    });
    await dispatchAction(action({ name: "Arrange_MoveUpLeft" }), {
      item: item(),
      slot: { ownerId: 42, slotId: 3, relationshipId: 88 },
      moveSlotRel,
    });
    expect(moveSlotRel).toHaveBeenCalledWith(88, "UP");
    await dispatchAction(action({ name: "Arrange_MoveDownRight" }), {
      item: item(),
      slot: { ownerId: 42, slotId: 3, relationshipId: 88 },
      moveSlotRel,
    });
    expect(moveSlotRel).toHaveBeenCalledWith(88, "DOWN");
    await dispatchAction(action({ name: "Change_Template" }), {
      item: item(),
      slot: { ownerId: 42, slotId: 3, relationshipId: 88 },
      changeSlotTemplate,
      pickSlotTemplateSlot: async () => ({ slotId: 5, templateId: 6 }),
    });
    expect(changeSlotTemplate).toHaveBeenCalledWith(88, 5, 6);
    await dispatchAction(action({ name: "Arrange_ChangeTemplateSlot" }), {
      item: item(),
      slot: { ownerId: 42, slotId: 3, relationshipId: 88 },
      changeSlotTemplate,
      pickSlotTemplateSlot: async () => ({ slotId: 5, templateId: 6 }),
    });
    expect(changeSlotTemplate).toHaveBeenCalledTimes(2);
    await dispatchAction(action({ name: "Move_To_Slot" }), {
      item: item(),
      slot: { ownerId: 42, slotId: 3, relationshipId: 88 },
      changeSlotTemplate,
      pickSlotTemplateSlot: async () => ({ slotId: 9, templateId: 4 }),
    });
    expect(changeSlotTemplate).toHaveBeenCalledWith(88, 9, 4);
  });

  it("change template without a pick stays needs-template", async () => {
    const changeSlotTemplate = vi.fn();
    const result = await dispatchAction(action({ name: "Change_Template" }), {
      item: item(),
      slot: { ownerId: 42, slotId: 3, relationshipId: 88 },
      changeSlotTemplate,
    });
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_NEEDS_TEMPLATE);
    expect(changeSlotTemplate).not.toHaveBeenCalled();
  });

  it("slot create picker cancel does not create or POST add", async () => {
    const addToSlot = vi.fn();
    const createItem = vi.fn();
    const openWindow = vi.fn();
    const result = await dispatchAction(action({ name: "Slot_Create" }), {
      item: item(),
      slot: { ownerId: 42, slotId: 3 },
      addToSlot,
      createItem,
      openWindow,
      pickSlotCreate: async () => null,
    });
    expect(result.kind).toBe("rest");
    expect(result.refresh).toBeUndefined();
    expect(result.messageKey).toBeUndefined();
    expect(createItem).not.toHaveBeenCalled();
    expect(addToSlot).not.toHaveBeenCalled();
    expect(openWindow).not.toHaveBeenCalled();
  });

  it("slot create opens the React editor after relationship add", async () => {
    const addToSlot = vi.fn().mockResolvedValue({
      relationshipId: 1,
      ownerId: 42,
      dependentId: 99,
      slotId: 3,
      templateId: 4,
      sortRank: 0,
    });
    const createItem = vi.fn().mockResolvedValue({
      itemId: "99",
      folderPath: "/Sites/A",
      name: "n",
      contentType: "rffEvent",
    });
    const openWindow = vi.fn();
    const result = await dispatchAction(action({ name: "Slot_Create" }), {
      item: item(),
      slot: { ownerId: 42, slotId: 3 },
      addToSlot,
      createItem,
      openWindow,
      pickSlotCreate: async () => ({
        contentType: "rffEvent",
        folderPath: "/Sites/A",
        snippetTemplateId: 4,
      }),
    });
    expect(result.refresh).toBe(true);
    expect(createItem).toHaveBeenCalledWith({
      contentType: "rffEvent",
      folderPath: "/Sites/A",
      templateId: undefined,
    });
    expect(addToSlot).toHaveBeenCalledWith({
      ownerId: 42,
      dependentId: 99,
      slotId: 3,
      templateId: 4,
    });
    expect(String(openWindow.mock.calls[0]?.[0] ?? "")).toContain("entry=editor");
    expect(String(openWindow.mock.calls[0]?.[0] ?? "")).not.toMatch(
      /editAsset\.jsp|itemassembly\.html/i,
    );
  });
});
