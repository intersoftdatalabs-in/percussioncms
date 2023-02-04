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
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.error.PSExceptionUtils;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.relationship.IPSEffect;
import com.percussion.relationship.IPSExecutionContext;
import com.percussion.relationship.PSEffectResult;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.cache.PSCacheProxy;
import com.percussion.services.assembly.impl.nav.PSNavConfig;
import com.percussion.services.contentmgr.IPSContentMgr;
import com.percussion.services.contentmgr.PSContentMgrConfig;
import com.percussion.services.contentmgr.PSContentMgrLocator;
import com.percussion.services.contentmgr.PSContentMgrOption;
import com.percussion.services.guidmgr.PSGuidUtils;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.utils.guid.IPSGuid;

import javax.jcr.Node;
import javax.jcr.Property;
import javax.jcr.RepositoryException;
import java.text.MessageFormat;
import java.util.List;

import static com.percussion.fastforward.managednav.PSNavFolderUtils.addNavonToChildFolder;

/**
 * A relationship effect for managing folders and navons. This effect is
 * designed for the <code>Folder Content</code> relationhip.
 * <p>
 * There are several different events that this effect must handle
 * <ul>
 * <li>A new folder is added to a folder</li>
 * <li>A new navon is added a folder</li>
 * <li>A Navon is removed from a folder</li>
 * <li>A folder is removed</li>
 * </ul>
 * <p>
 * New navons are added only when the folder above them already has a navon in
 * it. When a navon exists in a child folder, these navons are connected to the
 * current navon by an Active Assembly relationship.
 * <p>
 * When folders or navons are removed, these relationships are removed as well.
 * <p>
 * All navons added by this effect inherit the community of the navon above them
 * in the hierarchy, not the community of the calling user
 * <p>
 * This effect is designed to run as the <code>rxserver</code> user.
 * <p>
 * 
 * @author DavidBenua
 *  
 */
public class PSNavFolderEffect extends PSNavAbstractEffect implements IPSEffect
{

   /**
    * Tests whether the effect should allow the operation to continue.
    * 
    * @param params the effect parameters specified in the workbench. Not 
    *    used in this effect.
    * @param req the callers request context, not <code>null</code>.
    * @param excontext the execution context determines which event caused this
    *    effect to run, not <code>null</code>.
    * @param result the result object tells the effect processor whether the 
    *    event is allowed to continue or not, not <code>null</code>.
    * @throws PSExtensionProcessingException
    * @throws PSParameterMismatchException
    *  
    */
   @Override
   public void test(Object[] params, IPSRequestContext req,
      IPSExecutionContext excontext, PSEffectResult result)
      throws PSExtensionProcessingException, PSParameterMismatchException
   {
      if (req == null)
         throw new IllegalArgumentException("req cannot be null");
      if (excontext == null)
         throw new IllegalArgumentException("excontext cannot be null");
      if (result == null)
         throw new IllegalArgumentException("result cannot be null");
      
      //context must be construction
      if(!excontext.isPreConstruction() && !excontext.isPreDestruction())
      {
         result.setWarning(
            "This effect is active only during relationship construction or destruction.");
         return;
      }
      
      try
      {
         String userName = 
            req.getUserContextInformation("User/Name", "").toString();
         m_log.debug("User name {}" , userName);

         if (isExclusive(req))
         {
            m_log.debug("TEST - exclusion flag detected");
            result.setSuccess();
            return;
         }
         
         PSRelationship currRel = excontext.getCurrentRelationship();

         PSNavRelationshipInfo currentInfo;
         try
         {
            currentInfo = new PSNavRelationshipInfo(currRel, req);
         }
         catch (Exception ex)
         {
            m_log.warn("Unable to load relationship info rid is {}. Error: {}"
                  , currRel.getId(), PSExceptionUtils.getMessageForLog(ex));
            result.setSuccess();
            return;
         }

         String operation = String.valueOf(excontext.getContextType());
         m_log.debug("Test Current {}\n Operation {}" ,currentInfo, operation);

         if (excontext.isPreConstruction())
            handleTestNew(req, currentInfo, result);
         else
            result.setSuccess();
      }
      catch (Exception ex)
      {
         m_log.error(PSExceptionUtils.getMessageForLog(ex));
         result.setError(new PSExtensionProcessingException(this.getClass()
               .getName(), ex));
      }

   }

