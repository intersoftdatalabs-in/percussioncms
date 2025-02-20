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
package com.percussion.util;

import java.util.Collection;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;

/**
 * The PSCollection class is used to maintain a collection of objects.
 * Objects can be added, changed or removed from the collection. All
 * objects in the collection must be of the same class.
 *
 * @author      Tas Giakouminakis
 * @version      1.0
 * @since      1.0
 */
public class PSCollection extends PSConcurrentList
{
   /**
    * Construct a collection object to store objects of the specified type.
    *
    * @param      className   the name of the class which this collection's
    *                                                                     members must be or extend
    *
    * @exception   ClassNotFoundException   if the specified class cannot be
    *                                                                                 found
    */
   public PSCollection(String className)
      throws ClassNotFoundException
   {
      this(Class.forName(className));
   }

   /**
    * Construct a collection object to store objects of the specified type.
    *
    * @param      cl            the class which this collection's
    *                                                                     members must be or extend
    */
   public PSCollection(Class<?> cl)
   {
      m_memberClass = cl;
   }

   /**
    * Construct a collection object to store objects of the specified type
    * with the specified initial capacity and with its capacity increment equal
    * to zero.
    *
    * @param cl the class which this collection's members must be or extend.
    * @param initialCapacity   the initial capacity of the collection.
    */
   public PSCollection(Class cl, int initialCapacity)
   {
      m_memberClass = cl;
   }

   /**
    * Construct a collection object from the objects in the supplied Iterator.
    *
    * @param i An iterator over <code>zero</code> or more objects.  All objects
    * must be of the same type.  May not be <code>null</code>.
    *
    * @throws ClassCastException if all of the objects under the iterator are
    * not of the same type.
    */
   public PSCollection(Iterator i)
      throws ClassCastException
   {
      m_memberClass = null;

      while (i.hasNext())
      {
         Object o = i.next();
         if (m_memberClass == null)
            m_memberClass = o.getClass();

         add(o);
      }

   }

   /**
    * Default constructor needed for serialization.
    */
   protected PSCollection()
   {
      super();
   }
   
   /**
    * Inserts the specified element at the specified index to the collection.
    * All elements in the collection must be of the class or extend the
    * class which was defined at construction of the collection. If the
    * object is of an incorrect type, an exception will be thrown.
    *
    * @param index the insert position
    * @param o   the object to add to the collection
    * @throws ClassCastException if the object is not of the appropriate class
    */
   public void add(int index, Object o)
      throws ArrayIndexOutOfBoundsException, ClassCastException
   {
      checkType(o);

      super.add(index, o);
   }

   /**
    * Adds the specified element to the collection. All elements in the
    * collection must be of the class or extend the class which was
    * defined at construction of the collection. If the object is of an
    * incorrect type, an exception will be thrown.
    *
    * @param o   the object to add to the collection
    * @return <code>true</code> if the object was added,
    *        throws ClassCastException otherwise
    * @throws ClassCastException if the object is not of the appropriate class
    */
   public boolean add(Object o)
      throws ClassCastException
   {
      checkType(o);

      return super.add(o);
   }

   /**
    * Adds the specified element to the collection. All elements in the
    * collection must be of the class or extend the class which was
    * defined at construction of the collection. If the object is of an
    * incorrect type, an exception will be thrown.
    *
    * @param o   the object to add to the collection
    * @throws ClassCastException if the object is not of the appropriate class
    */
   public void addElement(Object o)
      throws ClassCastException
   {
      add(o);
   }

   /**
    * Adds the specified collection to this collection. All elements in the
    * collection must be of the class or extend the class which was
    * defined at construction of the collection. If the object is of an
    * incorrect type, an exception will be thrown.
    *
    * @param c   the collection to add to this collection
    * @return <code>true</code> if the collection was added,
    *        throws ClassCastException otherwise
    * @throws ClassCastException if the object is not of the appropriate class
    */
   public boolean addAll(Collection c)
      throws ArrayIndexOutOfBoundsException, ClassCastException
   {
      checkType(c);

      return super.addAll(c);
   }

   /**
    * Inserts the specified collection to this collection at the specified
    * index. All elements in the collection must be of the class or extend
    * the class which was defined at construction of the collection. If the
    * object is of an incorrect type, an exception will be thrown.
    *
    * @param c   the collection to add to this collection
    * @return <code>true</code> if the collection was added,
    *        throws ClassCastException otherwise
    * @throws ClassCastException if the object is not of the appropriate class
    */
   public boolean addAll(int index, Collection c)
      throws ArrayIndexOutOfBoundsException, ClassCastException
   {
      checkType(c);

      return super.addAll(index, c);
   }

