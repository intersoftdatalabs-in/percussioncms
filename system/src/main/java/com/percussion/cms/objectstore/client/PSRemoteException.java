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
package com.percussion.cms.objectstore.client;

import com.percussion.error.IPSErrorCode;
import com.percussion.error.PSException;

/** Exceptions of this type will be thrown from the Remote Agent */
public class PSRemoteException extends PSException {
  private static final long serialVersionUID = 1L;

  /*
   * @see {@link com.percussion.error.PSException(int, Object)}
   */
  public PSRemoteException(int msgCode, Object singleArg) {
    super(msgCode, singleArg);
  }

  /*
   * @see {@link com.percussion.error.PSException(int, Object[])}
   */
  public PSRemoteException(int msgCode, Object[] arrayArgs) {
    super(msgCode, arrayArgs);
  }

  /*
   * @see {@link com.percussion.error.PSException(int)}
   */
  public PSRemoteException(int msgCode) {
    super(msgCode);
  }

  /**
   * Typed construction from a catalogued {@link IPSErrorCode} (e.g. {@code RemoteErrorCodes}).
   *
   * @param code catalogued error code, never {@code null}
   */
  public PSRemoteException(IPSErrorCode code) {
    super(code);
  }

  /**
   * Typed construction with a single message argument.
   *
   * @param code catalogued error code, never {@code null}
   * @param singleArg sole message argument; may be {@code null}
   */
  public PSRemoteException(IPSErrorCode code, Object singleArg) {
    super(code, singleArg);
  }

  /**
   * Typed construction with message arguments.
   *
   * @param code catalogued error code, never {@code null}
   * @param arrayArgs message arguments; may be {@code null}
   */
  public PSRemoteException(IPSErrorCode code, Object[] arrayArgs) {
    super(code, arrayArgs);
  }

  /**
   * Construct an exception from a class derived from PSException. The name of the original
   * exception class is saved.
   *
   * @param ex The exception to use. Its message code and arguments are stored along with the
   *     original exception class name. May not be <code>null</code>.
   */
  public PSRemoteException(PSException ex) {
    super(ex);
  }
}
