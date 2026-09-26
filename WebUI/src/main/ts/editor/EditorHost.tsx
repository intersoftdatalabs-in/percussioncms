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
 * React Content Editor host (995): checkout + content-type field form.
 * Rich controls persist through itemmanagement fields / binary APIs.
 */

import React, { useEffect, useMemo, useRef, useState } from "react";
import { useSearchParams } from "react-router";
import {
  getContentTypeDetail,
  listContentTypes,
} from "../api/developer/contentTypesApi";
import type {
  CommunitySummary,
  ContentTypeFieldSummary,
  ContentTypeSummary,
  KeywordSummary,
} from "../api/developer/types";
import type { NamedObjectRef } from "../api/developer/types";
import type { PSLocalDependencySummary } from "../api/contentExplorer/relationship";
import type { SlotCanvas } from "../api/contentExplorer/slotRelationshipApi";
import type { ItemEditorBinaryMeta } from "./itemBinaryApi";
import {
  createNewCopy,
  createPromotableVersion,
  type ItemCopyResult,
} from "../api/contentExplorer/itemCopyApi";
import {
  changeItemWorkflow,
  getItemWorkflowChoices,
  getItemWorkflowTransitions,
  transitionItem,
  type ItemStateTransition,
  type ItemWorkflowChoices,
} from "../api/contentExplorer/itemWorkflowApi";
import { deleteFolderItem, findItemById, renameFolderItem } from "../api/contentExplorer/pathApi";
import { formatApiError, isSessionRedirectError } from "../api/client";
import { CopyDestinationPickerDialog } from "../contentExplorer/CopyDestinationPickerDialog";
import { MoveDestinationPickerDialog } from "../contentExplorer/MoveDestinationPickerDialog";
import { parsePositiveInt } from "../assembly/assemblyHostUrl";
import { message } from "../i18n/message";
import { mergeEditorRows, type EditorFieldRow } from "./controlKinds";
import {
  collectInvalidDateFieldErrors,
  collectRequiredFieldErrors,
  mapSaveApiErrorToFieldErrors,
} from "./editorFieldErrors";
import { collectUnsafeHtmlFieldErrors } from "./htmlField";
import { collectInvalidLongTextFieldErrors } from "./longTextField";
import { collectInvalidLinkFieldErrors } from "./linkField";
import { collectInvalidNumericFieldErrors } from "./numericField";
import { DateFieldWidget } from "./widgets/DateFieldWidget";
import { TableFieldWidget } from "./widgets/TableFieldWidget";
import {
  canCopyFromEditor,
  copyEditorItemToFolder,
  editorCopyErrorReason,
  parseCopyLandingContentId,
  type EditorCopyKind,
} from "./editorCopy";
import {
  canRenameFromEditor,
  editorItemPathFromLookup,
  editorListingName,
  editorRenameErrorReason,
  renameLanded,
} from "./editorRename";
import {
  canMoveFromEditor,
  cmsFoldersEqual,
  editorMoveErrorReason,
  moveEditorItemToFolder,
  parentFolderOfItemPath,
} from "./editorMove";
import {
  canRecycleFromEditor,
  editorRecycleErrorReason,
  editorRecycleItemPath,
  type EditorRecycleTarget,
} from "./editorRecycle";
import {
  canForceCheckInFromEditor,
  canUseEditorCheckoutActions,
  editorLockErrorReason,
  isCheckedOutToSelf,
  type EditorCheckoutUserInfo,
} from "./editorCheckout";
import { forceCheckInItem } from "../api/contentExplorer/itemWorkflowApi";
import {
  fetchItemRevisionCompare,
  fetchItemRevisions,
  restoreItemRevision,
  type ItemRevision,
  type ItemRevisionCompare,
  type ItemRevisionsSummary,
} from "../api/contentExplorer/itemRevisionsApi";
import {
  canRestoreFromEditor,
  editorRevisionErrorReason,
  isEmptyRevisionCompare,
  parseRevisionId,
  restoreRevisionConfirmBody,
  revisionCompareSelection,
  summarizeRevisionRow,
} from "./editorRevisions";
import { fetchPreviewLocation } from "../api/contentExplorer/assemblyApi";
import {
  canPreviewFromEditor,
  editorChosenTemplateFrameUrl,
  editorDefaultPreviewFrameUrl,
  editorDraftIsDirty,
  numericPreviewTemplates,
  positivePreviewTemplateId,
  previewEditorItem,
  PREVIEW_TEMPLATE_CURRENT,
} from "./editorPreview";
import {
  loadLinkedPagesForTakedown,
  type LinkedPageForTakedown,
} from "../contentExplorer/itemPublish";
import { PublishingHistoryDialog } from "../contentExplorer/PublishingHistoryDialog";
import {
  canPublishFromEditor,
  canStageFromEditor,
  canTakedownFromEditor,
  canViewPublishHistoryFromEditor,
  formatEditorTakedownConfirm,
  publishEditorItem,
  resolveEditorPublishKind,
  stageEditorItem,
  takedownEditorItem,
  type EditorPublishKind,
} from "./editorPublish";
import {
  buildEditorCreateRequest,
  canCreateFromEditor,
  editorCreateErrorReason,
  parseCreateLandingContentId,
} from "./editorCreate";
import { editorBinaryErrorReason, isImageFile } from "./editorBinary";
import { editorSaveErrorReason } from "./editorSave";
import {
  changePageTemplate,
  isAllowedPageTemplate,
  pageTemplateIdFromFields,
  resolvePageTemplateSelection,
  withPageTemplateField,
} from "./editorPageTemplate";
import {
  isExplorerPageType,
  loadPageTemplates,
  templatesFromContentType,
  type PageTemplateChoice,
} from "./pageTemplates";
import {
  canChangeEditorWorkflow,
  canRunEditorTransition,
  uniqueTransitionTriggers,
} from "./editorWorkflow";
import {
  createEditorItem,
  type ItemCreateRequest,
  type ItemCreateResult,
} from "./itemCreateApi";
import {
  checkinEditorItem,
  checkoutEditorItem,
  fetchItemEditorFields,
  saveItemEditorFields,
  type ItemEditorFields,
} from "./itemFieldsApi";
import { uploadItemEditorBinary } from "./itemBinaryApi";
import styles from "./EditorHost.module.css";
import { normalizeEditorMode, type EditorHostMode } from "./editorHostUrl";
import { EditorRelatedContentPanel } from "./EditorRelatedContentPanel";
import { EditorWorkflowPanel } from "./EditorWorkflowPanel";
import { EDITOR_MSG } from "./messages";
import { TranslationsPanel } from "../contentExplorer/TranslationsPanel";
import {
  createTranslations,
  listItemTranslationVariants,
  type CreateTranslationsResult,
  type ItemTranslationVariants,
} from "../api/contentExplorer/translationsApi";
import { listLocales } from "../api/developer/localesApi";
import type { LocaleSummary } from "../api/developer/types";
import { CommunityFieldWidget } from "./widgets/CommunityFieldWidget";
import { FileFieldWidget } from "./widgets/FileFieldWidget";
import { HtmlFieldWidget } from "./widgets/HtmlFieldWidget";
import { ImageFieldWidget } from "./widgets/ImageFieldWidget";
import { KeywordFieldWidget } from "./widgets/KeywordFieldWidget";
import { PromoteForm } from "./widgets/PromoteForm";

export { mergeEditorRows } from "./controlKinds";

/** Coerce item-field JSON so controlled inputs never receive a number. */
export function fieldValueAsString(value: unknown): string {
  return value == null ? "" : String(value);
}

export interface EditorHostProps {
  loadFields?: (itemId: string) => Promise<ItemEditorFields>;
  saveFields?: (
    itemId: string,
    payload: ItemEditorFields,
  ) => Promise<ItemEditorFields>;
  checkout?: (itemId: string) => Promise<EditorCheckoutUserInfo | void>;
  checkin?: (itemId: string, comment?: string) => Promise<void>;
  /** Admin force check-in of another user's checkout ({@code forceCheckIn/{id}}). */
  forceCheckin?: (itemId: string) => Promise<void>;
  /** Test seam: confirm force check-in (defaults to {@code window.confirm}). */
  confirmForceCheckin?: (body: string) => boolean;
  loadType?: (typeName: string) => Promise<{
    fields?: ContentTypeFieldSummary[];
    allowedTemplates?: unknown[];
  }>;
  uploadBinary?: (
    itemId: string,
    field: string,
    file: File,
  ) => Promise<unknown>;
  loadKeywords?: () => Promise<KeywordSummary[]>;
  loadCommunities?: () => Promise<CommunitySummary[]>;
  loadBinaryMeta?: (itemId: string, field: string) => Promise<ItemEditorBinaryMeta>;
  /** Test seam: allowed transitions ({@code getTransitions}). */
  loadTransitions?: (itemId: string) => Promise<ItemStateTransition>;
  /** Test seam: content-type workflow catalog ({@code allowedWorkflows}). */
  loadWorkflowChoices?: (itemId: string) => Promise<ItemWorkflowChoices>;
  /** Test seam: {@code POST changeWorkflow}. */
  changeWorkflow?: (itemId: string, workflowId: string) => Promise<ItemStateTransition>;
  /** Test seam: {@code transitionWithComments}. */
  runTransition?: (
    itemId: string,
    trigger: string,
    comment?: string,
  ) => Promise<unknown>;
  /** Extra / override names that require a comment (tests). */
  commentRequiredTriggers?: readonly string[];
  /** Test seam: sitemanage demand-publish ({@code publish/page|resource/{id}}). */
  publishItem?: (itemId: string, kind: EditorPublishKind) => Promise<boolean>;
  /** Test seam: confirm before Publish now (defaults to {@code window.confirm}). */
  confirmPublish?: (body: string) => boolean;
  /** Test seam: sitemanage stage ({@code publish/page|resource/staging/{id}}). */
  stageItem?: (itemId: string, kind: EditorPublishKind) => Promise<boolean>;
  /** Test seam: confirm before Stage (defaults to {@code window.confirm}). */
  confirmStage?: (body: string) => boolean;
  /** Test seam: sitemanage takedown ({@code takedown/page|resource/{id}}). */
  takedownItem?: (
    itemId: string,
    kind: EditorPublishKind,
    linked: LinkedPageForTakedown[],
  ) => Promise<boolean>;
  /** Test seam: linked pages listed on the takedown confirm. */
  loadTakedownLinked?: (itemId: string) => Promise<LinkedPageForTakedown[]>;
  /** Test seam: confirm before Take down (defaults to {@code window.confirm}). */
  confirmTakedown?: (body: string) => boolean;
  /** Test seam: Explorer {@code openPreviewItem} wrapper. */
  previewItem?: (itemId: string, kind: EditorPublishKind) => Promise<void>;
  /**
   * Test seam: {@code GET /assembly/preview-location} for a chosen template.
   * Does not change the saved page template.
   */
  loadPreviewLocation?: (
    contentId: number,
    templateId: number,
  ) => Promise<{ previewUrl: string }>;
  /** Test seam: confirm unsaved preview (defaults to {@code window.confirm}). */
  confirmUnsavedPreview?: (body: string) => boolean;
  /** Test seam: itemmanagement {@code newCopy}. */
  copyItem?: (itemId: string) => Promise<ItemCopyResult>;
  /** Test seam: itemmanagement {@code promotableVersion}. */
  copyPromotable?: (itemId: string) => Promise<ItemCopyResult>;
  /** Test seam: confirm new copy / promotable (defaults to {@code window.confirm}). */
  confirmCopy?: (body: string) => boolean;
  /** Test seam: {@code POST /rest/folders/copy/item} into a chosen folder. */
  copyItemToFolder?: (itemPath: string, targetFolderPath: string) => Promise<void>;
  /** Test seam: folders {@code POST /folders/rename/item}. */
  renameItem?: (itemPath: string, newName: string) => Promise<void>;
  /** Test seam: pathmanagement item path for the open content id. */
  loadItemLocation?: (itemId: string) => Promise<{ path: string }>;
  /** Test seam: {@code POST /rest/folders/move/item}. */
  moveItem?: (itemPath: string, targetFolderPath: string) => Promise<void>;
  /** Test seam: pathmanagement lookup of the open item. */
  resolveRecycleTarget?: (itemId: string) => Promise<EditorRecycleTarget>;
  /** Test seam: public REST item recycle ({@code DELETE /rest/folders/item}). */
  recycleItem?: (itemPath: string) => Promise<void>;
  /** Test seam: confirm recycle (defaults to {@code window.confirm}). */
  confirmRecycle?: (body: string) => boolean;
  /** Test seam: itemmanagement {@code revisions/{id}}. */
  loadRevisions?: (itemId: string) => Promise<ItemRevisionsSummary>;
  /** Test seam: itemmanagement {@code restoreRevision/{guid}}. */
  restoreRevision?: (itemId: string, revId: number) => Promise<void>;
  /** Test seam: itemmanagement {@code compare/{id}/{rev1}/{rev2}}. */
  compareRevisions?: (
    itemId: string,
    rev1: number,
    rev2: number,
  ) => Promise<ItemRevisionCompare>;
  /** Test seam: confirm restore (defaults to {@code window.confirm}). */
  confirmRestore?: (body: string) => boolean;
  /** Test seam: slot-relationships canvas for related content browse. */
  loadRelatedCanvas?: (ownerId: number) => Promise<SlotCanvas>;
  /** Test seam: local/inline related items. */
  loadRelatedLocal?: (itemId: string) => Promise<PSLocalDependencySummary>;
  /** Test seam: content-type catalog for New item. */
  loadContentTypes?: () => Promise<ContentTypeSummary[]>;
  /** Test seam: itemmanagement create. */
  createItem?: (req: ItemCreateRequest) => Promise<ItemCreateResult>;
  /** Test seam: GET translation variants for the open item. */
  loadTranslationVariants?: (itemId: string) => Promise<ItemTranslationVariants>;
  /** Test seam: locale catalog for create-variant targets. */
  loadTranslationLocales?: () => Promise<LocaleSummary[]>;
  /** Test seam: POST one or more locale copies. */
  createTranslationVariants?: (body: {
    itemIds: number[];
    locales?: string[];
  }) => Promise<CreateTranslationsResult>;
  /** Test seam: allowed page templates ({@code loadPageTemplates}). */
  loadTemplates?: (
    folderPath: string,
    contentType: string,
  ) => Promise<PageTemplateChoice[]>;
  /** Test seam: {@code PUT …/page/changeTemplate/{pageId}/{templateId}}. */
  changeTemplate?: (pageId: string, templateId: string) => Promise<void>;
}

