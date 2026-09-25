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

export const EDITOR_MSG = {
  TITLE: "perc.ui.editor@Content Editor",
  BADGE_EDIT: "perc.ui.editor@Edit",
  BADGE_VIEW: "perc.ui.editor@View",
  CONTENT_ID: "perc.ui.editor@Content",
  TYPE_LABEL: "perc.ui.editor@Type",
  PAGE_TEMPLATE: "perc.ui.editor@Page template",
  CHECKOUT: "perc.ui.editor@Checked out to",
  MISSING_ITEM:
    "perc.ui.editor@Open an item from Explorer or Home, or create one from Home → Create.",
  LOADING: "perc.ui.editor@Loading item fields…",
  LOAD_FAILED: "perc.ui.editor@Could not load this item for editing.",
  SAVE: "perc.ui.editor@Save",
  SAVING: "perc.ui.editor@Saving…",
  SAVE_FAILED: "perc.ui.editor@Could not save the item.",
  SAVE_STALE:
    "perc.ui.editor@This item was saved with a newer revision. Reload and try again.",
  SAVED: "perc.ui.editor@Saved",
  FIELD_REQUIRED: "perc.ui.editor@This field is required.",
  FIELD_INVALID_DATE: "perc.ui.editor@Enter a valid date.",
  INVALID_DATE_SAVE:
    "perc.ui.editor@Correct invalid dates before saving.",
  REQUIRED_SAVE:
    "perc.ui.editor@Fill in the required fields before saving.",
  REQUIRED_CHECKIN:
    "perc.ui.editor@Fill in the required fields before checking in.",
  CHECKIN: "perc.ui.editor@Check In",
  CHECKIN_COMMENT: "perc.ui.editor@Revision comment",
  CHECKIN_COMMENT_HINT:
    "perc.ui.editor@Optional comment stored with this check-in. Leave blank to check in without a comment.",
  CHECKIN_CONFIRM: "perc.ui.editor@Check in",
  CHECKIN_CANCEL: "perc.ui.editor@Cancel",
  CHECKOUT_ACTION: "perc.ui.editor@Check Out",
  CHECKOUT_FORBIDDEN:
    "perc.ui.editor@You are not allowed to check out this item.",
  CHECKOUT_CONFLICT:
    "perc.ui.editor@This item is checked out to another user.",
  CHECKIN_FORBIDDEN:
    "perc.ui.editor@You are not allowed to check in this item.",
  CHECKIN_CONFLICT:
    "perc.ui.editor@This item is not checked out to you.",
  CHECKOUT_FAILED: "perc.ui.editor@Could not check out this item.",
  CHECKIN_FAILED: "perc.ui.editor@Could not check in this item.",
  FORCE_CHECKIN: "perc.ui.editor@Force Check-in",
  CONFIRM_FORCE_CHECKIN:
    "perc.ui.editor@Force check-in this item? Other users will lose their checkout.",
  FORCE_CHECKIN_FORBIDDEN:
    "perc.ui.editor@You are not allowed to force check-in this item.",
  FORCE_CHECKIN_NOT_FOUND: "perc.ui.editor@This item was not found.",
  FORCE_CHECKIN_CONFLICT:
    "perc.ui.editor@This item is not checked out, so it cannot be force checked in.",
  FORCE_CHECKIN_FAILED: "perc.ui.editor@Could not force check-in this item.",
  CLOSE: "perc.ui.editor@Close",
  EMPTY: "perc.ui.editor@This content type has no editable text fields.",
  LOCKED: "perc.ui.editor@This item is checked out to another user.",
  HTML_LABEL: "perc.ui.editor@Rich text",
  HTML_UNSAFE:
    "perc.ui.editor@That HTML contains script or event markup that cannot be saved.",
  HTML_BAD_REQUEST: "perc.ui.editor@That HTML could not be saved.",
  HTML_INVALID_SAVE:
    "perc.ui.editor@Correct the HTML fields before saving.",
  LONGTEXT_INVALID:
    "perc.ui.editor@That long text contains a character that cannot be saved.",
  LONGTEXT_BAD_REQUEST: "perc.ui.editor@That long text could not be saved.",
  LONGTEXT_INVALID_SAVE:
    "perc.ui.editor@Correct the long text fields before saving.",
  NUMBER_INVALID: "perc.ui.editor@Enter a valid number.",
  NUMBER_RANGE: "perc.ui.editor@That number is outside the allowed range.",
  NUMBER_INVALID_SAVE:
    "perc.ui.editor@Correct the number fields before saving.",
  NUMBER_BAD_REQUEST: "perc.ui.editor@That number could not be saved.",
  LINK_INVALID: "perc.ui.editor@Enter a content id or a site path.",
  LINK_INVALID_SAVE:
    "perc.ui.editor@Correct the link fields before saving.",
  LINK_BAD_REQUEST: "perc.ui.editor@That link could not be saved.",
  LINK_NOT_FOUND: "perc.ui.editor@That link target was not found.",
  LINK_FORBIDDEN: "perc.ui.editor@You are not allowed to use that link target.",
  LINK_CLEAR: "perc.ui.editor@Clear link",
  TABLE_ADD_ROW: "perc.ui.editor@Add row",
  TABLE_REMOVE_ROW: "perc.ui.editor@Remove row",
  FILE_CHOOSE: "perc.ui.editor@Choose file",
  FILE_NONE: "perc.ui.editor@No file attached",
  FILE_FORBIDDEN:
    "perc.ui.editor@You are not allowed to upload a file for this item.",
  FILE_TOO_LARGE: "perc.ui.editor@That file is too large to upload.",
  FILE_BAD_REQUEST: "perc.ui.editor@That file could not be uploaded.",
  IMAGE_CHOOSE: "perc.ui.editor@Choose image",
  IMAGE_NONE: "perc.ui.editor@No image attached",
  IMAGE_FORBIDDEN:
    "perc.ui.editor@You are not allowed to upload an image for this item.",
  IMAGE_TOO_LARGE: "perc.ui.editor@That image is too large to upload.",
  IMAGE_BAD_REQUEST: "perc.ui.editor@That image could not be uploaded.",
  KEYWORD_EMPTY: "perc.ui.editor@Select a keyword",
  COMMUNITY_EMPTY: "perc.ui.editor@Select a community",
  BADGE_PROMOTE: "perc.ui.editor@Promote",
  PROMOTE: "perc.ui.editor@Promote revision",
  PROMOTE_HINT:
    "perc.ui.editor@Restore a prior revision as the current version. The item is checked in first if you have it checked out.",
  PROMOTE_REVISION: "perc.ui.editor@Revision",
  PROMOTE_FAILED: "perc.ui.editor@Could not promote this revision.",
  PROMOTED: "perc.ui.editor@Revision promoted.",
  PROMOTE_NONE: "perc.ui.editor@No revisions are available to promote.",
  WORKFLOW_LABEL: "perc.ui.editor@Workflow",
  WORKFLOW_STATE: "perc.ui.editor@State",
  WORKFLOW_EMPTY: "perc.ui.editor@No workflow transitions are available.",
  WORKFLOW_COMMENT: "perc.ui.editor@Transition comment",
  WORKFLOW_UNAUTHORIZED:
    "perc.ui.editor@That workflow transition is not allowed for this item.",
  WORKFLOW_COMMENT_REQUIRED:
    "perc.ui.editor@Enter a comment before running this transition.",
  WORKFLOW_FAILED: "perc.ui.editor@Could not run this workflow transition.",
  WORKFLOW_DONE: "perc.ui.editor@Workflow transition completed.",
  WORKFLOW_CHANGE: "perc.ui.editor@Change workflow",
  WORKFLOW_CHANGE_APPLY: "perc.ui.editor@Save workflow",
  WORKFLOW_CHANGE_EMPTY: "perc.ui.editor@Select a workflow.",
  WORKFLOW_CHANGE_UNCHANGED: "perc.ui.editor@That item is already on this workflow.",
  WORKFLOW_CHANGE_FORBIDDEN:
    "perc.ui.editor@That workflow is not allowed for this item.",
  WORKFLOW_CHANGE_FAILED: "perc.ui.editor@Could not change this item's workflow.",
  WORKFLOW_CHANGED: "perc.ui.editor@Workflow changed.",
  PUBLISH_NOW: "perc.ui.editor@Publish now",
  PUBLISHING: "perc.ui.editor@Publishing…",
  CONFIRM_PUBLISH_NOW: "perc.ui.editor@Publish this item now?",
  PUBLISH_DONE: "perc.ui.editor@Publish started.",
  PUBLISH_FAILED: "perc.ui.editor@Could not publish this item.",
  PUBLISH_UNAVAILABLE:
    "perc.ui.editor@Publish now is only available for pages and assets.",
  PUBLISHING_HISTORY: "perc.ui.editor@Publishing history",
  TAKE_DOWN: "perc.ui.editor@Take down",
  TAKING_DOWN: "perc.ui.editor@Taking down…",
  CONFIRM_TAKE_DOWN:
    "perc.ui.editor@Take down (unpublish) this item from its site?",
  CONFIRM_TAKE_DOWN_LINKED: "perc.ui.editor@Pages that link to this item:",
  TAKEDOWN_DONE: "perc.ui.editor@Item taken down.",
  TAKEDOWN_FAILED: "perc.ui.editor@Could not take down this item.",
  TAKEDOWN_UNAVAILABLE:
    "perc.ui.editor@Take down is only available for saved pages and assets.",
  PREVIEW: "perc.ui.editor@Preview",
  PREVIEWING: "perc.ui.editor@Opening preview…",
  PREVIEW_DONE: "perc.ui.editor@Preview opened.",
  PREVIEW_FAILED: "perc.ui.editor@Could not preview this item.",
  PREVIEW_UNAVAILABLE:
    "perc.ui.editor@Preview is only available for pages and assets.",
  PREVIEW_TEMPLATE: "perc.ui.editor@Preview template",
  PREVIEW_TEMPLATE_CURRENT: "perc.ui.editor@Current template",
  PREVIEW_TEMPLATE_FAILED:
    "perc.ui.editor@Could not preview with that template.",
  CONFIRM_PREVIEW_UNSAVED:
    "perc.ui.editor@Preview the last saved revision? Unsaved edits are not included.",
  NEW_COPY: "perc.ui.editor@New copy",
  PROMOTABLE_VERSION: "perc.ui.editor@Promotable version",
  COPYING: "perc.ui.editor@Creating copy…",
  CONFIRM_NEW_COPY: "perc.ui.editor@Create a new copy of this item in the same folder?",
  CONFIRM_PROMOTABLE:
    "perc.ui.editor@Create a promotable version of this item in the same folder?",
  COPY_FAILED: "perc.ui.editor@Could not create a copy of this item.",
  COPY_FORBIDDEN: "perc.ui.editor@You are not allowed to copy this item.",
  COPY_NOT_FOUND: "perc.ui.editor@This item was not found.",
  COPY_UNAVAILABLE: "perc.ui.editor@Copy is only available while editing an item.",
  COPY_TO_FOLDER: "perc.ui.editor@Copy to folder",
  COPY_TO_FOLDER_DONE:
    "perc.ui.editor@Copied into the chosen folder. This editor stays on the original item.",
  COPY_BAD_REQUEST: "perc.ui.editor@The destination folder was not valid.",
  MOVE_TO_FOLDER: "perc.ui.editor@Move to folder",
  MOVING: "perc.ui.editor@Moving…",
  MOVE_DONE:
    "perc.ui.editor@Item moved. This editor is still open on the same item.",
  MOVE_FAILED: "perc.ui.editor@Could not move this item.",
  MOVE_FORBIDDEN: "perc.ui.editor@You are not allowed to move this item.",
  MOVE_NOT_FOUND:
    "perc.ui.editor@This item or the destination folder was not found.",
  MOVE_CONFLICT:
    "perc.ui.editor@That folder already has an item with this name, or the destination is not a folder.",
  MOVE_SAME_FOLDER:
    "perc.ui.editor@Choose a different folder. This item is already there.",
  MOVE_UNAVAILABLE:
    "perc.ui.editor@Move is only available while editing an item.",
  RECYCLE: "perc.ui.editor@Recycle",
  RECYCLING: "perc.ui.editor@Recycling…",
  CONFIRM_RECYCLE:
    "perc.ui.editor@Move this item to the recycle bin? It will leave the folder and this editor.",
  RECYCLED: "perc.ui.editor@Item moved to the recycle bin.",
  RECYCLE_FAILED: "perc.ui.editor@Could not recycle this item.",
  RECYCLE_FORBIDDEN: "perc.ui.editor@You are not allowed to recycle this item.",
  RECYCLE_NOT_FOUND: "perc.ui.editor@This item was not found.",
  RECYCLE_CONFLICT:
    "perc.ui.editor@This item cannot be recycled because it is in use or checked out.",
  RECYCLE_FOLDER: "perc.ui.editor@Folders cannot be recycled from the editor.",
  RECYCLE_UNAVAILABLE:
    "perc.ui.editor@Recycle is only available while editing an item.",
  RESTORE_PRIOR_REVISION: "perc.ui.editor@Restore prior revision",
  RESTORING: "perc.ui.editor@Restoring…",
  RESTORE_CONFIRM:
    "perc.ui.editor@Restore the selected revision as the current version? Unsaved edits will be lost.",
  RESTORE_FAILED: "perc.ui.editor@Could not restore the prior revision.",
  RESTORE_FORBIDDEN:
    "perc.ui.editor@You are not allowed to restore a revision of this item.",
  RESTORE_NOT_FOUND:
    "perc.ui.editor@This item or revision was not found.",
  RESTORE_UNAVAILABLE:
    "perc.ui.editor@Restoring a prior revision is only available while editing an item.",
  RESTORE_NONE:
    "perc.ui.editor@No prior revisions are available to restore.",
  RESTORE_LOAD_FAILED:
    "perc.ui.editor@Could not load the revisions for this item.",
  RESTORED: "perc.ui.editor@Prior revision restored.",
  RESTORE_REVISION_LABEL: "perc.ui.editor@Revision",
  RESTORE_OPEN: "perc.ui.editor@Show revisions",
  RESTORE_HIDE: "perc.ui.editor@Hide revisions",
  COMPARE: "perc.ui.editor@Compare revisions",
  COMPARING: "perc.ui.editor@Comparing…",
  COMPARE_FROM: "perc.ui.editor@From revision",
  COMPARE_TO: "perc.ui.editor@To revision",
  COMPARE_NEED_TWO:
    "perc.ui.editor@Pick two different revisions to compare.",
  COMPARE_SAME:
    "perc.ui.editor@Choose two different revisions. The same revision is not a diff.",
  COMPARE_EMPTY:
    "perc.ui.editor@These revisions have no field differences.",
  COMPARE_FAILED: "perc.ui.editor@Could not compare these revisions.",
  COMPARE_FORBIDDEN:
    "perc.ui.editor@You are not allowed to compare revisions of this item.",
  COMPARE_NOT_FOUND:
    "perc.ui.editor@This item or revision was not found for compare.",
  COMPARE_LOAD_HINT:
    "perc.ui.editor@Revision history could not be loaded, so compare is unavailable.",
  COMPARE_COL_FIELD: "perc.ui.editor@Field",
  COMPARE_COL_LEFT: "perc.ui.editor@Older value",
  COMPARE_COL_RIGHT: "perc.ui.editor@Newer value",
  COMPARE_CHANGED: "perc.ui.editor@Changed",
  COMPARE_UNCHANGED: "perc.ui.editor@Unchanged",
  RELATED_TITLE: "perc.ui.editor@Related content",
  RELATED_LOADING: "perc.ui.editor@Loading related content…",
  RELATED_EMPTY: "perc.ui.editor@No related content for this item.",
  RELATED_FORBIDDEN:
    "perc.ui.editor@You are not allowed to list related content for this item.",
  RELATED_FAILED: "perc.ui.editor@Could not load related content.",
  RELATED_CHANGE_TEMPLATE: "perc.ui.editor@Change template",
  RELATED_CHANGE_TITLE: "perc.ui.editor@Change snippet template",
  RELATED_TEMPLATE_LABEL: "perc.ui.editor@Snippet template",
  RELATED_TEMPLATE_APPLY: "perc.ui.editor@Apply",
  RELATED_TEMPLATE_CANCEL: "perc.ui.editor@Cancel",
  RELATED_TEMPLATE_NEEDS:
    "perc.ui.editor@Choose a snippet template before applying.",
  RELATED_TEMPLATE_FAILED:
    "perc.ui.editor@Could not change the snippet template.",
  RELATED_TEMPLATE_FORBIDDEN:
    "perc.ui.editor@You are not allowed to change this snippet template.",
  RELATED_TEMPLATE_NOT_FOUND:
    "perc.ui.editor@That related item or template was not found.",
  RELATED_TEMPLATE_BAD_REQUEST:
    "perc.ui.editor@That snippet template is not allowed for this slot.",
  RELATED_TEMPLATE_SAVED: "perc.ui.editor@Snippet template updated.",
  RELATED_INSERT: "perc.ui.editor@Insert existing item",
  RELATED_ITEM_ID: "perc.ui.editor@Existing item id",
  RELATED_SLOT: "perc.ui.editor@Slot",
  RELATED_INSERTING: "perc.ui.editor@Inserting…",
  RELATED_INSERT_BAD:
    "perc.ui.editor@That related item could not be inserted. Check the item id and slot.",
  RELATED_INSERT_FORBIDDEN:
    "perc.ui.editor@You are not allowed to insert related content on this item.",
  RELATED_INSERT_NOT_FOUND:
    "perc.ui.editor@That item or slot was not found.",
  RELATED_INSERT_FAILED: "perc.ui.editor@Could not insert the related item.",
  RELATED_REMOVE: "perc.ui.editor@Remove",
  RELATED_REMOVING: "perc.ui.editor@Removing…",
  RELATED_REMOVE_FORBIDDEN:
    "perc.ui.editor@You are not allowed to remove related content from this item.",
  RELATED_REMOVE_NOT_FOUND:
    "perc.ui.editor@That related item was not found.",
  RELATED_REMOVE_FAILED: "perc.ui.editor@Could not remove the related item.",
  RELATED_MOVE_UP: "perc.ui.editor@Move up",
  RELATED_MOVE_DOWN: "perc.ui.editor@Move down",
  RELATED_MOVING: "perc.ui.editor@Moving…",
  RELATED_MOVE_FORBIDDEN:
    "perc.ui.editor@You are not allowed to reorder related content on this item.",
  RELATED_MOVE_NOT_FOUND:
    "perc.ui.editor@That related item was not found.",
  RELATED_MOVE_CONFLICT:
    "perc.ui.editor@This item is not checked out to you. Reorder was not saved.",
  RELATED_MOVE_FAILED: "perc.ui.editor@Could not reorder the related item.",
  RELATED_OPEN: "perc.ui.editor@Open",
  RELATED_OPEN_FAILED: "perc.ui.editor@Could not open the related item.",
  NEW_ITEM: "perc.ui.editor@New item",
  CREATING: "perc.ui.editor@Creating…",
  CREATE: "perc.ui.editor@Create",
  CREATE_TYPE: "perc.ui.editor@Content type",
  CREATE_FOLDER: "perc.ui.editor@Folder path",
  CREATE_NAME: "perc.ui.editor@Name (optional)",
  CREATE_HINT:
    "perc.ui.editor@Choose a content type and destination folder, then create the item. The editor opens the new item.",
  CREATE_INCOMPLETE:
    "perc.ui.editor@Choose a content type and folder path before creating.",
  CREATE_FAILED: "perc.ui.editor@Could not create the item.",
  CREATE_FORBIDDEN: "perc.ui.editor@You are not allowed to create an item here.",
  CREATE_NOT_FOUND: "perc.ui.editor@That folder or content type was not found.",
  CREATE_BAD_REQUEST:
    "perc.ui.editor@The create request was not valid for this type or folder.",
  CREATE_UNAVAILABLE:
    "perc.ui.editor@Create is not available in this editor mode.",
  RENAME: "perc.ui.editor@Rename",
  RENAMING: "perc.ui.editor@Renaming…",
  RENAME_NAME: "perc.ui.editor@Listing name",
  RENAMED: "perc.ui.editor@Renamed",
  RENAME_FAILED: "perc.ui.editor@Could not rename this item.",
  RENAME_BAD_REQUEST: "perc.ui.editor@Enter a listing name that does not contain a slash.",
  RENAME_FORBIDDEN: "perc.ui.editor@You are not allowed to rename this item.",
  RENAME_NOT_FOUND: "perc.ui.editor@This item was not found.",
  RENAME_NO_FOLDER: "perc.ui.editor@This item is not in a folder, so it cannot be renamed here.",
  RENAME_UNAVAILABLE: "perc.ui.editor@Rename is only available while editing an item.",
};
