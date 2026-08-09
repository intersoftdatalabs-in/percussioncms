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
package com.intsof.percussioncms.auditlog.sink;

import com.intsof.percussioncms.auditlog.AuditRecord;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/** Test sink that captures written records. */
public final class CapturingAuditLogSink implements AuditLogSink {

  private final String name;
  private final List<AuditRecord> records = new ArrayList<>();
  private final boolean fail;

  public CapturingAuditLogSink(String name) {
    this(name, false);
  }

  public CapturingAuditLogSink(String name, boolean fail) {
    this.name = name;
    this.fail = fail;
  }

  @Override
  public String name() {
    return name;
  }

  @Override
  public void write(AuditRecord record) {
    if (fail) {
      throw new IllegalStateException("forced sink failure");
    }
    records.add(record);
  }

  public List<AuditRecord> records() {
    return Collections.unmodifiableList(records);
  }

  public void clear() {
    records.clear();
  }
}
