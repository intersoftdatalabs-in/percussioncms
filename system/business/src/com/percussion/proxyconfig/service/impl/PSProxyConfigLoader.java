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
package com.percussion.proxyconfig.service.impl;

import static com.percussion.share.dao.PSSerializerUtils.marshal;
import static com.percussion.share.dao.PSSerializerUtils.unmarshalWithValidation;
import static org.apache.commons.lang3.StringUtils.isNotBlank;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.proxyconfig.data.PSProxyConfig;
import com.percussion.proxyconfig.service.impl.ProxyConfig.Password;
import com.percussion.security.PSEncryptionException;
import com.percussion.security.PSEncryptor;
import com.percussion.utils.io.PathUtils;
import com.percussion.legacy.security.deprecated.PSLegacyEncrypter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Loads proxy configuration from the on-disk proxy-config.xml file, decrypting any stored
 * passwords and re-encrypting them with the current key.
 *
 * @author LucasPiccoli
 */
public class PSProxyConfigLoader {
    public static final Logger log = LogManager.getLogger(PSProxyConfigLoader.class);
    private final List<PSProxyConfig> proxyConfigurations;

    public PSProxyConfigLoader(File configFile) {
        notNull(configFile);
        proxyConfigurations = new ArrayList<>();
        if (configFile.exists()) {
            try {
                readAndEncryptConfigFile(configFile);
            } catch (CloneNotSupportedException e) {
                throw new RuntimeException(e);
            }
        }
    }

    public List<PSProxyConfig> getProxyConfigurations() {
        return List.copyOf(proxyConfigurations);
    }

    private void readAndEncryptConfigFile(File configFile) throws CloneNotSupportedException {
        var config = getProxyConfig(configFile);
        var encrypterKey = PSLegacyEncrypter.getInstance(PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR)).getPartOneKey();
        var configChanged = false;
        for (var s : config.getConfigs()) {
            log.debug("Proxy Configuration: {}", s.getHost());
            var proxyConfig = new PSProxyConfig(s);
            proxyConfigurations.add(proxyConfig);
            configChanged |= processPassword(s.getPassword(), configFile, proxyConfig, encrypterKey);
        }
        if (configChanged) {
            updateConfigFile(configFile, config);
        }
    }

    private boolean processPassword(Password pwd, File configFile, PSProxyConfig proxyConfig, String encrypterKey) {
        if (pwd == null) return false;
        var pwdVal = pwd.getValue();
        String decryptedPassword;
        if (pwd.isEncrypted()) {
            try {
                decryptedPassword = PSEncryptor.decryptProperty(PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR), configFile.getAbsolutePath(), null, pwdVal);
            } catch (PSEncryptionException e) {
                try {
                    decryptedPassword = PSEncryptor.decryptWithOldKey(PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR), pwdVal);
                } catch (PSEncryptionException pe) {
                    decryptedPassword = PSLegacyEncrypter.getInstance(PathUtils.getRxDir(null).getAbsolutePath().concat(PSEncryptor.SECURE_DIR)).decrypt(pwdVal, encrypterKey, null);
                }
                String enc = null;
                try {
                    enc = PSEncryptor.encryptProperty(PathUtils.getRxDir().getAbsolutePath().concat(PSEncryptor.SECURE_DIR), configFile.getAbsolutePath(), null, pwdVal);
                } catch (PSEncryptionException e2) {
                    log.error("Error encrypting password: {}", e2.getMessage(), e2);
                    enc = "";
                }
                pwd.setValue(enc);
                pwd.setEncrypted(Boolean.TRUE);
                proxyConfig.setPassword(decryptedPassword);
                return true;
            }
            proxyConfig.setPassword(decryptedPassword);
            return false;
        }
        return false;
    }

    private void updateConfigFile(File configFile, ProxyConfigurations config) {
        try (var fileWriter = new FileWriter(configFile); var bfWriter = new BufferedWriter(fileWriter)) {
            bfWriter.write(marshal(config));
        } catch (IOException e) {
            log.error("Error writing the proxy configuration file: {}", PSExceptionUtils.getMessageForLog(e));
        }
    }

    private ProxyConfigurations getProxyConfig(File configFile) {
        try (InputStream in = new FileInputStream(configFile)) {
            return unmarshalWithValidation(in, ProxyConfigurations.class);
        } catch (Exception e) {
            var msg = "Unknown Exception";
            var cause = e.getCause();
            if (cause != null && isNotBlank(cause.getLocalizedMessage())) {
                msg = cause.getLocalizedMessage();
            } else if (isNotBlank(e.getLocalizedMessage())) {
                msg = e.getLocalizedMessage();
            }
            log.error("Error getting proxy server configurations from file: {}", msg);
            return new ProxyConfigurations();
        }
    }
}
