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
    expect(byId["view-translations"]).toBe("explorer-toggle-translations");
    expect(byId["view-relationships"]).toBe("explorer-toggle-relationships");
    expect(byId["view-dependencies"]).toBe("explorer-toggle-dependencies");
    expect(byId["view-clipboard"]).toBe("explorer-toggle-clipboard");
  });

  it("puts clipboard-add under Content with stable test id", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const add = content?.items.find((i) => i.id === "content-clipboard-add");
    expect(add?.testId).toBe("explorer-clipboard-add");
  });

  it("puts Site Copy under Content with site-context disable (#2767)", () => {
    const content = buildExplorerMenuBarGroups().find((g) => g.id === "content");
    const siteCopy = content?.items.find((i) => i.id === "content-site-copy");
    expect(siteCopy?.testId).toBe("explorer-content-site-copy");
    expect(siteCopy?.disabledWhen).toBe("noSiteContext");
    expect(siteCopy?.toggle).toBe(true);
    expect(siteCopy?.labelKey).toBe(EXPLORER_MSG.SITE_COPY_TITLE);
  });
});
