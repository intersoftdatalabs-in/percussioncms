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
package com.percussion.tablefactory;

/**
 * Logging sink contract used by the table factory. Implementations decide where the message and
 * exception data ultimately end up (console, file, network listener, ...).
 *
 * @author chadloder
 * @version 1.2 1999/08/20
 */
public interface LogSink {
  /**
   * Logs the given message.
   *
   * @param message the message to log, never {@code null}
   */
  public void log(String message);

  /**
   * Logs the given throwable, including a stack trace.
   *
   * @param t the throwable to log, never {@code null}
   */
  public void log(Throwable t);

  /**
   * Logs the given message together with the given throwable (including its stack trace). If {@code
   * message} is {@code null} it is not logged.
   *
   * @param message the message to log, may be {@code null}
   * @param t the throwable to log, never {@code null}
   */
  public void log(String message, Throwable t);
}
