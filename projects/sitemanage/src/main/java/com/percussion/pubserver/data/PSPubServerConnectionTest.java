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
package com.percussion.pubserver.data;

import jakarta.xml.bind.annotation.XmlRootElement;

/**
 * Result of a delivery-server connection test. Carries only the outcome —
 * never server fields, passwords, keys, or passphrases — so the test flow
 * cannot render secrets.
 */
@XmlRootElement(name = "connectionTest")
public class PSPubServerConnectionTest {
  private boolean connected;

  /** Creates an unconnected result for request readers. */
  public PSPubServerConnectionTest() {
    this(false);
  }

  /**
   * Creates a result.
   *
   * @param connected {@code true} when the stored server answered the delivery
   *     handler check
   */
  public PSPubServerConnectionTest(boolean connected) {
    this.connected = connected;
  }

  /** Returns {@code true} when the stored server answered the connection check. */
  public boolean isConnected() {
    return connected;
  }

  /** Sets the connection outcome. */
  public void setConnected(boolean connected) {
    this.connected = connected;
  }
}
