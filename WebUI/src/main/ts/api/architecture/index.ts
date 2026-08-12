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
  createSiteSection,
  updateSiteSection,
  moveSiteSection,
  deleteSiteSection,
  deleteSectionLink,
  convertSectionToFolder,
  sectionTreeUrl,
  sectionRootUrl,
  sectionPropertiesUrl,
  sectionDeleteUrl,
  sectionConvertToFolderUrl,
  sectionDeleteLinkUrl,
} from "./sectionApi";
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
  buildCreateSiteSectionBody,
  buildMoveSiteSectionBody,
  buildSiblingReorderMove,
  buildUpdateSiteSectionBody,
  canCreateChildUnder,
  canDeleteNavNode,
  canMoveNavNodeDown,
  canMoveNavNodeUp,
  findNavNodeById,
  findSiblingPlacement,
  isRootNavNode,
  isSectionLinkType,
  parseSiteSectionPropertiesPayload,
  resolveCreateParentFolderPath,
  validateSectionFolderName,
  validateSectionTitle,
} from "./sectionMutations";
export type {
  CreateSiteSectionFields,
  MoveSiteSectionFields,
  NavTreeNode,
  SectionNodeWire,
  SectionTarget,
  SectionType,
  SiblingPlacement,
  SiteSectionPropertiesWire,
  SiteSectionWire,
} from "./types";