   /**
    * Processes the actual operation.
    * 
    * @param params the array of Effect parameters specified in the workbench.
    *           Not used in this effect.
    * @param req the callers request context.
    * @param excontext the execution context specifies which event is being
    *           processed.
    * @param result the result block determines whether the event has been
    *           handled successfully.
    * 
    * @see com.percussion.relationship.IPSEffect#attempt(java.lang.Object[],
    *      com.percussion.server.IPSRequestContext,
    *      com.percussion.relationship.IPSExecutionContext,
    *      com.percussion.relationship.PSEffectResult)
    */
   @Override
   public void attempt(Object[] params, IPSRequestContext req,
         IPSExecutionContext excontext, PSEffectResult result)
         throws PSExtensionProcessingException, PSParameterMismatchException
   {
      try
      {
         if (isExclusive(req))
         {
            m_log.debug("ATTEMPT = exclusion flag detected");
            result.setSuccess();
            return;
         }

         PSRelationship currRel = excontext.getCurrentRelationship();

         PSNavRelationshipInfo currentInfo;
         try
         {
            currentInfo = new PSNavRelationshipInfo(currRel, req);
         }
         catch (Exception ex)
         {
            m_log.warn("Unable to load relationship info rid is {}. Error: {}"
                  , currRel.getId(), PSExceptionUtils.getMessageForLog(ex));
            result.setSuccess();
            return;
         }

         String operation = String.valueOf(excontext.getContextType());
         m_log.debug("Attempt Current {}\n Operation {}",currentInfo,
                operation);
         result.setSuccess();
         if (excontext.isPreConstruction())
         {
            handleAttemptNew(req, currentInfo, result);
         }
         else if (excontext.isPreDestruction())
         {
            handleAttemptDestroy(req, currentInfo);
         }
      }
      catch (Exception ex)
      {
         m_log.error(this.getClass().getName(), ex);

         result.setError(new PSExtensionProcessingException(this.getClass()
               .getName(), ex));
      }

   }

   /**
    * Handles the <code>test</code> event when a new item has been created.
    * 
    * @param req the parent request context, assumed not <code>null</code>.
    * @param currentInfo the information about the relationship being processed,
    *    assumed not <code>null</code>.
    * @param result the result objects to return to the caller, assumed not 
    *    <code>null</code>.
    * @throws PSNavException when any error occurs.
    */
   private void handleTestNew(IPSRequestContext req,
      PSNavRelationshipInfo currentInfo, PSEffectResult result)
      throws PSNavException
   {
      PSComponentSummary dependent = currentInfo.getDependent();
      if (dependent.isItem() && PSNavUtil.isNavType(req, dependent))
      {
         m_log.debug("test inserting new navon");

         PSComponentSummary owner = currentInfo.getOwner();
         PSComponentSummary currentNavon = 
            PSNavFolderUtils.getChildNavonSummary(req, owner);
         if (currentNavon != null)
         {
            PSNavConfig config = PSNavConfig.getInstance();
            
            String folderName = owner.getName();
            String currentItemName = currentNavon.getName();

            String[] args =
            {
               folderName,
               currentItemName
            };
            
            MessageFormat formatter = new MessageFormat(MSG_ALREADY_EXISTS);
            String errMsg = formatter.format(args);
            m_log.warn(errMsg);
            result.setError(errMsg);
            return;
         }
         else
            m_log.debug("no child navon found in folder {}" , owner.getName());
      }
      else
      {
         m_log.debug("ignore this event");
         result.setRecurseDependents(false);
      }

      result.setSuccess();
   }
   
   /**
    * Handles the <code>attempt</code> method when a new item has been
    * created.
    * 
    * @param req the parent request
    * @param currentInfo information about the relationship that caused this
    *           event.
    * @param result the result block to return to the caller.
    * @throws PSNavException when an error occurs.
    */
   private void handleAttemptNew(IPSRequestContext req,
         PSNavRelationshipInfo currentInfo, PSEffectResult result)
         throws PSNavException
   {
      PSComponentSummary dependent = currentInfo.getDependent();
      IPSGuid dependType = dependent.getContentTypeGUID();
      PSNavConfig config = PSNavConfig.getInstance();

      if (dependent.isFolder() && config.getNavonTypes().contains(dependType))
      {
         handleAttemptNewFolder(req, currentInfo, result);
      }
      else
      {

         if (config.getNavonTypes().contains(dependType))
         {
            handleAttemptNewNavon(req, currentInfo, result);
         }
         else if (config.getNavTreeTypes().contains(dependType))
         {
            handleAttemptNewNavTree(req, currentInfo, result);
         }
         else
         {
            m_log.debug("ignore this event, not Navon or NavTree");
            result.setSuccess();
         }
      }
   }

