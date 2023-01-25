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

package com.percussion.ant.install;

import com.percussion.install.PSLogger;
import com.percussion.xml.PSXmlDocumentBuilder;
import org.apache.tools.ant.taskdefs.condition.Condition;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilder;
import java.io.File;

/**
 * This class is used to determine if a particular element exists in the xml
 * file and its value is equal to a specific value.
 *
 * Returns <code>true</code> if the xml file specified by
 * <code>relativeFilePath</code> member variable has an element named
 * <code>xmlElementName</code> whose value is equal to
 * <code>xmlElementValue</code>. The comparision is case-sensitive
 * if <code>ignoreCase</code> member is <code>false</code> otherwise it is
 * case-insensitive.
 *
 * <br>
 * Example Usage:
 * <br>
 * <pre>
 *
 * First set the typedef:
 *
 *  <code>
 *  &lt;typedef name="xmlElementCondition"
 *              class="com.percussion.ant.install.PSXMLElementCondition"
 *              classpathref="INSTALL.CLASSPATH"/&gt;
 *  </code>
 *
 * Now use the task.
 *
 *  <code>
 *  &lt;condition property="SPRINTA_DRIVER_EXISTS"&gt;
 *     &lt;xmlElementCondition
 *        ignoreCase="true"
 *        relativeFilePath="rxconfig/Server/config.xml"
 *        xmlElementName="jdbcDriverName"
 *        xmlElementValue="inetdae7"/&gt;
 *  &lt;/condition&gt;
 *  </code>
 *
 * </pre>
 *
 */
public class PSXMLElementCondition extends PSAction implements Condition
{
   /* (non-Javadoc)
    * @see org.apache.tools.ant.taskdefs.condition.Condition#eval()
    */
   public boolean eval()
   {
      String installDir = getRootDir();
      if ((installDir == null) || (installDir.trim().length() == 0))
         return false;

      if (!installDir.endsWith(File.separator))
         installDir += File.separator;
      String strXmlFile = installDir + relativeFilePath;
      File xmlFile = new File(strXmlFile);
      if (!xmlFile.exists())
      {
         PSLogger.logInfo("file does not exist : " + strXmlFile);
         return false;
      }

      Document doc = null;
      DocumentBuilder db = PSXmlDocumentBuilder.getDocumentBuilder(false);

      try
      {
         File f = new File(strXmlFile);
         doc = db.parse(f);
         if (doc == null)
            return false;
         Element root = doc.getDocumentElement();
         if (root == null)
            return false;
         NodeList nl = root.getElementsByTagName(xmlElementName);
         if (nl == null)
            return false;
         int nodeListLen = nl.getLength();
         Element el = null;
         String elText = "";
         for (int j = 0; j < nodeListLen; j++)
         {
            el = (Element)nl.item(j);
            elText = getElementData(el);
            if (!((elText == null) || (elText.trim().length() == 0)))
            {
               if (ignoreCase)
               {
                  if (xmlElementValue.equalsIgnoreCase(elText))
                     return true;
               }
               else
               {
                  if (xmlElementValue.equals(elText))
                     return true;
               }
            }
         }
      }
      catch (Exception e)
      {
         PSLogger.logInfo("Exception in PSXMLElementCondition : "
            + e.getMessage());
         PSLogger.logInfo(e);
         return false;
      }
      return false;
   }

  /***************************************************************
  * Mutators and Accessors
  ***************************************************************/

  /**
   * Returns the relative path of the xml file from the installation directory.
   * @return the relative path of the xml file from the installation directory,
   * never <code>null</code> or empty
   */
   public String getRelativeFilePath()
   {
      return relativeFilePath;
   }

   /**
    * Sets the relative path of the xml file from the installation directory.
    * @param relativeFilePath the relative path of the xml file from the
    * installation directory, never <code>null</code> or empty
    * @throw IllegalArgumentException if relativeFilePath is <code>null</code>
    * or empty
    */
   public void setRelativeFilePath(String relativeFilePath)
   {
      if ((relativeFilePath == null) || (relativeFilePath.trim().length() == 0))
         throw new IllegalArgumentException(
            "relativeFilePath may not be null or empty");
      this.relativeFilePath = relativeFilePath;
   }

