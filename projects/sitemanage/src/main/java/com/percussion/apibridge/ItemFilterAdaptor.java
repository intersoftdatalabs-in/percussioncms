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

package com.percussion.apibridge;

import com.percussion.rest.Guid;
import com.percussion.rest.itemfilter.IItemFilterAdaptor;
import com.percussion.rest.itemfilter.ItemFilter;
import com.percussion.rest.itemfilter.ItemFilterRuleDefinition;
import com.percussion.rest.itemfilter.ItemFilterRuleDefinitionParam;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.filter.IPSFilterService;
import com.percussion.services.filter.IPSItemFilter;
import com.percussion.services.filter.IPSItemFilterRuleDef;
import com.percussion.services.filter.PSFilterException;
import com.percussion.services.filter.PSFilterServiceLocator;
import com.percussion.util.PSSiteManageBean;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@PSSiteManageBean
public class ItemFilterAdaptor implements IItemFilterAdaptor {

    private IPSFilterService filterService;
    private static final Logger log = LogManager.getLogger(ItemFilterAdaptor.class);

    public ItemFilterAdaptor(){
        filterService = PSFilterServiceLocator.getFilterService();
    }


    /***
     * Get a list of the ItemFilters available on the system populated with rules and parameters.
     * @return A list of item filters
     */
    @Override
    public List<ItemFilter> getItemFilters() {
        List<ItemFilter> ret = new ArrayList<>();
        List<IPSItemFilter> filters = filterService.findAllFilters();

        for(IPSItemFilter i : filters){
            ret.add(copyFilter(i));
        }
        return ret;
    }

    private ItemFilter copyFilter(IPSItemFilter filter){
        ItemFilter ret  = new ItemFilter();

        ret.setFilter_id(ApiUtils.convertGuid(filter.getGUID()));
        ret.setDescription(filter.getDescription());
        ret.setName(filter.getName());
        ret.setLegacyAuthtype(filter.getLegacyAuthtypeId());

        if(filter.getParentFilter() != null){
            ret.setParentFilter(copyFilter(filter.getParentFilter()));
        }

        Set<IPSItemFilterRuleDef> ruleDefs = filter.getRuleDefs();
        Set<ItemFilterRuleDefinition> rules = new HashSet<>();
        for(IPSItemFilterRuleDef def : ruleDefs){
            ItemFilterRuleDefinition r = copyItemFilterRuleDef(def);
            if(r != null) {
                rules.add(r);
            }
        }
        ret.setRules(rules);
        return ret;
    }

    private ItemFilterRuleDefinition copyItemFilterRuleDef(IPSItemFilterRuleDef def) {
        ItemFilterRuleDefinition ret = new ItemFilterRuleDefinition();

        try {
            ret.setName(def.getRuleName());
            ret.setRuleId(ApiUtils.convertGuid(def.getGUID()));

            Map<String,String> params = def.getParams();
            List<ItemFilterRuleDefinitionParam> retParams = new ArrayList<>();
            for(Map.Entry<String,String> pair : params.entrySet()){
                ItemFilterRuleDefinitionParam p = new ItemFilterRuleDefinitionParam();
                p.setName(pair.getKey());
                p.setValue(pair.getValue());
                retParams.add(p);
            }
           ret.setParams(retParams);
        } catch (PSFilterException e) {
            log.error("Error getting ItemFilter Rule Name.  Skipping Rule.", e);
            ret = null;
        }
        return ret;
    }


    /***
     * Update or create an ItemFilter
     * @param filter  The filter to update or create.
     * @return The updated ItemFilter.
     */
    @Override
    public ItemFilter updateOrCreateItemFilter(ItemFilter filter) {
        //TODO: Implement Me
        log.warn("updateOrCreateItemFilter not yet implemented");
        return null;
    }

    /***
     * Delete the specified item filter.
     * @param itemFilterId A valid ItemFilter id.  Filter must not be associated with any ContentLists or it won't be deleted.
     */
    @Override
    public void deleteItemFilter(Guid itemFilterId) throws PSNotFoundException {
        IPSItemFilter filter = filterService.loadFilter(ApiUtils.convertGuid(itemFilterId));

        filterService.deleteFilter(filter);
    }

    /***
     * Get a single ItemFilter by id.
     * @param itemFilterId  A Valid ItemFilter id
     * @return The ItemFilter
     */
    @Override
    public ItemFilter getItemFilter(Guid itemFilterId) throws PSNotFoundException {
        IPSItemFilter filter = filterService.loadFilter(ApiUtils.convertGuid(itemFilterId));
        return  copyFilter(filter);
    }
}
