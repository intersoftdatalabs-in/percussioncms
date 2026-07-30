/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
import com.percussion.system.utils.PSSiteManageBean;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/** Adaptor for ItemFilter management in Percussion CMS. */
@PSSiteManageBean
public class ItemFilterAdaptor implements IItemFilterAdaptor {

  private final IPSFilterService filterService;
  private static final Logger log = LogManager.getLogger(ItemFilterAdaptor.class);

  public ItemFilterAdaptor() {
    this.filterService = PSFilterServiceLocator.getFilterService();
  }

  /**
   * Get a list of the ItemFilters available on the system populated with rules and parameters.
   *
   * @return A list of item filters
   */
  @Override
  public List<ItemFilter> getItemFilters() {
    return filterService.findAllFilters().stream()
        .map(this::copyFilter)
        .collect(Collectors.toList());
  }

  private ItemFilter copyFilter(IPSItemFilter filter) {
    var ret = new ItemFilter();
    ret.setFilterId(ApiUtils.convertGuid(filter.getGUID()));
    ret.setDescription(filter.getDescription());
    ret.setName(filter.getName());
    ret.setLegacyAuthtype(filter.getLegacyAuthtypeId());

    if (filter.getParentFilter() != null) {
      ret.setParentFilter(copyFilter(filter.getParentFilter()));
    }

    var rules =
        filter.getRuleDefs().stream()
            .map(this::copyItemFilterRuleDef)
            .filter(Objects::nonNull)
            .collect(Collectors.toSet());
    ret.setRules(rules);
    return ret;
  }

  private ItemFilterRuleDefinition copyItemFilterRuleDef(IPSItemFilterRuleDef def) {
    try {
      var ret = new ItemFilterRuleDefinition();
      ret.setName(def.getRuleName());
      ret.setRuleId(ApiUtils.convertGuid(def.getGUID()));

      var retParams =
          def.getParams().entrySet().stream()
              .map(
                  pair -> {
                    var p = new ItemFilterRuleDefinitionParam();
                    p.setName(pair.getKey());
                    p.setValue(pair.getValue());
                    return p;
                  })
              .collect(Collectors.toList());
      ret.setParams(retParams);
      return ret;
    } catch (PSFilterException e) {
      log.error("Error getting ItemFilter Rule Name. Skipping Rule.", e);
      return null;
    }
  }

  /**
   * Update or create an ItemFilter.
   *
   * @param filter The filter to update or create.
   * @return The updated ItemFilter.
   */
  @Override
  public ItemFilter updateOrCreateItemFilter(ItemFilter filter) {
    log.warn("updateOrCreateItemFilter not yet implemented");
    return null;
  }

  /**
   * Delete the specified item filter.
   *
   * @param itemFilterId A valid ItemFilter id. Filter must not be associated with any ContentLists
   *     or it won't be deleted.
   */
  @Override
  public void deleteItemFilter(Guid itemFilterId) throws PSNotFoundException {
    var filter = filterService.loadFilter(ApiUtils.convertGuid(itemFilterId));
    filterService.deleteFilter(filter);
  }

  /**
   * Get a single ItemFilter by id.
   *
   * @param itemFilterId A Valid ItemFilter id
   * @return The ItemFilter
   */
  @Override
  public ItemFilter getItemFilter(Guid itemFilterId) throws PSNotFoundException {
    var filter = filterService.loadFilter(ApiUtils.convertGuid(itemFilterId));
    return copyFilter(filter);
  }

  @Override
  public ItemFilter findItemFilter(String idOrName) {
    if (!isSafeFilterKey(idOrName)) {
      return null;
    }
    String key = idOrName.trim();
    // Prefer name match (common Developer UX); fall back to GUID string.
    for (ItemFilter f : getItemFilters()) {
      if (f != null && key.equalsIgnoreCase(f.getName())) {
        return f;
      }
    }
    try {
      // type-host-uuid or long form accepted by PSGuid
      var guid = new com.percussion.services.guidmgr.data.PSGuid(key);
      return getItemFilter(ApiUtils.convertGuid((com.percussion.utils.guid.IPSGuid) guid));
    } catch (IllegalArgumentException e) {
      // Invalid GUID syntax (incl. NumberFormatException from parse) → generic 404
      log.debug("Invalid item filter GUID syntax: {}", e.getMessage());
      return null;
    } catch (PSNotFoundException e) {
      log.debug("Item filter not found for GUID key: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Single path component / guid token only — reject traversal and separators ({@code
   * java/path-injection}).
   */
  static boolean isSafeFilterKey(String key) {
    if (key == null || key.isBlank()) {
      return false;
    }
    return !key.contains("..")
        && key.indexOf('/') < 0
        && key.indexOf('\\') < 0
        && key.indexOf('\0') < 0;
  }
}
