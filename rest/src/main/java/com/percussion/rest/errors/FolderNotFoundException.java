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

// REFACTORED: CP-JAVA11

package com.percussion.rest.errors;

/**
 * Exception thrown when a folder is not found.
 *
 * <p>The cause is passed through {@link RestExceptionBase}'s cause-aware constructor (not via
 * {@link #initCause(Throwable)} after {@code super}), so construction is {@code this-escape} free
 * under {@code -Xlint:all}.
 */
public class FolderNotFoundException extends RestExceptionBase {

  private static final long serialVersionUID = -4398063672305185319L;

  public FolderNotFoundException() {
    this((Throwable) null);
  }

  public FolderNotFoundException(Throwable cause) {
    // always set the folder not found code; attach cause via super (not initCause)
    super(RestErrorCode.FOLDER_NOT_FOUND, null, null, null, null, cause);
  }
}
