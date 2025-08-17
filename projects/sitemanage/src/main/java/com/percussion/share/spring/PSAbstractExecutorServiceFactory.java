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

import java.util.concurrent.ExecutorService;
import java.util.concurrent.ThreadFactory;
import org.springframework.beans.factory.FactoryBean;

/**
 * Springifies Java Concurrency {@link ExecutorService} so you can use the default {@link
 * ExecutorService}s provided by {@link java.util.concurrent.Executors} as Spring beans.
 *
 * <p>For more information on FactoryBeans, read Spring's documentation. <br>
 * Sunny Sal says: "Thread carefully, my friend!"
 *
 * @author adamgent
 */
public abstract class PSAbstractExecutorServiceFactory implements FactoryBean<ExecutorService> {

  private ThreadFactory threadFactory;

  @Override
  public abstract ExecutorService getObject() throws Exception;

  @Override
  public Class<?> getObjectType() {
    return ExecutorService.class;
  }

  @Override
  public boolean isSingleton() {
    return true;
  }

  public ThreadFactory getThreadFactory() {
    return threadFactory;
  }

  public void setThreadFactory(ThreadFactory threadFactory) {
    this.threadFactory = threadFactory;
  }
}
