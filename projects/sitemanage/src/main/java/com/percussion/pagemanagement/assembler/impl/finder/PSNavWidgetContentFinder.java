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

package com.percussion.pagemanagement.assembler.impl.finder;

import com.percussion.pagemanagement.assembler.PSWidgetInstance;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.impl.finder.PSNavFinderUtils;

import javax.jcr.RepositoryException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The navigation widget content finder looks up a related navigation node 
 * (navon or navtree) for a specified item. The navigation node and the given 
 * item are under the same folder. The navigation node can be accessed from  
 * "$nav.self" binding of the returned assembly item. The navigation node 
 * implements IPSProxyNode. In addition, the binding of "$nav.root" is 
 * the root of the navigation.
 * <p>
 * All navigation nodes, from the related node to the root of the navigation
 * are filtered by the item filter, which is specified in the given item.
 * 
 * @author YuBingChen
 */
public class PSNavWidgetContentFinder extends PSWidgetContentFinder
{
    @Override
    public List<IPSAssemblyItem> find(IPSAssemblyItem sourceItem,
            PSWidgetInstance widget, Map<String, Object> params)
            throws RepositoryException, PSAssemblyException
    {
        IPSAssemblyItem item = PSNavFinderUtils.findItem(sourceItem, null);
        if (item == null)
            return Collections.emptyList();
        return Collections.singletonList(item);
    }       

    protected Set<ContentItem> getContentItems(
            IPSAssemblyItem item, PSWidgetInstance widget, Map<String, Object> params)
    {
        // this is not used, do nothing
        return null;
    }
}
