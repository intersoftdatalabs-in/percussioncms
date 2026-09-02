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

package com.percussion.util;

import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSException;

/**
 * Generic exception class to be used for caching errors. More specific caching errors should be
 * derived from this class.
 */
public class PSCacheException extends PSException {

  private static final long serialVersionUID = 1L;

  /**
   * Pass-through constructor to super class.
   *
   * @see PSException#PSException(int,Object)
   */
  public PSCacheException(int msgCode, Object singleArg) {
    super(msgCode, singleArg);
  }

  /**
   * Pass-through constructor to super class.
   *
   * @see PSException#PSException(int,Object[])
   */
  public PSCacheException(int msgCode, Object[] arrayArgs) {
    super(msgCode, arrayArgs);
  }

  /**
   * Pass-through constructor to super class.
   *
   * @see PSException#PSException(int)
   */
  public PSCacheException(int msgCode) {
    super(msgCode);
  }

  /**
   * Construct an exception for messages taking an array of arguments. Be sure to store the
   * arguments in the correct order in the array, where {0} in the string is array element 0, etc.
   *
   * @param msgCode the error string to load
   * @param cause the causal exception
   * @param arrayArgs the array of arguments to use as the arguments in the error message. May be
   *     <code>null</code>. <code>null</code> entries
   */
  public PSCacheException(int msgCode, Throwable cause, Object... arrayArgs) {
    super(msgCode, cause, arrayArgs);
  }

  /**
   * Typed construction with no message arguments.
   *
   * @param code catalogued error code, never {@code null}
   */
  public PSCacheException(IPSErrorCode code) {
    super(code);
  }

  /**
   * Typed construction with a single message argument.
   *
   * @param code catalogued error code, never {@code null}
   * @param singleArg the argument to use as the sole argument in the error message
   */
  public PSCacheException(IPSErrorCode code, Object singleArg) {
    super(code, singleArg);
  }

  /**
   * Typed construction with message arguments.
   *
   * @param code catalogued error code, never {@code null}
   * @param arrayArgs the array of arguments to use as the arguments in the error message
   */
  public PSCacheException(IPSErrorCode code, Object[] arrayArgs) {
    super(code, arrayArgs);
  }

  /**
   * Typed construction with a cause and message arguments.
   *
   * @param code catalogued error code, never {@code null}
   * @param cause the causal exception
   * @param arrayArgs the array of arguments to use as the arguments in the error message
   */
  public PSCacheException(IPSErrorCode code, Throwable cause, Object... arrayArgs) {
    super(code, arrayArgs, cause);
  }
}
