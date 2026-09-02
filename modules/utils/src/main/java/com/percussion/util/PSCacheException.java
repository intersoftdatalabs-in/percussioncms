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
   * <p>Do not pass a {@link Throwable} here. Use {@link #PSCacheException(IPSErrorCode, Throwable)}
   * (or {@link #PSCacheException(IPSErrorCode, Object[], Throwable)}) so the cause is retained.
   *
   * @param code catalogued error code, never {@code null}
   * @param singleArg the argument to use as the sole argument in the error message; must not be a
   *     {@link Throwable}
   * @throws IllegalArgumentException if {@code singleArg} is a {@link Throwable}
   */
  public PSCacheException(IPSErrorCode code, Object singleArg) {
    super(code, rejectThrowableMessageArg(singleArg));
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
   * Typed construction with a cause and no message arguments. Prefer this overload over {@link
   * #PSCacheException(IPSErrorCode, Object)} so the cause is retained rather than reformatted into
   * the message.
   *
   * @param code catalogued error code, never {@code null}
   * @param cause causal throwable; may be {@code null}
   */
  public PSCacheException(IPSErrorCode code, Throwable cause) {
    super(code, (Object[]) null, cause);
  }

  /**
   * Typed construction with message arguments and a cause. Parameter order matches {@link
   * PSException#PSException(IPSErrorCode, Object[], Throwable)} and {@code PSCatalogException}:
   * {@code (code, args, cause)}.
   *
   * @param code catalogued error code, never {@code null}
   * @param arrayArgs the array of arguments to use as the arguments in the error message
   * @param cause causal throwable; may be {@code null}
   */
  public PSCacheException(IPSErrorCode code, Object[] arrayArgs, Throwable cause) {
    super(code, arrayArgs, cause);
  }

  /**
   * {@link #PSCacheException(IPSErrorCode, Object)} must not accept a {@link Throwable}: that
   * argument belongs on {@link #PSCacheException(IPSErrorCode, Throwable)} so {@code getCause()} is
   * populated.
   */
  private static Object rejectThrowableMessageArg(Object singleArg) {
    if (singleArg instanceof Throwable) {
      throw new IllegalArgumentException(
          "PSCacheException(IPSErrorCode, Object) does not preserve cause; use"
              + " PSCacheException(IPSErrorCode, Throwable) (cast the second argument if javac"
              + " reports overload ambiguity)");
    }
    return singleArg;
  }
}
