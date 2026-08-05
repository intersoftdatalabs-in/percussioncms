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

import java.util.Set;

/**
 * Contract for the DAO that catalogs which fields on content types should be cataloged
 * (extracted as metadata) when hashed files are stored.
 *
 * @author stephenbolton
 */
public interface IPSHashedFieldCatalogerDAO
{

   /**
    * Persist a PSHashedColumn entity
    * @param column
    */
   public void save(PSHashedColumn column);
   
   /**
    * Persist a set of PSHashedColumn entities
    * @param columns
    */
   public void saveAll(Set<PSHashedColumn> columns);
   
   /**
    * Return a set of all known PSHashedColumn entities from the database.
    * @return the columns
    */
   public Set<PSHashedColumn> getStoredColumns();

   /**
    * Removes a PSHashedColumn
    * @param col
    */
   public void remove(PSHashedColumn col);


}
