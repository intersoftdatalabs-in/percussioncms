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
// REFACTORED: CP-JAVA11
package com.percussion.rx.delivery;

import com.percussion.error.IPSErrorCode;
import com.percussion.utils.exceptions.PSBaseException;

/**
 * Exception thrown due to problems in delivery.
 * 
 * @author dougrand
 */
public class PSDeliveryException extends PSBaseException
{
   /**
    * Ctor
    * @param msgCode
    * @param cause
    * @param args
    */
   public PSDeliveryException(int msgCode, Throwable cause, Object... args) 
   {
      super(msgCode, cause, args);
   }

   /**
    * Ctor
    * @param msgCode the message code
    * @param args the arguments
    */
   public PSDeliveryException(int msgCode, Object... args) 
   {
      super(msgCode, args);
   }

   /**
    * Ctor
    * @param msgCode
    */
   public PSDeliveryException(int msgCode) {
      super(msgCode);
   }

   /**
    * Typed construction from a catalogued {@link IPSErrorCode} (e.g. {@code
    * DeliveryErrorCodes}). Sets the legacy numeric code for message lookup and
    * retains the typed code for {@link #getTypedErrorCode()} / {@link
    * #isAuditable()}.
    *
    * @param code catalogued error code, never {@code null}
    */
   public PSDeliveryException(IPSErrorCode code)
   {
      super(code);
   }

   /**
    * Typed construction with message arguments.
    *
    * @param code catalogued error code, never {@code null}
    * @param args message arguments; may be {@code null}
    */
   public PSDeliveryException(IPSErrorCode code, Object... args)
   {
      super(code, args);
   }

   /**
    * Typed construction with a cause and message arguments.
    *
    * @param code catalogued error code, never {@code null}
    * @param cause original exception, may be {@code null}
    * @param args message arguments; may be {@code null}
    */
   public PSDeliveryException(IPSErrorCode code, Throwable cause, Object... args)
   {
      super(code, cause, args);
   }

   /**
    * 
    */
   private static final long serialVersionUID = -1655303624680066236L;

   @Override
   protected String getResourceBundleBaseName()
   {
      return "com.percussion.rx.delivery.PSDeliveryErrorStringBundle";
   }

}
