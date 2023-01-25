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
package com.percussion.webdav.objectstore;

import java.util.List;

import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.util.PSXMLDomUtil;
import com.percussion.webdav.error.IPSWebdavErrors;
import com.percussion.webdav.error.PSWebdavException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class PSPropertyFieldNameMapping extends PSWebdavComponent
{

   /**
    * Construct a new <code>PSWebdavProperty</code> object.
    * @param propertyName name of the property, cannot be <code>null</code> or
    * empty.
    * @param fieldName  fieldname for this property, cannot be <code>null</code>
    * or empty.
    */
   public PSPropertyFieldNameMapping(String propertyName, String fieldName)
   {
      if(null == propertyName || propertyName.trim().length() == 0)
         throw new IllegalArgumentException(
            "Property name cannot be null or empty.");
      if(null == fieldName || fieldName.trim().length() == 0)
         throw new IllegalArgumentException(
            "Field name cannot be null or empty.");

      m_propertyName = propertyName;
      m_fieldName = fieldName;
   }

   /**
    * Constructs a new property from its xml equivelent
    *
    * @param xml the xml Element for the property, cannot be <code>null</code>.
    *
    * @param capture flag indicating that validation exception should not
    * be thrown but instead be collected in a list for later use.
    * 
    * @param captureList the list to be used to store the captured validation
    * exceptions, if <code>null</code>, a default list will be used.
    * 
    * @throws PSWebdavException if an error occurs while creating this object
    * from xml.
    */
   public PSPropertyFieldNameMapping(
      Element xml, boolean capture, List captureList)
      throws PSWebdavException, PSUnknownNodeTypeException
   {
      setValidationExceptionCapture(capture);
      if(captureList != null)
         setValidationExceptionsList(captureList);
      fromXml(xml);
   }
   
   /**
    * Constructs a new property from its xml equivelent
    *
    * @param xml the xml Element for the property, cannot be <code>null</code>.
    *
    * @throws PSWebdavException if an error occurs while creating this object
    * from xml.
    */
   public PSPropertyFieldNameMapping(Element xml)
      throws PSWebdavException, PSUnknownNodeTypeException
   {
      fromXml(xml);
   }

   /**
    * Returns the field name for this property
    * @return name of the field name, never <code>null</code>
    * or empty.
    */
   public String getFieldName()
   {
      return m_fieldName;
   }

   // Implement IPSWebdavComponent interface method
   public Element toXml(Document doc)
   {
      if(null == doc)
         throw new IllegalArgumentException("Document cannot be null.");

      Element root = doc.createElement(getNodeName());
      root.setAttribute(IPSRxWebDavDTD.ATTR_NAME, getPropertyName());
      Element fieldname = doc.createElement(IPSRxWebDavDTD.ELEM_FIELDNAME);
      fieldname.appendChild(doc.createTextNode(getFieldName()));
      root.appendChild(fieldname);

      return root;
   }

   /**
    * Implements fromXml method for
    * {@link com.percussion.webdav.objectstore.IPSWebdavComponent}.
    * Expects the following xml format:
    * <p><pre>
    * &lt;!ELEMENT PSXPropertyFieldNameMapping (FieldName)&gt;
    * &lt;!ATTLIST PSXPropertyFieldNameMapping
    *  name CDATA #REQUIRED
    *  &gt;
    * &lt;!--
    * A field name
    * --&gt;
    * &lt;!ELEMENT FieldName (#PCDATA)&gt;
    * </pre></p>
    * @param src xml source element, cannot be <code>null</code>.
    */
   public void fromXml(Element src)
      throws PSWebdavException, PSUnknownNodeTypeException
   {
      if(null == src)
         throw new IllegalArgumentException("Source element cannot be null.");

      String propName = src.getAttribute(IPSRxWebDavDTD.ATTR_NAME);
      String fieldName = "";

      // Validate that property name is not empty
      if(propName.trim().length() == 0)
         handleValidationExceptions(
            new PSWebdavException(
               IPSWebdavErrors.XML_ATTRIBUTE_MUST_BE_SPECIFIED,
               new Object[] {IPSRxWebDavDTD.ATTR_NAME, getNodeName()}));


      
      try
      {
         Element fieldNameElem = PSXMLDomUtil.getFirstElementChild(
            src,
            IPSRxWebDavDTD.ELEM_FIELDNAME);
         
         fieldName = PSXMLDomUtil.getElementData(fieldNameElem);
      }
      catch (PSUnknownNodeTypeException ignore)
      {
        // This means the FieldName Element is completly missing
        // A validation error will get thrown because fieldname won't
        // get defined. So we just ignore this exception.
        
      }
         // Validate that fieldname is not empty
         if(fieldName.trim().length() == 0)
            handleValidationExceptions(
               new PSWebdavException(
                  IPSWebdavErrors.FIELDNAME_CANNOT_BE_EMPTY_OR_MISSING,
                  propName));

         m_propertyName = propName;
         m_fieldName = fieldName.trim();



   }

   // Implement IPSWebdavComponent interface method
   public String getNodeName()
   {
      return XML_NODE_NAME;
   }

   // Implement IPSWebdavComponent interface method
   public boolean equals(Object obj)
   {
      if(obj instanceof PSPropertyFieldNameMapping)
      {
         PSPropertyFieldNameMapping other = (PSPropertyFieldNameMapping) obj;

         return m_propertyName.equals(other.m_propertyName) &&
                m_fieldName.equals(other.m_fieldName);
      }
      return false;
   }

   // Implement IPSWebdavComponent interface method
   public int hashCode()
   {
       return m_propertyName.hashCode() +
          m_fieldName.hashCode();
   }


   /**
    * Returns the name of this property
    * @return name of the property
    */
   public String getPropertyName()
   {
      return m_propertyName;
   }


   /**
    * The fieldname for this property, set in the ctor or
    * {@link #fromXml(Node)}, never <code>null</code>
    * after that.
    */
   private String m_fieldName;

   /**
    * The name for this property, set in the ctor or
    * {@link #fromXml(Node)}, never <code>null</code>
    * after that.
    */
   private String m_propertyName;

   /**
    * Xml node name for this class
    */
   private final static String XML_NODE_NAME = 
      IPSRxWebDavDTD.ELEM_PROPERTYFIELD_MAPPING;

   }
