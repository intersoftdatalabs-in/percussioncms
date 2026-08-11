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

import type { DesignGap, DesignGapWire } from "./designGaps";

export type { DesignGap, DesignGapWire } from "./designGaps";
export { designGapCode, designGapKey, formatDesignGap } from "./designGaps";

/** Guid DTO from public REST (subset used by Developer module). */
export interface RestGuid {
  stringValue?: string;
  uuid?: number;
  longValue?: number;
  type?: number;
  hostId?: number;
  untypedString?: string;
}

/** Content type summary from {@code GET /services/contenttypes}. */
export interface ContentTypeSummary {
  guid?: RestGuid;
  objectType?: RestGuid;
  name?: string;
  label?: string;
  description?: string;
  newRequest?: string;
  queryRequest?: string;
  updateRequest?: string;
  hideFromMenu?: boolean;
}

/** Jackson / JAXB list envelope sometimes used by legacy REST resources. */
export interface ContentTypeListEnvelope {
  ContentType?: ContentTypeSummary[] | ContentTypeSummary;
}

/** Field row from {@code GET /services/contenttypes/{idOrName}}. */
export interface ContentTypeFieldSummary {
  name?: string;
  label?: string;
  fieldType?: string;
  dataType?: string;
  searchable?: boolean;
  required?: boolean;
  readOnly?: boolean;
  occurrence?: string;
  hasValidation?: boolean;
  hasVisibilityRules?: boolean;
  hasInputTranslation?: boolean;
  hasOutputTranslation?: boolean;
  control?: string;
  fieldSet?: string | null;
}

/** Workflow / template association row. */
export interface NamedObjectRef {
  guid?: RestGuid;
  name?: string;
  label?: string;
  isDefault?: boolean;
}

/** Read-only design detail for one content type. */
export interface ContentTypeDetail {
  guid?: RestGuid;
  name?: string;
  label?: string;
  description?: string;
  enabled?: boolean;
  hideFromMenu?: boolean;
  appName?: string;
  editorUrl?: string;
  fields?: ContentTypeFieldSummary[];
  childFieldSets?: string[];
  allowedWorkflows?: NamedObjectRef[];
  defaultWorkflow?: NamedObjectRef | null;
  allowedTemplates?: NamedObjectRef[];
  /** Structured {code,message} on CT detail (REST-GAPS-01); wire may still be legacy string. */
  designGaps?: DesignGapWire[];
}

export interface KeywordChoiceSummary {
  label?: string;
  value?: string;
  description?: string;
  sequence?: number;
}

export interface KeywordSummary {
  guid?: RestGuid;
  label?: string;
  value?: string;
  description?: string;
  sequence?: number;
  choices?: KeywordChoiceSummary[];
}

export interface TemplateSummary {
  templateId?: number;
  templateName?: string;
  templateLabel?: string;
  templateDescription?: string;
}

export interface TemplateBindingSummary {
  executionOrder?: number;
  variable?: string;
  expression?: string;
}

export interface TemplateSlotSummary {
  guid?: RestGuid;
  name?: string;
  label?: string;
  description?: string;
}

export interface TemplateDetail {
  guid?: RestGuid;
  templateId?: number;
  name?: string;
  label?: string;
  description?: string;
  assembler?: string;
  assemblyUrl?: string;
  styleSheet?: string;
  mimeType?: string;
  charset?: string;
  locationPrefix?: string;
  locationSuffix?: string;
  outputFormat?: string;
  aaType?: string;
  publishWhen?: string;
  templateType?: string;
  globalTemplateUsage?: string;
  variant?: boolean;
  templateSource?: string;
  bindings?: TemplateBindingSummary[];
  slots?: TemplateSlotSummary[];
  /** Structured {code,message} on template detail (REST-GAPS-01). */
  designGaps?: DesignGapWire[];
}

export interface SlotSummary {
  guid?: RestGuid;
  name?: string;
  label?: string;
  description?: string;
}

export interface SlotAssociationSummary {
  contentTypeGuid?: RestGuid;
  templateGuid?: RestGuid;
}

