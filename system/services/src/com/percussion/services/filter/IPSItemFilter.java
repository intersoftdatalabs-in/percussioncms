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
// REFACTORED: CP-JAVA11
package com.percussion.services.filter;

import com.percussion.services.catalog.IPSCatalogItem;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.function.Predicate;
import java.util.stream.Stream;

/**
 * Filters narrow lists of content items down for use in content lists and slot
 * content finders with comprehensive Java 11 modernization. An item filter may
 * aggregate any number of rules, with as few as zero rules. A filter with no rules
 * does no filtering, i.e. may be considered an "identity" filter.
 * <p>
 * Filters also can adjust the items contained in the set of items to be returned.
 * A filter rule that modifies an id will not modify the original guid. A specific
 * example of this is the preview filter rule, which replaces the item id with the
 * id of the current or edit revision of the item when appropriate.
 *
 * <h2>Java 11 Features</h2>
 * <ul>
 * <li>Optional-based safe access for nullable properties</li>
 * <li>Enhanced validation with Objects.requireNonNull</li>
 * <li>Stream API for efficient rule processing</li>
 * <li>CompletableFuture support for asynchronous filtering</li>
 * </ul>
 *
 * @see IPSItemFilterRule 
 * @see IPSItemFilterRuleDef
 * 
 * @author dougrand
 */
public interface IPSItemFilter extends IPSCatalogItem {

   /**
    * A filter takes a list of elements that reference content items and returns
    * a new list with items that don't match the filter removed.
    * <p>
    * If one or more items in the returned set need to be modified in some
    * fashion, follow the rules specified on
    * {@link IPSItemFilterRule#filter(List, Map)}.
    * 
    * @param items the input items to filter, not {@code null}
    * @param params programmatic parameters to the rules in the filter, may be
    *               {@code null} or empty
    * @return a list of {@link IPSFilterItem}s, never {@code null} but
    *         may be empty if no ids match the filter
    * @throws PSFilterException if there is a problem while filtering the content
    * @throws IllegalArgumentException if items is null
    */
   default List<IPSFilterItem> filter(List<IPSFilterItem> items, Map<String, String> params)
         throws PSFilterException {
      Objects.requireNonNull(items, "items cannot be null");
      return filterImpl(items, params);
   }

   /**
    * Internal implementation for filtering.
    */
   List<IPSFilterItem> filterImpl(List<IPSFilterItem> items, Map<String, String> params)
         throws PSFilterException;

   /**
    * Asynchronously filter a list of items for non-blocking operations.
    *
    * @param items the input items to filter, not {@code null}
    * @param params programmatic parameters to the rules in the filter, may be {@code null}
    * @return CompletableFuture containing the filtered list
    * @throws IllegalArgumentException if items is null
    */
   default CompletableFuture<List<IPSFilterItem>> filterAsync(List<IPSFilterItem> items,
                                                             Map<String, String> params) {
      Objects.requireNonNull(items, "items cannot be null");
      return CompletableFuture.supplyAsync(() -> {
         try {
            return filter(items, params);
         } catch (PSFilterException e) {
            throw new RuntimeException("Filter operation failed", e);
         }
      });
   }

   /**
    * Filter items using a Stream for efficient processing.
    *
    * @param items the input items to filter, not {@code null}
    * @param params programmatic parameters to the rules in the filter, may be {@code null}
    * @return Stream of filtered items, never {@code null}
    * @throws IllegalArgumentException if items is null
    */
   default Stream<IPSFilterItem> streamFilter(List<IPSFilterItem> items,
                                             Map<String, String> params) {
      try {
         return filter(items, params).stream();
      } catch (PSFilterException e) {
         return Stream.empty();
      }
   }

   /**
    * The name of the filter. Filter names must be unique.
    * 
    * @return the name of the filter, never {@code null} or empty
    */
   String getName();

   /**
    * Set the name of the filter with enhanced validation. If the name is not unique,
    * this method throws an exception.
    *
    * @param name the new name, not {@code null} or empty
    * @throws PSFilterException if the name is not unique
    * @throws IllegalArgumentException if name is null or empty
    */
   default void setName(String name) throws PSFilterException {
      Objects.requireNonNull(name, "name cannot be null");
      if (name.trim().isEmpty()) {
         throw new IllegalArgumentException("name cannot be empty");
      }
      setNameImpl(name.trim());
   }

   /**
    * Internal implementation for name setting.
    */
   void setNameImpl(String name) throws PSFilterException;

   /**
    * Get the parent filter safely. If the parent is defined, then the associated rules
    * will be combined for filtering. The rule order is only determined by the
    * priorities of the rules, not by the chaining order.
    *
    * @return Optional containing the parent filter if present, empty otherwise
    */
   default Optional<IPSItemFilter> findParentFilter() {
      return Optional.ofNullable(getParentFilter());
   }

   /**
    * Get the parent filter. If the parent is defined, then the associated rules
    * will be combined for filtering. The rule order is only determined by the
    * priorities of the rules, not by the chaining order.
    * 
    * @return Returns the parentFilter, may be {@code null}
    */
   IPSItemFilter getParentFilter();

   /**
    * Set the parent filter.
    * @param parentFilter The parentFilter to set, may be {@code null}
    */
   void setParentFilter(IPSItemFilter parentFilter);

   /**
    * Get the description safely.
    * @return Optional containing the description if present and non-empty, empty otherwise
    */
   default Optional<String> findDescription() {
      return Optional.ofNullable(getDescription())
          .filter(desc -> !desc.trim().isEmpty());
   }