function badgeKey(mode: EditorHostMode): string {
  if (mode === "view") {
    return EDITOR_MSG.BADGE_VIEW;
  }
  if (mode === "promote") {
    return EDITOR_MSG.BADGE_PROMOTE;
  }
  return EDITOR_MSG.BADGE_EDIT;
}

function EditorFieldControl({
  row,
  itemId,
  locked,
  invalid,
  onChange,
  onFile,
  loadKeywords,
  loadCommunities,
  loadBinaryMeta,
}: {
  row: EditorFieldRow;
  itemId: string;
  locked: boolean;
  invalid?: boolean;
  onChange: (name: string, value: string) => void;
  onFile: (name: string, file: File | null) => void;
  loadKeywords?: () => Promise<KeywordSummary[]>;
  loadCommunities?: () => Promise<CommunitySummary[]>;
  loadBinaryMeta?: (itemId: string, field: string) => Promise<ItemEditorBinaryMeta>;
}): React.ReactElement {
  if (row.kind === "html") {
    return (
      <HtmlFieldWidget
        name={row.name}
        value={row.value}
        readOnly={locked}
        onChange={(value) => onChange(row.name, value)}
      />
    );
  }
  if (row.kind === "file") {
    return (
      <FileFieldWidget
        itemId={itemId}
        name={row.name}
        readOnly={locked}
        loadMeta={loadBinaryMeta}
        onFile={(file) => onFile(row.name, file)}
      />
    );
  }
  if (row.kind === "image") {
    return (
      <ImageFieldWidget
        itemId={itemId}
        name={row.name}
        readOnly={locked}
        loadMeta={loadBinaryMeta}
        onFile={(file) => onFile(row.name, file)}
      />
    );
  }
  if (row.kind === "keyword") {
    return (
      <KeywordFieldWidget
        name={row.name}
        value={fieldValueAsString(row.value)}
        readOnly={locked}
        loadKeywords={loadKeywords}
        onChange={(value) => onChange(row.name, value)}
      />
    );
  }
  if (row.kind === "community") {
    return (
      <CommunityFieldWidget
        name={row.name}
        value={row.value}
        readOnly={locked}
        loadCommunities={loadCommunities}
        onChange={(value) => onChange(row.name, value)}
      />
    );
  }
  if (row.kind === "date" || row.kind === "datetime") {
    return (
      <DateFieldWidget
        name={row.name}
        value={fieldValueAsString(row.value)}
        kind={row.kind}
        readOnly={locked}
        invalid={invalid}
        required={row.required}
        onChange={(value) => onChange(row.name, value)}
      />
    );
  }
  if (row.kind === "table") {
    return (
      <TableFieldWidget
        name={row.name}
        value={fieldValueAsString(row.value)}
        readOnly={locked}
        invalid={invalid}
        required={row.required}
        onChange={(value) => onChange(row.name, value)}
      />
    );
  }
  if (row.kind === "link") {
    return (
      <div className={styles.linkRow}>
        <input
          className={`${styles.input} ${locked ? styles.readonly : ""}`}
          data-testid={`editor-field-${row.name}`}
          data-editor-kind="link"
          name={row.name}
          value={row.value}
          readOnly={locked}
          aria-invalid={invalid ? true : undefined}
          aria-required={row.required ? true : undefined}
          onChange={(e) => onChange(row.name, e.target.value)}
        />
        {locked ? null : (
          <button
            type="button"
            className={styles.button}
            data-testid={`editor-link-clear-${row.name}`}
            onClick={() => onChange(row.name, "")}
          >
            {message(EDITOR_MSG.LINK_CLEAR)}
          </button>
        )}
      </div>
    );
  }
  if (row.kind === "number") {
    return (
      <input
        className={`${styles.input} ${locked ? styles.readonly : ""}`}
        data-testid={`editor-field-${row.name}`}
        data-editor-kind="number"
        name={row.name}
        inputMode={row.numericInteger === false ? "decimal" : "numeric"}
        value={row.value}
        readOnly={locked}
        aria-invalid={invalid ? true : undefined}
        aria-required={row.required ? true : undefined}
        onChange={(e) => onChange(row.name, e.target.value)}
      />
    );
  }
  if (row.kind === "longtext") {
    return (
      <textarea
        className={`${styles.textarea} ${locked ? styles.readonly : ""}`}
        data-testid={`editor-field-${row.name}`}
        data-editor-kind="longtext"
        name={row.name}
        value={row.value}
        readOnly={locked}
        aria-invalid={invalid ? true : undefined}
        aria-required={row.required ? true : undefined}
        onChange={(e) => onChange(row.name, e.target.value)}
      />
    );
  }
  return (
    <input
      className={`${styles.input} ${locked ? styles.readonly : ""}`}
      data-testid={`editor-field-${row.name}`}
      data-editor-kind="text"
      name={row.name}
      value={row.value}
      readOnly={locked}
      aria-invalid={invalid ? true : undefined}
      aria-required={row.required ? true : undefined}
      onChange={(e) => onChange(row.name, e.target.value)}
    />
  );
}

