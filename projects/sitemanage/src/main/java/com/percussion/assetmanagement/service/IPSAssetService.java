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
package com.percussion.assetmanagement.service;

import com.percussion.assetmanagement.data.PSAbstractAssetRequest;
import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.data.PSAssetDropCriteria;
import com.percussion.assetmanagement.data.PSAssetEditUrlRequest;
import com.percussion.assetmanagement.data.PSAssetEditor;
import com.percussion.assetmanagement.data.PSAssetFolderRelationship;
import com.percussion.assetmanagement.data.PSAssetSummary;
import com.percussion.assetmanagement.data.PSAssetWidgetRelationship;
import com.percussion.assetmanagement.data.PSContentEditCriteria;
import com.percussion.assetmanagement.data.PSFileAssetReportLine;
import com.percussion.assetmanagement.data.PSImageAssetReportLine;
import com.percussion.assetmanagement.data.PSInspectedElementsData;
import com.percussion.assetmanagement.data.PSReportFailedToRunException;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pagemanagement.data.PSWidgetContentType;
import com.percussion.share.dao.IPSGenericDao;
import com.percussion.share.data.PSNoContent;
import com.percussion.share.service.IPSDataService;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.share.validation.PSValidationErrors;
import java.util.Collection;
import java.util.List;

/**
 * Provides various operations for asset objects.
 *
 * <p>This interface is Java 11 compatible and follows Google Java Style Guide. All method
 * signatures are backward compatible.
 */
public interface IPSAssetService extends IPSDataService<PSAsset, PSAssetSummary, String> {

  String CREATE_ASSET_ERROR_MESSAGE = "Unable to convert HTML asset to Rich Text asset";
  String ASSET_TYPE_IMAGE = "percImageAsset";
  String ASSET_TYPE_FILE = "percFileAsset";
  String HTML_FIELD = "html";
  String TEXT_FIELD = "text";
  String RICH_TEXT_ASSET_TYPE = "percRichTextAsset";
  String HTML_ASSET_TYPE = "percRawHtmlAsset";
  String SYS_WORKFLOWID = "sys_workflowid";
  String SYS_TITLE = "sys_title";

  /**
   * Creates the relationship defined by the specified asset widget relationship.
   *
   * @param rel the asset widget relationship, never <code>null</code>.
   * @return the Id of the newly created relationship.
   * @throws PSAssetServiceException if the relationship cannot be created.
   */
  String createAssetWidgetRelationship(PSAssetWidgetRelationship rel) throws PSDataServiceException;

  /**
   * Updates the relationship defined by the specified asset widget relationship.
   *
   * @param rel the asset widget relationship, never <code>null</code>.
   * @return the Id of the updated relationship.
   * @throws PSAssetServiceException if the relationship cannot be created.
   */
  String updateAssetWidgetRelationship(PSAssetWidgetRelationship rel)
      throws PSAssetServiceException,
          IPSWidgetAssetRelationshipService.PSWidgetAssetRelationshipServiceException,
          PSValidationException;

  /**
   * Promotes the asset widget.
   *
   * @param rel the asset widget relationship, never <code>null</code>.
   * @return the result of the promotion.
   * @throws PSDataServiceException if the promotion fails.
   * @throws IPSItemWorkflowService.PSItemWorkflowServiceException if the workflow service fails.
   */
  PSNoContent promoteAssetWidget(PSAssetWidgetRelationship rel)
      throws PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException;

  /**
   * Clears the specified relationship. If no other asset widget relationships exist for the asset,
   * the item will also be deleted.
   *
   * @param rel the asset widget relationship, never <code>null</code>.
   * @throws PSAssetServiceException if the relationship cannot be deleted.
   */
  void clearAssetWidgetRelationship(PSAssetWidgetRelationship rel)
      throws PSAssetServiceException,
          PSValidationException,
          IPSWidgetAssetRelationshipService.PSWidgetAssetRelationshipServiceException;

  PSValidationErrors validateAssetWidgetRelationship(PSAssetWidgetRelationship awr)
      throws PSValidationException;

  /**
   * Gets the criteria for a widget to allow an asset drop.
   *
   * @param id never <code>null</code>.
   * @param isPage never <code>null</code>.
   * @return PSAssetDropCriteria
   */
  List<PSAssetDropCriteria> getWidgetAssetCriteria(String id, Boolean isPage)
      throws PSDataServiceException;

