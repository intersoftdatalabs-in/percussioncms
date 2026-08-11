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
package com.percussion.pagemanagement.assembler.impl;

import com.percussion.rx.publisher.IPSAssemblyResultExpander;
import com.percussion.rx.publisher.impl.PSAbstractAssemblyResultExpander;
import com.percussion.rx.publisher.impl.PSPublishHandler;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyResult;
import com.percussion.system.utils.PSSiteManageBean;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * Expands an assembly result into multiple assembly items based on the resource definitions
 * associated with the assembly item's content type.
 *
 * <p>For example, an assembly result for an image content type might have two resource definitions:
 * full and thumbnail. Thus, full and thumbnail would be expanded to two assembly items with the
 * full and thumbnail resource definition ids added to each assembly item respectively.
 *
 * <p>This expander also uses the newer link/location services to generate the delivery location of
 * the assembly item.
 *
 * @author adamgent
 */
@PSSiteManageBean("resourceAssemblyResultExpander")
public class PSResourceAssemblyResultExpander extends PSAbstractAssemblyResultExpander
    implements IPSAssemblyResultExpander {

  public static final String ASSEMBLY_RESULT_EXPANDER_NAME =
      PSResourceAssemblyResultExpander.class.getSimpleName();

  private PSAssemblyItemBridge assemblyItemBridge;

  @Autowired
  public PSResourceAssemblyResultExpander(
      PSAssemblyItemBridge assemblyItemBridge, PSPublishHandler publishHandler) {
    this(assemblyItemBridge, publishHandler, ASSEMBLY_RESULT_EXPANDER_NAME);
  }

  /**
   * Intentional publish-to-registry: expander must be visible to the publish handler immediately.
   * Justified {@code this-escape} suppress (leaf bean; no subclass observes partial init).
   */
  @SuppressWarnings("this-escape")
  protected PSResourceAssemblyResultExpander(
      PSAssemblyItemBridge assemblyItemBridge,
      PSPublishHandler publishHandler,
      String expanderName) {
    super();
    this.assemblyItemBridge = assemblyItemBridge;
    // Register our result expander with the publishing handler.
    publishHandler.getAssemblyResultExpanders().put(expanderName, this);
  }

  @Override
  public List<IPSAssemblyItem> expand(IPSAssemblyResult result) throws Exception {
    var resources = new ArrayList<>(getAssemblyItemBridge().getResourceDefinitions(result));
    // AssetResource list is backed by a HashSet; need to order in a consistent way to get
    // page numbers correct. Primary resource should always be page 1.
    resources.sort(
        (s1, s2) -> {
          if (s1.isPrimary() || s2.isPrimary()) {
            return (s1.isPrimary() == s2.isPrimary() ? 0 : (s2.isPrimary() ? 1 : -1));
          }
          return s1.getUniqueId().compareTo(s2.getUniqueId());
        });

    var assemblyItems = new ArrayList<IPSAssemblyItem>();
    var isPaginated = resources.size() > 1;
    int i = 1;

    for (var r : resources) {
      var assemblyItem = clone(result);
      if (isPaginated) {
        assemblyItem.setPage(i);
      }
      if (i == 1) {
        assemblyItem.setReferenceId(result.getReferenceId());
      }
      getAssemblyItemBridge().setResourceDefinititionId(assemblyItem, r.getUniqueId());
      var location = getAssemblyItemBridge().getResourceLocation(assemblyItem);
      if (location != null) {
        PSAssemblyItemBridge.setDeliveryLocation(assemblyItem, location.getFilePath());
      }
      // We assume we should not have to set the template as it will be the
      // resource template that requested this expander.
      assemblyItems.add(assemblyItem);
      i++;
    }
    return assemblyItems;
  }

  protected PSAssemblyItemBridge getAssemblyItemBridge() {
    return assemblyItemBridge;
  }
}
