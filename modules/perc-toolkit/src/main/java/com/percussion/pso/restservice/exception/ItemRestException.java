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
package com.percussion.pso.restservice.exception;

import com.percussion.pso.restservice.model.Error.ErrorCode;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * ItemRestException class.
 */
public class ItemRestException extends Exception {

    private static final long serialVersionUID = 1L;

  private static final Logger log = LogManager.getLogger(ItemRestException.class);
  private ErrorCode errorCode = ErrorCode.UNKNOWN_ERROR;

  /**
   * Creates a new ItemRestException.
   */
  public ItemRestException() {}

  /**
   * Creates a new ItemRestException.
   *
   * @param errorCode the error code
   * @param msg the msg
   */
  public ItemRestException(ErrorCode errorCode, String msg) {
    super(msg);
    this.errorCode = errorCode;
    log.debug("Rest exception {}", msg);
  }

  /**
   * Creates a new ItemRestException.
   *
   * @param msg the msg
   */
  public ItemRestException(String msg) {
    super(msg);
    log.debug("Rest exception {}", msg);
  }

  /**
   * Creates a new ItemRestException.
   *
   * @param msg the msg
   * @param e the e
   */
  public ItemRestException(String msg, Exception e) {
    super(msg, e);
    log.debug("Rest exception {} {}", msg, e);
  }

  /**
   * Sets the error code.
   *
   * @param errorCode the error code
   */
  public void setErrorCode(ErrorCode errorCode) {
    this.errorCode = errorCode;
  }

  /**
   * Returns the error code.
   *
   * @return the result
   */
  public ErrorCode getErrorCode() {
    return errorCode;
  }
}
