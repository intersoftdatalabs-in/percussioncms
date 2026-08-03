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
package com.percussion.rx.config.impl.spring;

import com.percussion.rx.config.IPSBeanProperties;
import com.percussion.rx.config.PSBeanPropertiesLocator;
import java.util.Properties;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.PropertyPlaceholderConfigurer;

/**
 * This class uses {@link PropertyPlaceholderConfigurer} to attempt to resolve a given place-holder
 * 1st, then attempt to resolve the place-holder from the instance of {@link IPSBeanProperties} if
 * it cannot be resolved by its super class.
 *
 * @author YuBingChen
 */
public class PSPropertyPlaceholderConfigurer extends PropertyPlaceholderConfigurer {

  /** Default constructor for use by Spring. */
  public PSPropertyPlaceholderConfigurer() {}

  /**
   * Resolves the supplied placeholder.
   *
   * @param placeholder the placeholder name, may not be <code>null</code>.
   * @param props the properties to use, may not be <code>null</code>.
   * @return the resolved value, may be <code>null</code>.
   */
  @Override
  protected String resolvePlaceholder(String placeholder, Properties props) {
    var value = super.resolvePlaceholder(placeholder, props);
    if (value != null) {
      return value;
    }
    var pMgr = PSBeanPropertiesLocator.getBeanProperties();
    var v = pMgr.getString(placeholder);
    if (v == null) {
      ms_log.warn("Cannot replace placeholder: \"{}\".", placeholder);
    }
    return v;
  }

  /** Logger for this class. */
  private static final Logger ms_log = LogManager.getLogger("PSPropertyPlaceholderConfigurer");
}