export function EditorHost({
  loadFields = fetchItemEditorFields,
  saveFields = saveItemEditorFields,
  checkout = checkoutEditorItem,
  checkin = checkinEditorItem,
  forceCheckin = forceCheckInItem,
  confirmForceCheckin,
  loadType = getContentTypeDetail,
  uploadBinary = uploadItemEditorBinary,
  loadKeywords,
  loadCommunities,
  loadBinaryMeta,
  loadTransitions = getItemWorkflowTransitions,
  loadWorkflowChoices = getItemWorkflowChoices,
  changeWorkflow = changeItemWorkflow,
  runTransition = transitionItem,
  commentRequiredTriggers,
  publishItem = publishEditorItem,
  confirmPublish,
  stageItem = stageEditorItem,
  confirmStage,
  takedownItem = takedownEditorItem,
  loadTakedownLinked = loadLinkedPagesForTakedown,
  confirmTakedown,
  previewItem = previewEditorItem,
  loadPreviewLocation = fetchPreviewLocation,
  confirmUnsavedPreview,
  copyItem = createNewCopy,
  copyPromotable = createPromotableVersion,
  confirmCopy,
  copyItemToFolder = copyEditorItemToFolder,
  renameItem = async (itemPath: string, newName: string) => {
    await renameFolderItem({ itemPath, newName });
  },
  loadItemLocation = async (itemId: string) => {
    const item = await findItemById(itemId);
    const path = editorItemPathFromLookup(item);
    if (!path) {
      const missing = { status: 404, statusText: "Not Found", body: {} };
      throw missing;
    }
    return { path };
  },
  moveItem = moveEditorItemToFolder,
  resolveRecycleTarget = findItemById,
  recycleItem = deleteFolderItem,
  confirmRecycle,
  loadRevisions = fetchItemRevisions,
  restoreRevision = restoreItemRevision,
  compareRevisions = fetchItemRevisionCompare,
  confirmRestore,
  loadRelatedCanvas,
  loadRelatedLocal,
  loadContentTypes = listContentTypes,
  createItem = createEditorItem,
  loadTranslationVariants = listItemTranslationVariants,
  loadTranslationLocales = listLocales,
  createTranslationVariants = createTranslations,
  loadTemplates = loadPageTemplates,
  changeTemplate = changePageTemplate,
}: EditorHostProps = {}): React.ReactElement {
  const [params, setSearchParams] = useSearchParams();
  const contentId = parsePositiveInt(params.get("contentId"));
  const mode: EditorHostMode = normalizeEditorMode(params.get("mode"));
  const linkbackWarning = (params.get("warningMessage") ?? "").trim();
  const readOnly = mode === "view";
  const promote = mode === "promote";

  const [payload, setPayload] = useState<ItemEditorFields | null>(null);
  const [schema, setSchema] = useState<ContentTypeFieldSummary[]>([]);
  const [allowedTemplateCount, setAllowedTemplateCount] = useState(0);
  const [loadedAllowedTemplates, setLoadedAllowedTemplates] = useState<
    NamedObjectRef[] | null
  >(null);
  const [pageTemplateChoices, setPageTemplateChoices] = useState<PageTemplateChoice[]>(
    [],
  );
  const [pageTemplateId, setPageTemplateId] = useState("");
  const [pageTemplateBaseline, setPageTemplateBaseline] = useState("");
  const loadTemplatesRef = useRef(loadTemplates);
  loadTemplatesRef.current = loadTemplates;
  const loadItemLocationRef = useRef(loadItemLocation);
  loadItemLocationRef.current = loadItemLocation;
  const [draft, setDraft] = useState<Record<string, string>>({});
  const [pendingFiles, setPendingFiles] = useState<Record<string, File>>({});
  const [errorKey, setErrorKey] = useState<string | null>(null);
  const [errorDetail, setErrorDetail] = useState<string>(
    contentId == null ? linkbackWarning : "",
  );
  const [loading, setLoading] = useState(contentId != null && !promote);
  const [saving, setSaving] = useState(false);
  const [saved, setSaved] = useState(false);
  const [saveErrorKey, setSaveErrorKey] = useState<string | null>(null);
  const [saveErrorDetail, setSaveErrorDetail] = useState("");
  const [fieldErrors, setFieldErrors] = useState<Record<string, string>>({});
  const [workflowTriggers, setWorkflowTriggers] = useState<string[]>([]);
  const [workflowState, setWorkflowState] = useState<string>("");
  const [workflowComment, setWorkflowComment] = useState("");
  const [workflowErrorKey, setWorkflowErrorKey] = useState<string | null>(null);
  const [workflowErrorDetail, setWorkflowErrorDetail] = useState("");
  const [workflowBusy, setWorkflowBusy] = useState(false);
  const [workflowDone, setWorkflowDone] = useState(false);
  const [workflowChoices, setWorkflowChoices] = useState<
    { id: string; name: string }[]
  >([]);
  const [currentWorkflowId, setCurrentWorkflowId] = useState("");
  const [selectedWorkflowId, setSelectedWorkflowId] = useState("");
  const [workflowChanged, setWorkflowChanged] = useState(false);
  const [publishBusy, setPublishBusy] = useState(false);
  const [publishDone, setPublishDone] = useState(false);
  const [publishErrorKey, setPublishErrorKey] = useState<string | null>(null);
  const [publishErrorDetail, setPublishErrorDetail] = useState("");
  const [stageBusy, setStageBusy] = useState(false);
  const [stageDone, setStageDone] = useState(false);
  const [stageErrorKey, setStageErrorKey] = useState<string | null>(null);
  const [stageErrorDetail, setStageErrorDetail] = useState("");
  const [historyOpen, setHistoryOpen] = useState(false);
  const [takedownBusy, setTakedownBusy] = useState(false);
  const [takedownDone, setTakedownDone] = useState(false);
  const [takedownErrorKey, setTakedownErrorKey] = useState<string | null>(null);
  const [takedownErrorDetail, setTakedownErrorDetail] = useState("");
  const [previewBusy, setPreviewBusy] = useState(false);
  const [previewDone, setPreviewDone] = useState(false);
  const [previewErrorKey, setPreviewErrorKey] = useState<string | null>(null);
  const [previewErrorDetail, setPreviewErrorDetail] = useState("");
  const [previewTemplateId, setPreviewTemplateId] = useState(
    PREVIEW_TEMPLATE_CURRENT,
  );
  const [previewFrameUrl, setPreviewFrameUrl] = useState("");
  const [previewFrameError, setPreviewFrameError] = useState(false);
  const [copyBusy, setCopyBusy] = useState(false);
  const [copyErrorKey, setCopyErrorKey] = useState<string | null>(null);
  const [copyErrorDetail, setCopyErrorDetail] = useState("");
  const [copyFolderOpen, setCopyFolderOpen] = useState(false);
  const [copyFolderDefault, setCopyFolderDefault] = useState("");
  const [copyFolderDone, setCopyFolderDone] = useState("");
  const [renameName, setRenameName] = useState("");
  const [renameBusy, setRenameBusy] = useState(false);
  const [renameDone, setRenameDone] = useState(false);
  const [renameErrorKey, setRenameErrorKey] = useState<string | null>(null);
  const [renameErrorDetail, setRenameErrorDetail] = useState("");
  const [moveOpen, setMoveOpen] = useState(false);
  const [moveSourceParent, setMoveSourceParent] = useState("");
  const [moveBusy, setMoveBusy] = useState(false);
  const [moveDone, setMoveDone] = useState(false);
  const [moveErrorKey, setMoveErrorKey] = useState<string | null>(null);
  const [moveErrorDetail, setMoveErrorDetail] = useState("");
  const [recycleBusy, setRecycleBusy] = useState(false);
  const [recycleDone, setRecycleDone] = useState(false);
  const [recycleErrorKey, setRecycleErrorKey] = useState<string | null>(null);
  const [recycleErrorDetail, setRecycleErrorDetail] = useState("");
  const [sessionUser, setSessionUser] = useState("");
  const [lockUser, setLockUser] = useState("");
  /** User-info checkOutUser from the last checkout response (empty if omitted). */
  const [restLockUser, setRestLockUser] = useState("");
  const [lockBusy, setLockBusy] = useState(false);
  const [lockErrorKey, setLockErrorKey] = useState<string | null>(null);
  const [lockErrorDetail, setLockErrorDetail] = useState("");
  const [checkoutOk, setCheckoutOk] = useState(false);
  const [checkinPrompt, setCheckinPrompt] = useState(false);
  const [checkinComment, setCheckinComment] = useState("");
  const [restoreBusy, setRestoreBusy] = useState(false);
  const [restoreOpen, setRestoreOpen] = useState(false);
  const [restoreRevisions, setRestoreRevisions] = useState<ItemRevision[]>([]);
  const [restoreSelected, setRestoreSelected] = useState<number | "">("");
  const [restoreRestorable, setRestoreRestorable] = useState(false);
  const [restoreErrorKey, setRestoreErrorKey] = useState<string | null>(null);
  const [restoreErrorDetail, setRestoreErrorDetail] = useState("");
  const [restoreLoadErrorKey, setRestoreLoadErrorKey] = useState<string | null>(
    null,
  );
  const [restoreDone, setRestoreDone] = useState(false);
  const [compareLeft, setCompareLeft] = useState<number | "">("");
  const [compareRight, setCompareRight] = useState<number | "">("");
  const [compareBusy, setCompareBusy] = useState(false);
  const [compareResult, setCompareResult] = useState<ItemRevisionCompare | null>(
    null,
  );
  const [compareErrorKey, setCompareErrorKey] = useState<string | null>(null);
  const [compareErrorDetail, setCompareErrorDetail] = useState("");
  const [createOpen, setCreateOpen] = useState(contentId == null);
  const [createTypes, setCreateTypes] = useState<ContentTypeSummary[]>([]);
  const [createType, setCreateType] = useState("");
  const [createFolder, setCreateFolder] = useState("");
  const [createName, setCreateName] = useState("");
  const [createBusy, setCreateBusy] = useState(false);
  const [createErrorKey, setCreateErrorKey] = useState<string | null>(null);
  const [createErrorDetail, setCreateErrorDetail] = useState("");

  useEffect(() => {
    document.title = message(EDITOR_MSG.TITLE);
  }, []);

  useEffect(() => {
    if (contentId == null || promote) {
      return;
    }
    const itemId = String(contentId);
    let cancelled = false;
    setLoading(true);
    setErrorKey(null);
    setErrorDetail("");
    setLockErrorKey(null);
    setLockErrorDetail("");
    setCheckoutOk(false);
    setRestLockUser("");
    setLoadedAllowedTemplates(null);
    void (async () => {
      try {
        const fields = await loadFields(itemId);
        if (cancelled) {
          return;
        }
        setPayload(fields);
        setRenameName(editorListingName(fields));
        setLockUser(fields.checkoutUser ?? "");
        if (!readOnly) {
          try {
            const info = await checkout(itemId);
            if (!cancelled) {
              setCheckoutOk(true);
              if (info) {
                const nextLock = (info.checkOutUser ?? "").trim();
                const nextSession = (info.currentUser ?? "").trim();
                setRestLockUser(nextLock);
                if (nextLock) {
                  setLockUser(nextLock);
                }
                if (nextSession) {
                  setSessionUser(nextSession);
                }
              }
            }
          } catch (err) {
            if (!cancelled) {
              const reason = editorLockErrorReason(err);
              setLockErrorKey(
                reason === "forbidden"
                  ? EDITOR_MSG.CHECKOUT_FORBIDDEN
                  : reason === "conflict"
                    ? EDITOR_MSG.CHECKOUT_CONFLICT
                    : EDITOR_MSG.CHECKOUT_FAILED,
              );
              setLockErrorDetail(
                formatApiError(err, message(EDITOR_MSG.CHECKOUT_FAILED)),
              );
            }
          }
        }
        setDraft(
          Object.fromEntries(
            fields.fields.map((f) => [f.name, fieldValueAsString(f.value)]),
          ),
        );
        if (fields.contentType) {
          try {
            const detail = await loadType(fields.contentType);
            if (!cancelled) {
              setSchema(detail.fields ?? []);
              setAllowedTemplateCount(detail.allowedTemplates?.length ?? 0);
              setLoadedAllowedTemplates(
                (detail.allowedTemplates ?? []) as NamedObjectRef[],
              );
            }
          } catch {
            if (!cancelled) {
              setSchema([]);
              setAllowedTemplateCount(0);
              setLoadedAllowedTemplates([]);
            }
          }
        }
        if (!readOnly) {
          try {
            const trans = await loadTransitions(itemId);
            if (!cancelled) {
              setWorkflowTriggers(
                uniqueTransitionTriggers(trans.transitionTriggers),
              );
              setWorkflowState(trans.stateName ?? "");
              if (trans.workflowId) {
                setCurrentWorkflowId(trans.workflowId);
              }
            }
          } catch {
            if (!cancelled) {
              setWorkflowTriggers([]);
              setWorkflowState("");
            }
          }
          try {
            const catalog = await loadWorkflowChoices(itemId);
            if (!cancelled) {
              setWorkflowChoices(catalog.choices ?? []);
              const current = catalog.currentWorkflowId ?? "";
              if (current) {
                setCurrentWorkflowId(current);
              }
              setSelectedWorkflowId("");
            }
          } catch {
            if (!cancelled) {
              setWorkflowChoices([]);
            }
          }
        }
      } catch (err) {
        if (!cancelled) {
          setErrorDetail(err instanceof Error ? err.message : String(err));
          setErrorKey(EDITOR_MSG.LOAD_FAILED);
        }
      } finally {
        if (!cancelled) {
          setLoading(false);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [
    contentId,
    readOnly,
    promote,
    checkout,
    loadFields,
    loadType,
    loadTransitions,
    loadWorkflowChoices,
  ]);

  useEffect(() => {
    if (
      contentId == null ||
      payload == null ||
      !isExplorerPageType(payload.contentType)
    ) {
      setPageTemplateChoices([]);
      setPageTemplateId("");
      setPageTemplateBaseline("");
      return;
    }
    if (loadedAllowedTemplates == null) {
      return;
    }
    const fromType = templatesFromContentType(loadedAllowedTemplates);
    if (fromType.length > 0) {
      const selected = resolvePageTemplateSelection(
        pageTemplateIdFromFields(payload.fields),
        fromType,
      );
      setPageTemplateChoices(fromType);
      setPageTemplateId(selected);
      setPageTemplateBaseline(selected);
      return;
    }
    let cancelled = false;
    void (async () => {
      let folder = "";
      try {
        const loc = await loadItemLocationRef.current(String(contentId));
        folder = loc.path ?? "";
      } catch {
        folder = "";
      }
      if (!folder.trim()) {
        if (!cancelled) {
          setPageTemplateChoices([]);
          setPageTemplateId("");
          setPageTemplateBaseline("");
        }
        return;
      }
      try {
        const choices = await loadTemplatesRef.current(folder, payload.contentType);
        if (cancelled) {
          return;
        }
        const selected = resolvePageTemplateSelection(
          pageTemplateIdFromFields(payload.fields),
          choices,
        );
        setPageTemplateChoices(choices);
        setPageTemplateId(selected);
        setPageTemplateBaseline(selected);
      } catch {
        if (!cancelled) {
          setPageTemplateChoices([]);
          setPageTemplateId("");
          setPageTemplateBaseline("");
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [contentId, payload, loadedAllowedTemplates]);

  const rows = useMemo(() => {
    if (!payload) {
      return [];
    }
    return mergeEditorRows(
      {
        ...payload,
        fields: payload.fields.map((f) => ({
          name: f.name,
          value: fieldValueAsString(draft[f.name] ?? f.value),
        })),
      },
      schema,
    )
      .filter(
        (row) =>
          !(
            isExplorerPageType(payload.contentType) &&
            row.name.trim().toLowerCase() === "templateid"
          ),
      )
      .map((row) => ({
        ...row,
        value: fieldValueAsString(draft[row.name] ?? row.value),
      }));
  }, [payload, draft, schema]);

  function setField(name: string, value: string): void {
    setDraft((prev) => ({ ...prev, [name]: value }));
    setFieldErrors((prev) => {
      if (!(name in prev)) {
        return prev;
      }
      const next = { ...prev };
      delete next[name];
      return next;
    });
  }

  function setFile(name: string, file: File | null): void {
    setPendingFiles((prev) => {
      const next = { ...prev };
      if (file) {
        next[name] = file;
      } else {
        delete next[name];
      }
      return next;
    });
  }

  function requiredErrorsForDraft(): Record<string, string> {
    return collectRequiredFieldErrors(
      rows.map((row) => ({
        name: row.name,
        kind: row.kind,
        required: row.required,
        value: fieldValueAsString(draft[row.name] ?? row.value),
      })),
      pendingFiles,
      message(EDITOR_MSG.FIELD_REQUIRED),
    );
  }

  async function handleSave(): Promise<void> {
    if (contentId == null || payload == null) {
      return;
    }
    setSaving(true);
    setSaved(false);
    setSaveErrorKey(null);
    setSaveErrorDetail("");
    const missing = requiredErrorsForDraft();
    const invalidDates = collectInvalidDateFieldErrors(
      rows.map((row) => ({
        name: row.name,
        kind: row.kind,
        required: row.required,
        value: fieldValueAsString(draft[row.name] ?? row.value),
      })),
      message(EDITOR_MSG.FIELD_INVALID_DATE),
    );
    if (Object.keys(missing).length > 0) {
      setFieldErrors({ ...invalidDates, ...missing });
      setSaveErrorKey(EDITOR_MSG.REQUIRED_SAVE);
      setSaving(false);
      return;
    }
    if (Object.keys(invalidDates).length > 0) {
      setFieldErrors(invalidDates);
      setSaveErrorKey(EDITOR_MSG.INVALID_DATE_SAVE);
      setSaving(false);
      return;
    }
    const unsafeHtml = collectUnsafeHtmlFieldErrors(
      rows.map((row) => ({
        name: row.name,
        kind: row.kind,
        value: fieldValueAsString(draft[row.name] ?? row.value),
      })),
      message(EDITOR_MSG.HTML_UNSAFE),
    );
    if (Object.keys(unsafeHtml).length > 0) {
      setFieldErrors(unsafeHtml);
      setSaveErrorKey(EDITOR_MSG.HTML_INVALID_SAVE);
      setSaving(false);
      return;
    }
    const invalidLongText = collectInvalidLongTextFieldErrors(
      rows.map((row) => ({
        name: row.name,
        kind: row.kind,
        value: fieldValueAsString(draft[row.name] ?? row.value),
      })),
      message(EDITOR_MSG.LONGTEXT_INVALID),
    );
    if (Object.keys(invalidLongText).length > 0) {
      setFieldErrors(invalidLongText);
      setSaveErrorKey(EDITOR_MSG.LONGTEXT_INVALID_SAVE);
      setSaving(false);
      return;
    }
    const invalidNumbers = collectInvalidNumericFieldErrors(
      rows.map((row) => ({
        name: row.name,
        kind: row.kind,
        value: fieldValueAsString(draft[row.name] ?? row.value),
        numericInteger: row.numericInteger,
        numericMinimum: row.numericMinimum,
        numericMaximum: row.numericMaximum,
      })),
      message(EDITOR_MSG.NUMBER_INVALID),
      message(EDITOR_MSG.NUMBER_RANGE),
    );
    if (Object.keys(invalidNumbers).length > 0) {
      setFieldErrors(invalidNumbers);
      setSaveErrorKey(EDITOR_MSG.NUMBER_INVALID_SAVE);
      setSaving(false);
      return;
    }
    const invalidLinks = collectInvalidLinkFieldErrors(
      rows.map((row) => ({
        name: row.name,
        kind: row.kind,
        value: fieldValueAsString(draft[row.name] ?? row.value),
      })),
      message(EDITOR_MSG.LINK_INVALID),
    );
    if (Object.keys(invalidLinks).length > 0) {
      setFieldErrors(invalidLinks);
      setSaveErrorKey(EDITOR_MSG.LINK_INVALID_SAVE);
      setSaving(false);
      return;
    }
    setFieldErrors({});
    const imageFieldNames = new Set(
      rows.filter((row) => row.kind === "image").map((row) => row.name),
    );
    for (const [field, file] of Object.entries(pendingFiles)) {
      if (imageFieldNames.has(field) && !isImageFile(file)) {
        setSaveErrorKey(EDITOR_MSG.IMAGE_BAD_REQUEST);
        setSaveErrorDetail("");
        setSaving(false);
        return;
      }
    }
    try {
      const itemId = String(contentId);
      const next: ItemEditorFields = {
        ...payload,
        fields: rows
          .filter((row) => row.kind !== "file" && row.kind !== "image")
          .map((row) => ({
            name: row.name,
            value: fieldValueAsString(draft[row.name] ?? row.value),
            ...(row.kind === "number"
              ? {
                  dataType: row.numericInteger === false ? "float" : "integer",
                  minimum: row.numericMinimum,
                  maximum: row.numericMaximum,
                }
              : row.kind === "link"
                ? { dataType: "link" }
                : {}),
          })),
      };
      if (
        isExplorerPageType(payload.contentType) &&
        isAllowedPageTemplate(pageTemplateId, pageTemplateChoices)
      ) {
        next.fields = withPageTemplateField(next.fields, pageTemplateId);
      }
      const savedPayload = await saveFields(itemId, next);
      if (
        isExplorerPageType(payload.contentType) &&
        isAllowedPageTemplate(pageTemplateId, pageTemplateChoices) &&
        pageTemplateId !== pageTemplateBaseline
      ) {
        await changeTemplate(itemId, pageTemplateId);
        setPageTemplateBaseline(pageTemplateId);
      }
      try {
        for (const [field, file] of Object.entries(pendingFiles)) {
          await uploadBinary(itemId, field, file);
        }
      } catch (binErr) {
        if (isSessionRedirectError(binErr)) {
          return;
        }
        const binaryReason = editorBinaryErrorReason(binErr);
        const imageUpload = Object.keys(pendingFiles).some((name) =>
          imageFieldNames.has(name),
        );
        if (binaryReason === "forbidden") {
          setFieldErrors({});
          setSaveErrorKey(
            imageUpload ? EDITOR_MSG.IMAGE_FORBIDDEN : EDITOR_MSG.FILE_FORBIDDEN,
          );
          setSaveErrorDetail("");
          return;
        }
        if (binaryReason === "tooLarge") {
          setFieldErrors({});
          setSaveErrorKey(
            imageUpload ? EDITOR_MSG.IMAGE_TOO_LARGE : EDITOR_MSG.FILE_TOO_LARGE,
          );
          setSaveErrorDetail("");
          return;
        }
        if (binaryReason === "badRequest") {
          setFieldErrors({});
          setSaveErrorKey(
            imageUpload ? EDITOR_MSG.IMAGE_BAD_REQUEST : EDITOR_MSG.FILE_BAD_REQUEST,
          );
          setSaveErrorDetail("");
          return;
        }
        throw binErr;
      }
      setPendingFiles({});
      setPayload(savedPayload);
      setDraft(
        Object.fromEntries(
          savedPayload.fields.map((f) => [f.name, fieldValueAsString(f.value)]),
        ),
      );
      setSaved(true);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      if (editorSaveErrorReason(err) === "stale") {
        setFieldErrors({});
        setSaveErrorKey(EDITOR_MSG.SAVE_STALE);
        setSaveErrorDetail("");
        return;
      }
      const fallback = message(EDITOR_MSG.SAVE_FAILED);
      const mapped = mapSaveApiErrorToFieldErrors(
        err,
        rows.map((row) => row.name),
        fallback,
      );
      const htmlNames = rows
        .filter((row) => row.kind === "html")
        .map((row) => row.name);
      const namedHtml = Object.keys(mapped.fieldErrors).some((name) =>
        htmlNames.includes(name),
      );
      const html400 =
        editorSaveErrorReason(err) === "badRequest" &&
        (namedHtml ||
          (Object.keys(mapped.fieldErrors).length === 0 && htmlNames.length === 1));
      if (
        html400 &&
        Object.keys(mapped.fieldErrors).length === 0 &&
        htmlNames.length === 1
      ) {
        mapped.fieldErrors[htmlNames[0]] = message(EDITOR_MSG.HTML_BAD_REQUEST);
      }
      const longNames = rows
        .filter((row) => row.kind === "longtext")
        .map((row) => row.name);
      const namedLong = Object.keys(mapped.fieldErrors).some((name) =>
        longNames.includes(name),
      );
      const long400 =
        !html400 &&
        editorSaveErrorReason(err) === "badRequest" &&
        (namedLong ||
          (Object.keys(mapped.fieldErrors).length === 0 && longNames.length === 1));
      if (
        long400 &&
        Object.keys(mapped.fieldErrors).length === 0 &&
        longNames.length === 1
      ) {
        mapped.fieldErrors[longNames[0]] = message(EDITOR_MSG.LONGTEXT_BAD_REQUEST);
      }
      const numberNames = rows
        .filter((row) => row.kind === "number")
        .map((row) => row.name);
      const namedNumber = Object.keys(mapped.fieldErrors).some((name) =>
        numberNames.includes(name),
      );
      const number400 =
        !html400 &&
        !long400 &&
        editorSaveErrorReason(err) === "badRequest" &&
        (namedNumber ||
          (Object.keys(mapped.fieldErrors).length === 0 && numberNames.length === 1));
      if (
        number400 &&
        Object.keys(mapped.fieldErrors).length === 0 &&
        numberNames.length === 1
      ) {
        mapped.fieldErrors[numberNames[0]] = message(EDITOR_MSG.NUMBER_BAD_REQUEST);
      }
      const linkNames = rows
        .filter((row) => row.kind === "link")
        .map((row) => row.name);
      const namedLink = Object.keys(mapped.fieldErrors).some((name) =>
        linkNames.includes(name),
      );
      const saveReason = editorSaveErrorReason(err);
      const linkStatus =
        saveReason === "notFound" || saveReason === "forbidden" || saveReason === "badRequest";
      const linkMapped =
        !html400 &&
        !long400 &&
        !number400 &&
        linkStatus &&
        (namedLink ||
          (Object.keys(mapped.fieldErrors).length === 0 && linkNames.length === 1));
      if (linkMapped && Object.keys(mapped.fieldErrors).length === 0 && linkNames.length === 1) {
        mapped.fieldErrors[linkNames[0]] = message(
          saveReason === "notFound"
            ? EDITOR_MSG.LINK_NOT_FOUND
            : saveReason === "forbidden"
              ? EDITOR_MSG.LINK_FORBIDDEN
              : EDITOR_MSG.LINK_BAD_REQUEST,
        );
      }
      setFieldErrors(mapped.fieldErrors);
      setSaveErrorKey(
        html400
          ? EDITOR_MSG.HTML_BAD_REQUEST
          : long400
            ? EDITOR_MSG.LONGTEXT_BAD_REQUEST
            : number400
              ? EDITOR_MSG.NUMBER_BAD_REQUEST
              : linkMapped && saveReason === "notFound"
                ? EDITOR_MSG.LINK_NOT_FOUND
                : linkMapped && saveReason === "forbidden"
                  ? EDITOR_MSG.LINK_FORBIDDEN
                  : linkMapped
                    ? EDITOR_MSG.LINK_BAD_REQUEST
                    : EDITOR_MSG.SAVE_FAILED,
      );
      setSaveErrorDetail(mapped.banner === fallback ? "" : mapped.banner);
    } finally {
      setSaving(false);
    }
  }

  function workflowErrorFor(reason: string): string {
    if (reason === "unauthorized") {
      return EDITOR_MSG.WORKFLOW_UNAUTHORIZED;
    }
    if (reason === "comment") {
      return EDITOR_MSG.WORKFLOW_COMMENT_REQUIRED;
    }
    return EDITOR_MSG.WORKFLOW_FAILED;
  }

  async function handleTransition(trigger: string): Promise<void> {
    if (contentId == null) {
      return;
    }
    const gate = canRunEditorTransition({
      mode,
      trigger,
      allowed: workflowTriggers,
      comment: workflowComment,
      commentRequiredTriggers,
    });
    if (!gate.ok) {
      setWorkflowDone(false);
      setWorkflowErrorDetail("");
      setWorkflowErrorKey(workflowErrorFor(gate.reason));
      return;
    }
    setWorkflowBusy(true);
    setWorkflowDone(false);
    setWorkflowChanged(false);
    setWorkflowErrorKey(null);
    setWorkflowErrorDetail("");
    const itemId = String(contentId);
    const comment = workflowComment.trim();
    try {
      await runTransition(itemId, trigger, comment.length > 0 ? comment : undefined);
      setWorkflowComment("");
      if (!readOnly) {
        try {
          await checkout(itemId);
        } catch {
          // Transition already succeeded; stay on the host without a second checkout.
        }
        const trans = await loadTransitions(itemId);
        setWorkflowTriggers(uniqueTransitionTriggers(trans.transitionTriggers));
        setWorkflowState(trans.stateName ?? "");
      }
      setWorkflowDone(true);
    } catch (err) {
      setWorkflowErrorDetail(err instanceof Error ? err.message : String(err));
      setWorkflowErrorKey(EDITOR_MSG.WORKFLOW_FAILED);
    } finally {
      setWorkflowBusy(false);
    }
  }

  function workflowChangeErrorFor(reason: string): string {
    if (reason === "blank") {
      return EDITOR_MSG.WORKFLOW_CHANGE_EMPTY;
    }
    if (reason === "unchanged") {
      return EDITOR_MSG.WORKFLOW_CHANGE_UNCHANGED;
    }
    if (reason === "forbidden") {
      return EDITOR_MSG.WORKFLOW_CHANGE_FORBIDDEN;
    }
    return EDITOR_MSG.WORKFLOW_CHANGE_FAILED;
  }

  async function handleChangeWorkflow(): Promise<void> {
    if (contentId == null) {
      return;
    }
    const gate = canChangeEditorWorkflow({
      selectedId: selectedWorkflowId,
      currentId: currentWorkflowId,
      allowedIds: workflowChoices.map((choice) => choice.id),
    });
    if (!gate.ok) {
      setWorkflowChanged(false);
      setWorkflowDone(false);
      setWorkflowErrorDetail("");
      setWorkflowErrorKey(workflowChangeErrorFor(gate.reason));
      return;
    }
    setWorkflowBusy(true);
    setWorkflowChanged(false);
    setWorkflowDone(false);
    setWorkflowErrorKey(null);
    setWorkflowErrorDetail("");
    try {
      const trans = await changeWorkflow(String(contentId), gate.workflowId);
      setWorkflowTriggers(uniqueTransitionTriggers(trans.transitionTriggers));
      setWorkflowState(trans.stateName ?? "");
      const nextId = trans.workflowId ?? gate.workflowId;
      setCurrentWorkflowId(nextId);
      setSelectedWorkflowId("");
      setWorkflowChanged(true);
    } catch (err) {
      setWorkflowChanged(false);
      setWorkflowErrorDetail(err instanceof Error ? err.message : String(err));
      setWorkflowErrorKey(EDITOR_MSG.WORKFLOW_CHANGE_FAILED);
    } finally {
      setWorkflowBusy(false);
    }
  }

  async function handlePublish(): Promise<void> {
    if (contentId == null) {
      return;
    }
    const itemId = String(contentId);
    const kind = resolveEditorPublishKind(payload?.contentType, {
      id: itemId,
      allowedTemplateCount,
    });
    if (!canPublishFromEditor(mode, kind)) {
      setPublishDone(false);
      setPublishErrorDetail("");
      setPublishErrorKey(EDITOR_MSG.PUBLISH_UNAVAILABLE);
      return;
    }
    const confirmFn =
      confirmPublish ??
      ((body: string) =>
        typeof window !== "undefined" ? window.confirm(body) : false);
    if (!confirmFn(message(EDITOR_MSG.CONFIRM_PUBLISH_NOW))) {
      return;
    }
    setPublishBusy(true);
    setPublishDone(false);
    setPublishErrorKey(null);
    setPublishErrorDetail("");
    try {
      const published = await publishItem(itemId, kind);
      if (!published) {
        setPublishErrorKey(EDITOR_MSG.PUBLISH_UNAVAILABLE);
        return;
      }
      setPublishDone(true);
    } catch (err) {
      setPublishErrorDetail(formatApiError(err, message(EDITOR_MSG.PUBLISH_FAILED)));
      setPublishErrorKey(EDITOR_MSG.PUBLISH_FAILED);
    } finally {
      setPublishBusy(false);
    }
  }

  async function handleStage(): Promise<void> {
    if (contentId == null) {
      return;
    }
    const itemId = String(contentId);
    const kind = resolveEditorPublishKind(payload?.contentType, {
      id: itemId,
      allowedTemplateCount,
    });
    if (!canStageFromEditor(mode, kind)) {
      setStageDone(false);
      setStageErrorDetail("");
      setStageErrorKey(EDITOR_MSG.STAGE_UNAVAILABLE);
      return;
    }
    const confirmFn =
      confirmStage ??
      ((body: string) =>
        typeof window !== "undefined" ? window.confirm(body) : false);
    if (!confirmFn(message(EDITOR_MSG.CONFIRM_STAGE))) {
      return;
    }
    setStageBusy(true);
    setStageDone(false);
    setStageErrorKey(null);
    setStageErrorDetail("");
    try {
      const staged = await stageItem(itemId, kind);
      if (!staged) {
        setStageErrorKey(EDITOR_MSG.STAGE_UNAVAILABLE);
        return;
      }
      setStageDone(true);
    } catch (err) {
      setStageErrorDetail(formatApiError(err, message(EDITOR_MSG.STAGE_FAILED)));
      setStageErrorKey(EDITOR_MSG.STAGE_FAILED);
    } finally {
      setStageBusy(false);
    }
  }

  async function handleTakedown(): Promise<void> {
    if (contentId == null) {
      return;
    }
    const itemId = String(contentId);
    const kind = resolveEditorPublishKind(payload?.contentType, {
      id: itemId,
      allowedTemplateCount,
    });
    if (!canTakedownFromEditor(mode, kind)) {
      setTakedownDone(false);
      setTakedownErrorDetail("");
      setTakedownErrorKey(EDITOR_MSG.TAKEDOWN_UNAVAILABLE);
      return;
    }
    const linked = await loadTakedownLinked(itemId);
    const confirmFn =
      confirmTakedown ??
      ((body: string) =>
        typeof window !== "undefined" ? window.confirm(body) : false);
    if (!confirmFn(formatEditorTakedownConfirm(linked))) {
      return;
    }
    setTakedownBusy(true);
    setTakedownDone(false);
    setTakedownErrorKey(null);
    setTakedownErrorDetail("");
    try {
      const takenDown = await takedownItem(itemId, kind, linked);
      if (!takenDown) {
        setTakedownErrorKey(EDITOR_MSG.TAKEDOWN_UNAVAILABLE);
        return;
      }
      setTakedownDone(true);
    } catch (err) {
      setTakedownErrorDetail(
        formatApiError(err, message(EDITOR_MSG.TAKEDOWN_FAILED)),
      );
      setTakedownErrorKey(EDITOR_MSG.TAKEDOWN_FAILED);
    } finally {
      setTakedownBusy(false);
    }
  }

  async function handlePreview(): Promise<void> {
    if (contentId == null) {
      return;
    }
    const itemId = String(contentId);
    const kind = resolveEditorPublishKind(payload?.contentType, {
      id: itemId,
      allowedTemplateCount,
    });
    if (!canPreviewFromEditor(mode, kind)) {
      setPreviewDone(false);
      setPreviewErrorDetail("");
      setPreviewErrorKey(EDITOR_MSG.PREVIEW_UNAVAILABLE);
      return;
    }
    if (editorDraftIsDirty(payload?.fields, draft, pendingFiles)) {
      const confirmFn =
        confirmUnsavedPreview ??
        ((body: string) =>
          typeof window !== "undefined" ? window.confirm(body) : false);
      if (!confirmFn(message(EDITOR_MSG.CONFIRM_PREVIEW_UNSAVED))) {
        return;
      }
    }
    setPreviewBusy(true);
    setPreviewDone(false);
    setPreviewErrorKey(null);
    setPreviewErrorDetail("");
    try {
      await previewItem(itemId, kind);
      setPreviewDone(true);
      if (positivePreviewTemplateId(previewTemplateId) == null) {
        setPreviewFrameUrl(editorDefaultPreviewFrameUrl(itemId, kind));
      }
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setPreviewErrorDetail(formatApiError(err, message(EDITOR_MSG.PREVIEW_FAILED)));
      setPreviewErrorKey(EDITOR_MSG.PREVIEW_FAILED);
    } finally {
      setPreviewBusy(false);
    }
  }

  async function applyPreviewTemplate(nextId: string): Promise<void> {
    setPreviewTemplateId(nextId);
    setPreviewFrameError(false);
    if (contentId == null) {
      setPreviewFrameUrl("");
      return;
    }
    const kind = resolveEditorPublishKind(payload?.contentType, {
      id: String(contentId),
      allowedTemplateCount,
    });
    const templateId = positivePreviewTemplateId(nextId);
    if (templateId == null) {
      setPreviewFrameUrl(editorDefaultPreviewFrameUrl(String(contentId), kind));
      return;
    }
    try {
      const loc = await loadPreviewLocation(contentId, templateId);
      setPreviewFrameUrl(editorChosenTemplateFrameUrl(loc.previewUrl));
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setPreviewFrameUrl("");
      setPreviewFrameError(true);
    }
  }

  function copyErrorKeyFor(reason: ReturnType<typeof editorCopyErrorReason>): string {
    if (reason === "bad_request") {
      return EDITOR_MSG.COPY_BAD_REQUEST;
    }
    if (reason === "forbidden") {
      return EDITOR_MSG.COPY_FORBIDDEN;
    }
    if (reason === "not_found") {
      return EDITOR_MSG.COPY_NOT_FOUND;
    }
    return EDITOR_MSG.COPY_FAILED;
  }

  async function handleCopy(kind: EditorCopyKind): Promise<void> {
    if (contentId == null) {
      return;
    }
    if (!canCopyFromEditor(mode)) {
      setCopyErrorDetail("");
      setCopyErrorKey(EDITOR_MSG.COPY_UNAVAILABLE);
      return;
    }
    const confirmFn =
      confirmCopy ??
      ((body: string) =>
        typeof window !== "undefined" ? window.confirm(body) : false);
    const confirmKey =
      kind === "promotable"
        ? EDITOR_MSG.CONFIRM_PROMOTABLE
        : EDITOR_MSG.CONFIRM_NEW_COPY;
    if (!confirmFn(message(confirmKey))) {
      return;
    }
    setCopyBusy(true);
    setCopyErrorKey(null);
    setCopyErrorDetail("");
    const itemId = String(contentId);
    try {
      const result =
        kind === "promotable"
          ? await copyPromotable(itemId)
          : await copyItem(itemId);
      const nextId = parseCopyLandingContentId(result.itemId);
      if (nextId == null) {
        setCopyErrorKey(EDITOR_MSG.COPY_FAILED);
        setCopyErrorDetail("Copy result was missing item id");
        return;
      }
      const next = new URLSearchParams(params);
      next.set("contentId", String(nextId));
      next.set("mode", "edit");
      setSearchParams(next);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = editorCopyErrorReason(err);
      setCopyErrorKey(copyErrorKeyFor(reason));
      setCopyErrorDetail(formatApiError(err, message(copyErrorKeyFor(reason))));
    } finally {
      setCopyBusy(false);
    }
  }

  async function handleCopyToFolderOpen(): Promise<void> {
    if (contentId == null) {
      return;
    }
    if (!canCopyFromEditor(mode)) {
      setCopyErrorDetail("");
      setCopyErrorKey(EDITOR_MSG.COPY_UNAVAILABLE);
      return;
    }
    setCopyBusy(true);
    setCopyFolderDone("");
    setCopyErrorKey(null);
    setCopyErrorDetail("");
    try {
      const located = await loadItemLocation(String(contentId));
      const parent = parentFolderOfItemPath(located.path);
      if (!parent) {
        setCopyErrorKey(EDITOR_MSG.COPY_NOT_FOUND);
        return;
      }
      setCopyFolderDefault(parent);
      setCopyFolderOpen(true);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = editorCopyErrorReason(err);
      setCopyErrorKey(copyErrorKeyFor(reason));
      setCopyErrorDetail(formatApiError(err, message(copyErrorKeyFor(reason))));
    } finally {
      setCopyBusy(false);
    }
  }

  async function handleCopyToFolderPick(targetFolderPath: string): Promise<void> {
    setCopyFolderOpen(false);
    if (contentId == null || !canCopyFromEditor(mode)) {
      return;
    }
    const target = targetFolderPath.trim();
    if (!target) {
      setCopyFolderDone("");
      setCopyErrorKey(EDITOR_MSG.COPY_BAD_REQUEST);
      setCopyErrorDetail("");
      return;
    }
    setCopyBusy(true);
    setCopyFolderDone("");
    setCopyErrorKey(null);
    setCopyErrorDetail("");
    try {
      const located = await loadItemLocation(String(contentId));
      const source = String(located.path ?? "").trim();
      if (!source) {
        setCopyErrorKey(EDITOR_MSG.COPY_NOT_FOUND);
        return;
      }
      await copyItemToFolder(source, target);
      setCopyFolderDone(target);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = editorCopyErrorReason(err);
      setCopyErrorKey(copyErrorKeyFor(reason));
      setCopyErrorDetail(formatApiError(err, message(copyErrorKeyFor(reason))));
      setCopyFolderDone("");
    } finally {
      setCopyBusy(false);
    }
  }

  function renameErrorKeyFor(
    reason: ReturnType<typeof editorRenameErrorReason>,
  ): string {
    if (reason === "bad_request") {
      return EDITOR_MSG.RENAME_BAD_REQUEST;
    }
    if (reason === "forbidden") {
      return EDITOR_MSG.RENAME_FORBIDDEN;
    }
    if (reason === "not_found") {
      return EDITOR_MSG.RENAME_NOT_FOUND;
    }
    return EDITOR_MSG.RENAME_FAILED;
  }

  async function handleRename(): Promise<void> {
    if (contentId == null) {
      return;
    }
    if (!canRenameFromEditor(mode)) {
      setRenameDone(false);
      setRenameErrorDetail("");
      setRenameErrorKey(EDITOR_MSG.RENAME_UNAVAILABLE);
      return;
    }
    const wanted = renameName.trim();
    if (!wanted || wanted.includes("/") || wanted.includes("\\")) {
      setRenameDone(false);
      setRenameErrorDetail("");
      setRenameErrorKey(EDITOR_MSG.RENAME_BAD_REQUEST);
      return;
    }
    setRenameBusy(true);
    setRenameDone(false);
    setRenameErrorKey(null);
    setRenameErrorDetail("");
    const itemId = String(contentId);
    try {
      const located = await loadItemLocation(itemId);
      const itemPath = String(located?.path ?? "").trim();
      if (!itemPath) {
        setRenameErrorKey(EDITOR_MSG.RENAME_NO_FOLDER);
        return;
      }
      await renameItem(itemPath, wanted);
      const refreshed = await loadFields(itemId);
      const loaded = editorListingName(refreshed);
      if (!renameLanded(wanted, loaded)) {
        setRenameErrorKey(EDITOR_MSG.RENAME_FAILED);
        return;
      }
      setPayload(refreshed);
      setRenameName(loaded);
      setDraft((prev) => ({ ...prev, sys_title: loaded }));
      setRenameDone(true);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = editorRenameErrorReason(err);
      const errorKey = renameErrorKeyFor(reason);
      setRenameErrorKey(errorKey);
      setRenameErrorDetail(formatApiError(err, message(errorKey)));
      setRenameDone(false);
    } finally {
      setRenameBusy(false);
    }
  }

  function moveErrorKeyFor(
    reason: ReturnType<typeof editorMoveErrorReason>,
  ): string {
    if (reason === "forbidden") {
      return EDITOR_MSG.MOVE_FORBIDDEN;
    }
    if (reason === "not_found") {
      return EDITOR_MSG.MOVE_NOT_FOUND;
    }
    if (reason === "conflict") {
      return EDITOR_MSG.MOVE_CONFLICT;
    }
    return EDITOR_MSG.MOVE_FAILED;
  }

  async function handleMoveOpen(): Promise<void> {
    if (contentId == null) {
      return;
    }
    if (!canMoveFromEditor(mode)) {
      setMoveErrorDetail("");
      setMoveErrorKey(EDITOR_MSG.MOVE_UNAVAILABLE);
      return;
    }
    setMoveBusy(true);
    setMoveDone(false);
    setMoveErrorKey(null);
    setMoveErrorDetail("");
    try {
      const located = await loadItemLocation(String(contentId));
      const parent = parentFolderOfItemPath(located.path);
      if (!parent) {
        setMoveErrorKey(EDITOR_MSG.MOVE_NOT_FOUND);
        setMoveErrorDetail("");
        return;
      }
      setMoveSourceParent(parent);
      setMoveOpen(true);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = editorMoveErrorReason(err);
      setMoveErrorKey(moveErrorKeyFor(reason));
      setMoveErrorDetail(formatApiError(err, message(moveErrorKeyFor(reason))));
    } finally {
      setMoveBusy(false);
    }
  }

  async function handleMovePick(targetFolderPath: string): Promise<void> {
    setMoveOpen(false);
    if (contentId == null || !canMoveFromEditor(mode)) {
      return;
    }
    const target = targetFolderPath.trim();
    if (!target) {
      return;
    }
    if (cmsFoldersEqual(moveSourceParent, target)) {
      setMoveErrorDetail("");
      setMoveErrorKey(EDITOR_MSG.MOVE_SAME_FOLDER);
      return;
    }
    setMoveBusy(true);
    setMoveDone(false);
    setMoveErrorKey(null);
    setMoveErrorDetail("");
    try {
      const located = await loadItemLocation(String(contentId));
      const source = String(located.path ?? "").trim();
      const parent = parentFolderOfItemPath(source);
      if (!source || !parent) {
        setMoveErrorKey(EDITOR_MSG.MOVE_NOT_FOUND);
        return;
      }
      if (cmsFoldersEqual(parent, target)) {
        setMoveErrorKey(EDITOR_MSG.MOVE_SAME_FOLDER);
        return;
      }
      await moveItem(source, target);
      setMoveSourceParent(target);
      setMoveDone(true);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = editorMoveErrorReason(err);
      setMoveErrorKey(moveErrorKeyFor(reason));
      setMoveErrorDetail(formatApiError(err, message(moveErrorKeyFor(reason))));
    } finally {
      setMoveBusy(false);
    }
  }

  function recycleErrorKeyFor(
    reason: ReturnType<typeof editorRecycleErrorReason> | "folder",
  ): string {
    if (reason === "forbidden") {
      return EDITOR_MSG.RECYCLE_FORBIDDEN;
    }
    if (reason === "not_found") {
      return EDITOR_MSG.RECYCLE_NOT_FOUND;
    }
    if (reason === "conflict") {
      return EDITOR_MSG.RECYCLE_CONFLICT;
    }
    if (reason === "folder") {
      return EDITOR_MSG.RECYCLE_FOLDER;
    }
    return EDITOR_MSG.RECYCLE_FAILED;
  }

  async function handleRecycle(): Promise<void> {
    if (contentId == null || !canRecycleFromEditor(mode)) {
      setRecycleErrorDetail("");
      setRecycleErrorKey(EDITOR_MSG.RECYCLE_UNAVAILABLE);
      return;
    }
    const confirmFn =
      confirmRecycle ??
      ((body: string) =>
        typeof window !== "undefined" ? window.confirm(body) : false);
    if (!confirmFn(message(EDITOR_MSG.CONFIRM_RECYCLE))) {
      return;
    }
    setRecycleBusy(true);
    setRecycleDone(false);
    setRecycleErrorKey(null);
    setRecycleErrorDetail("");
    const itemId = String(contentId);
    try {
      const target = await resolveRecycleTarget(itemId);
      const resolved = editorRecycleItemPath(target);
      if (!resolved.ok) {
        setRecycleErrorKey(recycleErrorKeyFor(resolved.reason));
        setRecycleErrorDetail("");
        return;
      }
      await recycleItem(resolved.path);
      setRecycleDone(true);
      const next = new URLSearchParams(params);
      next.delete("contentId");
      next.set("mode", "view");
      next.delete("warningMessage");
      setSearchParams(next);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = editorRecycleErrorReason(err);
      setRecycleErrorKey(recycleErrorKeyFor(reason));
      setRecycleErrorDetail(
        formatApiError(err, message(recycleErrorKeyFor(reason))),
      );
    } finally {
      setRecycleBusy(false);
    }
  }

  function createErrorKeyFor(
    reason: ReturnType<typeof editorCreateErrorReason>,
  ): string {
    if (reason === "forbidden") {
      return EDITOR_MSG.CREATE_FORBIDDEN;
    }
    if (reason === "not_found") {
      return EDITOR_MSG.CREATE_NOT_FOUND;
    }
    if (reason === "bad_request") {
      return EDITOR_MSG.CREATE_BAD_REQUEST;
    }
    return EDITOR_MSG.CREATE_FAILED;
  }

  useEffect(() => {
    if (!createOpen || !canCreateFromEditor(mode)) {
      return;
    }
    let cancelled = false;
    void (async () => {
      try {
        const types = await loadContentTypes();
        if (!cancelled) {
          setCreateTypes(types);
        }
      } catch {
        if (!cancelled) {
          setCreateTypes([]);
        }
      }
    })();
    return () => {
      cancelled = true;
    };
  }, [createOpen, mode, loadContentTypes]);

  async function handleCreate(): Promise<void> {
    if (!canCreateFromEditor(mode)) {
      setCreateErrorDetail("");
      setCreateErrorKey(EDITOR_MSG.CREATE_UNAVAILABLE);
      return;
    }
    const req = buildEditorCreateRequest(createType, createFolder, createName);
    if (req == null) {
      setCreateErrorDetail("");
      setCreateErrorKey(EDITOR_MSG.CREATE_INCOMPLETE);
      return;
    }
    setCreateBusy(true);
    setCreateErrorKey(null);
    setCreateErrorDetail("");
    try {
      const result = await createItem(req);
      const nextId = parseCreateLandingContentId(result.itemId);
      if (nextId == null) {
        setCreateErrorKey(EDITOR_MSG.CREATE_FAILED);
        setCreateErrorDetail("Create result was missing item id");
        return;
      }
      const next = new URLSearchParams(params);
      next.set("contentId", String(nextId));
      next.set("mode", "edit");
      next.delete("warningMessage");
      setCreateOpen(false);
      setSearchParams(next);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = editorCreateErrorReason(err);
      setCreateErrorKey(createErrorKeyFor(reason));
      setCreateErrorDetail(
        formatApiError(err, message(createErrorKeyFor(reason))),
      );
    } finally {
      setCreateBusy(false);
    }
  }

  function lockErrorKeyFor(
    reason: ReturnType<typeof editorLockErrorReason>,
    kind: "in" | "out" | "force",
  ): string {
    if (kind === "force") {
      if (reason === "forbidden") {
        return EDITOR_MSG.FORCE_CHECKIN_FORBIDDEN;
      }
      if (reason === "not_found") {
        return EDITOR_MSG.FORCE_CHECKIN_NOT_FOUND;
      }
      if (reason === "conflict") {
        return EDITOR_MSG.FORCE_CHECKIN_CONFLICT;
      }
      return EDITOR_MSG.FORCE_CHECKIN_FAILED;
    }
    if (reason === "forbidden") {
      return kind === "in" ? EDITOR_MSG.CHECKIN_FORBIDDEN : EDITOR_MSG.CHECKOUT_FORBIDDEN;
    }
    if (reason === "conflict") {
      return kind === "in" ? EDITOR_MSG.CHECKIN_CONFLICT : EDITOR_MSG.CHECKOUT_CONFLICT;
    }
    return kind === "in" ? EDITOR_MSG.CHECKIN_FAILED : EDITOR_MSG.CHECKOUT_FAILED;
  }

  async function handleCheckout(): Promise<void> {
    if (contentId == null || !canUseEditorCheckoutActions(mode)) {
      return;
    }
    setLockBusy(true);
    setLockErrorKey(null);
    setLockErrorDetail("");
    try {
      const info = await checkout(String(contentId));
      setCheckoutOk(true);
      const nextLock = (info?.checkOutUser ?? "").trim();
      const nextSession = (info?.currentUser ?? "").trim();
      setRestLockUser(nextLock);
      if (nextLock) {
        setLockUser(nextLock);
      }
      if (nextSession) {
        setSessionUser(nextSession);
      }
      if (
        !isCheckedOutToSelf(
          nextLock,
          nextSession,
          payload?.checkoutUser,
          true,
        )
      ) {
        setLockErrorKey(EDITOR_MSG.CHECKOUT_CONFLICT);
      }
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = editorLockErrorReason(err);
      setLockErrorKey(lockErrorKeyFor(reason, "out"));
      setLockErrorDetail(formatApiError(err, message(lockErrorKeyFor(reason, "out"))));
    } finally {
      setLockBusy(false);
    }
  }

  async function handleCheckin(): Promise<void> {
    if (contentId == null) {
      return;
    }
    const missing = requiredErrorsForDraft();
    if (Object.keys(missing).length > 0) {
      setFieldErrors(missing);
      setSaveErrorKey(EDITOR_MSG.REQUIRED_CHECKIN);
      setSaveErrorDetail("");
      return;
    }
    setCheckinComment("");
    setCheckinPrompt(true);
  }

  function cancelCheckin(): void {
    setCheckinPrompt(false);
    setCheckinComment("");
  }

  async function confirmCheckin(): Promise<void> {
    if (contentId == null) {
      return;
    }
    const comment = checkinComment.trim();
    setLockBusy(true);
    setLockErrorKey(null);
    setLockErrorDetail("");
    try {
      await checkin(String(contentId), comment.length > 0 ? comment : undefined);
      setCheckinPrompt(false);
      setCheckinComment("");
      setLockUser("");
      if (typeof window !== "undefined") {
        window.close();
      }
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = editorLockErrorReason(err);
      setLockErrorKey(lockErrorKeyFor(reason, "in"));
      setLockErrorDetail(formatApiError(err, message(lockErrorKeyFor(reason, "in"))));
    } finally {
      setLockBusy(false);
    }
  }

  async function handleForceCheckin(): Promise<void> {
    const owner = (restLockUser || lockUser || payload?.checkoutUser || "").trim();
    if (
      contentId == null ||
      !canForceCheckInFromEditor(mode, owner, sessionUser)
    ) {
      return;
    }
    const confirmFn =
      confirmForceCheckin ??
      ((body: string) =>
        typeof window !== "undefined" ? window.confirm(body) : false);
    if (!confirmFn(message(EDITOR_MSG.CONFIRM_FORCE_CHECKIN))) {
      return;
    }
    setLockBusy(true);
    setLockErrorKey(null);
    setLockErrorDetail("");
    try {
      await forceCheckin(String(contentId));
      setRestLockUser("");
      setLockUser("");
      setCheckoutOk(false);
      setPayload((prev) => (prev ? { ...prev, checkoutUser: "" } : prev));
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = editorLockErrorReason(err);
      const errorKey = lockErrorKeyFor(reason, "force");
      setLockErrorKey(errorKey);
      setLockErrorDetail(formatApiError(err, message(errorKey)));
    } finally {
      setLockBusy(false);
    }
  }

  function restoreErrorKeyFor(
    reason: ReturnType<typeof editorRevisionErrorReason>,
  ): string {
    if (reason === "forbidden") {
      return EDITOR_MSG.RESTORE_FORBIDDEN;
    }
    if (reason === "not_found") {
      return EDITOR_MSG.RESTORE_NOT_FOUND;
    }
    return EDITOR_MSG.RESTORE_FAILED;
  }

  async function handleRestoreOpen(): Promise<void> {
    if (contentId == null || !canRestoreFromEditor(mode)) {
      return;
    }
    setRestoreOpen((prev) => !prev);
    setRestoreLoadErrorKey(null);
    setRestoreErrorKey(null);
    setRestoreErrorDetail("");
    setRestoreDone(false);
    setCompareResult(null);
    setCompareErrorKey(null);
    setCompareErrorDetail("");
    if (restoreOpen) {
      return;
    }
    setRestoreBusy(true);
    try {
      const summary = await loadRevisions(String(contentId));
      if (summary.revisions.length > 0) {
        setRestoreRevisions(summary.revisions);
        setRestoreRestorable(summary.restorable);
        setRestoreSelected(summary.revisions[0].revId);
        const ids = summary.revisions
          .map((rev) => rev.revId)
          .filter((id) => Number.isFinite(id))
          .sort((a, b) => a - b);
        if (ids.length >= 2) {
          setCompareLeft(ids[0] ?? "");
          setCompareRight(ids[ids.length - 1] ?? "");
        } else {
          setCompareLeft(ids[0] ?? "");
          setCompareRight(ids[0] ?? "");
        }
      } else {
        setRestoreRevisions([]);
        setRestoreRestorable(false);
        setRestoreSelected("");
        setCompareLeft("");
        setCompareRight("");
      }
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      setRestoreLoadErrorKey(EDITOR_MSG.RESTORE_LOAD_FAILED);
      setRestoreRevisions([]);
      setRestoreRestorable(false);
      setRestoreSelected("");
      setCompareLeft("");
      setCompareRight("");
    } finally {
      setRestoreBusy(false);
    }
  }

  function compareErrorKeyFor(
    reason: ReturnType<typeof editorRevisionErrorReason>,
  ): string {
    if (reason === "forbidden") {
      return EDITOR_MSG.COMPARE_FORBIDDEN;
    }
    if (reason === "not_found") {
      return EDITOR_MSG.COMPARE_NOT_FOUND;
    }
    return EDITOR_MSG.COMPARE_FAILED;
  }

  async function handleCompareRevisions(): Promise<void> {
    if (contentId == null || !canRestoreFromEditor(mode)) {
      return;
    }
    const selection = revisionCompareSelection(compareLeft, compareRight);
    setCompareResult(null);
    if (!selection.ok) {
      setCompareErrorKey(
        selection.reason === "same"
          ? EDITOR_MSG.COMPARE_SAME
          : EDITOR_MSG.COMPARE_NEED_TWO,
      );
      setCompareErrorDetail("");
      return;
    }
    setCompareBusy(true);
    setCompareErrorKey(null);
    setCompareErrorDetail("");
    try {
      const result = await compareRevisions(
        String(contentId),
        selection.left,
        selection.right,
      );
      setCompareResult(result);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = editorRevisionErrorReason(err);
      const errorKey = compareErrorKeyFor(reason);
      setCompareErrorKey(errorKey);
      setCompareErrorDetail(formatApiError(err, message(errorKey)));
    } finally {
      setCompareBusy(false);
    }
  }

  async function handleRestoreConfirm(): Promise<void> {
    if (contentId == null) {
      return;
    }
    if (!canRestoreFromEditor(mode)) {
      setRestoreErrorKey(EDITOR_MSG.RESTORE_UNAVAILABLE);
      setRestoreErrorDetail("");
      return;
    }
    const revId = parseRevisionId(restoreSelected);
    if (revId == null || !restoreRestorable) {
      setRestoreErrorKey(EDITOR_MSG.RESTORE_FAILED);
      setRestoreErrorDetail("");
      return;
    }
    const selectedRev = restoreRevisions.find((rev) => rev.revId === revId);
    if (!selectedRev) {
      setRestoreErrorKey(EDITOR_MSG.RESTORE_FAILED);
      setRestoreErrorDetail("");
      return;
    }
    const confirmFn =
      confirmRestore ??
      ((body: string) =>
        typeof window !== "undefined" ? window.confirm(body) : false);
    const body = restoreRevisionConfirmBody(
      summarizeRevisionRow(selectedRev),
      payload?.name ?? "",
    );
    if (!confirmFn(`${message(EDITOR_MSG.RESTORE_CONFIRM)} ${body}`)) {
      return;
    }
    setRestoreBusy(true);
    setRestoreErrorKey(null);
    setRestoreErrorDetail("");
    setRestoreDone(false);
    try {
      const itemId = String(contentId);
      await restoreRevision(itemId, revId);
      const refreshed = await loadFields(itemId);
      setPayload(refreshed);
      setDraft(
        Object.fromEntries(
          refreshed.fields.map((f) => [f.name, fieldValueAsString(f.value)]),
        ),
      );
      setPendingFiles({});
      setFieldErrors({});
      setSaveErrorKey(null);
      setSaveErrorDetail("");
      setRestoreDone(true);
      setRestoreOpen(false);
    } catch (err) {
      if (isSessionRedirectError(err)) {
        return;
      }
      const reason = editorRevisionErrorReason(err);
      const errorKey = restoreErrorKeyFor(reason);
      setRestoreErrorKey(errorKey);
      setRestoreErrorDetail(formatApiError(err, message(errorKey)));
    } finally {
      setRestoreBusy(false);
    }
  }

  const heldBySelf = isCheckedOutToSelf(
    restLockUser,
    sessionUser,
    payload?.checkoutUser,
    checkoutOk,
  );
  const canEdit = !readOnly && !promote && heldBySelf;
  const checkoutOwner = (
    restLockUser ||
    lockUser ||
    payload?.checkoutUser ||
    ""
  ).trim();
  const showCheckoutAction =
    canUseEditorCheckoutActions(mode) && contentId != null && !heldBySelf;
  const showForceCheckin = canForceCheckInFromEditor(
    mode,
    checkoutOwner,
    sessionUser,
  );
  const publishKind = resolveEditorPublishKind(payload?.contentType, {
    id: contentId != null ? String(contentId) : "",
    allowedTemplateCount,
  });
  const showPublish = canPublishFromEditor(mode, publishKind);
  const showStage = canStageFromEditor(mode, publishKind);
  const showTakedown = canTakedownFromEditor(mode, publishKind);
  const showPublishHistory = canViewPublishHistoryFromEditor(mode, publishKind);
  const showPreview = canPreviewFromEditor(mode, publishKind);
  const previewChoices = useMemo(
    () =>
      numericPreviewTemplates(
        pageTemplateChoices.length > 0
          ? pageTemplateChoices
          : templatesFromContentType(loadedAllowedTemplates ?? undefined),
      ),
    [pageTemplateChoices, loadedAllowedTemplates],
  );
  const showCopy = canCopyFromEditor(mode) && contentId != null;
  const showRename = canRenameFromEditor(mode) && contentId != null;
  const showMove = canMoveFromEditor(mode) && contentId != null;
  const showRecycle = canRecycleFromEditor(mode) && contentId != null;
  const showRestore = canRestoreFromEditor(mode) && contentId != null;
  const showCreate = canCreateFromEditor(mode);

  return (
    <div className={styles.root} data-testid="editor-host">
      <header className={styles.bar} data-testid="editor-overlay">
        <span className={styles.title}>{message(EDITOR_MSG.TITLE)}</span>
        <span className={styles.badge}>{message(badgeKey(mode))}</span>
        {contentId != null ? (
          <span className={styles.meta} data-testid="editor-content-id">
            {message(EDITOR_MSG.CONTENT_ID)} {contentId}
          </span>
        ) : null}
        {payload?.contentType ? (
          <span className={styles.meta} data-testid="editor-content-type">
            {message(EDITOR_MSG.TYPE_LABEL)} {payload.contentType}
          </span>
        ) : null}
        {pageTemplateChoices.length > 0 ? (
          <label className={styles.meta} data-testid="editor-page-template-label">
            {message(EDITOR_MSG.PAGE_TEMPLATE)}
            <select
              className={styles.input}
              data-testid="editor-page-template"
              value={pageTemplateId}
              disabled={
                !canEdit || saving || loading || pageTemplateChoices.length < 2
              }
              onChange={(e) => setPageTemplateId(e.target.value)}
            >
              {pageTemplateChoices.map((choice) => (
                <option key={choice.id} value={choice.id}>
                  {choice.name || choice.id}
                </option>
              ))}
            </select>
          </label>
        ) : null}
        {(lockUser || payload?.checkoutUser) ? (
          <span className={styles.meta} data-testid="editor-checkout-user">
            {message(EDITOR_MSG.CHECKOUT)} {lockUser || payload?.checkoutUser}
          </span>
        ) : null}
        <div className={styles.actions}>
          {saved ? (
            <span className={styles.meta} data-testid="editor-saved">
              {message(EDITOR_MSG.SAVED)}
            </span>
          ) : null}
          {workflowDone ? (
            <span className={styles.meta} data-testid="editor-workflow-done">
              {message(EDITOR_MSG.WORKFLOW_DONE)}
            </span>
          ) : null}
          {publishDone ? (
            <span className={styles.meta} data-testid="editor-publish-done">
              {message(EDITOR_MSG.PUBLISH_DONE)}
            </span>
          ) : null}
          {stageDone ? (
            <span className={styles.meta} data-testid="editor-stage-done">
              {message(EDITOR_MSG.STAGE_DONE)}
            </span>
          ) : null}
          {takedownDone ? (
            <span className={styles.meta} data-testid="editor-takedown-done">
              {message(EDITOR_MSG.TAKEDOWN_DONE)}
            </span>
          ) : null}
          {previewDone ? (
            <span className={styles.meta} data-testid="editor-preview-done">
              {message(EDITOR_MSG.PREVIEW_DONE)}
            </span>
          ) : null}
          {recycleDone ? (
            <span className={styles.meta} data-testid="editor-recycle-done">
              {message(EDITOR_MSG.RECYCLED)}
            </span>
          ) : null}
          {canEdit ? (
            <button
              type="button"
              className={`${styles.button} ${styles.buttonPrimary}`}
              data-testid="editor-save"
              disabled={saving || loading || payload == null}
              onClick={() => void handleSave()}
            >
              {message(saving ? EDITOR_MSG.SAVING : EDITOR_MSG.SAVE)}
            </button>
          ) : null}
          {showPublish ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-publish-now"
              disabled={publishBusy || loading || payload == null || saving}
              onClick={() => void handlePublish()}
            >
              {message(publishBusy ? EDITOR_MSG.PUBLISHING : EDITOR_MSG.PUBLISH_NOW)}
            </button>
          ) : null}
          {showStage ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-stage-item"
              disabled={stageBusy || loading || payload == null || saving}
              onClick={() => void handleStage()}
            >
              {message(stageBusy ? EDITOR_MSG.STAGING : EDITOR_MSG.STAGE)}
            </button>
          ) : null}
          {showTakedown ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-takedown"
              disabled={takedownBusy || loading || payload == null || saving}
              onClick={() => void handleTakedown()}
            >
              {message(takedownBusy ? EDITOR_MSG.TAKING_DOWN : EDITOR_MSG.TAKE_DOWN)}
            </button>
          ) : null}
          {showPublishHistory ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-publishing-history"
              disabled={loading || payload == null}
              onClick={() => setHistoryOpen(true)}
            >
              {message(EDITOR_MSG.PUBLISHING_HISTORY)}
            </button>
          ) : null}
          {showMove ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-move"
              disabled={moveBusy || loading || payload == null || saving}
              onClick={() => void handleMoveOpen()}
            >
              {message(moveBusy ? EDITOR_MSG.MOVING : EDITOR_MSG.MOVE_TO_FOLDER)}
            </button>
          ) : null}
          {renameDone ? (
            <span className={styles.meta} data-testid="editor-renamed">
              {message(EDITOR_MSG.RENAMED)}
            </span>
          ) : null}
          {showRename ? (
            <label className={styles.meta}>
              {message(EDITOR_MSG.RENAME_NAME)}
              <input
                className={styles.input}
                data-testid="editor-rename-name"
                value={renameName}
                disabled={renameBusy || loading || payload == null || saving}
                onChange={(e) => setRenameName(e.target.value)}
              />
            </label>
          ) : null}
          {showRename ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-rename"
              disabled={renameBusy || loading || payload == null || saving}
              onClick={() => void handleRename()}
            >
              {message(renameBusy ? EDITOR_MSG.RENAMING : EDITOR_MSG.RENAME)}
            </button>
          ) : null}
          {showCopy ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-copy-to-folder"
              disabled={copyBusy || loading || payload == null || saving}
              onClick={() => void handleCopyToFolderOpen()}
            >
              {message(copyBusy ? EDITOR_MSG.COPYING : EDITOR_MSG.COPY_TO_FOLDER)}
            </button>
          ) : null}
          {showCopy ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-new-copy"
              disabled={copyBusy || loading || payload == null || saving}
              onClick={() => void handleCopy("copy")}
            >
              {message(copyBusy ? EDITOR_MSG.COPYING : EDITOR_MSG.NEW_COPY)}
            </button>
          ) : null}
          {showRecycle ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-recycle"
              disabled={recycleBusy || loading || payload == null || saving}
              onClick={() => void handleRecycle()}
            >
              {message(recycleBusy ? EDITOR_MSG.RECYCLING : EDITOR_MSG.RECYCLE)}
            </button>
          ) : null}
          {showCopy ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-promotable-version"
              disabled={copyBusy || loading || payload == null || saving}
              onClick={() => void handleCopy("promotable")}
            >
              {message(EDITOR_MSG.PROMOTABLE_VERSION)}
            </button>
          ) : null}
          {showCheckoutAction ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-checkout"
              disabled={lockBusy || loading || payload == null}
              onClick={() => void handleCheckout()}
            >
              {message(EDITOR_MSG.CHECKOUT_ACTION)}
            </button>
          ) : null}
          {showForceCheckin ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-force-checkin"
              disabled={lockBusy || loading || payload == null}
              onClick={() => void handleForceCheckin()}
            >
              {message(EDITOR_MSG.FORCE_CHECKIN)}
            </button>
          ) : null}
          {canEdit ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-checkin"
              disabled={lockBusy || loading || payload == null}
              onClick={() => void handleCheckin()}
            >
              {message(EDITOR_MSG.CHECKIN)}
            </button>
          ) : null}
          {showPreview ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-preview"
              disabled={previewBusy || loading || payload == null}
              onClick={() => void handlePreview()}
            >
              {message(previewBusy ? EDITOR_MSG.PREVIEWING : EDITOR_MSG.PREVIEW)}
            </button>
          ) : null}
          {showRestore ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-restore-toggle"
              disabled={restoreBusy || loading || payload == null}
              onClick={() => void handleRestoreOpen()}
            >
              {message(
                restoreOpen ? EDITOR_MSG.RESTORE_HIDE : EDITOR_MSG.RESTORE_OPEN,
              )}
            </button>
          ) : null}
          {showCreate ? (
            <button
              type="button"
              className={styles.button}
              data-testid="editor-new-item"
              disabled={createBusy}
              onClick={() => {
                setCreateOpen((open) => !open);
                setCreateErrorKey(null);
                setCreateErrorDetail("");
              }}
            >
              {message(EDITOR_MSG.NEW_ITEM)}
            </button>
          ) : null}
          <button
            type="button"
            className={styles.button}
            data-testid="editor-close"
            onClick={() => {
              if (typeof window !== "undefined") {
                window.close();
              }
            }}
          >
            {message(EDITOR_MSG.CLOSE)}
          </button>
        </div>
      </header>
      <div className={styles.stage} data-testid="editor-stage">
        {checkinPrompt ? (
          <div
            className={styles.form}
            role="dialog"
            aria-labelledby="editor-checkin-comment-title"
            data-testid="editor-checkin-comment"
          >
            <p id="editor-checkin-comment-title" data-testid="editor-checkin-comment-title">
              {message(EDITOR_MSG.CHECKIN_COMMENT)}
            </p>
            <p>{message(EDITOR_MSG.CHECKIN_COMMENT_HINT)}</p>
            <label className={styles.field}>
              <span className={styles.label}>{message(EDITOR_MSG.CHECKIN_COMMENT)}</span>
              <textarea
                className={styles.textarea}
                data-testid="editor-checkin-comment-input"
                value={checkinComment}
                onChange={(e) => setCheckinComment(e.target.value)}
              />
            </label>
            <div className={styles.actions}>
              <button
                type="button"
                className={styles.button}
                data-testid="editor-checkin-cancel"
                disabled={lockBusy}
                onClick={cancelCheckin}
              >
                {message(EDITOR_MSG.CHECKIN_CANCEL)}
              </button>
              <button
                type="button"
                className={`${styles.button} ${styles.buttonPrimary}`}
                data-testid="editor-checkin-confirm"
                disabled={lockBusy}
                onClick={() => void confirmCheckin()}
              >
                {message(EDITOR_MSG.CHECKIN_CONFIRM)}
              </button>
            </div>
          </div>
        ) : null}
        {createOpen && showCreate ? (
          <div className={styles.form} data-testid="editor-create-panel">
            <p data-testid="editor-create-hint">{message(EDITOR_MSG.CREATE_HINT)}</p>
            {linkbackWarning && contentId == null ? (
              <div className={styles.status} role="status" data-testid="editor-error">
                {message(EDITOR_MSG.MISSING_ITEM)} {linkbackWarning}
              </div>
            ) : contentId == null ? (
              <p data-testid="editor-create-empty">{message(EDITOR_MSG.MISSING_ITEM)}</p>
            ) : null}
            <label>
              {message(EDITOR_MSG.CREATE_TYPE)}
              <select
                data-testid="editor-create-type"
                value={createType}
                onChange={(e) => setCreateType(e.target.value)}
              >
                <option value="">{message(EDITOR_MSG.CREATE_TYPE)}</option>
                {createTypes.map((t) => (
                  <option key={t.name} value={t.name}>
                    {t.label || t.name}
                  </option>
                ))}
              </select>
            </label>
            <label>
              {message(EDITOR_MSG.CREATE_FOLDER)}
              <input
                type="text"
                data-testid="editor-create-folder"
                value={createFolder}
                onChange={(e) => setCreateFolder(e.target.value)}
              />
            </label>
            <label>
              {message(EDITOR_MSG.CREATE_NAME)}
              <input
                type="text"
                data-testid="editor-create-name"
                value={createName}
                onChange={(e) => setCreateName(e.target.value)}
              />
            </label>
            {createErrorKey ? (
              <div
                className={styles.status}
                role="alert"
                data-testid="editor-create-error"
              >
                {message(createErrorKey)}
                {createErrorDetail ? ` ${createErrorDetail}` : ""}
              </div>
            ) : null}
            <button
              type="button"
              className={`${styles.button} ${styles.buttonPrimary}`}
              data-testid="editor-create-submit"
              disabled={createBusy}
              onClick={() => void handleCreate()}
            >
              {message(createBusy ? EDITOR_MSG.CREATING : EDITOR_MSG.CREATE)}
            </button>
          </div>
        ) : null}
        {contentId != null && promote ? (
          <PromoteForm itemId={String(contentId)} />
        ) : contentId == null ? null : errorKey ? (
          <div className={styles.status} role="alert" data-testid="editor-error">
            {message(errorKey)}
            {errorDetail ? ` ${errorDetail}` : ""}
          </div>
        ) : loading ? (
          <div className={styles.status} role="status" data-testid="editor-loading">
            {message(EDITOR_MSG.LOADING)}
          </div>
        ) : (
          <>
            {saveErrorKey ? (
              <div
                className={styles.status}
                role="alert"
                data-testid="editor-save-error"
              >
                {message(saveErrorKey)}
                {saveErrorDetail ? ` ${saveErrorDetail}` : ""}
              </div>
            ) : null}
            {publishErrorKey ? (
              <div
                className={styles.status}
                role="alert"
                data-testid="editor-publish-error"
              >
                {message(publishErrorKey)}
                {publishErrorDetail ? ` ${publishErrorDetail}` : ""}
              </div>
            ) : null}
            {stageErrorKey ? (
              <div
                className={styles.status}
                role="alert"
                data-testid="editor-stage-error"
              >
                {message(stageErrorKey)}
                {stageErrorDetail ? ` ${stageErrorDetail}` : ""}
              </div>
            ) : null}
            {takedownErrorKey ? (
              <div
                className={styles.status}
                role="alert"
                data-testid="editor-takedown-error"
              >
                {message(takedownErrorKey)}
                {takedownErrorDetail ? ` ${takedownErrorDetail}` : ""}
              </div>
            ) : null}
            {previewErrorKey ? (
              <div
                className={styles.status}
                role="alert"
                data-testid="editor-preview-error"
              >
                {message(previewErrorKey)}
                {previewErrorDetail ? ` ${previewErrorDetail}` : ""}
              </div>
            ) : null}
            {renameErrorKey ? (
              <div
                className={styles.status}
                role="alert"
                data-testid="editor-rename-error"
              >
                {message(renameErrorKey)}
                {renameErrorDetail ? ` ${renameErrorDetail}` : ""}
              </div>
            ) : null}
            {copyErrorKey ? (
              <div
                className={styles.status}
                role="alert"
                data-testid="editor-copy-error"
              >
                {message(copyErrorKey)}
                {copyErrorDetail ? ` ${copyErrorDetail}` : ""}
              </div>
            ) : null}
            {copyFolderDone ? (
              <div
                className={styles.status}
                role="status"
                data-testid="editor-copy-to-folder-done"
              >
                {message(EDITOR_MSG.COPY_TO_FOLDER_DONE)} {copyFolderDone}
              </div>
            ) : null}
            {moveDone ? (
              <div
                className={styles.status}
                role="status"
                data-testid="editor-move-done"
              >
                {message(EDITOR_MSG.MOVE_DONE)}
              </div>
            ) : null}
            {moveErrorKey ? (
              <div
                className={styles.status}
                role="alert"
                data-testid="editor-move-error"
              >
                {message(moveErrorKey)}
                {moveErrorDetail ? ` ${moveErrorDetail}` : ""}
              </div>
            ) : null}
            {recycleErrorKey ? (
              <div
                className={styles.status}
                role="alert"
                data-testid="editor-recycle-error"
              >
                {message(recycleErrorKey)}
                {recycleErrorDetail ? ` ${recycleErrorDetail}` : ""}
              </div>
            ) : null}
            {lockErrorKey ? (
              <div
                className={styles.status}
                role="alert"
                data-testid="editor-lock-error"
              >
                {message(lockErrorKey)}
                {lockErrorDetail ? ` ${lockErrorDetail}` : ""}
              </div>
            ) : null}
            {!canEdit && !readOnly && !promote && payload != null ? (
              <div className={styles.status} data-testid="editor-locked">
                {message(EDITOR_MSG.LOCKED)}
              </div>
            ) : null}
            {restoreDone ? (
              <div
                className={styles.status}
                role="status"
                data-testid="editor-restore-done"
              >
                {message(EDITOR_MSG.RESTORED)}
              </div>
            ) : null}
            {showRestore && restoreOpen ? (
              <div className={styles.form} data-testid="editor-restore-panel">
                {restoreLoadErrorKey ? (
                  <div
                    className={styles.status}
                    role="alert"
                    data-testid="editor-restore-load-error"
                  >
                    {message(restoreLoadErrorKey)}
                  </div>
                ) : null}
                {restoreErrorKey ? (
                  <div
                    className={styles.status}
                    role="alert"
                    data-testid="editor-restore-error"
                  >
                    {message(restoreErrorKey)}
                    {restoreErrorDetail ? ` ${restoreErrorDetail}` : ""}
                  </div>
                ) : null}
                {restoreRevisions.length === 0 && !restoreLoadErrorKey ? (
                  <div
                    className={styles.status}
                    data-testid="editor-restore-empty"
                  >
                    {message(EDITOR_MSG.RESTORE_NONE)}
                  </div>
                ) : (
                  <label className={styles.field}>
                    <span className={styles.label}>
                      {message(EDITOR_MSG.RESTORE_REVISION_LABEL)}
                    </span>
                    <select
                      className={styles.input}
                      data-testid="editor-restore-select"
                      value={restoreSelected === "" ? "" : String(restoreSelected)}
                      onChange={(e) =>
                        setRestoreSelected(
                          e.target.value ? Number(e.target.value) : "",
                        )
                      }
                    >
                      {restoreRevisions.map((rev) => (
                        <option key={rev.revId} value={rev.revId}>
                          {summarizeRevisionRow(rev, `#${rev.revId}`)}
                        </option>
                      ))}
                    </select>
                  </label>
                )}
                <div className={styles.actions}>
                  <button
                    type="button"
                    className={`${styles.button} ${styles.buttonPrimary}`}
                    data-testid="editor-restore-confirm"
                    disabled={
                      restoreBusy ||
                      restoreRevisions.length === 0 ||
                      !restoreRestorable ||
                      parseRevisionId(restoreSelected) == null
                    }
                    onClick={() => void handleRestoreConfirm()}
                  >
                    {message(
                      restoreBusy
                        ? EDITOR_MSG.RESTORING
                        : EDITOR_MSG.RESTORE_PRIOR_REVISION,
                    )}
                  </button>
                </div>
                <div data-testid="editor-compare" className={styles.form}>
                  {restoreLoadErrorKey ? (
                    <div
                      className={styles.status}
                      data-testid="editor-compare-unavailable"
                    >
                      {message(EDITOR_MSG.COMPARE_LOAD_HINT)}
                    </div>
                  ) : restoreRevisions.length < 2 ? (
                    <div
                      className={styles.status}
                      data-testid="editor-compare-need-two"
                    >
                      {message(EDITOR_MSG.COMPARE_NEED_TWO)}
                    </div>
                  ) : (
                    <>
                      <label className={styles.field}>
                        <span className={styles.label}>
                          {message(EDITOR_MSG.COMPARE_FROM)}
                        </span>
                        <select
                          className={styles.input}
                          data-testid="editor-compare-left"
                          value={compareLeft === "" ? "" : String(compareLeft)}
                          onChange={(e) => {
                            setCompareLeft(
                              e.target.value ? Number(e.target.value) : "",
                            );
                            setCompareResult(null);
                            setCompareErrorKey(null);
                          }}
                        >
                          {restoreRevisions.map((rev) => (
                            <option key={`cmp-l-${rev.revId}`} value={rev.revId}>
                              {summarizeRevisionRow(rev, `#${rev.revId}`)}
                            </option>
                          ))}
                        </select>
                      </label>
                      <label className={styles.field}>
                        <span className={styles.label}>
                          {message(EDITOR_MSG.COMPARE_TO)}
                        </span>
                        <select
                          className={styles.input}
                          data-testid="editor-compare-right"
                          value={compareRight === "" ? "" : String(compareRight)}
                          onChange={(e) => {
                            setCompareRight(
                              e.target.value ? Number(e.target.value) : "",
                            );
                            setCompareResult(null);
                            setCompareErrorKey(null);
                          }}
                        >
                          {restoreRevisions.map((rev) => (
                            <option key={`cmp-r-${rev.revId}`} value={rev.revId}>
                              {summarizeRevisionRow(rev, `#${rev.revId}`)}
                            </option>
                          ))}
                        </select>
                      </label>
                      <div className={styles.actions}>
                        <button
                          type="button"
                          className={styles.button}
                          data-testid="editor-compare-run"
                          disabled={
                            compareBusy ||
                            !revisionCompareSelection(compareLeft, compareRight)
                              .ok
                          }
                          onClick={() => void handleCompareRevisions()}
                        >
                          {message(
                            compareBusy
                              ? EDITOR_MSG.COMPARING
                              : EDITOR_MSG.COMPARE,
                          )}
                        </button>
                      </div>
                    </>
                  )}
                  {compareErrorKey ? (
                    <div
                      className={styles.status}
                      role="alert"
                      data-testid="editor-compare-error"
                    >
                      {message(compareErrorKey)}
                      {compareErrorDetail ? ` ${compareErrorDetail}` : ""}
                    </div>
                  ) : null}
                  {compareResult && isEmptyRevisionCompare(compareResult.fields) ? (
                    <div
                      className={styles.status}
                      data-testid="editor-compare-empty"
                    >
                      {message(EDITOR_MSG.COMPARE_EMPTY)}
                    </div>
                  ) : null}
                  {compareResult && !isEmptyRevisionCompare(compareResult.fields) ? (
                    <table data-testid="editor-compare-table">
                      <thead>
                        <tr>
                          <th>{message(EDITOR_MSG.COMPARE_COL_FIELD)}</th>
                          <th>{message(EDITOR_MSG.COMPARE_COL_LEFT)}</th>
                          <th>{message(EDITOR_MSG.COMPARE_COL_RIGHT)}</th>
                          <th>{message(EDITOR_MSG.RESTORE_REVISION_LABEL)}</th>
                        </tr>
                      </thead>
                      <tbody>
                        {compareResult.fields.map((field) => (
                          <tr
                            key={field.name}
                            data-testid={`editor-compare-row-${field.name}`}
                            data-changed={field.changed ? "true" : "false"}
                          >
                            <td>{field.name}</td>
                            <td>{field.leftValue}</td>
                            <td>{field.rightValue}</td>
                            <td>
                              {field.changed
                                ? message(EDITOR_MSG.COMPARE_CHANGED)
                                : message(EDITOR_MSG.COMPARE_UNCHANGED)}
                            </td>
                          </tr>
                        ))}
                      </tbody>
                    </table>
                  ) : null}
                </div>
              </div>
            ) : null}
            {showPreview ? (
              <section data-testid="editor-preview-panel">
                <label data-testid="editor-preview-template-label">
                  {message(EDITOR_MSG.PREVIEW_TEMPLATE)}
                  <select
                    className={styles.input}
                    data-testid="editor-preview-template"
                    value={previewTemplateId}
                    disabled={loading || payload == null}
                    onChange={(e) => void applyPreviewTemplate(e.target.value)}
                  >
                    <option value={PREVIEW_TEMPLATE_CURRENT}>
                      {message(EDITOR_MSG.PREVIEW_TEMPLATE_CURRENT)}
                    </option>
                    {previewChoices.map((choice) => (
                      <option key={choice.id} value={choice.id}>
                        {choice.name || choice.id}
                      </option>
                    ))}
                  </select>
                </label>
                {previewFrameError ? (
                  <div className={styles.status} data-testid="editor-preview-template-error" role="alert">
                    {message(EDITOR_MSG.PREVIEW_TEMPLATE_FAILED)}
                  </div>
                ) : null}
                {previewFrameUrl ? (
                  <iframe
                    className={styles.form}
                    data-testid="editor-preview-frame"
                    data-preview-template={previewTemplateId || "current"}
                    title={message(EDITOR_MSG.PREVIEW)}
                    src={previewFrameUrl}
                  />
                ) : null}
              </section>
            ) : null}
            {contentId != null && payload != null ? (
              <EditorRelatedContentPanel
                itemId={String(contentId)}
                readOnly={!canEdit}
                hostMode={mode}
                loadCanvas={loadRelatedCanvas}
                loadLocal={loadRelatedLocal}
              />
            ) : null}
            {canEdit ? (
              <EditorWorkflowPanel
                stateName={workflowState}
                triggers={workflowTriggers}
                comment={workflowComment}
                onCommentChange={setWorkflowComment}
                onTransition={(t) => void handleTransition(t)}
                busy={workflowBusy || saving}
                errorKey={workflowErrorKey}
                errorDetail={workflowErrorDetail}
                commentRequiredTriggers={commentRequiredTriggers}
                workflowChoices={workflowChoices}
                selectedWorkflowId={selectedWorkflowId}
                onWorkflowIdChange={setSelectedWorkflowId}
                onChangeWorkflow={() => void handleChangeWorkflow()}
                workflowChanged={workflowChanged}
              />
            ) : null}
            {rows.length === 0 ? (
              <div className={styles.status} data-testid="editor-empty">
                {message(EDITOR_MSG.EMPTY)}
              </div>
            ) : (
              <form
                className={styles.form}
                data-testid="editor-form"
                onSubmit={(e) => {
                  e.preventDefault();
                  if (canEdit) {
                    void handleSave();
                  }
                }}
              >
                {rows.map((row) => (
                  <label
                    key={row.name}
                    className={styles.field}
                    data-testid={`editor-field-row-${row.name}`}
                    data-required={row.required ? "true" : "false"}
                  >
                    <span className={styles.label}>
                      {row.label}
                      {row.required ? (
                        <span className={styles.requiredMark} aria-hidden="true">
                          {" "}
                          *
                        </span>
                      ) : null}
                    </span>
                    <EditorFieldControl
                      row={row}
                      itemId={String(contentId)}
                      locked={readOnly || !canEdit || row.readOnly}
                      invalid={Boolean(fieldErrors[row.name])}
                      onChange={setField}
                      onFile={setFile}
                      loadKeywords={loadKeywords}
                      loadCommunities={loadCommunities}
                      loadBinaryMeta={loadBinaryMeta}
                    />
                    {fieldErrors[row.name] ? (
                      <span
                        className={styles.fieldError}
                        role="alert"
                        data-testid={`editor-field-error-${row.name}`}
                      >
                        {fieldErrors[row.name]}
                      </span>
                    ) : null}
                  </label>
                ))}
              </form>
            )}
          </>
        )}
      </div>
      {copyFolderOpen ? (
        <CopyDestinationPickerDialog
          defaultPath={copyFolderDefault}
          onPick={(target) => {
            void handleCopyToFolderPick(target);
          }}
          onCancel={() => setCopyFolderOpen(false)}
        />
      ) : null}
      {moveOpen ? (
        <MoveDestinationPickerDialog
          defaultPath={moveSourceParent}
          onPick={(target) => {
            void handleMovePick(target);
          }}
          onCancel={() => setMoveOpen(false)}
        />
      ) : null}
      {historyOpen && contentId != null ? (
        <PublishingHistoryDialog
          itemId={String(contentId)}
          onClose={() => setHistoryOpen(false)}
        />
      ) : null}
      {contentId != null && !promote ? (
        <TranslationsPanel
          itemId={String(contentId)}
          loadVariants={loadTranslationVariants}
          loadLocaleCatalog={loadTranslationLocales}
          createVariants={createTranslationVariants}
          onOpenVariant={(nextId) => {
            if (!nextId || nextId === contentId) {
              return;
            }
            const next = new URLSearchParams(params);
            next.set("contentId", String(nextId));
            next.set("mode", mode === "view" ? "view" : "edit");
            next.delete("warningMessage");
            setSearchParams(next);
          }}
          onCreated={(result) => {
            const created = result.created ?? [];
            if (created.length !== 1 || !created[0]?.contentId) {
              return;
            }
            const nextId = created[0].contentId;
            if (nextId === contentId) {
              return;
            }
            const next = new URLSearchParams(params);
            next.set("contentId", String(nextId));
            next.set("mode", "edit");
            next.delete("warningMessage");
            setSearchParams(next);
          }}
        />
      ) : null}
    </div>
  );
}
