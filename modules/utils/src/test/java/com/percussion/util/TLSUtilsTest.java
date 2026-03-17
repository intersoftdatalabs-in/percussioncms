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

package com.percussion.util;

import java.io.OutputStream;
import java.io.PrintStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

public class TLSUtilsTest {

  private PrintStream originalOut;
  private final PrintStream silentOut = new PrintStream(OutputStream.nullOutputStream());

  @BeforeEach
  public void muteOutput() {
    originalOut = System.out;
    System.setOut(silentOut);
  }

  @AfterEach
  public void restoreOutput() {
    System.setOut(originalOut);
  }

  @Test
  public void getEnabledCiphers() {
    TLSUtils.getEnabledCiphers();
  }

  @Test
  public void getSecureCiphers() {}

  @Test
  public void getEnabledCiphers1() {}
}
