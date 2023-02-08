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
package com.percussion.cms.objectstore.server;

import com.percussion.cms.IPSCmsErrors;
import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSFolderAcl;
import com.percussion.cms.objectstore.PSFolderPermissions;
import com.percussion.cms.objectstore.PSObjectAclEntry;
import com.percussion.cms.objectstore.PSObjectPermissions;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.error.PSException;
import com.percussion.security.PSAuthorizationException;
import com.percussion.security.PSThreadRequestUtils;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.IPSServerErrors;
import com.percussion.server.PSInternalRequest;
import com.percussion.server.PSRequest;
import com.percussion.server.PSRequestContext;
import com.percussion.server.PSServer;
import com.percussion.server.webservices.PSServerFolderProcessor;
import com.percussion.util.IPSHtmlParameters;
import com.percussion.utils.request.PSRequestInfo;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This class is used to obtain the permissions on a specified folder
 * for a particular user. It loads the folder specified by a locator or
 * content id using the local processor and hence can only be used on the
 * server side (and not by Content Explorer or Web Services which require a
 * different processor).
 */
public class PSFolderSecurityManager
{
   /**
    * Constructor.
    *
    * @param locator locator for the folder, may not be <code>null</code>
    * @param request request context information, may not be <code>null</code>.
    *
    * @throws IllegalArgumentException if <code>locator</code> or
    * <code>request</code> is <code>null</code>
    * @throws PSCmsException if any error occurs
    */
   public PSFolderSecurityManager(PSLocator locator, IPSRequestContext request)
      throws PSCmsException
   {
      if (locator == null)
         throw new IllegalArgumentException("locator may not be null");
      if (request == null)
         throw new IllegalArgumentException("request may not be null");

      init(locator, request);
   }

   /**
    * Constructor.
    *
    * @param contentId content id for the folder, should be non-negative
    * @param request request context information, may not be <code>null</code>.
    *
    * @throws IllegalArgumentException if <code>contentId</code> is negative
    * or <code>request</code> is <code>null</code>
    * @throws PSCmsException if any error occurs
    */
   public PSFolderSecurityManager(int contentId, IPSRequestContext request)
      throws PSCmsException
   {
      this(new PSLocator(contentId, 1), request);
   }

   /**
    * Loads the folder acl objects from the repository for the specified folder
    * ids.
    *
    * @param ids The ids of the folders, it may be <code>null</code> for 
    *    loading all folder acles.
    *
    * @return An array of folder acl objects, never <code>null</code>, may be
    * emtpy.  Not guaranteed to contain an acl corresponding to each id
    * supplied.
    *
    * @throws IllegalArgumentException if <code>ids</code> is <code>null</code>.
    * @throws PSCmsException if there are any other errors.
    */
   public static PSFolderAcl[] loadFolderAcls(int[] ids) throws PSCmsException
   {
      // TODO: Steve - This should be replaced with AclService not an internal request.  
      // This method is not normally called directly and folder ACL will be pulled from PSItemSummaryCache through PSServerFolderProcessor and
      // when populating the cache initially.
      Map params = new HashMap<>();
      
      if (ids != null)
      {
         List idList = new ArrayList<>();
         for (int i = 0; i < ids.length; i++)
            idList.add(String.valueOf(ids[i]));
         params.put(IPSHtmlParameters.SYS_CONTENTID, idList);
      }

      PSInternalRequest ir = PSServer.getInternalRequest(FOLDER_ACL_RESOURCE,
         new PSRequestContext(PSRequest.getContextForRequest()), params, false,
         null);
      if (ir == null)
         throw new PSCmsException(IPSCmsErrors.REQUIRED_RESOURCE_MISSING,
            FOLDER_ACL_RESOURCE);
      List aclList = new ArrayList<>();
      ResultSet rs = null;
      try
      {
         // to improve the performance, we process the result set directly.
         rs = ir.getResultSet();
         int contentId, permissions, type, sysId, communityId;
         String name;
         PSFolderAcl folderAcl = null;
         PSObjectAclEntry aclEntry;
         while (rs.next())
         {
            contentId = rs.getInt(1);
            name = rs.getString(2);
            permissions = rs.getInt(3);
            type = rs.getInt(4);
            sysId = rs.getInt(5);
            communityId = rs.getInt(6);

            aclEntry = new PSObjectAclEntry(sysId, type, name, permissions);
            if (folderAcl == null || folderAcl.getContentId() != contentId)
            {
               folderAcl = new PSFolderAcl(contentId, communityId);
               aclList.add(folderAcl);
            }
            folderAcl.add(aclEntry);
         }

         return (PSFolderAcl[])aclList.toArray(new PSFolderAcl[aclList.size()]);
      }
      catch (SQLException e)
      {
         throw new PSCmsException(IPSServerErrors.CE_SQL_ERRORS, e.toString());
      }
      catch (PSException e)
      {
         throw new PSCmsException(e);
      }
      finally
      {
         if (rs != null)
         {
            try
            {
               rs.close();
            }
            catch (SQLException e)
            {
               // quiet
            }
         }
         ir.cleanUp();
      }
   }

