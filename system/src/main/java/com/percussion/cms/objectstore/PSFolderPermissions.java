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
package com.percussion.cms.objectstore;

import com.percussion.cms.PSCmsException;
import com.percussion.design.objectstore.PSAclEntry;
import com.percussion.security.PSAuthenticationRequiredException;
import com.percussion.security.PSAuthorizationException;
import com.percussion.security.PSThreadRequestUtils;

import java.util.Iterator;

/**
 * This class computes the user's permission on a folder object. This should
 * only be used in the context of a folder object.
 */
public class PSFolderPermissions extends PSObjectPermissions
{
   /**
    * This constructor will typically be used on the server side.
    *
    * @param folderAcl acl of the folder being accessed by the user,
    * may not be <code>null</code>
    *
    * @param request request context information, may not be <code>null</code>.
    *
    * @throws IllegalArgumentException if <code>request</code> or
    * <code>folderAcl</code> is <code>null</code>
    *
    * @throws PSCmsException if request to server to get the user info fails
    * for any reason
    *
    * @throws PSAuthorizationException if any error occurs getting the user's
    * server access level
    */
   public PSFolderPermissions(PSFolderAcl folderAcl)
      throws PSAuthorizationException, PSCmsException
   {
      super();

      if (folderAcl == null)
         throw new IllegalArgumentException("folderAcl may not be null");
      m_folderAcl = folderAcl;

      init(folderAcl);
   }

   public PSFolderPermissions(int permissions)
   {
      super(permissions);
   }

   /**
    * Returns a cloned object having the same permissions as this object.
    *
    * @return the clone of this object, never <code>null</code>
    */
   public Object clone()
   {
      return new PSFolderPermissions(m_permissions);
   }

   /**
    * Compares the specified object with this object. Returns <code>true</code>
    * if the reference to this object itself is specified. Returns
    * <code>false</code> if the specified object is not an instance of this
    * class.
    *
    * @param obj the object with which this object should be compared,
    * may not be <code>null</code>
    *
    * @return <code>true</code> if the specified object is an instance of this
    * class and represents the same permissions.
    * Returns <code>false</code> otherwise.
    *
    * @throws IllegalArgumentException if <code>obj</code> is <code>null</code>
    */
   public boolean equals(Object obj)
   {
      boolean equals = super.equals(obj);
      if (equals)
      {
         if (!(obj instanceof PSFolderPermissions))
            equals = false;
      }
      return equals;
   }

   /**
    * Returns the hashcode of this object. This simply returns the permissions
    * set on the securable object.
    *
    * @return the hashcode of this object, always non-negative
    */
   public int hashCode()
   {
      return super.hashCode();
   }

   /**
    * See base class.
    * {@link com.percussion.cms.objectstore.PSObjectPermissions#processAcl()
    * processAcl()}
    * @throws PSAuthorizationException 
    * @throws PSAuthenticationRequiredException 
    */
   protected boolean processAcl() throws PSAuthenticationRequiredException, PSAuthorizationException
   {
      int permissions = -1;

      // check if the user has server administrator privileges
      int userAccessLevel = PSThreadRequestUtils.getUserAccessLevel();
      int adminAccessLevel = PSAclEntry.SACE_ADMINISTER_SERVER;

      if ((userAccessLevel & adminAccessLevel) == adminAccessLevel)
      {
         // server administrator has all the privileges
         permissions = ACCESS_SERVER_ADMIN;
      }
      else
      {
         // if no ACL is specified then everyone has all the permissions
         // this is also for backwards compatibility
         if (m_objectAcl.isEmpty())
         {
            permissions = ACCESS_ALL;
         }
         else
         {
            PSObjectAclEntry aclEntry = new PSObjectAclEntry(
                  PSObjectAclEntry.ACL_ENTRY_TYPE_USER,
                  m_userInfo.getUserName(),
                  PSObjectAclEntry.ACCESS_DENY);

            aclEntry = (PSObjectAclEntry)m_objectAcl.get(aclEntry);
            if (aclEntry != null)
            {
               // if user specific ACL entry is found then it solely
               // determines the user's privileges on the object and no
               // additional calculation is necessary
               permissions = aclEntry.getPermissions();
            }
         }
      }

      if (permissions != -1)
      {
         m_permissions = permissions;
         return false;
      }
      return true;
   }

   /**
    * See base class.
    * {@link com.percussion.cms.objectstore.PSObjectPermissions#processAclEntry(PSObjectAclEntry)
    * processAclEntry(PSObjectAclEntry)}
    */
   protected boolean processAclEntry(PSObjectAclEntry aclEntry)
   {
      if (aclEntry != null)
      {
         switch (aclEntry.getType())
         {
            case PSObjectAclEntry.ACL_ENTRY_TYPE_USER:
               // already processed the user in processAcl() method.
               break;

            case PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE:
               String aclRole = aclEntry.getName();
               Iterator it = m_userInfo.getRoles();
               while (it.hasNext())
               {
                  String userRole = (String)it.next();
                  if (userRole.equalsIgnoreCase(aclRole))
                     m_permissions = m_permissions | aclEntry.getPermissions();
               }
               break;

            case PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL:
               String virEntryName = aclEntry.getName();
               if (virEntryName.equalsIgnoreCase(
                  PSObjectAclEntry.ACL_ENTRY_FOLDER_COMMUNITY))
               {
                  int folderCommunity = m_folderAcl.getCommunityId();
                  int userCommunity = m_userInfo.getCommunityId();
                  if ((folderCommunity == userCommunity) ||
                     (folderCommunity == -1))
                  {
                     m_permissions = m_permissions | aclEntry.getPermissions();
                  }
               }
               else if (virEntryName.equalsIgnoreCase(
                  PSObjectAclEntry.ACL_ENTRY_EVERYONE))
               {
                  m_permissions = m_permissions | aclEntry.getPermissions();
               }
               break;

            default:
               break;
         }
      }
      return true;
   }

   /**
    * Acl of the folder for which the permissions is being evaluated,
    * may be <code>null</code> if single arg constructor is used, otherwise
    * initialized in the constructor, never modified after initializartion.
    */
   private PSFolderAcl m_folderAcl = null;
}


















