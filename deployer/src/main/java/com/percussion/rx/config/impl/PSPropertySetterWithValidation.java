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

package com.percussion.rx.config.impl;

import com.percussion.rx.config.IPSConfigHandler.ObjectState;
import com.percussion.rx.config.IPSPropertySetter;
import com.percussion.rx.config.PSConfigException;
import com.percussion.rx.config.PSConfigValidation;
import com.percussion.services.error.PSNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Provides convenience methods for setters implementing property validation. The derived class must
 * override {@link #validate(String, ObjectState, String, Object, Object)}.
 *
 * @author YuBingChen
 */
public class PSPropertySetterWithValidation extends PSSimplePropertySetter {

  @Override
  /**
   * REST endpoint.
   */
  public List<PSConfigValidation> validate(
      String objName, ObjectState state, IPSPropertySetter setter) {
    var properties = getProperties();
    if (properties == null || properties.isEmpty()) return Collections.emptyList();

    var otherProps = setter.getProperties();
    if (otherProps == null || otherProps.isEmpty()) return Collections.emptyList();

    try {
      var result = new ArrayList<PSConfigValidation>();
      for (var prop : properties.entrySet()) {
        var subResult =
            validate(objName, state, prop.getKey(), prop.getValue(), otherProps.get(prop.getKey()));
        result.addAll(subResult);
      }
      return result;
    } catch (Exception e) {
      var errorMsg = "Failed to validate the name \"" + objName + "\"";
      ms_log.error(errorMsg, e);
      throw new PSConfigException(errorMsg, e);
    }
  }

  /**
   * Validates a specified property for a design object. Override in subclasses for custom
   * validation logic.
   */
  protected List<PSConfigValidation> validate(
      String objName, ObjectState state, String propName, Object propValue, Object otherValue)
      throws PSNotFoundException {
    return Collections.emptyList();
  }

  private static final Logger ms_log = LogManager.getLogger("PSPropertySetterWithValidation");
}
