/**
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

/**
 * Explorer folder security ACL save (#4672 / parent #4530).
 *
 * @see tests/explorer-folder-acl-save.spec.js
 */

"use strict";

const { explorerSpaUrl } = require("./explorer-sites-list-create");
const {
  TEST_IDS,
  assetsFolderUrl,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
} = require("./explorer-sites-assets-tree-list");

const ACL_TEST_IDS = Object.freeze({
  ...TEST_IDS,
  toggleSecurity: "explorer-toggle-security",
  securityPanel: "folder-security-panel",
  securityHint: "explorer-security-hint",
  securityError: "folder-security-error",
  save: "folder-security-save",
  dirty: "folder-security-dirty",
  adminList: "folder-security-list-adminPrincipals",
  adminAdd: "folder-security-list-adminPrincipals-add",
  adminInput: "folder-security-list-adminPrincipals-input",
  adminAddConfirm: "folder-security-list-adminPrincipals-add-confirm",
});

function explorerProductAclUrl(baseUrl) {
  return explorerSpaUrl(baseUrl);
}

function saveFolderPropertiesUrl(baseUrl) {
  const root = String(baseUrl || "").replace(/\/$/, "");
  return `${root}/Rhythmyx/services/pathmanagement/path/saveFolderProperties`;
}

function isSaveFolderPropertiesUrl(url) {
  return /\/path\/saveFolderProperties(?:\?|$)/i.test(String(url || ""));
}

function uniqueAclPrincipalName() {
  return `Acl4672_${Date.now().toString(36)}`;
}

function missingFolderSaveBody() {
  return {
    FolderProperties: {
      id: "16777215-101-999999",
      name: "MissingAclFolder",
      permission: { accessLevel: "ADMIN" },
    },
  };
}

module.exports = {
  ACL_TEST_IDS,
  explorerProductAclUrl,
  saveFolderPropertiesUrl,
  isSaveFolderPropertiesUrl,
  uniqueAclPrincipalName,
  missingFolderSaveBody,
  assetsFolderUrl,
  treeRootLocator,
  isKnownExplorerSitesConsoleNoise,
};
