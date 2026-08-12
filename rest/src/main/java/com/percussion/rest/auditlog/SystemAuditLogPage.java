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
package com.percussion.rest.auditlog;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonRootName;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.ArrayList;
import java.util.List;

/**
 * Paged query result for system audit log entries.
 *
 * <p>Wire root {@code SystemAuditLogPage} matches {@code WRAP_ROOT_VALUE}/{@code
 * UNWRAP_ROOT_VALUE} (JacksonContextResolver). SPA clients must unwrap before reading {@code
 * entries}/{@code total} (#3089).
 */
@XmlRootElement(name = "SystemAuditLogPage")
@JsonRootName("SystemAuditLogPage")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Paged system security audit log query result")
public class SystemAuditLogPage {

  private List<SystemAuditLogEntry> entries = new ArrayList<>();
  private long total;
  private int offset;
  private int limit;

  public SystemAuditLogPage() {}

  public SystemAuditLogPage(List<SystemAuditLogEntry> entries, long total, int offset, int limit) {
    this.entries = entries != null ? entries : new ArrayList<>();
    this.total = total;
    this.offset = offset;
    this.limit = limit;
  }

  public List<SystemAuditLogEntry> getEntries() {
    return entries;
  }

  public void setEntries(List<SystemAuditLogEntry> entries) {
    this.entries = entries != null ? entries : new ArrayList<>();
  }

  public long getTotal() {
    return total;
  }

  public void setTotal(long total) {
    this.total = total;
  }

  public int getOffset() {
    return offset;
  }

  public void setOffset(int offset) {
    this.offset = offset;
  }

  public int getLimit() {
    return limit;
  }

  public void setLimit(int limit) {
    this.limit = limit;
  }
}
