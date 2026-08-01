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
package com.percussion.cas;

import com.percussion.cms.PSCmsException;
import com.percussion.data.PSConversionException;
import com.percussion.extension.PSSimpleJavaUdfExtension;
import com.percussion.server.IPSRequestContext;
import com.percussion.system.utils.PSCms;

/** Tests if the addressed content is publishable or not. */
public class PSPublishableContent extends PSSimpleJavaUdfExtension {
  /** Creates a new PSPublishableContent. */
  public PSPublishableContent() {}

  /**
   * This UDF tests whether or not the addressed content is publishable or not. Makes a call to
   * isPublishable method to determine the item's publishable status.
   *
   * @param params extension args: [0] publishable tokens (comma-separated; default y,i), [1]
   *     content id (optional), [2] revision (optional)
   * @param request the request to operate on, not <code>null</code>.
   * @return a <code>Boolean</code> with a value of <code>true</code> if the supplied content item
   *     is publishable, <code>false</code> otherwise.
   * @throws PSConversionException for any missing or invalid required parameter and any other error
   *     that can occur.
   */
  public Object processUdf(Object[] params, IPSRequestContext request)
      throws PSConversionException {
    Boolean result = Boolean.FALSE;
    try {
      result = PSCms.isPublishable(params, request);
    } catch (PSCmsException e) {
      throw new PSConversionException(e.getErrorCode(), e.getErrorArguments());
    }
    return result;
  }
}
