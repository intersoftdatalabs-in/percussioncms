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
// REFACTORED: Updated for Java 11 - modernized imports, validation, and string handling
package com.percussion.services.content.data;

import java.io.Serializable;
import java.util.Objects;
import java.util.StringJoiner;

import javax.persistence.Column;
import javax.persistence.Embeddable;

import org.apache.commons.lang3.StringUtils;

/**
 * Primary key for the {@link PSAutoTranslation}.
 * This class represents the composite key for auto translation records.
 */
@Embeddable
public class PSAutoTranslationPK implements Serializable
{
   private static final long serialVersionUID = 1L;

   @Column(name = "CONTENTTYPEID", nullable = false)
   private long contentTypeId;
   
   @Column(name = "LOCALE", nullable = false)
   private String locale;
   
   /**
    * Default constructor for JPA.
    */
   public PSAutoTranslationPK()
   {
      // Default constructor for JPA
   }
   
   /**
    * Constructs a primary key with the specified content type ID and locale.
    *
    * @param cTypeId The content type id
    * @param lang The locale's language string, may not be {@code null} or empty
    * @throws IllegalArgumentException if lang is {@code null} or empty
    */
   public PSAutoTranslationPK(long cTypeId, String lang)
   {
      if (StringUtils.isBlank(lang))
         throw new IllegalArgumentException("lang may not be null or empty");
      
      this.contentTypeId = cTypeId;
      this.locale = lang;
   }

   /**
    * Gets the content type id of this auto translation.
    *
    * @return The content type id
    */
   public long getContentTypeId()
   {
      return contentTypeId;
   }

   /**
    * Sets the content type id of this auto translation.
    *
    * @param id The content type id
    */
   public void setContentTypeId(long id)
   {
      this.contentTypeId = id;
   }

   /**
    * Gets the locale code of this auto translation.
    *
    * @return the locale code, never {@code null} or empty
    */
   public String getLocale()
   {
      return locale;
   }
   
   /**
    * Sets a new locale code for this auto translation.
    *
    * @param lang the new locale code, not {@code null} or empty
    * @throws IllegalArgumentException if lang is {@code null} or empty
    */
   public void setLocale(String lang)
   {
      if (StringUtils.isBlank(lang))
         throw new IllegalArgumentException("locale cannot be null or empty");

      this.locale = lang;
   }

   @Override
   public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof PSAutoTranslationPK)) return false;
      var that = (PSAutoTranslationPK) o;
      return getContentTypeId() == that.getContentTypeId() &&
             Objects.equals(getLocale(), that.getLocale());
   }

   @Override
   public int hashCode() {
      return Objects.hash(getContentTypeId(), getLocale());
   }

   @Override
   public String toString() {
      return new StringJoiner(", ", PSAutoTranslationPK.class.getSimpleName() + "[", "]")
         .add("contentTypeId=" + contentTypeId)
         .add("locale='" + locale + "'")
         .toString();
   }
}
