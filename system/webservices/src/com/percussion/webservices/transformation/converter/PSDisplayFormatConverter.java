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
import com.percussion.cms.objectstore.PSDFColumns;
import com.percussion.cms.objectstore.PSDFMultiProperty;
import com.percussion.cms.objectstore.PSDisplayColumn;
import com.percussion.cms.objectstore.PSDisplayFormat;
import com.percussion.cms.objectstore.PSKey;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.ui.data.CommunityRef;
import com.percussion.webservices.ui.data.Property; // WS DTOs (use fully qualified names where necessary) 
import org.apache.axis.types.UnsignedInt;
import org.apache.commons.beanutils.BeanUtilsBean;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Converts objects between the classes
 * <code>com.percussion.i18n.PSLocale</code> and
 * <code>com.percussion.webservices.content.data.PSLocale</code>.
 */
public class PSDisplayFormatConverter extends PSConverter
{

   /* (non-Javadoc)
    * @see PSConverter#PSConvert(BeanUtilsUtil)
    */
   public PSDisplayFormatConverter(BeanUtilsBean beanUtils)
   {
      super(beanUtils);
   }

   /* (non-Javadoc)
    * @see PSConverter#convert(Class, Object)
    */
   @Override
   public Object convert(Class type, Object value)
   {
      if (value == null)
         return null;

      if (isClientToServer(value))
      {
         com.percussion.webservices.ui.data.PSDisplayFormat source =
            (com.percussion.webservices.ui.data.PSDisplayFormat) value;

         PSDisplayFormat target;
         try
         {
            target = new PSDisplayFormat();
            PSDesignGuid guid = new PSDesignGuid(source.getId());
            String[] key = new String[]{String.valueOf(guid.longValue())};
            PSKey locator = PSDisplayFormat.createKey(key);
            locator.setPersisted(false);
            target.setLocator(locator);
            
            target.setInternalName(source.getName());
            target.setDisplayName(source.getLabel());
            target.setDescription(source.getDescription());
            
            PSDFColumns cols = getColumns(source.getColumns(), guid.longValue());
            target.setColumnList(cols);
            setProperties(target, source);
         }
         catch (Exception e)
         {
            log.error(PSExceptionUtils.getMessageForLog(e));
            throw new RuntimeException(e);
         }
         
         return target;
      }
      else // convert from objectstore to webservice
      {
         PSDisplayFormat source = (PSDisplayFormat) value;
         // Build WS display format using generated nested types
         com.percussion.webservices.ui.data.PSDisplayFormat ws = new com.percussion.webservices.ui.data.PSDisplayFormat();
         // Columns
         com.percussion.webservices.ui.data.PSDisplayFormat.Columns cols = new com.percussion.webservices.ui.data.PSDisplayFormat.Columns();
         for (int i=0;i<source.getColumnContainer().size();i++) {
            PSDisplayColumn srcCol = (PSDisplayColumn) source.getColumnContainer().get(i);
            com.percussion.webservices.ui.data.PSDisplayFormat.Columns.Column c = new com.percussion.webservices.ui.data.PSDisplayFormat.Columns.Column();
            c.setName(srcCol.getSource());
            c.setLabel(srcCol.getDisplayName());
            c.setDescription(srcCol.getDescription());
            c.setCategory(srcCol.isCategorized());
            c.setDefaultSortColumn(srcCol.getSource().equalsIgnoreCase(source.getSortedColumnName()));
            // renderType and sortOrder are strings in generated classes
            if (srcCol.isNumberType()) c.setRenderType("number");
            else if (srcCol.isImageType()) c.setRenderType("image");
            else if (srcCol.isDateType()) c.setRenderType("date");
            else c.setRenderType("text");
            c.setSortOrder(srcCol.isAscendingSort() ? "ascending" : "descending");
            c.setSequence((long) srcCol.getPosition());
            c.setWidth(srcCol.getWidth());
            cols.getColumn().add(c);
         }
         ws.setColumns(cols);
         // Communities
         if (source.getAllowedCommunities()!=null && !source.getAllowedCommunities().isEmpty()) {
            com.percussion.webservices.ui.data.PSDisplayFormat.Communities comm = new com.percussion.webservices.ui.data.PSDisplayFormat.Communities();
            for (Map.Entry<IPSGuid,String> entry : source.getAllowedCommunities().entrySet()) {
               CommunityRef cr = new CommunityRef();
               PSDesignGuid g = new PSDesignGuid(entry.getKey());
               cr.setId(g.getValue());
               cr.setName(entry.getValue());
               comm.getCommunityRef().add(cr);
            }
            ws.setCommunities(comm);
         }
         // Properties
         if (source.getProperties()!=null) {
            com.percussion.webservices.ui.data.PSDisplayFormat.Properties props = new com.percussion.webservices.ui.data.PSDisplayFormat.Properties();
            Iterator propsItr = source.getProperties();
            while (propsItr.hasNext()) {
               PSDFMultiProperty mp = (PSDFMultiProperty) propsItr.next();
               Property p = new Property();
               p.setName(mp.getName());
               if (mp.hasValues()) p.setValue((String) mp.iterator().next());
               props.getProperty().add(p);
            }
            ws.setProperties(props);
         }
         PSDesignGuid guid = new PSDesignGuid(source.getGUID());
         ws.setName(source.getInternalName());
         ws.setLabel(source.getDisplayName());
         ws.setDescription(source.getDescription());

         return ws;

      }
   }
   /**
    * Gets the webservice communities from objectstore instance. 
    * @param source the display format object, assumed not <code>null</code>.
    * @return the communties, never <code>null</code>, may be empty.
    */
   private Property[] getProperties(PSDisplayFormat source)
   {
      List<Property> resultList = new ArrayList<>();

      // handle all community property
      if (source.doesPropertyHaveValue(PSDisplayFormat.PROP_COMMUNITY,
            PSDisplayFormat.PROP_COMMUNITY_ALL))
      {
         resultList.add(new Property(
               PSDisplayFormat.PROP_COMMUNITY,
               PSDisplayFormat.PROP_COMMUNITY_ALL));
      }
      
      // handle the rest of the properties, except sys_community
      Iterator props = source.getProperties();
      PSDFMultiProperty srcProp;
      Property tgtProp;
      
      // get all properties, except the actual community ids
      while (props.hasNext())
      {
         srcProp = (PSDFMultiProperty) props.next();

         if (!PSDisplayFormat.PROP_COMMUNITY.equals(srcProp.getName()))
         {
            String propName = srcProp.getName();
            if (srcProp.hasValues())
            {
               Iterator valueItr = srcProp.iterator();
               while (valueItr.hasNext())
               {
                  tgtProp = new Property(propName, (String) valueItr.next());
                  resultList.add(tgtProp);
               }
            }
            else
            {
               tgtProp = new Property(propName, null);
               resultList.add(tgtProp);
            }
         }
      }
      
      Property[] result = new Property[resultList.size()];
      resultList.toArray(result);
      
      return result;
   }
   
