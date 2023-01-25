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

import java.util.HashMap;
import java.util.List;

/**
 * The PSDtdElementEntry class is used as a placeholder for elements
 * referenced in a DTD tree, these will contain occurrence information
 * and a reference to the element and its content.
 *
 * @see   PSDtdNode
 * @see   PSDtdNodeList
 * @see   PSDtdElement
 * @see   PSDtdTree
 *
 * @author   David Gennaco
 * @version   1.0
 * @since   1.0
 */
public class PSDtdElementEntry extends PSDtdNode {
   private static final Logger log = LogManager.getLogger(PSDtdElementEntry.class);
   /**
    * Construct a Dtd Element Entry to reference an
    *   element declaration
    *
    * @param   PSDtdElement         reference to actual element
    *
    * @param   parent               parent node
    *
    * @param   occurrences   the occurrence setting
    *
    */
   PSDtdElementEntry(PSDtdElement element, PSDtdNode parent, int occurrences)
   {
      super(occurrences);
      m_element = element;
      m_parent = parent;
   }
/*
  public boolean isEqual(PSDtdElementEntry entry)
  {
     boolean bRet=false;

     PSDtdElement elem=entry.getElement();

     String srcName=elem.getName();
     String trgName=m_element.getName();
     if(trgName.equals(srcName))
     {
       bRet=true;
     }
     return(bRet);
  }
*/
   // print is used for manual debugging/checking of DTD structures
   public void print(String tab)
   {
      if (m_element != null)
         m_element.print(tab, "-" + m_occurrenceType + "-");
      else
         log.info(tab + " NULL Element");
   }

   /**
    * Add this entry to the catalog list, checking recursion and for
    *   content model
    *
    * This function should be overridden for all extended classes.
    *
    * @param   stack   the recursion detection stack
    *
    * @param   catalogList the catalog list being built
    *
    * @param   cur            the current name to expand on
    *
    * @param   sep            the element separator string
    *
    * @param   attribId  the string used to identify an attribute entry
    *
    */
   public void catalog(HashMap stack, List catalogList, String cur,
      String sep, String attribId)
   {
      if (catalogList.size() >= PSDtdTree.MAX_CATALOG_SIZE)
      {
         catalogList.add("TRUNCATED!");
         return;
      }

      if (m_element == null)
      {
         cur = cur + sep + "<NULL>";
         catalogList.add(cur);
         return;
      }

      if (stack.containsKey(m_element.getName()))
      {
      /* # signifies recursive state which starts with this element
       - and this is the second occurrence */
         cur = cur + sep + m_element.getName() + "<#RECURSION>";
         catalogList.add(cur);
      }
      else
      {
         if (cur.equals(""))
            cur = m_element.getName();
         else
            cur = cur + sep + m_element.getName();

//         catalogList.add(cur);

         // Process member element's attributes
         m_element.catalogAttributes(catalogList, cur, sep, attribId);

         // Process member element's content
         if (!(m_element.isAny() || m_element.isEmpty()))
         {
            stack.put(m_element.getName(), null);
            m_element.getContent().catalog(stack, catalogList, cur, sep, attribId);
            stack.remove(m_element.getName());
         }
         else
            catalogList.add(cur);
      }
   }

   /**
    *      Return the maximum occurrence setting for this node, based
    *         on the context of this node under its parent element.
    *
    *   @return      occurrence type (max)
    */
   public int getMaxMergedOccurrenceSetting()
   {
      return getMaxMergedOccurrenceSetting(m_occurrenceType);
   }

   /**
    *   Return the element associated with this entry
    *
    * @return   the element for this element entry node
    */
   public PSDtdElement getElement()
   {
      return m_element;
   }

  /**
   *
   */
  void setElement( PSDtdElement element )
  {
    m_element = element;
  }

   public Object acceptVisitor(PSDtdTreeVisitor visitor, Object data)
   {
      return visitor.visit(this, data);
   }

   public Object childrenAccept(PSDtdTreeVisitor visitor, Object data)
   {
      PSDtdNode content = m_element.getContent();
      if (content != null)
      {
         return content.acceptVisitor(visitor, data);
      }
      return null;
   }

   PSDtdElement m_element;
}

