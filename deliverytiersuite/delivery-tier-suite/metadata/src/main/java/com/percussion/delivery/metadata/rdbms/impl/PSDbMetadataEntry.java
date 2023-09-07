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
 * 
 * @author erikserating
 * 
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.NONSTRICT_READ_WRITE, region = "PSMetadataEntry")
@Table(name = "PERC_PAGE_METADATA")
public class PSDbMetadataEntry implements IPSMetadataEntry, Serializable
{

    @Id
    @Column(length = 40)
    @Nationalized
    private String pagepathHash;

    // This column may be marked as unique, but keep in mind that unique
    // keys greater than 767 characters are not supported on MySQL.
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

    /**
     * HashCalculator instance used to get the hash of the metadata entry's
     * pagepath.
     */
    private static PSHashCalculator hashCalculator = new PSHashCalculator();

    public PSDbMetadataEntry()
    {

    }

    /**
     * Ctor
     * 
     * @param name the file name, cannot be <code>null</code> or empty.
     * @param folder the folder path of the containing folder without the site
     *            folder. Cannot be <code>null</code> or empty.
     * @param pagepath the path of the file including sitefolder. This is used
     *            as a unique key for the entry. Cannot be <code>null</code> or
     *            empty.
     * @param type
     */
    public PSDbMetadataEntry(String name, String folder, String pagepath, String type, String site)
    {
        if (name == null || name.length() == 0)
            throw new IllegalArgumentException("name cannot be null or empty");
        if (folder == null || folder.length() == 0)
            throw new IllegalArgumentException("folder cannot be null or empty");
        if (pagepath == null || pagepath.length() == 0)
            throw new IllegalArgumentException("pagepath cannot be null or empty");
        if (site == null || site.length() == 0)
            throw new IllegalArgumentException("site cannot be null or empty");

        setName(name);
        setFolder(folder);
        setType(type);
        setPagepath(pagepath);
        setSite(site);
    }

    /**
     * @return the name
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @return the folder
     */
    public String getFolder()
    {
        return folder;
    }

    /**
     * @param folder the folder to set
     */
    public void setFolder(String folder)
    {
        this.folder = folder;
    }

    /**
     * @return the pagepathHash
     */
    public String getPagepathHash()
    {
        return pagepathHash;
    }

    /**
     * @return the page path
     */
    public String getPagepath()
    {
        return pagepath;
    }

    /**
     * @param path the pagepath to set
     */
    public void setPagepath(String path)
    {
        this.pagepath = path;

        if (this.pagepath == null)
            pagepathHash = hashCalculator.calculateHash(StringUtils.EMPTY);
        else
            pagepathHash = hashCalculator.calculateHash(this.pagepath);
    }

    /**
     * @return the linktext
     */
    public String getLinktext()
    {
        return linktext;
    }

    /**
     * @param linktext the linktext to set
     */
    public void setLinktext(String linktext)
    {
        this.linktext = linktext == null ? "" : linktext;
        this.linktext_lower = this.linktext.toLowerCase();
    }

    /**
     * @return the type
     */
    public String getType()
    {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(String type)
    {
        this.type = type == null ? "" : type;
    }

    /**
     * @return the site
     */
    public String getSite()
    {
        return site;
    }

    /**
     * @param site the site to set
     */
    public void setSite(String site)
    {
        this.site = site;
    }

    /**
     * @return the properties
     */
    public Set<IPSMetadataProperty> getProperties()
    {
        if(properties == null)
            return null;
        Set<IPSMetadataProperty> results = new HashSet<>(properties.size());
        for(IPSMetadataProperty p : properties)
            results.add(p);
        return results;
    }

    /**
     * @param properties the properties to set
     */
    public void setProperties(Set<IPSMetadataProperty> properties)
    {
        if(properties == null)
            this.properties = null;
        Set<PSDbMetadataProperty> dbprops = new HashSet<>();
        for(IPSMetadataProperty p : properties)
        {
            if(p instanceof PSDbMetadataProperty)
            {
                dbprops.add((PSDbMetadataProperty)p);
            }
            else
            {
                dbprops.add(new PSDbMetadataProperty(p.getName(), p.getValuetype(), p.getValue()));
            }
        }
            
        this.properties = dbprops;
    }
    
    public void clearProperties()
    {
        if(properties != null)
            properties.clear();
    }

    public void addProperty(IPSMetadataProperty prop)
    {
        ((PSDbMetadataProperty)prop).setMetadataEntry(this);
        this.properties.add((PSDbMetadataProperty)prop);
    }

    /**
     * Helper method to return number of properties.
     * 
     * @return number of properties.
     */
    public int getPropertyCount()
    {
        if (properties == null)
            return 0;
        return properties.size();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj)
    {
        if (obj == null || !getClass().getName().equals(obj.getClass().getName()))
            return false;
        PSDbMetadataEntry entry = (PSDbMetadataEntry) obj;
        return new EqualsBuilder()
            .append(folder, entry.folder)
            .append(linktext, entry.linktext)
            .append(name, entry.name)
            .append(pagepath, entry.pagepath)
            .append(site, entry.site)
            .append(type, entry.type)
            .isEquals();
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
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