export interface SlotDetail {
  guid?: RestGuid;
  name?: string;
  label?: string;
  description?: string;
  slotType?: string;
  systemSlot?: boolean;
  finderName?: string;
  relationshipName?: string;
  finderArguments?: Record<string, string>;
  /** ADR-003 structural slot_layout map (schemaVersion + layout keys). */
  slotLayout?: Record<string, unknown>;
  /** ADR-003 presentational slot_styles map (schemaVersion + style tokens). */
  slotStyles?: Record<string, unknown>;
  associations?: SlotAssociationSummary[];
  /** Structured {code,message} on slot detail (REST-GAPS-01). */
  designGaps?: DesignGapWire[];
}

export interface CommunitySummary {
  id?: number;
  guid?: RestGuid;
  name?: string;
  label?: string;
  description?: string;
}

export interface CommunityRoleSummary {
  communityId?: number;
  roleId?: number;
  roleName?: string;
  communityGuid?: RestGuid;
  roleGuid?: RestGuid;
}

/** Community detail including associated roles. */
export interface CommunityDetail extends CommunitySummary {
  roleList?: CommunityRoleSummary[] | { CommunityRole?: CommunityRoleSummary[] };
}

/** Object visible to a community (from POST /services/communities/visibility). */
export interface CommunityVisibleObject {
  id?: number;
  name?: string;
  label?: string;
  description?: string;
  type?: string;
  guid?: RestGuid;
  objectLocked?: boolean;
}

export interface CommunityVisibility {
  id?: number;
  guid?: RestGuid;
  visibleObjects?: CommunityVisibleObject[] | { ObjectSummary?: CommunityVisibleObject[] };
}

/** Design-time object ACL entry (from `/services/acls/object/{guid}`). */
export interface ObjectAclPermission {
  id?: number;
  permission?: string;
  permissions?: string[] | { Permission?: string[] };
}

export interface ObjectAclEntry {
  id?: number;
  name?: string;
  aclId?: number;
  principal?: { name?: string; type?: string };
  type?: { name?: string; type?: string };
  permissions?: ObjectAclPermission[] | { UserAccessLevel?: ObjectAclPermission[] };
}

export interface ObjectAcl {
  id?: number;
  name?: string;
  description?: string;
  objectId?: number;
  objectType?: number;
  guid?: RestGuid;
  objectGuid?: RestGuid;
  aclEntries?: ObjectAclEntry[] | { AclEntry?: ObjectAclEntry[] };
}

/** Classic XML Application / pipeline package summary. */
export interface ApplicationSummary {
  id?: number;
  name?: string;
  description?: string;
  enabled?: boolean;
  appRoot?: string;
  appType?: string;
  version?: string;
  empty?: boolean;
  hidden?: boolean;
}

/** Data set row from GET /services/pipelines/{idOrName}. */
export interface ApplicationDataSetSummary {
  name?: string;
  description?: string;
  requestPage?: string;
  kind?: string;
}

/** Read-only application detail with data set catalog. */
export interface ApplicationDetail extends ApplicationSummary {
  dataSets?: ApplicationDataSetSummary[];
  designGaps?: string[];
}

/** CMS locale summary from GET /services/locales. */
export interface LocaleSummary {
  id?: number;
  languageString?: string;
  label?: string;
  description?: string;
  status?: string;
  /** RXLOCALE.ISBASE — language-only / base locale. */
  baseLocale?: boolean;
  /** Exact RXLOCALEFORMAT row present for this language string. */
  hasFormatProfile?: boolean;
}

/** RXLOCALEFORMAT row (keyed by language string, not LOCALEID). */
export interface LocaleFormatSummary {
  languageString?: string;
  textDir?: string;
  datePattern?: string;
  timePattern?: string;
  dateTimePattern?: string;
  decimalSep?: string;
  groupingSep?: string;
  currencyCode?: string;
  currencyPattern?: string;
  firstDayOfWeek?: number;
  measurementSystem?: string;
  defaultTz?: string;
  numberingSystem?: string;
  calendar?: string;
}

/** Locale detail including optional exact format profile. */
export interface LocaleDetail extends LocaleSummary {
  format?: LocaleFormatSummary | null;
  designGaps?: string[];
}

/** Shared field group summary from GET /services/sharedfields. */
export interface SharedFieldGroupSummary {
  name?: string;
  filename?: string;
  fieldCount?: number;
}

/** Field row from GET /services/sharedfields/{name}. */
export interface SharedFieldSummary {
  name?: string;
  dataType?: string;
  searchable?: boolean;
  required?: boolean;
  readOnly?: boolean;
  occurrence?: string;
}

