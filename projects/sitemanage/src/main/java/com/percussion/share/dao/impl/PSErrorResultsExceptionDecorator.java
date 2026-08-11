// REFACTORED: CP-JAVA11
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
package com.percussion.share.dao.impl;

import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorResultsException;
import java.util.Map.Entry;

/**
 * Wraps a Legacy Webservice multi-error exception into a single exception by wrapping one of the
 * original exceptions.
 *
 * <p>Most operations in the new system are done on a single item, so wrapping the first exception
 * found is generally the real exception we want.
 */
public class PSErrorResultsExceptionDecorator extends PSExceptionDecorator {

  private static final long serialVersionUID = 1L;

  public PSErrorResultsExceptionDecorator(String message, PSErrorResultsException cause) {
    this(cause);
  }

  /**
   * Wraps the first nested error (or the multi-error container) as this decorator. {@code wrap}
   * must publish stack/cause during construction so the decorator resembles the original
   * throwable; justified {@code this-escape} suppress for that intentional Throwable API use.
   */
  @SuppressWarnings("this-escape")
  public PSErrorResultsExceptionDecorator(PSErrorResultsException cause) {
    var errors = cause.getErrors();
    Throwable realCause = cause;
    if (!errors.isEmpty()) {
      Entry<IPSGuid, Object> entry = errors.entrySet().iterator().next();
      var object = entry.getValue();
      if (object instanceof Throwable) {
        realCause = (Throwable) object;
      }
    }
    wrap(realCause);
  }
}