   /**
    * Called when the new item being inserted is a folder.
    * 
    * @param req the parent request context
    * @param currentInfo information about the relationship that caused this
    *           event.
    * @param result the result block to return to the caller.
    * @throws PSNavException when an error occurs.
    */
   private void handleAttemptNewFolder(IPSRequestContext req,
         PSNavRelationshipInfo currentInfo, PSEffectResult result)
         throws PSNavException
   {
      m_log.debug("inserting new folder");
      PSComponentSummary currentFolder = currentInfo.getDependent();
      PSComponentSummary parentFolder = currentInfo.getOwner();
      
      setExclusive(req, true);
      PSComponentSummary navon = null;
      try
      {
         navon = addNavonToChildFolder(req, parentFolder, currentFolder);
      }
      finally
      {
         setExclusive(req, false);
      }
      
      if (navon == null)
         result.setSuccess();
      else
         flushall();         
   }

   /**
    * Called when the new item being inserted is a navon.
    * 
    * @param req the parent request context
    * @param currentInfo information about the relationship that caused this
    *           event.
    * @param result the result block to return to the caller.
    * @throws PSNavException when an error occurs.
    */
   private void handleAttemptNewNavon(IPSRequestContext req,
         PSNavRelationshipInfo currentInfo, PSEffectResult result)
         throws PSNavException
   {
      m_log.debug("inserting new navon");
      PSComponentSummary folder = currentInfo.getOwner();
      PSComponentSummary navon = currentInfo.getDependent();
      m_log
            .debug("navon is {} rev {}",
                   navon.getCurrentLocator().getId(),
                   navon.getCurrentLocator().getRevision());
      PSComponentSummary myParent = PSNavFolderUtils.getParentFolder(req,
            folder);
      if (myParent != null)
      {
         PSNavFolder parentNavFolder = PSNavFolderUtils.getNavParentFolder(req,
               myParent, false);

         if (parentNavFolder != null)
         { //There is a Navon in the folder above this one
            m_log.debug("found parent folder with Navon");
            PSNavFolderUtils.addNavonSubmenu(req, parentNavFolder
                  .getNavonSummary().getCurrentLocator(), navon
                  .getCurrentLocator());
         }
      }

      m_log
            .debug("navon is {} rev {}",
                    navon.getCurrentLocator().getId(),
              navon.getCurrentLocator().getRevision());

      boolean propFlag = isPropagate(navon.getCurrentLocator());
      setExclusive(req, true);
      PSNavFolderUtils.processSubFolders(req, folder, navon, propFlag);
      setExclusive(req, false);

      flushall();
      m_log.debug("done with new Navon");
      result.setSuccess();
   }

   /**
    * Called when the new item being inserted is a NavTree.
    * 
    * @param req the parent request context
    * @param currentInfo information about the relationship that caused this
    *           event.
    * @param result the result block to return to the caller.
    * @throws PSNavException when an error occurs.
    */
   private void handleAttemptNewNavTree(IPSRequestContext req,
         PSNavRelationshipInfo currentInfo, PSEffectResult result)
         throws PSNavException
   {
      PSComponentSummary folder = currentInfo.getOwner();
      PSComponentSummary navon = currentInfo.getDependent();
      m_log.debug("inserting new navtree");

      boolean propFlag = isPropagate(navon.getCurrentLocator());

      setExclusive(req, true);
      PSNavFolderUtils.processSubFolders(req, folder, navon, propFlag);
      setExclusive(req, false);

      flushall();
      m_log.debug("done with new navtree");
      result.setSuccess();
   }

