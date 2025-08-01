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
// REFACTORED: CP-JAVA11
package com.percussion.services.publisher;

import com.percussion.services.catalog.IPSCatalogItem;
import com.percussion.services.filter.IPSItemFilter;
import com.percussion.services.publisher.data.PSContentList;
import com.percussion.services.publisher.data.PSEditionType;
import com.percussion.utils.guid.IPSGuid;

import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Represents a content list from the database with comprehensive Java 11 modernization.
 * A content list feeds a list of assembly items into the publishing system. Legacy content
 * lists run a query resource to create a content list XML. New content lists consist of:
 * <ul>
 * <li>A generator that creates the initial set of content items</li>
 * <li>A filter, which removes inappropriate items such as items that have
 * not yet reached a public state</li>
 * <li>A template expander, which takes each qualified item and finds the
 * templates that should be used when publishing</li>
 * </ul>
 * The content list execution results in a set of assembly items.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Optional-based safe access for nullable properties</li>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Stream API for parameter processing</li>
 * <li>Improved enum design with lookup capabilities</li>
 * </ul>
 *
 * @author dougrand
 */
public interface IPSContentList extends IPSCatalogItem, Cloneable {
   /**
    * The type of content list, which dictates how it will be processed.
    */
   enum Type {
      /**
       * Normal processing, pass all items from the template expander
       */
      NORMAL("Normal"), 
      /**
       * Incremental processing, only pass items that must be published 
       * due to changes or calculated content
       */
      INCREMENTAL("Incremental");
      
      private final String label;

      /**
       * Create a type with the specified label.
       * @param label the label of this type, not {@code null}
       */
      Type(String label) {
         this.label = Objects.requireNonNull(label, "label cannot be null");
      }
      
      /**
       * Get the label of this type.
       * @return the label, never {@code null} or empty
       */
      public String getLabel() {
         return label;
      }
      
      /**
       * Lookup value by ordinal with safe fallback.
       *
       * @param ordinal the ordinal
       * @return the matching enum value, or Normal as a default
       */
      public static Type valueOf(int ordinal) {
         return Stream.of(values())
             .filter(t -> t.ordinal() == ordinal)
             .findFirst()
             .orElse(NORMAL);
      }

      /**
       * Lookup value by label with Optional return.
       *
       * @param label the label to search for, not {@code null}
       * @return Optional containing the matching type, empty if not found
       * @throws IllegalArgumentException if label is null
       */
      public static Optional<Type> findByLabel(String label) {
         Objects.requireNonNull(label, "label cannot be null");
         return Stream.of(values())
             .filter(t -> t.getLabel().equalsIgnoreCase(label.trim()))
             .findFirst();
      }
   }
   
   /**
    * Get the type of the content list. The type of the content list dictates
    * the semantics of the content list.
    *
    * @return the type, never {@code null}
    * @see Type
    */
   Type getContentListType();

   /**
    * Set the type of the content list with enhanced validation.
    * @param newtype the new type, not {@code null}
    * @throws IllegalArgumentException if newtype is null
    */
   default void setContentListType(Type newtype) {
      Objects.requireNonNull(newtype, "newtype cannot be null");
      setContentListTypeImpl(newtype);
   }

   /**
    * Internal implementation for type setting.
    */
   void setContentListTypeImpl(Type newtype);

   /**
    * Gets the registered arguments for the generator plugin. The generator
    * is given these arguments, with appropriate overrides.
    * @return a map of parameter name to parameter value, all as strings, may be
    * empty but never {@code null}
    */
   Map<String, String> getGeneratorParams();

   /**
    * Set the generator arguments with enhanced validation. This method carefully
    * folds the new arguments into the old arguments.
    *
    * @param newargs the new arguments, not {@code null}
    * @throws IllegalArgumentException if newargs is null
    */
   default void setGeneratorParams(Map<String, String> newargs) {
      Objects.requireNonNull(newargs, "newargs cannot be null");
      setGeneratorParamsImpl(newargs);
   }

   /**
    * Internal implementation for generator parameters setting.
    */
   void setGeneratorParamsImpl(Map<String, String> newargs);

   /**
    * Gets the registered arguments for the expander plugin. The expander
    * is given these arguments, with appropriate overrides.
    * @return a map of parameter name to parameter value, all as strings, may be
    * empty but never {@code null}
    */
   Map<String, String> getExpanderParams();

   /**
    * Set the expander arguments with enhanced validation. This method carefully
    * folds the new arguments into the old arguments.
    *
    * @param newargs the new arguments, not {@code null}
    * @throws IllegalArgumentException if newargs is null
    */
   default void setExpanderParams(Map<String, String> newargs) {
      Objects.requireNonNull(newargs, "newargs cannot be null");
      setExpanderParamsImpl(newargs);
   }

