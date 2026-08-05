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

package com.percussion.wrapper;

/**
 * Represents the lifecycle states that a managed process can be in.
 *
 * <p>This enumeration is used by {@link StartWrapper} and its subclasses to track and persist the
 * current operational state of each wrapped service (Jetty, Production DTS, Staging DTS) so that
 * the wrapper can resume monitoring across restarts and react to status requests consistently.
 *
 * @author luisteixeira
 */
public enum ProcState {
  /** The service is not installed on this host and therefore cannot be started. */
  NOT_INSTALLED,

  /** The service process is not currently running. */
  STOPPED,

  /** The service process has been launched and is initializing. */
  STARTING,

  /** The service process is running and has reported it is ready to accept work. */
  STARTED,

  /** The service process is in an error state and is not operating normally. */
  ERROR,

  /** A stop request has been issued and the service is shutting down. */
  STOPPING,

  /** The service process failed to start or has terminated unexpectedly. */
  FAILED
}
