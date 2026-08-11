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

package com.percussion.pso.relationshipbuilder;

import static java.util.Collections.singleton;

import com.percussion.cms.objectstore.PSAaRelationship;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.error.PSException;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.system.utils.IPSHtmlParameters;
import java.util.Collection;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * PSActiveAssemblyRelationshipBuilder class.
 */
public abstract class PSActiveAssemblyRelationshipBuilder extends PSRelationshipBuilder {

  /** The log instance to use for this class, never <code>null</code>. */
  private static final Logger log = LogManager.getLogger(PSActiveAssemblyRelationshipBuilder.class);

  private String slotName;
  private String templateName;

  private IPSAssemblyService m_assemblyService;

  /**
   * Creates a new PSActiveAssemblyRelationshipBuilder.
   */
  public PSActiveAssemblyRelationshipBuilder() {}

  /**
   * Creates a new PSActiveAssemblyRelationshipBuilder.
   *
   * @param isParent {@code true} when the updated item is the relationship owner
   * @see PSRelationshipBuilder#PSRelationshipBuilder(boolean)
   */
  protected PSActiveAssemblyRelationshipBuilder(boolean isParent) {
    super(isParent);
  }

  /**
   * init operation.
   */
  public void init() {
    if (m_assemblyService == null) {
      m_assemblyService = PSAssemblyServiceLocator.getAssemblyService();
    }
    super.init();
  }

  /**
   * Sets the up filter.
   *
   * @throws PSAssemblyException if an error occurs
   * @throws PSException if an error occurs
   */
  protected void setupFilter() throws PSAssemblyException, PSException {
    if (slotName != null) {
      IPSTemplateSlot slot = findSlot(slotName);
      super.getFilter()
          .setProperty(IPSHtmlParameters.SYS_SLOTID, String.valueOf(slot.getGUID().longValue()));
    }

    if (templateName != null) {
      IPSAssemblyTemplate template = findTemplate(templateName);
      super.getFilter()
          .setProperty(
              IPSHtmlParameters.SYS_VARIANTID, String.valueOf(template.getGUID().longValue()));
    }
    super.setupFilter();
  }

  /**
   * Returns the slot name.
   *
   * @return the result
   */
  public String getSlotName() {
    return slotName;
  }

  /**
   * Sets the slot name.
   *
   * @param slotName the slot name
   */
  public void setSlotName(String slotName) {
    this.slotName = slotName;
  }

  /**
   * Returns the template name.
   *
   * @return the result
   */
  public String getTemplateName() {
    return templateName;
  }

  /**
   * Sets the template name.
   *
   * @param templateName the template name
   */
  public void setTemplateName(String templateName) {
    this.templateName = templateName;
  }

  /**
   * addRelationships operation.
   *
   * @param ids the ids
   * @throws PSAssemblyException if an error occurs
   * @throws PSException if an error occurs
   */
  public void addRelationships(Collection<Integer> ids) throws PSAssemblyException, PSException {
    IPSAssemblyTemplate template = findTemplate(templateName);
    IPSTemplateSlot slot = findSlot(slotName);
    validateSlot(slot);
    Collection<PSLocator> ownerLocators =
        isParent() ? asLocators(singleton(getId())) : asLocators(ids);
    Collection<PSLocator> dependentLocators =
        isParent() ? asLocatorsNoRev(ids) : asLocatorsNoRev(singleton(getId()));
    Collection<PSRelationship> relationshipSet = createEmptyRelationshipCollection();
    for (PSLocator ownerLoc : ownerLocators) {
      for (PSLocator dependentLoc : dependentLocators) {
        PSAaRelationship newRelationship =
            new PSAaRelationship(ownerLoc, dependentLoc, slot, template);
        log.debug(
            "Adding relation Owner id="
                + ownerLoc.getId()
                + " Owner Revision="
                + ownerLoc.getRevision());
        log.debug(
            "Adding relation Dependent id="
                + dependentLoc.getId()
                + " Dependent Revision="
                + dependentLoc.getRevision());

        relationshipSet.add(newRelationship);
      }
    }
    saveRelationships(relationshipSet);
  }

  /**
   * Validates that the slot is setup correctly to add relationships to. Emits log messages to help
   * the user find errors.
   *
   * @param  the slot
   * @return 0 if successful, non-zero otherwise.
   */
  private int validateSlot(IPSTemplateSlot slot) {
    int rvalue = 1;
    if (slot.getRelationshipName() == null || StringUtils.isBlank(slot.getRelationshipName())) {
      log.warn(
          "The slot does not have relationship name set."
              + "The relationship name should be active assembly."
              + "Check the Slot type table to make sure the relationship name is set.");
      rvalue = 1;
    } else {
      rvalue = 0;
    }

    return rvalue;
  }

  /**
   * Finds the definition for a slot given its name, using the assembly service.
   *
   * @param templateName name of the template to find. not <code>null</code>, must exist.
   * @throws PSAssemblyException if the template is not found
   */
  private IPSAssemblyTemplate findTemplate(String templateName) throws PSAssemblyException {
    if (m_assemblyService == null) init();
    return m_assemblyService.findTemplateByName(templateName);
  }

  /**
   * Finds the definition for a slot given its name, using the assembly service.
   *
   * @param slotname name of the slot to find. not <code>null</code>, must exist.
   * @return the slot definition for the specified name
   * @throws PSAssemblyException propagated from assembly service if the slot is not found
   */
  private IPSTemplateSlot findSlot(String slotname) throws PSAssemblyException {
    if (m_assemblyService == null) init();
    return m_assemblyService.findSlotByName(slotname);
  }
}
