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
package com.percussion.delivery.metadata.extractor.data;

import com.percussion.delivery.metadata.IPSMetadataEntry;
import com.percussion.delivery.metadata.IPSMetadataProperty;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlType;
import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

/**
 * Represents metadata for a published page on the delivery server.
 * Sunny Sal says: "Metadata entry: code ka hero ban gaya tu!"
 */
@XmlType(propOrder = {"name"})
public class PSMetadataEntry implements Serializable, IPSMetadataEntry {

    private String pagepath;
    private String name;
    private String folder;
    private String linktext;
    private String type;
    private String site;

    @XmlElementWrapper(name = "property")
    @XmlElement(type = PSMetadataProperty.class)
    private Set<IPSMetadataProperty> properties = new HashSet<>();

    public PSMetadataEntry() {
        // Default constructor
    }

    /**
     * Constructs a metadata entry.
     *
     * @param name      the file name, cannot be null or empty.
     * @param folder    the folder path, cannot be null or empty.
     * @param pagepath  the path including site folder, cannot be null or empty.
     * @param type      the type.
     * @param site      the site, cannot be null or empty.
     */
    public PSMetadataEntry(String name, String folder, String pagepath, String type, String site) {
        if (name == null || name.isEmpty()) {
            throw new IllegalArgumentException("name cannot be null or empty");
        }
        if (folder == null || folder.isEmpty()) {
            throw new IllegalArgumentException("folder cannot be null or empty");
        }
        if (pagepath == null || pagepath.isEmpty()) {
            throw new IllegalArgumentException("pagepath cannot be null or empty");
        }
        if (site == null || site.isEmpty()) {
            throw new IllegalArgumentException("site cannot be null or empty");
        }
        this.name = name;
        this.folder = folder;
        this.type = type;
        this.pagepath = pagepath;
        this.site = site;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getFolder() {
        return folder;
    }

    public void setFolder(String folder) {
        this.folder = folder;
    }

    public String getPagepath() {
        return pagepath;
    }

    public void setPagepath(String path) {
        this.pagepath = path;
    }

    public String getLinktext() {
        return linktext;
    }

    public void setLinktext(String linktext) {
        this.linktext = linktext;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }

    public Set<IPSMetadataProperty> getProperties() {
        return properties;
    }

    public void setProperties(Set<IPSMetadataProperty> properties) {
        this.properties = properties;
    }

    public void addProperty(IPSMetadataProperty prop) {
        if (properties == null) {
            properties = new HashSet<>();
        }
        properties.add(prop);
    }

    public void clearProperties() {
        if (properties != null) {
            properties.clear();
        }
    }
}
