/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package com.percussion.pagemanagement.data;

import net.sf.oval.constraint.AssertValid;
import org.apache.commons.beanutils.BeanUtils;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang.builder.ToStringStyle;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Contains the page regions that will override the templates regions.
 * It also contains the region-widgets assocations ({@link #getRegionWidgetAssociations()}).
 * 
 * @author adamgent
 *
 */
@XmlRootElement(name = "RegionBranches")
public class PSRegionBranches extends PSRegionWidgetAssociations
{
    @AssertValid()
    private List<PSRegion> regions = new ArrayList<>();

    @AssertValid()
    @XmlElementWrapper(name = "regions")
    @XmlElement(name = "region")
    public List<PSRegion> getRegions()
    {
        return regions;
    }
    
    public void setRegions(List<PSRegion> pageRegions)
    {
        this.regions = pageRegions;
    }


    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSRegionBranches)) return false;
        if (!super.equals(o)) return false;
        PSRegionBranches that = (PSRegionBranches) o;
        return Objects.equals(getRegions(), that.getRegions());
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getRegions());
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("PSRegionBranches{");
        sb.append("regions=").append(regions);
        sb.append('}');
        return sb.toString();
    }

    @Override
    public PSRegionBranches clone()
    {
        try
        {
            return (PSRegionBranches) BeanUtils.cloneBean(this);
        }
        catch (Exception e)
        {
            throw new RuntimeException("Cannot clone", e);
        }
    }
    

}
