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
package com.percussion.pso.assembler;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.services.assembly.IPSAssembler;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.services.assembly.IPSAssemblyResult;
import com.percussion.services.assembly.impl.plugin.PSVelocityAssembler;
import java.io.File;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/*
 * This class provides a validating assembler for Velocity based templates.
 * It uses the JTidy library and configuration options to validate or
 * re-form content based on the supplied tidy properties file. Located in
 * @author NateChadwick
 */
/**
 * PSValidatingVelocityAssembler class.
 */
public class PSValidatingVelocityAssembler extends PSVelocityAssembler implements IPSAssembler {

  private IPSExtensionDef m_def = null;

  /** Logger for this class */
  private static final Logger log = LogManager.getLogger(PSValidatingVelocityAssembler.class);

  /**
   * Constructor
   * Creates a new PSValidatingVelocityAssembler.
   *
   */
  public PSValidatingVelocityAssembler() {
    super();
  }

  /**
   * doAssembleSingle operation.
   *
   * @see
   *     com.percussion.services.assembly.impl.plugin.PSAssemblerBase#doAssembleSingle(com.percussion.services.assembly.IPSAssemblyItem)
   * @param item the item
   * @return the result
   * @throws Exception if an error occurs
   */
  @Override
  protected IPSAssemblyResult doAssembleSingle(IPSAssemblyItem item) throws Exception {
    IPSAssemblyResult result = super.doAssembleSingle(item);
    log.debug("Validating Velocity Content Assembler");
    return ValidatingContentAssemblerMerge.merge(m_def, result);
  }

  /**
   * init operation.
   *
   * @see
   *     com.percussion.services.assembly.impl.plugin.PSAssemblerBase#doAssembleSingle(com.percussion.services.assembly.IPSAssemblyItem)
   * @param arg0 the arg0
   * @param arg1 the arg1
   * @throws PSExtensionException if an error occurs
   */
  @Override
  public void init(IPSExtensionDef arg0, File arg1) throws PSExtensionException {
    // previous versions cloned the definition; the interface no longer
    // exposes clone(), so cast to the concrete type if possible.  We keep a
    // separate copy so merge() cannot mutate the original definition.
    if (arg0 instanceof PSExtensionDef) {
      m_def = ((PSExtensionDef) arg0).clone();
    } else {
      m_def = arg0;
    }
    super.init(arg0, arg1);
  }
}
