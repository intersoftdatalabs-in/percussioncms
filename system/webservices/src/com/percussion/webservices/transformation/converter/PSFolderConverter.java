/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.webservices.transformation.converter;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.cms.objectstore.PSFolderProperty;
import com.percussion.cms.objectstore.PSObjectAcl;
import com.percussion.cms.objectstore.PSObjectAclEntry;
import com.percussion.cms.objectstore.PSObjectPermissions;
import com.percussion.design.objectstore.PSLocator;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.webservices.common.Reference;
import com.percussion.webservices.content.PSFolderPropertiesProperty;
import com.percussion.webservices.content.PSFolderSecurityAclEntry;
import com.percussion.webservices.content.PSFolderSecurityAclEntryType;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.lang3.StringUtils;

/**
 * Converts objects between the classes
 * {@link com.percussion.cms.objectstore.PSFolder} and
 * {@link com.percussion.webservices.content.PSFolder}
 * <p>
 * Note, when converting from webservice to objectore, the permissions
 * will always be set to {@link PSObjectPermissions#ACCESS_ADMIN}. It is the
 * caller's responsibility to reset the permissions. The permissions value is
 * a transient data. See
 * {@link com.percussion.cms.objectstore.PSFolder#getPermissions()} for detail.
 */
public class PSFolderConverter extends PSConverter
{
   /*
    * (non-Javadoc)
    *
    * @see PSConverter#PSConvert(BeanUtilsUtil)
    */
   public PSFolderConverter(BeanUtilsBean beanUtils)
   {
      super(beanUtils);
   }

   /*
    * (non-Javadoc)
    *
    * @see PSConverter#convert(Class, Object)
    */
   public Object convert(Class type, Object value) {
      if (value == null)
         return null;

      if (isClientToServer(value))
      {
         com.percussion.webservices.content.PSFolder source =
            (com.percussion.webservices.content.PSFolder) value;

         int id = -1;
         if (source.getId() != null)
            id = new PSLegacyGuid(source.getId()).getContentId();
         int communityId = -1;
         if (source.getCommunity() != null)
         {
            communityId = (int) new PSGuid(PSTypeEnum.COMMUNITY_DEF,
                  source.getCommunity().getId()).longValue();
         }
         int displayFormatId = -1;
         if (source.getDisplayFormat() != null)
         {
            displayFormatId = new PSGuid(PSTypeEnum.DISPLAY_FORMAT,
               source.getDisplayFormat().getId()).getUUID();
         }

         // assumed full access for this transient data when converting
         // from webservice to objectstore. The caller needs to reset
         // this value as needed.
         int permissions = PSObjectPermissions.ACCESS_ADMIN;

         PSFolder target = new PSFolder(source.getName(), communityId, permissions,
               source.getDescription());
         if (id >= 0)
         {
            PSLocator locator = new PSLocator(id, 1);
            target.setLocator(locator);
         }

         if (displayFormatId != -1)
         {
            target.setDisplayFormatId(displayFormatId);
            target.setDisplayFormatName(source.getDisplayFormat().getName());
         }
         if (communityId != -1)
            target.setCommunityName(source.getCommunity().getName());
         if (! StringUtils.isBlank(source.getLocaleCode()))
            target.setLocale(source.getLocaleCode());
         target.setGlobalTemplateProperty(source.getGlobalTemplate());
         if (! StringUtils.isBlank(source.getPath()))
            target.setFolderPath(source.getPath());
         setProperties(target, source);
         setActSecurity(target, source);

         return target;
      }
      else // convert from server to webservice
      {
         PSFolder source = (PSFolder) value;

         // get the display format reference
         Reference displayFormat = null;
         if (source.getDisplayFormatId() >= 0)
         {
            PSDesignGuid id = new PSDesignGuid(PSTypeEnum.DISPLAY_FORMAT,
                  source.getDisplayFormatId());
            displayFormat = new Reference();
            displayFormat.setId(id.getValue());
            displayFormat.setName(source.getDisplayFormatName());
         }
         // get the community reference
         Reference community = null;
         if (source.getCommunityId() >= 0)
         {
            PSDesignGuid id = new PSDesignGuid(PSTypeEnum.COMMUNITY_DEF,
                  source.getCommunityId());
            community = new Reference();
            community.setId(id.getValue());
            community.setName(source.getCommunityName());
         }

         PSDesignGuid id = new PSDesignGuid(
            new PSLegacyGuid(source.getLocator()));

         com.percussion.webservices.content.PSFolder target = new com.percussion.webservices.content.PSFolder();

         // Use reflection to set possibly differing DTO shapes for security and properties
         try {
            java.lang.reflect.Method mSec = target.getClass().getMethod("setSecurity", getWsSecurity(source).getClass());
            try { mSec.invoke(target, (Object) getWsSecurity(source)); } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) { /* ignore */ }
         } catch (NoSuchMethodException nsme) {
            try {
               java.lang.reflect.Method mSec2 = target.getClass().getMethod("setSecurity", java.util.List.class);
               try { mSec2.invoke(target, java.util.Arrays.asList(getWsSecurity(source))); } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) { /* ignore */ }
            } catch (Exception __) {
               // ignore - best effort
            }
         }

