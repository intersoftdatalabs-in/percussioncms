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
package com.percussion.services.filter;

import com.percussion.services.data.IPSIdentifiableItem;
import com.percussion.utils.guid.IPSGuid;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents an item to be filtered. Items are considered immutable in filter
 * rules. If a modification is required, clone the item and modify the clone.
 * This interface provides safe access methods and enhanced validation for
 * filter item operations.
 *
 * @author dougrand
 */
public interface IPSFilterItem extends Cloneable, IPSIdentifiableItem {

   /**
    * Get the item's identifying folder GUID.
    *
    * @return the item's folder GUID, may be {@code null}
    */
   IPSGuid getFolderId();

   /**
    * Get the item's identifying folder GUID safely.
    *
    * @return Optional containing the folder GUID, or empty if not set
    */
   default Optional<IPSGuid> getFolderIdSafely() {
      return Optional.ofNullable(getFolderId());
   }

   /**
    * Get the item's containing site GUID.
    *
    * @return the item's site GUID, may be {@code null}
    */
   IPSGuid getSiteId();

   /**
    * Get the item's containing site GUID safely.
    *
    * @return Optional containing the site GUID, or empty if not set
    */
   default Optional<IPSGuid> getSiteIdSafely() {
      return Optional.ofNullable(getSiteId());
   }

   /**
    * A number of operations require deciding if a filter item is in a set or
    * map. But the items are mutable, which can render these maps and sets
    * invalid. This method returns an opaque key that can be used for this 
    * purpose. The returned key is not dependent on the specific revision
    * of the item, but will be dependent on the folder and site.
    * <p>
    * Note to implementers, do not use an array for this purpose. Java does not
    * hash arrays, it uses the default hash code calculation. This will not
    * work properly.
    * 
    * @return an opaque key, never {@code null}, that identifies the
    * tuple content item id, folder id and site id.
    */
   Object getKey();

   /**
    * Get the key safely with validation.
    *
    * @return Optional containing the key, guaranteed non-null if present
    */
   default Optional<Object> getKeySafely() {
      try {
         return Optional.ofNullable(getKey());
      } catch (Exception e) {
         return Optional.empty();
      }
   }

   /**
    * Make a clone and replace the item id. This is provided to enable 
    * implementers who need to return a modified item in the result set of
    * a filter to create a clone to modify. The original item should be
    * considered immutable. The returned object will not be equals to the 
    * original object (unless the new id happens to be the same).
    * <p>
    * Note that the general clone method is not guaranteed to be present.
    * 
    * @param newItemId the new item id, never {@code null}
    * @return the cloned and modified object, never {@code null}
    * @throws IllegalArgumentException if newItemId is null
    */
   IPSFilterItem clone(IPSGuid newItemId);

   /**
    * Make a clone safely with validation and error handling.
    *
    * @param newItemId the new item id, never {@code null}
    * @return Optional containing the cloned item, or empty if cloning fails
    */
   default Optional<IPSFilterItem> cloneSafely(IPSGuid newItemId) {
      try {
         Objects.requireNonNull(newItemId, "New item ID cannot be null");
         return Optional.ofNullable(clone(newItemId));
      } catch (Exception e) {
         return Optional.empty();
      }
   }

   /**
    * Set a new item id, this must only be called on a cloned object.
    * 
    * @param newId the new item id, never {@code null}
    * @throws IllegalArgumentException if newId is null
    */
   void setItemId(IPSGuid newId);

   /**
    * Set a new item id safely with validation.
    *
    * @param newId the new item id, never {@code null}
    * @return true if the id was set successfully, false otherwise
    */
   default boolean setItemIdSafely(IPSGuid newId) {
      try {
         Objects.requireNonNull(newId, "New item ID cannot be null");
         setItemId(newId);
         return true;
      } catch (Exception e) {
         return false;
      }
   }

   /**
    * Check if this filter item has a folder assignment.
    *
    * @return true if folder ID is present and valid
    */
   default boolean hasFolder() {
      return getFolderIdSafely().isPresent();
   }

   /**
    * Check if this filter item has a site assignment.
    *
    * @return true if site ID is present and valid
    */
   default boolean hasSite() {
      return getSiteIdSafely().isPresent();
   }

   /**
    * Check if this filter item is fully specified with folder and site.
    *
    * @return true if both folder and site IDs are present
    */
   default boolean isFullySpecified() {
      return hasFolder() && hasSite();
   }
}
