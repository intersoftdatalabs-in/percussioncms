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

import com.percussion.util.PSCollection;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.util.ArrayList;
import java.util.List;

/**
 * The PSCollectionComponent class implements some of the IPSComponent
 * interface as a convenience to objects extending this class which are
 * also extensions of the collection class.
 *
 * @author       Tas Giakouminakis
 * @version     1.0
 * @since    1.0
 */
public abstract class PSCollectionComponent
   extends com.percussion.util.PSCollection implements IPSComponent
{
   /**
    * Simplified method for converted a collection to XML.
    *
    * @param   doc         the XML document
    *
    * @param   root         the element to add the XML objects to
    *
    * @param   coll         a collection containing IPSComponent objects
    */
   public static void appendCollectionToXml(
      Document doc, Element root, PSCollection coll)
   {
      if (coll != null) {
         IPSComponent entry;
         int size = coll.size();
         for(int i=0; i < size; i++)
         {
            entry = (IPSComponent)coll.get(i);
            root.appendChild(entry.toXml(doc));
         }
      }
   }

   /**
    * Construct a collection component to store objects of the specified
    * type.
    *
    * @param      className   the name of the class which this collection's
    *                         members must be or extend
    *
    * @exception   ClassNotFoundException   if the specified class cannot be
    *                                     found
    */
   protected PSCollectionComponent(java.lang.String className)
      throws ClassNotFoundException
   {
      super(className);
   }

   /**
    * Construct a collection component to store objects of the specified
    * type with the specified initial capacity and with its capacity increment 
    * equal to zero.
    *
    * @param cl the class which this collection's members must be or extend
    * @param initialCapacity   the initial capacity of the collection.
    */
   public PSCollectionComponent(Class cl,
         int initialCapacity)
   {
      super(cl, initialCapacity);
   }

   /**
    * Construct a collection component to store objects of the specified
    * class.
    *
    * @param      cl           the class which this collection's
    *                         members must be or extend
    */
   protected PSCollectionComponent(Class cl)
   {
      super(cl);
   }

   /**
    * Get the id assigned to this component.
    *
    * @return                the id assigned to this component
    */
   public int getId()
   {
      return m_id;
   }

   /**
    * Get the id assigned to this component.
    *
    * @param    id       the to assign the component
    */
   public void setId(int id)
   {
      m_id = id;
   }

   /**
    * Performs a shallow copy of the data in the supplied component to this
    * component. Derived classes should implement this method for their data,
    * calling the base class method first.
    *
    * @param c a valid PSComponent. 
    */
   public void copyFrom( PSCollectionComponent c )
   {
      if ( null == c )
         throw new IllegalArgumentException( "invalid object for copy");
      setId( c.getId());
      // copy all elements in supplied collection to this one
      int size = c.size();
      for ( int index = 0; index < size; index++ )
      {
         add( c.get( index ));
      }
      setId( c.getId());
   }

   /**
    * This method is called to create an XML element node with the
    * appropriate format for the given object. An element node may contain a
    * hierarchical structure, including child objects. The element node can
    * also be a child of another element node.
    *
    * @return     the newly created XML element node
    */
   public abstract Element toXml(Document doc);

   /**
    * This method is called to populate an object from an XML
    * element node. An element node may contain a hierarchical structure,
    * including child objects. The element node can also be a child of
    * another element node.
    *
    * @exception PSUnknownNodeTypeException   if the XML element node does not
    *                                         represent a type supported
    *                                         by the class.
    */
   public abstract void fromXml(Element sourceNode, IPSDocument parentDoc,
                        List parentComponents)
      throws PSUnknownNodeTypeException;

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
      for (int i = 0; i < size(); i++)
      {
         Object o = get(i);
         if (o instanceof IPSComponent)
         {
            IPSComponent comp = (IPSComponent)o;
            comp.validate(cxt);
         }
      }
   }

   /**
    * Add this to the list of parent objects in the array list.
    * <P>
    * After a call to this method, the caller should keep get the size
    * of the arraylist so that a call to resetParentList can
    * be made (with size - 1) to allow for proper reset.
    *
    * @param      parentComponents      the parent list
    *
    * @return      the new parent list (in case parentComponents was null)
    */
   protected List updateParentList(
      List parentComponents)
   {
      if (parentComponents == null)
         parentComponents = new ArrayList<>();

      parentComponents.add(this);

      return parentComponents;
   }

   /**
    * Reset the list of parent objects in the array list to the specified
    * size.
    *
    * @param      parentComponents      the parent list
    *
    * @param      size                  the size to set the list to
    */
   protected void resetParentList(
      List parentComponents, int size)
   {
      if (parentComponents == null)
         return;

      if (size == 0)
         parentComponents.clear();
      else {
         for (int i = parentComponents.size(); i > size; ) {
            i--;
            parentComponents.remove(i);
         }
      }
   }

   /**
    * The id assigned to this component.
    */
   protected int m_id = 0;
}

