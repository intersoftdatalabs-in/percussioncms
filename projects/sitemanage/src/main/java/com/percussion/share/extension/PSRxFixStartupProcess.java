/*
 *     Percussion CMS
 *     Copyright (C) 1999-2020 Percussion Software, Inc.
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
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

public class PSRxFixStartupProcess implements IPSStartupProcess
{
    private static final Logger log = LogManager.getLogger(IPSConstants.SERVER_LOG);

    @Override
    public void doStartupWork(Properties startupProps) throws Exception
    {
        String propName = getPropName();
        String propValue = startupProps.getProperty(propName);
        if (StringUtils.isEmpty(propValue))
        {
            log.info("Nothing to process");
            return;
        }
        List<String> fixes = Arrays.asList(propValue.split(",\\s*"));

        PSRxFix fixer = getFixer(fixes);

        fixer.doFix(false);

        List<PSRxFix.Entry> entries = fixer.getEntries();

        // Print out results
        for (PSRxFix.Entry e : entries)
        {
            log.info("Running RxFix Fix: {}" ,e.getFixname());
            List<PSFixResult> result = e.getResults();
            if (result != null)
            {
                for (PSFixResult r : result)
                {
                    log.info(r);
                }
            }
        }
        
        try {
            if (PSCacheManager.isAvailable()) {
                PSCacheManager cacheManager = PSCacheManager.getInstance();
                cacheManager.flush();
                PSCacheProxy.flushFolderCache();
            }
        }
        catch (Exception e) {
            log.error("Error flushing folder cache. Error: {}",
                    PSExceptionUtils.getMessageForLog(e));
        }

        log.info("Finished running data updates.");
    }

    private PSRxFix getFixer(List<String> fixes) throws Exception
    {
        PSRxFix fixer = new PSRxFix();

        // TODO: only test running fixes that are used by the installer since
        // others fail, and we aren't going to take the time to fix them now.
        Iterator<Entry> iter = fixer.getEntries().iterator();
        while (iter.hasNext())
        {
            Entry entry = iter.next();
            if (fixes.contains(entry.getFix().getSimpleName()))
            {
                // keep these
                continue;
            }

            // remove others
            iter.remove();
        }
        return fixer;
    }

    static String getPropName()
    {
        return "RXFIX";
    }

    /*
     * (non-Javadoc)
     * 
     * @see
     * com.percussion.server.IPSStartupProcess#setStartupProcessManager(com.
     * percussion.server.IPSStartupProcessManager)
     */
    @Override
    public void setStartupProcessManager(IPSStartupProcessManager mgr)
    {
        mgr.addStartupProcess(this);
    }

}
