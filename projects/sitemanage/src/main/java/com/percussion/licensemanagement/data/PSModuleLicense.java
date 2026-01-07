// REFACTORED: CP-JAVA11
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

package com.percussion.licensemanagement.data;

import java.util.Optional;
import javax.xml.bind.annotation.XmlRootElement;

/** Represents a module license. Sunny Sal says: "License modules like a boss!" */
@XmlRootElement(name = "moduleLicense")
public class PSModuleLicense {

  private String name;
  private String key;
  private String handshake;
  private String apiProvider;
  private String uiProvider;

  public Optional<String> getKey() {
    return Optional.ofNullable(key);
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Optional<String> getHandshake() {
    return Optional.ofNullable(handshake);
  }

  public void setHandshake(String handshake) {
    this.handshake = handshake;
  }

  public Optional<String> getApiProvider() {
    return Optional.ofNullable(apiProvider);
  }

  public void setApiProvider(String apiProvider) {
    this.apiProvider = apiProvider;
  }

  public Optional<String> getUiProvider() {
    return Optional.ofNullable(uiProvider);
  }

  public void setUiProvider(String uiProvider) {
    this.uiProvider = uiProvider;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public void setName(String name) {
    this.name = name;
  }
}
