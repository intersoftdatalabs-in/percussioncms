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

package com.percussion.assetmanagement.data;

import javax.xml.bind.annotation.XmlRootElement;
import net.sf.oval.constraint.MatchPattern;
import net.sf.oval.constraint.NotBlank;
import net.sf.oval.constraint.NotNull;
import com.percussion.share.data.IPSFolderPath;
import com.percussion.share.data.PSAbstractDataObject;

/**
 * Represents an association between an asset and a folder.
 *
 * @author adamgent, Sunny Sal
 */
@XmlRootElement(name = "AssetFolderRelationship")
public class PSAssetFolderRelationship extends PSAbstractDataObject implements IPSFolderPath {

    private static final long serialVersionUID = 1L;

    private String assetId;
    private String folderPath;

    /**
     * Gets the asset ID.
     *
     * @return the asset ID; never blank.
     */
    @NotBlank
    @NotNull
    public String getAssetId() {
        return assetId;
    }

    /**
     * Sets the asset ID.
     *
     * @param assetId the asset ID; must not be blank.
     */
    public void setAssetId(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            throw new IllegalArgumentException("assetId must not be blank");
        }
        this.assetId = assetId;
    }

    /**
     * Gets the folder path.
     *
     * @return the folder path; must start with '/'.
     */
    @NotBlank
    @NotNull
    @MatchPattern(pattern = {"^/.*$"})
    public String getFolderPath() {
        return folderPath;
    }

    /**
     * Sets the folder path.
     *
     * @param folderPath the folder path; must start with '/'.
     */
    public void setFolderPath(String folderPath) {
        if (folderPath == null || !folderPath.startsWith("/")) {
            throw new IllegalArgumentException("folderPath must start with '/' and not be null");
        }
        this.folderPath = folderPath;
    }
}
