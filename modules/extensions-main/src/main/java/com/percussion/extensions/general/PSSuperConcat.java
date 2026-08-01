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
package com.percussion.extensions.general;

import com.percussion.extension.PSSimpleJavaUdfExtension;
import com.percussion.server.IPSRequestContext;

/**
 * A simple concatenation extension that joins multiple non-empty parameters into a single string.
 *
 * <p>This UDF (User Defined Function) processes all provided parameters and concatenates them
 * together, skipping any null or empty values. It is useful for combining multiple field values or
 * creating computed strings in Rhythmyx workflows.
 *
 * <p>Example usage in Rhythmyx:
 *
 * <pre>
 * {%sysworkflowid%}_{%syscontentid%} -> use this extension to combine fields
 * </pre>
 *
 * @see PSSimpleJavaUdfExtension
 */
public class PSSuperConcat extends PSSimpleJavaUdfExtension {
  /** Creates a new PSSuperConcat. */
  public PSSuperConcat() {}

  /**
   * Processes the provided parameters and concatenates all non-empty values.
   *
   * <p>This method iterates through all parameters, converts each to a string, trims whitespace,
   * and appends non-empty values to the result. The order of parameters is preserved.
   *
   * @param params the array of parameters to concatenate. May contain null elements or empty
   *     strings. Parameters are processed in order from index 0 to length-1.
   * @param request the request context. This parameter is required but not used by this
   *     implementation. Pass a valid IPSRequestContext; passing null will result in a
   *     NullPointerException.
   * @return a string containing the concatenation of all non-empty parameter values, in the order
   *     they appeared in the input array. Returns an empty string if all parameters are null or
   *     empty.
   * @throws NullPointerException if request is null
   */
  public Object processUdf(Object[] params, IPSRequestContext request) {
    StringBuilder result = new StringBuilder(100);
    int parmCount = params.length;
    for (int i = 0; i < parmCount; i++) {
      if (null != params[i] && params[i].toString().trim().length() > 0) {
        result.append(params[i].toString());
      }
    }

    return result.toString();
  }
}
