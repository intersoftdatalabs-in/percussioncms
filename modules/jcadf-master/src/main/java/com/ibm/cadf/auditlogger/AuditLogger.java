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

package com.ibm.cadf.auditlogger;

import com.ibm.cadf.exception.CADFException;
import com.ibm.cadf.model.Event;

/**
 * Abstract base for CADF audit-log sinks. Concrete subclasses decide how the persisted event is
 * encoded (e.g., JSON or CSV) and where it is written; the base class exposes a uniform {@link
 * #audit(Event)} entry point and an output-file path accessor shared by every sink.
 */
public abstract class AuditLogger {

  private String outputFilePath;

  /** Default no-argument constructor for {@link AuditLogger}. */
  public AuditLogger() {
    // Default constructor for AuditLogger.
  }

  /**
   * Writes the supplied audit event to the underlying sink. Implementations decide on the encoding
   * and target location.
   *
   * @param auditEvent the CADF event to persist, never {@code null}.
   * @throws CADFException when the event cannot be encoded or written.
   */
  public abstract void writeLog(Event auditEvent) throws CADFException;

  /**
   * Convenience entry point that delegates to {@link #writeLog(Event)} and propagates any {@link
   * CADFException} thrown by the underlying sink.
   *
   * @param auditEvent the CADF event to record, never {@code null}.
   * @throws CADFException when the event cannot be encoded or written.
   */
  public void audit(Event auditEvent) throws CADFException {
    try {
      writeLog(auditEvent);
    } catch (CADFException e) {
      throw e;
    }
  }

  /**
   * Returns the configured output file path for this audit sink.
   *
   * @return the file path, may be {@code null} when no destination has been configured.
   */
  public String getOutputFilePath() {
    return outputFilePath;
  }

  /**
   * Sets the output file path the audit sink should write to.
   *
   * @param outputFilePath the absolute path, may be {@code null}.
   */
  public void setOutputFilePath(String outputFilePath) {
    this.outputFilePath = outputFilePath;
  }
}
