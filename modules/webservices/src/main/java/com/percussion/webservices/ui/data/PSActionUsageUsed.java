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
package com.percussion.webservices.ui.data;

/** Usage entry DTO for actions. */
public class PSActionUsageUsed {
  private String userInterfaceName;
  private Long userInterfaceId;
  private String contextName;
  private Long contextId;

  public PSActionUsageUsed() {}

  public PSActionUsageUsed(
      String userInterfaceName, Long userInterfaceId, String contextName, Long contextId) {
    this.userInterfaceName = userInterfaceName;
    this.userInterfaceId = userInterfaceId;
    this.contextName = contextName;
    this.contextId = contextId;
  }

  public String getUserInterfaceName() {
    return userInterfaceName;
  }

  public void setUserInterfaceName(String userInterfaceName) {
    this.userInterfaceName = userInterfaceName;
  }

  public Long getUserInterfaceId() {
    return userInterfaceId;
  }

  public void setUserInterfaceId(Long userInterfaceId) {
    this.userInterfaceId = userInterfaceId;
  }

  public String getContextName() {
    return contextName;
  }

  public void setContextName(String contextName) {
    this.contextName = contextName;
  }

  public Long getContextId() {
    return contextId;
  }

  public void setContextId(Long contextId) {
    this.contextId = contextId;
  }
}
