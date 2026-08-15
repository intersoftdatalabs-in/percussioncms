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

package com.percussion.rest.assembly;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Objects;

/** Result of an assembly administration operation (flush cache, nav reset). */
@XmlRootElement(name = "AssemblyOperationResult")
@Schema(description = "Result of an assembly cache or navigation reset operation")
public class AssemblyOperationResult {

  @Schema(description = "True when the operation completed")
  private boolean ok;

  @Schema(description = "Operator-facing status message")
  private String message;

  public AssemblyOperationResult() {}

  public AssemblyOperationResult(boolean ok, String message) {
    this.ok = ok;
    this.message = message;
  }

  public static AssemblyOperationResult ok(String message) {
    return new AssemblyOperationResult(true, message);
  }

  public boolean isOk() {
    return ok;
  }

  public void setOk(boolean ok) {
    this.ok = ok;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof AssemblyOperationResult that)) {
      return false;
    }
    return ok == that.ok && Objects.equals(message, that.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(ok, message);
  }
}
