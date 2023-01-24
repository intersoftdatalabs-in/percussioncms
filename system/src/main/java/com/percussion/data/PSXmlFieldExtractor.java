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

package com.percussion.data;

import com.percussion.server.PSRequest;
import com.percussion.util.PSPurgableTempFile;
import com.percussion.xml.PSXmlTreeWalker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import java.net.MalformedURLException;
import java.net.URL;


/**
 * The PSXmlFieldExtractor class is used to extract data from the
 * XML document associated with the request.
 * 
 * @author     Tas Giakouminakis
 * @version    1.0
 * @since      1.0
 */
public class PSXmlFieldExtractor extends PSDataExtractor
{
   /**
    * Construct an object from its object store counterpart.
    *
    * @param   source      the object defining the source of this value
    */
   public PSXmlFieldExtractor(
      com.percussion.design.objectstore.PSXmlField source)
   {
      super(source);
      m_source = source.getName();
      setXmlFieldBase(null);   // no base specified at construction
   }

   /**
    * Extract a data value using the run-time data.
    *
    * @param   data    the execution data associated with this request.
    *                      This includes all context data, result sets, etc.
    *
    * @return               the associated value; <code>null</code> if a
    *                        value is not found
    */
   public Object extract(PSExecutionData data)
      throws PSDataExtractionException
   {
      return extract(data, null);
   }

   /**
    * Extract a data value using the run-time data.
    *
    * @param   data    the execution data associated with this request.
    *                      This includes all context data, result sets, etc.
    *
    * @param   defValue      the default value to use if a value is not found
    *
    * @return               the associated value; <code>defValue</code> if a
    *                        value is not found
    */
   public Object extract(PSExecutionData data, Object defValue)
      throws PSDataExtractionException
   {
      Object value = null;

      PSXmlTreeWalker walker = data.getInputDocumentWalker();
      if (walker == null) {
         PSRequest request = data.getRequest();
         if (request != null)
         {
            Document doc = request.getInputDocument();
            if (doc != null)
            {
               walker = new PSXmlTreeWalker(doc);
               data.setInputDocumentWalker(walker);
            }
         }
      }

      if (walker != null)
      {
         // if this does not have a base node (which depends on the
         // walker being positioned correctly) we can read from the root
         String base = m_sourceFromBase == null ? m_source : m_sourceFromBase;
         String strVal = walker.getElementData(base, false);

         if (((strVal == null) || (strVal.length() == 0)) && !(base.indexOf("@") >=0) )
         {
            Node holdCur = walker.getCurrent();
            
            // see if it's a file reference
            Element ele = walker.getNextElement(base, true);

            walker.setCurrent(holdCur);
            
            String urlRef = null;

            if (ele != null)
               urlRef = ele.getAttribute(XML_URL_REFERENCE_ATTRIBUTE);
               
            if ((urlRef != null) && (urlRef.length() > 0))
            {
               PSPurgableTempFile f =
                  data.getRequest().getTempFileResource(urlRef);
                  
               if (f == null)
                  try {
                     URL url = new URL(urlRef);
                     value = url;
                  } catch(MalformedURLException e)
                  {
                     Object[] args = {XML_URL_REFERENCE_ATTRIBUTE, 
                        URL.class.getName(), e.toString()};

                     throw new PSDataExtractionException(
                        IPSDataErrors.DATA_CANNOT_CONVERT_WITH_REASON,
                        args);
                  }
               else
                  value = f;
            } else
               value = strVal;
         } else
            value = strVal;
      }

      return (value == null) ? defValue : value;
   }

   /**
    * Set the base from which this XML field will be extracted. When
    * extracting from XML fields, 
    *
    * @param   base         the base field name to use
    */
   public void setXmlFieldBase(String base)
   {
      /* part of fix for bug id TGIS-4BWSL9
       *
       * when we have conditionals and other such context insensitive
       * objects, we need to deal with their being no base. In those
       * cases, we need to get the XML field from the root
       */
      if (base == null)
      {
         m_sourceFromBase = null;
         if (!m_source.startsWith("/"))
            m_source = "/" + m_source;
      }
      else
      {
         String source;
         if (m_source.startsWith("/"))
            source = m_source.substring(1);
         else
            source = m_source;

         m_sourceFromBase
            = PSXmlTreeWalker.getRelativeFieldName(base, source);
         if (m_sourceFromBase != null)
            m_source = source;
      }
   }


   private String m_source;
   private String m_sourceFromBase;
   public static final String XML_URL_REFERENCE_ATTRIBUTE = "PSXUrlReferenceAttribute";
}

