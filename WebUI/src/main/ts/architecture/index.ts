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

export { ArchitectureShell } from "./ArchitectureShell";
export type { ArchitectureShellProps } from "./ArchitectureShell";
export { NavTree } from "./NavTree";
export type { NavTreeProps } from "./NavTree";
export { SitePicker } from "./SitePicker";
export type { SitePickerProps, SiteOption } from "./SitePicker";
export { StructureActionBar } from "./StructureActionBar";
export type { StructureActionBarProps } from "./StructureActionBar";
export { CreateSectionDialog } from "./CreateSectionDialog";
export type {
  CreateSectionDialogFields,
  CreateSectionDialogProps,
} from "./CreateSectionDialog";
export { CreateSectionFromFolderDialog } from "./CreateSectionFromFolderDialog";
export type { CreateSectionFromFolderDialogProps } from "./CreateSectionFromFolderDialog";
export { MoveSectionDialog } from "./MoveSectionDialog";
export type { MoveSectionDialogProps } from "./MoveSectionDialog";
export { RenameSectionDialog } from "./RenameSectionDialog";
export type { RenameSectionDialogProps } from "./RenameSectionDialog";
export { SectionPropertiesDialog } from "./SectionPropertiesDialog";
export type { SectionPropertiesDialogProps } from "./SectionPropertiesDialog";
export { FolderAclDialog } from "./FolderAclDialog";
export type { FolderAclDialogProps } from "./FolderAclDialog";
export {
  canEditFolderAcl,
  defaultResolveSectionFolderId,
  folderPropertiesFromSection,
  isFolderPropertiesId,
  resolveSectionFolderId,
  resolveSectionFolderPath,
} from "./folderAcl";
export type { SectionFolderPathOptions } from "./folderAcl";
export { ReplaceLandingPageDialog } from "./ReplaceLandingPageDialog";
export type { ReplaceLandingPageDialogProps } from "./ReplaceLandingPageDialog";
export { SectionLinkDialog } from "./SectionLinkDialog";
export type { SectionLinkDialogProps } from "./SectionLinkDialog";
export { ExternalLinkDialog } from "./ExternalLinkDialog";
export type {
  ExternalLinkDialogProps,
  ExternalLinkDialogValues,
} from "./ExternalLinkDialog";
export { SectionTreePickerDialog } from "./SectionTreePickerDialog";
export type { SectionTreePickerDialogProps } from "./SectionTreePickerDialog";
export {
  canPostReplaceLandingPage,
  LANDING_PAGE_ALLOWED_TYPES,
  resolveLandingPagePick,
} from "./landingPagePicker";
export type {
  LandingPagePick,
  LandingPagePickError,
  LandingPagePickItem,
  LandingPageSelection,
} from "./landingPagePicker";
export {
  FINDER_FOLDER_MIME,
  FINDER_ITEM_MIME,
  FINDER_PAGE_MIME,
  canAcceptLandingPageDragOver,
  finderDragMimeForItem,
  mapLandingPageDrop,
  serializeFinderItemDrag,
} from "./landingPageDrop";
export type {
  DropDataLike,
  FinderItemDragPayload,
  LandingPageDropOptions,
  LandingPageDropReason,
  LandingPageDropRequest,
  LandingPageDropResult,
} from "./landingPageDrop";
export { ARCH_MSG, ARCH_MSG_KEYS } from "./messages";
export type { ArchitectureMsgKey } from "./messages";
export {
  buildNavParentMap,
  collectVisibleNavNodes,
  isNavTreeRovingKey,
  NAV_TREE_ROVING_KEYS,
  resolveNavTreeKey,
} from "./navTreeKeyboard";
export type { NavTreeKeyResult, NavTreeRovingKey } from "./navTreeKeyboard";
export {
  countNavTreeNodes,
  flattenNavTree,
  isNavBranch,
  mapSectionNodeToTree,
  parseSectionNodePayload,
  sectionTypeLabel,
} from "./treeModel";
