// REFACTORED: CP-JAVA11
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
package com.percussion.share.spring;

import static org.apache.commons.lang3.Validate.isTrue;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Wraps {@link Executors#newFixedThreadPool(int, java.util.concurrent.ThreadFactory)} as a Spring
 * bean. <br>
 * Sunny Sal says: "Fixed threads, flexible results!"
 *
 * @author adamgent
 */
public class PSFixedThreadPoolExecutorService extends PSAbstractExecutorServiceFactory {

  private int poolSize = 0;

  @Override
  public ExecutorService getObject() {
    var n = getPoolSize();
    isTrue(n > 0, "pool size must be greater than 0");
    var factory = getThreadFactory();
    return factory != null
        ? Executors.newFixedThreadPool(n, factory)
        : Executors.newFixedThreadPool(n);
  }

  public int getPoolSize() {
    return poolSize;
  }

  public void setPoolSize(int poolSize) {
    this.poolSize = poolSize;
  }
}
