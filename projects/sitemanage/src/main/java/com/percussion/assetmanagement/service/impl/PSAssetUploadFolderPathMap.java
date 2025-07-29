/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

package com.percussion.assetmanagement.service.impl;

import com.percussion.assetmanagement.data.PSAssetSummary;
import com.percussion.assetmanagement.service.IPSAssetService;
import com.percussion.assetmanagement.service.IPSAssetService.PSAssetServiceException;
import com.percussion.pathmanagement.service.impl.PSAssetPathItemService;
import com.percussion.share.dao.IPSFolderHelper;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;

/**
 * Responsible for mapping asset content types to Rhythmyx system folder paths.
 * Paths are relative to the root asset folder and can be configured via Spring.
 * Folders are created on demand if they do not already exist.
 */
public class PSAssetUploadFolderPathMap {

    private static final String UPLOADS_FOLDER_NAME = "uploads";
    private IPSFolderHelper folderHelper;
    private Map<String, String> typeToFolderPathMap = new HashMap<>();
    private static final String ASSET_ROOT = PSAssetPathItemService.ASSET_ROOT;

    public PSAssetUploadFolderPathMap(IPSFolderHelper folderHelper) {
        this.folderHelper = folderHelper;
    }

    /**
     * Finds the upload folder for an asset as a legacy folder id.
     * Uses one of the asset's existing folders if present, otherwise uses the type's default.
     *
     * @param asset never {@code null}.
     * @return never {@code null}.
     */
    public Number getLegacyFolderIdForAsset(PSAssetSummary asset) throws PSAssetServiceException {
        notNull(asset, "asset");
        var folderIds = getFolderIdsForPaths(asset.getFolderPaths());
        if (!folderIds.isEmpty())
            return folderIds.entrySet().iterator().next().getValue();
        return getLegacyFolderIdForType(asset.getType());
    }

    private Map<String, Number> getFolderIdsForPaths(Collection<String> paths) {
        var pathToFolderId = new HashMap<String, Number>();
        if (paths == null) return pathToFolderId;
        for (var p : paths) {
            try {
                var folderId = folderHelper.findLegacyFolderIdFromPath(p);
                pathToFolderId.put(p, folderId);
            } catch (Exception e) {
                // Skip this folder path.
                log.warn("Bad folder path: {}", p);
            }
        }
        return pathToFolderId;
    }

    /**
     * Retrieves the uploads folder associated with an asset type.
     * The folder will be created if it does not exist.
     * If the folder cannot be created, the default folder path will be used.
     *
     * @param type never {@code null}.
     * @return never {@code null}.
     * @throws PSAssetServiceException if no valid folder path can be found.
     */
    public Number getLegacyFolderIdForType(String type) throws PSAssetServiceException {
        notNull(type);
        var path = getFolderPathForType(type);
        boolean defaultPath = false;
        if (path == null) {
            defaultPath = true;
            path = getDefaultFolderPath();
        }

        try {
            return getFolderForTypeHelper(path);
        } catch (PSAssetServiceException e) {
            if (defaultPath) {
                throw e;
            }
            log.warn("Cannot use folder path for uploading assets: {}", path);
            path = getDefaultFolderPath();
        }
        return getFolderForTypeHelper(path);
    }

    private Number getFolderForTypeHelper(String folderPath) throws PSAssetServiceException {
        notEmpty(folderPath, "folderPath for type");
        try {
            folderHelper.createFolder(folderPath);
            return folderHelper.findLegacyFolderIdFromPath(folderPath);
        } catch (Exception e) {
            throw new IPSAssetService.PSAssetServiceException("Failed to get uploads folder", e);
        }
    }

    /**
     * Gets the folder path for a given type.
     * @param type never {@code null}.
     * @return {@code null} if there is no type matching a path.
     */
    protected String getFolderPathForType(String type) {
        notNull(type);
        var path = getTypeToFolderPathMap().get(type);
        if (path == null) return null;
        return folderHelper.concatPath(ASSET_ROOT, path);
    }

    /**
     * The base uploads folder path.
     * @return never {@code null} or empty.
     */
    protected String getBaseUploadsFolderPath() {
        return folderHelper.concatPath(ASSET_ROOT, UPLOADS_FOLDER_NAME);
    }

    /**
     * Gets the default upload folder path.
     * @return never {@code null}.
     */
    protected String getDefaultFolderPath() {
        return getBaseUploadsFolderPath();
    }

    public Map<String, String> getTypeToFolderPathMap() {
        return typeToFolderPathMap;
    }

    public void setTypeToFolderPathMap(Map<String, String> typeForFolderPath) {
        this.typeToFolderPathMap = typeForFolderPath;
    }

    private static final Logger log = LogManager.getLogger(PSAssetUploadFolderPathMap.class);
}
