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

package com.percussion.design.catalog.data.server;

import com.percussion.error.PSSqlException;
import com.percussion.design.catalog.IPSCatalogErrors;
import com.percussion.design.catalog.IPSCatalogRequestHandler;
import com.percussion.error.PSIllegalArgumentException;
import com.percussion.server.PSRequest;
import com.percussion.utils.jdbc.IPSConnectionInfo;
import com.percussion.utils.jdbc.PSConnectionDetail;
import com.percussion.utils.jdbc.PSConnectionHelper;
import com.percussion.utils.jdbc.PSConnectionInfo;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;


/**
 * The PSIndexCatalogHandler class implements cataloging of
 * indexes. This request type is used to locate the indexes defined
 * on a table. Indexes are used to sort data. This allows for faster
 * access to the data. They can also be used to enforce unique column
 * values.
 * <p>
 * The request format is defined in the
 * {@link com.percussion.design.catalog.data.PSIndexCatalogHandler
 *  com.percussion.design.catalog.data.PSIndexCatalogHandler} class.
 */
public class PSIndexCatalogHandler
   extends com.percussion.design.catalog.PSCatalogRequestHandler
   implements IPSCatalogRequestHandler
{
   /**
    * Constructs an instance of this handler.
    */
   public PSIndexCatalogHandler()
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
      return new String[] { "PSXIndexCatalog" };
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

      String datasource = tree.getElementData("datasource");
      String table = tree.getElementData("tableName");
      
      Document   retDoc = PSXmlDocumentBuilder.createXmlDocument();

      root = PSXmlDocumentBuilder.createRoot(retDoc, "PSXIndexCatalogResults");

      PSXmlDocumentBuilder.addElement(retDoc, root, "datasource",
         datasource == null ? "" : datasource);

      if (table != null)
         PSXmlDocumentBuilder.addElement(retDoc, root, "tableName", table);

      Connection conn;
      PSConnectionDetail detail;
      IPSConnectionInfo connInfo = new PSConnectionInfo(datasource);
      try
      {
         conn = PSConnectionHelper.getDbConnection(connInfo);
         detail = PSConnectionHelper.getConnectionDetail(connInfo);
      }
      catch (Exception e)
      {
         createErrorResponse(request, e);
         return;
      }

      Element node;
      Element indexNode = null;
      boolean indexUnique;
      String indexName, colName, colSort;
      short colPos;
      String lastIndex = "noSuchIndexNamePossible";
      try
      {
         DatabaseMetaData meta = conn.getMetaData();
         ResultSet rs = meta.getIndexInfo(detail.getDatabase(), 
            detail.getOrigin(), table, false, true);
         if (rs != null)
         {
            while (rs.next())
            {
               /* THESE MUST BE READ IN THE CORRECT SEQUENCE !!! */
               indexUnique = rs.getBoolean(COLNO_UNIQUE);
               indexName = rs.getString(COLNO_INDEX_NAME);
               if (rs.getShort(COLNO_INDEX_TYPE) == 
                  DatabaseMetaData.tableIndexStatistic)
               {
                  continue; /* not a real index, move on */
               }
               colPos = rs.getShort(COLNO_COL_POS);
               colName = rs.getString(COLNO_COL_NAME);
               colSort = rs.getString(COLNO_COL_SORT);

               if (!lastIndex.equals(indexName))
               {
                  indexNode = PSXmlDocumentBuilder.addEmptyElement(retDoc,
                     root, "Index");
                  lastIndex = indexName;
                  indexNode.setAttribute("enforceUniqueness", (indexUnique? 
                     "yes" : "no"));
                  PSXmlDocumentBuilder.addElement(retDoc, indexNode, "name",
                     indexName);
               }

               node = PSXmlDocumentBuilder.addElement(retDoc, indexNode,
                  "indexColumn", colName);

               node.setAttribute("position", String.valueOf(colPos));

               if (colSort == null)
                  node.setAttribute("sorting", "unk");
               else if (colSort.equals("A"))
                  node.setAttribute("sorting", "asc");
               else if (colSort.equals("D"))
                  node.setAttribute("sorting", "desc");
               else
                  node.setAttribute("sorting", "unk");
            }

            rs.close();
         }
      }
      catch (SQLException e)
      {
         // if this feature is not supported, just return the empty doc
         // otherwise, treat this as a real error
         if (!PSSqlException.hasFeatureNotSupported(e, connInfo))
         {
            createErrorResponse(request, e);
            return;
         }
      }
      finally
      {
         try
         {
            conn.close();
         }
         catch (SQLException e)
         {
         }
      }

      /* send the result to the caller */
      sendXmlData(request, retDoc);
   }

   /**
    * Shutdown the request handler, freeing any associated resources.
    */
   public void shutdown()
   {
      /* nothing to do here */
   }


   private static final String   ms_RequestCategory   = "data";
   private static final String   ms_RequestType         = "Index";
   private static final String   ms_RequestDTD         = "PSXIndexCatalog";

   private static final int COLNO_UNIQUE      = 4;
   private static final int COLNO_INDEX_NAME   = 6;
   private static final int COLNO_INDEX_TYPE   = 7;
   private static final int COLNO_COL_POS      = 8;
   private static final int COLNO_COL_NAME   = 9;
   private static final int COLNO_COL_SORT   = 10;
}

