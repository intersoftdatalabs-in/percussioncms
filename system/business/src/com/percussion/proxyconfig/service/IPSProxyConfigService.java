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
package com.percussion.proxyconfig.service;

import com.percussion.proxyconfig.data.PSProxyConfig;

import java.util.List;
import java.util.Optional;

/**
 * Contract for the service that reads and queries the proxy-server configuration used by the
 * CMS for outbound HTTP traffic.
 *
 * @author LucasPiccoli
 */
public interface IPSProxyConfigService {
    /**
     * Finds all proxy configurations in the file.
     *
     * @return a list containing all located proxy configurations, will be empty if none are found. Never {@code null}. The list is sorted in the order found in the file.
     */
    List<PSProxyConfig> findAll();

    /**
     * Finds the proxy configuration that supports the given protocol.
     *
     * @param protocol The name of the protocol for which a proxy configuration needs to be retrieved. Cannot be {@code null} or empty. If more than one server runs the specified service, only the first found is returned.
     * @return An {@link Optional} containing the proxy configuration found for the protocol, or empty if no matches were found.
     */
    Optional<PSProxyConfig> findByProtocol(String protocol);

    /**
     * Checks if the proxy configuration file exists.
     *
     * @return {@code true} if the configuration file exists, {@code false} otherwise.
     */
    boolean configFileExists();
}
