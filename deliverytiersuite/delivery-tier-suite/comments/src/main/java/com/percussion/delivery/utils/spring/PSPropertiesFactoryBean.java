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
package com.percussion.delivery.utils.spring;

import java.util.List;
import org.springframework.beans.factory.config.PropertiesFactoryBean;

/**
 * Backward-compatible Spring {@link PropertiesFactoryBean} referenced by DTS Spring XML.
 *
 * <p>The DTS test Spring contexts (for comments/likes) expect this type to exist and to accept
 * {@code autoSecure} and {@code securedProperties} setters.
 */
public class PSPropertiesFactoryBean extends PropertiesFactoryBean {

  private boolean autoSecure;
  private List<String> securedProperties;

  public boolean isAutoSecure() {
    return autoSecure;
  }

  public void setAutoSecure(boolean autoSecure) {
    this.autoSecure = autoSecure;
  }

  public List<String> getSecuredProperties() {
    return securedProperties;
  }

  public void setSecuredProperties(List<String> securedProperties) {
    this.securedProperties = securedProperties;
  }
}
