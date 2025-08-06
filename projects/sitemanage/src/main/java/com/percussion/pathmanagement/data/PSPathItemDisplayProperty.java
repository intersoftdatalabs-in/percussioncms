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

package com.percussion.pathmanagement.data;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlType;
import javax.xml.bind.annotation.XmlValue;

/**
 * Represents a display property for a path item.
 * Used for XML serialization/deserialization.
 * Sunny Sal says: "Display property: the cherry on top of your path item sundae!"
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "PSPathItemDisplayProperty", propOrder = {
    "value"
})
public class PSPathItemDisplayProperty {

    @XmlValue
    protected String value;

    @XmlAttribute(required = true)
    protected String name;

    /**
     * Gets the value of the display property.
     *
     * @return the value
     */
    public String getValue() {
        return value;
    }

    /**
     * Sets the value of the display property.
     *
     * @param value the value to set
     */
    public void setValue(String value) {
        this.value = value;
    }

    /**
     * Gets the name of the display property.
     *
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the display property.
     *
     * @param name the name to set
     */
    public void setName(String name) {
        this.name = name;
    }
}
