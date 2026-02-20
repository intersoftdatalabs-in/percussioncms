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

import com.percussion.cms.PSCmsException;
import com.percussion.cms.objectstore.PSCoreItem;
import com.percussion.cms.objectstore.PSItemDefinition;

import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.webservices.content.PSItem;
import com.percussion.webservices.content.PSItemFolders;
import com.percussion.webservices.content.PSItemChildren;
import com.percussion.webservices.content.PSItemSlots;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.beanutils.BeanUtilsBean;
import org.apache.commons.beanutils.ConversionException;
import org.apache.commons.lang3.StringUtils;

/**
 * Converts objects between the classes
 * {@link com.percussion.cms.objectstore.server.PSServerItem} and
 * {@link com.percussion.webservices.content.PSItem}
 *
 * If the item contains related item, then the related item does not contain
 * its related items or binary data. See {@link PSRelatedItemConverter}
 */
public class PSItemConverter extends PSConverter
{
   /**
    * @see PSConverter#PSConverter(BeanUtilsBean)
    */
   public PSItemConverter(BeanUtilsBean beanUtils)
   {
      super(beanUtils);
   }

   /* (non-Javadoc)
    * @see PSConverter#convert(Class, Object)
    */
   @Override
   public Object convert(@SuppressWarnings("unused") Class type, Object value)
   {
      if (value == null)
         return null;

      try
      {
         if (isClientToServer(value))
         {
            PSItem orig = (PSItem) value;

            PSLegacyGuid guid = new PSLegacyGuid(orig.getId());
            PSItemDefinition itemDefinition =
               PSItemConverterUtils.getItemDefinition(orig.getContentType());

            PSCoreItem dest = new PSCoreItem(itemDefinition);
            dest.setContentId(guid.getContentId());
            if (guid.getRevision() != -1)
               dest.setRevision(guid.getRevision());
            if (!StringUtils.isBlank(orig.getSystemLocale()))
               dest.setSystemLocale(new Locale(orig.getSystemLocale()));
            if (!StringUtils.isBlank(orig.getDataLocale()))
               dest.setDataLocale(new Locale(orig.getDataLocale()));
            dest.setCheckedOutByName(orig.getCheckedOutBy());

            // convert fields
            if (orig.getFields() != null && orig.getFields().getPSField() != null) {
               java.util.List<com.percussion.webservices.content.PSField> fList = orig.getFields().getPSField();
               com.percussion.webservices.content.PSField[] fa = fList.toArray(new com.percussion.webservices.content.PSField[fList.size()]);
               PSItemConverterUtils.toServerFields(dest, guid, fa);
            }

            // convert children
            if (orig.getChildren() != null && !orig.getChildren().isEmpty()) {
               java.util.List<PSItemChildren> children = new java.util.ArrayList<>();
               for (com.percussion.webservices.content.PSItem.Children c : orig.getChildren()) {
                  PSItemChildren cc = new PSItemChildren();
                  cc.setName(c.getName());
                  cc.setDisplayName(c.getDisplayName());
                  cc.setSequenced(c.isSequenced());
                  cc.setPSChildEntry(c.getPSChildEntry());
                  children.add(cc);
               }
               PSItemConverterUtils.toServerChildren(dest, children, this);
            }

            // convert related content
            if (orig.getSlots() != null && !orig.getSlots().isEmpty()) {
               java.util.List<PSItemSlots> slots = new java.util.ArrayList<>();
               for (com.percussion.webservices.content.PSItem.Slots s : orig.getSlots()) {
                  PSItemSlots ss = new PSItemSlots();
                  ss.setName(s.getName());
                  ss.setPSRelatedItem(s.getPSRelatedItem());
                  slots.add(ss);
               }
               PSItemConverterUtils.toServerRelatedContent(dest, slots, this);
            }

            // convert folders (support array or List depending on DTO shape)
            Object foldersObj = orig.getFolders();
            if (foldersObj != null)
            {
               List<PSItemFolders> folderList = null;
               if (foldersObj instanceof PSItemFolders[])
               {
                  folderList = Arrays.asList((PSItemFolders[]) foldersObj);
               }
               else if (foldersObj instanceof List)
               {
                  @SuppressWarnings("unchecked")
                  List<PSItemFolders> tmp = (List<PSItemFolders>) foldersObj;
                  folderList = tmp;
               }

               if (folderList != null && !folderList.isEmpty())
                  dest.setFolderPaths(PSItemConverterUtils.toServerFolders(
                        folderList));
            }

            return dest;
         }
         else
         {
            PSCoreItem orig = (PSCoreItem) value;

            PSLegacyGuid guid = new PSLegacyGuid(orig.getContentId(),
               orig.getRevision());

            String contentType = orig.getItemDefinition().getName();

            PSItem dest = new PSItem();
            dest.setId(guid.longValue());
            dest.setContentType(contentType);
            if (orig.getSystemLocale() != null)
               dest.setSystemLocale(orig.getSystemLocale().getLanguage());
            if (orig.getDataLocale() != null)
               dest.setDataLocale(orig.getDataLocale().getLanguage());
            dest.setCheckedOutBy(orig.getCheckedOutByName());

            // convert fields
            try {
               com.percussion.webservices.content.PSItem.Fields fwrap = new com.percussion.webservices.content.PSItem.Fields();
               com.percussion.webservices.content.PSField[] farr = PSItemConverterUtils.toClientFields(
                  orig.getAllFields(), contentType, this);
               java.util.Collections.addAll(fwrap.getPSField(), farr);
               dest.setFields(fwrap);
            } catch (Exception e) {
               throw new ConversionException(e.getLocalizedMessage());
            }

            // convert children
            try {
               com.percussion.webservices.content.PSItemChildren[] carr = PSItemConverterUtils.toClientChildren(
                  orig.getAllChildren(), this);
               for (com.percussion.webservices.content.PSItemChildren top : carr) {
                  com.percussion.webservices.content.PSItem.Children c = new com.percussion.webservices.content.PSItem.Children();
                  c.setName(top.getName());
                  c.setDisplayName(top.getDisplayName());
                  c.setSequenced(top.getSequenced());
                  c.getPSChildEntry().addAll(top.getPSChildEntry());
                  dest.getChildren().add(c);
               }
            } catch (Exception e) {
               throw new ConversionException(e.getLocalizedMessage());
            }

            // convert related content
            try {
               com.percussion.webservices.content.PSItemSlots[] sarr = PSItemConverterUtils.toClientRelatedContent(
                  orig.getAllRelatedItems(), this);
               for (com.percussion.webservices.content.PSItemSlots top : sarr) {
                  com.percussion.webservices.content.PSItem.Slots s = new com.percussion.webservices.content.PSItem.Slots();
                  s.setName(top.getName());
                  s.getPSRelatedItem().addAll(top.getPSRelatedItem());
                  dest.getSlots().add(s);
               }
            } catch (Exception e) {
               throw new ConversionException(e.getLocalizedMessage());
            }

            // convert folders
            try {
               com.percussion.webservices.content.PSItemFolders[] farr2 = PSItemConverterUtils.toClientFolders(
                  orig.getFolderPaths());
               for (com.percussion.webservices.content.PSItemFolders top : farr2) {
                  com.percussion.webservices.content.PSItem.Folders f = new com.percussion.webservices.content.PSItem.Folders();
                  f.setPath(top.getPath());
                  dest.getFolders().add(f);
               }
            } catch (Exception e) {
               throw new ConversionException(e.getLocalizedMessage());
            }

            return dest;
         }
      }
      catch (PSCmsException e)
      {
         throw new ConversionException(e.getLocalizedMessage());
      }
   }
}
