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
package com.percussion.delivery.metadata.error;

/**
 * Signals that a single element of a metadata query criteria expression could not be parsed into
 * the name / operation / value triple consumed by the indexer.
 *
 * @author erikserating
 */
public class PSMalformedMetadataQueryException extends Exception {

  private static final long serialVersionUID = 1L;

  /** Default no-arg constructor. */
  public PSMalformedMetadataQueryException() {
    super();
  }

  /**
   * Constructs an instance with the given message and cause.
   *
   * @param message the detail message describing the parse failure; may be <code>null</code>.
   * @param cause the underlying cause; may be <code>null</code>.
   */
  public PSMalformedMetadataQueryException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Constructs an instance with the given message.
   *
   * @param message the detail message describing the parse failure; may be <code>null</code>.
   */
  public PSMalformedMetadataQueryException(String message) {
    super(message);
  }

  /**
   * Constructs an instance wrapping the given cause.
   *
   * @param cause the underlying cause; may be <code>null</code>.
   */
  public PSMalformedMetadataQueryException(Throwable cause) {
    super(cause);
  }
}
