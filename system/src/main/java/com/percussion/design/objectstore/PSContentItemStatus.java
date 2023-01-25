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

package com.percussion.design.objectstore;

import com.percussion.util.PSStringOperation;
import org.w3c.dom.Element;

import java.util.List;

/**
 * The replacement values used for content item status information.
 */
public class PSContentItemStatus extends PSNamedReplacementValue
{
   /**
    * Constructs a new content itme status from its XML representation.
    * 
    * @param source the XML element node to construct this object from, not
    *    <code>null</code>.
    * @param parent the Java object which is the parent of this object, may be
    *    <code>null</code>.
    * @param parentComponents   the parent objects of this object, may be 
    *    <code>null</code> or empty.
    * @throws IllegalArgumentException if source is <code>null</code>.
    * @throws PSUnknownNodeTypeException if the XML representation is not
    *    in the expected format.
    */
   public PSContentItemStatus(Element source, IPSDocument parent, 
      List parentComponents) throws PSUnknownNodeTypeException
   {
      super(source, parent, parentComponents);
   }

   /**
    * Constructs a new content item status replacement value for the supplied
    * name. 
    * 
    * @param table the table name, this currently supports the CONTENTSTATUS, 
    *    WORKFLOWAPPS and STATES tables, assumed not <code>null</code>.
    * @param column the column name, assumed not <code>null</code>.
    * 
    * @throws IllegalArgumentException if the supplied table name is not 
    * supported.
    */
   public PSContentItemStatus(String table, String column)
   {
      super(table.toUpperCase() + "." + column.toUpperCase());

      validateTable(table);
   }
   
   /**
    * Constructs a new content item status replacement value for the supplied
    * name. 
    * 
    * @param tableCol The table and column names, in the form TABLE.COLUMN.  
    * This currently supports the CONTENTSTATUS, WORKFLOWAPPS and STATES tables, 
    * assumed not <code>null</code>. 
    * 
    * @throws IllegalArgumentException if the supplied table name is not 
    * supported.
    */
   public PSContentItemStatus(String tableCol)
   {
      super(tableCol);

      List tokens = PSStringOperation.getSplittedList(tableCol, '.');
      
      if (tokens.size() != 2)
         throw new IllegalArgumentException("Invalid tableCol");

      String table = tokens.iterator().next().toString();
         
      validateTable(table);
   }   
   
   
   
   /**
    * Checks {@link #isSupportedTable(String)} for the supplied table and
    * throws an {@link IllegalArgumentException} if it is not supported, or if
    * it is <code>null</code> or empty.
    * 
    * @param table The table to check, may be <code>null</code> or empty.
    */
   private void validateTable(String table)
   {
      if (!isSupportedTable(table))
      {      
         String msg = "Only the following tables are supported: ";
         for (int i=0; i<SUPPORTED_TABLES.length; i++)
            msg += SUPPORTED_TABLES[i] + ", ";
         
         throw new IllegalArgumentException(msg);
      }
   }

   /**
    * Checks whether the supplied table is a supported table for this object.
    * Currently supports the CONTENTSTATUS, WORKFLOWAPPS and STATES tables.
    * 
    * @param table the table name, may not be <code>null</code> or empty.
    * 
    * @return <code>true</code> if it is supported, otherwise <code>false</code>
    */
   public static boolean isSupportedTable(String table)
   {
      if(table == null || table.trim().length() == 0)
         throw new IllegalArgumentException("table may not be null or empty.");
         
      for (int i = 0; i < SUPPORTED_TABLES.length; i++) 
      {
         if(SUPPORTED_TABLES[i].equalsIgnoreCase(table))
            return true;         
      }
      
      return false;
   }

   /**
    * Gets the type of replacement value this object represents.
    * 
    * @return {@link #VALUE_TYPE}
    */
   public String getValueType()
   {
      return VALUE_TYPE;
   }

   /**
    * Gets the text which can be displayed to represent this value.
    * 
    * @return "PSXContentItemStatus/" + <code>getName()</code>
    */
   public String getValueDisplayText()
   {
      return XML_NODE_NAME + "/" + getName();
   }

   // see base class for description
   public String getNodeName()
   {
      return XML_NODE_NAME;
   }

   // see base class for description
   public int getErrorCode()
   {
      return IPSObjectStoreErrors.RELATIONSHIP_PROPERTY_NAME_EMPTY;
   }

   /**
    * The value type associated with instances of this class.
    */
   public static final String VALUE_TYPE = "ContentItemStatus";
   
   /**
    * The node name used in XML representations.
    */
   public static final String XML_NODE_NAME = "PSXContentItemStatus";
   
   /**
    * A list of all supported tables names.
    */
   public static final String[] SUPPORTED_TABLES =
   {
      "CONTENTSTATUS",
      "WORKFLOWAPPS",
      "STATES"
   };
}
