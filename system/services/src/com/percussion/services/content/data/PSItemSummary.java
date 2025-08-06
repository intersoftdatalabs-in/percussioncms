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
// REFACTORED: Updated for Java 11 - modernized imports, validation, var usage, and string handling
package com.percussion.services.content.data;

import com.percussion.cms.objectstore.PSFolder;
import com.percussion.services.guidmgr.data.PSLegacyGuid;
import com.percussion.utils.guid.IPSGuid;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Objects;
import java.util.StringJoiner;

import org.apache.commons.lang3.StringUtils;

/**
 * Represents a content object which is currently an item or folder.
 * This class provides a summary view of content items and folders
 * with their basic properties and allowed operations.
 */
public class PSItemSummary
{
   /**
    * The content id.
    */
   protected int contentId;
   
   /**
    * The revision id.
    */
   protected int revision;
   
   /**
    * Revision lock of the item.
    */
   protected boolean revisionLock = false;
   
   /**
    * The item name, the {@code sys_title} for items, the folder name for
    * folders, never {@code null} or empty.
    */
   protected String name;
   
   /**
    * The content type id.
    */
   protected int contentTypeId;
   
   /**
    * The content type name, never {@code null} or empty.
    */
   protected String contentTypeName;
   
   /**
    * The object type, never {@code null}.
    */
   protected ObjectTypeEnum objectType = ObjectTypeEnum.ITEM;
   
   /**
    * The allowed operations for this item, never {@code null}, may be
    * empty.
    */
   protected Collection<OperationEnum> operations = new ArrayList<>();

   /**
    * Use this constructor to create an item.
    * 
    * @param contentId the content id
    * @param revision the revision id
    * @param name the item name, not {@code null} or empty
    * @param contentTypeId the items content type id
    * @param contentTypeName the items content type name, not
    *    {@code null} or empty
    * @param revisionLock if the content item is revisionable
    */
   public PSItemSummary(int contentId, int revision, String name, 
      int contentTypeId, String contentTypeName, boolean revisionLock)
   {
      setGUID(new PSLegacyGuid(contentId, revision));
      setName(name);
      setContentTypeId(contentTypeId);
      setContentTypeName(contentTypeName);
      if (contentTypeId == PSFolder.FOLDER_CONTENT_TYPE_ID) 
      {
         setRevisionLock(false);
         setObjectType(ObjectTypeEnum.FOLDER);
      }
      else 
      {
         setRevisionLock(revisionLock);
         setObjectType(ObjectTypeEnum.ITEM);
      }
   }
   
   /**
    * Use this constructor to create a folder.
    * 
    * @param contentId the folder id
    * @param name the folder name, not {@code null} or empty
    */
   public PSItemSummary(int contentId, String name)
   {
      setGUID(new PSLegacyGuid(contentId, -1));
      setName(name);
      setContentTypeId(PSFolder.FOLDER_CONTENT_TYPE_ID);
      setContentTypeName("Folder");
      setObjectType(ObjectTypeEnum.FOLDER);
      setRevisionLock(false);
   }

   /**
    * Default constructor. Should only be used by webservice converters.
    */
   public PSItemSummary()
   {
      // Default constructor for webservice converters
   }
   
   /**
    * Get the item id.
    * 
    * @return the item id, never {@code null}
    */
   public IPSGuid getGUID()
   {
      return new PSLegacyGuid(contentId, revision);
   }
   
   /**
    * Set a new item id.
    * 
    * @param id the id, must be an instanceof {@code PSLegacyGuid}
    * @throws IllegalArgumentException if id is not an instance of PSLegacyGuid
    */
   public void setGUID(IPSGuid id)
   {
      if (!(id instanceof PSLegacyGuid))
         throw new IllegalArgumentException("id must be an instanceof PSLegacyGuid");

      var guid = (PSLegacyGuid) id;
      this.contentId = guid.getContentId();
      this.revision = guid.getRevision();
   }
   
   /**
    * Get the content type id of this item.
    * 
    * @return the content type id
    */
   public int getContentTypeId()
   {
      return contentTypeId;
   }
   
   /**
    * Set a new content type id.
    * 
    * @param id the new content type id
    */
   public void setContentTypeId(int id)
   {
      this.contentTypeId = id;
   }
   
   /**
    * Get the item name.
    * 
    * @return the item name, never {@code null} or empty
    */
   public String getName()
   {
      return name;
   }
   
   /**
    * Set a new item name.
    * 
    * @param name the new name, not {@code null} or empty
    * @throws IllegalArgumentException if name is {@code null} or empty
    */
   public void setName(String name)
   {
      if (StringUtils.isBlank(name))
         throw new IllegalArgumentException("name cannot be null or empty");
      
      this.name = name;
   }
   
   /**
    * Get the content type name.
    * 
    * @return the content type name, never {@code null} or empty
    */
   public String getContentTypeName()
   {
      return contentTypeName;
   }
   
   /**
    * Set a new content type name.
    * 
    * @param newName the new content type name, not {@code null} or empty
    * @throws IllegalArgumentException if newName is {@code null}
    */
   public void setContentTypeName(String newName)
   {
      if (newName == null)
         throw new IllegalArgumentException("newName cannot be null");
      
      this.contentTypeName = newName;
   }

