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
package com.percussion.assetmanagement.forms.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.itemmanagement.data.IPSEditableItem;
import com.percussion.share.data.PSAbstractDataObject;
import net.sf.oval.constraint.NotEmpty;
import net.sf.oval.constraint.NotNull;

import static org.apache.commons.lang.Validate.notEmpty;
import static org.apache.commons.lang.Validate.notNull;


/**
 * This object holds the summary information of a form asset.
 */
@JsonRootName("FormSummary")
public class PSFormSummary extends PSAbstractDataObject implements IPSEditableItem
{
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * For serialization.
     */
    public PSFormSummary()
    {             
    }
    
    /**
     * @return the name of the form, never blank.
     */
    public String getName()
    {
        return name;
    }

    /**
     * @param name must not be blank.
     */
    public void setName(String name)
    {
        notEmpty(name);
        
        this.name = name;
    }

    /**
     * @return the title of the form.
     */
    public String getTitle()
    {
        return title;
    }

    /**
     * @param title of the form.  Must not be <code>null</code>.
     */
    public void setTitle(String title)
    {
        notNull(title);
        
        this.title = title;
    }
    
    /**
     * @return the form description.
     */
    public String getDescription()
    {
        return description;
    }

    /**
     * @param description the form description to set.  Must not be <code>null</code>.
     */
    public void setDescription(String description)
    {
        notNull(description);
        
        this.description = description;
    }

    /**
     * @return the workflow state of the form.  May be a valid workflow state or "None" for forms which only exist on
     * the delivery tier.
     */
    public String getState()
    {
        return state;
    }

    /**
     * @param state the workflow state of the form to set.  Must not be blank.
     */
    public void setState(String state)
    {
        notEmpty(state);
        
        this.state = state;
    }

    /**
     * @return type of the form.  All forms are assets.
     */
    public String getType()
    {
        return IPSEditableItem.ASSET_TYPE;
    }

    /**
     * @param type of the form to set.
     */
    public void setType(String type)
    {
        this.type = type;
    }

    /**
     * @return id of the form.
     */
    public String getId()
    {
        return id;
    }

    /**
     * @param id the id to set.
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * @return the total number of submissions that have come in for the form.
     */
    public int getTotalSubmissions()
    {
        return totalSubmissions;
    }

    /**
     * @param totalSubmissions the total number of submissions that have come in for the form.
     */
    public void setTotalSubmissions(int totalSubmissions)
    {
        this.totalSubmissions = totalSubmissions;
    }

    /**
     * @return the number of submissions for the form which have not been exported.
     */
    public int getNewSubmissions()
    {
        return newSubmissions;
    }

    /**
     * @param newSubmissions the number of submissions for the form which have not been exported.
     */
    public void setNewSubmissions(int newSubmissions)
    {
        this.newSubmissions = newSubmissions;
    }
    
    /**
     * Set the path.
     * 
     * @param path the new path, it should not be <code>null</code> or empty for a valid path.
     */
    public void setPath(String path)
    {
        this.path = path;
    }
    
    /**
     * Gets the path.
     * 
     * @return the path, it should not be <code>null</code> or empty for a valid path.
     */
    public String getPath()
    {
        return path;
    }
    
    @NotEmpty
    private String name;
    
    @NotNull
    private String title;
    
    @NotNull
    private String description;
    
    @NotEmpty
    private String state;
    
    private String type;
    
    private String id;
    
    private String path;
    
    /**
     * see {@link #getTotalSubmissions()}.
     */
    private int totalSubmissions = 0;
    
    /**
     * see {@link #getNewSubmissions()}.
     */
    private int newSubmissions = 0;
    private String site;

    public String getSite() {
        return site;
    }

    public void setSite(String site) {
        this.site = site;
    }


 
}