   /**
    * Handles the <code>attempt</code> method for any destroy event.
    * 
    * @param req the parent request, assumed not <code>null</code>.
    * @param currentInfo information about the current relationship, assumed
    *    not <code>null</code>.
    * @throws PSNavException when any error occurs.
    */
   private void handleAttemptDestroy(IPSRequestContext req,
      PSNavRelationshipInfo currentInfo) throws PSNavException
   {
      PSNavConfig config = PSNavConfig.getInstance();

      PSComponentSummary parentFolder = currentInfo.getOwner();
      PSComponentSummary dependent = currentInfo.getDependent();
      if (dependent.getType() == PSComponentSummary.TYPE_FOLDER)
      {
         m_log.debug("removing folder from folder");
         PSComponentSummary parentNavon = PSNavFolderUtils.getChildNavonSummary(
            req, parentFolder);
         if (parentNavon == null)
         {
            m_log.debug("parent folder has no Navon");
            return;
         }
         else
            m_log.debug("parent navon is {}" , parentNavon.getName());

         PSComponentSummary navon = PSNavFolderUtils.getChildNavonSummary(req, 
            dependent);
         if (navon == null)
         {
            m_log.debug("child Navon not found");
            return;
         }
         else
            m_log.debug("child Navon is {}" , navon.getName());

         PSNavFolderUtils.removeNavonChild(req, parentNavon.getCurrentLocator(), 
            navon.getCurrentLocator());
      }
      else if (config.getNavonTypes().contains(dependent.getContentTypeGUID()))
      {
         m_log.debug("removing Navon from folder");
         m_log.debug("child navon is {}" , dependent.getName());

         PSNavFolderUtils.removeNavonParents(req, dependent.getCurrentLocator(), null);
      }
      
      flushall(); 
   }

   /**
    * Flushes the assembly cache. This method is here primarily to encapsulate
    * any exceptions thrown from the <code>PSCacheProxy</code>.
    * 
    * @throws PSNavException
    */
   private void flushall() throws PSNavException
   {
      try
      {
         PSCacheProxy.flushAssemblers(null, null, null, null);
      }
      catch (Exception ex)
      {
         throw new PSNavException(ex);
      }
      m_log.debug("cache flushed");
   }

   /**
    * Determines if this Navon or NavTree has the "propagate" flag set.
    * 
    * @param navonLoc the locator for the current navon, assumed to exist.
    * 
    * @return <code>true</code> if the propagate check box was checked.
    * 
    * @throws PSNavException when any error occurs.
    */
   private boolean isPropagate(PSLocator navonLoc) throws PSNavException
   {
      IPSContentMgr mgr = PSContentMgrLocator.getContentMgr();
      PSNavConfig config = PSNavConfig.getInstance();
      try
      {
         PSContentMgrConfig conf = new PSContentMgrConfig();

         //Don't return the binary value by default.
         conf.addOption(PSContentMgrOption.LOAD_MINIMAL);
         conf.addOption(PSContentMgrOption.LAZY_LOAD_CHILDREN);

         List<Node> nodes = mgr.findItemsByGUID(PSGuidUtils.toGuidList(
            new PSLegacyGuid(navonLoc)), conf);
         if (nodes.isEmpty())
         {
            throw new RuntimeException(
               "Failed to check propagation property of navon: " + 
               navonLoc);
         }
         Node navonNode = nodes.get(0);
         String propField = config.getNavonPropagateField();
         
         String propValue = null;
         if (propField != null && navonNode.hasProperty(propField))
         {
            Property prop = navonNode.getProperty(propField);
            propValue = prop.getString();
         }
         
         if (propValue != null)
         {
            m_log.debug("propagate field returns {}" , propValue);
            if (propValue.equalsIgnoreCase("1"))
            {
               m_log.debug("propagate is TRUE");
               return true;
            }
         }         
      }
      catch (RepositoryException e)
      {
         throw new PSNavException(e);
      }
      catch (Exception ex)
      {
         m_log.error(this.getClass(), ex);
         throw new PSNavException(this.getClass().getName(), ex);
      }

      return false;
   }

   /**
    * This message is displayed to the end-user if he tries to add a 
    * <code>Navon</code> or <code>NavonTree</code> item to a folder which 
    * already contains an object of either type.
    */
   private static final String MSG_ALREADY_EXISTS = "Folder \"{0}\" already " +
      "contains item \"{1}\" of Navigation type. \"{2}\". Multiple items of navigation types cannot co-exist in a Folder.";
}