  /**
   * Gets list of asset editors and their URLs.
   *
   * @param parentFolderPath The parent folder path where the asset will be created in. May be
   *     <code>null</code> or empty, in that case the default workflow will be used.
   * @return never <code>null</code>.
   */
  List<PSAssetEditor> getAssetEditors(String parentFolderPath)
      throws PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException;

  /**
   * Gets list of asset editors and their URLs.
   *
   * @param parentFolderPath The parent folder path where the asset will be created in. May be
   *     <code>null</code> or empty, in that case the default workflow will be used.
   * @param filterDisabledWidgets if not null and equals ignore case to "yes", then disabled widgets
   *     are filtered.
   * @return never <code>null</code>.
   */
  List<PSAssetEditor> getAssetEditors(String parentFolderPath, String filterDisabledWidgets)
      throws PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException;

  /**
   * Gets list of asset type names and their internal ids.
   *
   * @param filterDisabledWidgets if not null and equals ignore case to "yes", then disabled widgets
   *     are filtered.
   * @return never <code>null</code> may be empty.
   */
  List<PSWidgetContentType> getAssetTypes(String filterDisabledWidgets)
      throws PSDataServiceException;

  /**
   * Gets the asset editor for the widgetId
   *
   * @param widgetId must not be <code>null</code>
   * @return never <code>null</code>.
   */
  PSAssetEditor getAssetEditor(String widgetId)
      throws PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException;

  /**
   * Gets the asset editor for the widgetId and specified folder path
   *
   * @param widgetId must not be <code>null</code>
   * @return never <code>null</code>.
   */
  PSAssetEditor getAssetEditor(String widgetId, String folderPath)
      throws PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException;

  /**
   * Gets edit URL for an asset.
   *
   * @param id - long string form of the asset id. example: 1-101-710
   * @param readonly - flag indicating the url should be for a view only asset.
   * @return never <code>null</code>.
   */
  String getAssetUrl(String id, boolean readonly) throws PSDataServiceException;

  /**
   * Adds the specified asset to the specified folder.
   *
   * @param assetFolderRelationship never <code>null</code>.
   */
  void addAssetToFolder(PSAssetFolderRelationship assetFolderRelationship)
      throws PSDataServiceException;

  /**
   * Removes the specified asset from the specified folder.
   *
   * @param assetFolderRelationship never <code>null</code>.
   */
  void removeAssetFromFolder(PSAssetFolderRelationship assetFolderRelationship)
      throws PSDataServiceException;

  /**
   * Gets an object of {@link PSContentEditCriteria} for the given PSAssetEditUrlRequest. If the
   * request is for a new item, then fills the contentName, if the content type does not produce a
   * resource.
   *
   * @param request the request info, never <code>null</code>.
   * @return the content editor criteria, never <code>null</code>.
   */
  PSContentEditCriteria getContentEditCriteria(PSAssetEditUrlRequest request)
      throws PSDataServiceException, IPSItemWorkflowService.PSItemWorkflowServiceException;

  /**
   * Creates a new asset for the specified request. Any required parent folders will also be created
   * if necessary.
   *
   * @param request the asset request used to create the asset. May not be <code>null</code>.
   * @return the created asset, never <code>null</code>. May not be valid if the asset was not
   *     created due to warnings.
   * @throws PSAssetServiceException if an error occurs creating the asset.
   */
  PSAsset createAsset(PSAbstractAssetRequest request)
      throws PSAssetServiceException, PSValidationException;

  /**
   * Finds all assets of the specified type in the specified workflow and state.
   *
   * @param type the content type of the assets, never blank.
   * @param workflow name, never blank.
   * @param state name, set to <code>null</code> to include assets in all workflow states.
   * @return collection of assets, never <code>null</code>, may be empty.
   */
  Collection<PSAsset> findByTypeAndWf(String type, String workflow, String state)
      throws PSAssetServiceException, IPSGenericDao.LoadException;

  /**
   * Similar with load(String), except caller has to specify if the returned object contains all
   * properties or just summary properties.
   *
   * @param id the identifier of the asset, not blank.
   * @param isSummary <code>true</code> if load summary properties of the items, which does not
   *     include Clob or Blob type fields; otherwise load all properties of the items.
   * @return the asset. It may be <code>null</code> if the asset does not exist.
   */
  PSAsset load(String id, boolean isSummary) throws PSAssetServiceException;