/** Read-only shared field group detail. */
export interface SharedFieldGroupDetail {
  name?: string;
  filename?: string;
  fields?: SharedFieldSummary[];
  designGaps?: string[];
}

/** Field row from GET /services/systemdef. */
export interface SystemDefFieldSummary {
  name?: string;
  dataType?: string;
  searchable?: boolean;
  required?: boolean;
  readOnly?: boolean;
  occurrence?: string;
}

/** Read-only content-editor system definition. */
export interface SystemDefDetail {
  fieldCount?: number;
  cacheTimeoutMinutes?: number;
  fields?: SystemDefFieldSummary[];
  designGaps?: string[];
}

/** Item filter rule param from GET /services/itemfilters. */
export interface ItemFilterRuleParam {
  name?: string;
  value?: string;
}

/** Item filter rule definition. */
export interface ItemFilterRule {
  name?: string;
  ruleId?: RestGuid;
  params?: ItemFilterRuleParam[];
}

/** Assembly item filter (AS-07). */
export interface ItemFilter {
  filterId?: RestGuid;
  name?: string;
  description?: string;
  legacyAuthtype?: number;
  rules?: ItemFilterRule[];
  parentFilter?: ItemFilter | null;
}

/** Display format column from GET /services/displayformats. */
export interface DisplayFormatColumn {
  source?: string;
  displayName?: string;
  description?: string;
  renderType?: string;
  position?: number;
  width?: number;
  categorized?: boolean;
  ascendingSort?: boolean;
  textType?: boolean;
  numberType?: boolean;
  dateType?: boolean;
  imageType?: boolean;
}

/** CX display format (UI-05). */
export interface DisplayFormat {
  guid?: RestGuid;
  name?: string;
  label?: string;
  displayName?: string;
  description?: string;
  internalName?: string;
  displayId?: number;
  validForRelatedContent?: boolean;
  validForViewsAndSearches?: boolean;
  validForFolder?: boolean;
  ascendingSort?: boolean;
  descendingSort?: boolean;
  columns?: DisplayFormatColumn[] | { DisplayFormatColumn?: DisplayFormatColumn[] };
}

/** Action menu parameter. */
export interface ActionMenuParameter {
  name?: string;
  value?: string;
  description?: string;
}

/** Action menu property. */
export interface ActionMenuProperty {
  name?: string;
  value?: string;
  description?: string;
  actionId?: number;
}

/** CX action menu (UI-02). */
export interface ActionMenu {
  id?: number;
  guid?: RestGuid;
  name?: string;
  label?: string;
  description?: string;
  url?: string;
  sortRank?: number;
  menuType?: string;
  handler?: string;
  parameters?: ActionMenuParameter[];
  properties?: ActionMenuProperty[];
}

/** Search field criterion from GET /services/searches. */
export interface SearchFieldSummary {
  fieldName?: string;
  displayName?: string;
  operator?: string;
  fieldValue?: string;
  fieldType?: string;
  position?: number;
}

/** CX search definition (UI-06). */
export interface SearchDef {
  guid?: RestGuid;
  id?: number;
  name?: string;
  label?: string;
  description?: string;
  type?: string;
  displayFormatId?: string;
  url?: string;
  parentCategory?: number;
  maximumResultSize?: number;
  userSearch?: boolean;
  customSearch?: boolean;
  standardSearch?: boolean;
  userCustomizable?: boolean;
  caseSensitive?: boolean;
  fields?: SearchFieldSummary[];
  designGaps?: string[];
}

/**
 * Optional overrides for {@code POST /services/searches/{idOrName}/execute}.
 * Mirrors {@code com.percussion.rest.searches.SearchExecuteRequest}.
 */
export interface SearchExecuteRequest {
  folderPath?: string;
  startIndex?: number;
  maxResults?: number;
  sortColumn?: string;
  sortOrder?: string;
}

/**
 * One result row from design-search execute.
 * Mirrors {@code com.percussion.rest.searches.SearchResultItem}.
 */
export interface SearchResultItem {
  id?: string;
  name?: string;
  title?: string;
  folderPath?: string;
  type?: string;
}

/**
 * Paged result envelope for design-search execute.
 * Mirrors {@code com.percussion.rest.searches.SearchExecuteResult}.
 */
export interface SearchExecuteResult {
  children?: SearchResultItem[];
  totalCount?: number;
  startIndex?: number;
  searchName?: string;
  displayFormatId?: string;
}

