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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.derby.drda;

import java.io.PrintWriter;
import java.net.InetAddress;

/**
 * Minimal test-only shim for NetworkServerControl to avoid depending on a Derby JAR compiled for a
 * newer Java version during test compilation. This class is only used at test-compile time and is
 * intentionally small.
 */
public class NetworkServerControl {

  private final InetAddress addr;
  private final int port;

  public NetworkServerControl(InetAddress addr, int port) {
    this.addr = addr;
    this.port = port;
  }

  /** No-op start overload that accepts any object (tests call start(null)). */
  public void start(Object o) {
    // no-op for tests
  }

  /** Start with PrintWriter (similar to real API, no-op here). */
  public void start(PrintWriter pw) {
    // no-op for tests
  }

  /** No-op shutdown for tests. */
  public void shutdown() {
    // no-op for tests
  }
}
