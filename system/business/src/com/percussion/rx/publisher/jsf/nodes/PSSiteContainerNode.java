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
package com.percussion.rx.publisher.jsf.nodes;

import com.percussion.rx.jsf.PSEditableNodeContainer;
import com.percussion.rx.jsf.PSNodeBase;
import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSCatalogException;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerLocator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This node is the primary container of site items for the design view as well
 * as used in the site list view.
 * 
 * @author dougrand
 * 
 */
public class PSSiteContainerNode extends PSEditableNodeContainer
{
   /**
    * Outcome for the list page.
    */
   public static final String PUB_DESIGN_SITE_VIEWS = "pub-design-site-views";
   
   /**
    * Is this container in the design or runtime trees.
    */
   protected boolean m_design;

   /**
    * The logger for the site container node.
    */
   private static final Logger ms_log =
         LogManager.getLogger(PSSiteContainerNode.class);

   /**
    * Ctor.
    * 
    * @param title the title, never <code>null</code> or empty.
    * @param design <code>true</code> for the design tree, <code>false</code>
    * for the runtime tree.
    */
   /**
    * Constructs a site container node.
    * @param title node title, never null or empty
    * @param design true for design tree, false for runtime tree
    */
   public PSSiteContainerNode(String title, boolean design) {
      super(title, design ? PUB_DESIGN_SITE_VIEWS : PSRuntimeStatusNode.STATUS_VIEW);
      m_design = design;
   }

   // ...existing code...
   /**
    * Gets the child nodes for all sites.
    * @return list of child nodes
    */
   // ...existing code...
   public List<? extends PSNodeBase> getChildren() throws PSNotFoundException {
      synchronized (this) {
         if (m_children != null) return getChildrenWithOneSelected();
         try {
            var sums = getSiteSummaries();
            sums.sort((o1, o2) -> o1.getName().compareToIgnoreCase(o2.getName()));
            for (var s : sums) {
               try {
                  PSNodeBase snode;
                  if (m_design) {
                     var site = getSiteManager().loadSiteModifiable(s.getGUID());
                     snode = new PSSiteNode(site);
                  } else {
                     var site = getSiteManager().loadSite(s.getGUID());
                     snode = new PSRuntimeSiteNode(site);
                  }
                  addNode(snode);
               } catch (PSNotFoundException e) {
                  ms_log.error("Can't load site info: " + s.getName(), e);
               }
            }
         } catch (PSCatalogException | PSNotFoundException e) {
            ms_log.error("Problem loading children for sites", e);
         }
         return getChildrenWithOneSelected();
      }
   }

   /**
    * Gets all child nodes with one selected node.
    * @return the child nodes, may be <code>null</code> or empty.
    */
   /**
    * Gets all child nodes with one selected node.
    * @return child nodes, may be null or empty
    */
   private List<? extends PSNodeBase> getChildrenWithOneSelected() throws PSNotFoundException {
      var childList = super.getChildren();
      if (childList == null || childList.isEmpty()) return childList;
      var isOneSelected = childList.stream().anyMatch(PSNodeBase::getSelectedRow);
      if (!isOneSelected) childList.get(0).setSelectedRow(true);
      return childList;
   }
   
   /**
    * Provides summaries for all the sites.
    * @return the site summaries. Never <code>null</code>.
    * @throws PSCatalogException if cataloging fails.
    */
   /**
    * Provides summaries for all the sites.
    * @return site summaries, never null
    */
   private List<IPSCatalogSummary> getSiteSummaries() throws PSCatalogException, PSNotFoundException {
      return getSiteManager().getSummaries(PSTypeEnum.SITE);
   }

   /**
    * A convenience method to access
    * {@link PSSiteManagerLocator#getSiteManager()}.
    * @return the site manager. Never <code>null</code>.
    */
   /**
    * Gets the site manager.
    * @return site manager, never null
    */
   private IPSSiteManager getSiteManager() {
      return PSSiteManagerLocator.getSiteManager();
   }

   // ...existing code...
   /**
    * Returns true if the container is empty.
    */
   // ...existing code...
   public boolean isContainerEmpty() {
      if (m_children != null) return m_children.isEmpty();
      return false;
   }
   
   /**
    * Action to create a new site, and add it to the tree.
    * @return the perform action for the site node, which will navigate to the
    * editor.
    */
   /**
    * Creates a new site and adds it to the tree.
    * @return perform action for the new site node
    */
   public String createSite() throws PSNotFoundException {
      var smgr = getSiteManager();
      var newsite = smgr.createSite();
      newsite.setName(getUniqueName("Site", false));
      var node = new PSSiteNode(newsite);
      return node.handleNewSite(this, node);
   }

   // see base
   // ...existing code...
   /**
    * Finds a site by name.
    * @param name site name
    * @return true if found, false otherwise
    */
   // ...existing code...
   protected boolean findObjectByName(String name) {
      var smgr = PSSiteManagerLocator.getSiteManager();
      try {
         smgr.loadSite(name);
         return true;
      } catch (PSNotFoundException e) {
         return false;
      }
   }

   // ...existing code...
   /**
    * Gets all site names.
    * @return set of site names
    */
   // ...existing code...
   public Set<Object> getAllNames() {
      var names = new HashSet<>();
      try {
         for (var summary : getSiteSummaries()) {
            names.add(summary.getName());
         }
      } catch (PSCatalogException | PSNotFoundException e) {
         ms_log.error("Problem obtaining site names", e);
      }
      return names;
   }

   // ...existing code...
   /**
    * Returns to the site list view.
    */
   // ...existing code...
   public String returnToListView() {
      return "return-to-sites";
   }
   
   // ...existing code...
   /**
    * Gets the help topic for this node.
    */
   // ...existing code...
   public String getHelpTopic() {
      return "SiteList";
   }
}
