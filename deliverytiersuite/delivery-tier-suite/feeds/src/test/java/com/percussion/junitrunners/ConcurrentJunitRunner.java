/*
 * Copyright (c) 2025 Intersoft Data Labs, Inc.
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

package com.percussion.junitrunners;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;

/**
 * JUnit 5 extension that provides concurrent test execution. Ported from the JUnit 4
 * ConcurrentJunitRunner. The test class must be annotated with {@link Concurrent} to configure the
 * number of threads.
 */
public class ConcurrentJunitRunner implements BeforeAllCallback {

  @Override
  public void beforeAll(ExtensionContext context) {
    // Concurrent scheduling is handled at the JUnit Platform level via junit-platform.properties.
    // This extension is retained for annotation-based configuration discovery.
  }

  static final class NamedThreadFactory implements ThreadFactory {
    static final AtomicInteger poolNumber = new AtomicInteger(1);
    final AtomicInteger threadNumber = new AtomicInteger(1);
    final ThreadGroup group;

    NamedThreadFactory(String poolName) {
      group = new ThreadGroup(poolName + "-" + poolNumber.getAndIncrement());
    }

    public Thread newThread(Runnable r) {
      return new Thread(group, r, group.getName() + "-thread-" + threadNumber.getAndIncrement(), 0);
    }
  }
}
