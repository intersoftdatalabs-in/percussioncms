/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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

package com.percussion.rest.pipelines;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Named JDBC datasource backend on one native pipeline resource (datasource write slice, parent
 * #1690).
 *
 * <p>{@code datasourceId} is an allow-listed catalog token (never a JDBC URL or credentials) that
 * resolves via {@code databaseExplorer.allowListedDatasources}. {@code connectionOk} / {@code
 * connectionMessage} carry the named connection-test outcome: a failed test is named and is never
 * persisted as a success. The native IR execute path (#4365) is untouched.
 */
@XmlRootElement(name = "PipelineDatasourceBackend")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Named allow-listed JDBC datasource backend on a native pipeline resource")
public class PipelineDatasourceBackend {

  /** Adapter type: JDBC (or SQL). */
  private String adapterType;

  /** Allow-listed datasource catalog id (not a JDBC URL). */
  private String datasourceId;

  /** Connection-test outcome; {@code null} when no test ran for this response. */
  private Boolean connectionOk;

  /** Named connection-test message; never echoes JDBC URLs or credentials. */
  private String connectionMessage;

  public String getAdapterType() {
    return adapterType;
  }

  public void setAdapterType(String adapterType) {
    this.adapterType = adapterType;
  }

  public String getDatasourceId() {
    return datasourceId;
  }

  public void setDatasourceId(String datasourceId) {
    this.datasourceId = datasourceId;
  }

  public Boolean getConnectionOk() {
    return connectionOk;
  }

  public void setConnectionOk(Boolean connectionOk) {
    this.connectionOk = connectionOk;
  }

  public String getConnectionMessage() {
    return connectionMessage;
  }

  public void setConnectionMessage(String connectionMessage) {
    this.connectionMessage = connectionMessage;
  }
}
