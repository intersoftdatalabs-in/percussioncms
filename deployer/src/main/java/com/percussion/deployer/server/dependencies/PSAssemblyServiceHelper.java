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
package com.percussion.deployer.server.dependencies;

import com.percussion.error.IPSDeploymentErrors;
import com.percussion.error.PSDeployException;
import com.percussion.extension.IPSExtension;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSAssemblyTemplate;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.assembly.data.PSTemplateSlot;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSCatalogException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.IPSNodeDefinition;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.utils.guid.IPSGuid;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import javax.jcr.RepositoryException;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * A helper class for the new templates, slots, bindings, contentlists etc to use
 *
 * @author vamsinukala
 */
public class PSAssemblyServiceHelper {

  private static final Logger log = LogManager.getLogger(PSAssemblyServiceHelper.class);

  /** CTOR */
  public PSAssemblyServiceHelper() {
    if (m_namedTemplatesMap != null) {
      m_namedTemplatesMap.clear();
      m_namedTemplatesMap = null;
    }
    if (m_guidTemplatesMap != null) {
      m_guidTemplatesMap.clear();
      m_guidTemplatesMap = null;
    }
    if (m_legacyTemplatesMap != null) {
      m_legacyTemplatesMap.clear();
      m_legacyTemplatesMap = null;
    }
    if (m_templateCTMap != null) {
      m_templateCTMap.clear();
      m_templateCTMap = null;
    }
    if (m_templateSlotMap != null) {
      m_templateSlotMap.clear();
      m_templateSlotMap = null;
    }
    if (m_slotContentTypeMap != null) {
      m_slotContentTypeMap.clear();
      m_slotContentTypeMap = null;
    }
    if (m_slotTemplatesMap != null) {
      m_slotTemplatesMap.clear();
      m_slotTemplatesMap = null;
    }
    if (m_slots != null) {
      m_slots.clear();
      m_slots = null;
    }

    m_namedTemplatesMap = new HashMap<>();
    m_guidTemplatesMap = new HashMap<>();
    m_legacyTemplatesMap = new HashMap<>();
    m_templateSlotMap = new HashMap<>();
    m_slotContentTypeMap = new HashMap<>();
    m_slotTemplatesMap = new HashMap<>();
    m_slots = new ArrayList<>();
  }

  /**
   * Helper method to 1. find and load all the **NEW** templates: List<IPSAssemblyTemplate> 2.
   * generate a map m_templatesMap
   *
   * @param doLegacyTmps if <code>true</code> build legacyTemplatesMap else build the new templates
   *     map
   * @throws PSDeployException
   */
  private void doCatalogTemplatesMap(boolean doLegacyTmps) throws PSDeployException {
    Set<IPSAssemblyTemplate> templates = null;
    /*
     * Get all the templates Assembler does not distinguish between old
     * variants and the new tempatles
     */
    try {
      templates = m_assemblySvc.findAllTemplates();
    } catch (PSAssemblyException e) {
      throw new PSDeployException(
          IPSDeploymentErrors.UNEXPECTED_ERROR,
          "Assembly exception occurred while cataloging Templates");
    }

    Iterator<IPSAssemblyTemplate> it = templates.iterator();

    while (it.hasNext()) {
      IPSAssemblyTemplate tmp = (IPSAssemblyTemplate) it.next();
      // separate the legacy(variants) from new templates
      if (doLegacyTmps == false) {
        if (tmp.getAssembler().compareTo(IPSExtension.LEGACY_ASSEMBLER) == 0) continue;
        m_namedTemplatesMap.put(tmp.getName(), tmp);
        m_guidTemplatesMap.put(tmp.getGUID(), tmp);
      } else if (doLegacyTmps == true) {
        if (!tmp.getAssembler().equals(IPSExtension.LEGACY_ASSEMBLER)) continue;
        m_legacyTemplatesMap.put(tmp.getName(), tmp);
      }
    }
  }

  /**
   * Utility Method to init LegacyTemplatesMap
   *
   * @throws PSDeployException
   */
  private void catalogLegacyTemplatesMap() throws PSDeployException {
    doCatalogTemplatesMap(true);
  }

