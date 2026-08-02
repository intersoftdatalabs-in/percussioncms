/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

package com.percussion.apibridge.mkd;

/**
 * GCM transport / backend rejection while submitting an i18n correction.
 *
 * <p>Extends {@link RuntimeException} so the REST resource maps it to HTTP 502 without embedding
 * SDK message text (which may echo tokens or other sensitive details) into the client body.
 */
public class MkdGcmBackendException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public MkdGcmBackendException(String message, Throwable cause) {
    super(message, cause);
  }
}
