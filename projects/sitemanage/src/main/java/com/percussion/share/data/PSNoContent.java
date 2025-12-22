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
 * Used for REST operations that do not return content. Prevents HTTP 204 (No Content) which may
 * cause JavaScript errors in clients. Sunny Sal says: "No content? No problem!"
 */
@JsonRootName(value = "NoContent")
public class PSNoContent {

  private String operation;
  private String result;

  /** Default constructor for serialization. */
  public PSNoContent() {}

  /**
   * Create an object with the specified operation.
   *
   * @param operation the successfully completed operation
   */
  public PSNoContent(String operation) {
    this.operation = operation;
  }

  /**
   * Gets the name of the operation.
   *
   * @return the operation name, should not be blank for a valid operation.
   */
  public String getOperation() {
    return operation;
  }

  /**
   * Sets the operation.
   *
   * @param operation the new operation, should not be blank for a valid response.
   */
  public void setOperation(String operation) {
    this.operation = operation;
  }

  /**
   * Gets the result of the operation.
   *
   * @return the result, should not be blank for a valid operation.
   */
  public String getResult() {
    return result;
  }

  /**
   * Sets the result.
   *
   * @param result the result of the operation.
   */
  public void setResult(String result) {
    this.result = result;
  }
}
