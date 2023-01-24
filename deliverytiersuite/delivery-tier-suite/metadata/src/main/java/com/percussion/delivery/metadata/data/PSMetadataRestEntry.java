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

/**
 * Represents a metadata entry in the REST layer. It's used to return
 * exactly what's needed.
 *
 */
public class PSMetadataRestEntry
{
    /**
     * Date format used for string serialized date. 2011-01-21T09:36:05
     */
    FastDateFormat dateFormat =  FastDateFormat.getInstance("yyyy-MM-dd'T'HH:mm:ss");
    
    private String pagepath;

    private String name;

    private String folder;

    private String linktext;

    private String type;

    private String site;

    private HashMap<String, Object> properties = new HashMap<>();

    public String getPagepath()
    {
        return pagepath;
    }

    public void setPagepath(String pagepath)
    {
        this.pagepath = pagepath;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getFolder()
    {
        return folder;
    }

    public void setFolder(String folder)
    {
        this.folder = folder;
    }

    public String getLinktext()
    {
        return linktext;
    }

    public void setLinktext(String linktext)
    {
        this.linktext = linktext;
    }

    public String getType()
    {
        return type;
    }

    public void setType(String type)
    {
        this.type = type;
    }

    public String getSite()
    {
        return site;
    }

    public void setSite(String site)
    {
        this.site = site;
    }

    public HashMap<String, Object> getProperties()
    {
        return properties;
    }

    public void setProperties(HashMap<String, Object> properties)
    {
        this.properties = properties;
    }

    /**
     * Adds a PSMetadataProperty to the Map 'properties', so it's converted
     * to String as desired with this format:
     * <code>
     * {
     *      "propertyName" : "propertyValue"
     * }
     * </code>.
     * @param metadataProperty A PSMetadataProperty instance that will be
     * added to the 'properties' Map.
     */
    public void addMetadataProperty(IPSMetadataProperty metadataProperty)
    {
    	String newValue = "";
        if (metadataProperty.getValuetype().equals(IPSMetadataProperty.VALUETYPE.NUMBER))
        {
        	newValue = metadataProperty.getNumbervalue().toString();
        }
        else if (metadataProperty.getValuetype().equals(IPSMetadataProperty.VALUETYPE.DATE))
        {
        	newValue = dateFormat.format(metadataProperty.getDatevalue());
        }
        else
        {
        	newValue = metadataProperty.getStringvalue();
        }
        if (!this.properties.containsKey(metadataProperty.getName())){
    		this.properties.put(metadataProperty.getName(),	newValue);	
    	}
    	else{
    		Object value = this.properties.get(metadataProperty.getName());
    		if (value instanceof String)
    		{
    			List<String> multiValued = new ArrayList<>();
    			multiValued.add((String)value);
    			multiValued.add(newValue);
    			this.properties.put(metadataProperty.getName(), multiValued);
    		}
    		else
    		{
    			((List<String>)value).add(newValue);
    			this.properties.put(metadataProperty.getName(), value);
    		}
    		
    	}
    }
}
