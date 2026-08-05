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
import com.thoughtworks.xstream.XStream;
import java.nio.charset.StandardCharsets;

/**
 * {@link IPSAssemblyResult} implementation whose payload is the XStream-serialized form of a Java
 * object supplied by the caller.
 */
public class XStreamAssemblyResult extends MutableAssemblyResult implements IPSAssemblyResult {
  /** Safe to serialize */
  private static final long serialVersionUID = 1L;

  /** Shared XStream instance, lazily initialized by {@link #getXStream()}. */
  private static XStream xstream;

  /** The object that will be XStream-serialized into the result payload. */
  private Object data;

  /**
   * Creates a new XStream result for the supplied assembly item and payload object.
   *
   * @param assemblyItem the assembly item whose state backs this result
   * @param data the object to serialize, never {@code null}
   */
  public XStreamAssemblyResult(IPSAssemblyItem assemblyItem, Object data) {
    super(assemblyItem, null, "application/xml");
    if (data == null)
      throw new IllegalArgumentException("Data object to be serialized cannot be null");
    this.data = data;
  }

  private XStream getXStream() {
    if (xstream == null) {
      xstream = new XStream();
    }

    return xstream;
  }

  public byte[] getResultData() {
    if (super.getResultData() == null) {
      super.setResultData(getXStream().toXML(getData()).getBytes(StandardCharsets.UTF_8));
    }
    return super.getResultData();
  }

  /**
   * Returns the object that will be serialized as the result payload.
   *
   * @return the data object, may be {@code null}
   */
  public Object getData() {
    return data;
  }

  /**
   * Replaces the object that will be serialized as the result payload.
   *
   * @param data the new data object
   */
  public void setData(Object data) {
    this.data = data;
  }
}