  /**
   * Finds all local assets of the specified type.
   *
   * @param type the content type of the assets, never blank.
   * @return collection of assets, never <code>null</code>, may be empty.
   */
  Collection<PSAsset> findLocalByType(String type)
      throws PSAssetServiceException,
          PSValidationException,
          IPSItemWorkflowService.PSItemWorkflowServiceException,
          IPSGenericDao.LoadException;

  /**
   * At present asset update happens through the content editor. Adding a dummy service to get
   * notified on asset update. If pageId is null then don't do anything just ignore it. If pageid is
   * not null then assetId can not be <code>null</code>.
   *
   * @param pageId can be <code>null</code>.
   * @param assetId
   */
  void updateAsset(String pageId, String assetId) throws PSAssetServiceException;

  /**
   * Method to update the existing asset by changing only the binary file with out changing the
   * asset path. There is an option to choose to override any checkout on the asset.
   *
   * @param id id, for which the binary has to be modified
   * @param ar request having the new binary
   * @param forceCheckOut check out the asset if it is checked out by another user.
   * @return modified asset.
   */
  PSAsset updateAsset(String id, PSAbstractAssetRequest ar, boolean forceCheckOut)
      throws PSAssetServiceException;

  /**
   * Copy a widget's local content to a shared asset using the supplied name, folder, and
   * relationship. The asset specified by the relationship will be copied and the new shared copy
   * will be related to the widget specified by the relationship.
   *
   * @param name The name to use for the new asset, may not be <code>null</code> or empty.
   * @param path The path that specifies the folder in which to create the asset, not <code>null
   *     </code> or empty, must be a valid path.
   * @param awRel The source asset-widget relationship, must specify local content, not <code>null
   *     </code>.
   * @return The new shared asset's item id. Eg: -1-101-709
   * @throws PSAssetServiceException If there are any errors.
   */
  String shareLocalContent(String name, String path, PSAssetWidgetRelationship awRel)
      throws PSAssetServiceException;

  /**
   * Creates an asset from a specified (source) asset. The type of the source asset may not be the
   * same as the created asset. Current implementation only support creating a Rich Text Asset from
   * a HTML asset.
   *
   * @param srcAssetId The ID of the source asset. Must not be blank.
   * @param targetAssetType The type of created asset. This is not used for now.
   * @return the created {@link PSAsset asset}, never <code>null</code>. May not be valid if the
   *     asset was not created due to warnings.
   * @throws PSAssetServiceException if an error occurs creating the asset.
   */
  PSAsset createAssetFromSourceAsset(String srcAssetId, String targetAssetType)
      throws PSAssetServiceException;

  /**
   * Method to update the inspected elements data, if the list of html asset data in the inspected
   * elemenet data is not null or empty then creates new html assets with the supplied data and
   * associates them to the supplied owner through the supplied widget. If the clear asset list is
   * not empty then clears the assets with the provided data.
   *
   * @param inspectedElementsData Must not be <code>null</code>.
   * @return List of newly created html assets, Never <code>null</code> may be empty.
   * @throws PSAssetServiceException
   */
  List<PSAsset> updateInspectedElements(PSInspectedElementsData inspectedElementsData)
      throws PSDataServiceException;

  /***
   * Will return an Image report that lists all images.
   *
   *
   * @throws PSReportFailedToRunException
   */
  List<PSImageAssetReportLine> findNonCompliantImageAssets() throws PSReportFailedToRunException;

  /***
   * A listing of all Images in the content repository.
   *
   *
   * @throws PSReportFailedToRunException
   */
  List<PSImageAssetReportLine> findAllImageAssets() throws PSReportFailedToRunException;

  /***
   * A listing of all non compliant File assets in the content repository.
   *
   *
   * @throws PSReportFailedToRunException
   */
  List<PSFileAssetReportLine> findNonCompliantFileAssets() throws PSReportFailedToRunException;

  /***
   * A listing of all File assets in the Content Repository.
   *
   *
   * @throws PSReportFailedToRunException
   */
  List<PSFileAssetReportLine> findAllFileAssets() throws PSReportFailedToRunException;

  /** (Runtime) Exception thrown when an unexpected error occurs in this service. */
  class PSAssetServiceException extends PSDataServiceException {
    private static final long serialVersionUID = 1L;

    public PSAssetServiceException() {
      super();
    }

    public PSAssetServiceException(String message, Throwable cause) {
      super(message, cause);
    }

    public PSAssetServiceException(String message) {
      super(message);
    }

    public PSAssetServiceException(Throwable cause) {
      super(cause);
    }
  }
}