   /**
    * Gets the webservice communities from objectstore instance. 
    * @param source the display format object, assumed not <code>null</code>.
    * @return the communties, never <code>null</code>, may be empty.
    */
   private CommunityRef[] getCommunities(PSDisplayFormat source)
   {
      CommunityRef[] result = new CommunityRef[0];
      Map<IPSGuid, String> communities = source.getAllowedCommunities(); 
      if (communities != null)
      {
         result = new CommunityRef[communities.size()];
         int i=0;
         for (Map.Entry<IPSGuid, String> community : communities.entrySet())
         {
            PSDesignGuid guid = new PSDesignGuid(community.getKey());
            result[i++] = new CommunityRef(
                  guid.getValue(), community.getValue());
         }
      }
      else // get ids only if any
      {
         if (!source.doesPropertyHaveValue(PSDisplayFormat.PROP_COMMUNITY,
               PSDisplayFormat.PROP_COMMUNITY_ALL))
         {
            Iterator props = source.getProperties();
            PSDFMultiProperty prop;

            while (props.hasNext())
            {
               prop = (PSDFMultiProperty) props.next();

               if (PSDisplayFormat.PROP_COMMUNITY.equals(prop.getName()))
               {
                  result = new CommunityRef[prop.size()];
                  Iterator values = prop.iterator();
                  String value;
                  int i=0;
                  while (values.hasNext())
                  {
                     value = (String) values.next();
                     long id = Long.parseLong(value);
                     PSDesignGuid guid = new PSDesignGuid(new PSGuid(
                           PSTypeEnum.COMMUNITY_DEF, id));
                     result[i++] = new CommunityRef(guid.getValue(), "");
                  }
                  break;
               }
            }
         }
      }
      return result;
   }
   /**
    * Converts the columns from objectstore type to webservice type.
    * 
    * @param cols the to be converted column data, assumed not <code>null</code>.
    * @param sortColumn the name of the sort column, assumed not 
    *    <code>null</code>.
    * 
    * @return the converted columns, never <code>null</code>, may be empty.
    */
   private com.percussion.webservices.ui.data.PSDisplayFormat.Columns getColumns(
         PSDFColumns cols, String sortColumn)
   {
      com.percussion.webservices.ui.data.PSDisplayFormat.Columns wsCols = new com.percussion.webservices.ui.data.PSDisplayFormat.Columns();
      if (cols == null) return wsCols;
      for (int i=0; i<cols.size(); i++) {
         PSDisplayColumn col = (PSDisplayColumn)cols.get(i);
         com.percussion.webservices.ui.data.PSDisplayFormat.Columns.Column wsCol = new com.percussion.webservices.ui.data.PSDisplayFormat.Columns.Column();
         wsCol.setName(col.getSource());
         wsCol.setLabel(col.getDisplayName());
         wsCol.setDescription(col.getDescription());
         wsCol.setCategory(col.isCategorized());
         wsCol.setDefaultSortColumn(col.getSource().equalsIgnoreCase(sortColumn));
         wsCol.setRenderType(col.isNumberType() ? "number" : (col.isImageType() ? "image" : (col.isDateType() ? "date" : "text")));
         wsCol.setSortOrder(col.isAscendingSort() ? "ascending" : "descending");
         wsCol.setSequence((long) col.getPosition());
         wsCol.setWidth(col.getWidth());
         wsCols.getColumn().add(wsCol);
      }
      return wsCols;
   }
   /**
    * Converts a list of WS columns to a list of objectstore columns
    * 
    * @param cols the WS columns, assume not <code>null</code>.
    * 
    * @return a list of objectstore columns, never <code>null</code>, but
    *    may be empty.
    *    
    * @throws PSCmsException if an error occurs during the convertion. 
    * @throws ClassNotFoundException if cannot find {@link PSDisplayColumn} 
    *    class. 
    */
   private PSDFColumns getColumns(
         com.percussion.webservices.ui.data.PSDisplayFormat.Columns cols, long displayId)
         throws PSCmsException, ClassNotFoundException 
   {
      PSDFColumns tgtCols = new PSDFColumns();
      if (cols == null || cols.getColumn() == null)
         return tgtCols;
      
      PSDisplayColumn tgtCol;
      boolean isAscendingSort;
      int groupType;
      for (com.percussion.webservices.ui.data.PSDisplayFormat.Columns.Column col : cols.getColumn())
      {
         PSKey key = PSDisplayColumn.createKey(col.getName(), displayId, false);
         isAscendingSort = "ascending".equalsIgnoreCase(col.getSortOrder());
         groupType = col.isCategory() ? PSDisplayColumn.GROUPING_CATEGORY
               : PSDisplayColumn.GROUPING_FLAT;
         
         tgtCol = new PSDisplayColumn(key);
               
         tgtCol.setDisplayName(col.getLabel());
         tgtCol.setDescription(col.getDescription());
         tgtCol.setSortOrder(isAscendingSort);
         tgtCol.setPosition((int) col.getSequence());
         tgtCol.setWidth(col.getWidth());
         tgtCol.setGroupingType(groupType);

         String dataType = col.getRenderType();
         if ("number".equalsIgnoreCase(dataType))
            dataType = PSDisplayColumn.DATATYPE_NUMBER;
         else if ("image".equalsIgnoreCase(dataType))
            dataType = PSDisplayColumn.DATATYPE_IMAGE;
         else if ("date".equalsIgnoreCase(dataType))
            dataType = PSDisplayColumn.DATATYPE_DATE;
         else
            dataType = PSDisplayColumn.DATATYPE_TEXT;
         tgtCol.setRenderType(dataType);

         tgtCols.add(tgtCol);
      }
      return tgtCols;
   }
   
