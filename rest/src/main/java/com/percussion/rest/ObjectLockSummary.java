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

package com.percussion.rest;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.Optional;
import javax.xml.bind.annotation.XmlRootElement;

@JsonInclude(JsonInclude.Include.NON_NULL)
@XmlRootElement
@Schema(description = "Represents a multi-user lock on an object in the system.")
public class ObjectLockSummary {

  /** The session which has this object locked, never <code>null</code> or empty. */
  @Schema(description = "The session id of the user who has this object locked.")
  private String session;

  /** The user who has this object locked, never <code>null</code> or empty. */
  @Schema(description = "The username of the user that has the object locked, never null or empty")
  private String locker;

  /** The remaining lock time, always > 0. */
  @Schema(description = "The remaining lock time, always >0")
  private long remainingTime;

  @Schema(
      description =
          "The date and time that the API client last checked this lock.  Can be used for retries.")
  private String callerAccessTime;

  public Optional<String> getSession() {
    return Optional.ofNullable(session);
  }

  public void setSession(String session) {
    this.session = session;
  }

  public Optional<String> getLocker() {
    return Optional.ofNullable(locker);
  }

  public void setLocker(String locker) {
    this.locker = locker;
  }

  public long getRemainingTime() {
    return remainingTime;
  }

  public void setRemainingTime(long remainingTime) {
    this.remainingTime = remainingTime;
  }

  public Optional<String> getCallerAccessTime() {
    return Optional.ofNullable(callerAccessTime);
  }

  public void setCallerAccessTime(String callerAccessTime) {
    this.callerAccessTime = callerAccessTime;
  }

  public ObjectLockSummary() {}
}
