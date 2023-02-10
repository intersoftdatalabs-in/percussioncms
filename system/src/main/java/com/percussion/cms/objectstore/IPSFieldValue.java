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
package com.percussion.cms.objectstore;

import com.percussion.cms.PSCmsException;

/**
 * Base interface for any type of value contained by an <code>PSItemField</code>.
 * Sub-interfaces should implement accessor methods appropriate for the type of
 * value they support.
 */
public interface IPSFieldValue extends Cloneable
{
   /**
    * Gets the data contained by this value as an object.  If the value is a
    * binary value, it may not be retrieved from the server until this method is 
    * called. If there is an error, a <code>RuntimeException</code> may be
    * thrown.
    *
    * @return The data contained by this value.  The type of object will depend
    * on the type of data the field specifies it contains. May be
    * <code>null</code>.
    * 
    * @throws RuntimeException if there is an error lazily loading binary data
    * from the server.
    */
   public Object getValue();

   /**
    * Gets the data contained by this value as a string.  This effectively
    * enforces a toString implementation on the object being return in
    * <code>getValue()</code>.
    *
    * @return The data contained by this value as a String. May be empty, never
    * <code>null</code>.
    * @throws PSCmsException implementers may need this, if conversions are
    * not so easy.
    */
   public String getValueAsString() throws PSCmsException;

   /**
    * Creates a new instance of this object, deep copying all member variables.
    * If an implementing class has mutable member variables, it must override
    * this method and clone() each of those variables.  This method will create
    * a shallow copy if it is not overridden.
    *
    * @return a deep-copy clone of this instance, never <code>null</code>.
    */
   public Object clone();

   /**
    * Indicates whether some other object is "equal to" this one.
    * All implementing classes must override this method as the default
    * behavior is not desired. If the implementation does not follow the
    * contract documented for this method in
    * {@link Object#equals(Object) Object}, then it must clearly state how it
    * deviates from that contract.
    * @param obj the reference object with which to compare.
    * @return <code>true</code> if this object is the same as the
    * <code>obj</code> argument; <code>false</code> otherwise. If
    * <code>null</code> supplied or obj is not an instance of this class,
    * <code>false</code> is returned.
    */
   public boolean equals(Object obj);

   /**
    * Must be overridden to fulfill contract of this method as described in
    * Object.  {@link Object#hashCode() Object} because equals(Object) was
    * overridden.
    *
    * @return A hash code value for this object
    */
   public int hashCode();

}
