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

package com.percussion.design.catalog.system.server;

import com.percussion.content.PSContentFactory;
import com.percussion.design.catalog.IPSCatalogErrors;
import com.percussion.design.catalog.IPSCatalogRequestHandler;
import com.percussion.design.catalog.PSCatalogRequestHandler;
import com.percussion.error.PSIllegalArgumentException;
import com.percussion.server.PSRequest;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * This class is the server-side implementation to handle requests generated by 
 * the {@link com.percussion.design.catalog.system.PSMimeTypeCatalogHandler}.  
 * See that class for more info.
 */
public class PSMimeTypeCatalogHandler
   extends PSCatalogRequestHandler
   implements IPSCatalogRequestHandler
{
   /**
    * Constructs an instance of this handler. This is used primarily
    * by the cataloger.
    */
   public PSMimeTypeCatalogHandler()
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
      return new String[] {ms_requestDTD};
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
      if ((doc == null) || ((root = doc.getDocumentElement()) == null))
      {
         Object[] args = {ms_requestCategory, ms_requestType, ms_requestDTD};
         createErrorResponse(
            request, new PSIllegalArgumentException(IPSCatalogErrors.
                  REQ_DOC_MISSING, args));
         return;
      }

      /* verify this is the appropriate request type */
      if (!ms_requestDTD.equals(root.getTagName()))
      {
         Object[] args = {ms_requestDTD, root.getTagName()};
         createErrorResponse(request,
            new PSIllegalArgumentException(IPSCatalogErrors.
                  REQ_DOC_INVALID_TYPE, args));
         return;
      }

      Document retDoc = PSXmlDocumentBuilder.createXmlDocument();

      root = PSXmlDocumentBuilder.createRoot(retDoc, ms_requestDTD +
            "Results");

      // load the mimetypes
      String[] mimetypes = PSContentFactory.getSupportedMimeTypes();
      for (String mimetype : mimetypes)
      {
         Element mtEl = PSXmlDocumentBuilder.addEmptyElement(doc, root, 
         "mimetype");
         
         PSXmlDocumentBuilder.addElement(doc, mtEl, "name", mimetype);
      }
               
      /* and send the result to the caller */
      sendXmlData(request, retDoc);
   }

   /**
    * Shutdown the request handler, freeing any associated resources.
    */
   public void shutdown()
   {
      /* nothing to do here */
   }

   private static final String   ms_requestCategory   = "system";
   private static final String   ms_requestType       = "MimeType";
   private static final String   ms_requestDTD        = "PSXMimeTypeCatalog";
}

