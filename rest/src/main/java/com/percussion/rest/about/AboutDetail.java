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

package com.percussion.rest.about;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Read-only "About" details: server version and license/copyright disclaimer text.
 *
 * <p>Backed by the same {@code com.percussion.server.PSStringResources} bundle keys ({@code
 * copyright}, {@code thirdPartyCopyright}) that are printed to the console at server startup (see
 * issue #1529), so this is a single source of truth shared by the startup log and the UI About
 * dialog.
 */
@XmlRootElement(name = "AboutDetail")
@JsonInclude(JsonInclude.Include.NON_NULL)
@Schema(description = "Server version and third-party license disclaimer, shared with startup log")
public class AboutDetail {

  private String productName;
  private String versionString;
  private String copyright;
  private String thirdPartyCopyright;

  public AboutDetail() {}

  public String getProductName() {
    return productName;
  }

  public void setProductName(String productName) {
    this.productName = productName;
  }

  public String getVersionString() {
    return versionString;
  }

  public void setVersionString(String versionString) {
    this.versionString = versionString;
  }

  public String getCopyright() {
    return copyright;
  }

  public void setCopyright(String copyright) {
    this.copyright = copyright;
  }

  public String getThirdPartyCopyright() {
    return thirdPartyCopyright;
  }

  public void setThirdPartyCopyright(String thirdPartyCopyright) {
    this.thirdPartyCopyright = thirdPartyCopyright;
  }
}
