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

package com.percussion.xml;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The PSDtdNodeList class is used to represent sequences and
 * option lists in our internal DTD tree.
 *
 * This list will represent either a sequence of nodes or a list
 * of optional nodes.
 *
 * @see        PSDtdNode
 * @see        PSDtdElement
 * @see        PSDtdTree
 *
 * @author     David Gennaco
 * @version    1.0
 * @since      1.0
 */
public class PSDtdNodeList extends PSDtdNode {

   private static final Logger log = LogManager.getLogger(PSDtdNodeList.class);
   /**
    *                        Base constructor, initialize array list
    *                           default - type set to Sequence
    *
    */
   PSDtdNodeList(int type) {
      super();
      m_nodes = new ArrayList();
      /* Translate invalid types to sequence list */
      if (type != OPTIONLIST)
         type = SEQUENCELIST;

      m_type = type;
   }

   /**
    *                        Construct with occurrences
    *
    *                        @param  type         SEQUENCELIST (,) or OPTIONLIST (|)
    *
    *                        @param  occurrences   the occurrence setting
    *
    */
   PSDtdNodeList(int type, int occurrences)
   {
      super(occurrences);

      /* Translate invalid types to sequence list */
      if (type != OPTIONLIST)
         type = SEQUENCELIST;

      m_type  = type;
      m_nodes = new ArrayList();
   }

   /**
    *   Adds a <code>PSDtdElementEntry</code> object to this list. If node is not
    *   an instance of <code>PSDtdElementEntry</code> then does not add it to
    *   the list.
    *   @param      node      The node to add (append).
    */
   public void add(PSDtdNode node)
   {
      if (node instanceof PSDtdElementEntry)
         m_nodes.add(node);
   }

   /**
    *   Get the number of nodes in this list
    *
    *   @return            Number of nodes
    */
   public int getNumberOfNodes()
   {
      return m_nodes.size();
   }

   /**
    *   Get the node at the specified index
    *
    *   @return            the node at the specified index or
    *                                             <code>null</code> if index is out of range
    */
   public PSDtdNode getNode(int index)
   {
      if ((index < 0) || (index >= m_nodes.size())) {
         return null;
      } else {
         return (PSDtdNode) m_nodes.get(index);
      }
   }


   /**
    *   Return the type associated with this list
    *                        <p>
    *                possible values:
    *       <code>SEQUENCELIST</code>
    *       <code>OPTIONLIST</code>
    */
   public int getType()
   {
      return m_type;
   }

   /**
    *  print is used for debugging/checking DTD structure manually
    */
   public void print(String tab)
   {
      log.info(tab + m_type + " " + m_occurrenceType);

      for (int i = 0; i < m_nodes.size(); i++)
      {
         ((PSDtdNode) m_nodes.get(i)).print(tab + "   ");
      }
   }

   /**
    * Add the items in this list to the catalog list.
    * This function should be overridden for all extended classes.
    *   @param   stack         the recursion detection stack
    *   @param   catalogList   the catalog list being built
    * @param   cur         the current name to expand on
    * @param   sep         the element separator string
    * @param   attribId      the string used to identify an attribute entry
    */
   public void catalog(HashMap stack, List catalogList, String cur,
                     String sep, String attribId)
   {
      if (m_nodes != null)
      {
         for (int i = 0; i < m_nodes.size(); i++)
         {
            if (catalogList.size() > PSDtdTree.MAX_CATALOG_SIZE)
               break;

            ((PSDtdNode) m_nodes.get(i)).catalog(stack, catalogList, cur, sep, attribId);
         }
      }

      return;
   }

   public Object acceptVisitor(PSDtdTreeVisitor visitor, Object data)
   {
      return visitor.visit(this, data);
   }

   public Object childrenAccept(PSDtdTreeVisitor visitor, Object data)
   {
      for (int i = 0; i < m_nodes.size(); i++)
      {
         PSDtdNode n = (PSDtdNode)m_nodes.get(i);
         n.acceptVisitor(visitor, data);
      }
      return null;
   }

   List m_nodes;  /* use ArrayList: fastest (non-synchronized) way to
                     maintain insert/append elements */

   int m_type;

   static public final int SEQUENCELIST = ',';
   static public final int OPTIONLIST = '|';
}
