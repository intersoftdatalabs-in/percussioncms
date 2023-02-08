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

import com.percussion.cx.error.IPSContentExplorerErrors;
import com.percussion.cx.error.PSContentExplorerException;
import com.percussion.util.PSXMLDomUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * This class represents the UserOptions element in the sys_Options.dtd.
 */
public class PSUserOptions implements IPSClientObjects
{
   /**
    * Creates an instance from the XML representation specified in the
    * dtd mentioned in the  class description.
    *
    * @param optionsElement - must not be <code>null</code>, must be defined
    * as specified in the dtd list in the class description.
    * @throws PSOptionException - if optionsElement has an invalid
    * definition.
    */
   public PSUserOptions(Element optionsElement)
      throws PSContentExplorerException
   {
      fromXml(optionsElement);
   }

   /** @see IPSClientObject  **/
   public void fromXml(Element sourceNode) throws PSContentExplorerException
   {
      try
      {
         // validate the root element
         PSXMLDomUtil.checkNode(sourceNode, ELEM_USEROPTIONS);

         // get list of options and create:
         Element option = PSXMLDomUtil.getFirstElementChild(sourceNode);
         while (option != null)
         {
            addOptions(new PSOptions(option));
            option = PSXMLDomUtil.getNextElementSibling(option);
         }
      }
      catch (Exception e)
      {
         throw new PSContentExplorerException(
            IPSContentExplorerErrors.MISC_PROCESSING_OPTIONS_ERROR,
            e.getLocalizedMessage());
      }
   }

   /** @see IPSClientObjects */
   public Element toXml(Document doc)
   {
      if (doc == null)
         throw new IllegalArgumentException("doc must not be null");

      // create root and its attributes
      Element root = doc.createElement(ELEM_USEROPTIONS);

      PSOptionManagerConstants.toXmlCollection(root, doc, m_optionsMap.values());

      return root;
   }

   /**
    * Returns a <code>PSOptions</code> object specified by the case-sensitive
    * category.
    *
    * @param category must not be <code>null</code> or empty.
    * @return the <code>PSOptions</code> specified, any modifications to the
    * <code>PSOptions</code> will affect this class, may be <code>null</code>
    * if not found.
    */
   public PSOptions getOptions(String category)
   {
      if (category == null || category.trim().length() == 0)
         throw new IllegalArgumentException("category must not be null or empty");

      return (PSOptions)m_optionsMap.get(category);
   }

   /**
    * Returns an iterator of category names of the <code>PSOptions</code> this
    * objects contains.
    *
    * @return an unmodifiable <code>Iterator</code> all of the
    * <code>PSOptions</code> categories as <code>Strings</code>.  May be empty
    * but not <code>null</code>.
    *
    */
   public Iterator getOptionCategories()
   {
      return Collections.unmodifiableSet(m_optionsMap.keySet()).iterator();
   }

   /**
    * Indicates whether some other object is "equal to" this one.
    * Overrides the method in {@link Object.equals(Object) Object} and adheres
    * to that contract.
    * @param obj the reference object with which to compare.
    * @return <code>true</code> if this object is the same as the
    * <code>obj</code> argument; <code>false</code> otherwise. If
    * <code>null</code> supplied or obj is not an instance of this class,
    * <code>false</code> is returned.
    */
   public boolean equals(Object obj)
   {
      if (obj == null || !(getClass().isInstance(obj)))
         return false;

      PSUserOptions comp = (PSUserOptions)obj;

      if (!PSOptionManagerConstants.compare(m_optionsMap, comp.m_optionsMap))
         return false;

      return true;
   }

   /**
    * Overridden to fulfill contract of this method as described in
    * {@link Object#hashCode() Object}.
    *
    * @return A hash code value for this object
    */
   public int hashCode()
   {
      int hash = 0;

      hash += m_optionsMap.hashCode();

      return hash;
   }

   /**
    * The key will be the category of the PSOptions and stored as is,
    * therefore it is case sensitive and the PSOption itself will be the
    * value.
    *
    * @param option must not be <code>null</code>
    */
   public void addOptions(PSOptions option)
   {
      if (option == null)
         throw new IllegalArgumentException("option must not be null");

      m_optionsMap.put(option.getCategory(), option);
   }

   /**
    * A Map of PSOptions, never <code>null</code>, may be empty, keys
    * are stored as is --  case sensitive,
    * @see #addOptions(String, PSOptions)
    */
   Map m_optionsMap = new HashMap();

   /**
   * Root element of the XML document
   */
   private static final String ELEM_USEROPTIONS = "PSXUserOptions";
}
