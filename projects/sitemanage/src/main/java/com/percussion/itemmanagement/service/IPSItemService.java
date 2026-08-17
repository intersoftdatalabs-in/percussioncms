/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.itemmanagement.service;

import com.percussion.itemmanagement.data.PSAssetSiteImpact;
import com.percussion.itemmanagement.data.PSItemCopyResult;
import com.percussion.itemmanagement.data.PSItemDates;
import com.percussion.itemmanagement.data.PSItemCreateRequest;
import com.percussion.itemmanagement.data.PSItemCreateResult;
import com.percussion.itemmanagement.data.PSItemEditorBinaryMeta;
import com.percussion.itemmanagement.data.PSItemEditorFields;
import com.percussion.itemmanagement.data.PSRevisionsSummary;
import com.percussion.itemmanagement.data.PSSoProMetadata;
import com.percussion.services.useritems.data.PSUserItem;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.data.PSNoContent;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.webservices.PSErrorResultsException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

/**
 * Handles all item operations, such as retrieving revisions, dates, and user items.
 *
 * <p>Sunny Sal says: "If you need to manage items, this is your Swiss Army knife!"
 *
 * @author Jose Annunziato
 */
public interface IPSItemService {

  /**
   * Scalar content-editor fields for the React editor (995). Omits system fields
   * except {@code sys_title} and omits binary values.
   *
   * @param id item id or GUID, must not be blank
   */
  PSItemEditorFields getEditorFields(String id) throws PSItemServiceException;

  /**
   * Saves scalar content-editor fields. Caller must have the item checked out.
   * Only names present in {@code req} are updated.
   */
  PSItemEditorFields saveEditorFields(String id, PSItemEditorFields req)
      throws PSItemServiceException;

  /**
   * Creates a new content item in {@code req.folderPath} for the React editor New Item flow.
   */
  PSItemCreateResult createEditorItem(PSItemCreateRequest req) throws PSItemServiceException;

  /**
   * Filename / presence metadata for a binary content field. Does not stream bytes.
   */
  PSItemEditorBinaryMeta getEditorBinary(String id, String field) throws PSItemServiceException;

  /**
   * Replaces a binary content field. Caller must have the item checked out.
   */
  PSItemEditorBinaryMeta saveEditorBinary(
      String id, String field, InputStream data, String filename, String contentType)
      throws PSItemServiceException;

  /**
   * Retrieves the revisions for a given page or asset.
   *
   * @param id the full id of the page or asset, must not be blank
   * @return a list of revisions, never null, may be empty
   * @throws PSItemServiceException if an error occurs
   */
  PSRevisionsSummary getRevisions(String id) throws PSItemServiceException;

  /**
   * Retrieves the start and end date for a given page or asset resource. Returned dates are in the
   * format: MM/dd/yyyy HH:mm
   *
   * @param id the full id of the page or asset, must not be blank
   * @return object with start date, end date, and item id, never null, may be empty
   * @throws PSItemServiceException if an error occurs
   */
  PSItemDates getItemDates(String id) throws PSItemServiceException;

  /**
   * Sets the start and end date for a given page or asset resource. Dates must be in the format:
   * MM/dd/yyyy HH:mm
   *
   * @param req PSItemDates object with itemId set, and start and end dates values to set/replace
   * @return PSNoContent with a successful save message
   * @throws PSItemServiceException if an error occurs
   */
  PSNoContent setItemDates(PSItemDates req) throws Exception;

  /**
   * Retrieves the social promotion metadata for a given page.
   *
   * @param id the full id of the page or asset, must not be blank
   * @return object with the item id and the metadata for that id
   * @throws PSItemServiceException if an error occurs
   */
  PSSoProMetadata getSoProMetadata(String id) throws PSItemServiceException;

  /**
   * Sets the social promotion metadata for a given page.
   *
   * @param req PSSoProMetadata object with itemId set, and the metadata value to set/replace
   * @return PSNoContent with a successful save message
   * @throws PSItemServiceException if an error occurs
   */
  PSNoContent setSoProMetadata(PSSoProMetadata req) throws PSItemServiceException;

  /**
   * Restores a prior revision of an item. If the item is checked out by the current user then
   * checks the item in, if the item is in Live or Pending state then moves the item to quick edit
   * state before restoring the older revision. If the item has local content, restores the local
   * content from that revision. After restoring adjusts the parent local content relationships.
   * Validates whether the item can be restored from a prior revision or not, if not throws an
   * exception. The item can't be restored in one of the following three cases: 1. User has read
   * access to the folder. 2. User has read or none access to the item. 3. Item is checked out by
   * someone else.
   *
   * @param id The id of the item must not be blank. Expects the string format of the item guid. The
   *     revision part of the guid must be valid prior revision.
   * @return an object of PSNoContent
   * @throws PSItemServiceException if the item is not valid for restoring from a prior revision
   */
  PSNoContent restoreRevision(String id) throws PSItemServiceException;

  /**
   * Creates a new copy of the item in its current folder ({@code System/New Copy}).
   *
   * @param id content id or guid string, must not be blank
   * @return the new item id and folder path
   * @throws PSItemServiceException if the item has no folder path or the copy fails
   */
  PSItemCopyResult createNewCopy(String id) throws PSItemServiceException;

  /**
   * Creates a promotable version of the item in its current folder ({@code System/Promotable
   * Version}).
   *
   * @param id content id or guid string, must not be blank
   * @return the new item id and folder path
   * @throws PSItemServiceException if the item has no folder path or the copy fails
   */
  PSItemCopyResult createPromotableVersion(String id) throws PSItemServiceException;

