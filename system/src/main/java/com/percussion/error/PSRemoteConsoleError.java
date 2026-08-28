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

/**
 * The PSRemoteConsoleError class is used to report a remote console command error.
 *
 * @author Tas Giakouminakis
 * @version 1.0
 * @since 1.0
 */
public class PSRemoteConsoleError extends PSLogError {

  /**
   * Report a remote console exception.
   *
   * @param command the command executing at the time of the error
   * @param t the exception
   */
  public PSRemoteConsoleError(String command, Throwable t) {
    super(0);
    if (command != null) {
      m_errorCode = ServerErrorCodes.RCONSOLE_EXEC_EXCEPTION.numericCode();
      m_errorArgs = new Object[2];
      m_errorArgs[0] = command;
      m_errorArgs[1] = t.getMessage();
    } else {
      m_errorCode = ServerErrorCodes.RCONSOLE_COMMAND_EXCEPTION.numericCode();
      m_errorArgs = new Object[1];
      m_errorArgs[0] = t.getMessage();
    }
  }

  /** sublcasses must override this to build the messages in the specified locale */
  protected PSLogSubMessage[] buildSubMessages(Locale loc) {
    PSLogSubMessage[] msgs = new PSLogSubMessage[2];

    /* the generic submessage first */
    msgs[0] =
        new PSLogSubMessage(
            ServerErrorCodes.RCONSOLE_COMMAND_ERROR_MSG.numericCode(),
            PSErrorManager.getErrorText(
                ServerErrorCodes.RCONSOLE_COMMAND_ERROR_MSG.numericCode(), false, loc));

    /* use the errorCode/errorParams to format the second submessage */
    msgs[1] =
        new PSLogSubMessage(
            m_errorCode, PSErrorManager.createMessage(m_errorCode, m_errorArgs, loc));

    return msgs;
  }

  private int m_errorCode;
  private Object[] m_errorArgs;
}
