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
package com.percussion.delivery.metadata.data;

import com.percussion.delivery.metadata.IPSMetadataProperty;
import org.apache.commons.lang3.time.FastDateFormat;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Represents a metadata entry in the REST layer. Used to return exactly what's needed.
 */
public class PSMetadataRestEntry {

    private static final FastDateFormat DATE_FORMAT = FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss");

    private String pagepath;
    private String name;
    private String folder;
    private String linktext;
    private String type;
    private String site;
    private Map<String, Object> properties = new HashMap<>();

    public Optional<String> getPagepath() {
        return Optional.ofNullable(pagepath);
    }

    public void setPagepath(String pagepath) {
        this.pagepath = pagepath;
    }

    public Optional<String> getName() {
        return Optional.ofNullable(name);
    }

    public void setName(String name) {
        this.name = name;
    }

    public Optional<String> getFolder() {
        return Optional.ofNullable(folder);
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public Optional<String> getLinktext() {
        return Optional.ofNullable(linktext);
    }

    public void setLinktext(String linktext) {
        this.linktext = linktext;
    }

    public Optional<String> getType() {
        return Optional.ofNullable(type);
    }

    public void setType(String type) {
        this.type = type;
    }

    public Optional<String> getSite() {
        return Optional.ofNullable(site);
    }

    public void setSite(String site) {
        this.site = site;
    }

    public Map<String, Object> getProperties() {
        return properties;
    }

    public void setProperties(Map<String, Object> properties) {
        this.properties = properties;
    }

    /**
     * Adds a PSMetadataProperty to the properties map, converting to String as needed.
     * If the property already exists, stores as a list of values.
     *
     * @param metadataProperty the property to add.
     */
    public void addMetadataProperty(IPSMetadataProperty metadataProperty) {
        var newValue = "";
        switch (metadataProperty.getValuetype()) {
            case NUMBER:
                newValue = metadataProperty.getNumbervalue().toString();
                break;
            case DATE:
                newValue = DATE_FORMAT.format(metadataProperty.getDatevalue());
                break;
            default:
                newValue = metadataProperty.getStringvalue();
        }
        properties.compute(metadataProperty.getName(), (k, v) -> {
            if (v == null) {
                return newValue;
            } else if (v instanceof String) {
                var multiValued = new ArrayList<String>();
                multiValued.add((String) v);
                multiValued.add(newValue);
                return multiValued;
            } else if (v instanceof List) {
                ((List<String>) v).add(newValue);
                return v;
            }
            return newValue;
        });
    }
}