  /**
   * Returns the xml element whose value is to be matched.
   * @return the xml element whose value is to be matched,
   * never <code>null</code> or empty
   */
   public String getXmlElementName()
   {
      return xmlElementName;
   }

   /**
    * Sets the xml element whose value is to be matched.
    * @param xmlElementName the xml element whose value is to be matched,
    * never <code>null</code> or empty
    * @throw IllegalArgumentException if xmlElementName is <code>null</code>
    * or empty
    */
   public void setXmlElementName(String xmlElementName)
   {
      if ((xmlElementName == null) || (xmlElementName.trim().length() == 0))
         throw new IllegalArgumentException(
            "xmlElementName may not be null or empty");
      this.xmlElementName = xmlElementName;
   }

  /**
   * Returns the value of the xml element to match.
   * @return the value of the xml element to match,
   * never <code>null</code> or empty
   */
   public String getXmlElementValue()
   {
      return xmlElementValue;
   }

   /**
    * Sets the value of the xml element to match.
    * @param xmlElementValue the value of the xml element to match,
    * never <code>null</code> or empty
    * @throw IllegalArgumentException if xmlElementValue is <code>null</code>
    * or empty
    */
   public void setXmlElementValue(String xmlElementValue)
   {
      if ((xmlElementValue == null) || (xmlElementValue.trim().length() == 0))
         throw new IllegalArgumentException(
            "xmlElementValue may not be null or empty");
      this.xmlElementValue = xmlElementValue;
   }

   /**
    * Returns the boolean value indicating if the comparision of the
    * XML Element values should be case-sensitive or not.
    * @return <code>true</code> if the comparision of XML Element values is
    * not case-sensitive, <code>false</code> otherwise.
    */
   public boolean getIgnoreCase()
   {
      return ignoreCase;
   }

   /**
    * Sets the boolean value indicating if the comparision of the
    * XML Element values should be case-sensitive or not.
    * @param ignoreCase <code>true</code> if the comparision of XML Element
    * values is not case-sensitive, <code>false</code> otherwise.
    */
   public void setIgnoreCase(boolean ignoreCase)
   {
      this.ignoreCase = ignoreCase;
   }

  /***************************************************************
  * Bean properties
  ***************************************************************/

   /**
    * stores the relative path of the xml file, never <code>null</code>
    * or empty
    */
   String relativeFilePath = "rxconfig/Server/config.xml";

   /**
    * the xml element whose value is to be matched,
    * never <code>null</code> or empty.
    */
   String xmlElementName = "jdbcDriverName";

   /**
    * the value of the xml element to match,
    * never <code>null</code> or empty.
    */
   String xmlElementValue = "inetdae7";

   /**
    * Determines if the string comparion of the xml element value should be
    * case-sensitive or not. If <code>true</code> it is case-insensitive,
    * otherwise it is case-sensitive.
    */
   boolean ignoreCase = true;

  /**************************************************************************
  * private function
  **************************************************************************/

   /**
    * Get the value (text data) associated with the specified node.
    * If the specified node is <code>null</code> or has no text data,
    * returns the empty string.
    *
    * @param node the element or entity ref node to retrieve the data from,
    * if it is <code>null</code>, returns an empty string.
    *
    * @return the value of the element, never <code>null</code> may be empty if
    * the specified element is <code>null</code> or has no text data.
    */
   private static String getElementData(Node node)
   {
      StringBuilder ret = new StringBuilder();
      Node text;

      if (node != null)
      {
         for (text = node.getFirstChild();
         text != null;
         text = text.getNextSibling() )
         {
            /* the item's value is in one or more text nodes which are
             * its immediate children
             */
            if (text.getNodeType() == Node.TEXT_NODE)
               ret.append(text.getNodeValue());
            else
               /***
                * DB: when there are embedded entities in element data, the
                * "Actual Value" of the entity will be contained in one or more
                * Text nodes as children of the entity ref node.  We call ourselves
                * recursively to process these additional nodes.
                ***/
               if (text.getNodeType() == Node.ENTITY_REFERENCE_NODE)
               {
                  ret.append(getElementData(text));
               }
         }
      }

      return ret.toString();
   }

}
