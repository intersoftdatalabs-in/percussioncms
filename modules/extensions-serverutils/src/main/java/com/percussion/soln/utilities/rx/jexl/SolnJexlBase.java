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
package com.percussion.soln.utilities.rx.jexl;

import com.percussion.extension.IPSJexlMethod;
import com.percussion.extension.PSJexlUtilBase;
import java.lang.reflect.Method;

/**
 * Base class for JEXL (Java Expression Language) utility functions.
 *
 * <p>Provides common functionality for JEXL function classes including method introspection and
 * string representation of available functions.
 *
 * <p>Subclasses should annotate methods with {@link IPSJexlMethod} to expose them as callable JEXL
 * functions.
 *
 * @author DavidBenua
 * @since 8.0.0
 */
public class SolnJexlBase extends PSJexlUtilBase {

  /** No-op constructor. */
  public SolnJexlBase() {
    // no-op
  }

  /**
   * Returns a string representation of all JEXL methods available in this class. Includes method
   * names and parameter types for each annotated method.
   *
   * @return comma-separated list of method signatures
   */
  @Override
  public String toString() {
    boolean first = true;
    StringBuilder s = new StringBuilder();
    Method[] methods = this.getClass().getMethods();
    for (int i = 0; i < methods.length; i++) {
      Method m = methods[i];
      if (m.isAnnotationPresent(IPSJexlMethod.class)) {
        if (first) first = false;
        else s.append(",");
        s.append(m.getName() + "(");
        Class<?>[] params = m.getParameterTypes();
        for (int j = 0; j < params.length; j++) {
          s.append(params[j].getName());
          if (j < (params.length - 1)) s.append(",");
        }
        s.append(")");
      }
    }
    return s.toString();
  }
}
