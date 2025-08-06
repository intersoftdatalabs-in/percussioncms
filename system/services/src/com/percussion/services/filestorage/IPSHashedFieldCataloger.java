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

package com.percussion.services.filestorage;

import com.percussion.services.filestorage.data.PSHashedColumn;

import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

/**
 * Service for cataloging database columns that contain binary hash references using modern Java 11 patterns.
 *
 * <p>This service helps locate and store records of database columns containing references
 * to binaries by hash. This is crucial for accurately and safely removing unused binaries
 * without missing any references that could lead to orphaned data.</p>
 *
 * @author Percussion Software
 * @since 6.0
 */
public interface IPSHashedFieldCataloger {

   /**
    * Gets the set of hashed columns currently active on the server.
    *
    * @return immutable set of server hashed columns, never {@code null}
    */
   Set<PSHashedColumn> getServerHashedColumns();

   /**
    * Gets the set of hashed columns currently active on the server as a Stream for efficient processing.
    *
    * @return Stream of server hashed columns for functional-style operations
    */
   default Stream<PSHashedColumn> streamServerHashedColumns() {
      return getServerHashedColumns().stream();
   }

   /**
    * Stores the specified columns in the cataloger with validation.
    *
    * @param columns the set of columns to store, not {@code null}
    * @throws IllegalArgumentException if columns is {@code null} or contains invalid entries
    */
   void storeColumns(Set<PSHashedColumn> columns);

   /**
    * Stores the specified columns asynchronously for non-blocking operations.
    *
    * @param columns the set of columns to store, not {@code null}
    * @return CompletableFuture that completes when storage is finished
    */
   CompletableFuture<Void> storeColumnsAsync(Set<PSHashedColumn> columns);

   /**
    * Gets the set of stored columns from the cataloger.
    *
    * @return immutable set of stored columns, never {@code null}
    */
   Set<PSHashedColumn> getStoredColumns();

   /**
    * Gets the set of stored columns as a Stream for efficient processing.
    *
    * @return Stream of stored columns for functional-style operations
    */
   default Stream<PSHashedColumn> streamStoredColumns() {
      return getStoredColumns().stream();
   }

   /**
    * Validates all stored columns against the current server schema.
    *
    * @return immutable set of validated columns, never {@code null}
    */
   Set<PSHashedColumn> validateColumns();

   /**
    * Validates all stored columns asynchronously for non-blocking operations.
    *
    * @return CompletableFuture that completes with the set of validated columns
    */
   CompletableFuture<Set<PSHashedColumn>> validateColumnsAsync();

   /**
    * Adds a column to the cataloger with validation.
    *
    * @param table the table name, not {@code null} or empty
    * @param column the column name, not {@code null} or empty
    * @throws IllegalArgumentException if table or column is {@code null} or empty
    */
   void addColumn(String table, String column);

   /**
    * Adds a column to the cataloger safely with Optional result.
    *
    * @param table the table name, not {@code null} or empty
    * @param column the column name, not {@code null} or empty
    * @return Optional containing the added PSHashedColumn, or empty if addition failed
    */
   Optional<PSHashedColumn> addColumnSafely(String table, String column);

   /**
    * Removes a column from the cataloger.
    *
    * @param table the table name, not {@code null} or empty
    * @param column the column name, not {@code null} or empty
    * @throws IllegalArgumentException if table or column is {@code null} or empty
    */
   void removeColumn(String table, String column);

   /**
    * Removes a column from the cataloger safely with boolean result.
    *
    * @param table the table name, not {@code null} or empty
    * @param column the column name, not {@code null} or empty
    * @return {@code true} if the column was removed, {@code false} if it didn't exist
    */
   boolean removeColumnSafely(String table, String column);

   /**
    * Checks if a specific column is cataloged.
    *
    * @param table the table name, not {@code null} or empty
    * @param column the column name, not {@code null} or empty
    * @return {@code true} if the column is cataloged, {@code false} otherwise
    */
   default boolean hasColumn(String table, String column) {
      return streamStoredColumns()
         .anyMatch(col -> col.getTable().equals(table) && col.getColumn().equals(column));
   }

   /**
    * Gets the count of cataloged columns.
    *
    * @return the number of cataloged columns
    */
   default long getColumnCount() {
      return getStoredColumns().size();
   }

   /**
    * Gets columns for a specific table.
    *
    * @param table the table name, not {@code null} or empty
    * @return Stream of columns for the specified table
    */
   default Stream<PSHashedColumn> getColumnsForTable(String table) {
      return streamStoredColumns()
         .filter(col -> col.getTable().equals(table));
   }
}
