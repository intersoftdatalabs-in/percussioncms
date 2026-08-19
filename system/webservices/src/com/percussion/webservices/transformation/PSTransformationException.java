/*
 * Copyright 1999-2025 Percussion Software, Inc.
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
package com.percussion.webservices.transformation;

import com.percussion.error.IPSErrorCode;
import com.percussion.utils.exceptions.PSBaseException;

public class PSTransformationException extends PSBaseException
{
   /**
    * Compiler generated serial version ID used for serialization.
    */
   private static final long serialVersionUID = -763062444961430893L;

   private transient IPSErrorCode typedErrorCode;

   /*
    * (non-Javadoc)
    * 
    * @see PSBaseException#PSBaseException(int)
    */
   public PSTransformationException(int msgCode)
   {
      super(msgCode);
   }

   /*
    * (non-Javadoc)
    * 
    * @see PSBaseException#PSBaseException(int, Object...)
    */
   public PSTransformationException(int msgCode, Object... arrayArgs)
   {
      super(msgCode, arrayArgs);
   }

   /*
    * (non-Javadoc)
    * 
    * @see PSBaseException#PSBaseException(int, Throwable, Object)
    */
   public PSTransformationException(int msgCode, Throwable cause, 
      Object... arrayArgs)
   {
      super(msgCode, cause, arrayArgs);
   }

   /**
    * Typed construction from a catalogued {@link IPSErrorCode}.
    *
    * @param code catalogued error code, never {@code null}
    */
   public PSTransformationException(IPSErrorCode code)
   {
      super(requireCode(code).numericCode());
      this.typedErrorCode = code;
   }

   /**
    * Typed construction with message arguments.
    *
    * @param code catalogued error code, never {@code null}
    * @param arrayArgs message arguments; may be {@code null}
    */
   public PSTransformationException(IPSErrorCode code, Object... arrayArgs)
   {
      super(requireCode(code).numericCode(), arrayArgs);
      this.typedErrorCode = code;
   }

   /**
    * Typed error code when constructed via {@link IPSErrorCode} overloads;
    * otherwise {@code null}.
    */
   public IPSErrorCode getTypedErrorCode()
   {
      return typedErrorCode;
   }

   /**
    * Whether dual-write should consider this exception auditable.
    */
   public boolean isAuditable()
   {
      return typedErrorCode != null && typedErrorCode.isAuditable();
   }

   private static IPSErrorCode requireCode(IPSErrorCode code)
   {
      if (code == null)
      {
         throw new IllegalArgumentException("code may not be null");
      }
      return code;
   }

   /*
    * (non-Javadoc)
    * 
    * @see PSBaseException#getResourceBundleBaseName()
    */
   @Override
   protected String getResourceBundleBaseName()
   {
      return "com.percussion.webservices.transformation.PSTransformationErrorStringBundle";
   }
}

