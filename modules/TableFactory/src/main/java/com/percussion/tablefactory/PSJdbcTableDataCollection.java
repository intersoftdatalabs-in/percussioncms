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

package com.percussion.tablefactory;

import com.percussion.error.PSExceptionUtils;
import com.percussion.util.PSCollection;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import static com.percussion.tablefactory.IPSLogger.LOG_CATEGORY;

/**
 * This class is a container for a list of PSJdbcTableData objects, enabling
 * them to be serialized as a collection to and from Xml.
 */
public class PSJdbcTableDataCollection extends PSCollection
{
   private static final Logger log = LogManager.getLogger(LOG_CATEGORY);

   /**
    * Constructs an empty PSJdbcTableDataCollection
    */
   public PSJdbcTableDataCollection()
   {
      super(PSJdbcTableData.class);
   }
   
   /**
    * Creates this object from its Xml representation.  See {@link #fromXml(
    * Element) fromXml} for more information.
    *
    * @param doc The document from which this object is to be constructed.
    *    Root element must conform to the definition for the tables element in
    *    the tabledata.dtd.  May not be <code>null</code>.
    *
    * @throws IllegalArgumentException if doc is <code>null</code>.
    * @throws PSJdbcTableFactoryException if the Xml definition is invalid, or
    * if there are any other errors.
    */
   public PSJdbcTableDataCollection(Document doc) 
      throws PSJdbcTableFactoryException
   {
      this();
      
      if (doc == null)
         throw new IllegalArgumentException("doc may not be null");

      fromXml(doc.getDocumentElement());
   }

   /**
    * Restore this object from an Xml representation.
    *
    * @param sourceNode The element from which to get this object's state.
    *    Element must conform to the definition for the component
    *    element in the tabledata.dtd.  May not be <code>null</code>.
    *                     
    * @throws IllegalArgumentException if sourceNode is <code>null</code>.
    * @throws PSJdbcTableFactoryException if there are any errors.
    */
   public void fromXml(Element sourceNode)
      throws PSJdbcTableFactoryException
   {
      if (sourceNode == null)
         throw new IllegalArgumentException("sourceNode may not be null");

      if (!sourceNode.getNodeName().equals(NODE_NAME))
      {
         Object[] args = {NODE_NAME, sourceNode.getNodeName()};
         throw new PSJdbcTableFactoryException(
            IPSTableFactoryErrors.XML_ELEMENT_WRONG_TYPE, args);
      }

      clear();

      PSXmlTreeWalker walker = new PSXmlTreeWalker(sourceNode);
      int firstFlags = PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN |
         PSXmlTreeWalker.GET_NEXT_RESET_CURRENT;
      int nextFlags = PSXmlTreeWalker.GET_NEXT_ALLOW_SIBLINGS |
         PSXmlTreeWalker.GET_NEXT_RESET_CURRENT;

      Element table = walker.getNextElement(PSJdbcTableData.NODE_NAME,
         firstFlags); 
         
      if (table == null)
      {
         Object[] args = {NODE_NAME, PSJdbcTableData.NODE_NAME, "null"};
         throw new PSJdbcTableFactoryException(
            IPSTableFactoryErrors.XML_ELEMENT_NULL, args);
      }
      
      while (table != null)
      {
         add(new PSJdbcTableData(table));
         table = walker.getNextElement(PSJdbcTableData.NODE_NAME, nextFlags);
      }
   }

   /**
    * Serializes this object's state to Xml conforming with the tabledata.dtd.
    *
    * @param doc The document to use when creating elements.  May not be <code>
    *    null</code>.
    *
    * @return The element containing this object's state, never <code>
    *    null</code>.
    *
    * @throws IllegalArgumentException if doc is <code>null</code>.
    */
   public Element toXml(Document doc)
   {
      if (doc == null)
         throw new IllegalArgumentException("doc may not be null");

      // create the root element
      Element   root = doc.createElement(NODE_NAME);
      for (Object o : this) {
         PSJdbcTableData table = (PSJdbcTableData) o;
         root.appendChild(table.toXml(doc));
      }
         
      return root;
   }

   /**
    * Returns the table data object with the specified name.
    *
    * @param name The name of the table data to locate.  May not be <code>null
    * </code> or empty.
    *
    * @return The matching data object, or <code>null</code> if it is not
    * found.
    *
    * @throws IllegalArgumentException if name is <code>null</code> or emtpy.
    */
   public PSJdbcTableData getTableData(String name)
   {
      if (name == null || name.trim().length() == 0)
         throw new IllegalArgumentException(
            "name may not be null or empty");

      PSJdbcTableData tableData = null;

      for (Object o : this) {
         PSJdbcTableData tempTable = (PSJdbcTableData) o;
         if (tempTable.getName().equalsIgnoreCase(name)) {
            if (tableData != null) {
               log.info("Duplicate table data for table {}", name);
            } else
               tableData = tempTable;

         }
      }

      return tableData;
   }



   // testing only
   public static void main(String[] args)
   {
      if (args.length != 1)
      {
         log.info("test usage: ");
         log.info("java PSJdbcTableDataCollection <tables>");
         log.info(
            "where <tables> is an xml file containing the expected xml.");
         System.exit(1);
      }
      try(java.io.FileInputStream in = new java.io.FileInputStream(args[0]))
      {

         Document doc = PSXmlDocumentBuilder.createXmlDocument(in, false);
         PSJdbcTableDataCollection coll = new PSJdbcTableDataCollection(doc);

        try (java.io.FileOutputStream out = new java.io.FileOutputStream(args[0] + ".tst") ){
           PSXmlDocumentBuilder.write(coll.toXml(doc), out);
        }
      }
      catch (Exception e)
      {
         log.error(PSExceptionUtils.getMessageForLog(e));
      }
   }

   /**
    * The name of this objects root Xml element.
    */
   public static final String NODE_NAME = "tables";

}

