/**
 * Explorer Rename Site helpers (#4764 / parent #4530).
 */
"use strict";

const {
  TEST_IDS: COPY_IDS,
  explorerSpaUrl,
  explorerSpaUrlWithPath,
  openContentMenu,
} = require("./explorer-site-copy");

const TEST_IDS = Object.freeze({
  ...COPY_IDS,
  siteRenameMenu: "explorer-content-site-rename",
  siteRenamePanel: "explorer-site-rename-panel",
  siteRenameName: "site-rename-name",
  siteRenameSubmit: "site-rename-submit",
  siteRenameCancel: "site-rename-cancel",
  siteRenameError: "site-rename-error",
});

module.exports = {
  TEST_IDS,
  explorerSpaUrl,
  explorerSpaUrlWithPath,
  openContentMenu,
};
