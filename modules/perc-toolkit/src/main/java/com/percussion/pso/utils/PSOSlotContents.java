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
/*
 * com.percussion.pso.utils PSOSlotContents.java
 *
 * @author DavidBenua
 *
 */
package com.percussion.pso.utils;

import com.percussion.cms.objectstore.PSAaRelationship;
import com.percussion.cms.objectstore.PSRelationshipFilter;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.services.assembly.IPSAssemblyService;
import com.percussion.services.assembly.IPSTemplateSlot;
import com.percussion.services.assembly.PSAssemblyException;
import com.percussion.services.assembly.PSAssemblyServiceLocator;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.content.IPSContentWs;
import com.percussion.webservices.content.PSContentWsLocator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Tool for loading the contents of a slot as PSAaRelationship objects.
 *
 * <p>The standard load method in Web Services {@link
 * IPSContentWs#loadContentRelations(PSRelationshipFilter, boolean)} loads all content
 * relationships, regardless of slot. This tool uses this method and returns a filtered and sorted
 * list of <code>PSAaRelationship</code> objects.
 *
 * <p>Note that this tool is based on relationships and does not work on autoslots. If you are
 * assembling a item, consider using {@link
 * com.percussion.pso.jexl.PSOSlotTools#getSlotContents(com.percussion.services.assembly.IPSAssemblyItem,
 * String, java.util.Map)} instead of this method.
 *
 * <p>The returned slot has not been filtered by any item filters, and the revisions of the
 * dependent items will not have been set.
 *
 * <p>The implementation of this method is highly dependent on the sortrank property of the
 * relationships having been set correctly. The behavior when the sortrank is missing or invalid
 * (e.g. 0 or -1) may be inconsistent.
 *
 * @see com.percussion.webservices.content.IPSContentWs#loadContentRelations(PSRelationshipFilter,
 *     boolean)
 * @author DavidBenua
 */
public class PSOSlotContents {
  private static final Logger log = LogManager.getLogger(PSOSlotContents.class);

  private static IPSContentWs cws = null;
  private static IPSGuidManager gmgr = null;
  private static IPSAssemblyService mAss;

  /**
   * Default constructor.
   * Creates a new PSOSlotContents.
   *
   */
  public PSOSlotContents() {}

  /**
   * Gets the contents of a slot.
   *
   * @param parentItem the parent item
   * @param slot the slot
   * @return all relationships in the given slot for this parent. Never <code>null</code>. May be
   *     <code>empty</code>.
   * @throws PSErrorException if an error occurs
   */
  public List<PSAaRelationship> getSlotContents(IPSGuid parentItem, IPSGuid slot)
      throws PSErrorException {
    initServices();

    SortedSet<PSAaRelationship> slotRelations =
        new TreeSet<PSAaRelationship>(new SlotItemComparator());

    PSRelationshipFilter filter = new PSRelationshipFilter();
    filter.setCategory(PSRelationshipFilter.FILTER_CATEGORY_ACTIVE_ASSEMBLY);
    PSLocator oloc = gmgr.makeLocator(parentItem);
    log.debug("Owner locator is  " + oloc);
    log.debug("Owner Slot GUID is " + slot);

    filter.setOwner(oloc);

    // load ALL AA relations for the parent item
    // Note that Slot will be null unless we load the reference info.
    List<PSAaRelationship> allRelations = cws.loadContentRelations(filter, true);
    log.debug("this item has " + allRelations.size() + " active assembly children ");

    for (PSAaRelationship rel : allRelations) {
      // log.debug("returned slot GUID is " + rel.getSlotId());
      if (slot.equals(
          rel.getSlotId())) { // this item is in our slot. Order will be determined by the
        // comparator.
        // log.debug("found matching slot");
        slotRelations.add(rel);
      }
      //         else
      //         {
      //            log.debug("no match on slot");
      //         }
    }

    // we just need our slot as a list.
    List<PSAaRelationship> outputRelations = new ArrayList<PSAaRelationship>(slotRelations);
    return outputRelations;
  }

  private static void initServices() {
    if (cws == null) {
      gmgr = PSGuidManagerLocator.getGuidMgr();
      cws = PSContentWsLocator.getContentWebservice();
    }
  }

  /**
   * Compares two AA Relationships by sort rank. Since this comparator depends solely on the sort
   * rank, it may not be consistent with the contract of the Set interface.
   *
   * <p>Note: this comparator imposes orderings that are inconsistent with equals.
   *
   * @author DavidBenua
   */
  protected class SlotItemComparator implements Comparator<PSAaRelationship> {
    /**
     * Creates a new SlotItemComparator.
     */
    public SlotItemComparator() {}

    /**
     * Compares PSAaRelationships by sort rank.
     *
     * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
     * @param rel1 the rel1
     * @param rel2 the rel2
     * @return the result
     */
    public int compare(PSAaRelationship rel1, PSAaRelationship rel2) {
      if (rel1 == null || rel2 == null) {
        String emsg = "cannot compare null relationships";
        log.error(emsg);
        throw new IllegalArgumentException(emsg);
      }
      int sr1 = rel1.getSortRank();
      int sr2 = rel2.getSortRank();

      if (sr1 == sr2) return 0;
      if (sr1 < sr2) return -1;
      return 1;
    }

    /**
     * All SlotItemComparators return the same order.
     *
     * @see java.lang.Object#equals(java.lang.Object)
     * @param obj the obj
     * @return the result
     */
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof SlotItemComparator) {
        return true;
      }
      return super.equals(obj);
    }

    /**
     * All instances of this comparator are equal, so all share one hash code.
     * @return the result
     */
    @Override
    public int hashCode() {
      return 1;
    }
  }

  /**
   * Sets the cws.
   * @param cws the cws to set. Used for testing.
   */
  public void setCws(IPSContentWs cws) {
    PSOSlotContents.cws = cws;
  }

  /**
   * Sets the gmgr.
   * @param gmgr the gmgr to set. Used for testing
   */
  public void setGmgr(IPSGuidManager gmgr) {
    PSOSlotContents.gmgr = gmgr;
  }

  /***
   * Loads the specified slot.
   * @param name the name of the slot
   * @return Null if the slot is not found, otherwise a valie IPSTemplateSlot instance for the specified slot.
   */
  public static IPSTemplateSlot getSlot(String name) {
    IPSTemplateSlot ret = null;

    try {
      ret = getAssemblyService().findSlotByName(name);
      log.debug("Loaded slot " + name);
    } catch (PSAssemblyException e) {
      log.error("Unable to load slot " + name);
    }

    return ret;
  }

  /**
   * Returns the assembly service.
   *
   * @return the result
   */
  protected static IPSAssemblyService getAssemblyService() {
    if (mAss == null) {
      mAss = PSAssemblyServiceLocator.getAssemblyService();
    }
    return mAss;
  }
}
