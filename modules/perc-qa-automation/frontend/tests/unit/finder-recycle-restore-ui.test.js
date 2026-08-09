/**
 * Unit tests for classic Finder recycle/restore pure helpers (no live CMS).
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
  classicFinderDashboardUrl,
  isFinderControlEnabled,
  normalizeFinderPathInput,
  finderPathSegments,
  isRestoreEligiblePath,
  isStillOnLoginPage,
  loginContextDownFailureMessage,
  deleteFolderApiPathFragment,
  restoreFolderApiPathFragment,
  emptyRecyclingApiPathFragment,
  recycledFolderFinderPath,
  exactFinderItemNameMatcher,
  cssAttrEscape,
  finderListingIdSelector,
  folderFinderPath,
  isDeleteEligiblePath,
  pathBarReflectsFolderName,
  finderRecycleSelectStrategies,
  shouldUseRestRecycleFallback,
  millerSelectionFailureMessage,
  chooseRestoreOrEmptyBranch,
  PATH_RESTORE_FOLDER,
  PATH_DELETE_FOLDER,
} = require("../helpers/finder-recycle-restore-ui");

describe("finder-recycle-restore-ui helpers", () => {
  it("exposes stable classic Finder selectors and surface tags", () => {
    assert.equal(SELECTORS.deleteButton, "#perc-finder-delete");
    assert.equal(SELECTORS.restoreItem, "#perc-finder-restore-item");
    assert.equal(
      SELECTORS.emptyAction,
      '[data-testid="perc-finder-empty-recycling"]',
    );
    assert.equal(SELECTORS.actionsButton, "#perc-finder-actions-button");
    assert.equal(SELECTORS.pathSummary, "#mcol-path-summary");
    assert.equal(SELECTORS.millerListing, ".mcol-listing");
    assert.equal(SELECTORS.listViewRow, ".perc-datatable-row");
    assert.equal(SELECTORS.chooseColumnView, "#perc-finder-choose-columnview");
    assert.ok(SURFACE_TAGS.includes("finder-recycle-restore"));
    assert.ok(SURFACE_TAGS.includes("folder-recycle"));
    assert.ok(SURFACE_TAGS.includes("smoke"));
  });

  it("classicFinderDashboardUrl joins base and cache-busts", () => {
    assert.equal(
      classicFinderDashboardUrl("http://127.0.0.1:9993/", 12345),
      "http://127.0.0.1:9993/Rhythmyx/cm/app/dashboard.jsp?_=12345",
    );
    assert.equal(
      classicFinderDashboardUrl("http://localhost:9992", 1),
      "http://localhost:9992/Rhythmyx/cm/app/dashboard.jsp?_=1",
    );
    const live = classicFinderDashboardUrl("http://127.0.0.1:9993");
    assert.match(live, /\/Rhythmyx\/cm\/app\/dashboard\.jsp\?_=\d+$/);
  });

  it("isFinderControlEnabled mirrors ui-enabled / ui-disabled rules", () => {
    assert.equal(isFinderControlEnabled(null), false);
    assert.equal(isFinderControlEnabled(""), false);
    assert.equal(isFinderControlEnabled("ui-disabled"), false);
    assert.equal(isFinderControlEnabled("ui-enabled"), true);
    assert.equal(isFinderControlEnabled("perc-font-icon ui-enabled"), true);
    // No explicit disabled → treat as enabled when other classes present.
    assert.equal(isFinderControlEnabled("perc-font-icon"), true);
  });

  it("normalizeFinderPathInput and finderPathSegments are portable", () => {
    assert.equal(normalizeFinderPathInput(""), "/");
    assert.equal(normalizeFinderPathInput("/"), "/");
    assert.equal(normalizeFinderPathInput("Assets"), "/Assets");
    assert.equal(normalizeFinderPathInput("/Assets/"), "/Assets");
    assert.equal(
      normalizeFinderPathInput("/Recycling//Assets/foo/"),
      "/Recycling/Assets/foo",
    );
    assert.deepEqual(finderPathSegments("/Recycling/Assets/foo"), [
      "",
      "Recycling",
      "Assets",
      "foo",
    ]);
    assert.deepEqual(finderPathSegments("/"), [""]);
  });

  it("isRestoreEligiblePath matches perc_restore_button depth rule", () => {
    assert.equal(isRestoreEligiblePath("/Recycling"), false);
    assert.equal(isRestoreEligiblePath("/Recycling/Assets"), false);
    assert.equal(isRestoreEligiblePath("/Recycling/Assets/seed"), true);
    assert.equal(isRestoreEligiblePath("/Assets/seed"), false);
    assert.equal(
      isRestoreEligiblePath(["", "Recycling", "Assets", "seed"]),
      true,
    );
    assert.equal(isRestoreEligiblePath(["", "Recycling", "Assets"]), false);
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
      isStillOnLoginPage("http://127.0.0.1:9993/Rhythmyx/cm/app/dashboard.jsp"),
      false,
    );
    assert.equal(isStillOnLoginPage(""), false);
  });

  it("loginContextDownFailureMessage cites #2489/#2423 hard fail", () => {
    const msg = loginContextDownFailureMessage({
      url: "http://127.0.0.1:9993/Rhythmyx/login",
      baseUrl: "http://127.0.0.1:9993",
    });
    assert.match(msg, /#2489/);
    assert.match(msg, /#2423/);
    assert.match(msg, /hard fail/i);
    assert.match(msg, /login/i);
  });

  it("API path fragments align with pathmanagement endpoints", () => {
    assert.match(deleteFolderApiPathFragment(), /deleteFolder$/);
    assert.match(restoreFolderApiPathFragment(), /restoreFolder$/);
    assert.match(emptyRecyclingApiPathFragment(), /recycle\/empty$/);
    assert.match(PATH_DELETE_FOLDER, /deleteFolder$/);
    assert.match(PATH_RESTORE_FOLDER, /restoreFolder$/);
  });

  it("recycledFolderFinderPath builds structural Recycling paths", () => {
    assert.equal(
      recycledFolderFinderPath("seed-a"),
      "/Recycling/Assets/seed-a",
    );
    assert.equal(
      recycledFolderFinderPath("seed-a", "Sites"),
      "/Recycling/Sites/seed-a",
    );
    assert.equal(recycledFolderFinderPath("seed-a", ""), "/Recycling/seed-a");
    assert.equal(recycledFolderFinderPath(""), "/Recycling/Assets");
  });

  it("exactFinderItemNameMatcher is exact trim match only", () => {
    const match = exactFinderItemNameMatcher("qa-folder-1");
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

  it("cssAttrEscape and finderListingIdSelector are safe", () => {
    assert.equal(cssAttrEscape('a"b'), 'a\\"b');
    assert.equal(cssAttrEscape("a\\b"), "a\\\\b");
    assert.equal(finderListingIdSelector(""), "");
    assert.equal(
      finderListingIdSelector("0-101-456"),
      "#perc-finder-listing-0-101-456",
    );
    assert.equal(
      finderListingIdSelector('x"y'),
      '[id="perc-finder-listing-x\\"y"]',
    );
  });

  it("folderFinderPath and isDeleteEligiblePath match product depth rule", () => {
    assert.equal(folderFinderPath("seed-a"), "/Assets/seed-a");
    assert.equal(folderFinderPath("seed-a", "Sites"), "/Sites/seed-a");
    assert.equal(folderFinderPath("", "Assets"), "/Assets");
    assert.equal(isDeleteEligiblePath("/Assets"), false);
    assert.equal(isDeleteEligiblePath("/Assets/seed-a"), true);
    assert.equal(isDeleteEligiblePath(["", "Assets", "seed-a"]), true);
    assert.equal(isDeleteEligiblePath("/"), false);
  });

  it("pathBarReflectsFolderName matches path leaf only", () => {
    assert.equal(pathBarReflectsFolderName("/Assets/seed-a", "seed-a"), true);
    assert.equal(pathBarReflectsFolderName("/Assets/seed-a/", "seed-a"), true);
    assert.equal(pathBarReflectsFolderName("/Assets", "seed-a"), false);
    assert.equal(pathBarReflectsFolderName("/Assets/seed-ab", "seed-a"), false);
    assert.equal(pathBarReflectsFolderName("", "seed-a"), false);
  });

  it("finderRecycleSelectStrategies orders path-bar then miller then list", () => {
    const withGuid = finderRecycleSelectStrategies({
      name: "qa-folder-1",
      parentPath: "Assets",
      guid: "0-101-99",
    });
    assert.equal(withGuid[0].kind, "path-bar");
    assert.equal(withGuid[0].path, "/Assets/qa-folder-1");
    assert.equal(withGuid[1].kind, "listing-id");
    assert.equal(withGuid[1].selector, "#perc-finder-listing-0-101-99");
    assert.equal(withGuid[2].kind, "miller-title");
    assert.match(withGuid[2].selector, /mcol-listing\[title=/);
    assert.equal(withGuid[3].kind, "miller-name");
    assert.equal(withGuid[4].kind, "list-row");

    const noGuid = finderRecycleSelectStrategies({ name: "only-name" });
    assert.equal(noGuid[0].kind, "path-bar");
    assert.ok(!noGuid.some((s) => s.kind === "listing-id"));
    assert.equal(finderRecycleSelectStrategies({}).length, 0);
  });

  it("shouldUseRestRecycleFallback is false on UI happy path", () => {
    assert.equal(
      shouldUseRestRecycleFallback({
        selected: true,
        deleteEnabled: true,
        recycledViaUi: true,
      }),
      false,
    );
    assert.equal(
      shouldUseRestRecycleFallback({
        selected: true,
        deleteEnabled: true,
        recycledViaUi: false,
      }),
      false,
    );
    assert.equal(
      shouldUseRestRecycleFallback({
        selected: false,
        deleteEnabled: false,
      }),
      true,
    );
    assert.equal(
      shouldUseRestRecycleFallback({
        selected: true,
        deleteEnabled: false,
      }),
      true,
    );
  });

  it("millerSelectionFailureMessage cites #2541 and residual chrome gaps", () => {
    const msg = millerSelectionFailureMessage({
      name: "seed",
      strategiesTried: ["path-bar", "miller-title"],
      pathBar: "/Assets",
    });
    assert.match(msg, /#2541/);
    assert.match(msg, /data-testid/i);
    assert.match(msg, /seed/);
    assert.match(msg, /path-bar/);
  });
});
