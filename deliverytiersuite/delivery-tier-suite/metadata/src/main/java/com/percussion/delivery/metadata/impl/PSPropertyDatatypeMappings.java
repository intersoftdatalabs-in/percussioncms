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
package com.percussion.delivery.metadata.impl;

import java.util.Properties;
import com.percussion.delivery.metadata.IPSMetadataProperty.VALUETYPE;

/**
 * Holds property-to-datatype mappings used to determine which datatype to store the property value as.
 * Intended to be instantiated and injected via Spring. The mappings are defined in beans.xml.
 * @author erikserating
 */
public class PSPropertyDatatypeMappings {

    protected Properties datatypeMappings;

    public PSPropertyDatatypeMappings() {}

    /**
     * @return the mappings
     */
    public Properties getDatatypeMappings() {
        return datatypeMappings;
    }

    /**
     * @param mappings the mappings to set
     */
    public void setDatatypeMappings(Properties mappings) {
        this.datatypeMappings = mappings;
    }

    /**
     * Given a datatype key, returns a VALUETYPE object according to it.
     * For example, "STRING" will return VALUETYPE.STRING.
     * @param key Datatype key (STRING, DATE, NUMBER, etc).
     * @return VALUETYPE
     */
    public VALUETYPE getDatatype(String key) {
        if (datatypeMappings == null) {
            throw new IllegalStateException(
                    "datatypeMappings is null, this class must be instantiated via Spring for data to be filled in.");
        }
        var stringProp = datatypeMappings.getProperty(key);
        var prop = VALUETYPE.STRING;
        if (stringProp == null) {
            return prop;
        }
        try {
            prop = VALUETYPE.valueOf(stringProp);
        } catch (Exception ex) {
            // Default to STRING if mapping fails
        }
        return prop;
    }
}
