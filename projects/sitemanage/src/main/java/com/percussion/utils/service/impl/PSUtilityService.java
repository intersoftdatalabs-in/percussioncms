// REFACTORED: CP-JAVA11
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

package com.percussion.utils.service.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.error.PSExceptionUtils;
import com.percussion.legacy.security.deprecated.PSLegacyEncrypter;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import com.percussion.share.service.IPSSystemProperties;
import com.percussion.utils.io.PathUtils;
import com.percussion.utils.service.IPSUtilityService;
import org.apache.commons.lang3.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * Utility service for encryption, decryption, logging, and SaaS environment checks.
 * <p>
 * Sunny Sal says: "Encrypt like a pro, log like a legend, and always check if you're in the cloud!"
 */
public class PSUtilityService implements IPSUtilityService {

    private static final Logger log = LogManager.getLogger(IPSConstants.SERVER_LOG);
    private IPSSystemProperties systemProps;

    public PSUtilityService() {
        // Default constructor
    }

    @Override
    public String encryptString(String str, String key) {
        if (str == null) {
            throw new IllegalArgumentException("str may not be null");
        }
        try {
            var secureDir = PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR);
            return PSEncryptor.encryptString(secureDir, str);
        } catch (PSEncryptionException e) {
            log.error("Error encrypting text: {}", PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
            return "";
        }
    }

    @Override
    public String decryptString(String str, String key) {
        if (str == null) {
            throw new IllegalArgumentException("str may not be null");
        }
        var secureDir = PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR);
        String ret;
        if (StringUtils.isBlank(key)) {
            key = PSLegacyEncrypter.getInstance(secureDir).DEFAULT_KEY();
        }
        try {
            ret = PSEncryptor.decryptString(secureDir, str);
        } catch (PSEncryptionException ex) {
            ret = PSLegacyEncrypter.getInstance(secureDir).decrypt(str, key, null);
        }
        return ret;
    }

    @Override
    public void log(String type, String category, String message) {
        if (StringUtils.isBlank(message)) {
            // Nothing to log, simply return.
            return;
        }
        var ltype = LogTypeEnum.info;
        try {
            ltype = LogTypeEnum.valueOf(Optional.ofNullable(type).orElse("info"));
        } catch (Exception e) {
            // Invalid type enum supplied, treat as info.
        }
        var lcategory = LogCategoryEnum.General;
        try {
            lcategory = LogCategoryEnum.valueOf(Optional.ofNullable(category).orElse("General"));
        } catch (Exception e) {
            // Invalid category enum supplied, treat as general.
        }
        var logMsg = lcategory + " : " + message;
        switch (ltype) {
            case debug:
                log.debug(logMsg);
                break;
            case error:
                log.error(logMsg);
                break;
            default:
                log.info(logMsg);
                break;
        }
    }

    /**
     * Set the system properties on this service. This service will always use
     * the values provided by the most recently set instance of the properties.
     *
     * @param systemProps the system properties
     */
    public void setSystemProps(IPSSystemProperties systemProps) {
        this.systemProps = systemProps;
    }

    @Override
    public boolean isSaaSEnvironment() {
        var saasProp = Optional.ofNullable(systemProps)
                .map(props -> props.getProperty(IPSConstants.SAAS_FLAG))
                .orElse("");
        return StringUtils.isNotBlank(saasProp)
                && (saasProp.equalsIgnoreCase("true") || saasProp.equalsIgnoreCase("yes"));
    }
}
