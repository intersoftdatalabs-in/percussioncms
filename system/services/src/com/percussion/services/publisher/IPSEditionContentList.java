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
package com.percussion.services.publisher;

import com.percussion.services.catalog.IPSCatalogIdentifier;
import com.percussion.utils.guid.IPSGuid;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents the association between an edition and a content list with
 * delivery and assembly context configuration. This interface provides
 * modern Java 11 patterns for edition-content list management with
 * enhanced validation and safe access methods.
 */
public interface IPSEditionContentList extends IPSCatalogIdentifier {

   /**
    * Gets the Edition ID.
    *
    * @return the Edition ID, never {@code null}
    */
   IPSGuid getEditionId();
   
   /**
    * Gets the Content List ID.
    *
    * @return the Content List ID, never {@code null}
    */
   IPSGuid getContentListId();

   /**
    * Gets the authentication type.
    *
    * @return the authentication type, may be {@code null}
    */
   Integer getAuthtype();

   /**
    * Gets the authentication type safely with Optional wrapper.
    *
    * @return Optional containing the auth type, or empty if not set
    */
   default Optional<Integer> getAuthtypeSafely() {
      return Optional.ofNullable(getAuthtype());
   }

   /**
    * Sets the authentication type.
    *
    * @param authtype the authentication type to set, may be {@code null}
    */
   void setAuthtype(Integer authtype);

   /**
    * Gets the delivery context ID.
    *
    * @return the delivery context ID, never {@code null}
    */
   IPSGuid getDeliveryContextId();

   /**
    * Sets the ID of the delivery context.
    *
    * @param context the new context ID, never {@code null}
    * @throws IllegalArgumentException if context is null
    */
   void setDeliveryContextId(IPSGuid context);
   
   /**
    * Gets the assembly context ID.
    *
    * @return the context ID, may be {@code null} if not defined
    */
   IPSGuid getAssemblyContextId();

   /**
    * Gets the assembly context ID safely with Optional wrapper.
    *
    * @return Optional containing the assembly context ID, or empty if not set
    */
   default Optional<IPSGuid> getAssemblyContextIdSafely() {
      return Optional.ofNullable(getAssemblyContextId());
   }

   /**
    * Sets the assembly context ID.
    *
    * @param context the new context ID, may be {@code null}
    */
   void setAssemblyContextId(IPSGuid context);   

   /**
    * Gets the sequence number for ordering content lists within an edition.
    *
    * @return the sequence number, may be {@code null}
    */
   Integer getSequence();

   /**
    * Gets the sequence number safely with Optional wrapper.
    *
    * @return Optional containing the sequence number, or empty if not set
    */
   default Optional<Integer> getSequenceSafely() {
      return Optional.ofNullable(getSequence());
   }

   /**
    * Sets the sequence number for ordering content lists within an edition.
    *
    * @param seq the sequence number to set, may be {@code null}
    */
   void setSequence(Integer seq);

   /**
    * Checks if this edition-content list association has both delivery and assembly contexts.
    *
    * @return true if both contexts are configured
    */
   default boolean hasFullContext() {
      return getDeliveryContextId() != null && getAssemblyContextId() != null;
   }

   /**
    * Checks if this association has a valid sequence for ordering.
    *
    * @return true if sequence is set and non-negative
    */
   default boolean hasValidSequence() {
      return getSequenceSafely()
         .map(seq -> seq >= 0)
         .orElse(false);
   }

   /**
    * Validates the required fields for this edition-content list association.
    *
    * @return true if all required fields are properly set
    */
   default boolean isValid() {
      return getEditionId() != null &&
             getContentListId() != null &&
             getDeliveryContextId() != null;
   }

   /**
    * Compares this edition-content list with another for ordering by sequence.
    *
    * @param other the other edition-content list to compare to
    * @return comparison result based on sequence, then by content list ID
    */
   default int compareBySequence(IPSEditionContentList other) {
      Objects.requireNonNull(other, "Other edition-content list cannot be null");

      var thisSeq = getSequenceSafely().orElse(Integer.MAX_VALUE);
      var otherSeq = other.getSequenceSafely().orElse(Integer.MAX_VALUE);

      var seqComparison = Integer.compare(thisSeq, otherSeq);
      if (seqComparison != 0) {
         return seqComparison;
      }

      // If sequences are equal, compare by content list ID for stable ordering
      return getContentListId().compareTo(other.getContentListId());
   }

   @Override
   boolean equals(Object b);

   @Override
   int hashCode();
}
