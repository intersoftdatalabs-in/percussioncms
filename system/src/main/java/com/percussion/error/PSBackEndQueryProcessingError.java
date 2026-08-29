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
package com.percussion.error;

import com.intsof.percussioncms.auditlog.codes.DataErrorCodes;
import com.intsof.percussioncms.auditlog.codes.ServerErrorCodes;
import com.percussion.log.PSLogSubMessage;
import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.Locale;
import org.w3c.dom.Element;

/**
 * The PSBackEndQueryProcessingError class is used to report an error encountered during back-end
 * (database) query processing.
 *
 * @author Tas Giakouminakis
 * @version 1.0
 * @since 1.0
 */
public class PSBackEndQueryProcessingError extends PSBackEndError {
  /**
   * Report an error encountered during back-end (database) query processing using the native error
   * information and the SELECT statement.
   *
   * <p>The application id is most commonly obtained by calling {@link
   * com.percussion.data.PSExecutionData#getId PSExecutionData.getId()} or {@link
   * com.percussion.server.PSApplicationHandler#getId PSApplicationHandler.getId()}.
   *
   * <p>The session id can be obtained from the {@link com.percussion.server.PSUserSession
   * PSUserSession} object contained in the {@link com.percussion.server.PSRequest PSRequest}
   * object.
   *
   * @param applId the id of the application that generated the error
   * @param sessionId the session id of the user making the request
   * @param errorCode the error code reported by the back-end (database)
   * @param errorString the error text reported by the back-end (database)
   * @param queryString the SELECT statement which caused the error
   */
  public PSBackEndQueryProcessingError(
      int applId,
      java.lang.String sessionId,
      int errorCode,
      java.lang.String errorString,
      java.lang.String queryString) {
    this(
        applId,
        sessionId,
        ServerErrorCodes.NATIVE_ERROR.numericCode(),
        new Object[] {
          Integer.valueOf(errorCode),
          // REFACTORED: CP-JAVA11
          ((errorString == null) ? "" : errorString)
        },
        queryString);
  }

  /**
   * Report an error encountered during back-end (database) query processing using the internal
   * error information. This usually occurs when E2 encounters an error pre-processing the request.
   *
   * <p>The application id is most commonly obtained by calling {@link
   * com.percussion.data.PSExecutionData#getId PSExecutionData.getId()} or {@link
   * com.percussion.server.PSApplicationHandler#getId PSApplicationHandler.getId()}.
   *
   * <p>The session id can be obtained from the {@link com.percussion.server.PSUserSession
   * PSUserSession} object contained in the {@link com.percussion.server.PSRequest PSRequest}
   * object.
   *
   * @param applId the id of the application that generated the error
   * @param sessionId the session id of the user making the request
   * @param errorCode the error code describing the type of error
   * @param errorParams if the error string associated with the error code specifies parameters,
   *     this is an array of values to use to fill the string appropriately. Be sure to include the
   *     correct arguments in their correct positions!
   * @param source the sub-tree containing the element(s) causing the error (may be null)
   */
  public PSBackEndQueryProcessingError(
      int applId, java.lang.String sessionId, int errorCode, Object[] errorParams, Element source) {
    this(applId, sessionId, errorCode, errorParams, PSXmlDocumentBuilder.toString(source));
  }

  /**
   * Report an error encountered during back-end (database) query processing using the specified
   * error information and the SELECT statement.
   *
   * <p>The application id is most commonly obtained by calling {@link
   * com.percussion.data.PSExecutionData#getId PSExecutionData.getId()} or {@link
   * com.percussion.server.PSApplicationHandler#getId PSApplicationHandler.getId()}.
   *
   * <p>The session id can be obtained from the {@link com.percussion.server.PSUserSession
   * PSUserSession} object contained in the {@link com.percussion.server.PSRequest PSRequest}
   * object.
   *
   * @param applId the id of the application that generated the error
   * @param sessionId the session id of the user making the request
   * @param errorCode the error code describing the type of error
   * @param errorParams if the error string associated with the error code specifies parameters,
   *     this is an array of values to use to fill the string appropriately. Be sure to include the
   *     correct arguments in their correct positions!
   * @param source the SELECT statement which caused the error
   */
  public PSBackEndQueryProcessingError(
      int applId,
      java.lang.String sessionId,
      int errorCode,
      Object[] errorParams,
      java.lang.String source) {
    super(applId, errorCode, errorParams);

    if (sessionId == null) m_sessId = "";
    else m_sessId = sessionId;

    if (source == null) m_source = "";
    else m_source = source;
  }

  /** Subclasses must override this to build the messages in the specified locale. */
  protected PSLogSubMessage[] buildSubMessages(Locale loc) {
    PSLogSubMessage[] msgs = new PSLogSubMessage[3];

    /* use IPSDataErrors.QUERY_PROCESSING_ERROR along with:
     *    [0] = m_sessId
     * to format the first submessage
     */
    msgs[0] =
        new PSLogSubMessage(
            DataErrorCodes.QUERY_PROCESSING_ERROR.numericCode(),
            PSErrorManager.createMessage(
                DataErrorCodes.QUERY_PROCESSING_ERROR.numericCode(),
                new Object[] {m_sessId},
                loc));

    /* use the errorCode/errorParams to format the second submessage
     */
    msgs[1] =
        new PSLogSubMessage(
            m_errorCode, PSErrorManager.createMessage(m_errorCode, m_errorArgs, loc));

    /* use IPSServerErrors.RAW_DUMP along with source
     * to format the third submessage
     */
    msgs[2] =
        new PSLogSubMessage(
            ServerErrorCodes.RAW_DUMP.numericCode(),
            PSErrorManager.createMessage(
                ServerErrorCodes.RAW_DUMP.numericCode(), new Object[] {m_source}, loc));

    return msgs;
  }

  private String m_sessId;
  private String m_source;
}
