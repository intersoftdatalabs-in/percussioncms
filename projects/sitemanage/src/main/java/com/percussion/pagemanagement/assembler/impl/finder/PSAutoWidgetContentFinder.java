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

package com.percussion.pagemanagement.assembler.impl.finder;

import com.percussion.cms.IPSConstants;
import com.percussion.pagemanagement.assembler.PSWidgetInstance;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.PSBaseServiceLocator;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.impl.finder.IPSAutoFinderUtils;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * The auto widget content finder allows a widget to be filled with items returned by a query.
 *
 * <p><b>Parameters:</b>
 *
 * <ul>
 *   <li><b>query</b> - The JSR-170 query to be performed
 *   <li><b>max_results</b> - Optional parameter. It is the maximum number of the returned result
 *       from the find method if specified, zero or negative indicates no limit. It defaults to zero
 *       if not specified.
 * </ul>
 *
 * @author YuBingChen
 */
public class PSAutoWidgetContentFinder extends PSWidgetContentFinder {

  public PSAutoWidgetContentFinder() {
    super();
    this.utils = (IPSAutoFinderUtils) PSBaseServiceLocator.getBean("sys_autoFinderUtils");
  }

  @Override
  protected Set<ContentItem> getContentItems(
      IPSAssemblyItem sourceItem, PSWidgetInstance widget, Map<String, Object> params) {
    // Use var and Optional for clarity and null-safety
    var items = new HashSet<ContentItem>();
    try {
      var foundItems =
          utils.getContentItems(sourceItem, -1, params, sourceItem.getTemplate().getGUID());
      items =
          super.filter(foundItems, sourceItem.getFilter(), params).stream()
              .collect(Collectors.toCollection(HashSet::new));
    } catch (Exception e) {
      ms_logger.error(
          "Error getting content items for the given finder. "
              + "\nPlease make sure the query parameters are valid. Error: {}",
          PSExceptionUtils.getMessageForLog(e));
    }
    return items;
  }

  public IPSAutoFinderUtils getUtils() {
    return utils;
  }

  public void setUtils(IPSAutoFinderUtils utils) {
    this.utils = utils;
  }

  /** The utility object, used to fetch the content items. */
  private IPSAutoFinderUtils utils;

  /** Log for this class. */
  private static final Logger ms_logger = LogManager.getLogger(IPSConstants.ASSEMBLY_LOG);
}
