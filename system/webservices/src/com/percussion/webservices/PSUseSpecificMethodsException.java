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
package com.percussion.webservices;

/**
 * Thrown for services operating with system relationships. 
 */
public class PSUseSpecificMethodsException extends PSErrorException
{
   /**
    * Compiler generated serial version ID used for serialization.
    */
   private static final long serialVersionUID = -7487928427736004273L;

   /*(non-Javadoc)
    * @see PSErrorException#PSErrorException()
    */
   public PSUseSpecificMethodsException()
   {
      super();
   }

   /*(non-Javadoc)
    * @see PSErrorException#PSErrorException(int, String, String)
    */
   public PSUseSpecificMethodsException(int code, String errorMessage, 
      String stack)
   {
      super(code, errorMessage, stack);
   }
}

