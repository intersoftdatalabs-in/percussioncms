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

export {
  loadSectionTree,
  loadRootSection,
  loadSectionProperties,
  loadSection,
  createSiteSection,
  updateSiteSection,
  moveSiteSection,
  deleteSiteSection,
  deleteSectionLink,
  convertSectionToFolder,
  createSectionFromFolder,
  replaceLandingPage,
  createSectionLink,
  createExternalLinkSection,
  updateSectionLink,
  updateExternalLink,
  sectionTreeUrl,
  sectionRootUrl,
  sectionPropertiesUrl,
  sectionDeleteUrl,
  sectionConvertToFolderUrl,
  sectionDeleteLinkUrl,
  sectionCreateLinkUrl,
  sectionUpdateExternalLinkUrl,
  sectionLoadUrl,
} from "./sectionApi";
export {
  buildSiteCopyRequestBody,
  copyManagedSite,
  deleteManagedSite,
  isSiteBeingImported,
  isSiteCopyInProgress,
  loadSiteCopyInfo,
  normalizeCopyAssetFolder,
  siteCopyInfoUrl,
  siteCopyUrl,
  siteDeleteUrl,
  siteImportingUrl,
  suggestCopySiteName,
} from "./siteAdminApi";
export type { SiteCopyFields } from "./siteAdminApi";
export {
  countNavTreeNodes,
  flattenNavTree,
  isNavBranch,
  mapSectionNodeToTree,
  normalizeChildNodes,
  parseSectionNodePayload,
  sectionTypeLabel,
} from "./mapSectionTree";
export {
  applyTitleToProperties,
  buildCreateExternalLinkBody,
  buildCreateSectionFromFolderBody,
  buildCreateSectionLinkPath,
  buildCreateSiteSectionBody,
  buildMoveSiteSectionBody,
  buildReplaceLandingPageBody,
  buildSiblingReorderMove,
  buildUpdateSectionLinkBody,
  buildUpdateSiteSectionBody,
  canConvertSectionToFolder,
  canCreateChildUnder,
  canDeleteNavNode,
  canEditLinkNode,
  canMoveNavNodeDown,
  canMoveNavNodeUp,
  canReplaceLandingPage,
  findNavNodeById,
  findSiblingPlacement,
  isBlogSectionType,
  isExternalLinkType,
  isRootNavNode,
  isSectionLinkType,
  isValidSectionLinkTarget,
  parseReplaceLandingPagePayload,
  parseSiteSectionPayload,
  parseSiteSectionPropertiesPayload,
  resolveCreateParentFolderPath,
  splitCmsPagePath,
  validateExternalUrl,
  validateLandingPageName,
  validateSectionFolderName,
  validateSectionTitle,
  validateSourceFolderPath,
} from "./sectionMutations";
export type {
  CreateExternalLinkFields,
  CreateSectionFromFolderFields,
  CreateSiteSectionFields,
  MoveSiteSectionFields,
  NavTreeNode,
  ReplaceLandingPageFields,
  ReplaceLandingPageResult,
  SectionNodeWire,
  SectionTarget,
  SectionType,
  SiblingPlacement,
  SiteSectionPropertiesWire,
  SiteSectionWire,
  UpdateSectionLinkFields,
} from "./types";
