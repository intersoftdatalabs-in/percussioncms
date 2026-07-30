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
package com.percussion.packagemanagement;

import static com.percussion.share.spring.PSSpringWebApplicationContextUtils.getWebApplicationContext;

import com.percussion.cms.IPSConstants;
import com.percussion.cms.objectstore.server.PSItemDefManager;
import com.percussion.deployer.server.IPSPackageInstaller;
import com.percussion.maintenance.service.IPSMaintenanceManager;
import com.percussion.maintenance.service.IPSMaintenanceProcess;
import com.percussion.packagemanagement.PSPackageFileEntry.PackageFileStatus;
import com.percussion.rx.services.deployer.IPSPackageUninstaller;
import com.percussion.rx.services.deployer.PSPackageUninstall;
import com.percussion.security.error.PSExceptionUtils;
import com.percussion.server.PSServer;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.jdbc.PSConnectionHelper;
import com.percussion.utils.jdbc.PSJdbcConnectionDiagnostics;
import java.io.*;
import java.util.*;
import java.util.stream.Collectors;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;

/**
 * Handles installing packages when the server is started. Sunny Sal says: "Startup package
 * installer—because every server deserves a fresh start!"
 *
 * @author JaySeletz
 */
@PSSiteManageBean("startupPackageInstaller")
public class PSStartupPkgInstaller implements IPSNotificationListener, IPSMaintenanceProcess {

  private String packageFileListPath;
  private String logFilePath;
  private IPSPackageInstaller packageInstaller;
  private IPSPackageUninstaller packageUninstaller;
  private File packageDir = null;
  private File logFile = null;
  private IPSMaintenanceManager maintenanceManager;
  private IPSNotificationService notificationService;

  private static final Logger log = LogManager.getLogger(IPSConstants.SERVER_LOG);
  private static final String MAINT_PROC_NAME = PSStartupPkgInstaller.class.getName();

  public PSStartupPkgInstaller() {
    log.info("PSStartupPkgInstaller Bean created");
  }

  public void setPackageDir(File packageDir) {
    this.packageDir = packageDir;
  }

  @Autowired
  public void setPackageInstaller(IPSPackageInstaller packageInstaller) {
    this.packageInstaller = packageInstaller;
  }

  @Value("rxconfig/Installer/InstallPackages.xml")
  public void setPackageFileListPath(String packageFileListPath) {
    this.packageFileListPath = packageFileListPath;
  }

  @Value("rxconfig/Installer/InstallPackages.log")
  public void setLogFilePath(String logFilePath) {
    this.logFilePath = logFilePath;
  }

  public IPSPackageUninstaller getPackageUninstaller() {
    return packageUninstaller;
  }

  public void setPackageUninstaller(IPSPackageUninstaller packageUninstaller) {
    this.packageUninstaller = packageUninstaller;
  }

  @Override
  public String getProcessId() {
    return MAINT_PROC_NAME;
  }

