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
import com.percussion.services.assembly.impl.plugin.PSAssemblerBase;

/**
 * Assembler base class that produces an XStream-serialized object as its output payload. Subclasses
 * provide the actual object to serialize via {@link #createObject(IPSAssemblyItem)}.
 *
 * @param <T> the type of object produced by the assembler
 */
public abstract class ObjectAssembler<T> extends PSAssemblerBase {

  /** No-op constructor. */
  public ObjectAssembler() {
    // no-op
  }

  @Override
  public IPSAssemblyResult assembleSingle(IPSAssemblyItem assemblyItem) {
    T object = createObject(assemblyItem);
    return new XStreamAssemblyResult(assemblyItem, object);
  }

  /**
   * Creates the payload object for the supplied assembly item.
   *
   * @param assemblyItem the assembly item being processed
   * @return the object to serialize as the result payload
   */
  public abstract T createObject(IPSAssemblyItem assemblyItem);
}
