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
package com.percussion.services.assembly.data;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.percussion.services.utils.xml.PSXmlSerializationHelper;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.IPSXmlSerialization;
import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import java.io.Serializable;
import java.util.Objects;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import tools.jackson.dataformat.xml.annotation.JacksonXmlRootElement;

/**
 * Persist an association between a slot, template and content type.
 *
 * <p>Package/design nested element name is {@code slot-type-association} (registered on {@link
 * PSTemplateSlot}); standalone mapped type name is {@code template-type-slot-association}. Property
 * element names are hyphenated ({@code content-type-id}, {@code template-id}, {@code slot-id})
 * matching package-normalize rewrite and historical {@code PSTemplateTypeSlotAssociation.betwixt}
 * (issue #1891 / epic #505).
 *
 * @author dougrand
 */
@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "PSTemplateTypeSlotAssociation")
@Table(name = "RXSLOTCONTENT")
@JacksonXmlRootElement(localName = "slot-type-association")
@JsonAutoDetect(
    getterVisibility = JsonAutoDetect.Visibility.NONE,
    isGetterVisibility = JsonAutoDetect.Visibility.NONE,
    fieldVisibility = JsonAutoDetect.Visibility.NONE,
    setterVisibility = JsonAutoDetect.Visibility.PUBLIC_ONLY,
    creatorVisibility = JsonAutoDetect.Visibility.NONE)
@JsonPropertyOrder({"contentTypeId", "slotId", "templateId", "version"})
public class PSTemplateTypeSlotAssociation implements Serializable
{
   static
   {
      // Register types with XML serializer for read creation of objects
      PSXmlSerializationHelper.addType("template-type-slot-association",
            PSTemplateTypeSlotAssociation.class);
      // Nested under PSTemplateSlot as package/design item element name
      PSXmlSerializationHelper.addType("slot-type-association",
            PSTemplateTypeSlotAssociation.class);
   }

   /**
    * Serial id identifies versions of serialized data
    */
   private static final long serialVersionUID = 1L;



   @EmbeddedId
   PSTemplateTypeSlotAssociationPK id;

   @Column(name = "VERSION")
   Integer version = 0;


   /**
    * No args default ctor
    */
   public PSTemplateTypeSlotAssociation() {
   }

   /**
    * Ctor
    * 
    * @param ctype the content type guid, never <code>null</code>
    * @param template the template guid, never <code>null</code>
    * @param slotId the slot id
    */
   public PSTemplateTypeSlotAssociation(IPSGuid ctype, IPSGuid template,
                                        long slotId) {
      if (ctype == null)
      {
         throw new IllegalArgumentException("ctype may not be null");
      }
      if (template == null)
      {
         throw new IllegalArgumentException("template may not be null");
      }
      id = new PSTemplateTypeSlotAssociationPK(template.longValue(), ctype.longValue(), slotId);
   }


   /**
    * Hibernate embedded id. Suppressed from Betwixt/Jackson so package XML attributes like {@code
    * id="2"} (Betwixt object identity) are not mapped onto this PK — that left content/template
    * ids at 0 and broke perc.nav slot deploy (ContentType source ID 0).
    */
   @IPSXmlSerialization(suppress = true)
   @JsonIgnore
   public PSTemplateTypeSlotAssociationPK getId() {
      return id;
   }

   @JsonIgnore
   public void setId(PSTemplateTypeSlotAssociationPK id) {
      this.id = id;
   }

   /**
    * @return Returns the contentTypeId.
    */
   @JsonProperty
   public long getContentTypeId()
   {
      if (id != null)
         return id.getContentTypeId();
      else
         return 0;
   }

   /**
    * @param contentTypeId The contentTypeId to set.
    */
   public void setContentTypeId(long contentTypeId)
   {
      if (id == null)
      {
         id = new PSTemplateTypeSlotAssociationPK();
      }
      id.setContentTypeId(contentTypeId);
   }

   /**
    * @return Returns the slotId.
    */
   @JsonProperty
   public long getSlotId()
   {
      if (id != null)
         return id.getSlotId();
      else
         return 0;
   }

   /**
    * @param slotId The slotId to set.
    */
   public void setSlotId(long slotId)
   {

      if (id == null)
      {
         id = new PSTemplateTypeSlotAssociationPK();
      }
      id.setSlotId(slotId);
   }


   /**
    * @return Returns the templateId.
    */
   @JsonProperty
   public long getTemplateId()
   {
      if (id != null)
         return id.getTemplateId();
      else
         return 0;
   }

   /**
    * @param templateId The templateId to set.
    */
   public void setTemplateId(long templateId)
   {
      if (id == null)
      {
         id = new PSTemplateTypeSlotAssociationPK();
      }
      id.setTemplateId(templateId);
   }
   /**
    * @return Returns the version.
    */
   @JsonProperty
   public Integer getVersion()
   {
      return version;
   }

   /**
    * @param version The version to set.
    */
   public void setVersion(Integer version)
   {
      this.version = version;
   }


   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PSTemplateTypeSlotAssociation)) return false;
      PSTemplateTypeSlotAssociation that = (PSTemplateTypeSlotAssociation) o;
      return Objects.equals(id, that.id);
   }

   @Override
   public int hashCode() {
      return Objects.hash(id);
   }
}
