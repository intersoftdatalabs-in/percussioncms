/*
 *     Percussion CMS
 *     Copyright (C) 1999-2021 Percussion Software, Inc.
 *
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU Affero General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU Affero General Public License for more details.
 *
 *     Mailing Address:
 *
 *      Percussion Software, Inc.
 *      PO Box 767
 *      Burlington, MA 01803, USA
 *      +01-781-438-9900
 *      support@percussion.com
 *      https://www.percussion.com
 *
 *     You should have received a copy of the GNU Affero General Public License along with this program.  If not, see <https://www.gnu.org/licenses/>
 */
package com.percussion.i18n;

import com.percussion.error.PSExceptionUtils;
import com.percussion.i18n.rxlt.PSCommandLineProcessor;
import com.percussion.i18n.rxlt.PSRxltMain;
import com.percussion.server.PSServer;
import com.percussion.services.notification.IPSNotificationListener;
import com.percussion.services.notification.IPSNotificationService;
import com.percussion.services.notification.PSNotificationEvent;
import com.percussion.services.notification.PSNotificationEvent.EventType;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.time.FastDateFormat;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Date;
import java.util.Properties;


/**
 * Runs Language Tool at server startup if needed.  Installer sets a flag in
 * rxlt/i18n.properties, this class clears the value if tool is run.
 *
 * @author JaySeletz
 *
 */
public class PSI18nStartupManager implements IPSNotificationListener {
    private static final String RUN_AT_STARTUP = "runAtStartup";
    private static final Logger log = LogManager
            .getLogger(PSI18nStartupManager.class);


    private static final String pattern = "dd_M_yyyy-hh_mm_ss.";
    private static final FastDateFormat dateFormat = FastDateFormat.getInstance(pattern);


    public void setNotificationService(
        IPSNotificationService notificationService) {
        notificationService.addListener(EventType.CORE_SERVER_INITIALIZED, this);
    }

    private static File generateBackupFile(File file) {
        String fileName = FilenameUtils.getBaseName(file.getAbsolutePath());
        String fileExtension = FilenameUtils.getExtension(file.getName());
        return new File(file.getParent(), fileName + "-invalid-" +
                dateFormat.format(new Date()) +
                fileExtension);
    }


    public void notifyEvent(PSNotificationEvent notification) {
        if (!EventType.CORE_SERVER_INITIALIZED.equals(notification.getType())) {
            return;
        }
        boolean success = true;
        // see if need to run
        boolean needToRun = getRunFlag();

        File masterFile = PSTmxResourceBundle.getMasterResourceFile(PSServer.getRxDir().getAbsolutePath());

        if (masterFile.exists())
        {
            boolean backup=false;
            try {
                success = PSTmxResourceBundle.getInstance().loadResources();
                if (success && !needToRun)
                    return;
            } catch (IOException | SAXException | ParserConfigurationException e) {
                log.error("Invalid tmx file detected. will backup and regenerate {} Error: {}",
                        masterFile.getAbsolutePath() ,
                        PSExceptionUtils.getMessageForLog(e));
                log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                backup = true;
                success = false;
            }

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
            log.error("Error reloading 18n resources: {}",
                    PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        finally {
            if ( needToRun && !success)
                resetRunFlag(true);
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
                log.error("Cannot generate backup filename for {}. Error: {}",
                        masterFile.getAbsolutePath(),
                        PSExceptionUtils.getMessageForLog(backupException));
            else
                log.error("Could not backup I18n file {} to {}.",
                        masterFile,
                        backupFile.getAbsolutePath());

        }
    }

    private boolean getRunFlag() {
        boolean doRun = false;

        File propFile = getPropFile();

        if (propFile.exists()) {
            Properties props = new Properties();

            try(InputStream in = new FileInputStream(propFile)) {
                props.load(in);
                doRun = "true".equalsIgnoreCase(props.getProperty(
                            RUN_AT_STARTUP, "false"));

                if (!doRun) {
                    log.info(
                        "i18n resources are up to date, Language Tool will not run");
                }
            } catch (IOException e) {
                log.warn("Unable to load file {}, Language Tool will not run. Error: {}",
                        propFile,
                        e.getMessage());
            }
        } else {
            log.warn("Unable to locate i18n property file {}, Language Tool will not run.",
                    propFile);
        }

        return doRun;
    }

    private File getPropFile() {
        return new File(PSServer.getBaseConfigDir(), "I18n/rxlt.properties");
    }

    private void resetRunFlag() {
        resetRunFlag(false);
    }
    private void resetRunFlag(boolean value) {
        File propFile = getPropFile();
        Properties props = new Properties();
        props.setProperty(RUN_AT_STARTUP, Boolean.toString(value));

        try(OutputStream out =new FileOutputStream(propFile) ) {
            props.store(out, null);
        } catch (IOException e) {
            log.warn("Unable to write to file {}. Error: {}",
                    propFile,
                    PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }
}
