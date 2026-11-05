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

import static org.apache.commons.lang3.StringUtils.equalsIgnoreCase;
import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.proxyconfig.data.PSProxyConfig;
import com.percussion.proxyconfig.service.IPSProxyConfigService;
import com.percussion.server.PSServer;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

/**
 * Default {@link IPSProxyConfigService} implementation that reads and caches the proxy
 * configuration from the configured location, exposing the standard lookup operations.
 *
 * @author LucasPiccoli
 */
public class PSProxyConfigService implements IPSProxyConfigService {
    /**
     * The configuration file path, never <code>null</code>.
     */
    private static final File PROXY_CONFIG_FILE = new File(PSServer.getRxDir(), "rxconfig/Proxy/proxy-config.xml");

    /**
     * A list of configurations specified in the proxy configuration file.
     */
    private List<PSProxyConfig> proxyConfigurations = new ArrayList<>();

    public PSProxyConfigService() {
        if (!configFileExists()) return;
        var proxyConfigLoader = new PSProxyConfigLoader(PROXY_CONFIG_FILE);
        proxyConfigurations = proxyConfigLoader.getProxyConfigurations();
    }

    /**
     * This constructor is for JUnit Testing purposes
     */
    public PSProxyConfigService(File file) {
        var proxyConfigLoader = new PSProxyConfigLoader(file);
        proxyConfigurations = proxyConfigLoader.getProxyConfigurations();
    }

    @Override
    public List<PSProxyConfig> findAll() {
        return List.copyOf(proxyConfigurations);
    }

    @Override
    public Optional<PSProxyConfig> findByProtocol(String protocol) {
        notNull(protocol);
        return proxyConfigurations.stream()
                .filter(proxyConf -> proxyConf.getProtocols().stream()
                        .anyMatch(confProtocol -> equalsIgnoreCase(protocol, confProtocol)))
                .findFirst();
    }

    @Override
    public boolean configFileExists() {
        return PROXY_CONFIG_FILE.exists();
    }
}