  /**
   * Upon server startup, it will look through the packagesInstall.xml for any uninstall or revert
   * entries. For each uninstall entry, it will uninstall the package and remove the entry from the
   * xml. For each revert, we uninstall the package and then mark the package entry as 'pending'. So
   * that it gets reinstalled.
   */
  public void uninstallPackages() {
    startMaintWork();
    PSPackageFileList packageFileList = null;

    appendLogEntry(null, null, false);
    appendLogEntry(
        "Uninstalling packages based on package file list: " + packageFileListPath, null, true);

    try {
      packageFileList = getPackageFileList();
      var entries = packageFileList.getEntries();
      var entriesToUninstall =
          entries.stream()
              .filter(
                  entry ->
                      PackageFileStatus.UNINSTALL.equals(entry.getStatus())
                          || PackageFileStatus.REVERT.equals(entry.getStatus()))
              .collect(Collectors.toList());

      if (entriesToUninstall.isEmpty()) {
        maintenanceManager.workCompleted(this);
        appendLogEntry("No packages to uninstall", null, true);
        packageFileList = null;
        return;
      }

      for (var entry : entriesToUninstall) {
        packageFileList = uninstallPackage(entry, packageFileList);
      }
    } catch (Exception e) {
      log.error("Package failed to uninstall. Error: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
      failMaintWork();
    } finally {
      if (packageFileList != null) {
        savePackageFileList(packageFileList);
      }
    }

    completeMaintWork();
    appendLogEntry("Packages successfully uninstalled", null, true);
  }

  private PSPackageFileList uninstallPackage(
      PSPackageFileEntry entry, PSPackageFileList packageFileList) {
    var packageName = entry.getPackageName();
    var isRevertEntry = entry.getStatus() == PackageFileStatus.REVERT;

    try {
      appendLogEntry("Uninstalling package: " + packageName + "...", null, false);
      doPackageUninstall(packageName, isRevertEntry);

      if (!PackageFileStatus.REVERT.equals(entry.getStatus())) {
        packageFileList.getEntries().remove(entry);
        appendLogEntry(packageName + " uninstalled successfully", null, false);
      } else {
        entry.setStatus(PackageFileStatus.PENDING);
        appendLogEntry(
            "Setting package to 'PENDING' to be reinstalled: " + packageName, null, false);
      }
      return packageFileList;
    } catch (Exception e) {
      appendLogEntry(
          "Package: " + packageName + " failed to uninstall: " + e.getLocalizedMessage(), e, true);
      packageFileList = null;
      failMaintWork();
      return packageFileList;
    }
  }

  protected void doPackageUninstall(String packageName) throws PSNotFoundException {
    doPackageUninstall(packageName, false);
  }

  protected void doPackageUninstall(String packageName, boolean isRevertEntry)
      throws PSNotFoundException {
    packageUninstaller.uninstallPackages(packageName, isRevertEntry);
  }

  /**
   * When the server starts, it notifies the listener which calls upon install packages. This method
   * will then perform maintenance work and attempt to install each package marked pending. If it
   * fails, it will set the specified package as failed, and not continue. Maintenance mode will not
   * end until all packages pass during next server startup.
   */
  public void installPackages() {
    startMaintWork();

    PSPackageFileList packageFileList = null;
    appendLogEntry(null, null, false);
    appendLogEntry(
        "Starting package installation using package file list: " + packageFileListPath,
        null,
        true);
    // Prove H2 NON_KEYWORDS / unquoted VALUE on the same connection package install will use.
    try (var diagConn = PSConnectionHelper.getDbConnection()) {
      appendLogEntry(
          "Package install JDBC diagnostics: "
              + PSJdbcConnectionDiagnostics.describeConnection(diagConn),
          null,
          true);
    } catch (Exception e) {
      appendLogEntry("Package install JDBC diagnostics unavailable: " + e.getMessage(), null, true);
    }

    try {
      packageFileList = getPackageFileList();
      var entries = packageFileList.getEntries();
      var entriesToInstall =
          entries.stream()
              .filter(
                  entry ->
                      !PackageFileStatus.INSTALLED.equals(entry.getStatus())
                          && !PackageFileStatus.UNINSTALL.equals(entry.getStatus()))
              .collect(Collectors.toList());

      if (entriesToInstall.isEmpty()) {
        notifyComplete();
        maintenanceManager.workCompleted(this);
        appendLogEntry("All packages are up to date.", null, true);
        packageFileList = null;
        return;
      }

      var completed = true;
      for (var entry : entriesToInstall) {
        var pkgName = entry.getPackageName();

        try {
          appendLogEntry("Installing package: " + pkgName + "...", null, false);
          var pkgFile = getPackageFile(pkgName);
          packageInstaller.installPackage(pkgFile, entry.getStatus() != PackageFileStatus.REVERT);
          entry.setStatus(PackageFileStatus.INSTALLED);
          appendLogEntry(pkgName + " installed successfully", null, false);
        } catch (Exception e) {
          entry.setStatus(PackageFileStatus.FAILED);
          appendLogEntry(
              "Package: " + pkgName + " failed to install: " + PSExceptionUtils.getMessageForLog(e),
              e,
              true);
          completed = false;
        }
      }

      copyImmutableObjectStore();

      if (completed) {
        notifyComplete();
        completeMaintWork();
        appendLogEntry("Package installation completed", null, true);
      } else {
        failMaintWork();
        appendLogEntry("Package installation aborted due to errors", null, true);
      }
    } catch (Exception e) {
      failMaintWork();
      log.error("Package installation failed: {}", PSExceptionUtils.getMessageForLog(e));
      log.debug(PSExceptionUtils.getDebugMessageForLog(e));
    } finally {
      if (packageFileList != null) {
        savePackageFileList(packageFileList);
      }
    }
  }

  private void copyImmutableObjectStore() {
    if (PSServer.getServerProps() != null
        && StringUtils.equalsIgnoreCase(
            PSServer.getServerProps().getProperty(IPSConstants.SAAS_FLAG), "true")) {
      var mutableDir = new File(PSServer.getRxDir(), "var");
      var objectStoreDir = new File(PSServer.getRxDir(), "ObjectStore");
      var mutableObjectStoreDir = new File(mutableDir, "ObjectStore");

      if (!mutableObjectStoreDir.exists()) {
        try {
          mutableObjectStoreDir.mkdirs();
        } catch (Exception e) {
          log.error(
              "Unable to create mutable object store directory: {}",
              mutableObjectStoreDir.getAbsolutePath());
          throw new RuntimeException(
              "Unable to create mutable object store directory: "
                  + mutableObjectStoreDir.getAbsoluteFile());
        }
      }

      try {
        FileUtils.copyDirectory(objectStoreDir, mutableObjectStoreDir);
      } catch (Exception e) {
        log.error(e);
        if (e instanceof RuntimeException) {
          throw (RuntimeException) e;
        }
        throw new RuntimeException("Failed to copy content type to mutables");
      }
    }
  }

  private void notifyComplete() {
    if (notificationService != null) {
      notificationService.notifyEvent(
          new PSNotificationEvent(EventType.STARTUP_PKG_INSTALL_COMPLETE, null));
    }
  }

  private void completeMaintWork() {
    if (maintenanceManager != null) {
      maintenanceManager.workCompleted(this);
    }
  }

  private void failMaintWork() {
    if (maintenanceManager != null) {
      maintenanceManager.workFailed(this);
    }
  }

  private void startMaintWork() {
    if (maintenanceManager != null) {
      maintenanceManager.startingWork(this);
    }
  }

  private void appendLogEntry(String msg, Exception ex, boolean logToServer) {
    var isError = (ex != null);

    String output;
    if (msg == null) {
      output = "\n";
    } else {
      output =
          FastDateFormat.getInstance("yyyy/MM/dd HH:mm:ss").format(Calendar.getInstance().getTime())
              + ": ";
      if (isError) {
        output += "ERROR: ";
      }
      output += msg;
      output += "\n";
    }

    var file = getLogFile();
    if (file == null) {
      System.out.println(output);
      return;
    }

    Writer writer = null;
    try {
      writer = new FileWriter(file, true);
      IOUtils.write(output, writer);
    } catch (IOException e) {
      log.error(
          "Failed to log entry to log file {}: {}",
          file.getAbsolutePath(),
          PSExceptionUtils.getMessageForLog(e));
    } finally {
      IOUtils.closeQuietly(writer);
    }

    if (logToServer && msg != null) {
      if (isError) {
        log.error(msg);
        log.debug(PSExceptionUtils.getDebugMessageForLog(ex));
      } else {
        log.info(msg);
      }
    }
  }

  private File getLogFile() {
    if (logFilePath == null) {
      return null;
    }

    if (logFile == null) {
      logFile = new File(PSServer.getRxDir(), logFilePath);
    }

    return logFile;
  }

  private File getPackageFile(String packageName) throws IOException {
    var file = new File(getPackageDir(), packageName + ".ppkg");
    if (!file.exists()) {
      throw new IOException("Package file does not exist: " + file.getPath());
    }
    return file;
  }

  private File getPackageDir() {
    if (packageDir == null) {
      packageDir = new File(PSServer.getRxDir(), "Packages/Percussion");
    }
    return packageDir;
  }

  private PSPackageFileList getPackageFileList() throws IOException {
    try (var in = new FileInputStream(new File(PSServer.getRxDir(), packageFileListPath))) {
      var xmlString = IOUtils.toString(in);
      return PSPackageFileList.fromXml(xmlString);
    }
  }

  private void savePackageFileList(PSPackageFileList packageFileList) {
    try (var out = new FileOutputStream(new File(PSServer.getRxDir(), packageFileListPath))) {
      IOUtils.write(packageFileList.toXml(), out);
    } catch (Exception e) {
      log.error(
          "Failed to save package installer results to file {}:{}",
          packageFileListPath,
          PSExceptionUtils.getMessageForLog(e));
    }
  }

  @Override
  public void notifyEvent(PSNotificationEvent notification) {
    if (EventType.CORE_SERVER_POST_INIT.equals(notification.getType())) {
      var itemDefManager = PSItemDefManager.getInstance();

      try {
        itemDefManager.deferUpdateNotifications();
        setPackageUninstaller(new PSPackageUninstall());

        var scheduler =
            (Scheduler)
                getWebApplicationContext()
                    .getBean("org.springframework.scheduling.quartz.SchedulerFactoryBean");
        log.info("Pausing Quartz Scheduler...");
        scheduler.pauseAll();

        uninstallPackages();
        installPackages();

        log.info("Resuming Quartz Scheduler...");
        scheduler.resumeAll();

      } catch (SchedulerException e) {
        log.error(
            "Error pausing/resuming Quartz with message: {}", PSExceptionUtils.getMessageForLog(e));
      } finally {
        itemDefManager.commitUpdateNotifications();
      }
    }
  }

  @Autowired
  public void setNotificationService(IPSNotificationService notificationService) {
    notificationService.addListener(EventType.CORE_SERVER_POST_INIT, this);
    this.notificationService = notificationService;
  }

  @Autowired
  public void setMaintenanceManager(IPSMaintenanceManager maintenanceManager) {
    this.maintenanceManager = maintenanceManager;
  }
}
