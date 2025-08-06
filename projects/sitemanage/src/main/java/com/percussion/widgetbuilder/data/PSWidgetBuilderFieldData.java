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
package com.percussion.widgetbuilder.data;

import com.percussion.share.data.PSAbstractDataObject;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/**
 * Represents a single widget field definition.
 */
@XmlRootElement(name = "WidgetBuilderFieldData")
public class PSWidgetBuilderFieldData extends PSAbstractDataObject {

    private static final long serialVersionUID = 1L;

    private String name;
    private String label;
    private String type;

    public PSWidgetBuilderFieldData() {
        // Default constructor
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getType() {
        return type;
    }

    /**
     * Set the type of the field. Must be a valid FieldType.
     *
     * @param type the type string
     * @throws IllegalArgumentException if type is not valid
     */
    public void setType(String type) {
        FieldType.valueOf(type);
        this.type = type;
    }

    public enum FieldType {
        TEXT,
        TEXT_AREA,
        DATE,
        RICH_TEXT,
        FILE,
        FILE_LINK,
        IMAGE,
        IMAGE_LINK,
        PAGE,
        PAGE_LINK
    }

    @Override
    public String toString() {
        return "PSWidgetBuilderFieldData{" +
                "name='" + name + '\'' +
                ", label='" + label + '\'' +
                ", type='" + type + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSWidgetBuilderFieldData)) return false;
        var that = (PSWidgetBuilderFieldData) o;
        return Objects.equals(getName(), that.getName())
                && Objects.equals(getLabel(), that.getLabel())
                && Objects.equals(getType(), that.getType());
    }

    @Override
    public int hashCode() {
        return Objects.hash(getName(), getLabel(), getType());
    }
}
