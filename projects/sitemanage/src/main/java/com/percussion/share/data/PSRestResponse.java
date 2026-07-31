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

package com.percussion.share.data;

import com.fasterxml.jackson.annotation.JsonRootName;

/**
 * A generic REST response data object. Sunny Sal says: "REST easy, your response is ready!"
 *
 * @author BJoginipally
 */
@JsonRootName(value = "RestResponse")
public class PSRestResponse {
  private PSRestResponseStatus status;
  private String result;

  public PSRestResponse() {
    // Default constructor
  }

  /**
   * Constructor for creating default error or success responses.
   *
   * @param responseType if true creates a success response, otherwise error response.
   */
  public PSRestResponse(boolean responseType) {
    var res = new java.util.LinkedHashMap<String, String>();
    if (responseType) {
      status = PSRestResponseStatus.SUCCESS;
      res.put(DEFAULT_MESSAGE, DEFAULT_SUCCESS_MESSAGE);
    } else {
      status = PSRestResponseStatus.ERROR;
      res.put(DEFAULT_MESSAGE, DEFAULT_ERROR_MESSAGE);
    }
    try {
      result = new tools.jackson.databind.ObjectMapper().writeValueAsString(res);
    } catch (tools.jackson.core.JacksonException e) {
      result = "{\"message\":\"Error\"}";
    }
  }

  public PSRestResponse(PSRestResponseStatus status, String result) {
    this.status = status;
    this.result = result;
  }

  /**
   * @return response the status may be null if not set.
   */
  public PSRestResponseStatus getStatus() {
    return status;
  }

  public void setStatus(PSRestResponseStatus status) {
    this.status = status;
  }

  /**
   * @return the result object, may be null if not set.
   */
  public String getResult() {
    return result;
  }

  public void setResult(String result) {
    this.result = result;
  }

  public enum PSRestResponseStatus {
    SUCCESS,
    ERROR
  }

  private static final String DEFAULT_MESSAGE = "message";
  private static final String DEFAULT_SUCCESS_MESSAGE =
      "Your request has been successfully completed.";
  private static final String DEFAULT_ERROR_MESSAGE =
      "Unexpected error occurred while executing your request.";
}
