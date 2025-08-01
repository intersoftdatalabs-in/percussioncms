/*
 * Copyright 1999-2023 Percussion Software, Inc.
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

import com.percussion.cms.IPSConstants;
import com.percussion.error.PSExceptionUtils;
import com.percussion.rxfix.PSFixResult;
import com.percussion.rxfix.PSRxFix;
import com.percussion.rxfix.PSRxFix.Entry;
import com.percussion.server.IPSStartupProcess;
import com.percussion.server.IPSStartupProcessManager;
import com.percussion.server.cache.PSCacheManager;
import com.percussion.server.cache.PSCacheProxy;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

/**
 * Startup process to run RxFix data updates at server startup.
 * Only runs fixes specified in the RXFIX property.
 */
public class PSRxFixStartupProcess implements IPSStartupProcess {
    private static final Logger log = LogManager.getLogger(IPSConstants.SERVER_LOG);

    private IPSStartupProcessManager startupProcessManager;

    @Override
    public void doStartupWork(Properties startupProps) {
        var propName = getPropName();
        var propValue = startupProps.getProperty(propName);
        if (StringUtils.isEmpty(propValue)) {
            log.info("Nothing to process");
            return;
        }
        var fixes = Arrays.asList(propValue.split(",\\s*"));
        try {
            var fixer = getFixer(fixes);
            fixer.doFix(false, startupProcessManager);

            var entries = fixer.getEntries();
            for (var e : entries) {
                log.info("Running RxFix Fix: {}", e.getFixname());
                var result = e.getResults();
                if (result != null) {
                    for (var r : result) {
                        log.info(r);
                    }
                }
            }

            if (PSCacheManager.isAvailable()) {
                var cacheManager = PSCacheManager.getInstance();
                cacheManager.flush();
                PSCacheProxy.flushFolderCache();
            }
        } catch (Exception e) {
            log.error("Error running RxFix startup process. Error: {}", PSExceptionUtils.getMessageForLog(e));
        }
        log.info("Finished running data updates.");
    }

    private PSRxFix getFixer(List<String> fixes) throws Exception {
        var fixer = new PSRxFix();
        var iter = fixer.getEntries().iterator();
        while (iter.hasNext()) {
            var entry = iter.next();
            if (fixes.contains(entry.getFix().getSimpleName())) {
                continue;
            }
            iter.remove();
        }
        return fixer;
    }

    static String getPropName() {
        return "RXFIX";
    }

    @Override
    public void setStartupProcessManager(IPSStartupProcessManager mgr) {
        if (mgr != null) {
            startupProcessManager = mgr;
            mgr.addStartupProcess(this);
        }
    }
}
