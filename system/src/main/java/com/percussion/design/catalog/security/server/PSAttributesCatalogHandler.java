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

package com.percussion.design.catalog.security.server;

import com.percussion.design.catalog.IPSCatalogErrors;
import com.percussion.error.PSIllegalArgumentException;
import com.percussion.security.IPSSecurityProviderMetaData;
import com.percussion.security.PSSecurityProvider;
import com.percussion.security.PSSecurityProviderPool;
import com.percussion.server.PSRequest;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.sql.ResultSet;


/**
 * The PSAttributesCatalogHandler class implements cataloging of 
 * attributes. This request type is used to locate the attributes
 * associated with an object defined in the specified security provider.
 * <p>
 * Attribute catalog requests are sent to the server using the
 * PSXSecurityAttributesCatalog XML document. Its definition is as follows:
 * <pre>
 *    &lt;!ELEMENT PSXSecurityAttributesCatalog (instanceName, objectType*)&gt;
 *
 *    &lt;-- the name of the security provider instance to use for the query.
 *     --&gt;
 *    &lt;!ELEMENT instanceName      (#PCDATA)&gt;
 *
 *    &lt;-- 
 *       the type of object to locate attributes for. By specifying
 *         multiple objectType elements, the attribute list for multiple
 *       object types can be retrieved.
 *     --&gt;
 *    &lt;!ELEMENT objectType       (#PCDATA)&gt;
 * </pre>
 *
 * The PSXSecurityAttributesCatalogResults XML document is sent as the
 * response. Its definition is as follows:
 * <pre>
 *    &lt;!ELEMENT PSXSecurityAttributesCatalogResults (instanceName, Attributes*)&gt;
 *
 *    &lt;-- the name of the security provider instance which was queried.
 *     --&gt;
 *    &lt;!ELEMENT instanceName      (#PCDATA)&gt;
 *
 *    &lt;!ELEMENT Attributes         (name*)&gt;
 *
 *    &lt;-- 
 *       objectType - the type of security object the attributes are for.
 *     --&gt;
 *    &lt;!ATTLIST Attributes
 *       objectType      CDATA         #REQUIRED
 *    &gt;
 *
 *    &lt;-- the name of the attribute.
 *     --&gt;
 *    &lt;!ELEMENT name               (#PCDATA)&gt;
 * </pre>
 *
 * @author     Tas Giakouminakis
 * @version    1.0
 * @since      1.0
 */
public class PSAttributesCatalogHandler
   extends com.percussion.design.catalog.PSCatalogRequestHandler
   implements com.percussion.design.catalog.IPSCatalogRequestHandler
{
   /**
    * Constructs an instance of this handler.
    */
   public PSAttributesCatalogHandler()
   {
      super();
   }


   /* ********  IPSCatalogRequestHandler Interface Implementation ******** */

   /**
    * Get the request type(s) (XML document types) supported by this
    * handler.
    * 
    * @return      the supported request type(s)
    */
   public String[] getSupportedRequestTypes()
   {
      return new String[] { ms_RequestDTD };
   }


   /* ************ IPSRequestHandler Interface Implementation ************ */

   /**
    * Process the catalog request. This uses the XML document sent as the
    * input data. The results are written to the specified output
    * stream using the appropriate XML document format.
    *
    * @param   request     the request object containing all context
    *                      data associated with the request
    */
   public void processRequest(PSRequest request)
   {
      Document doc = request.getInputDocument();
      Element root = null;
      if (   (doc == null) ||
            ((root = doc.getDocumentElement()) == null) ) {
         Object[] args = { ms_RequestCategory, ms_RequestType, ms_RequestDTD };
         createErrorResponse(
            request, new PSIllegalArgumentException(
               IPSCatalogErrors.REQ_DOC_MISSING, args));
         return;
      }

      /* verify this is the appropriate request type */
      if (!ms_RequestDTD.equals(root.getTagName())) {
         Object[] args = { ms_RequestDTD, root.getTagName() };
         createErrorResponse(
            request, new PSIllegalArgumentException(
               IPSCatalogErrors.REQ_DOC_INVALID_TYPE, args));
         return;
      }

      PSXmlTreeWalker tree = new PSXmlTreeWalker(doc);

      String instanceName = tree.getElementData("instanceName");

      /* parse the tableType string and build an array from it */
      java.util.ArrayList typeList = new java.util.ArrayList();
      while (tree.getNextElement("objectType", true, false) != null) {
         typeList.add(tree.getElementData((Element)tree.getCurrent()));
      }
      String[] objectTypes = null;
      final int typeListSize = typeList.size();
      if (typeListSize != 0)
      {
         objectTypes = new String[typeListSize];
         typeList.toArray(objectTypes);
      }

      Document   retDoc = PSXmlDocumentBuilder.createXmlDocument();

      root = PSXmlDocumentBuilder.createRoot(retDoc, (ms_RequestDTD + "Results"));

      if (instanceName != null)
         PSXmlDocumentBuilder.addElement(retDoc, root, "instanceName", instanceName);

      if (objectTypes != null) {
         for (int i = 0; i < objectTypes.length; i++)
            PSXmlDocumentBuilder.addElement(retDoc, root, "objectType", objectTypes[i]);
      }

      ResultSet rs = null;
      try {
         PSSecurityProvider sp
            = PSSecurityProviderPool.getProvider(instanceName);
         if (sp != null) {
            IPSSecurityProviderMetaData meta = sp.getMetaData();
            rs = meta.getAttributes(objectTypes);
            while (rs.next()) {
               String objectType = rs.getString(1);
               String attrName = rs.getString(2);

               Element attrNode = PSXmlDocumentBuilder.addEmptyElement(
                  retDoc, root, "Attributes");
               attrNode.setAttribute("type", objectType);
               PSXmlDocumentBuilder.addElement(
                  retDoc, attrNode, "name", attrName);
            }
         }

         /* and send the result to the caller */
         sendXmlData(request, retDoc);
      } catch (java.sql.SQLException e) {
         createErrorResponse(request, e);
      } finally {
         if (rs != null) {
            try { rs.close(); }
            catch (java.sql.SQLException e) { /* ignore this */ }
         }
      }
   }

   /**
    * Shutdown the request handler, freeing any associated resources.
    */
   public void shutdown()
   {
      /* nothing to do here */
   }

   private static final String   ms_RequestCategory   = "security";
   private static final String   ms_RequestType         = "Attributes";
   private static final String   ms_RequestDTD         = "PSXSecurityAttributesCatalog";
}