  private void catalogTemplatesMap() throws PSDeployException {
    doCatalogTemplatesMap(false);
  }

  /**
   * Get slots associated with a template
   *
   * @param name the template name
   * @return a list of all the slots associated with this template(name)
   */
  public Set<IPSTemplateSlot> getSlotsByTemplate(String name) {
    return m_templateSlotMap.get(name);
  }

  /**
   * Get all the ContentType GUIDs that reference this template, <b>dynamically.</b>
   *
   * @param t the template definition never <code>null</code>
   * @return the content type guids associated with the template
   */
  public List<IPSGuid> getContentTypesByTemplate(IPSAssemblyTemplate t) {
    List<IPSGuid> cTypes = new ArrayList<>();
    try {
      List<IPSNodeDefinition> nodeDefs = m_contentMgr.findNodeDefinitionsByTemplate(t.getGUID());
      Iterator<IPSNodeDefinition> it = nodeDefs.iterator();
      while (it.hasNext()) {
        IPSNodeDefinition def = it.next();
        cTypes.add(def.getGUID());
      }
    } catch (RepositoryException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
    return cTypes;
  }

  /**
   * Catalog templates which match a pattern in ASSEMBLYURL. Pattern such as "../appname/%" etc
   *
   * @param pattern never <code>null</code>
   * @return the list of templates that satisfy the pattern in assemblyurl. May return <code>null
   *     </code>
   */
  public List<IPSAssemblyTemplate> findTemplatesByAssemblyURL(String pattern) {
    if (StringUtils.isBlank(pattern)) throw new IllegalArgumentException("pattern may not be null");
    List<IPSAssemblyTemplate> tmps = null;
    try {
      tmps = m_assemblySvc.findTemplatesByAssemblyUrl(pattern, false);
    } catch (PSAssemblyException e) { // dont care
    }

    return tmps;
  }

  /**
   * A helper method for catalogTemplateSlotsAssociations
   *
   * @param tmpMap
   */
  private void doCatalogTemplateSlotAssociations(HashMap<String, IPSAssemblyTemplate> tmpMap) {
    Set<String> nameSet = tmpMap.keySet();
    Iterator<String> it = nameSet.iterator();
    while (it.hasNext()) {
      String tmpName = it.next();
      IPSAssemblyTemplate tmp = tmpMap.get(tmpName);
      m_templateSlotMap.put(tmp.getName(), tmp.getSlots());
    }
  }

  /**
   * Create a map of list of all slots(their GUIDs) associated with a template TemplateName <--> Set
   * of SLOTS
   */
  private void catalogTemplateSlotsAssociations() {
    // only set here
    doCatalogTemplateSlotAssociations(m_namedTemplatesMap);
    doCatalogTemplateSlotAssociations(m_legacyTemplatesMap);
  }

  /**
   * Utility method to return all &lt;templateName, ListOfSlots&gt; Map
   *
   * @return &lt;templateName, ListOfSlots&gt;
   */
  public HashMap<String, Set<IPSTemplateSlot>> getTemplateSlotsMap() {
    m_templateSlotMap.clear();
    catalogTemplateSlotsAssociations();
    return m_templateSlotMap;
  }

  /**
   * Utility method to return all &lt;slotGUID, ListOfTemplates&gt; Map
   *
   * @return &lt;slotGUID, ListOfTemplates&gt;
   */
  public HashMap<IPSGuid, Set<IPSAssemblyTemplate>> getSlotTemplatesMap() {
    return m_slotTemplatesMap;
  }

  /**
   * Utility method to return all &lt;slotGUID, ListOfContentTypeDefs&gt; Map
   *
   * @return &lt;slotGUID, ListOfContentTypeDefs&gt; Map
   */
  public HashMap<IPSGuid, Set<IPSNodeDefinition>> getSlotContentTypesMap() {
    return m_slotContentTypeMap;
  }

  /**
   * Utility method to get TemplatesMap
   *
   * @return the hashmap of templates
   * @throws PSDeployException if there are any errors loading the templates.
   */
  public HashMap<String, IPSAssemblyTemplate> getNamedTemplatesMap() throws PSDeployException {
    if (m_namedTemplatesMap != null) {
      m_namedTemplatesMap.clear();
      m_namedTemplatesMap = new HashMap<>();
    }

    if (m_guidTemplatesMap != null) {
      m_guidTemplatesMap.clear();
      m_guidTemplatesMap = new HashMap<>();
    }
    catalogTemplatesMap();
    return m_namedTemplatesMap;
  }

  /**
   * Utility method to get the template &lt;==&gt; ContentType map
   *
   * @return this association template &lt;==&gt; ContentType map
   */
  public HashMap<String, List<IPSNodeDefinition>> getTemplateCTMap() {
    return m_templateCTMap;
  }

  /**
   * Convenience method to return all VARIANTS to be known as Legacry Templates
   *
   * @return a map of &lt;template name, Template&gt;
   * @throws PSDeployException if there are any errors loading the legacy templates.
   */
  public HashMap<String, IPSAssemblyTemplate> getLegacyTemplatesMap() throws PSDeployException {
    m_legacyTemplatesMap.clear();
    m_legacyTemplatesMap = null;
    m_legacyTemplatesMap = new HashMap<>();
    catalogLegacyTemplatesMap();
    return m_legacyTemplatesMap;
  }

  private void catalogSlots() {
    try {
      var summaries = m_assemblySvc.getSummaries(PSTypeEnum.SLOT);
      if (summaries != null && !summaries.isEmpty()) {
        var slotGuids = summaries.stream().map(IPSCatalogSummary::getGUID).toList();
        m_slots = m_assemblySvc.loadSlots(slotGuids);
        m_slots.sort(Comparator.comparingLong(slot -> Math.abs(slot.getGUID().longValue())));
      }
    } catch (PSCatalogException | PSNotFoundException | PSAssemblyException e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }

  /**
   * method name says it all
   *
   * @return assembly service
   */
  public IPSAssemblyService getAssemblySvc() {
    return m_assemblySvc;
  }

  /**
   * clear the list and re-catalog to return all the slots
   *
   * @return List of all slots
   */
  public List<IPSTemplateSlot> getSlots() {
    m_slots.clear();
    catalogSlots();
    return m_slots;
  }

  /** Da assembly Service */
  private static IPSAssemblyService m_assemblySvc = PSAssemblyServiceLocator.getAssemblyService();

  /** ContentManagerService */
  private static IPSContentMgr m_contentMgr = PSContentMgrLocator.getContentMgr();

  /** Map of a list of all the Content Types associated with a template */
  private HashMap<String, List<IPSNodeDefinition>> m_templateCTMap = null;

  /** Map of all the legacy templates name aka VARIANTS,IPSAssemblyTemplate */
  private HashMap<String, IPSAssemblyTemplate> m_legacyTemplatesMap = null;

  /** Map of all the assembly templates name,IPSAssemblyTemplate */
  private HashMap<String, IPSAssemblyTemplate> m_namedTemplatesMap = null;

  /** Map of all the assembly templates name,IPSAssemblyTemplate */
  private HashMap<IPSGuid, IPSAssemblyTemplate> m_guidTemplatesMap = null;

  /** Map of all the slots associated to a template */
  private HashMap<String, Set<IPSTemplateSlot>> m_templateSlotMap = null;

  /** given a slot guid, find all the templates that refer this slot */
  private HashMap<IPSGuid, Set<IPSAssemblyTemplate>> m_slotTemplatesMap = null;

  /** Slot <--> Contenttype List */
  private HashMap<IPSGuid, Set<IPSNodeDefinition>> m_slotContentTypeMap = null;

  /** Listing of TemplateSlots */
  private List<IPSTemplateSlot> m_slots = null;
}

/**
 * Utility sorter for list
 *
 * @author vamsinukala
 */
class SlotsComparer implements Comparator {
  public int compare(Object obj1, Object obj2) {
    return (int)
        (Math.abs(((PSTemplateSlot) obj1).getGUID().longValue())
            - Math.abs(((PSTemplateSlot) obj1).getGUID().longValue()));
  }
}
