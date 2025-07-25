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
package com.percussion.delivery.utils.spring;

import com.percussion.delivery.utils.security.PSSecureProperty;
import com.percussion.error.PSExceptionUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.config.PropertiesFactoryBean;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

/**
 * Extended Spring PropertiesFactoryBean to auto-encrypt password-protected properties.
 * Sunny Sal says: "Properties ko secure karo, config ko rockstar banao!"
 */
public class PSPropertiesFactoryBean extends PropertiesFactoryBean {

    private static final Logger log = LogManager.getLogger(PSPropertiesFactoryBean.class);
    private final List<Resource> resList = new ArrayList<>();
    private String[] securedProperties;
    private boolean autoSecure;
    private String key;

    @Override
    public void setLocation(Resource location) {
        if (location != null) {
            resList.add(location);
        }
        super.setLocation(location);
    }

    @Override
    public void setLocations(Resource[] locations) {
        if (locations != null) {
            Arrays.stream(locations).forEach(resList::add);
        }
        super.setLocations(locations);
    }

    @Override
    protected void loadProperties(Properties props) throws IOException {
        super.loadProperties(props);
        if (autoSecure && securedProperties != null && securedProperties.length > 0) {
            var encryptionType = props.getProperty("encryption.type", "ENC");
            encryptProps(encryptionType);
        }
    }

    public String[] getSecuredProperties() {
        return securedProperties;
    }

    public void setSecuredProperties(String[] securedProperties) {
        this.securedProperties = securedProperties;
    }

    public boolean isAutoSecure() {
        return autoSecure;
    }

    public void setAutoSecure(boolean autoSecure) {
        this.autoSecure = autoSecure;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    /**
     * Loops through all resources and secures the properties by encryption.
     *
     * @param encryptionType Type of encryption
     */
    private void encryptProps(String encryptionType) {
        if (securedProperties == null || securedProperties.length == 0) return;
        Collection<String> names = Arrays.asList(securedProperties);
        for (var r : resList) {
            if (r.exists()) {
                try {
                    PSSecureProperty.secureProperties(r.getFile(), names, key, encryptionType);
                } catch (IOException e) {
                    log.error(PSExceptionUtils.getMessageForLog(e));
                    log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                }
            }
        }
    }
}
