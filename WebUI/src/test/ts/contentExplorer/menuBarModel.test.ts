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

import { describe, expect, it } from "vitest";
import {
  buildExplorerMenuBarGroups,
  explorerMenuBarGroupIds,
} from "../../../main/ts/contentExplorer/menuBarModel";
import { EXPLORER_MSG } from "../../../main/ts/contentExplorer/messages";

describe("buildExplorerMenuBarGroups (#2731 DCE ContentExplorerMenu.xml)", () => {
  it("exposes Content / View / Help in DCE order", () => {
    expect(explorerMenuBarGroupIds()).toEqual(["content", "view", "help"]);
  });

  it("uses perc.ui.explorer@ i18n keys for group and item labels", () => {
    const groups = buildExplorerMenuBarGroups();
    for (const g of groups) {
      expect(g.labelKey.startsWith("perc.ui.explorer@")).toBe(true);
      for (const item of g.items) {
        expect(item.labelKey.startsWith("perc.ui.explorer@")).toBe(true);
      }
    }
    expect(groups[0]?.labelKey).toBe(EXPLORER_MSG.MENU_CONTENT);
    expect(groups[1]?.labelKey).toBe(EXPLORER_MSG.MENU_VIEW);
    expect(groups[2]?.labelKey).toBe(EXPLORER_MSG.MENU_HELP);
  });

  it("wires View toggles to legacy explorer-toggle-* test ids", () => {
    const view = buildExplorerMenuBarGroups().find((g) => g.id === "view");
    expect(view).toBeTruthy();
    const byId = Object.fromEntries(
      (view?.items ?? []).map((i) => [i.id, i.testId]),
    );
    expect(byId["view-search"]).toBe("explorer-toggle-search");
    expect(byId["view-security"]).toBe("explorer-toggle-security");
    expect(byId["view-item-properties"]).toBe("explorer-toggle-item-properties");
    expect(byId["view-translations"]).toBe("explorer-toggle-translations");
    expect(byId["view-relationships"]).toBe("explorer-toggle-relationships");
    expect(byId["view-dependencies"]).toBe("explorer-toggle-dependencies");
    expect(byId["view-clipboard"]).toBe("explorer-toggle-clipboard");
  });

  it("View → Clipboard is an always-enabled toggle (#3544)", () => {
    const view = buildExplorerMenuBarGroups().find((g) => g.id === "view");
    const clipboard = view?.items.find((i) => i.id === "view-clipboard");
    expect(clipboard?.toggle).toBe(true);
    expect(clipboard?.disabledWhen).toBeUndefined();
    expect(clipboard?.testId).toBe("explorer-toggle-clipboard");
  });

  it("puts clipboard-add under Content with stable test id", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const add = content?.items.find((i) => i.id === "content-clipboard-add");
    expect(add?.testId).toBe("explorer-clipboard-add");
  });

  it("puts multi-select copy under Content, disabled without a selection (#4855)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const copy = content?.items.find((i) => i.id === "content-multi-copy");
    expect(copy?.testId).toBe("explorer-multi-copy");
    expect(copy?.disabledWhen).toBe("noSelection");
    expect(copy?.labelKey).toBe(EXPLORER_MSG.MULTI_COPY);
  });

  it("puts multi-select move under Content, disabled without a selection (#4856)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const move = content?.items.find((i) => i.id === "content-multi-move");
    expect(move?.testId).toBe("explorer-multi-move");
    expect(move?.disabledWhen).toBe("noSelection");
    expect(move?.labelKey).toBe(EXPLORER_MSG.MULTI_MOVE);
  });

  it("puts multi-select recycle under Content, disabled without a selection (#4857)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const recycle = content?.items.find((i) => i.id === "content-multi-recycle");
    expect(recycle?.testId).toBe("explorer-multi-recycle");
    expect(recycle?.disabledWhen).toBe("noSelection");
    expect(recycle?.labelKey).toBe(EXPLORER_MSG.MULTI_RECYCLE);
  });

  it("puts multi-select purge under Content, disabled without a selection (#4883)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const purge = content?.items.find((i) => i.id === "content-multi-purge");
    expect(purge?.testId).toBe("explorer-multi-purge");
    expect(purge?.disabledWhen).toBe("noSelection");
    expect(purge?.labelKey).toBe(EXPLORER_MSG.MULTI_PURGE);
  });

  it("puts multi-select restore under Content, disabled without a selection (#4884)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const restore = content?.items.find((i) => i.id === "content-multi-restore");
    expect(restore?.testId).toBe("explorer-multi-restore");
    expect(restore?.disabledWhen).toBe("noSelection");
    expect(restore?.labelKey).toBe(EXPLORER_MSG.MULTI_RESTORE);
  });

  it("Content → Search is a toggle sharing the Search panel (#2850)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const search = content?.items.find((i) => i.id === "content-search");
    expect(search?.testId).toBe("explorer-menu-content-search");
    expect(search?.toggle).toBe(true);
    expect(search?.ariaLabelKey).toBe(EXPLORER_MSG.TOGGLE_SEARCH_ARIA);
  });

  it("puts Copy folder path under Content (#4911)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const copyPath = content?.items.find((i) => i.id === "content-copy-folder-path");
    expect(copyPath?.testId).toBe("explorer-copy-folder-path");
    expect(copyPath?.disabledWhen).toBeUndefined();
    expect(copyPath?.labelKey).toBe(EXPLORER_MSG.COPY_FOLDER_PATH);
  });

  it("puts Set folder workflow under Content (#5104)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const setFolder = content?.items.find((i) => i.id === "content-set-folder-workflow");
    expect(setFolder?.testId).toBe("explorer-set-folder-workflow");
    expect(setFolder?.disabledWhen).toBeUndefined();
    expect(setFolder?.labelKey).toBe(EXPLORER_MSG.SET_FOLDER_WORKFLOW);
    expect(setFolder?.ariaLabelKey).toBe(EXPLORER_MSG.SET_FOLDER_WORKFLOW_ARIA);
  });

  it("puts Set folder community under Content (#5105)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const setFolder = content?.items.find((i) => i.id === "content-set-folder-community");
    expect(setFolder?.testId).toBe("explorer-set-folder-community");
    expect(setFolder?.disabledWhen).toBeUndefined();
    expect(setFolder?.labelKey).toBe(EXPLORER_MSG.SET_FOLDER_COMMUNITY);
    expect(setFolder?.ariaLabelKey).toBe(EXPLORER_MSG.SET_FOLDER_COMMUNITY_ARIA);
  });

  it("puts Set folder locale under Content (#5106)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const setFolder = content?.items.find((i) => i.id === "content-set-folder-locale");
    expect(setFolder?.testId).toBe("explorer-set-folder-locale");
    expect(setFolder?.disabledWhen).toBeUndefined();
    expect(setFolder?.labelKey).toBe(EXPLORER_MSG.SET_FOLDER_LOCALE);
    expect(setFolder?.ariaLabelKey).toBe(EXPLORER_MSG.SET_FOLDER_LOCALE_ARIA);
  });

  it("puts Set folder display format under Content (#5131)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const setFolder = content?.items.find((i) => i.id === "content-set-folder-display-format");
    expect(setFolder?.testId).toBe("explorer-set-folder-display-format");
    expect(setFolder?.disabledWhen).toBeUndefined();
    expect(setFolder?.labelKey).toBe(EXPLORER_MSG.SET_FOLDER_DISPLAY_FORMAT);
    expect(setFolder?.ariaLabelKey).toBe(EXPLORER_MSG.SET_FOLDER_DISPLAY_FORMAT_ARIA);
  });

  it("puts Set allowed publish sites under Content (#5132)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const setFolder = content?.items.find((i) => i.id === "content-set-folder-allowed-sites");
    expect(setFolder?.testId).toBe("explorer-set-folder-allowed-sites");
    expect(setFolder?.disabledWhen).toBeUndefined();
    expect(setFolder?.labelKey).toBe(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES);
    expect(setFolder?.ariaLabelKey).toBe(EXPLORER_MSG.SET_FOLDER_ALLOWED_SITES_ARIA);
  });

  it("puts Set community under Content (#5077)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const setCommunity = content?.items.find((i) => i.id === "content-set-community");
    expect(setCommunity?.testId).toBe("explorer-set-community");
    expect(setCommunity?.disabledWhen).toBeUndefined();
    expect(setCommunity?.labelKey).toBe(EXPLORER_MSG.SET_COMMUNITY);
  });

  it("puts Change page template under Content (#5200)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const changeTemplate = content?.items.find(
      (i) => i.id === "content-change-page-template",
    );
    expect(changeTemplate?.testId).toBe("explorer-change-page-template");
    expect(changeTemplate?.disabledWhen).toBeUndefined();
    expect(changeTemplate?.labelKey).toBe(EXPLORER_MSG.CHANGE_PAGE_TEMPLATE);
    expect(changeTemplate?.ariaLabelKey).toBe(EXPLORER_MSG.CHANGE_PAGE_TEMPLATE_ARIA);
  });

  it("puts Mobile preview under Content (#5078)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const mobile = content?.items.find((i) => i.id === "content-mobile-preview");
    expect(mobile?.testId).toBe("explorer-mobile-preview");
    expect(mobile?.disabledWhen).toBeUndefined();
    expect(mobile?.labelKey).toBe(EXPLORER_MSG.MOBILE_PREVIEW);
    expect(mobile?.ariaLabelKey).toBe(EXPLORER_MSG.MOBILE_PREVIEW_ARIA);
  });

  it("puts Copy item id under Content (#4989)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const copyGuid = content?.items.find((i) => i.id === "content-copy-item-guid");
    expect(copyGuid?.testId).toBe("explorer-copy-item-guid");
    expect(copyGuid?.disabledWhen).toBeUndefined();
    expect(copyGuid?.labelKey).toBe(EXPLORER_MSG.COPY_ITEM_GUID);
  });

  it("puts Create Site under Content always enabled (#3002)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const create = content?.items.find((i) => i.id === "content-create-site");
    expect(create?.testId).toBe("explorer-content-create-site");
    expect(create?.disabledWhen).toBeUndefined();
    expect(create?.toggle).toBe(true);
    expect(create?.labelKey).toBe(EXPLORER_MSG.SITE_CREATE_TITLE);
  });

  it("puts Site Copy under Content with site-context disable (#2767)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const siteCopy = content?.items.find((i) => i.id === "content-site-copy");
    expect(siteCopy?.testId).toBe("explorer-content-site-copy");
    expect(siteCopy?.disabledWhen).toBe("noSiteContext");
    expect(siteCopy?.toggle).toBe(true);
    expect(siteCopy?.labelKey).toBe(EXPLORER_MSG.SITE_COPY_TITLE);
  });

  it("puts Subfolder Copy under Content with folder-context disable (#2792)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const sub = content?.items.find((i) => i.id === "content-subfolder-copy");
    expect(sub?.testId).toBe("explorer-content-subfolder-copy");
    expect(sub?.disabledWhen).toBe("noFolderContext");
    expect(sub?.toggle).toBe(true);
    expect(sub?.labelKey).toBe(EXPLORER_MSG.SUBFOLDER_COPY_TITLE);
  });
});
