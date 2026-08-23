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

package com.percussion.rest.contenttypes;

/**
 * Thrown when a content-type design write requires a held design-session lock that is missing or
 * owned by another user. Maps to HTTP 409.
 */
public class ContentTypeDesignLockException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ContentTypeDesignLockException(String message) {
    super(message);
  }

  public ContentTypeDesignLockException(String message, Throwable cause) {
    super(message, cause);
  }
}
