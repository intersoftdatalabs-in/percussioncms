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
package com.percussion.i18n;

import com.percussion.i18n.rxlt.PSCommandLineProcessor;
import com.percussion.i18n.rxlt.PSRxltMain;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.PSServer;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Date;
import java.util.Properties;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Runs Language Tool at server startup if needed. Installer sets a flag in rxlt/i18n.properties,
 * this class clears the value if tool is run.
 *
 * @author JaySeletz
 */
public class PSI18nStartupManager implements IPSNotificationListener {
  private static final String RUN_AT_STARTUP = "runAtStartup";
  private static final Logger log = LogManager.getLogger(PSI18nStartupManager.class);

  private static final String pattern = "dd_M_yyyy-hh_mm_ss.";
  private static final FastDateFormat dateFormat = FastDateFormat.getInstance(pattern);

  public void setNotificationService(IPSNotificationService notificationService) {
    notificationService.addListener(EventType.CORE_SERVER_INITIALIZED, this);
  }

  private static File generateBackupFile(File file) {
    String fileName = FilenameUtils.getBaseName(file.getAbsolutePath());
    String fileExtension = FilenameUtils.getExtension(file.getName());
    return new File(
        file.getParent(), fileName + "-invalid-" + dateFormat.format(new Date()) + fileExtension);
  }

  public void notifyEvent(PSNotificationEvent notification) {
    if (!EventType.CORE_SERVER_INITIALIZED.equals(notification.getType())) {
      return;
    }
    boolean success = true;
    // see if need to run
    boolean needToRun = getRunFlag();

    File masterFile =
        PSTmxResourceBundle.getMasterResourceFile(PSServer.getRxDir().getAbsolutePath());

    if (masterFile.exists()) {
      boolean backup = false;
      success = PSTmxResourceBundle.getInstance().loadResources();
      if (success && !needToRun) return;

      if (backup) {
        backupInvaidFile(masterFile);
      }
    }

    // run

    // clear cache
    try {
      log.info("Running Language Tool...");
      PSCommandLineProcessor.setDotsEnabled(false);
      success = PSRxltMain.process(false, PSServer.getRxFile("."));
      log.info("Language Tool completed, reloading i18n resources...");

      success |= PSTmxResourceBundle.getInstance().loadResources();

      // reset run flag
      if (success) {
        resetRunFlag();
      }

    } catch (Exception e) {
      log.error("Error reloading 18n resources: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    } finally {
      if (needToRun && !success) resetRunFlag(true);
    }
  }

  private void backupInvaidFile(File masterFile) {
    Exception backupException = null;
    boolean backupSuccess;
    File backupFile = null;
    try {
      backupFile = generateBackupFile(masterFile);
      backupSuccess = masterFile.renameTo(backupFile);
    } catch (Exception e) {
      backupException = e;
      backupSuccess = false;
    }

    if (!backupSuccess) {
      if (backupFile == null)
        log.error(
            "Cannot generate backup filename for {}. Error: {}",
            masterFile.getAbsolutePath(),
            PSExceptionUtils.getMessageForLog(backupException));
      else
        log.error("Could not backup I18n file {} to {}.", masterFile, backupFile.getAbsolutePath());
    }
  }

  private boolean getRunFlag() {
    boolean doRun = false;

    File propFile = getPropFile();

    if (propFile.exists()) {
      Properties props = new Properties();

      try (InputStream in = Files.newInputStream(propFile.toPath())) {
        props.load(in);
        doRun = "true".equalsIgnoreCase(props.getProperty(RUN_AT_STARTUP, "false"));

        if (!doRun) {
          log.info("i18n resources are up to date, Language Tool will not run");
        }
      } catch (IOException e) {
        log.warn(
            "Unable to load file {}, Language Tool will not run. Error: {}",
            propFile,
            e.getMessage());
      }
    } else {
      log.warn("Unable to locate i18n property file {}, Language Tool will not run.", propFile);
    }

    return doRun;
  }

  /**
   * Resolve rxlt.properties under the canonical lowercase {@code i18n} directory, falling back to
   * the legacy uppercase {@code I18n} name when only that exists (case-sensitive installs). Writes
   * always target the canonical path via {@link #getCanonicalPropFile()}.
   */
  private File getPropFile() {
    File canonical = getCanonicalPropFile();
    if (canonical.isFile()) {
      return canonical;
    }
    File legacy =
        new File(PSServer.getBaseConfigDir(), "I18n" + File.separator + "rxlt.properties");
    if (legacy.isFile()) {
      return legacy;
    }
    return canonical;
  }

  /** Canonical lowercase path for rxlt.properties (never create uppercase I18n on Linux). */
  private File getCanonicalPropFile() {
    return new File(PSServer.getBaseConfigDir(), "i18n" + File.separator + "rxlt.properties");
  }

  private void resetRunFlag() {
    resetRunFlag(false);
  }

  private void resetRunFlag(boolean value) {
    File propFile = getCanonicalPropFile();
    Properties props = new Properties();
    props.setProperty(RUN_AT_STARTUP, Boolean.toString(value));

    try {
      File parent = propFile.getParentFile();
      if (parent != null) {
        Files.createDirectories(parent.toPath());
      }
      try (OutputStream out = Files.newOutputStream(propFile.toPath())) {
        props.store(out, null);
      }
    } catch (IOException e) {
      log.warn(
          "Unable to write to file {}. Error: {}", propFile, PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    }
  }
}
