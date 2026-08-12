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

/**
 * Wire types for sitemanage section / Architecture nav tree (#3095).
 *
 * <p>Mirrors {@code PSSectionNode} / {@code PSSiteSection} and
 * {@code PSSectionTypeEnum} from projects/sitemanage.</p>
 */

/** Section type enum values from {@code PSSiteSection.PSSectionTypeEnum}. */
export type SectionType =
  | "section"
  | "sectionlink"
  | "externallink"
  | "blog"
  | string;

/**
 * Wire DTO for {@code GET /sitemanage/section/tree/{siteName}}
 * ({@code PSSectionNode} / Jackson root {@code SectionNode}).
 */
export interface SectionNodeWire {
  id?: string | null;
  title?: string | null;
  folderPath?: string | null;
  sectionType?: SectionType | null;
  requiresLogin?: boolean | null;
  allowAccessTo?: string | null;
  /** Nested children; Jackson may also use {@code SectionNode} array wrappers. */
  childNodes?: SectionNodeWire[] | SectionNodeWire | null;
  /** Alternate Jackson/list wrapper keys seen on some payloads. */
  ChildNodes?: SectionNodeWire[] | SectionNodeWire | null;
  SectionNode?: SectionNodeWire[] | SectionNodeWire | null;
}

/**
 * Wire DTO for {@code GET /sitemanage/section/root/{siteName}}
 * ({@code PSSiteSection} / Jackson root {@code SiteSection}).
 */
export interface SiteSectionWire {
  id?: string | null;
  title?: string | null;
  folderPath?: string | null;
  sectionType?: SectionType | null;
  externalLinkUrl?: string | null;
  target?: string | null;
  childIds?: string[] | string | null;
  requiresLogin?: boolean | null;
  allowAccessTo?: string | null;
  cssClassNames?: string | null;
}

/** Normalized in-app nav tree node (read-only UI model). */
export interface NavTreeNode {
  id: string;
  title: string;
  folderPath: string | null;
  sectionType: SectionType;
  requiresLogin: boolean;
  children: NavTreeNode[];
}

/** Target window values from {@code PSSiteSection.PSSectionTargetEnum}. */
export type SectionTarget = "_self" | "_blank" | "_top" | "_parent" | string;

/**
 * Wire fields for {@code POST /sitemanage/section/create}
 * ({@code CreateSiteSection} / {@code PSCreateSiteSection}).
 */
export interface CreateSiteSectionFields {
  pageTitle: string;
  pageLinkTitle: string;
  pageName: string;
  pageUrlIdentifier: string;
  templateId: string;
  folderPath: string;
  sectionType?: SectionType;
  target?: SectionTarget;
  copyTemplates?: boolean;
  blogPostTemplateId?: string;
}

/**
 * Wire fields for {@code POST /sitemanage/section/update}
 * ({@code SiteSectionProperties} / {@code PSSiteSectionProperties}).
 */
export interface SiteSectionPropertiesWire {
  id: string;
  title: string;
  folderName: string;
  target?: SectionTarget | null;
  requiresLogin?: boolean | null;
  allowAccessTo?: string | null;
  cssClassNames?: string | null;
  secureSite?: boolean | null;
  secureAncestor?: boolean | null;
  siteRootSection?: boolean | null;
  /** Folder ACL object from server — pass through on update when present. */
  folderPermission?: unknown;
}

/**
 * Wire fields for {@code POST /sitemanage/section/move}
 * ({@code MoveSiteSection} / {@code PSMoveSiteSection}).
 */
export interface MoveSiteSectionFields {
  sourceId: string;
  targetId: string;
  targetIndex: number;
  sourceParentId?: string | null;
}

/** Result of locating a node among its siblings for reorder. */
export interface SiblingPlacement {
  parent: NavTreeNode;
  index: number;
  siblings: NavTreeNode[];
}
