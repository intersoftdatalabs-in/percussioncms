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
package com.percussion.cx;

import java.util.Iterator;
import java.util.Objects;

/**
 * The class that describes the action to take upon execution of an action.
 */
public final class PSActionEvent
{
   /**
    * Constructs the event with the hint that needs to be refreshed.
    *
    * @param refreshHint the hint, may not be {@code null} or empty.
    * @throws IllegalArgumentException if refreshHint is {@code null} or empty
    */
   public PSActionEvent(String refreshHint)
   {
      Objects.requireNonNull(refreshHint, "refreshHint cannot be null");

      var trimmedHint = refreshHint.trim();
      if (trimmedHint.isEmpty()) {
         throw new IllegalArgumentException("refreshHint cannot be empty");
      }

      m_refreshHint = trimmedHint;
   }

   /**
    * Gets the refresh hint that describes the action to take by the receiver
    * of this event object.
    *
    * @return the hint, never {@code null} or empty, may be one of the
    * REFRESH_xxx values.
    */
   public String getRefreshHint()
   {
      return m_refreshHint;
   }
   
   /**
    * Sets the nodes to refresh, these should be existing nodes in the tree.
    * Should be called if the refresh hint is {@code REFRESH_NODES}. The
    * first one in the list is the default selection for the listener.
    * 
    * @param nodes the nodes to refresh, may not be {@code null} or empty.
    *
    * @throws IllegalArgumentException if nodes is not valid.
    */
   public void setRefreshNodes(Iterator<?> nodes)
   {
      Objects.requireNonNull(nodes, "nodes cannot be null");

      if (!nodes.hasNext()) {
         throw new IllegalArgumentException("nodes cannot be empty");
      }

      m_nodes = nodes;      
   }   
   
   /**
    * Gets the list of nodes to refresh in UI, should be called by the listener.
    * The first one in the list should be selected in the tree.
    * 
    * @return the nodes, may be {@code null} if the refresh hint is not
    * {@code REFRESH_NODES}.
    */
   public Iterator<?> getRefreshNodes()
   {
      return m_nodes;
   }

   /**
    * Set if a full vs partial refresh is required.  
    * 
    * @param isFull {@code true} if a full refresh is to be performed,
    * {@code false} if a refresh of only dirty nodes is to be performed.
    */
   public void setIsFullRefresh(boolean isFull)
   {
      m_fullRefresh = isFull;
   }
   
   /**
    * Determine if a full vs partial refresh is required.  See 
    * {@link #setIsFullRefresh(boolean)} for more info.
    * 
    * @return {@code true} if a full refresh is to be performed,
    * {@code false} if not.
    */
   public boolean isFullRefresh()
   {
      return m_fullRefresh;
   }

   /**
    * The hint that describes the action that needs to be performed by the
    * receiver, initialized in the ctor and never {@code null}, empty or
    * modified after that.
    */
   private final String m_refreshHint;

   /**
    * The list of nodes to refresh in UI, {@code null} until call to {@code
    * setRefreshNodes(Iterator)}.
    */
   private Iterator<?> m_nodes;

   /**
    * Determines if a full vs partial refresh is required. Is {@code true}
    * if a full refresh is to be performed, {@code false} if a refresh of
    * only dirty nodes is to be performed.
    */
   private boolean m_fullRefresh = false;

   /**
    * The constant that describes the root of the navigational tree need to be
    * refreshed.
    */
   public static final String REFRESH_NAV_ROOT = "Root";

   /**
    * The constant that describes the selected node of the navigational tree
    * need to be refreshed.
    */
   public static final String REFRESH_NAV_SELECTED = "Selected";

   /**
    * The constant that describes the parent of the selected node of the
    * navigational tree need to be refreshed.
    */
   public static final String REFRESH_NAV_SEL_PARENT = "Parent";

   /**
    * The constant that describes list of the nodes of the navigational tree
    * need to be refreshed and first one in the list need to be selected.
    */
   public static final String REFRESH_NODES = "Nodes";

   /**
    * The constant that describes the display options of the applet need to be
    * refreshed.
    */
   public static final String REFRESH_OPTIONS = "Options";
   
   /**
    * The constant that describes that the any nodes matching the supplied nodes 
    * should be marked as dirty throughout the tree.
    */
   public static final String DIRTY_NODES = "DirtyNodes";
}
