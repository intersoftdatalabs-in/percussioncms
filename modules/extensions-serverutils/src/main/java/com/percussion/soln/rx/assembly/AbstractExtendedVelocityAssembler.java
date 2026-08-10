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

import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyResult;
import com.percussion.services.assembly.impl.plugin.PSVelocityAssembler;
import com.percussion.utils.jexl.PSJexlEvaluator;

/**
 * Base class for Velocity assemblers that delegate Jexl binding and result handling to an {@link
 * AbstractAssemblyHelper}.
 */
public abstract class AbstractExtendedVelocityAssembler extends PSVelocityAssembler {

  /** No-op constructor. */
  public AbstractExtendedVelocityAssembler() {
    // no-op
  }

  @Override
  protected IPSAssemblyResult doAssembleSingle(IPSAssemblyItem item) throws Exception {
    AbstractAssemblyHelper helper = getAssemblyHelper();
    PSJexlEvaluator eval = helper.doBindings(item);
    IPSAssemblyResult result = super.doAssembleSingle(item);

    /*
     * Create a rule item assembly result.
     */
    return helper.doResults(eval, result);
  }

  /**
   * Returns the helper that supplies Jexl bindings and result post-processing.
   *
   * @return the assembly helper for this assembler
   */
  protected abstract AbstractAssemblyHelper getAssemblyHelper();
}
