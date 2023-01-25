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
package com.percussion.delivery.feeds.services.rdbms;

import com.percussion.delivery.feeds.data.FeedType;
import com.percussion.delivery.feeds.data.IPSFeedDescriptor;

import java.io.Serializable;


import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

/**
 * @author erikserating
 *
 */
@Entity
@Table(name = "PERC_FEED_DESCRIPTORS")
public class PSFeedDescriptor implements IPSFeedDescriptor, Serializable
{

    /**
	 * 
	 */
	private static final long serialVersionUID = 2756156009184830398L;

	@Id
    @Column(length = 255)
    private String site;
    
    @Id
    @Column(length = 255)
    private String name;
    
    @Basic
    @Column(length = 2000)
    private String title;
    
    @Basic
    @Column(length = 4000)
    private String description;
    
    @Basic
    @Column(length = 2000)
    private String link;
    
    @Basic
    @Column(length = 2000)
    private String type;
    
    @Basic
    @Column(length = 4000)
    private String query;
    
    public PSFeedDescriptor()
    {
        
    }
    
    public PSFeedDescriptor(IPSFeedDescriptor descriptor)
    {
        this.name = descriptor.getName();
        this.site = descriptor.getSite();
        this.title = descriptor.getTitle();
        this.description = descriptor.getDescription();
        this.link = descriptor.getLink();
        this.type = descriptor.getType();
        this.query = descriptor.getQuery();
    }
    
    /* (non-Javadoc)
     * @see com.percussion.feeds.data.IPSFeedDescriptor#getDescription()
     */
    public String getDescription()
    {
        return description;
    }

    /* (non-Javadoc)
     * @see com.percussion.feeds.data.IPSFeedDescriptor#getFeedType()
     */
    public FeedType getFeedType()
    {
        return FeedType.valueOf(type);
    }

    /* (non-Javadoc)
     * @see com.percussion.feeds.data.IPSFeedDescriptor#getLink()
     */
    public String getLink()
    {
        return link;
    }

    /* (non-Javadoc)
     * @see com.percussion.feeds.data.IPSFeedDescriptor#getName()
     */
    public String getName()
    {
        return name;
    }

    /* (non-Javadoc)
     * @see com.percussion.feeds.data.IPSFeedDescriptor#getQuery()
     */
    public String getQuery()
    {
        return query;
    }

    /* (non-Javadoc)
     * @see com.percussion.feeds.data.IPSFeedDescriptor#getSite()
     */
    public String getSite()
    {
        return site;
    }

    /* (non-Javadoc)
     * @see com.percussion.feeds.data.IPSFeedDescriptor#getTitle()
     */
    public String getTitle()
    {
        return title;
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
        this.type = type;
    }

    /**
     * @param site the site to set
     */
    public void setSite(String site)
    {
        this.site = site;
    }

    /**
     * @param name the name to set
     */
    public void setName(String name)
    {
        this.name = name;
    }

    /**
     * @param title the title to set
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * @param description the description to set
     */
    public void setDescription(String description)
    {
        this.description = description;
    }

    /**
     * @param link the link to set
     */
    public void setLink(String link)
    {
        this.link = link;
    }

    /**
     * @param query the query to set
     */
    public void setQuery(String query)
    {
        this.query = query;
    }

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((site == null) ? 0 : site.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PSFeedDescriptor other = (PSFeedDescriptor) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (site == null) {
            return other.site == null;
		} else {
            return site.equals(other.site);
        }
    }

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("PSFeedDescriptor [");
		if (site != null) {
			builder.append("site=");
			builder.append(site);
			builder.append(", ");
		}
		if (name != null) {
			builder.append("name=");
			builder.append(name);
			builder.append(", ");
		}
		if (title != null) {
			builder.append("title=");
			builder.append(title);
			builder.append(", ");
		}
		if (description != null) {
			builder.append("description=");
			builder.append(description);
			builder.append(", ");
		}
		if (link != null) {
			builder.append("link=");
			builder.append(link);
			builder.append(", ");
		}
		if (type != null) {
			builder.append("type=");
			builder.append(type);
			builder.append(", ");
		}
		if (query != null) {
			builder.append("query=");
			builder.append(query);
		}
		builder.append("]");
		return builder.toString();
	}


    
    

}
