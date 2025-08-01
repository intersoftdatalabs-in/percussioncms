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

package com.percussion.assetmanagement.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import java.util.Optional;

/**
 * Contains data for requesting a URL, which can be used for creating a
 * related asset or editing an existing asset.
 *
 * <p>Type can be either {@link #PAGE_PARENT} or {@link #TEMPLATE_PARENT}.</p>
 *
 * @author YuBingChen, Sunny Sal
 */
@JsonRootName("AssetEditUrlRequest")
public class PSAssetEditUrlRequest {

    /**
     * The page parent type.
     * TODO: Switch this to an enum (Adam Gent)
     */
    public static final String PAGE_PARENT = "page";
    public static final String TEMPLATE_PARENT = "template";

    /**
     * The type of the parent. It can be either {@link #PAGE_PARENT} or {@link #TEMPLATE_PARENT}.
     */
    private String type;

    /**
     * The ID of the parent, which can be a page or template.
     */
    private String parentId;

    /**
     * The asset ID. It may be {@code null} if requesting a URL for creating the asset.
     */
    private String assetId;

    /**
     * The widget instance ID on a page or template.
     */
    private String widgetId;

    /**
     * The widget definition ID on a page or template.
     */
    private String widgetDefinition;

    /**
     * Gets the type of the parent. It can be either {@link #PAGE_PARENT} or {@link #TEMPLATE_PARENT}.
     *
     * @return the parent type, should not be blank.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the type of the parent.
     *
     * @param type the type to set; must not be blank.
     */
    public void setType(String type) {
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type must not be blank");
        }
        this.type = type;
    }

    /**
     * Gets the parent ID of the asset.
     *
     * @return the parent ID, must be an existing page or template.
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * Sets the parent ID.
     *
     * @param parentId the parentId to set; must not be blank.
     */
    public void setParentId(String parentId) {
        if (parentId == null || parentId.isBlank()) {
            throw new IllegalArgumentException("parentId must not be blank");
        }
        this.parentId = parentId;
    }

    /**
     * Gets the asset ID.
     *
     * @return the asset ID. It may be {@code null} if requesting the URL
     * for creating an asset; otherwise requesting the URL for editing.
     */
    public Optional<String> getAssetId() {
        return Optional.ofNullable(assetId);
    }

    /**
     * Sets the asset ID.
     *
     * @param assetId the assetId to set; may be null.
     */
    public void setAssetId(String assetId) {
        this.assetId = assetId;
    }

    /**
     * Gets the widget instance ID.
     *
     * @return the widget ID; may be null.
     */
    public Optional<String> getWidgetId() {
        return Optional.ofNullable(widgetId);
    }

    /**
     * Sets the widget instance ID.
     *
     * @param widgetId the widgetId to set; may be null.
     */
    public void setWidgetId(String widgetId) {
        this.widgetId = widgetId;
    }

    /**
     * Gets the widget definition of the asset.
     *
     * @return the widgetDefinition, must be an existing asset; may be null.
     */
    public Optional<String> getWidgetDefinition() {
        return Optional.ofNullable(widgetDefinition);
    }

    /**
     * Sets the widget definition.
     *
     * @param widgetDefinition the widgetDefinition to set; may be null.
     */
    public void setWidgetDefinition(String widgetDefinition) {
        this.widgetDefinition = widgetDefinition;
    }
}