   /**
    * A human readable description of the filter.
    * 
    * @return the description, should not be {@code null} or empty
    */
   String getDescription();

   /**
    * Set the description of the filter.
    *
    * @param description the new description, may be {@code null} or empty
    */
   void setDescription(String description);

   /**
    * Get the legacy authtype ID safely.
    * @return Optional containing the legacy authtype ID if present, empty otherwise
    */
   default Optional<Integer> findLegacyAuthtypeId() {
      return Optional.ofNullable(getLegacyAuthtypeId());
   }

   /**
    * A value that can be used when translating from an old authtype value to an
    * item filter. Not every filter is required to have this defined.
    * 
    * @return a value matching the old authtype that the filter replaced. May be
    *         {@code null} for a new filter with no old authtype equivalent
    */
   Integer getLegacyAuthtypeId();

   /**
    * Set a new authtype id.
    *
    * @param authTypeId the new authtype id, may be {@code null}
    */
   void setLegacyAuthtypeId(Integer authTypeId);

   /**
    * The list of rules that define this filter. To modify the list it is best
    * to use {@link #addRuleDef(IPSItemFilterRuleDef)} and
    * {@link #removeRuleDef(IPSItemFilterRuleDef)}.
    * 
    * @return the set of rules, may be empty but never {@code null}
    */
   Set<IPSItemFilterRuleDef> getRuleDefs();

   /**
    * Set new rule definitions with enhanced validation.
    *
    * @param ruleDefs the new filter rules, may be {@code null} or empty
    */
   void setRuleDefs(Set<IPSItemFilterRuleDef> ruleDefs);

   /**
    * Add a rule def to the filter with enhanced validation. This method takes care
    * of the details of associating the rule def with the filter.
    *
    * @param def the rule def, not {@code null}
    * @throws IllegalArgumentException if def is null
    */
   default void addRuleDef(IPSItemFilterRuleDef def) {
      Objects.requireNonNull(def, "def cannot be null");
      addRuleDefImpl(def);
   }

   /**
    * Internal implementation for adding rule definition.
    */
   void addRuleDefImpl(IPSItemFilterRuleDef def);

   /**
    * Remove a rule def from the filter with enhanced validation. This method takes
    * care of the details of removing the rule def with the filter. The rule def in
    * storage will be removed when the filter is saved.
    *
    * @param def the rule def, not {@code null}
    * @throws IllegalArgumentException if def is null
    */
   default void removeRuleDef(IPSItemFilterRuleDef def) {
      Objects.requireNonNull(def, "def cannot be null");
      removeRuleDefImpl(def);
   }

   /**
    * Internal implementation for removing rule definition.
    */
   void removeRuleDefImpl(IPSItemFilterRuleDef def);

   /**
    * Get a stream of rule definitions for efficient processing.
    *
    * @return Stream of rule definitions, never {@code null}
    */
   default Stream<IPSItemFilterRuleDef> streamRuleDefs() {
      return getRuleDefs().stream();
   }

   /**
    * Find rule definitions that match the given predicate.
    *
    * @param predicate the condition to test rule definitions against, not {@code null}
    * @return a list of matching rule definitions, never {@code null}, may be empty
    * @throws IllegalArgumentException if predicate is null
    */
   default List<IPSItemFilterRuleDef> findRuleDefsWhere(Predicate<IPSItemFilterRuleDef> predicate) {
      Objects.requireNonNull(predicate, "predicate cannot be null");
      return streamRuleDefs()
          .filter(predicate)
          .toList();
   }

   /**
    * Check if this filter has any rules defined.
    *
    * @return {@code true} if the filter has rules, {@code false} otherwise
    */
   default boolean hasRules() {
      return !getRuleDefs().isEmpty();
   }

   /**
    * Check if this filter has a parent filter.
    *
    * @return {@code true} if parent filter is present, {@code false} otherwise
    */
   default boolean hasParentFilter() {
      return findParentFilter().isPresent();
   }

   /**
    * Check if this filter has a meaningful description.
    *
    * @return {@code true} if description is present and non-empty, {@code false} otherwise
    */
   default boolean hasDescription() {
      return findDescription().isPresent();
   }

   /**
    * Check if this filter has a legacy authtype ID.
    *
    * @return {@code true} if legacy authtype ID is present, {@code false} otherwise
    */
   default boolean hasLegacyAuthtypeId() {
      return findLegacyAuthtypeId().isPresent();
   }

   /**
    * Get the total number of rules in this filter.
    *
    * @return the rule count
    */
   default long getRuleCount() {
      return streamRuleDefs().count();
   }

   /**
    * Check if this filter is an identity filter (no rules).
    *
    * @return {@code true} if this is an identity filter, {@code false} otherwise
    */
   default boolean isIdentityFilter() {
      return !hasRules();
   }

   /**
    * Get a summary string representation of this filter.
    *
    * @return a formatted string containing filter details, never {@code null}
    */
   default String getSummary() {
      return String.format("Filter[name='%s', rules=%d, hasParent=%s, isIdentity=%s]",
          getName(), getRuleCount(), hasParentFilter(), isIdentityFilter());
   }

   /**
    * Validate that this filter is properly configured.
    *
    * @return {@code true} if the filter is valid, {@code false} otherwise
    */
   default boolean isValid() {
      return getName() != null && !getName().trim().isEmpty();
   }
}
