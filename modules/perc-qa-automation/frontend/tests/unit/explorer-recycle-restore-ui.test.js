/**
 * Unit tests for modern Content Explorer recycle/restore pure helpers (no live CMS).
 *
 * Run from modules/perc-qa-automation/frontend:
 *   npm run test:unit
 */

"use strict";

const { describe, it } = require("node:test");
const assert = require("node:assert/strict");
const {
  SELECTORS,
  SURFACE_TAGS,
  modernExplorerUrl,
  normalizeExplorerPath,
  treeNodeSelectors,
  isActionControlEnabled,
  isStillOnLoginPage,
  loginContextDownFailureMessage,
  deleteItemApiPathFragment,
  deleteFolderApiPathFragment,
  restoreFolderApiPathFragment,
  emptyRecyclingApiPathFragment,
  recycledFolderExplorerPath,
  exactExplorerItemNameMatcher,
  chooseRestoreOrEmptyBranch,
  isRestoreEligibleExplorerPath,
  isRestoreActionName,
  isEmptyRecyclingActionName,
  actionToolbarItemSelector,
  contextMenuItemSelector,
  PATH_RESTORE_FOLDER,
  PATH_DELETE_FOLDER,
} = require("../helpers/explorer-recycle-restore-ui");

describe("explorer-recycle-restore-ui helpers", () => {
  it("exposes stable modern explorer selectors and surface tags", () => {
    assert.equal(SELECTORS.shell, '[data-testid="content-explorer-shell"]');
    assert.equal(SELECTORS.explorerTree, '[data-testid="explorer-tree"]');
    assert.equal(SELECTORS.detailList, '[data-testid="detail-list"]');
    assert.equal(SELECTORS.reducedActions, '[data-testid="reduced-actions"]');
    assert.equal(SELECTORS.actionDelete, '[data-testid="action-delete"]');
    assert.equal(SELECTORS.classicWebManagement, "#perc-web-management");
    assert.ok(SURFACE_TAGS.includes("explorer-recycle-restore"));
    assert.ok(SURFACE_TAGS.includes("folder-recycle"));
    assert.ok(SURFACE_TAGS.includes("smoke"));
  });

  it("modernExplorerUrl joins base and cache-busts", () => {
    assert.equal(
      modernExplorerUrl("http://127.0.0.1:9993/", 12345),
      "http://127.0.0.1:9993/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=12345",
    );
    assert.equal(
      modernExplorerUrl("http://localhost:9992", 1),
      "http://localhost:9992/Rhythmyx/cm/app/spa.jsp?entry=explorer&_=1",
    );
    const live = modernExplorerUrl("http://127.0.0.1:9993");
    assert.match(live, /\/Rhythmyx\/cm\/app\/spa\.jsp\?entry=explorer&_=\d+$/);
  });

  it("normalizeExplorerPath is portable (no OS separators)", () => {
    assert.equal(normalizeExplorerPath(""), "/");
    assert.equal(normalizeExplorerPath("/"), "/");
    assert.equal(normalizeExplorerPath("Assets"), "/Assets");
    assert.equal(normalizeExplorerPath("/Assets/"), "/Assets");
    assert.equal(
      normalizeExplorerPath("/Recycling//Assets/foo/"),
      "/Recycling/Assets/foo",
    );
  });

  it("treeNodeSelectors cover trailing-slash variants", () => {
    const assets = treeNodeSelectors("Assets");
    assert.ok(assets.includes('[data-testid="tree-node-/Assets/"]'));
    assert.ok(assets.includes('[data-testid="tree-node-/Assets"]'));
    const recycling = treeNodeSelectors("/Recycling/Assets/seed");
    assert.ok(
      recycling.some((s) => s.includes("tree-node-/Recycling/Assets/seed")),
    );
  });

  it("isActionControlEnabled respects disabled / aria-disabled", () => {
    assert.equal(isActionControlEnabled({}), true);
    assert.equal(isActionControlEnabled({ disabled: true }), false);
    assert.equal(isActionControlEnabled({ disabled: false }), true);
    assert.equal(isActionControlEnabled({ ariaDisabled: "true" }), false);
    assert.equal(isActionControlEnabled({ ariaDisabled: "false" }), true);
  });

  it("isStillOnLoginPage detects login hard-fail URLs", () => {
    assert.equal(
      isStillOnLoginPage("http://127.0.0.1:9993/Rhythmyx/login"),
      true,
    );
    assert.equal(
      isStillOnLoginPage("http://127.0.0.1:9993/Rhythmyx/login?error=1"),
      true,
    );
    assert.equal(
      isStillOnLoginPage(
        "http://127.0.0.1:9993/Rhythmyx/cm/app/spa.jsp?entry=explorer",
      ),
      false,
    );
    assert.equal(isStillOnLoginPage(""), false);
  });

  it("loginContextDownFailureMessage cites #2542/#2423 hard fail", () => {
    const msg = loginContextDownFailureMessage({
      url: "http://127.0.0.1:9993/Rhythmyx/login",
      baseUrl: "http://127.0.0.1:9993",
    });
    assert.match(msg, /#2542/);
    assert.match(msg, /#2423/);
    assert.match(msg, /hard fail/i);
    assert.match(msg, /login/i);
    assert.match(msg, /Content Explorer/i);
  });

  it("API path fragments align with pathmanagement endpoints", () => {
    assert.match(deleteItemApiPathFragment(), /path\/delete$/);
    assert.match(deleteFolderApiPathFragment(), /deleteFolder$/);
    assert.match(restoreFolderApiPathFragment(), /restoreFolder$/);
    assert.match(emptyRecyclingApiPathFragment(), /recycle\/empty$/);
    assert.match(PATH_DELETE_FOLDER, /deleteFolder$/);
    assert.match(PATH_RESTORE_FOLDER, /restoreFolder$/);
  });

  it("recycledFolderExplorerPath builds structural Recycling paths", () => {
    assert.equal(
      recycledFolderExplorerPath("seed-a"),
      "/Recycling/Assets/seed-a",
    );
    assert.equal(
      recycledFolderExplorerPath("seed-a", "Sites"),
      "/Recycling/Sites/seed-a",
    );
    assert.equal(
      recycledFolderExplorerPath("seed-a", ""),
      "/Recycling/seed-a",
    );
    assert.equal(recycledFolderExplorerPath(""), "/Recycling/Assets");
  });

  it("exactExplorerItemNameMatcher is exact trim match only", () => {
    const match = exactExplorerItemNameMatcher("qa-folder-1");
    assert.equal(match("qa-folder-1"), true);
    assert.equal(match("  qa-folder-1  "), true);
    assert.equal(match("qa-folder-10"), false);
    assert.equal(match(""), false);
  });

  it("chooseRestoreOrEmptyBranch prefers restore only when eligible+enabled", () => {
    assert.equal(
      chooseRestoreOrEmptyBranch({ pathEligible: true, restoreEnabled: true }),
      "restore",
    );
    assert.equal(
      chooseRestoreOrEmptyBranch({ pathEligible: true, restoreEnabled: false }),
      "empty",
    );
    assert.equal(
      chooseRestoreOrEmptyBranch({ pathEligible: false, restoreEnabled: true }),
      "empty",
    );
    assert.equal(chooseRestoreOrEmptyBranch({}), "empty");
  });

  it("isRestoreEligibleExplorerPath matches Recycling depth rule", () => {
    assert.equal(isRestoreEligibleExplorerPath("/Recycling"), false);
    assert.equal(isRestoreEligibleExplorerPath("/Recycling/Assets"), false);
    assert.equal(
      isRestoreEligibleExplorerPath("/Recycling/Assets/seed"),
      true,
    );
    assert.equal(isRestoreEligibleExplorerPath("/Assets/seed"), false);
  });

  it("restore / empty action name matchers", () => {
    assert.equal(isRestoreActionName("Restore"), true);
    assert.equal(isRestoreActionName("restore-item"), true);
    assert.equal(isRestoreActionName("Empty Recycling"), false);
    assert.equal(isEmptyRecyclingActionName("Empty Recycling"), true);
    assert.equal(isEmptyRecyclingActionName("empty-recycling"), true);
    assert.equal(isEmptyRecyclingActionName("Restore"), false);
  });

  it("action toolbar / context menu selectors use data-testid", () => {
    assert.equal(
      actionToolbarItemSelector("Restore"),
      '[data-testid="action-toolbar-item-Restore"]',
    );
    assert.equal(
      contextMenuItemSelector("delete"),
      '[data-testid="context-menu-item-delete"]',
    );
  });
});
