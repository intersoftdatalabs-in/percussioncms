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
package com.percussion.sitemanage.importer;

import com.percussion.sitemanage.importer.dao.IPSImportLogDao;
import java.util.concurrent.Executors;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Saves the import log in a separate thread. Waits for {@link
 * IPSSiteImportLogger#waitForThreads(long)} to ensure all threads have written to the log.
 */
public class PSDeferredLogWriter implements Runnable {
  private static final Logger log = LogManager.getLogger(PSDeferredLogWriter.class);

  private final String siteId;
  private final String desc;
  private final IPSSiteImportLogger logger;
  private final String objectId;
  private final IPSImportLogDao logDao;

  /**
   * Constructs a log writer. Call {@link #saveWhenReady()} to start the deferred log writer thread.
   *
   * @param siteId The site ID.
   * @param desc The description.
   * @param logger The logger.
   * @param objectId The object ID.
   * @param logDao The log DAO.
   */
  public PSDeferredLogWriter(
      String siteId,
      String desc,
      IPSSiteImportLogger logger,
      String objectId,
      IPSImportLogDao logDao) {
    this.siteId = siteId;
    this.desc = desc;
    this.logger = logger;
    this.objectId = objectId;
    this.logDao = logDao;
  }

  /** Starts a thread and waits for the logger to be ready, then saves it. */
  public void saveWhenReady() {
    Executors.newSingleThreadExecutor().execute(this);
  }

  @Override
  public void run() {
    try {
      logger.waitForThreads(60);
      PSSiteImporter.saveImportLog(objectId, logger, logDao, siteId, desc);
    } catch (Exception e) {
      log.error(
          "Failed to save import log for ID {} and type {}: {}",
          objectId,
          logger.getType().name(),
          e.getLocalizedMessage(),
          e);
    }
  }
}
