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
package com.percussion.process;

import com.percussion.design.objectstore.PSUnknownNodeTypeException;
import com.percussion.util.PSXMLDomUtil;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Objects;

/**
 * This class represents a parameter definition used by a process def.  See
 * {@link PSProcessDef} for more info.  This class is immutable.
 */
public final class PSParamDef
{
   /**
    * Construct this object from its XML representation.
    *   
    * @param source The element containing the XML representation, may not be
    * {@code null}.  See {@link #toXml(Document)} for more info.
    *
    * @throws PSProcessException if the source element is malformed or if there 
    * are any errors.
    */
   public PSParamDef(Element source) throws PSProcessException
   {
      Objects.requireNonNull(source, "source cannot be null");

      try
      {
         PSXMLDomUtil.checkNode(source, XML_NODE_NAME);
         
         m_name = PSXMLDomUtil.getAttributeTrimmed(source, ATTR_NAME);
         m_value = new PSResolvableValue(source);
         m_ifDefinedName = PSXMLDomUtil.getAttributeTrimmed(source, ATTR_IFDEF);
         m_separator = PSXMLDomUtil.getAttributeTrimmed(source, ATTR_SEPARATOR);
      }
      catch (PSUnknownNodeTypeException e)
      {
         throw new PSProcessException(e.getLocalizedMessage());
      }
   }
   
   /**
    * Get the name of this param.
    * 
    * @return The name, may be {@code null}, never empty.
    */
   public String getName()
   {
      return m_name;
   }
   
   /**
    * Get the resolvable value of this param.
    * 
    * @return The value, never {@code null}.
    */
   public PSResolvableValue getValue()
   {
      return m_value;
   }
   
   /**
    * Get the name of the variable that must be defined for this parameter to
    * be included in the process request. 
    * 
    * @return The name, may be {@code null}, never empty.
    */
   public String getIfDefinedName()
   {
      return m_ifDefinedName;
   }
   
   /**
    * Get the separator to use when formatting the command arguments from the
    * name and value.  If {@code null}, a space is assumed which will
    * result in two separate command arguments for name and value.  If not,
    * then a single command argument is formed by concatenating 
    * name + separator + value.
    * 
    * @return The separator, may be {@code null}, never empty.
    */
   public String getSeparator()
   {
      return m_separator;
   }
   
   /**
    * Serializes this object to its XML representation.
    * 
    * @param doc The document to use, may not be {@code null}.
    *
    * @return The element containing this object's state, never {@code null}.
    *
    * @throws IllegalArgumentException if doc is {@code null}.
    */
   public Element toXml(Document doc)
   {
      Objects.requireNonNull(doc, "document cannot be null");

      var el = doc.createElement(XML_NODE_NAME);
      el.setAttribute(ATTR_NAME, m_name);
      el.appendChild(m_value.toXml(doc));

      if (m_ifDefinedName != null)
         el.setAttribute(ATTR_IFDEF, m_ifDefinedName);

      if (m_separator != null)
         el.setAttribute(ATTR_SEPARATOR, m_separator);

      return el;
   }

   /**
    * Compares this object with another for equality.
    *
    * @param obj The object to compare with.
    *
    * @return {@code true} if the objects are equal, {@code false} otherwise.
    */
   @Override
   public boolean equals(Object obj)
   {
      if (this == obj) return true;
      if (obj == null || getClass() != obj.getClass()) return false;

      var other = (PSParamDef) obj;
      return Objects.equals(m_name, other.m_name) &&
             Objects.equals(m_value, other.m_value) &&
             Objects.equals(m_ifDefinedName, other.m_ifDefinedName) &&
             Objects.equals(m_separator, other.m_separator);
   }

   /**
    * Returns the hash code for this object.
    *
    * @return The hash code.
    */
   @Override
   public int hashCode()
   {
      return Objects.hash(m_name, m_value, m_ifDefinedName, m_separator);
   }

   /**
    * Returns a string representation of this object.
    *
    * @return The string representation.
    */
   @Override
   public String toString()
   {
      return "PSParamDef{" +
             "name='" + m_name + '\'' +
             ", value=" + m_value +
             ", ifDefinedName='" + m_ifDefinedName + '\'' +
             ", separator='" + m_separator + '\'' +
             '}';
   }

   /**
    * The name of this parameter, may be {@code null}, never empty.
    */
   private final String m_name;

   /**
    * The resolvable value of this parameter, never {@code null}.
    */
   private final PSResolvableValue m_value;

   /**
    * The name of the variable that must be defined for this parameter to be
    * included in the process request, may be {@code null}, never empty.
    */
   private final String m_ifDefinedName;

   /**
    * The separator to use when formatting command arguments, may be {@code null},
    * never empty.
    */
   private final String m_separator;

   /**
    * The XML node name for this element.
    */
   public static final String XML_NODE_NAME = "PSXParam";
   
   /**
    * The name attribute.
    */
   private static final String ATTR_NAME = "name";

   /**
    * The ifDefined attribute.
    */
   private static final String ATTR_IFDEF = "ifDefined";

   /**
    * The separator attribute.
    */
   private static final String ATTR_SEPARATOR = "separator";
}
