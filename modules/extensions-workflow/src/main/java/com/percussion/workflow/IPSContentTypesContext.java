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

package com.percussion.workflow;

import java.sql.SQLException;

/**
 * An interface that defines methods for content types context.
 *
 * @author Rammohan Vangapalli
 * @version 1.0
 * @since 2.0
 * @deprecated
 */
@Deprecated
public interface IPSContentTypesContext {
  /**
   * Gets Query Request for the current entry in the context.
   *
   * @return the query request string for the current entry in the context.
   * @throws SQLException if an SQL error occurs while building the request.
   */
  public String getContentTypeQueryRequest() throws SQLException;

  /**
   * Gets Update Request for the current entry in the context.
   *
   * @return the update request string for the current entry in the context.
   * @throws SQLException if an SQL error occurs while building the request.
   */
  public String getContentTypeUpdateRequest() throws SQLException;

  /**
   * Gets New Request for the current entry in the context.
   *
   * @return the new request string for the current entry in the context.
   * @throws SQLException if an SQL error occurs while building the request.
   */
  public String getContentTypeNewRequest() throws SQLException;

  /**
   * Gets content type name for the current entry in the context.
   *
   * @return the content type name for the current entry in the context.
   * @throws SQLException if an SQL error occurs while accessing the data.
   */
  public String getContentTypeName() throws SQLException;

  /**
   * Gets content type description for the current entry in the context.
   *
   * @return the content type description for the current entry in the context.
   * @throws SQLException if an SQL error occurs while accessing the data.
   */
  public String getContentTypeDescription() throws SQLException;

  /**
   * Closes the context freeing all JDBC resources.
   */
  public void close();
}
