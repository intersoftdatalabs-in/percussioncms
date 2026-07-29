/*
 * Copyright 1999-2026 Percussion Software, Inc.
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
  designGaps?: string[];
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
  designGaps?: string[];
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
  associations?: SlotAssociationSummary[];
  designGaps?: string[];
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
