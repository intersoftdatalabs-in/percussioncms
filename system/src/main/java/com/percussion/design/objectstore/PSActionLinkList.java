/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package com.percussion.design.objectstore;

import com.percussion.xml.PSXmlTreeWalker;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.Iterator;
import java.util.List;

/**
 * Implementation for the PSXActionLinkList DTD in BasicObjects.dtd.
 */
public class PSActionLinkList extends PSCollectionComponent
{
   /**
    * Creates a new action link collection of PSAcionLink objects.
    *
    * @param actionlink an action link, not <code>null</code>.
    */
   public PSActionLinkList(PSActionLink actionLink)
   {
      super( PSActionLink.class );

      if (actionLink == null)
         throw new IllegalArgumentException("actionLink cannot be null");
      add(actionLink);
   }

   /**
    * Construct a Java object from its XML representation.
    *
    * @param sourceNode   the XML element node to construct this object from,
    *    not <code>null</code>.
    * @param parentDoc the Java object which is the parent of this object,
    *    not <code>null</code>.
    * @param parentComponents   the parent objects of this object, not
    *    <code>null</code>.
    * @throws PSUnknownNodeTypeException if the XML element node is not of
    *    the appropriate type
    */
   public PSActionLinkList(Element sourceNode, IPSDocument parentDoc,
                           List parentComponents)
      throws PSUnknownNodeTypeException
   {
      this();
      fromXml(sourceNode, parentDoc, parentComponents);
   }

   /**
    * Needed for serialization.
    */
   protected PSActionLinkList()
   {
      super( PSActionLink.class );
   }


   /**
    * @return a deep-copy clone of this vector.
    */
   @Override
   public synchronized Object clone()
   {
      PSActionLinkList copy = (PSActionLinkList) super.clone();
      copy.removeAllElements();
      for (int i = 0; i < size(); i++)
      {
         PSActionLink action = (PSActionLink) elementAt( i );
         copy.add( i, action.clone() );
      }
      return copy;
   }


   /**
    * Performs a shallow copy of the data in the supplied component to this
    * component. Derived classes should implement this method for their data,
    * calling the base class method first.
    *
    * @param c a valid PSField, not <code>null</code>.
    */
   public synchronized void copyFrom(PSActionLinkList c)
   {
      try
      {
         super.copyFrom(c);
      }
      catch (IllegalArgumentException e)
      {
         throw new IllegalArgumentException(e);
      }
   }

   /**
    * Test if the provided object and this are equal.
    *
    * @param o the object to compare to.
    * @return <code>true</code> if this and o are equal,
    *    <code>false</code> otherwise.
    */
   @Override
   public boolean equals(Object o)
   {
      if (!(o instanceof PSActionLinkList))
         return false;

      PSActionLinkList t = (PSActionLinkList) o;

      boolean equal = true;

      if (size() != t.size())
         equal = false;
      else
      {
         for (int i=0; i<size() && equal; i++)
         {
            IPSComponent c1 = (IPSComponent) get(i);
            IPSComponent c2 = (IPSComponent) t.get(i);
            if (!PSComponent.compare(c1, c2))
               equal = false;
         }
      }

      return equal;
   }

   /**
    * Generates code of the object. Overrides {@link Object#hashCode().
    */
   @Override
   public int hashCode()
   {
      //It is error-prone that equals is redefined but hasCode is not
      //Looks like the behavior is the same, but still...
      return super.hashCode();
   }

   // see IPSComponent
   public void fromXml(Element sourceNode, IPSDocument parentDoc,
                       List parentComponents)
      throws PSUnknownNodeTypeException
   {
      if (sourceNode == null)
         throw new PSUnknownNodeTypeException(
            IPSObjectStoreErrors.XML_ELEMENT_NULL, XML_NODE_NAME);

      if (!XML_NODE_NAME.equals(sourceNode.getNodeName()))
      {
         Object[] args = { XML_NODE_NAME, sourceNode.getNodeName() };
         throw new PSUnknownNodeTypeException(
            IPSObjectStoreErrors.XML_ELEMENT_WRONG_TYPE, args);
      }

      parentComponents = updateParentList(parentComponents);
      int parentSize = parentComponents.size() - 1;

      int firstFlags = PSXmlTreeWalker.GET_NEXT_ALLOW_CHILDREN |
         PSXmlTreeWalker.GET_NEXT_RESET_CURRENT;
      int nextFlags = PSXmlTreeWalker.GET_NEXT_ALLOW_SIBLINGS |
         PSXmlTreeWalker.GET_NEXT_RESET_CURRENT;

      try
      {
         PSXmlTreeWalker tree = new PSXmlTreeWalker(sourceNode);

         // get all action links
         Element node = tree.getNextElement(
            PSActionLink.XML_NODE_NAME, firstFlags);
         while (node != null)
         {
            add(new PSActionLink(node, parentDoc, parentComponents));

            node = tree.getNextElement(
               PSActionLink.XML_NODE_NAME, nextFlags);
         }
      }
      finally
      {
         resetParentList(parentComponents, parentSize);
      }
   }

   // see IPSComponent
   public Element toXml(Document doc)
   {
      // create root and its attributes
      Element root = doc.createElement(XML_NODE_NAME);

      // create the action links
      Iterator it = iterator();
      while (it.hasNext())
         root.appendChild(((IPSComponent) it.next()).toXml(doc));

      return root;
   }

   /** the XML node name */
   public static final String XML_NODE_NAME = "PSXActionLinkList";
}

