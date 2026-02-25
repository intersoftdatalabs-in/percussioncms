package com.percussion.pubserver.impl.helper;

import com.percussion.services.contentchange.IPSContentChangeService;
import com.percussion.services.contentchange.data.PSContentChangeType;
import com.percussion.services.sitemgr.IPSSite;
import com.percussion.security.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

/**
 * Helper for clearing the incremental queue for a site.  Pulled out of the
 * main service to keep it smaller and easier to test.
 */
@Component
public class PSPubServerQueueHelper {
    private static final Logger log = LogManager.getLogger(PSPubServerQueueHelper.class);

    private final IPSContentChangeService contentChangeService;

    public PSPubServerQueueHelper(IPSContentChangeService contentChangeService) {
        this.contentChangeService = contentChangeService;
    }

    public void clearLiveIncrementalQueue(IPSSite site) {
        clearIncrementalQueue(site, PSContentChangeType.PENDING_LIVE);
    }

    public void clearStagingIncrementalQueue(IPSSite site) {
        clearIncrementalQueue(site, PSContentChangeType.PENDING_STAGED);
    }

    private void clearIncrementalQueue(IPSSite site, PSContentChangeType changeType) {
        try {
            contentChangeService.deleteChangeEventsForSite(site.getSiteId(), changeType);
        } catch (Exception e) {
            log.error(
                    "Failed to clear the incremental queue for site: {}, Error: {}",
                    site.getName(),
                    PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
    }
}
