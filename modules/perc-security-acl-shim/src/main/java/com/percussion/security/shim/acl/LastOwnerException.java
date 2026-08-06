/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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
package com.percussion.security.shim.acl;

/**
 * Compatibility exception mirroring the legacy {@code java.security.acl.LastOwnerException} type.
 * Thrown when an operation would leave an {@link Acl} with zero owners.
 *
 * <p>Every ACL must have at least one owner; an attempt to delete the final owner fails with this
 * exception so that the ACL remains administrable.
 */
public class LastOwnerException extends Exception {
  private static final long serialVersionUID = 1L;

  /** Creates a new exception with no message or cause. */
  public LastOwnerException() {
    super();
  }

  /**
   * Creates a new exception with the given message.
   *
   * @param message a human-readable description of the failure, may be {@code null}
   */
  public LastOwnerException(String message) {
    super(message);
  }

  /**
   * Creates a new exception with the given message and underlying cause.
   *
   * @param message a human-readable description of the failure, may be {@code null}
   * @param cause the underlying cause of the failure, may be {@code null}
   */
  public LastOwnerException(String message, Throwable cause) {
    super(message, cause);
  }

  /**
   * Creates a new exception wrapping the given cause.
   *
   * @param cause the underlying cause of the failure, may be {@code null}
   */
  public LastOwnerException(Throwable cause) {
    super(cause);
  }
}
