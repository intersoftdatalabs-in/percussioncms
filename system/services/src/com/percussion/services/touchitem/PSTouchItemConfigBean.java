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
package com.percussion.services.touchitem;

import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Encapsulates a single touch item configuration which consists
 * of source and target content types, level (folder) value, and
 * flag to indicate if AA parents should be touched.
 * <p>
 * This configuration bean is used to determine which items should be
 * touched when relationship changes occur, particularly for incremental
 * publishing scenarios.
 *
 * @author peterfrontiero
 */
public class PSTouchItemConfigBean {

   /**
    * Gets the source content type names.
    *
    * @return the source content type names, never {@code null}
    */
   public Set<String> getSourceTypes() {
      return sourceTypes;
   }

   /**
    * Sets the source content type names.
    *
    * @param sourceTypes the source content type names, may be {@code null}
    */
   public void setSourceTypes(Set<String> sourceTypes) {
      this.sourceTypes = sourceTypes != null ?
         ConcurrentHashMap.newKeySet(sourceTypes.size()) :
         ConcurrentHashMap.newKeySet();
      if (sourceTypes != null) {
         this.sourceTypes.addAll(sourceTypes);
      }
   }

   /**
    * Gets the target content type names.
    *
    * @return the target content type names, never {@code null}
    */
   public Set<String> getTargetTypes() {
      return targetTypes;
   }

   /**
    * Sets the target content type names.
    *
    * @param targetTypes the target content type names, may be {@code null}
    */
   public void setTargetTypes(Set<String> targetTypes) {
      this.targetTypes = targetTypes != null ?
         ConcurrentHashMap.newKeySet(targetTypes.size()) :
         ConcurrentHashMap.newKeySet();
      if (targetTypes != null) {
         this.targetTypes.addAll(targetTypes);
      }
   }

   /**
    * Gets the level which indicates the folder relative to the current
    * item's folder in which target items will be touched.
    *
    * @return the folder level
    */
   public int getLevel() {
      return level;
   }

   /**
    * Sets the folder level for touching items.
    *
    * @param level the level to set
    */
   public void setLevel(int level) {
      this.level = level;
   }

   /**
    * Checks if direct AA parents should be touched.
    *
    * @return {@code true} to touch direct AA parents of the items,
    *         {@code false} otherwise
    */
   public boolean isTouchAAParents() {
      return touchAAParents;
   }

   /**
    * Sets whether to touch direct AA parents.
    *
    * @param touchAAParents {@code true} to touch direct AA parents of the items,
    *                       {@code false} otherwise
    */
   public void setTouchAAParents(boolean touchAAParents) {
      this.touchAAParents = touchAAParents;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
         return false;
      }

      var other = (PSTouchItemConfigBean) obj;
      return level == other.level &&
             touchAAParents == other.touchAAParents &&
             Objects.equals(sourceTypes, other.sourceTypes) &&
             Objects.equals(targetTypes, other.targetTypes);
   }

   @Override
   public int hashCode() {
      return Objects.hash(sourceTypes, targetTypes, level, touchAAParents);
   }

   @Override
   public String toString() {
      return String.format("PSTouchItemConfigBean{sourceTypes=%s, targetTypes=%s, level=%d, touchAAParents=%s}",
                          sourceTypes, targetTypes, level, touchAAParents);
   }

   /**
    * The source content type names. Never {@code null} after initialization.
    */
   private Set<String> sourceTypes = ConcurrentHashMap.newKeySet();

   /**
    * The target content type names. Never {@code null} after initialization.
    */
   private Set<String> targetTypes = ConcurrentHashMap.newKeySet();

   /**
    * The folder level relative to the current item's folder.
    */
   private int level;

   /**
    * Flag indicating whether to touch direct AA parents.
    */
   private boolean touchAAParents;
}