   /**
    * Converts the properties from webservice (client) object to objectstore
    * object.
    * @param target the objectstore object, assumed not <code>null</code>.
    * @param source the webservice object, assumed not <code>null</code>.
    */
   private void setProperties(PSDisplayFormat target,
         com.percussion.webservices.ui.data.PSDisplayFormat source)
   {
      
      boolean allowAllCommunities = false;
      // get non PROP_COMMUNITY properties
      for (Property prop : source.getProperties())
      {
         if (prop.getName().equalsIgnoreCase(PSDisplayFormat.PROP_COMMUNITY)
               && prop.getValue().equals(PSDisplayFormat.PROP_COMMUNITY_ALL))
         {
            allowAllCommunities = true;
         }
         else
         {
            target.setProperty(prop.getName(), prop.getValue());
         }
      }
      
      // get PROP_COMMUNITY properties
      if (allowAllCommunities)
      {
         target.addCommunity(PSDisplayFormat.PROP_COMMUNITY_ALL);
      }
      else // get a list of community id(s), throw away name(s)
      {
         for (CommunityRef community : source.getCommunities())
         {
            PSDesignGuid guid = new PSDesignGuid(community.getId());
            target.addCommunity(String.valueOf(guid.longValue()));
         }
      }
   }
   
   /**
    * The string value for ascending sort order used in WS.
    */
   private static final String SORT_ORDER_ASCENDING = "ascending";
   private static final String SORT_ORDER_DESCENDING = "descending";
}
