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
package com.percussion.workflow;

import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSException;

/**
 * PSCheckInCheckOutException is thrown if exception occurs in checking in or checking out of the
 * files from the repository.Reason for the failure is indicated in the exception message.
 */
public class PSCheckInCheckOutException extends PSException {
  private static final long serialVersionUID = 1L;

  /**
   * Construct an exception for messages taking locale and msgCode arguments.
   *
   * @param language language string to use while lookingup for the message text in the resource
   *     bundle e.g. 'en-us', may be <code>null</code> or <code>empty</code>.
   * @param msgCode the error string to load
   */
  public PSCheckInCheckOutException(String language, int msgCode) {
    super(language, msgCode);
  }

  /**
   * Typed construction with locale and no message arguments.
   *
   * @param language language string to use while looking up the message text
   * @param code catalogued error code, never {@code null}
   */
  public PSCheckInCheckOutException(String language, IPSErrorCode code) {
    super(language, requireCode(code).numericCode());
    m_typedErrorCode = code;
  }

  /**
   * Construct an exception for messages taking locale and message code arguments and and a single
   * argument.
   *
   * @param language language string to use while lookingup for the message text in the resource
   *     bundle e.g. 'en-us', may be <code>null</code> or <code>empty</code>.
   * @param msgCode the error string to load
   * @param singleArg the argument to use as the sole argument in the error message.Can be <code>
   *     null</code>.
   */
  public PSCheckInCheckOutException(String language, int msgCode, Object singleArg) {
    super(language, msgCode, singleArg);
  }

  /**
   * Typed construction with locale and a single message argument.
   *
   * @param language language string to use while looking up the message text
   * @param code catalogued error code, never {@code null}
   * @param singleArg the argument to use as the sole argument in the error message
   */
  public PSCheckInCheckOutException(String language, IPSErrorCode code, Object singleArg) {
    super(language, requireCode(code).numericCode(), singleArg);
    m_typedErrorCode = code;
  }

  /**
   * Construct an exception for messages taking language, message code and an array of arguments. Be
   * sure to store the arguments in the correct order in the array, where {0} in the string is array
   * element 0, etc.
   *
   * @param language language string to use while lookingup for the message text in the resource
   *     bundle e.g. 'en-us', may be <code>null</code> or <code>empty</code>.
   * @param msgCode the error string to load
   * @param arrayArgs the array of arguments to use as the arguments in the error message.Can be
   *     <code>null</code>.
   */
  public PSCheckInCheckOutException(String language, int msgCode, Object[] arrayArgs) {
    super(language, msgCode, arrayArgs);
  }

  /**
   * Typed construction with locale and message arguments.
   *
   * @param language language string to use while looking up the message text
   * @param code catalogued error code, never {@code null}
   * @param arrayArgs the array of arguments to use as the arguments in the error message
   */
  public PSCheckInCheckOutException(String language, IPSErrorCode code, Object[] arrayArgs) {
    super(language, requireCode(code).numericCode(), arrayArgs);
    m_typedErrorCode = code;
  }
}