   /**
    * Internal implementation for expander parameters setting.
    */
   void setExpanderParamsImpl(Map<String, String> newargs);

   /**
    * Modify the generator parameters by adding the given name and value with validation.
    * @param name the param name, not {@code null} or empty
    * @param value the parameter value, not {@code null} or empty
    * @throws IllegalArgumentException if name or value is null or empty
    */
   default void addGeneratorParam(String name, String value) {
      Objects.requireNonNull(name, "name cannot be null");
      Objects.requireNonNull(value, "value cannot be null");
      if (name.trim().isEmpty()) {
         throw new IllegalArgumentException("name cannot be empty");
      }
      if (value.trim().isEmpty()) {
         throw new IllegalArgumentException("value cannot be empty");
      }
      addGeneratorParamImpl(name.trim(), value.trim());
   }

   /**
    * Internal implementation for adding generator parameter.
    */
   void addGeneratorParamImpl(String name, String value);

   /**
    * Remove the given generator parameter with validation.
    * @param name the parameter name, not {@code null} or empty
    * @throws IllegalArgumentException if name is null or empty
    */
   default void removeGeneratorParam(String name) {
      Objects.requireNonNull(name, "name cannot be null");
      if (name.trim().isEmpty()) {
         throw new IllegalArgumentException("name cannot be empty");
      }
      removeGeneratorParamImpl(name.trim());
   }

   /**
    * Internal implementation for removing generator parameter.
    */
   void removeGeneratorParamImpl(String name);

   /**
    * Modify the expander parameters by adding the given name and value with validation.
    * @param name the parameter name, not {@code null} or empty
    * @param value the parameter value, not {@code null} or empty
    * @throws IllegalArgumentException if name or value is null or empty
    */
   default void addExpanderParam(String name, String value) {
      Objects.requireNonNull(name, "name cannot be null");
      Objects.requireNonNull(value, "value cannot be null");
      if (name.trim().isEmpty()) {
         throw new IllegalArgumentException("name cannot be empty");
      }
      if (value.trim().isEmpty()) {
         throw new IllegalArgumentException("value cannot be empty");
      }
      addExpanderParamImpl(name.trim(), value.trim());
   }

   /**
    * Internal implementation for adding expander parameter.
    */
   void addExpanderParamImpl(String name, String value);

   /**
    * Remove the given expander parameter with validation.
    * @param name the parameter name, not {@code null} or empty
    * @throws IllegalArgumentException if name is null or empty
    */
   default void removeExpanderParam(String name) {
      Objects.requireNonNull(name, "name cannot be null");
      if (name.trim().isEmpty()) {
         throw new IllegalArgumentException("name cannot be empty");
      }
      removeExpanderParamImpl(name.trim());
   }

   /**
    * Internal implementation for removing expander parameter.
    */
   void removeExpanderParamImpl(String name);

   /**
    * Get the description safely.
    * @return Optional containing the description if present, empty otherwise
    */
   default Optional<String> findDescription() {
      return Optional.ofNullable(getDescription()).filter(desc -> !desc.trim().isEmpty());
   }

   /**
    * @return Returns the description, may be {@code null}
    */
   String getDescription();

   /**
    * Set the description with validation.
    * @param description The description to set, may be {@code null}
    */
   void setDescription(String description);

   /**
    * Get the edition type safely.
    * @return Optional containing the edition type if present, empty otherwise
    */
   default Optional<PSEditionType> findEditionType() {
      return Optional.ofNullable(getEditionType());
   }

   /**
    * @return Returns the editionType, may be {@code null}
    */
   PSEditionType getEditionType();

   /**
    * @param editionType The editionType to set, may be {@code null}
    */
   void setEditionType(PSEditionType editionType);

   /**
    * Get the expander safely.
    * @return Optional containing the expander if present, empty otherwise
    */
   default Optional<String> findExpander() {
      return Optional.ofNullable(getExpander()).filter(exp -> !exp.trim().isEmpty());
   }

   /**
    * @return Returns the expander, may be {@code null}
    */
   String getExpander();

   /**
    * Set the expander with validation.
    * @param expander The expander to set, may be {@code null}
    */
   void setExpander(String expander);

   /**
    * Get the generator safely.
    * @return Optional containing the generator if present, empty otherwise
    */
   default Optional<String> findGenerator() {
      return Optional.ofNullable(getGenerator()).filter(gen -> !gen.trim().isEmpty());
   }

   /**
    * @return Returns the generator, may be {@code null}
    */
   String getGenerator();

