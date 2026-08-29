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
import com.percussion.xml.PSXmlDocumentBuilder;
import java.util.Locale;
import java.util.Objects;
import org.w3c.dom.Element;

/**
 * The PSApplicationDesignError class is used to report a design error in the application. When E2
 * encounters a design error at run-time, this may be caused by files having been deleted or
 * renamed, or the application may have been saved with validation disabled.
 *
 * <p>The following information is logged:
 *
 * <ul>
 *   <li>the text of the error
 *   <li>the XML element node(s) in error
 * </ul>
 *
 * @author Tas Giakouminakis
 * @version 1.0
 * @since 1.0
 */
public final class PSApplicationDesignError extends PSLogError {

  /**
   * Report an application design error.
   *
   * <p>The application id is most commonly obtained by calling {@link
   * com.percussion.data.PSExecutionData#getId PSExecutionData.getId()} or {@link
   * com.percussion.server.PSApplicationHandler#getId PSApplicationHandler.getId()}.
   *
   * @param applId the id of the application that generated the error
   * @param errorCode the error code describing the type of error
   * @param errorParams if the error string associated with the error code specifies parameters,
   *     this is an array of values to use to fill the string appropriately. Be sure to include the
   *     correct arguments in their correct positions!
   * @param source the XML sub-tree containing the element(s) causing the error
   */
  public PSApplicationDesignError(int applId, int errorCode, Object[] errorParams, Element source) {
    super(applId);

    m_errorCode = errorCode;
    m_errorArgs = errorParams;
    m_source = Objects.isNull(source) ? "" : PSXmlDocumentBuilder.toString(source);
  }

  /** Subclasses must override this to build the messages in the specified locale */
  @Override
  protected PSLogSubMessage[] buildSubMessages(Locale loc) {
    Objects.requireNonNull(loc, "locale cannot be null");

    var msgCount = m_source.isEmpty() ? 1 : 2;
    var msgs = new PSLogSubMessage[msgCount];

    // Use the errorCode/errorString to format the first submessage
    msgs[0] =
        new PSLogSubMessage(
            m_errorCode, PSErrorManager.createMessage(m_errorCode, m_errorArgs, loc));

    if (msgCount == 2) {
      msgs[1] =
          new PSLogSubMessage(
              ServerErrorCodes.RAW_DUMP.numericCode(),
              PSErrorManager.createMessage(
                  ServerErrorCodes.RAW_DUMP.numericCode(), new Object[] {m_source}, loc));
    }

    return msgs;
  }

  private final int m_errorCode;
  private final Object[] m_errorArgs;
  private final String m_source;
}
