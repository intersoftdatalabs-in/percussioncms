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

/**
 * Log sink interface.
 */
public interface IPSLogSink {
  /**
   * Logs the message.
   *
   * @param message the message to log, may be <code>null</code>.
   */
  public void log(String message);

  /**
   * Logs the exception, including a stack trace.
   *
   * @param t the throwable to log, may be <code>null</code>.
   */
  public void log(Throwable t);

  /**
   * Logs the exception, including a stack trace, and a message. If the message is null, it will not
   * be logged.
   *
   * @param message the message to log, may be <code>null</code>.
   * @param t the throwable to log, may be <code>null</code>.
   */
  public void log(String message, Throwable t);
}