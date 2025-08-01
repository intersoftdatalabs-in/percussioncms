// REFACTORED: CP-JAVA11
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
package com.percussion.ui.data;

import com.fasterxml.jackson.annotation.JsonRootName;

/**
 * Represents a column in a display format for UI lists.
 * Immutable after construction for safety.
 */
@JsonRootName("DisplayFormatColumn")
public class PSDisplayFormatColumn {
    private String name;
    private String label;
    private String type;
    private String width;

    public PSDisplayFormatColumn() {
        // Default constructor for serialization frameworks
    }

    public PSDisplayFormatColumn(String name, String label) {
        this.name = name;
        this.label = label;
    }

    /**
     * Gets the column name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the column name.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Gets the column label.
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets the column label.
     */
    public void setLabel(String label) {
        this.label = label;
    }

    /**
     * Gets the column type.
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the column type.
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the column width.
     */
    public String getWidth() {
        return width;
    }

    /**
     * Sets the column width.
     */
    public void setWidth(String width) {
        this.width = width;
    }
}
