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
package com.percussion.services.catalog;

import com.percussion.services.error.PSNotFoundException;
import com.percussion.utils.guid.IPSGuid;

import java.util.List;

/**
 * A cataloger enumerates types and allows queries for data. Each type is a kind
 * of object the particular service can handle. Each enumeration provides a full
 * list of identifiers for that kind of object.
 * <p>
 * The cataloger can be used in multiple contexts. It is primarily intended as
 * an interface to aid MSM in deploying objects to and from Rhythmyx
 * installations, but could also be used by import or export applications.
 * 
 * @author dougrand
 */
public interface IPSCataloger
{
   /**
    * A given service handles one or more system types. Only one service should
    * handle any specific type. This method returns an array of these types,
    * which can be used by the deployment system to decide what service handles
    * each type, especially for import.
    * 
    * @return an array of types, never <code>null</code>
    */
   PSTypeEnum[] getTypes();

   /**
    * For a given type, query what the known objects are. This can be used in
    * exporting data or by the deployment system to obtain a list of known data
    * prior to export.
    * 
    * @param type the specific type in question, never <code>null</code>
    * 
    * @return a list of summaries for items of the given type, never
    *         <code>null</code> but may be empty if the given implementation
    *         does not know about the specified type, or the set of items of
    *         that type is empty
    * @throws PSCatalogException if there is a problem while loading the summary
    *            data
    */
   List<IPSCatalogSummary> getSummaries(PSTypeEnum type)
           throws PSCatalogException, PSNotFoundException;

   /**
    * Load the item specified by the given document and store into persistent
    * storage. This may overwrite an object in the persistent store.
    * 
    * @param type the type for the incoming item, never <code>null</code>
    * @param item the XML document as a string representing the item in
    *           question, never <code>null</code> or empty
    * @throws PSCatalogException if there's an error when saving the item to the
    *            repository, or the id type isn't handled by the service
    */
   void loadByType(PSTypeEnum type, String item) throws PSCatalogException;

   /**
    * Save the item specified by the guid into an XML string. The type is taken
    * from the passed guid. The item in question is retrieved from persistent
    * storage as required.
    * 
    * @param id the id of the item in question, never <code>null</code>
    * @return the XML document string representing the item, never
    *         <code>null</code> or empty
    * @throws PSCatalogException if there's an error when loading the item from
    *            the repository, or the id type isn't handled by the service
    */
   String saveByType(IPSGuid id) throws PSCatalogException;
}
