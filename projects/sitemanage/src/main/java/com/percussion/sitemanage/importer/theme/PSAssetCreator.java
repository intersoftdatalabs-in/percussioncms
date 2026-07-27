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
// REFACTORED: CP-JAVA11
package com.percussion.sitemanage.importer.theme;

import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.getWebApplicationContext;

import com.percussion.assetmanagement.data.PSAbstractAssetRequest;
import com.percussion.assetmanagement.data.PSAbstractAssetRequest.AssetType;
import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.assetmanagement.data.PSBinaryAssetRequest;
import com.percussion.assetmanagement.data.PSExtractedAssetRequest;
import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.content.PSContentFactory;
import com.percussion.itemmanagement.service.IPSItemWorkflowService;
import com.percussion.pathmanagement.service.impl.PSPathUtils;
import com.percussion.share.service.exception.PSExtractHTMLException;
import com.percussion.share.service.exception.PSValidationException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Utility for creating assets during theme import. */
public class PSAssetCreator {

  private IPSAssetService assetService;
  private IPSItemWorkflowService itemWorkflowService;
  private static final Logger logger = LogManager.getLogger("PSAssetCreator");

  /** Mapping of asset type string name to the AssetType. */
  private static final Map<String, AssetType> ms_assetTypeMap = new HashMap<>();

  public IPSAssetService getAssetService() {
    if (assetService == null) {
      assetService = (IPSAssetService) getWebApplicationContext().getBean("assetService");
    }
    return assetService;
  }

  public IPSItemWorkflowService getItemWorkflowService() {
    if (itemWorkflowService == null) {
      itemWorkflowService =
          (IPSItemWorkflowService) getWebApplicationContext().getBean("workflowRestService");
    }
    return itemWorkflowService;
  }

  /**
   * Create the asset.
   *
   * @param folderpath Path of the target folder, not null.
   * @param type Asset type, not null.
   * @param fileInput File input, not null. Stream will be closed by this method.
   * @param fileName Name of the file, not null.
   * @param selector CSS selector for content extraction, not null.
   * @param includeOuterHtml true to include the selector element with the extracted content.
   * @param approveOnUpload true to approve asset on upload.
   * @return Newly created asset, never null.
   * @throws IOException
   * @throws PSExtractHTMLException if fail to create asset due to error on extracting content
   */
  public PSAsset createAsset(
      String folderpath,
      AssetType type,
      InputStream fileInput,
      String fileName,
      String selector,
      boolean includeOuterHtml,
      boolean approveOnUpload)
      throws IOException,
          PSExtractHTMLException,
          IPSItemWorkflowService.PSItemWorkflowServiceException,
          IPSAssetService.PSAssetServiceException,
          PSValidationException {
    try {
      PSAbstractAssetRequest ar;
      if (type == AssetType.FILE || type == AssetType.IMAGE) {
        ar =
            new PSBinaryAssetRequest(
                folderpath, type, fileName, determineMIMEType(fileName), fileInput);
      } else {
        // must be an extracted asset (html, rich text, simple text)
        ar =
            new PSExtractedAssetRequest(
                folderpath, type, fileName, fileInput, selector, includeOuterHtml);
      }

      var newAsset = getAssetService().createAsset(ar);
      var id = newAsset.getId();
      if (!StringUtils.isBlank(id)) {
        // transition the asset to an approved state
        if (approveOnUpload) {
          try {
            getItemWorkflowService()
                .transition(id, IPSItemWorkflowService.TRANSITION_TRIGGER_APPROVE);
          } catch (Exception e) {
            // If it fails to approve, just log and move on. Asset will be in draft workflow status.
            logger.warn("Failed to approve the asset: {}", newAsset.getName(), e);
          }
        }
        // Checkin asset
        getItemWorkflowService().checkIn(id);
      }
      return newAsset;
    } finally {
      IOUtils.closeQuietly(fileInput);
    }
  }

  /**
   * Create an asset if the downloaded resource is an Image Asset.
   *
   * @param fileInput the image for the asset.
   * @param destinationPath the path of the downloaded image, including file name.
   * @throws PSExtractHTMLException
   * @throws IOException
   */
  public PSAsset createAssetIfNeeded(InputStream fileInput, String destinationPath)
      throws PSExtractHTMLException,
          IOException,
          IPSAssetService.PSAssetServiceException,
          PSValidationException,
          IPSItemWorkflowService.PSItemWorkflowServiceException {
    var destination = new File(destinationPath);
    if (!isAsset(destinationPath)) return null;

    var folderPath = PSPathUtils.getFolderPath(destination.getParent().replace("\\", "/"));
    var fileName = destination.getName();
    var assetType = determineAssetTypeByFile(destinationPath);

    return createAsset(folderPath, getAssetType(assetType), fileInput, fileName, null, false, true);
  }

  /**
   * Get the type of the asset for the supplied asset type as string.
   *
   * @param assetType the asset type as string.
   * @return AssetType the asset type
   */
  public static AssetType getAssetType(String assetType) {
    return ms_assetTypeMap.get(assetType);
  }

  /**
   * Helper method to check if the downloaded resource is an Asset.
   *
   * @param destinationPath
   * @return true if the resource is an Asset, false otherwise.
   */
  private boolean isAsset(String destinationPath) {
    return destinationPath.replace("\\", "/").startsWith("/Assets/uploads");
  }

  /**
   * Helper method to guess the MIME type for the uploaded file.
   *
   * @param filename the filename including extension. Not null or empty.
   * @return the appropriate MIME type or the default of "application/octet-stream", never null.
   */
  private String determineMIMEType(String filename) {
    var f = new File(filename);
    return PSContentFactory.guessMimeType(f, "application/octet-stream");
  }

  /**
   * Helper method to guess the string asset type based on the MIME type of the file. Default asset
   * type is "file".
   *
   * @param fileName the filename including extension. Not null or empty.
   * @return the appropriate string asset type. By default is "file", never null.
   */
  private String determineAssetTypeByFile(String fileName) {
    var mimeType = determineMIMEType(fileName);
    var assetType = "file";
    if (StringUtils.containsIgnoreCase(mimeType, "image")) {
      assetType = "image";
    }
    return assetType;
  }

  static {
    ms_assetTypeMap.put("file", AssetType.FILE);
    ms_assetTypeMap.put("image", AssetType.IMAGE);
    ms_assetTypeMap.put("html", AssetType.HTML);
    ms_assetTypeMap.put("richtext", AssetType.RICH_TEXT);
    ms_assetTypeMap.put("simpletext", AssetType.SIMPLE_TEXT);
  }
}
