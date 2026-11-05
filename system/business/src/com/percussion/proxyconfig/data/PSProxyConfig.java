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
package com.percussion.proxyconfig.data;

import com.percussion.proxyconfig.service.impl.ProxyConfig;
import com.percussion.share.data.PSAbstractDataObject;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Contains proxy configuration information to allow traffic for certain protocols going through
 * a proxy server.
 *
 * @author LucasPiccoli
 */
public class PSProxyConfig extends PSAbstractDataObject {
    private static final long serialVersionUID = 1L;

    protected String host;
    protected String port;
    protected String user;
    protected String password;
    protected List<String> protocols;

    public PSProxyConfig() {
        super();
        this.protocols = new ArrayList<>();
    }

    public PSProxyConfig(String host, String port, String user, String password, List<String> protocols) {
        super();
        this.host = host;
        this.port = port;
        this.user = user;
        this.password = password;
        this.protocols = protocols == null ? new ArrayList<>() : new ArrayList<>(protocols);
    }

    public PSProxyConfig(ProxyConfig proxyConfig) {
        this.host = proxyConfig.getHost();
        this.password = Optional.ofNullable(proxyConfig.getPassword())
                .map(p -> p.getValue())
                .orElse(null);
        this.port = proxyConfig.getPort();
        this.user = proxyConfig.getUser();
        this.protocols = proxyConfig.getProtocols() == null ? new ArrayList<>() :
                new ArrayList<>(proxyConfig.getProtocols().getProtocols());
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getPort() {
        return port;
    }

    public void setPort(String port) {
        this.port = port;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public List<String> getProtocols() {
        return protocols == null ? List.of() : List.copyOf(protocols);
    }

    public void setProtocols(List<String> protocols) {
        this.protocols = protocols == null ? new ArrayList<>() : new ArrayList<>(protocols);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), host, port, user, password, protocols);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof PSProxyConfig)) return false;
        if (!super.equals(obj)) return false;
        PSProxyConfig other = (PSProxyConfig) obj;
        return Objects.equals(host, other.host)
                && Objects.equals(port, other.port)
                && Objects.equals(user, other.user)
                && Objects.equals(password, other.password)
                && Objects.equals(protocols, other.protocols);
    }
}
