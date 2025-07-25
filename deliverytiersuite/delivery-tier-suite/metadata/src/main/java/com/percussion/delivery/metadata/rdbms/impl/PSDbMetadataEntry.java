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
package com.percussion.delivery.metadata.rdbms.impl;

import com.percussion.delivery.metadata.IPSMetadataEntry;
import com.percussion.delivery.metadata.IPSMetadataProperty;
import com.percussion.delivery.metadata.utils.PSHashCalculator;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.Basic;
import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.Id;
import javax.persistence.OneToMany;
import javax.persistence.Table;
import javax.persistence.Version;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.*;

/**
 * Represents metadata for a published page on the delivery server.
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "PSMetadataEntry")
@Table(name = "PERC_PAGE_METADATA")
public class PSDbMetadataEntry implements IPSMetadataEntry, Serializable {

    @Id
    @Column(length = 40)
    @Nationalized
    private String pagepathHash;

    @Column(length = 2000)
    @Nationalized
    private String pagepath;

    @Basic
    @Nationalized
    private String name;

    @Column(length = 2000)
    @Nationalized
    private String folder;

    @Basic
    @Nationalized
    private String linktext;

    @Basic
    @Nationalized
    private String linktext_lower;

    @Basic
    @Index(name = "typeIndex")
    @Nationalized
    private String type;

    @Basic
    @Nationalized
    private String site;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL,
        orphanRemoval = true, mappedBy = "entry", targetEntity = PSDbMetadataProperty.class)
    private Set<PSDbMetadataProperty> properties = new HashSet<>();

    private static final PSHashCalculator hashCalculator = new PSHashCalculator();

    public PSDbMetadataEntry() {}

    public PSDbMetadataEntry(String name, String folder, String pagepath, String type, String site) {
        if (name == null || name.isEmpty())
            throw new IllegalArgumentException("name cannot be null or empty");
        if (folder == null || folder.isEmpty())
            throw new IllegalArgumentException("folder cannot be null or empty");
        if (pagepath == null || pagepath.isEmpty())
            throw new IllegalArgumentException("pagepath cannot be null or empty");
        if (site == null || site.isEmpty())
            throw new IllegalArgumentException("site cannot be null or empty");

        setName(name);
        setFolder(folder);
        setType(type);
        setPagepath(pagepath);
        setSite(site);
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setName(String name) {
        this.name = name;
    }

    @Override
    public String getFolder() {
        return folder;
    }

    @Override
    public void setFolder(String folder) {
        this.folder = folder;
    }

    @Override
    public String getPagepathHash() {
        return pagepathHash;
    }

    @Override
    public String getPagepath() {
        return pagepath;
    }

    @Override
    public void setPagepath(String path) {
        this.pagepath = path;
        pagepathHash = (this.pagepath == null)
            ? hashCalculator.calculateHash(StringUtils.EMPTY)
            : hashCalculator.calculateHash(this.pagepath);
    }

    @Override
    public String getLinktext() {
        return linktext;
    }

    @Override
    public void setLinktext(String linktext) {
        this.linktext = linktext == null ? "" : linktext;
        this.linktext_lower = this.linktext.toLowerCase();
    }

    @Override
    public String getType() {
        return type;
    }

    @Override
    public void setType(String type) {
        this.type = type == null ? "" : type;
    }

    @Override
    public String getSite() {
        return site;
    }

    @Override
    public void setSite(String site) {
        this.site = site;
    }

    @Override
    public Set<IPSMetadataProperty> getProperties() {
        if (properties == null) return null;
        var results = new HashSet<IPSMetadataProperty>(properties.size());
        results.addAll(properties);
        return results;
    }

    @Override
    public void setProperties(Set<IPSMetadataProperty> properties) {
        if (properties == null) {
            this.properties = null;
            return;
        }
        var dbprops = new HashSet<PSDbMetadataProperty>();
        for (var p : properties) {
            if (p instanceof PSDbMetadataProperty) {
                dbprops.add((PSDbMetadataProperty) p);
            } else {
                dbprops.add(new PSDbMetadataProperty(p.getName(), p.getValuetype(), p.getValue()));
            }
        }
        this.properties = dbprops;
    }

    @Override
    public void clearProperties() {
        if (properties != null) properties.clear();
    }

    @Override
    public void addProperty(IPSMetadataProperty prop) {
        ((PSDbMetadataProperty) prop).setMetadataEntry(this);
        this.properties.add((PSDbMetadataProperty) prop);
    }

    @Override
    public int getPropertyCount() {
        return (properties == null) ? 0 : properties.size();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null || !getClass().getName().equals(obj.getClass().getName()))
            return false;
        var entry = (PSDbMetadataEntry) obj;
        return new EqualsBuilder()
            .append(folder, entry.folder)
            .append(linktext, entry.linktext)
            .append(name, entry.name)
            .append(pagepath, entry.pagepath)
            .append(site, entry.site)
            .append(type, entry.type)
            .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder()
            .append(folder)
            .append(linktext)
            .append(name)
            .append(pagepath)
            .append(site)
            .append(type)
            .toHashCode();
    }
}
