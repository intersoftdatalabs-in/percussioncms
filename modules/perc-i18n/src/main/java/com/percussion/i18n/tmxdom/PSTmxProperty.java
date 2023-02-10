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
package com.percussion.i18n.tmxdom;

import com.percussion.i18n.PSI18nUtils;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

/**
 * This class wraps the functionality of property DOM element of the TMX document
 * as an easy to use TMX node. The TMX counterpart provides methods to manipulate
 * the property, the most important one being to merge two nodes applying the
 * merge configuration.
 */
public class PSTmxProperty
   extends PSTmxLeafNode
   implements IPSTmxProperty
{
   /**
    * Constructor. Takes the parent TMX document object and the DOM element
    * representing the property. The property type and language this property
    * associated with are constructed from the supplied DOM element.
    * @param tmxdoc parent TMX document, nust not be <code>null</code>.
    * @param prop DOM element for the TMX property to be constructed, must not
    * be <code>null</code>.
    * @throws IllegalArgumentException if tmxDoc or prop is <code>null</code>)
    */
   PSTmxProperty(IPSTmxDocument tmxdoc, Element prop)
   {
      if(tmxdoc == null)
         throw new IllegalArgumentException("tmxdoc must not be null");
      if(prop == null)
         throw new IllegalArgumentException("prop must not be null");

      m_DOMElement = prop;
      m_PSTmxDocument = tmxdoc;
      m_Lang = m_DOMElement.getAttribute(IPSTmxDtdConstants.ATTR_XML_LANG);
      m_Type = m_DOMElement.getAttribute(IPSTmxDtdConstants.ATTR_TYPE);
      Node node = m_DOMElement.getFirstChild();
      if(node instanceof Text)
         m_Value = ((Text)node).getData();
   }

   /**
    * Returns langauge attribute of this node
    * @return language this node associated with,  Never <code>null</code> or
    * <code>empty</code>.
    */
   public String getLang()
   {
      return m_Lang;
   }

   /**
    * Sets langauge attribute for this node
    * @param    lang Must not be <code>null</code> may be <code>empty</code>.
    * @throws IllegalArgumentException if lang is <code>null</code>
    */
   public void setLang(String lang)
   {
      if(lang == null || lang.trim().length() < 1)
         throw new IllegalArgumentException("lang must not be null");

      m_DOMElement.setAttribute(IPSTmxDtdConstants.ATTR_XML_LANG, lang);
      m_Lang = lang;
   }

   /*
    * Implementation of the method defined in the interface
    */
   public String getType(){
      return m_Type;
   }

   /*
    * Implementation of the method defined in the interface
    */
   public void setType(String type){
      m_DOMElement.setAttribute(IPSTmxDtdConstants.ATTR_TYPE, type);
      m_Type = type;
   }

   /*
    * Implementation of the method defined in the interface
    */
   public void merge(IPSTmxNode node)
      throws PSTmxDomException
   {
      if(node == null)
      {
         throw new IllegalArgumentException("node must not be null for merging");
      }
      else if(!(node instanceof IPSTmxProperty))
      {
         throw new PSTmxDomException("onlyOneTypeAllowedToMerge",
            "IPSTmxProperty");
      }
      IPSTmxProperty propSrc = (IPSTmxProperty)node;
      String configType = IPSTmxMergeConfig.MERGE_NODEID_TU_PROPERTY;
      if(getParent() instanceof IPSTmxTranslationUnitVariant)
         configType = IPSTmxMergeConfig.MERGE_NODEID_TUV_PROPERTY;

      PSTmxConfigParams options = getTMXDocument().getMergeConfig()
            .getConfigParams(configType);
      boolean exists = getType().equals(propSrc.getType());
      if(exists)
      {
         if(options.getParam(IPSTmxMergeConfig.MERGE_OPTION_MODIFYIFEXISTS)
            .equalsIgnoreCase(IPSTmxMergeConfig.YES))
         {
            setValue(propSrc.getValue());
         }
      }
   }

   /**
    * The type this node is associated with. Value is <code>empty</code>
    * initially and initialized during construction.
    */
   protected String m_Type = "";

   /**
    * The default language this node is associated with. A translation variant
    * can be associated with a language.
    */
   protected String m_Lang = PSI18nUtils.DEFAULT_LANG;

}
