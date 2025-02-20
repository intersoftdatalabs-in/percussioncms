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

import com.percussion.xml.PSXmlDocumentBuilder;
import com.percussion.xml.PSXmlTreeWalker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.List;


/**
 * The PSTextLiteral class is used to define a replacement value is a 
 * static text literal value.
 *
 * @see         IPSReplacementValue
 *
 * @author      Tas Giakouminakis
 * @version    1.0
 * @since      1.0
 */
public class PSTextLiteral extends PSLiteral
      implements IPSMutatableReplacementValue
{
   /**
    * The value type associated with this instances of this class.
    */
   public static final String VALUE_TYPE = "TextLiteral";

   /**
    * Construct a Java object from its XML representation. See the
    * {@link #toXml(Document) toXml} method for a description of the XML object.
    *
    * @param      sourceNode      the XML element node to construct this
    *                              object from
    *
    * @param      parentDoc      the Java object which is the parent of this
    *                              object
    *
    * @param      parentComponents   the parent objects of this object
    *
    * @exception   PSUnknownNodeTypeException
    *                              if the XML element node is not of the
    *                              appropriate type
    */
   public PSTextLiteral(Element sourceNode, IPSDocument parentDoc, 
                        List parentComponents)
      throws PSUnknownNodeTypeException
   {
      fromXml(sourceNode, parentDoc, parentComponents);
   }

   /**
    * Constructs a literal.
    *
    * @param   text       the literal text
    */
   public PSTextLiteral(String text)
   {
      setText(text);
   }


   /**
    * Gets the literal text.
    *
    * @return the text, never <code>null</code>, may be empty.
    */
   public String getText()
   {
      return m_text;
   }
   
   /**
    * Sets the literal text.
    *
    * @param text the literal text to assign. if <code>null</code>, an empty
    * string is assigned
    */
   public void setText(String text)
   {
      if (null == text)
         text = "";

      m_text = text;
   }

   
   /* *********** IPSReplacementValue Interface Implementation *********** */

   /**
    * Get the type of replacement value this object represents.
    */
   public String getValueType()
   {
      return VALUE_TYPE;
   }

   /**
    * Get the text which can be displayed to represent this value.
    * @return the text, never <code>null</code>, may be empty.
    * @see #getText
    */
   public String getValueDisplayText()
   {
      return getText();
   }
   
   /**
    * Get the implementation specific text which for this value.
    * @return the text, never <code>null</code>, may be empty.
    * @see #getText
    */
   public String getValueText()
   {
      return getText();
   }


   /**
    * Sets the literal text.
    * @param text the literal text to assign. if <code>null</code>, an empty
    * string is assigned
    * @see #setText
    */ 
   public void setValueText(String text)
   {
      setText( text );
   }
   
   
   /* **************  IPSComponent Interface Implementation ************** */
   
   /**
    * This method is called to create a PSXTextLiteral XML element node
    * containing the data described in this object.
    * <p>
    * The structure of the XML document is:
    * <pre><code>
    *    &lt;!--
    *         PSXTextLiteral is used to define a replacement value is a 
    *         static literal value.
    *    --&gt;
    *    &lt;!ELEMENT PSXTextLiteral (text)&gt;
    *
    *    &lt;!--
    *       the static literal text.
    *    --&gt;
    *    &lt;!ELEMENT text         (#PCDATA)&gt;
    * </code></pre>
    *
    * @return     the newly created PSXTextLiteral XML element node
    */
   public Element toXml(Document doc)
   {
      Element   root = doc.createElement (ms_NodeType);
      root.setAttribute("id", String.valueOf(m_id));
      
      //create text element
      PSXmlDocumentBuilder.addElement(doc, root, "text", m_text);

      return root;
   }
   
   /**
    * This method is called to populate a PSTextLiteral Java object from a
    * PSXTextLiteral XML element node. See the
    * {@link #toXml(Document) toXml} method for a description of the XML object.
    *
    * @exception   PSUnknownNodeTypeException if the XML element node is not
    *                                        of type PSXTextLiteral
    */
   public void fromXml(Element sourceNode, IPSDocument parentDoc, 
                        List parentComponents)
      throws PSUnknownNodeTypeException
   {
      if (sourceNode == null)
         throw new PSUnknownNodeTypeException(
            IPSObjectStoreErrors.XML_ELEMENT_NULL, ms_NodeType);
      
      //make sure we got the ACL type node
      if (false == ms_NodeType.equals (sourceNode.getNodeName()))
      {
         Object[] args = { ms_NodeType, sourceNode.getNodeName() };
         throw new PSUnknownNodeTypeException(
            IPSObjectStoreErrors.XML_ELEMENT_WRONG_TYPE, args);
      }
      
      PSXmlTreeWalker tree = new PSXmlTreeWalker(sourceNode);
      
      String sTemp = tree.getElementData("id");
      try {
         m_id = Integer.parseInt(sTemp);
      } catch (Exception e) {
         Object[] args = { ms_NodeType, ((sTemp == null) ? "null" : sTemp) };
         throw new PSUnknownNodeTypeException(
            IPSObjectStoreErrors.XML_ELEMENT_INVALID_ID, args);
      }
      
      //read text from XML node
      setText(tree.getElementData("text"));
   }

   /**
    * Validates this object within the given validation context. The method
    * signature declares that it throws PSSystemValidationException, but the
    * implementation must not directly throw any exceptions. Instead, it
    * should register any errors with the validation context, which will
    * decide whether to throw the exception (in which case the implementation
    * of <CODE>validate</CODE> should not catch it unless it is to be
    * rethrown).
    *
    * @param   cxt The validation context.
    *
    * @throws PSSystemValidationException According to the implementation of the
    * validation context (on warnings and/or errors).
    */
   public void validate(IPSValidationContext cxt) throws PSSystemValidationException
   {
      super.validate(cxt);

      if (!cxt.startValidation(this, null))
         return;
   }

   public boolean equals(Object o)
   {
      if (!(o instanceof PSTextLiteral))
         return false;

      PSTextLiteral other = (PSTextLiteral)o;
      return compare( m_text, other.m_text );
   }
   
   /** Assigned in <code>setText</code>; never <code>null</code> */
   private String m_text = "";
   
  /** public access on this so it can be referenced by other packages */
  public static final String ms_NodeType = "PSXTextLiteral";
   
   /* (non-Javadoc)
    * @see java.lang.Object#hashCode()
    */
   public int hashCode()
   {
      return m_text.hashCode();
   }

}

