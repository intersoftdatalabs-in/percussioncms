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

package com.percussion.error;

import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.log.PSLogError;
import com.percussion.log.PSLogSubMessage;
import java.util.Locale;
import java.util.Objects;

/**
 * The PSFatalError class is used to report end-conditions. These are usually errors which were
 * considered "impossible" yet it seems we've managed to hit them. These are always logged so we can
 * revisit them when they surface.
 *
 * @author Tas Giakouminakis
 * @version 1.0
 * @since 1.0
 */
// REFACTORED: CP-JAVA11
public class PSInternalError extends PSLogError {
  /**
   * Report am internal error. The error code and parameters should clearly define where the error
   * occurred for easy debugging.
   *
   * @param errorCode the error code describing the type of error
   * @param errorParams if the error string associated with the error code specifies parameters,
   *     this is an array of values to use to fill the string appropriately. Be sure to include the
   *     correct arguments in their correct positions!
   */
  public PSInternalError(int errorCode, Object[] errorParams) {
    super(0);
    m_errorCode = errorCode;
    m_errorArgs = errorParams;
  }

  /**
   * Report am internal error. The error code and parameters should clearly define where the error
   * occurred for easy debugging.
   *
   * @param errorCode the error code describing the type of error
   * @param singleArg the argument to use as the sole argument in the error message
   */
  public PSInternalError(int errorCode, Object singleArg) {
    this(errorCode, new Object[] {singleArg});
  }

  /**
   * Typed construction with message arguments.
   *
   * @param code catalogued error code, never {@code null}
   * @param errorParams message arguments; may be {@code null}
   */
  public PSInternalError(IPSErrorCode code, Object[] errorParams) {
    this(Objects.requireNonNull(code, "code").numericCode(), errorParams);
  }

  /**
   * Typed construction with a single message argument.
   *
   * @param code catalogued error code, never {@code null}
   * @param singleArg the argument to use as the sole argument in the error message
   */
  public PSInternalError(IPSErrorCode code, Object singleArg) {
    this(code, new Object[] {singleArg});
  }

  /** sublcasses must override this to build the messages in the specified locale */
  protected PSLogSubMessage[] buildSubMessages(Locale loc) {
    PSLogSubMessage[] msgs = new PSLogSubMessage[2];

    /* the generic submessage first */
    msgs[0] =
        new PSLogSubMessage(
            ServerErrorCodes.INTERNAL_SERVER_ERROR_MSG.numericCode(),
            PSErrorManager.getErrorText(
                ServerErrorCodes.INTERNAL_SERVER_ERROR_MSG.numericCode(), false, loc));

    /* use the errorCode/errorParams to format the second submessage */
    msgs[1] =
        new PSLogSubMessage(
            m_errorCode, PSErrorManager.createMessage(m_errorCode, m_errorArgs, loc));

    return msgs;
  }

  private int m_errorCode;
  private Object[] m_errorArgs;
}
