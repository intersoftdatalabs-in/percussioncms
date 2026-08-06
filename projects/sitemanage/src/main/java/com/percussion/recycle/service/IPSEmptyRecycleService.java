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
package com.percussion.recycle.service;

import com.percussion.recycle.data.PSEmptyRecycleResult;
import com.percussion.share.service.exception.PSDataServiceException;

/**
 * Permanently empties purgeable content under the system Recycling root only ({@code
 * //Folders/$System$/Recycling} / finder path {@code /Recycling/}).
 *
 * <p>Never touches live Sites or Assets outside the Recycling tree.
 */
public interface IPSEmptyRecycleService {

  /**
   * Permanently purges all purgeable top-level children under the Recycling root (and their
   * descendants), reusing path deleteFolder {@code shouldPurge=true} semantics for folders and
   * folder-helper purge for leaf items.
   *
   * <p>Idempotent when the bin is already empty: returns a result with {@code alreadyEmpty=true}
   * and zero counts.
   *
   * @return summary of purge attempts; never {@code null}
   * @throws PSEmptyRecycleNotAuthorizedException if the current user is not an Admin
   * @throws PSDataServiceException on authorization lookup failure
   * @throws PSEmptyRecycleException on unexpected bulk failures that abort the operation
   */
  PSEmptyRecycleResult emptyRecyclingBin()
      throws PSDataServiceException, PSEmptyRecycleException, PSEmptyRecycleNotAuthorizedException;

  /** Thrown when a non-Admin attempts to empty the Recycling bin. */
  class PSEmptyRecycleNotAuthorizedException extends Exception {
    private static final long serialVersionUID = 1L;

    public PSEmptyRecycleNotAuthorizedException(String message) {
      super(message);
    }
  }

  /** Thrown when the empty operation cannot complete. */
  class PSEmptyRecycleException extends Exception {
    private static final long serialVersionUID = 1L;

    public PSEmptyRecycleException(String message) {
      super(message);
    }

    public PSEmptyRecycleException(String message, Throwable cause) {
      super(message, cause);
    }
  }
}