         try {
            java.lang.reflect.Method mProps = target.getClass().getMethod("setProperties", getWsProperties(source).getClass());
            try { mProps.invoke(target, (Object) getWsProperties(source)); } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) { /* ignore */ }
         } catch (NoSuchMethodException nsme) {
            try {
               java.lang.reflect.Method mProps2 = target.getClass().getMethod("setProperties", java.util.List.class);
               try { mProps2.invoke(target, java.util.Arrays.asList(getWsProperties(source))); } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) { /* ignore */ }
            } catch (Exception __) {
               // ignore - best effort
            }
         }

         if (displayFormat != null) {
            try {
               java.lang.reflect.Method mDisplay = target.getClass().getMethod("setDisplayFormat", displayFormat.getClass());
               try { mDisplay.invoke(target, displayFormat); } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) { /* ignore */ }
            } catch (Exception ignored) {
            }
         }
         if (community != null) {
            try {
               java.lang.reflect.Method mComm = target.getClass().getMethod("setCommunity", community.getClass());
               try { mComm.invoke(target, community); } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) { /* ignore */ }
            } catch (Exception ignored) {
            }
         }

         try { java.lang.reflect.Method mId = target.getClass().getMethod("setId", long.class); try { mId.invoke(target, id.getValue()); } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) { /* ignore */ } } catch (Exception ignored) {}
         try { java.lang.reflect.Method mName = target.getClass().getMethod("setName", String.class); try { mName.invoke(target, source.getName()); } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) { /* ignore */ } } catch (Exception ignored) {}
         try { java.lang.reflect.Method mPath = target.getClass().getMethod("setFolderPath", String.class); try { mPath.invoke(target, source.getFolderPath()); } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) { /* ignore */ } } catch (Exception ignored) {}
         try { java.lang.reflect.Method mLocale = target.getClass().getMethod("setLocale", String.class); try { mLocale.invoke(target, source.getLocale()); } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) { /* ignore */ } } catch (Exception ignored) {}
         try { java.lang.reflect.Method mGlobal = target.getClass().getMethod("setGlobalTemplate", String.class); try { mGlobal.invoke(target, source.getGlobalTemplateProperty()); } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) { /* ignore */ } } catch (Exception ignored) {}
         try { java.lang.reflect.Method mDesc = target.getClass().getMethod("setDescription", String.class); try { mDesc.invoke(target, source.getDescription()); } catch (IllegalAccessException|java.lang.reflect.InvocationTargetException e) { /* ignore */ } } catch (Exception ignored) {}

         return target;
      }
   }

   /**
    * Sets the properties from the specified webservice source object to
    * the specified server target object.
    *
    * @param target the target folder, assumed not <code>null</code>.
    * @param source the source folder, assumed not <code>null</code>.
    */
   private void setProperties(PSFolder target,
         com.percussion.webservices.content.PSFolder source)
   {
      Object propsObj = source.getProperties();
      if (propsObj == null)
         return;

      if (propsObj instanceof PSFolderPropertiesProperty[]) {
         for (PSFolderPropertiesProperty prop : (PSFolderPropertiesProperty[]) propsObj) {
            if (prop.getValue() != null) {
               target.setProperty(prop.getName(), prop.getValue());
            }
         }
         return;
      }

      if (propsObj instanceof java.util.List) {
         for (Object o : (java.util.List<?>) propsObj) {
            PSFolderPropertiesProperty prop = (PSFolderPropertiesProperty) o;
            if (prop.getValue() != null) {
               target.setProperty(prop.getName(), prop.getValue());
            }
         }
         return;
      }

      // Best-effort: attempt to iterate via iterator() method reflectively
      try {
         java.lang.reflect.Method m = propsObj.getClass().getMethod("iterator");
         java.util.Iterator<?> it = (java.util.Iterator<?>) m.invoke(propsObj);
         while (it.hasNext()) {
            PSFolderPropertiesProperty prop = (PSFolderPropertiesProperty) it.next();
            if (prop.getValue() != null) {
               target.setProperty(prop.getName(), prop.getValue());
            }
         }
      } catch (Exception ignored) {
         // give up
      }
   }

   /**
    * Converts the properties from the objectstore to webservice objects,
    * excluding the known properties.
    *
    * @param source the source folder, assumed not <code>null</code>.
    *
    * @return the converted properties. It may be <code>null</code> if there
    *    is no unknown properties.
    */
   private PSFolderPropertiesProperty[] getWsProperties(PSFolder source)
   {
      List<PSFolderPropertiesProperty> tgtProps =
         new ArrayList<PSFolderPropertiesProperty>();
      java.util.Iterator<?> props = source.getProperties();
      PSFolderProperty prop;
      PSFolderPropertiesProperty tgtProp;
      while (props.hasNext())
      {
         prop = (PSFolderProperty) props.next();
         String pname = prop.getName();
         if ((!pname.equals(PSFolder.PROPERTY_DISPLAYFORMATID)) &&
               (!pname.equals(PSFolder.PROPERTY_GLOBALTEMPLATE)))
         {
            tgtProp = new PSFolderPropertiesProperty();
            tgtProp.setName(prop.getName());
            tgtProp.setValue(prop.getValue());
            tgtProps.add(tgtProp);
         }
      }
      if (tgtProps.isEmpty())
         return null;

      PSFolderPropertiesProperty[] result =
         new PSFolderPropertiesProperty[tgtProps.size()];
      tgtProps.toArray(result);
      return result;
   }

   /**
    * Converts the security (or the ACL entries from the objectstore to
    * webservice objects.
    *
    * @param source the source folder, assumed not <code>null</code>.
    *
    * @return the converted ACL entries. It may be <code>null</code> if there
    *    is no ACLs in the source folder.
    */
   private PSFolderSecurityAclEntry[] getWsSecurity(PSFolder source)
   {
      List<PSFolderSecurityAclEntry> tgtAclEntries =
         new ArrayList<PSFolderSecurityAclEntry>();

      java.util.Iterator<PSObjectAclEntry> sourceAcls = source.getAcl().iterator();
      PSObjectAclEntry srcAcl;
      PSFolderSecurityAclEntry tgtAcl;
      PSFolderSecurityAclEntryType tgtType;
      while (sourceAcls.hasNext())
      {
         srcAcl = sourceAcls.next();

         // get the ACL type
         if (srcAcl.getType() == PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE)
            tgtType = PSFolderSecurityAclEntryType.role;
         else if (srcAcl.getType() == PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL)
            tgtType = PSFolderSecurityAclEntryType.group;
         else
            tgtType = PSFolderSecurityAclEntryType.user;

         tgtAcl = new PSFolderSecurityAclEntry();
         tgtAcl.setName(srcAcl.getName());
         tgtAcl.setType(tgtType);

         tgtAclEntries.add(tgtAcl);
      }

      if (tgtAclEntries.isEmpty())
         return null;

      PSFolderSecurityAclEntry[] result =
         new PSFolderSecurityAclEntry[tgtAclEntries.size()];
      tgtAclEntries.toArray(result);

      return result;
   }

   /**
    * Sets the security or ACL entries from the specified webservice source
    * object to the specified server target object.
    *
    * @param target the target folder, assumed not <code>null</code>.
    * @param source the source folder, assumed not <code>null</code>.
    */
   private void setActSecurity(PSFolder target,
         com.percussion.webservices.content.PSFolder source)
   {
      if (source.getSecurity() == null)
         return;

      PSObjectAclEntry aclEntry;
      PSObjectAcl tgtAcls = new PSObjectAcl();
      Object secObj = source.getSecurity();
      if (secObj == null) return;
      if (secObj instanceof PSFolderSecurityAclEntry[]) {
         for (PSFolderSecurityAclEntry acl : (PSFolderSecurityAclEntry[]) secObj) {
            int permissions = PSObjectAclEntry.ACCESS_ADMIN;
            PSFolderSecurityAclEntryType srcType = acl.getType();
            int tgtType = srcType == PSFolderSecurityAclEntryType.role ? PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE : srcType == PSFolderSecurityAclEntryType.group ? PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL : PSObjectAclEntry.ACL_ENTRY_TYPE_USER;
            aclEntry = new PSObjectAclEntry(tgtType, acl.getName(), permissions);
            tgtAcls.add(aclEntry);
         }
      }

      if (secObj instanceof java.util.List) {
         for (Object o : (java.util.List<?>) secObj) {
            PSFolderSecurityAclEntry acl = (PSFolderSecurityAclEntry) o;
            int permissions = PSObjectAclEntry.ACCESS_ADMIN;
            PSFolderSecurityAclEntryType srcType = acl.getType();
            int tgtType = srcType == PSFolderSecurityAclEntryType.role ? PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE : srcType == PSFolderSecurityAclEntryType.group ? PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL : PSObjectAclEntry.ACL_ENTRY_TYPE_USER;
            aclEntry = new PSObjectAclEntry(tgtType, acl.getName(), permissions);
            tgtAcls.add(aclEntry);
         }
      }

      try {
         java.lang.reflect.Method m = secObj.getClass().getMethod("iterator");
         java.util.Iterator<?> it = (java.util.Iterator<?>) m.invoke(secObj);
         while (it.hasNext()) {
            PSFolderSecurityAclEntry acl = (PSFolderSecurityAclEntry) it.next();
            int permissions = PSObjectAclEntry.ACCESS_ADMIN;
            PSFolderSecurityAclEntryType srcType = acl.getType();
            int tgtType = srcType == PSFolderSecurityAclEntryType.role ? PSObjectAclEntry.ACL_ENTRY_TYPE_ROLE : srcType == PSFolderSecurityAclEntryType.group ? PSObjectAclEntry.ACL_ENTRY_TYPE_VIRTUAL : PSObjectAclEntry.ACL_ENTRY_TYPE_USER;
            aclEntry = new PSObjectAclEntry(tgtType, acl.getName(), permissions);
            tgtAcls.add(aclEntry);
         }
      } catch (Exception ignored) {
         // give up
      }

      target.setAcl(tgtAcls);
   }

   // Constants defined in PSFolderSecurityAclEntryType
   // NOTE: The webservice enum uses 'role','user','group' values.

}
