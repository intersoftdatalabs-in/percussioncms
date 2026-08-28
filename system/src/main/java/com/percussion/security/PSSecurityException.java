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

package com.percussion.security;

import com.intsof.percussioncms.auditlog.codes.SecurityErrorCodes;
import com.percussion.error.IPSErrorCode;
import com.percussion.error.IPSException;
import com.percussion.error.PSRuntimeException;

/**
 * This exception is thrown when a serious problem occurs that prevents the security sub-system from
 * obtaining the information it needs to perform the requested action.
 */
public class PSSecurityException extends PSRuntimeException {
  /**
   * Constructs a new exception based on an existing exception. Used to pass through exceptions.
   *
   * @param e The original exception.
   */
  public PSSecurityException(IPSException e) {
    super(e.getErrorCode(), e.getErrorArguments());
  }

  /** See {@link PSRuntimeException#PSRuntimeException(int) base class} for desc. */
  public PSSecurityException(int errorCode) {
    super(errorCode);
  }

  /** See {@link PSRuntimeException#PSRuntimeException(int,Object[]) base class} for desc. */
  public PSSecurityException(int errorCode, Object[] messageArgs) {
    super(errorCode, messageArgs);
  }

  /**
   * See {@link PSRuntimeException#PSRuntimeException(int,Object[],Throwable) base class} for desc.
   */
  public PSSecurityException(int errorCode, Object[] messageArgs, Throwable cause) {
    super(errorCode, messageArgs, cause);
  }

  /**
   * Typed construction from a catalogued {@link IPSErrorCode}.
   *
   * @param code catalogued error code, never {@code null}
   */
  public PSSecurityException(IPSErrorCode code) {
    super(code);
  }

  /**
   * Typed construction with message arguments.
   *
   * @param code catalogued error code, never {@code null}
   * @param messageArgs the array of arguments to use as the arguments in the error message; may be
   *     {@code null}
   */
  public PSSecurityException(IPSErrorCode code, Object[] messageArgs) {
    super(code, messageArgs);
  }

  /**
   * Typed construction with message arguments and a cause.
   *
   * @param code catalogued error code, never {@code null}
   * @param messageArgs the array of arguments to use as the arguments in the error message; may be
   *     {@code null}
   * @param cause causal throwable; may be {@code null}
   */
  public PSSecurityException(IPSErrorCode code, Object[] messageArgs, Throwable cause) {
    super(code, messageArgs, cause);
  }

  /**
   * Creates an exception that indicates cataloging for some type of security object failed.
   *
   * @param detailMsg The text describing the problem.
   */
  public PSSecurityException(String detailMsg) {
    super(SecurityErrorCodes.METADATA_UNAVAILABLE, new Object[] {detailMsg});
  }
}
