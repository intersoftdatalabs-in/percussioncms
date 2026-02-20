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

import com.percussion.cms.objectstore.PSAaRelationship;
import com.percussion.design.objectstore.PSRelationship;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.guidmgr.data.PSGuid;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.system.utils.IPSHtmlParameters;
import com.percussion.webservices.common.Reference;
import com.percussion.webservices.content.PSAaRelationshipFolder;

import org.apache.axis.types.NonNegativeInteger;
import org.apache.commons.beanutils.BeanUtilsBean;

/**
 * Converts objects between the classes
 * {@link com.percussion.cms.objectstore.PSAaRelationship} and
 * {@link com.percussion.webservices.content.PSAaRelationship}
 */
public class PSAaRelationshipConverter extends PSRelationshipConverter
{
   /*
    * (non-Javadoc)
    *
    * @see PSConverter#PSConvert(BeanUtilsUtil)
    */
   public PSAaRelationshipConverter(BeanUtilsBean beanUtils)
   {
      super(beanUtils);
   }

   /*
    * (non-Javadoc)
    *
    * @see PSConverter#convert(Class, Object)
    */
   @SuppressWarnings("deprecation")
   @Override
   public Object convert(@SuppressWarnings("unused") Class type, Object value) {
      if (value == null)
         return null;

      if (isClientToServer(value))
      {
         // convert ID properties without reference info since the reference
         // info is transient data and will not be needed in the server
         // envirenment.
         com.percussion.webservices.content.PSAaRelationship source =
            (com.percussion.webservices.content.PSAaRelationship) value;

         PSRelationship origRel = getRelationshipFromClient(source);

         // set required ID properties
         // slot and template may be represented differently across generated DTOs; use reflection to support multiple shapes
         try {
            Object slotObj = null;
            try {
               slotObj = getBeanUtils().getPropertyUtils().getSimpleProperty(source, "slot");
            } catch (Exception e1) {
               // ignore if property is absent or shape differs
            }
            if (slotObj != null) {
               Object idVal = null;
               try {
                  idVal = getBeanUtils().getPropertyUtils().getSimpleProperty(slotObj, "id");
               } catch (Exception e2) {
                  // ignore missing id
               }
               if (idVal != null) {
                  long idLong = ((Number) idVal).longValue();
                  PSGuid slotId = new PSGuid(PSTypeEnum.SLOT, idLong);
                  origRel.setProperty(IPSHtmlParameters.SYS_SLOTID, String.valueOf(slotId.longValue()));
               }
            }
         } catch (Exception e) {
            throw new RuntimeException(e);
         }

         try {
            Object tplObj = null;
            try {
               tplObj = getBeanUtils().getPropertyUtils().getSimpleProperty(source, "template");
            } catch (Exception e1) {
               // ignore if property absent or shape differs
            }
            if (tplObj != null) {
               Object idVal = null;
               try {
                  idVal = getBeanUtils().getPropertyUtils().getSimpleProperty(tplObj, "id");
               } catch (Exception e2) {
                  // ignore missing id
               }
               if (idVal != null) {
                  long idLong = ((Number) idVal).longValue();
                  PSGuid templetId = new PSGuid(PSTypeEnum.TEMPLATE, idLong);
                  origRel.setProperty(IPSHtmlParameters.SYS_VARIANTID, String.valueOf(templetId.longValue()));
               }
            }
         } catch (Exception e) {
            throw new RuntimeException(e);
         }

         PSAaRelationship target = new PSAaRelationship(origRel);

         // set known properties
         // extract sortRank numeric value from the webservice source and set on the server target
         int sortInt = 0;
         try {
            Object sortVal = null;
            try {
               sortVal = getBeanUtils().getPropertyUtils().getSimpleProperty(source, "sortRank");
            } catch (Exception e1) {
               // ignore, property may not exist
            }
            if (sortVal != null) {
               java.math.BigInteger bi;
               if (sortVal instanceof jakarta.xml.bind.JAXBElement) {
                  bi = (java.math.BigInteger) ((jakarta.xml.bind.JAXBElement<?>) sortVal).getValue();
               } else if (sortVal instanceof javax.xml.bind.JAXBElement) {
                  bi = (java.math.BigInteger) ((javax.xml.bind.JAXBElement<?>) sortVal).getValue();
               } else if (sortVal instanceof java.math.BigInteger) {
                  bi = (java.math.BigInteger) sortVal;
               } else {
                  bi = new java.math.BigInteger(String.valueOf(sortVal));
               }
               sortInt = bi.intValue();
            }
         } catch (Exception e) {
            throw new RuntimeException(e);
         }
         target.setSortRank(sortInt);

         // site and folder may be represented as nested refs; use reflection
         try {
            Object siteObj = null;
            try {
               siteObj = getBeanUtils().getPropertyUtils().getSimpleProperty(source, "site");
            } catch (Exception e1) {
               // ignore if missing
            }
            if (siteObj != null) {
               Object idVal = null;
               try {
                  idVal = getBeanUtils().getPropertyUtils().getSimpleProperty(siteObj, "id");
               } catch (Exception e2) {
                  // ignore
               }
               if (idVal != null) {
                  PSGuid siteId = new PSGuid(PSTypeEnum.SITE, ((Number) idVal).longValue());
                  target.setSiteId(siteId);
               }
            }
         } catch (Exception e) {
            throw new RuntimeException(e);
         }

         try {
            Object folderObj = null;
            try {
               folderObj = getBeanUtils().getPropertyUtils().getSimpleProperty(source, "folder");
            } catch (Exception e1) {
               // ignore if missing
            }
            if (folderObj != null) {
               Object idVal = null;
               try {
                  idVal = getBeanUtils().getPropertyUtils().getSimpleProperty(folderObj, "id");
               } catch (Exception e2) {
                  // ignore
               }
               if (idVal != null) {
                  PSLegacyGuid folderId = new PSLegacyGuid(((Number) idVal).longValue());
                  target.setFolderId(folderId.getContentId());
               }
            }
         } catch (Exception e) {
            throw new RuntimeException(e);
         }

         return target;
      }
      else // convert from server to webservice
      {
         PSAaRelationship source = (PSAaRelationship) value;

         com.percussion.webservices.content.PSAaRelationship target =
            new com.percussion.webservices.content.PSAaRelationship();
         setRelationshipFromServer(source, target);

         // set known properties
         PSDesignGuid slotId = new PSDesignGuid(source.getSlotId());
         Reference slot = new Reference();
         slot.setId(slotId.getValue());
         slot.setName(source.getSlotName());
         target.setSlot(slot);

         PSDesignGuid templateId = new PSDesignGuid(source.getTemplateId());
         Reference template = new Reference();
         template.setId(templateId.getValue());
         template.setName(source.getTemplateName());
         target.setTemplate(template);

         // convert server int sort rank to webservice JAXBElement<BigInteger>
         target.setSortRank(new jakarta.xml.bind.JAXBElement<java.math.BigInteger>(
            new javax.xml.namespace.QName("", "sortRank"), java.math.BigInteger.class, java.math.BigInteger.valueOf(source.getSortRank())));

         Reference site = null;
         if (source.getSiteId() != null)
         {
            site = new Reference();
            site.setId(source.getSiteId().longValue());
            site.setName(source.getSiteName());
            target.setSite(site);
         }
         if (source.getFolderId() != -1)
         {
            PSLegacyGuid folderGuid = new PSLegacyGuid(source.getFolderId(),-1);
            com.percussion.webservices.content.PSAaRelationship.Folder folder = new com.percussion.webservices.content.PSAaRelationship.Folder();
            folder.setId(new PSDesignGuid(folderGuid).longValue());
            folder.setName(source.getFolderName());
            folder.setPath(source.getFolderPath());
            target.setFolder(folder);
         }

         return target;
      }
   }
}
