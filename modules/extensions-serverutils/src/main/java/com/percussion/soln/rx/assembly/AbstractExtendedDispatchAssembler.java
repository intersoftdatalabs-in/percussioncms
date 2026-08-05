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

package com.percussion.soln.rx.assembly;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyResult;
import com.percussion.services.assembly.impl.plugin.PSDispatchAssembler;
import com.percussion.utils.jexl.PSJexlEvaluator;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Base class for {@link PSDispatchAssembler}-style assemblers that delegate Jexl binding and result
 * handling to an {@link AbstractAssemblyHelper}.
 */
public abstract class AbstractExtendedDispatchAssembler extends PSDispatchAssembler {

  /** The log instance to use for this class, never <code>null</code>. */
  private static final Log log = LogFactory.getLog(AbstractExtendedDispatchAssembler.class);

  /** No-op constructor. */
  public AbstractExtendedDispatchAssembler() {
    // no-op
  }

  @Override
  public IPSAssemblyResult assembleSingle(IPSAssemblyItem item) {
    AbstractAssemblyHelper helper = getAssemblyHelper();
    PSJexlEvaluator eval;
    try {
      eval = helper.doBindings(item);
    } catch (Exception e) {
      log.error(PSExceptionUtils.getMessageForLog(e));
      throw new RuntimeException(e);
    }
    IPSAssemblyResult result = super.assembleSingle(item);

    try {
      return helper.doResults(eval, result);
    } catch (Exception e) {
      log.error(e);
      throw new RuntimeException(e);
    }
  }

  /**
   * Returns the helper that supplies Jexl bindings and result post-processing.
   *
   * @return the assembly helper for this assembler
   */
  protected abstract AbstractAssemblyHelper getAssemblyHelper();
}
