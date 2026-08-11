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
package com.percussion.pso.relationships;

// REFACTORED: CP-JAVA11
import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.cms.objectstore.PSRelationshipProcessorProxy;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.design.objectstore.PSRelationshipSet;
import com.percussion.error.PSException;
import com.percussion.pso.utils.PSORequestContext;
import com.percussion.pso.utils.UniqueIdLocatorSet;
import com.percussion.pso.workflow.IPSOWorkflowInfoFinder;
import com.percussion.pso.workflow.PSOWorkflowInfoFinder;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.system.utils.IPSHtmlParameters;
import java.util.List;
import java.util.Set;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Finds parent items for a given content item.
 *
 * @author DavidBenua
 */
public class PSOParentFinder implements IPSOParentFinder {
  // REFACTORED: CP-JAVA11

  private IPSRequestContext requestContext = null;
  private PSRelationshipProcessorProxy proxy = null;
  private IPSGuidManager gmgr = null;
  private IPSAssemblyService asm = null;
  private IPSOWorkflowInfoFinder workflow;

  private static final Logger log = LogManager.getLogger(PSOParentFinder.class);

  /**
   * Default constructor.
   * Creates a new PSOParentFinder.
   *
   */
  public PSOParentFinder() {
    workflow = new PSOWorkflowInfoFinder();
  }

  /**
   * findAllParents operation.
   *
   * @see com.percussion.pso.relationships.IPSOParentFinder#findAllParents(java.lang.String,
   *     java.lang.String)
   * @param contentid the contentid
   * @param slotName the slot name
   * @return the result
   * @throws PSAssemblyException if an error occurs
   * @throws PSCmsException if an error occurs
   */
  public Set<PSLocator> findAllParents(String contentid, String slotName)
      throws PSAssemblyException, PSCmsException {
    PSLocator dependent = new PSLocator(contentid);
    return findAllParents(dependent, slotName);
  }

  /**
   * findAllParents operation.
   *
   * @see
   *     com.percussion.pso.relationships.IPSOParentFinder#findAllParents(com.percussion.design.objectstore.PSLocator,
   *     java.lang.String)
   * @param dependent the dependent
   * @param slotName the slot name
   * @return the result
   * @throws PSAssemblyException if an error occurs
   * @throws PSCmsException if an error occurs
   */
  public Set<PSLocator> findAllParents(PSLocator dependent, String slotName)
      throws PSAssemblyException, PSCmsException {
    Set<PSLocator> parents = new UniqueIdLocatorSet();
    // add the parents for the current revision
    parents.addAll(findParents(dependent, slotName, false));
    // add the parents for the last public revision
    parents.addAll(findParents(dependent, slotName, true));
    return parents;
  }

  /**
   * findParents operation.
   *
   * @see com.percussion.pso.relationships.IPSOParentFinder#findParents(java.lang.String,
   *     java.lang.String, boolean)
   * @param contentid the contentid
   * @param slotName the slot name
   * @param usePublic the use public
   * @return the result
   * @throws PSAssemblyException if an error occurs
   * @throws PSCmsException if an error occurs
   */
  public Set<PSLocator> findParents(String contentid, String slotName, boolean usePublic)
      throws PSAssemblyException, PSCmsException {
    PSLocator dependent = new PSLocator(contentid);
    return findParents(dependent, slotName, usePublic);
  }

  /**
   * findParents operation.
   *
   * @see
   *     com.percussion.pso.relationships.IPSOParentFinder#findParents(com.percussion.design.objectstore.PSLocator,
   *     java.lang.String, boolean)
   * @param dependent the dependent
   * @param slotName the slot name
   * @param usePublic the use public
   * @return the result
   * @throws PSAssemblyException if an error occurs
   * @throws PSCmsException if an error occurs
   */
  public Set<PSLocator> findParents(PSLocator dependent, String slotName, boolean usePublic)
      throws PSAssemblyException, PSCmsException {
    initServices();
    String slotid = getSlotId(slotName);
    log.debug("Slot name {} id is {}", slotName, slotid);
    PSRelationshipFilter filter = new PSRelationshipFilter();
    filter.setDependent(dependent);
    filter.setCategory(PSRelationshipFilter.FILTER_CATEGORY_ACTIVE_ASSEMBLY);
    if (usePublic) {
      filter.limitToPublicOwnerRevision(true);
    } else {
      filter.limitToEditOrCurrentOwnerRevision(true);
    }
    filter.setProperty(IPSHtmlParameters.SYS_SLOTID, slotid);
    PSRelationshipSet rels = proxy.getRelationships(filter);
    log.debug("there were " + rels.size() + " parents found");
    Set<PSLocator> parents = new UniqueIdLocatorSet();
    for (Object relobj : rels) {
      PSRelationship rel = (PSRelationship) relobj;
      PSLocator parent = rel.getOwner();
      parents.add(parent);
    }

    return parents;
  }

  /**
   * hasOnlyPublicAncestors operation.
   *
   * @see com.percussion.pso.relationships.IPSOParentFinder#hasOnlyPublicAncestors(java.lang.String,
   *     java.lang.String, java.util.List)
   * @param contentId the content id
   * @param slotName the slot name
   * @param validFlags the valid flags
   * @return the result
   * @throws PSAssemblyException if an error occurs
   * @throws PSException if an error occurs
   */
  public boolean hasOnlyPublicAncestors(String contentId, String slotName, List<String> validFlags)
      throws PSAssemblyException, PSException {
    Set<PSLocator> parents = this.findAllParents(contentId, slotName);
    for (PSLocator p : parents) {
      String id = p.getPart(PSLocator.KEY_ID);
      if (!workflow.IsWorkflowValid(id, validFlags)) {
        return false;
      }
      if (!hasOnlyPublicAncestors(id, slotName, validFlags)) {
        return false;
      }
    }
    return true;
  }

  /**
   * Gets the slotid from the slot name
   *
   * @param slotName the slot name.
   * @return the slot id.
   * @throws PSAssemblyException if the named slot does not exist.
   */
  protected String getSlotId(String slotName) throws PSAssemblyException {
    initServices();
    IPSTemplateSlot slot = asm.findSlotByName(slotName);
    int slotid = slot.getGUID().getUUID();
    return String.valueOf(slotid);
  }

  private void initServices() {
    if (gmgr == null) {
      gmgr = PSGuidManagerLocator.getGuidMgr();
    }
    if (asm == null) {
      asm = PSAssemblyServiceLocator.getAssemblyService();
    }
    if (proxy == null) {
      try {
        requestContext = new PSORequestContext();
        proxy =
            new PSRelationshipProcessorProxy(
                PSRelationshipProcessorProxy.PROCTYPE_SERVERLOCAL, requestContext);
      } catch (PSCmsException ex) {
        log.error("Unexpected Exception initializing proxy, Error: {}", ex.getMessage());
        log.debug(ex.getMessage(), ex);
      }
    }
  }

  /**
   * Sets the proxy.
   *
   * @param proxy the proxy to set
   */
  public void setProxy(PSRelationshipProcessorProxy proxy) {
    this.proxy = proxy;
  }

  /**
   * Sets the gmgr.
   *
   * @param gmgr the gmgr to set
   */
  public void setGmgr(IPSGuidManager gmgr) {
    this.gmgr = gmgr;
  }

  /**
   * Sets the asm.
   *
   * @param asm the asm to set
   */
  public void setAsm(IPSAssemblyService asm) {
    this.asm = asm;
  }
}
