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

  it("New Item parent without a type asks to choose a type", async () => {
    const createItem = vi.fn();
    const result = await dispatchAction(action({ name: "Create_New_Item" }), {
      item: item({ type: "folder", path: "/Sites/Demo" }),
      folderPath: "/Sites/Demo",
      createItem,
    });
    expect(result.messageKey).toBe(EXPLORER_MSG.ACTION_NEEDS_TYPE);
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
    await dispatchAction(action({ name: "Arrange_ChangeTemplateSlot" }), {
      item: item(),
      slot: { ownerId: 42, slotId: 3, relationshipId: 88 },
      changeSlotTemplate,
      pickSlotTemplateSlot: async () => ({ slotId: 5, templateId: 6 }),
    });
    expect(changeSlotTemplate).toHaveBeenCalledWith(88, 5, 6);
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
    expect(createItem).toHaveBeenCalled();
    expect(addToSlot).toHaveBeenCalled();
    expect(String(openWindow.mock.calls[0]?.[0] ?? "")).toContain("entry=editor");
  });
});
