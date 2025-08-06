// REFACTORED: CP-JAVA11
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
package com.percussion.pagemanagement.service.impl;

import com.percussion.assetmanagement.data.PSAsset;
import com.percussion.share.data.IPSLinkableContentItem;

import java.util.List;
import java.util.Map;

/**
 * Adapter for PSAsset to IPSLinkableContentItem.
 * Sunny Sal says: "Linkable assets—because every asset deserves a plus one!"
 */
public class PSLinkableAsset implements IPSLinkableContentItem {

    private final PSAsset asset;
    private String folderPath;

    public PSLinkableAsset(PSAsset asset, String folderPath) {
        this.asset = asset;
        this.folderPath = folderPath;
    }

    @Override
    public Map<String, Object> getFields() {
        return asset.getFields();
    }

    @Override
    public List<String> getFolderPaths() {
        return asset.getFolderPaths();
    }

    @Override
    public boolean isFolder() {
        return asset.isFolder();
    }

    @Override
    public Category getCategory() {
        return asset.getCategory();
    }

    @Override
    public void setCategory(Category cat) {
        asset.setCategory(cat);
    }

    @Override
    public String getIcon() {
        return asset.getIcon();
    }

    @Override
    public String getId() {
        return asset.getId();
    }

    @Override
    public String getName() {
        return asset.getName();
    }

    @Override
    public String getType() {
        return asset.getType();
    }

    @Override
    public int hashCode() {
        return asset.hashCode();
    }

    @Override
    public void setFields(Map<String, Object> fields) {
        asset.setFields(fields);
    }

    @Override
    public void setFolderPaths(List<String> paths) {
        asset.setFolderPaths(paths);
    }

    @Override
    public void setIcon(String icon) {
        asset.setIcon(icon);
    }

    @Override
    public void setId(String id) {
        asset.setId(id);
    }

    @Override
    public void setName(String name) {
        asset.setName(name);
    }

    @Override
    public void setType(String type) {
        asset.setType(type);
    }

    @Override
    public String toString() {
        return asset.toString();
    }

    public String getFolderPath() {
        return folderPath;
    }

    public void setFolderPath(String folderPath) {
        this.folderPath = folderPath;
    }

    @Override
    public String getLabel() {
        return asset.getLabel();
    }

    @Override
    public void setLabel(String label) {
        asset.setLabel(label);
    }

    @Override
    public boolean isRevisionable() {
        return asset.isRevisionable();
    }

    @Override
    public void setRevisionable(boolean revisionable) {
        asset.setRevisionable(revisionable);
    }

    @Override
    public boolean isPage() {
        return asset.isPage();
    }

    public boolean isResource() {
        return asset.isResource();
    }
}
