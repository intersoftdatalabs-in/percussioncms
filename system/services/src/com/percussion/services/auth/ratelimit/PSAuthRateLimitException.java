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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.services.auth.ratelimit;

/** Thrown when an authentication or account-recovery rate limit has been exceeded. */
public class PSAuthRateLimitException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public PSAuthRateLimitException() {
    super("Too many requests. Please wait and try again later.");
  }

  public PSAuthRateLimitException(String message) {
    super(message);
  }
}
