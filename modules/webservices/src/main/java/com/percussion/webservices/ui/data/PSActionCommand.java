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

/** Simple wrapper for command used by conversion code and tests. */
public class PSActionCommand {
  private PSActionCommandParametersParameter[] parameters;
  private String url;

  public PSActionCommand() {}

  public PSActionCommand(PSActionCommandParametersParameter[] parameters, String url) {
    this.parameters = parameters;
    this.url = url;
  }

  public PSActionCommandParametersParameter[] getParameters() {
    return parameters;
  }

  public void setParameters(PSActionCommandParametersParameter[] parameters) {
    this.parameters = parameters;
  }

  public String getUrl() {
    return url;
  }

  public void setUrl(String url) {
    this.url = url;
  }
}
