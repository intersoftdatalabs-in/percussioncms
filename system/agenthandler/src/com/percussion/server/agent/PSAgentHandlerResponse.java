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

package com.percussion.server.agent;

import org.apache.commons.lang3.StringUtils;

import java.time.Instant;
import java.util.Objects;

/**
 * Implementation of {@link IPSAgentHandlerResponse} that handles agent response data.
 * This class is thread-safe and immutable after construction for response data.
 *
 * @since Java 11
 */
public class PSAgentHandlerResponse implements IPSAgentHandlerResponse {

   private volatile int responseType = RESPONSE_TYPE_SUCCESS;
   private volatile String responseContent;
   private volatile String stylesheetPath;
   private final String timestamp;

   /**
    * Default constructor initializing with current timestamp.
    */
   public PSAgentHandlerResponse() {
      this.timestamp = Instant.now().toString();
   }

   @Override
   public void setResponse(int responseType, String responseContent) {
      if (responseType != RESPONSE_TYPE_SUCCESS && responseType != RESPONSE_TYPE_ERROR) {
         throw new IllegalArgumentException("Invalid response type: " + responseType);
      }
      this.responseType = responseType;
      this.responseContent = responseContent;
   }

   @Override
   public int getResponseType() {
      return responseType;
   }

   @Override
   public String getResponseContent() {
      return responseContent;
   }

   @Override
   public void setStyleSheet(String stylesheetPath) {
      this.stylesheetPath = stylesheetPath;
   }

   @Override
   public String getStyleSheet() {
      return stylesheetPath;
   }

   /**
    * Gets the timestamp when this response was created.
    *
    * @return the creation timestamp in ISO-8601 format
    */
   public String getTimestamp() {
      return timestamp;
   }

   /**
    * Checks if the response indicates success.
    *
    * @return {@code true} if response type is success, {@code false} otherwise
    */
   public boolean isSuccess() {
      return responseType == RESPONSE_TYPE_SUCCESS;
   }

   /**
    * Checks if the response indicates an error.
    *
    * @return {@code true} if response type is error, {@code false} otherwise
    */
   public boolean isError() {
      return responseType == RESPONSE_TYPE_ERROR;
   }

   /**
    * Checks if the response has content.
    *
    * @return {@code true} if response content is not blank, {@code false} otherwise
    */
   public boolean hasContent() {
      return StringUtils.isNotBlank(responseContent);
   }

   @Override
   public boolean equals(Object obj) {
      if (this == obj) {
         return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
         return false;
      }
      var that = (PSAgentHandlerResponse) obj;
      return responseType == that.responseType &&
             Objects.equals(responseContent, that.responseContent) &&
             Objects.equals(stylesheetPath, that.stylesheetPath) &&
             Objects.equals(timestamp, that.timestamp);
   }

   @Override
   public int hashCode() {
      return Objects.hash(responseType, responseContent, stylesheetPath, timestamp);
   }

   @Override
   public String toString() {
      return String.format("PSAgentHandlerResponse{type=%d, hasContent=%s, timestamp=%s}",
         responseType, hasContent(), timestamp);
   }
}