/** View field criterion from GET /services/views. */
export interface ViewFieldSummary {
  fieldName?: string;
  displayName?: string;
  operator?: string;
  fieldValue?: string;
  fieldType?: string;
  position?: number;
}

/** CX view definition (UI-07). */
export interface ViewDef {
  guid?: RestGuid;
  id?: number;
  name?: string;
  label?: string;
  description?: string;
  type?: string;
  displayFormatId?: string;
  url?: string;
  parentCategory?: number;
  maximumResultSize?: number;
  standardView?: boolean;
  customView?: boolean;
  view?: boolean;
  userCustomizable?: boolean;
  caseSensitive?: boolean;
  fields?: ViewFieldSummary[];
  designGaps?: string[];
}

/** Server extension from GET /services/extensions/catalog. */
export interface ExtensionDef {
  handlerName?: string;
  context?: string;
  extensionName?: string;
  category?: string;
  fqn?: string;
  version?: number;
  deprecated?: boolean;
  jexlExtension?: boolean;
  supportedInterfaces?: string[];
  runtimeParameters?: { name?: string; dataType?: string; description?: string }[];
  initParameters?: Record<string, string>;
  methods?: Record<string, { name?: string; description?: string }>;
}

/** Effect row on a relationship type. */
export interface RelationshipTypeEffectSummary {
  name?: string;
  extensionRef?: string;
  activationEndPoint?: string;
}

/** Property row on a relationship type. */
export interface RelationshipTypePropertySummary {
  name?: string;
  value?: string;
}

/** System relationship type from GET /services/relationshiptypes (SY-03). */
export interface RelationshipTypeDef {
  guid?: RestGuid;
  name?: string;
  label?: string;
  description?: string;
  type?: string;
  category?: string;
  categoryLabel?: string;
  systemType?: boolean;
  userType?: boolean;
  allowCloning?: boolean;
  useOwnerRevision?: boolean;
  useDependentRevision?: boolean;
  effects?: RelationshipTypeEffectSummary[];
  systemProperties?: RelationshipTypePropertySummary[];
  userProperties?: RelationshipTypePropertySummary[];
  designGaps?: string[];
}

/** Workflow step from workflowmanagement PSUiWorkflow. */
export interface WorkflowStepSummary {
  stepName?: string;
  permissionNames?: string[];
  stepRoles?: { roleName?: string; roleId?: number }[];
}

/**
 * Workflow catalog row from GET /services/workflowmanagement/workflows
 * (PSUiWorkflow — SY-04 association browse).
 */
export interface WorkflowDef {
  workflowName?: string;
  workflowDescription?: string;
  stagingRoleNames?: string;
  defaultWorkflow?: boolean;
  workflowSteps?: WorkflowStepSummary[];
  /** Developer surface honesty; defaults filled by workflowsApi when absent. */
  designGaps?: string[];
}

/** Server configuration from GET /services/serverconfigs (SY-02). */
export interface ServerConfigDef {
  name?: string;
  displayName?: string;
  fileName?: string;
  description?: string;
  typeId?: number;
  content?: string;
  mimeType?: string;
  characterEncoding?: string;
  contentLength?: number;
  designGaps?: string[];
}

/** CE control parameter from GET /services/cecontrols. */
export interface ControlParameterSummary {
  name?: string;
  description?: string;
  dataType?: string;
  paramType?: string;
  defaultValue?: string;
  required?: boolean;
}

/** Content editor control from GET /services/cecontrols (UI-01). */
export interface ControlDef {
  name?: string;
  displayName?: string;
  description?: string;
  dimension?: string;
  choiceSet?: string;
  scope?: string;
  deprecated?: boolean;
  deprecatedReplacement?: string;
  parameters?: ControlParameterSummary[];
  designGaps?: string[];
}

/**
 * Site catalog row from GET /services/sites (SY-04 association browse).
 * Optional fields may arrive as plain strings from Jackson.
 */
export interface SiteDef {
  name?: string;
  description?: string;
  baseUrl?: string;
  siteProtocol?: string;
  defaultDocument?: string;
  defaultFileExtention?: string;
  pageBasedSite?: boolean;
  isCanonical?: boolean;
  canonical?: boolean;
  guid?: RestGuid;
  designGaps?: string[];
}

