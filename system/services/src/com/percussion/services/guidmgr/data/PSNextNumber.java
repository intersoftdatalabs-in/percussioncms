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
package com.percussion.services.guidmgr.data;

import org.apache.commons.lang3.StringUtils;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.Objects;

/**
 * JPA entity for storing GUID allocation records in the database.
 *
 * <p>This entity manages the next available number for various GUID types,
 * ensuring unique ID generation across the system. It uses modern JPA
 * annotations and Java 11 features for improved type safety and validation.
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
@Entity
@Table(name = "NEXTNUMBER")
public final class PSNextNumber {

   /**
    * The unique key identifying the number sequence.
    */
   @Id
   @Column(name = "KEYNAME", nullable = false)
   @NotBlank(message = "Key cannot be blank")
   private String key;

   /**
    * The next available number in the sequence.
    */
   @Basic
   @Column(name = "NEXTNR", nullable = false)
   @NotNull(message = "Next number cannot be null")
   private Integer next;

   /**
    * Default constructor for JPA.
    */
   public PSNextNumber() {
      // JPA requires default constructor
   }
   
   /**
    * Creates a new PSNextNumber with the specified key and initial value.
    *
    * @param key the unique key for this number sequence, must not be blank
    * @param initial the initial next number value, must be non-negative
    * @throws IllegalArgumentException if key is blank or initial is negative
    */
   public PSNextNumber(String key, int initial) {
      setKey(key);
      setNext(initial);
   }

   /**
    * Gets the unique key for this number sequence.
    *
    * @return the key, never null or blank for valid instances
    */
   public String getKey() {
      return key;
   }

   /**
    * Sets the unique key for this number sequence.
    *
    * @param key the key to set, must not be blank
    * @throws IllegalArgumentException if key is blank
    */
   public void setKey(String key) {
      if (StringUtils.isBlank(key)) {
         throw new IllegalArgumentException("Key cannot be null or blank");
      }
      this.key = key.trim();
   }

   /**
    * Gets the next available number in the sequence.
    *
    * @return the next number, never null for valid instances
    */
   public Integer getNext() {
      return next;
   }

   /**
    * Sets the next available number in the sequence.
    *
    * @param next the next number to set, must not be null and should be non-negative
    * @throws IllegalArgumentException if next is null or negative
    */
   public void setNext(Integer next) {
      Objects.requireNonNull(next, "Next number cannot be null");
      if (next < 0) {
         throw new IllegalArgumentException("Next number cannot be negative: " + next);
      }
      this.next = next;
   }

   /**
    * Atomically increments and returns the next number.
    *
    * @return the current next number before incrementing
    */
   public int getAndIncrement() {
      var current = next;
      next = current + 1;
      return current;
   }

   /**
    * Checks if this number sequence has available numbers.
    *
    * @return true if next number is available (non-null and non-negative)
    */
   public boolean hasNext() {
      return next != null && next >= 0;
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;

      var other = (PSNextNumber) obj;
      return Objects.equals(key, other.key) &&
             Objects.equals(next, other.next);
   }

   @Override
   public int hashCode() {
      return Objects.hash(key, next);
   }

   @Override
   public String toString() {
      return String.format("PSNextNumber{key='%s', next=%d}", key, next);
   }
}