   /**
    * If the item has revision lock turned on.
    *
    * @return {@code true} if revision lock is enabled, {@code false} otherwise
    */
   public boolean isRevisionLock()
   {
      return revisionLock;
   }

   /**
    * Set the revision lock status.
    *
    * @param revisionLock the revision lock status
    */
   public void setRevisionLock(boolean revisionLock)
   {
      this.revisionLock = revisionLock;
   }

   /**
    * Get the type of this object.
    * 
    * @return the object type, never {@code null}
    */
   public ObjectTypeEnum getObjectType()
   {
      return objectType;
   }
   
   /**
    * Set a new object type.
    * 
    * @param type the new object type, not {@code null}
    * @throws IllegalArgumentException if type is {@code null}
    */
   public void setObjectType(ObjectTypeEnum type)
   {
      if (type == null)
         throw new IllegalArgumentException("type cannot be null");
      
      this.objectType = type;
   }
   
   /**
    * Get all allowed operations for this object.
    * 
    * @return the allowed operations, never {@code null}, may be empty
    */
   public Collection<OperationEnum> getOperations()
   {
      return operations;
   }
   
   /**
    * Set new allowed operations.
    * 
    * @param operations the new operations, not {@code null}, may be empty
    * @throws IllegalArgumentException if operations is {@code null}
    */
   public void setOperations(Collection<OperationEnum> operations)
   {
      if (operations == null)
         throw new IllegalArgumentException("operations cannot be null");
      
      this.operations = operations;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PSItemSummary)) return false;
      var that = (PSItemSummary) o;
      return contentId == that.contentId &&
             revision == that.revision &&
             isRevisionLock() == that.isRevisionLock() &&
             getContentTypeId() == that.getContentTypeId() &&
             Objects.equals(getName(), that.getName()) &&
             Objects.equals(getContentTypeName(), that.getContentTypeName()) &&
             getObjectType() == that.getObjectType() &&
             Objects.equals(getOperations(), that.getOperations());
   }

   @Override
   public int hashCode() {
      return Objects.hash(contentId, revision, isRevisionLock(), getName(),
         getContentTypeId(), getContentTypeName(), getObjectType(), getOperations());
   }

   @Override
   public String toString() {
      return new StringJoiner(", ", PSItemSummary.class.getSimpleName() + "[", "]")
         .add("contentId=" + contentId)
         .add("revision=" + revision)
         .add("revisionLock=" + revisionLock)
         .add("name='" + name + "'")
         .add("contentTypeId=" + contentTypeId)
         .add("contentTypeName='" + contentTypeName + "'")
         .add("objectType=" + objectType)
         .add("operations=" + operations)
         .toString();
   }

   /**
    * The enumeration of all supported object types.
    */
   public enum ObjectTypeEnum
   {
      ITEM(1),
      FOLDER(2);
      
      private final int ordinal;

      /**
       * Constructs an enumeration for the specified ordinal.
       *
       * @param ordinal the enumeration ordinal
       */
      ObjectTypeEnum(int ordinal)
      {
         this.ordinal = ordinal;
      }

      /**
       * Get the ordinal of the enumeration.
       * 
       * @return the ordinal
       */
      public int getOrdinal()
      {
         return ordinal;
      }
      
      /**
       * Get the enumeration for the supplied ordinal.
       * 
       * @param ordinal the ordinal for which to get the enumeration
       * @return the enumeration, never {@code null}
       * @throws IllegalArgumentException if no enumeration exists for the
       *    supplied ordinal
       */
      public static ObjectTypeEnum valueOf(int ordinal)
      {
         for (var value : values())
            if (value.getOrdinal() == ordinal)
               return value;

         throw new IllegalArgumentException(
            "No object type is defined for the supplied ordinal.");
      }
   }
   
   /**
    * The enumeration of all supported item operations.
    */
   public enum OperationEnum
   {
      NONE(0),
      READ(1),
      WRITE(2),
      TRANSITION(3),
      CHECKIN(4),
      CHECKOUT(5);
      
      private final int ordinal;

      /**
       * Constructs an enumeration for the specified ordinal.
       *
       * @param ordinal the enumeration ordinal
       */
      OperationEnum(int ordinal)
      {
         this.ordinal = ordinal;
      }

      /**
       * Get the ordinal of the enumeration.
       * 
       * @return the ordinal
       */
      public int getOrdinal()
      {
         return ordinal;
      }
      
      /**
       * Get the enumeration for the supplied ordinal.
       * 
       * @param ordinal the ordinal for which to get the enumeration
       * @return the enumeration, never {@code null}
       * @throws IllegalArgumentException if no enumeration exists for the
       *    supplied ordinal
       */
      public static OperationEnum valueOf(int ordinal)
      {
         for (var value : values())
            if (value.getOrdinal() == ordinal)
               return value;

         throw new IllegalArgumentException(
            "No operation is defined for the supplied ordinal.");
      }
   }
}
