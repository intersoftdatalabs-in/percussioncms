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
package com.percussion.security.shim.acl;

/**
 * Compatibility exception mirroring java.security.acl.LastOwnerException. Thrown when attempting to
 * remove the final owner from an ACL.
 */
public class LastOwnerException extends Exception {
    private static final long serialVersionUID = 1L;
  public LastOwnerException() {
    super();
  }

  public LastOwnerException(String message) {
    super(message);
  }

  public LastOwnerException(String message, Throwable cause) {
    super(message, cause);
  }

  public LastOwnerException(Throwable cause) {
    super(cause);
  }
}
