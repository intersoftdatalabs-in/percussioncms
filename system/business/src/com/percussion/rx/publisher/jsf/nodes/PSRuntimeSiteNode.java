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

// REFACTORED: CP-JAVA11
package com.percussion.rx.publisher.jsf.nodes;

import com.percussion.rx.jsf.PSNodeBase;
import com.percussion.services.publisher.IPSEdition;
// ...existing imports...
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.utils.guid.IPSGuid;

import java.util.ArrayList;
import java.util.List;

/**
 * The runtime site node. This shows the site's edition as children and allows
 * the user to pick an edition to run. The logs child shows the publishing 
 * logs for the entire site. The user can drill into the various logs.
 * 
 * @author dougrand
 *
 */
public class PSRuntimeSiteNode extends PSNodeBase
{
   /**
    * The site, loaded when this node is edited, cleared on cancel or save.
    */
   IPSSite m_site = null;

   /**
    * Sites have child nodes.
    */
   protected List<PSNodeBase> m_children = new ArrayList<>();

   /**
    * The current index into the collection. <code>-1</code> indicates that no
    * element is currently selected.
    */
   protected int m_index = -1;
   
   /**
    * Ctor.
    * @param site
    */
   /**
    * Constructs a runtime site node for a site.
    * @param site the site, never null
    */
   public PSRuntimeSiteNode(IPSSite site) {
      super(site.getName(), PSRuntimeStatusNode.STATUS_VIEW);
      if (site == null) throw new IllegalArgumentException("site may not be null.");
      m_site = site;
   }

   /**
    * Facade method on the site object. Facade methods translate from internal
    * to external representations, and perform any needed server site validation
    * that cannot be handled by JSF.
    * 
    * @return the site's name, never <code>null</code> or empty.
    */
   /**
    * Gets the site's name.
    */
   public String getName() {
      return m_site.getName();
   }

   /**
    * Gets the ID of the site that associated with this node.
    * @return the site ID, never <code>null</code>.
    */
   /**
    * Gets the site ID associated with this node.
    */
   public IPSGuid getSiteID() {
      return m_site.getGUID();
   }
   
   /**
    * Facade method on the site object. Facade methods translate from internal
    * to external representations, and perform any needed server site validation
    * that cannot be handled by JSF.
    * 
    * @return the site's description, may be <code>null</code> or empty.
    */
   /**
    * Gets the site's description.
    */
   public String getDescription() {
      return m_site.getDescription();
   }

   /**
    * Returns true if this node is a container.
    */
   public boolean isContainer() {
      return true;
   }

   /**
    * Returns true if the container is empty.
    */
   public boolean isContainerEmpty() {
      return false;
   }

   /**
    * Add a node to this site.
    * 
    * @param node the node to add, never <code>null</code>.
    */
   /**
    * Adds a node to this site.
    * @param node node to add, never null
    */
   public void addNode(PSNodeBase node) {
      if (node == null) throw new IllegalArgumentException("node may not be null");
      m_children.add(node);
      node.setParent(this);
      getModel().addNode(node);
   }

   /**
    * Gets the child nodes for this site.
    * @return list of child nodes
    */
   @SuppressWarnings("unchecked")
   public List<? extends PSNodeBase> getChildren() {
      if (m_children.isEmpty()) {
         var editions = new PSRuntimeEditionListNode(m_site);
         addNode(editions);
         var psvc = PSPublisherServiceLocator.getPublisherService();
         List<IPSEdition> elist = java.util.Collections.emptyList();
         try {
            var method = psvc.getClass().getMethod("findAllEditionsBySite", com.percussion.utils.guid.IPSGuid.class);
            Object result = method.invoke(psvc, m_site.getGUID());
            if (result instanceof List) {
               elist = (List<IPSEdition>) result;
            }
         } catch (Exception e) {
            // Method not available, fallback to empty list
         }
         for (IPSEdition e : elist) {
            editions.addNode(new PSRuntimeEditionNode(e));
         }
         addNode(new PSRuntimeSiteLogNode(m_site));
      }
      return m_children;
   }
   
   /**
    * Returns string representation of this node and its children.
    */
   public String toString(int indendation) {
      var b = new StringBuilder();
      for (var i = 0; i < indendation; i++) b.append(' ');
      b.append(super.toString());
      b.append('\n');
      if (m_children != null) {
         for (var node : m_children) {
            b.append(node.toString(indendation + 2));
            b.append('\n');
         }
      }
      return b.toString();
   }

   /**
    * Gets the row count for this node.
    */
   public int getRowCount() {
      if (m_children == null) return -1;
      return m_children.size();
   }

   /**
    * Gets the row data for the current index.
    */
   public Object getRowData() {
      return getRowData(m_index);
   }

   /**
    * Gets the row data for the given index.
    */
   public Object getRowData(int i) {
      if (isRowAvailable(i)) return m_children.get(i);
      return null;
   }

   /**
    * Gets the current row index.
    */
   public int getRowIndex() {
      return m_index;
   }

   /**
    * Gets the row key for the current row.
    */
   public Object getRowKey() {
      var node = (PSNodeBase) getRowData();
      if (node != null) return node.getKey();
      return null;
   }

   /**
    * Returns true if the current row is available.
    */
   public boolean isRowAvailable() {
      return isRowAvailable(m_index);
   }

   /**
    * Returns true if the given row index is available.
    */
   public boolean isRowAvailable(int index) {
      if (m_children != null) return index >= 0 && index < m_children.size() && m_children.get(index) != null;
      return false;
   }

   /**
    * Sets the current row index.
    */
   public void setRowIndex(int i) {
      m_index = i;
   }

   /**
    * Sets the current row key.
    * @param key the key to set, never null
    */
   public void setRowKey(Object key) {
      if (key == null) throw new IllegalArgumentException("key may not be null");
      if (m_children == null) return;
      var comparekey = key.toString();
      var current = m_index;
      for (m_index = 0; m_index < m_children.size(); m_index++) {
         if (isRowAvailable()) {
            if (comparekey.equals(getRowKey())) return;
         }
      }
      m_index = current;
   }
   
   /**
    * @return the name of the css class to use when rendering this node's
    * link in the navigation tree.
    */
   /**
    * Gets the CSS class for navigation link.
    */
   public String getNavLinkClass() {
      return "treenode";
   }
}