   /**
    * Sets the specified element in the collection. All elements in the
    * collection must be of the class or extend the class which was
    * defined at construction of the collection. If the object is of an
    * incorrect type, an exception will be thrown.
    *
    * @param index the index of the element to set
    * @param o the object to set
    * @return the previous element in the specified position
    * @throws ArrayIndexOutOfBoundsException   if index is out of range
    * @throws ClassCastException   if the object is not of the appropriate class
    */
   public Object set(int index, Object o)
      throws ArrayIndexOutOfBoundsException, ClassCastException
   {
      checkType(o);

      return super.set(index, o);
   }

   /**
    * Sets the specified element in the collection. All elements in the
    * collection must be of the class or extend the class which was
    * defined at construction of the collection. If the object is of an
    * incorrect type, an exception will be thrown.
    *
    * @param index the index of the element to set
    * @param o the object to set
    * @throws ArrayIndexOutOfBoundsException   if index is out of range
    * @throws ClassCastException   if the object is not of the appropriate class
    */
   public void setElementAt(Object o, int index)
      throws ArrayIndexOutOfBoundsException, ClassCastException
   {
      checkType(o);

      set(index, o);
   }

   public void insertElementAt(Object o, int i){
      this.add(i,o);
   }

   /**
    * Get the Class type of valid member objects.
    *
    * @return the class type
    */
   public Class getMemberClassType()
   {
      return m_memberClass;
   }

   /**
    * Get the name of the Class type of valid member objects.
    *
    * @return the name of the class type
    */
   public String getMemberClassName()
   {
      return m_memberClass.getName();
   }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PSCollection)) return false;
        PSCollection that = (PSCollection) o;
        return m_memberClass.equals(that.m_memberClass);
    }

    @Override
    public int hashCode() {
        return Objects.hash(m_memberClass);
    }

    /**
    * Check the type of the object and throw the appropriate execption if
    * it's the wrong type.
    *
    * @param o the object to check
    * @exception ClassCastException   if the object is of the wrong type
    */
   protected void checkType(Object o)
      throws ClassCastException
   {
      if (!(m_memberClass.isInstance(o)))
         throw new ClassCastException("Cannot add an object of class " +
            o.getClass().getName() + " to the collection of " +
            m_memberClass.getName());
   }

   /**
    * Check the type of all objects in the provided collection and throw the
    * appropriate execption if any of the collections objects has the wrong
    * type.
    *
    * @param c the collection to check
    * @exception ClassCastException   if any object in the provided collection
    *    is of the wrong type.
    */
   private void checkType(Collection c)
      throws ClassCastException
   {
      Iterator i = c.iterator();
      while (i.hasNext())
      {
         Object o = i.next();
         if (!(m_memberClass.isInstance(o)))
            throw new ClassCastException("Cannot add an object of class " +
               o.getClass().getName() + " to the collection of " +
               m_memberClass.getName());
      }
   }
   /**
    * Creates a deep copy of PSCollection object.
    * First clear the clone collection, then  check if the objects inside
    * the collection is immutable if so the method just add to the clone.
    * If not call the corresponding clone method for each of the object.
    * The caller must perform their own clone of this object if
    * the member of the collection are resources such as inputstream,
    * database connection etc. 'String' and 'File' are the only classes
    * considered immutable.
    * Each object inside the collection has to have clone() method.
    *
    * @return A new collection with each mutable member cloned and a reference
    *    copy of each immutable member.
    *
    * @throws InternalError If any mutable member doesn't implement the clone
    *    method or there are any problems executing that method.
    */
   public Object clone()
   {
      Object copy = null;
      try {
         copy = super.clone();
      } catch (CloneNotSupportedException e) {
         throw new InternalError(e.toString());
      }
      return copy;
   }

   public  Object lastElement(){
      if(this.size()>0){
         return this.get(this.size()-1);
      }else{
         throw new NoSuchElementException();
      }
   }

   public void removeAllElements(){
      this.clear();
   }

   public Object elementAt(int index){
      return this.get(index);
   }

   public Object firstElement(){
      if(this.size()>0)
      return this.get(0);
      else
         throw new NoSuchElementException();
   }
   /**
    * The one and only valid class type for this collection.
    */
   private Class m_memberClass;
}
