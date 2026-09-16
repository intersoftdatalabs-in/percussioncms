/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.cx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Behavioral tests for {@link PSContentExplorerAppletStub} after dropping the JDK applet API. */
public class PSContentExplorerAppletStubTest {

  @Test
  public void storesAndReturnsParameters() {
    PSContentExplorerAppletStub stub = new PSContentExplorerAppletStub();
    stub.setParemeter("SWING", "true");
    assertEquals("true", stub.getParameter("SWING"));
    assertSame(stub, stub.getAppletContext());
    assertFalse(stub.isActive());
  }

  @Test
  public void setParametersReplacesMap() {
    PSContentExplorerAppletStub stub = new PSContentExplorerAppletStub();
    Map<String, String> params = new HashMap<>();
    params.put("view", "CX");
    stub.setParameters(params);
    assertEquals("CX", stub.getParameter("view"));
    assertSame(params, stub.getParameters());
  }

  @Test
  public void getStreamIsUnsupported() {
    PSContentExplorerAppletStub stub = new PSContentExplorerAppletStub();
    assertThrows(UnsupportedOperationException.class, () -> stub.getStream("x"));
  }

  @Test
  public void wiredAppletReadsStubParameters() {
    PSContentExplorerAppletStub stub = new PSContentExplorerAppletStub();
    stub.setParemeter("LABEL", "Desktop");
    PSContentExplorerApplet applet = new PSContentExplorerApplet(true);
    applet.setStub(stub);
    assertEquals("Desktop", applet.getParameter("LABEL"));
  }
}
