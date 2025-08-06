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

// REFACTORED: CP-JAVA11
package com.percussion.share.extension;

import com.percussion.server.IPSStartupProcess;
import com.percussion.server.IPSStartupProcessManager;
import com.percussion.server.PSServer;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.publisher.IPSEditionTaskDef;
import com.percussion.services.publisher.IPSPublisherService;
import com.percussion.services.publisher.PSPublisherServiceLocator;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.services.sitemgr.IPSSiteManager;
import com.percussion.services.sitemgr.PSSiteManagerLocator;
import com.percussion.utils.guid.IPSGuid;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Startup process to remove specific post-edition tasks from publish/unpublish on demand jobs.
 * These tasks aren't necessary for on demand jobs and have been causing these jobs to take quite a long time.
 *
 * @author chriswright
 */
public class PSRemovePostEditionTasks implements IPSStartupProcess {

    private static final Logger log = LogManager.getLogger(PSRemovePostEditionTasks.class);

    private static final String BASE_DIR = PSServer.getBaseConfigDir();
    private static final String WORK_COMPLETED_FILE_LOCATION = BASE_DIR + "/Server/";
    private static final String WORK_COMPLETED_FILE = WORK_COMPLETED_FILE_LOCATION + "PSRemovePostEditionTasks.txt";
    private static final String MESSAGE = "Delete this file to run the PSRemovePostEditionTasks job again."
            + "\nThe job removes the following tasks from unpublish now and publish now jobs:"
            + "\n\nJava/global/percussion/task/perc_PushFeedDescriptorTask"
            + "\nJava/global/percussion/task/sys_flushPublicationCache"
            + "\n\nContact Percussion Technical Support if unsure.";

    @Override
    public void doStartupWork(Properties startupProps) {
        var propName = getPropName();
        if (!"true".equalsIgnoreCase(startupProps.getProperty(propName))) {
            log.info("{} is set to false or missing from startup properties file. Nothing to run.", propName);
            return;
        }

        if (hasWorkBeenCompleted()) {
            log.info("{} has already been completed. Nothing to run.", propName);
            return;
        }

        try {
            var smgr = PSSiteManagerLocator.getSiteManager();
            var psvc = PSPublisherServiceLocator.getPublisherService();
            for (var guid : smgr.getAllSiteIdNames().keySet()) {
                IPSSite site = smgr.loadSite(guid);
                List<IPSEdition> editions = psvc.findAllEditionsBySite(site.getGUID());
                for (var edition : editions) {
                    if (edition.getDisplayTitle().contains("PUBLISH_NOW")) {
                        List<IPSEditionTaskDef> tasks = psvc.loadEditionTasks(edition.getGUID());
                        for (var task : tasks) {
                            if ("Java/global/percussion/task/perc_PushFeedDescriptorTask".equals(task.getExtensionName())) {
                                log.info("Deleting task perc_PushFeedDescriptorTask from edition: {}", edition.getDisplayTitle());
                                psvc.deleteEditionTask(task);
                            } else if ("Java/global/percussion/task/sys_flushPublicationCache".equals(task.getExtensionName())) {
                                log.info("Deleting task sys_flushPublicationCache from edition: {}", edition.getDisplayTitle());
                                psvc.deleteEditionTask(task);
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error removing post-edition tasks within {}", propName, e);
        }

        markAsCompleted();
        log.info("{} has completed. File location: {}", propName, WORK_COMPLETED_FILE);
    }

    @Override
    public void setStartupProcessManager(IPSStartupProcessManager mgr) {
        mgr.addStartupProcess(this);
    }

    /**
     * Creates an empty file in rxconfig/Server to denote that this process has been run.
     * File can be deleted to re-run this process.
     */
    public void markAsCompleted() {
        var workCompleted = new File(WORK_COMPLETED_FILE);
        if (!workCompleted.exists() || !workCompleted.isFile()) {
            try (var fw = new FileWriter(workCompleted); var bw = new BufferedWriter(fw)) {
                workCompleted.createNewFile();
                bw.write(MESSAGE);
            } catch (IOException e) {
                log.error("Unable to create new file to indicate the completion of: {}", getPropName(), e);
            }
        }
    }

    /**
     * Determines if the work required for this startup task has already been completed.
     *
     * @return true if the work has been completed.
     */
    public boolean hasWorkBeenCompleted() {
        var workCompleted = new File(WORK_COMPLETED_FILE);
        return workCompleted.isFile();
    }

    static String getPropName() {
        return PSRemovePostEditionTasks.class.getSimpleName();
    }
}
