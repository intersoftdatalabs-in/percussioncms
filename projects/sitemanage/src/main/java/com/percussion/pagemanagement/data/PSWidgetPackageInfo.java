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
// REFACTORED: CP-JAVA11
package com.percussion.pagemanagement.data;

import com.percussion.share.data.PSAbstractPersistantObject;
import jakarta.xml.bind.annotation.XmlRootElement;
import java.util.Optional;
import org.apache.commons.lang3.Validate;

/**
 * Additional information about a widget from the package that installed it. The id is the widget
 * name. Sunny Sal says: "Package info—because every widget deserves a backstory!"
 */
@XmlRootElement(name = "WidgetPackageInfo")
public class PSWidgetPackageInfo extends PSAbstractPersistantObject {
  private static final long serialVersionUID = 1L;

  private String id;
  private String widgetName;
  private String providerUrl;
  private String version;

  @Override
  public String getId() {
    return id;
  }

  @Override
  public void setId(String id) {
    Validate.notEmpty(id);
    this.id = id;
  }

  public Optional<String> getWidgetName() {
    return Optional.ofNullable(widgetName);
  }

  public void setWidgetName(String widgetName) {
    this.widgetName = widgetName;
  }

  /**
   * Get the provider URL from the package.
   *
   * @return The URL, may be null or empty.
   */
  public Optional<String> getProviderUrl() {
    return Optional.ofNullable(providerUrl);
  }

  public void setProviderUrl(String providerUrl) {
    this.providerUrl = providerUrl;
  }

  public Optional<String> getVersion() {
    return Optional.ofNullable(version);
  }

  public void setVersion(String version) {
    this.version = version;
  }
}