   /**
    * Loads the folder using local processor and obtains its ACL.
    * Obtains the permissions set on the folder for the user using the
    * folder's ACL and user's info obtained from the Request Context object.
    * <p>
    * If the request context object has a private object with a key of
    * <code>CHECK_USER_FOLDER_PERMISSION</code> and a value of the current
    * thread (<code>PSUserThread</code>) object, then this method does not
    * load the folder to get its ACL, instead it just sets the permission to
    * <code>PSFolderPermissions.ACCESS_ALL</code>. This is to avoid going into
    * a recursive loop since loading a folder results in a call to this method
    * from the <code>PSExitAuthenticateUser</code> exit. The first call to this
    * method sets the private object in the request object associated with the
    * thread and then loads the folder to get its ACL. The next calls to this
    * method see the private object and do not try to load the folder.
    * <p>
    *
    * @param locator locator for the folder, assumed not <code>null</code>
    * @param request request context information, may not be <code>null</code>.
    *
    * @throws PSCmsException if any error occurs
    */
   private void init(PSLocator locator, IPSRequestContext request)
      throws PSCmsException
   {
      
   }

   /**
    * Returns a <code>PSObjectPermissions</code> object which encapsulates an
    * access mask. This mask determines the level of access for the user
    * accessing the folder.
    * @param i 
    * @param ipsRequestContext 
    *
    * @return the permissions set on the folder for the user accessing the
    * folder, never <code>null</code>
    * @throws PSCmsException 
    */
   public static PSObjectPermissions getPermissions(int i) throws PSCmsException
   {
      PSFolderPermissions m_permissions = null;
      Object contextVal =
            PSThreadRequestUtils.getPSRequest().getPrivateObject(CHECK_USER_FOLDER_PERMISSION);
      Thread thread = Thread.currentThread();
      boolean process = true;

      if ((contextVal != null) && (contextVal == thread))
      {
         m_permissions = new PSFolderPermissions(
            PSFolderPermissions.ACCESS_ALL);
         process = false;
      }

      if (process)
      {
         PSRequest req = (PSRequest) PSRequestInfo
               .getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
         if (req != null)
         {
            Object requestVal = req
                  .getPrivateObject(CHECK_USER_FOLDER_PERMISSION);
            if ((requestVal != null) && (requestVal == thread))
            {
               m_permissions = new PSFolderPermissions(
                     PSFolderPermissions.ACCESS_ALL);
               process = false;
            }
         }
     
         try
         {
            // obtain the folder permissions for the user
            PSServerFolderProcessor processor = PSServerFolderProcessor.getInstance();
            int[] ids = new int[] {i};
            PSFolderAcl[] acls = processor.getFolderAcls(ids);
            if (acls.length == 0)
            {
               // no acl, so they get full permissions
               m_permissions = new PSFolderPermissions(
                  PSFolderPermissions.ACCESS_ALL);
               process = false;
            }
            else
            {
               m_permissions = new PSFolderPermissions(acls[0]);
               process = false;
            }
         }
         catch (PSAuthorizationException ex)
         {
            throw new PSCmsException(ex);
         }
      }
      if (m_permissions==null) m_permissions = new PSFolderPermissions(PSFolderPermissions.ACCESS_DENY);
      return m_permissions;
   }

   /**
    * Checks if the user making the request has the specified permission on the
    * folder whose content id equals <code>contentId</code>.
    *
    * @param contentId content id for the folder, should be non-negative
    * @param request request context information, may not be <code>null</code>.
    * @param permission the permission which the user should have on the folder
    * for this method to return <code>true</code>
    *
    * @return <code>true</code> if the user making the request has the specified
    * permission on the folder specified by <code>contentId</code>,
    * <code>false</code> otherwise
    *
    * @throws IllegalArgumentException if <code>contentId</code> is negative
    * or <code>request</code> is <code>null</code>
    * @throws PSCmsException if any error occurs
    */
   public static boolean verifyFolderPermissions(int contentId, int permission) throws PSCmsException
   {
      PSObjectPermissions objPermissions = PSFolderSecurityManager.getPermissions(contentId);
      return objPermissions.hasAccess(permission);
   }

   /**
    * This method can be used to turn on or off user's folder permissions
    * checking by the <code>PSExitAuthenticateUser</code> exit. This adds a
    * private object with a key of <code>CHECK_USER_FOLDER_PERMISSION</code>
    * and the value of the current thread (<code>PSUserThread</code>) object.
    * This methods should be used only in case the caller will make sure that
    * it will perform the necessary folder permission checking. This provides
    * a workaround to the folder being loaded twice for the same request and
    * the authorization being done twice.
    *
    * @param request the request object on which to tun on or off the folder
    * permission checking, may not be <code>null</code>
    *
    * @param checkPermission <code>true</code> if the folder permission checking
    * should be done on the specified request, <code>false</code> otherwise
    *
    * @throws IllegalArgumentException if <code>request</code> is
    * <code>null</code>
    */
   public static void setCheckFolderPermissions(
       boolean checkPermission)
   {
      PSRequest request = PSThreadRequestUtils.getPSRequest();
      if (request == null)
         throw new IllegalArgumentException("request may not be null");

      if (checkPermission)
      {
         request.setPrivateObject(CHECK_USER_FOLDER_PERMISSION, null);
      }
      else
      {
         request.setPrivateObject(
            CHECK_USER_FOLDER_PERMISSION, Thread.currentThread());
      }
   }


   /**
    * If the request's private object contains this key and if the value of
    * this key matches the current thread object then this class simply sets
    * the access to "admin" access and does not actually load the folder to
    * obtain its ACL and use it to obtain the user's permission. This is to
    * prevent going into a loop.
    * See {@link #init(PSLocator, IPSRequestContext)
    * init(PSLocator, IPSRequestContext)} for details.
    */
   public static final String CHECK_USER_FOLDER_PERMISSION =
      "checkUserFolderPermission";

   /**
    * Resource used to query folder acl lists.
    */
   private static final String FOLDER_ACL_RESOURCE =
      "sys_psxInternalResources/getFolderAcls";
}



