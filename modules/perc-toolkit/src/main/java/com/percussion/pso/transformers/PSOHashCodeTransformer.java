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
package com.percussion.pso.transformers;

import com.percussion.data.PSConversionException;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSFieldInputTransformer;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionException;
import com.percussion.pso.utils.PSOExtensionParamsHelper;
import com.percussion.server.IPSRequestContext;
import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * PSOHashCodeTransformer class.
 */
public class PSOHashCodeTransformer extends PSDefaultExtension implements IPSFieldInputTransformer {
  /**
   * Creates a new PSOHashCodeTransformer.
   */
  public PSOHashCodeTransformer() {
    // default
  }

  private static final Logger log = LogManager.getLogger(PSOHashCodeTransformer.class);

  private IPSExtensionDef extDef = null;

  /**
   * processUdf operation.
   *
   * @param params the params
   * @param request the request
   * @return the result
   * @throws PSConversionException if an error occurs
   */
  public Object processUdf(Object[] params, IPSRequestContext request)
      throws PSConversionException {

    PSOExtensionParamsHelper helper = new PSOExtensionParamsHelper(extDef, params, request, log);

    return helper.getParameter("source").hashCode();
  }

  /**
   * init operation.
   *
   * @param def the def
   * @param codeRoot the code root
   * @throws PSExtensionException if an error occurs
   */
  @Override
  public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {
    super.init(def, codeRoot);
    extDef = def;
  }
}