   /**
    * Set the generator with validation.
    * @param generator The generator to set, may be {@code null}
    */
   void setGenerator(String generator);

   /**
    * @return Returns the name, never {@code null}
    */
   String getName();

   /**
    * Set the name with enhanced validation.
    * @param name The name to set, not {@code null} or empty. The name must be unique
    * @throws IllegalArgumentException if name is null or empty
    */
   default void setName(String name) {
      Objects.requireNonNull(name, "name cannot be null");
      if (name.trim().isEmpty()) {
         throw new IllegalArgumentException("name cannot be empty");
      }
      setNameImpl(name.trim());
   }

   /**
    * Internal implementation for name setting.
    */
   void setNameImpl(String name);

   /**
    * The url to invoke the content list.
    * 
    * @return Returns the url, never {@code null} or empty
    */
   String getUrl();

   /**
    * Set the URL with enhanced validation.
    * @param url The url to set, not {@code null} or empty
    * @throws IllegalArgumentException if url is null or empty
    */
   default void setUrl(String url) {
      Objects.requireNonNull(url, "url cannot be null");
      if (url.trim().isEmpty()) {
         throw new IllegalArgumentException("url cannot be empty");
      }
      setUrlImpl(url.trim());
   }

   /**
    * Internal implementation for URL setting.
    */
   void setUrlImpl(String url);

   /**
    * Get the item filter for this content list safely. The filter, if present,
    * limits the results from the generator for new style content lists.
    * <p>
    * Note, the returned Optional will always be empty if the Content List object
    * is not loaded from service layer; otherwise the returned Optional may contain
    * a value if the Content List object is loaded from service layer and
    * {@link #getFilterId()} is not {@code null}.
    *
    * @return Optional containing the filter if present, empty otherwise
    */
   default Optional<IPSItemFilter> findFilter() {
      return Optional.ofNullable(getFilter());
   }

   /**
    * Get the item filter for this content list. The filter, if present, 
    * limits the results from the generator for new style content lists.
    * <p>
    * Note, the returned object will always be {@code null} if the
    * Content List object is not loaded from service layer; otherwise the
    * returned object may not be {@code null} if the Content List object
    * is loaded from service layer and {@link #getFilterId()} is not
    * {@code null}.
    *
    * @return the filter, may be {@code null}
    */
   IPSItemFilter getFilter();

   /**
    * Get the ID of the item filter for this content list safely.
    *
    * @return Optional containing the item filter ID if present, empty otherwise
    */
   default Optional<IPSGuid> findFilterId() {
      return Optional.ofNullable(getFilterId());
   }

   /**
    * Get the ID of the item filter for this content list. The filter, if 
    * present, limits the results from the generator for new style content lists.
    * 
    * @return the item filter ID, may be {@code null} if the item filter is
    *    not defined for this Content List
    */
   IPSGuid getFilterId();
   
   /**
    * Set a new item filter.
    * @param filterId the ID of the new item filter. It may be {@code null}
    *    if need to clear the item filter from this object
    */
   void setFilterId(IPSGuid filterId);
   
   /**
    * Set the item filter
    * @param filter the filter, may be {@code null}
    * @deprecated use {@link #setFilterId(IPSGuid)} instead
    */
   @Deprecated
   void setFilter(IPSItemFilter filter);

   /**
    * Determines if this is a legacy Content List or not. A legacy Content List
    * does not have item filter, generator or expander.
    * @return {@code true} if is a legacy Content List
    */
   boolean isLegacy();
   
   /**
    * Check if this content list has any parameters configured.
    * @return {@code true} if either generator or expander parameters are configured
    */
   default boolean hasParameters() {
      return !getGeneratorParams().isEmpty() || !getExpanderParams().isEmpty();
   }

   /**
    * Check if this content list has a specific generator parameter.
    * @param paramName the parameter name to check, not {@code null}
    * @return {@code true} if the parameter exists
    * @throws IllegalArgumentException if paramName is null
    */
   default boolean hasGeneratorParam(String paramName) {
      Objects.requireNonNull(paramName, "paramName cannot be null");
      return getGeneratorParams().containsKey(paramName);
   }

   /**
    * Check if this content list has a specific expander parameter.
    * @param paramName the parameter name to check, not {@code null}
    * @return {@code true} if the parameter exists
    * @throws IllegalArgumentException if paramName is null
    */
   default boolean hasExpanderParam(String paramName) {
      Objects.requireNonNull(paramName, "paramName cannot be null");
      return getExpanderParams().containsKey(paramName);
   }

   /**
    * Clone the content list.
    * @return the cloned content list, never {@code null}
    */
   PSContentList clone();
}
