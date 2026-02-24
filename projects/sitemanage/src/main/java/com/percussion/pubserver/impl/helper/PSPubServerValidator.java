package com.percussion.pubserver.impl.helper;

import com.percussion.pubserver.IPSPubServerService;
import org.apache.commons.lang3.StringUtils;

/**
 * Simple validation routines formerly in {@link
 * com.percussion.pubserver.impl.PSPubServerService}.
 */
public final class PSPubServerValidator {

    private PSPubServerValidator() {
        // static methods only
    }

    /**
     * Validates a server name, trimming whitespace and enforcing non-blank.
     *
     * @param serverName the name to check, may be {@code null}
     * @return trimmed non-null string
     * @throws IPSPubServerService.PSPubServerServiceException if the name is
     *     blank after trimming
     */
    public static String validateServerName(String serverName)
            throws IPSPubServerService.PSPubServerServiceException {
        serverName = StringUtils.trim(serverName);
        if (StringUtils.isBlank(serverName)) {
            throw new IPSPubServerService.PSPubServerServiceException("The server name cannot be empty.");
        }
        return serverName;
    }
}
