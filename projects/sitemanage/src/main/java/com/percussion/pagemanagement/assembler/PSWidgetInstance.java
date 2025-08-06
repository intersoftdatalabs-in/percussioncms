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
package com.percussion.pagemanagement.assembler;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang.StringUtils;

import com.percussion.pagemanagement.data.PSWidgetDefinition;
import com.percussion.pagemanagement.data.PSWidgetItem;
import com.percussion.utils.types.PSPair;

/**
 * Represents a fully loaded widget for rendering.
 * @author adamgent
 */
public class PSWidgetInstance {

    private PSWidgetDefinition definition;
    private PSWidgetItem item;
    private List<PSPair<String, String>> ownerAssetIds = new ArrayList<>();

    /**
     * Temporary for sprint.
     * @return asset ids as a comma-separated string.
     */
    @Deprecated
    public String getAssets() {
        return String.join(",", getAssetIds());
    }

    /**
     * @deprecated Use {@link #getOwnerAssetIds()} instead.
     * @return list of asset ids.
     */
    @Deprecated
    public List<String> getAssetIds() {
        return ownerAssetIds.stream()
                .map(PSPair::getSecond)
                .collect(Collectors.toList());
    }

    public List<PSPair<String, String>> getOwnerAssetIds() {
        return ownerAssetIds;
    }

    public void setOwnerAssetIds(List<PSPair<String, String>> ids) {
        ownerAssetIds = ids;
    }

    public PSWidgetDefinition getDefinition() {
        return definition;
    }

    public void setDefinition(PSWidgetDefinition definition) {
        this.definition = definition;
    }

    public PSWidgetItem getItem() {
        return item;
    }

    public void setItem(PSWidgetItem item) {
        this.item = item;
    }
}
