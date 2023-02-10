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

package com.percussion.design.catalog.exit;

import com.percussion.conn.PSServerException;
import com.percussion.design.catalog.IPSCatalogHandler;
import com.percussion.design.catalog.PSCataloger;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSExtensionDefFactory;
import com.percussion.extension.PSExtensionDefFactory;
import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;
import java.util.Properties;


/**
 * This class implements cataloging of extension handlers
 * installed on the server.
 * <p>
 * Extension handler catalog requests are sent to the server
 * using the PSXExtensionCatalog XML document. Its definition
 * is as follows:
 * <pre><code>
 *
 *    &lt;!ELEMENT PSXExtensionCatalog EMPTY&gt;
 *
 * <pre><code>
 *
 * The PSXExtensionCatalogResults XML document is sent
 * as the response. Its definition is as follows:
 * <pre><code>
 *
 *    &lt;!ELEMENT PSXExtensionCatalogResults   (IPSExtensionDef*)&gt;
 *
 * <pre><code>
 */
public class PSExtensionCatalogHandler implements IPSCatalogHandler
{
   /**
    * Constructs an instance of this handler. This is used primarily
    * by the cataloger.
    */
   public PSExtensionCatalogHandler()
   {
      super();
   }

   /**
    * Format the catalog request based upon the specified
    * request information. The request information for this
    * request type is:
    *
    * <table border="1">
    * <tr>
    *      <th>Key</th>
    *      <th>Value</th>
    *      <th>Required</th>
    *   </tr>
    * <tr>
    *      <td>RequestCategory</td>
    *      <td>exit</td>
    *      <td>yes</td>
    *   </tr>
    * <tr>
    *      <td>RequestType</td>
    *      <td>Extension</td>
    *      <td>yes</td>
    *   </tr>
    * <tr>
    *      <td>RequestType</td>
    *      <td>ExtensionHandlerName</td>
    *      <td>yes</td>
    *   </tr>
    * </table>
    *
    * @param   req         the request information
    *
    * @return               an XML document containing the appropriate
    *                        catalog request information
    */
   public org.w3c.dom.Document formatRequest(Properties req)
   {
      String sTemp = (String)req.get("RequestCategory");
      if ( (sTemp == null) || !"exit".equalsIgnoreCase(sTemp) ) {
         throw new IllegalArgumentException("req category invalid: exit or null");
      }

      sTemp = (String)req.get("RequestType");
      if ( (sTemp == null) || !"Extension".equalsIgnoreCase(sTemp) ) {
         throw new IllegalArgumentException("req type invalid: Extension or null");
      }

      sTemp = (String)req.get("ExtensionHandlerPattern");
      if ( (sTemp == null) || (sTemp.length() == 0) ) {
         throw new IllegalArgumentException("reqd prop not specified: ExtensionHandlerPattern or null");
      }

      Document reqDoc = PSXmlDocumentBuilder.createXmlDocument();

      Element root = PSXmlDocumentBuilder.createRoot(reqDoc, "PSXExtensionCatalog");

      // pass all properties in the supplied list thru
      Enumeration keys = req.propertyNames();
      while ( keys.hasMoreElements())
      {
         String key = (String) keys.nextElement();
         if ( !(key.equals( "RequestCategory" ) || key.equals( "RequestType" )))
            PSXmlDocumentBuilder.addElement(reqDoc, root, key, req.getProperty(key));
      }

      return reqDoc;
   }


   /**
    * Convenience method that doesn't limit the types of interfaces or
    * contexts. See {@link #getCatalog(PSCataloger,String,String,String)
    * getCatalog} for details.
    */
   public static IPSExtensionDef[] getCatalog(
      PSCataloger cataloger, String handlerName )
      throws
         com.percussion.conn.PSServerException,
         com.percussion.security.PSAuthenticationFailedException,
         com.percussion.security.PSAuthorizationException,
         java.io.IOException
   {
      return getCatalog( cataloger, handlerName, null, null );
   }


   /**
    * Get an array containing the extensions installed
    * on the server. This is a convenience method which makes a
    * call to the cataloger's catalog method using the appropriate
    * properties for this request type.
    *
    * @param   cataloger      a cataloger containing a connection to
    *                           the Rhythmyx server we will catalog through
    *
    * @param   handlerName      the name of the extension handler to catalog
    *
    * @param   context        The extension context to look in. This cannot
    *                         be a pattern. Pass in <code>null</code> to get all
    *                         contexts.
    *
    * @param   interfacePattern
    *                         A filter to limit the types of extensions
    *                         returned. Pass in <code>null</code> to get all
    *                         interfaces. Use SQL search syntax for any patterns.
    *
    * @return                  an array containing the extension handlers
    *                           installed on the server.
    *
    * @exception   PSServerException
    *                           if the server is not responding.
    *
    * @exception   PSAuthenticationFailedException
    *                           if the credentials specified for the
    *                           server connection are invalid.
    *
    * @exception   PSAuthorizationException
    *                           if the user does not have designer or
    *                           administrator access to the server.
    *
    * @exception   IOException
    *                           if a communication error occurs while
    *                           processing the request
    */
   public static IPSExtensionDef[] getCatalog(
      PSCataloger cataloger, String handlerName, String context,
      String interfacePattern )
      throws
         com.percussion.conn.PSServerException,
         com.percussion.security.PSAuthenticationFailedException,
         com.percussion.security.PSAuthorizationException,
         java.io.IOException
   {
      IPSExtensionDef[] ret = null;

      // create the properties
      java.util.Properties req = new java.util.Properties();

      req.put( "RequestCategory", "exit" );
      req.put( "RequestType", "Extension" );
      req.put( "ExtensionHandlerPattern", handlerName );
      if ( null != context )
         req.put( "Context", context );
      if ( null != interfacePattern )
         req.put( "InterfacePattern", interfacePattern );

      // perform the catalog request
      Document doc = null;
      try{
         doc = cataloger.catalog(req);
      } catch (IllegalArgumentException e){
         throw new PSServerException(e);
      }

      /* store the extension definitions in a list.
       * The returned XML tree contains the standard root node
       * (which we can ignore) then each extension
       * (where each extension is a child of the root, but siblings to
       * each other). To walk the tree we can get the root node
       * create a walker for it. We can get the first child to get
       * the first extension then iterate siblings to get
       * all subsequent extensions.
       */
      Element root = doc.getDocumentElement();
      if (root != null)
      {
         List l = new ArrayList();
         PSXmlTreeWalker w = new PSXmlTreeWalker(doc);
         IPSExtensionDefFactory factory = new PSExtensionDefFactory();
         for (   Element e = w.getNextElement(PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN
                  | PSXmlTreeWalker.GET_NEXT_ALLOW_SIBLINGS);
               e != null;
               e = w.getNextElement(PSXmlTreeWalker.GET_NEXT_ALLOW_SIBLINGS) )
         {
            try
            {
               l.add( factory.fromXml( e ));
            }
            catch (Exception exc)
            {
               throw new PSServerException(exc);
            }
         }

         // and convert the list to an array
         final int size = l.size();
         ret = new IPSExtensionDef[size];
         if (size > 0)
            l.toArray(ret);
      }
      else
      {   // create an empty one - no extension defs!
         ret = new IPSExtensionDef[0];
      }

      return ret;
   }
}