  /**
   * Copies all contents of the specified folder to a folder under the specified location.
   * Currently, only folders in the Assets Library may be copied.
   *
   * @param srcFolder the path of the source folder, may not be null or empty
   * @param destFolder the path of the destination folder, may not be null or empty
   * @param name name of the folder to which the contents of the source folder will be copied, may
   *     not be null or empty
   * @return a map of copied items
   */
  Map<String, String> copyFolder(String srcFolder, String destFolder, String name)
      throws PSItemServiceException,
          IPSItemWorkflowService.PSItemWorkflowServiceException,
          PSErrorResultsException,
          PSDataServiceException;

  /**
   * Calculates the site impact of an asset and returns its result as a String representation of a
   * JSONObject. The JSON object will have three arrays:
   *
   * <pre>
   * {
   *   "pages":[PSItemProperties],
   *   "templates":[{"template":PSTemplateSummary, "site":sitename}]
   * }
   * </pre>
   *
   * Pages is an array of PSItemProperties objects of all the pages that the asset is related to.
   * Templates is an array of objects that consist of PSTemplateSummary object and site name of all
   * the templates the asset is related to. If the asset is related to another asset (case of inline
   * images and file links) then its parent page or template is returned as impacted item. If the
   * parent asset is not on any page or template then it is ignored. If the asset is not used on any
   * page or template then empty arrays are returned for pages and templates.
   *
   * @param assetId The id of the asset in the string format of the guid, must not be blank and must
   *     be a valid asset id
   * @return String representation of the JSONObject never null, see the description
   */
  String getAssetSiteImpact(String assetId);

  /**
   * Calculates the site impact of a page: pages and templates that reverse-relate to or hold
   * managed links targeting the given page. Response JSON shape matches {@link
   * #getAssetSiteImpact(String)}.
   *
   * @param pageId The id of the page in the string format of the guid, must not be blank
   * @return String representation of the JSON object, never null
   */
  String getPageSiteImpact(String pageId);

  /**
   * Adds a page to logged in user's my pages.
   *
   * @param pageId guid of the page id, must not be blank
   * @return PSNoContent with successful page added message
   */
  PSNoContent addToMyPages(String pageId);

  /**
   * Adds a page to the specified user's my pages.
   *
   * @param userName The userName of the user
   * @param pageId The guid of the Page
   * @return PSNoContent with successful page added message
   */
  PSNoContent addToMyPages(String userName, String pageId);

  /**
   * Removes a page from logged in user's my pages.
   *
   * @param pageId guid of the page id, must not be blank
   * @return PSNoContent with successful page removed message
   */
  PSNoContent removeFromMyPages(String pageId);

  /**
   * Checks whether the supplied pageId is a user page or not.
   *
   * @param pageId guid of the page id, must not be blank
   * @return true if the page is in currently logged in user's pages, otherwise false
   */
  boolean isMyPage(String pageId);

  /**
   * Finds all user items associated with the supplied user name.
   *
   * @param userName name of the user; if blank or items don't exist returns empty list
   * @return list of user items, may be empty, never null
   */
  List<PSUserItem> getUserItems(String userName);

  /**
   * Finds all user items associated with the supplied item id.
   *
   * @param itemId assumed to be a valid contentId (raw)
   * @return list of user items, may be empty, never null
   */
  List<PSUserItem> getUserItems(int itemId);

  /**
   * Adds a user item for the given user name and item id. If an entry with same details already
   * exists logs a warning and ignores the request.
   *
   * @param userName must not be blank
   * @param itemId assumed to be a valid contentId (raw)
   * @param type user item type
   */
  void addUserItem(String userName, int itemId, PSUserItemTypeEnum type)
      throws IPSGenericDao.SaveException;

  /**
   * Removes a user item corresponding to the supplied user name and item id. If no user item exists
   * with the supplied user name and item id logs the warning and ignores the request.
   *
   * @param userName name of the user, must not be blank
   * @param itemId assumed to be a valid contentId (raw)
   */
  void removeUserItem(String userName, int itemId);

  /**
   * Deletes all user item entries corresponding to the supplied itemId. If no entries exist does
   * nothing.
   *
   * @param itemId assumed to be a valid contentId (raw)
   */
  void deleteUserItems(int itemId);

  /**
   * Deletes all user item entries corresponding to the supplied user name. If no entries exist does
   * nothing.
   *
   * @param userName must not be blank
   */
  void deleteUserItems(String userName);

  /**
   * Cleanup assets folder on error.
   *
   * @param itemMap map from copyFolder contains all known id maps until error
   * @param folderName Root destination folder
   * @throws PSItemServiceException if cleanup fails
   */
  void rollBackCopiedFolder(Map<String, String> itemMap, String folderName)
      throws PSItemServiceException;

  /**
   * Finds all user items associated with the logged in user.
   *
   * @return list of user items, may be empty, never null
   */
  List<PSItemProperties> getMyContent();

  /** Thrown when an error is encountered in the item service. */
  class PSItemServiceException extends Exception {
    private static final long serialVersionUID = 1L;

    public PSItemServiceException() {
      super();
    }

    public PSItemServiceException(String message) {
      super(message);
    }

    public PSItemServiceException(String message, Throwable cause) {
      super(message, cause);
    }

    public PSItemServiceException(Throwable cause) {
      super(cause);
    }
  }

  /** The item type enum used as the type for user items. */
  enum PSUserItemTypeEnum {
    FAVORITE_PAGE
  }

  /**
   * Checks site impact for a list of asset ids.
   *
   * @param assetIds A list of asset ids to check site impact for
   * @return Returns a list of PSAssetSiteImpact items
   */
  List<PSAssetSiteImpact> getAssetSiteImpact(List<String> assetIds);
}
