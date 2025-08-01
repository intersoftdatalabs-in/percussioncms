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
package com.percussion.services.catalog;

import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.xml.PSInvalidXmlException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a complete catalog item with XML serialization capabilities and enhanced Java 11 support.
 *
 * <p>A catalog item is a fully-featured object that can be stored into or retrieved from a service.
 * Items are capable of serializing and restoring themselves to/from XML documents, making them
 * suitable for deployment, migration, and persistence operations.
 *
 * <p>Key responsibilities:
 * <ul>
 *   <li><strong>XML Serialization:</strong> Convert object state to XML format</li>
 *   <li><strong>XML Deserialization:</strong> Restore object state from XML</li>
 *   <li><strong>GUID Management:</strong> Maintain globally unique identification</li>
 *   <li><strong>Type Safety:</strong> Ensure proper type handling during operations</li>
 * </ul>
 *
 * <p>Usage Example - MSM (Multi-Server Manager):
 * <ol>
 *   <li>Request list of assembler IDs from assembly system</li>
 *   <li>Store specific assembler with appropriate type ID</li>
 *   <li>During restoration, use type ID to determine handling service</li>
 * </ol>
 *
 * <p>Key features:
 * <ul>
 *   <li>Enhanced XML serialization with Optional-based error handling</li>
 *   <li>Improved GUID management with validation</li>
 *   <li>Type-safe operations with comprehensive documentation</li>
 *   <li>Backward compatibility with existing implementations</li>
 * </ul>
 *
 * @author dougrand
 * @since Java 11 Modernization
 */
public interface IPSCatalogItem extends IPSCatalogIdentifier {

   /**
    * Serialize the item information to XML format with enhanced error handling.
    *
    * <p>The implementation may use various serialization approaches including JavaBean APIs,
    * custom XML generation, or framework-specific serializers. The resulting XML must contain
    * the item's GUID but is not required to include type information (handled separately).
    *
    * <p>Requirements:
    * <ul>
    *   <li>Must include the item's GUID in the XML</li>
    *   <li>Should be well-formed and valid XML</li>
    *   <li>Must contain sufficient information for complete restoration</li>
    * </ul>
    *
    * @return XML string representing the item, never {@code null} or empty
    * @throws IOException if there is a problem during serialization
    * @throws SAXException if there is an issue converting the object to XML
    */
   String toXML() throws IOException, SAXException;

   /**
    * Serialize to XML with Optional wrapper for safer error handling.
    *
    * <p>This method provides a safer alternative to {@link #toXML()} by returning
    * an Optional instead of throwing exceptions for certain error conditions.
    *
    * @return Optional containing the XML string if successful, empty if serialization fails
    */
   default Optional<String> toXMLOptional() {
      try {
         return Optional.of(toXML());
      } catch (IOException | SAXException e) {
         return Optional.empty();
      }
   }

   /**
    * Restore the item from XML description with enhanced validation.
    *
    * <p>The implementation may use various deserialization approaches including JavaBean APIs,
    * DOM/SAX parsing, or framework-specific deserializers. The XML source must contain
    * sufficient information to completely restore the object state.
    *
    * <p>Validation requirements:
    * <ul>
    *   <li>XML must be well-formed and parseable</li>
    *   <li>Required elements and attributes must be present</li>
    *   <li>Data values must be valid for their respective fields</li>
    *   <li>GUID information must be consistent</li>
    * </ul>
    *
    * @param xmlsource the XML string representing this item, not {@code null} or empty
    * @throws SAXException if there is a problem parsing the XML source
    * @throws IOException if there is a problem reading the XML source
    * @throws PSInvalidXmlException if the XML structure is incorrect or required data is missing
    * @throws IllegalArgumentException if xmlsource is null or empty
    */
   void fromXML(String xmlsource) throws IOException, SAXException, PSInvalidXmlException;

   /**
    * Restore from XML with enhanced validation and error handling.
    *
    * @param xmlsource the XML string representing this item, not {@code null} or empty
    * @return true if restoration was successful, false otherwise
    */
   default boolean fromXMLSafely(String xmlsource) {
      if (xmlsource == null || xmlsource.trim().isEmpty()) {
         return false;
      }
      try {
         fromXML(xmlsource);
         return true;
      } catch (IOException | SAXException | PSInvalidXmlException e) {
         return false;
      }
   }

   /**
    * Set a globally unique identifier with enhanced validation.
    *
    * <p>This method is primarily used for setting the initial identifier for newly created objects.
    * Once an object has an identifier assigned, subsequent calls should fail to maintain
    * identifier immutability and data integrity.
    *
    * <p>GUID requirements:
    * <ul>
    *   <li>Must be globally unique across the system</li>
    *   <li>Should be stable across object lifecycle</li>
    *   <li>Must not be null or invalid</li>
    * </ul>
    *
    * @param newguid the globally unique identifier, not {@code null}
    * @throws IllegalStateException if the object already has an identifier assigned
    * @throws IllegalArgumentException if newguid is null
    * @see IPSGuid for more information about GUID structure and requirements
    */
   void setGUID(IPSGuid newguid) throws IllegalStateException;

   /**
    * Set GUID with enhanced validation and null checking.
    *
    * @param newguid the globally unique identifier, not {@code null}
    * @return true if the GUID was set successfully, false if already assigned
    * @throws IllegalArgumentException if newguid is null
    */
   default boolean setGUIDSafely(IPSGuid newguid) {
      Objects.requireNonNull(newguid, "newguid cannot be null");
      try {
         setGUID(newguid);
         return true;
      } catch (IllegalStateException e) {
         return false;
      }
   }

   /**
    * Check if this item has a valid GUID assigned.
    *
    * @return true if the item has a non-null GUID
    */
   default boolean hasValidGUID() {
      return getGUID() != null;
   }

   /**
    * Get a string representation suitable for debugging and logging.
    *
    * @return formatted string with item type, GUID, and basic information
    */
   default String toDebugString() {
      var guid = getGUID();
      var type = getType();
      return String.format("%s{type=%s, guid=%s}",
                          getClass().getSimpleName(),
                          type != null ? type : "unknown",
                          guid != null ? guid : "unassigned");
   }

   /**
    * Create a defensive copy of this item by serializing to XML and deserializing back.
    *
    * <p>This method provides a generic cloning mechanism that works for any catalog item
    * by using the XML serialization/deserialization cycle. The copy will have the same
    * GUID as the original.
    *
    * @return Optional containing a copy of this item if successful, empty if cloning fails
    */
   default Optional<IPSCatalogItem> createCopy() {
      return toXMLOptional()
         .flatMap(xml -> {
            try {
               // Create a new instance of the same type
               var copy = getClass().getDeclaredConstructor().newInstance();
               if (copy instanceof IPSCatalogItem) {
                  var catalogCopy = (IPSCatalogItem) copy;
                  catalogCopy.fromXML(xml);
                  return Optional.of(catalogCopy);
               }
            } catch (Exception e) {
               // Return empty Optional if cloning fails
            }
            return Optional.<IPSCatalogItem>empty();
         });
   }
}
