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
package com.percussion.fastforward.managednav;

import com.percussion.cms.objectstore.PSComponentSummary;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.server.IPSRequestContext;
import com.percussion.services.assembly.impl.nav.PSNavConfig;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;

import java.util.ArrayList;
import java.util.List;

/**
 * Implements a stack of PSComponentSummaries represeting Navon objects. This
 * stack differs from the standard Java stack in 2 ways:
 * <p>
 * It tracks the <code>imageSelector</code> and <code>variableSelector</code>
 * attributes. These attributss take on the values that are specified at the <b>
 * lowest </b> level where they are specfied in the tree.
 * <p>
 * It returns <code>null</code> when an attempt is made to access an empty
 * Stack
 * </p>
 * 
 * @author DavidBenua
 * 
 *  
 */
public class PSNavonStack
{
   /**
    * Constructs an empty stack
    */
   public PSNavonStack()
   {
      m_relLevel = 0;
   }

   /**
    * Consttructs a stack starting at a locator. Convenience method for
    * PSNavonStack(IPSRequestContext, PSComponentSummary)
    * 
    * @param req the parent request context
    * @param loc the locator for the self node.
    * @throws PSNavException
    */
   public PSNavonStack(IPSRequestContext req, PSLocator loc)
         throws PSNavException
   {
      this(req, PSNavUtil.getItemSummary(req, loc));
   }

   /**
    * Construct a Stack starting at a given Navon.
    * 
    * @param req the parent request context
    * @param navon the navon to start as the self node.
    * @throws PSNavException
    */
   public PSNavonStack(IPSRequestContext req, PSComponentSummary navon)
         throws PSNavException
   {
      this();

      this.push(req, navon);
      PSNavConfig config = PSNavConfig.getInstance(req);
      if (config.getNavTreeTypes().contains(navon.getContentTypeGUID()))
      {
         return;
      }
      while (navon != null)
      {
         log.debug("walking up chain {}", navon.getName());
         PSComponentSummary next = PSNavFolderUtils.findParentSummary(req,
               navon.getCurrentLocator());

         if (next == null)
         { // unexpected Navon with no parent.
            log.debug("found navon with no parent");
            break;
         }
         this.push(req, next);
         if (config.getNavTreeTypes().contains(next.getContentTypeGUID()))
         { // we found a NavTree content item
            log.debug("NavTree found");
            break;
         }
         else
         {
            navon = next;
         }
      }
   }

   /**
    * determines if the stack is empty.
    * 
    * @return <code>true</code> when the stack is empty.
    */
   public boolean isEmpty()
   {
      return m_navStack.isEmpty();
   }

   /**
    * pushes a new navon onto the stack.
    * @param req
    * 
    * @param navon the navon to push onto the stack
    * @throws PSNavException
    */
   public void push(IPSRequestContext req, PSComponentSummary navon)
         throws PSNavException
   {
      log.debug("pushing Navon {} on to Stack ", navon.getName());
      if (m_navStack.isEmpty())
      {
         m_relLevel = 0;
      }
      else
      {
         m_relLevel -= 1;
      }
      m_navStack.add(0, navon);

      
      String oldFolderId = null; 
      Document doc = null;

      // The "sys_folderid" HTML parameter in 'req' may not relate to the
      // 'navon'. Removing it from the 'req', so that it will not be part of
      // the cache's key. This made the cache more reusable/accessable for the
      // PSNavUtil.getNavonDocument(IPSRequest, PSComponentSummary) 
      try
      {
         oldFolderId = req.getParameter(IPSHtmlParameters.SYS_FOLDERID);
         if (oldFolderId != null)
            req.removeParameter(IPSHtmlParameters.SYS_FOLDERID);
         
         doc = PSNavUtil.getNavonDocument(req, navon);
      }
      finally 
      {
         if (oldFolderId != null)
            req.setParameter(IPSHtmlParameters.SYS_FOLDERID, oldFolderId);
      }

      String navDoc = PSXmlDocumentBuilder.toString(doc);
      log.debug("Navon document is {}", navDoc);

      PSNavConfig config = PSNavConfig.getInstance(req);
      String navImageSelect = PSNavUtil.getFieldValueFromXML(doc, config
            .getNavonSelectorField());
      log.debug("navImageSelect is {}", navImageSelect);
      if (m_imageSelector == null && navImageSelect != null
            && navImageSelect.trim().length() > 0)
      {
         log.debug("setting image selector {}", navImageSelect);
         m_imageSelector = navImageSelect.trim();
      }

      String varSelect = PSNavUtil.getFieldValueFromXML(doc, config
            .getNavonVariableName());
      log.debug("navVarSelect is {}", varSelect);
      if (m_varSelector == null && varSelect != null
            && varSelect.trim().length() > 0)
      {
         log.debug("setting variable selector {}", varSelect);
         this.m_varSelector = varSelect;
      }
   }

   /**
    * gets the top Navon on the stack.
    * 
    * @return the top Navon on the stack. <code>Null</code> if the stack is
    *         <code>empty</code>.
    */
   public PSComponentSummary peek()
   {
      if (m_navStack.isEmpty())
      {
         return null;
      }
      return (PSComponentSummary) (m_navStack.get(0));
   }

   /**
    * gets an arbitrary Navon in the stack.
    * 
    * @param i the position in ths stack. The top of the stack is 0.
    * @return the Navon at the specified position. Will be <code>null</code>
    *         if the index is out of range.
    */
   public PSComponentSummary peek(int i)
   {
      if (m_navStack.isEmpty() || m_navStack.size() <= i)
      {
         return null;
      }
      return (PSComponentSummary) m_navStack.get(i);
   }

   /**
    * Gets the image selector for the stack.
    * 
    * @return
    */
   public String getImageSelector()
   {
      log.debug("getting image selector {}", m_imageSelector);
      return m_imageSelector;
   }

   /**
    * gets the relative level of the top of stack.
    * 
    * @return the relative level of the top of stack.
    */
   public int getRelLevel()
   {
      return m_relLevel;
   }

   /**
    * Gets the variable selector for the stack.
    * 
    * @return the variable selector or <code>null</code> if not specified.
    */
   public String getVarSelector()
   {
      log.debug("getting variable selector {}", m_varSelector);
      return m_varSelector;
   }

   /**
    * gets the id of the Navon in the stack at the specified position.
    * 
    * @param i the index of the desired Navon. Specify 0 for the top of stack.
    * @return the id of the selected Navon. Will be <code>null</code> if the
    *         stack is empty.
    */
   public String getId(int i)
   {
      if (this.isEmpty())
      {
         return null;
      }
      return this.peek(i).getLocator().getPart(PSLocator.KEY_ID);
   }

   /**
    * List of Navons
    */
   private List m_navStack = new ArrayList(); // not really a stack

   /**
    * Image selector value for this stack
    */
   private String m_imageSelector = null;

   /**
    * Variable selector value for this stack
    */
   private String m_varSelector = null;

   /**
    * Relative Level of of the current top of stack.
    */
   private int m_relLevel;

   /**
    * Logger for this class.
    */
   private static final Logger log = LogManager.getLogger(PSNavonStack.class);
}
