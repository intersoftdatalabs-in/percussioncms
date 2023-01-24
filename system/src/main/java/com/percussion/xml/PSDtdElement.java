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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * The PSDtdElement class contains element declaration information
 * references by Element nodes in our internal DTD tree.  This class
 * contains attribute and content model information.
 */
public class PSDtdElement implements Serializable {
    private static final Logger log = LogManager.getLogger(PSDtdElement.class);
   /**
    * Construct a Dtd Element which corresponds to a Element decl in the Dtd
    *
    * @param   name      the name of this element
    *
    */
   PSDtdElement(String name)
   {
      m_name       = name;
      m_content    = null;
      m_isAny      = false;
      m_hasCharData = false;
   }

   /**
    * Set the content for this Dtd Element
    *
    * @param   content   the content of this element, null if EMPTY or ANY
    *
    * @param   isAny    if null content, setting this true will mark this
    *                                                                  element as ANY, this is ignored if content is
    *                                                                  provided (not null)
    */
   public void setContent(PSDtdNode content, boolean isAny)
   {
      m_content = content;

      if (m_content == null)
         m_isAny       = isAny;
      else
         m_hasCharData = checkForCharData(m_content);
   }

   /**
    * Set the attributes for this Dtd Element
    *
    * @param   dtd   the parsed DTD in which the attributes will be looked up
    *
    */
   public void setAttributes(PSDtd dtd)
   {
      m_attributes = new ArrayList();
      java.util.Enumeration e = dtd.getAttributeDeclarations(m_name);
      while (e.hasMoreElements())
      {
         m_attributes.add(new PSDtdAttribute((PSXmlAttributeDecl) e.nextElement()));
      }
   }

  /**
   * A simple clear method.
   */
  public void resetAttributes()
  {
    m_attributes = new ArrayList();
  }


   public void addAttribute(PSDtdAttribute attr)
   {
    if ( null == m_attributes )
      resetAttributes();

    m_attributes.add(attr);
   }

   /**
    * Get the number of attributes contained in this element definition
    *
    * @return   the number of attributes for this element
    *
    */
   public int getNumAttributes()
   {
      if (m_attributes == null)
         return 0;
      else
         return m_attributes.size();
   }

   /**
    * Gets the attribute
    *
    * @param   index   attribute index
    *
    * @return   the attributes at the specified index
    *
    */
   public PSDtdAttribute getAttribute(int index)
   {
      return (PSDtdAttribute) m_attributes.get(index);
   }

   /**
    * Gets the attribute with the given name, or null if no
    * such attribute is found.
    *
    */
   public PSDtdAttribute getAttribute(String name)
   {
      for (int i = 0; i < m_attributes.size(); i++)
      {
         PSDtdAttribute attr = getAttribute(i);
         if (attr.getName().equals(name))
            return attr;
      }
      return null;
   }

   /**
    * Return the content model for this element
    *
    * @return   the content model
    *
    */
   public PSDtdNode getContent()
   {
      return m_content;
   }

   /**
    * Return the name this element
    *
    * @return   the name
    *
    */
   public String getName()
   {
      return m_name;
   }

  /**
   * Sets the name of this element
   *
   * @param the name
   */
  void setName( String name )
  {
    m_name = name;
  }

   /**
    * Is this element declared as EMPTY?
    *
    * @return   <code>true</code> it is empty
    *                                                      <code>false</code> it is not empty
    *
    */
   public boolean isEmpty()
   {
      return ((m_content == null) && !m_isAny);
   }

   /**
    * Is this element declared as ANY?
    *
    * @return   <code>true</code> it is any
    *                                                      <code>false</code> it is not any
    *
    */
   public boolean isAny()
   {
      return ((m_content == null) && m_isAny);
   }

   /**
    * print is used for manually watching debugging DTDs
    *
    */
   public void print(String tab, String occurrenceString)
   {
      log.info(tab + m_name + " element " + occurrenceString);
      if (isAny())
      {
         log.info(tab + "ANY ");
      }
      else if (isEmpty())
      {
         log.info(tab + "EMPTY");
      }
      else
      {
         m_content.print(tab + "   ");
      }
   }

   /**
    * Add the attributes from this element to the catalog list
    * @param   catalogList   the catalog list being built
    * @param   cur         the current name to expand on
    * @param   sep         the element separator string
    * @param   attribId      the string used to identify an attribute entry
    */
   public void catalogAttributes(List catalogList, String cur,
                        String sep, String attribId)
   {
      if (m_attributes == null)
         return;
      else
         for (int i = 0; i < m_attributes.size(); i++)
         {
            if (catalogList.size() > PSDtdTree.MAX_CATALOG_SIZE)
               break;

            ((PSDtdAttribute)m_attributes.get(i)).catalog(catalogList, cur, sep, attribId);
         }
   }

   /**
    * @author   chadloder
    *
    * @version 1.4 1999/06/02
    *
    * Returns true if this node could ever possibly have character data.
    *
    * @return   boolean
    */
   public boolean hasCharacterData()
   {
      return m_hasCharData;
   }

   /**
    * @author   chadloder
    *
    * @version 1.4 1999/06/02
    *
    * Private utility method to see if the given node could ever possibly
    * be or directly contain PCDATA.
    *
    * @param   node   The node
    *
    * @return   boolean   true if this node could ever be or directly contain
    * PCDATA.
    */
   private boolean checkForCharData(PSDtdNode node)
   {
      if (node instanceof PSDtdDataElement)
         return true;
      else if (node instanceof PSDtdNodeList)
      {
         PSDtdNodeList list = (PSDtdNodeList)node;
         for (int i = 0; i < list.getNumberOfNodes(); i++)
         {
            PSDtdNode n = list.getNode(i);
            if (n instanceof PSDtdDataElement)
            {
               return true;
            }
            else if (n instanceof PSDtdNodeList)
            {
               if (checkForCharData(n) == true)
                  return true;
               else
                  continue;
            }
         }
      }
      return false;
   }

   private PSDtdNode    m_content;
   private String       m_name;
   private boolean       m_isAny;
   private List    m_attributes;
   private boolean m_hasCharData;
}



