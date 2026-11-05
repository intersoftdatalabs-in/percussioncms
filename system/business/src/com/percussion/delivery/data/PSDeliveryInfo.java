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
package com.percussion.delivery.data;

import com.percussion.share.data.PSAbstractDataObject;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * This class encapsulates information for a delivery tier. A delivery
 * tier represents the target location of publishing and/or various
 * processing services (forms, etc.).
 */
public class PSDeliveryInfo extends PSAbstractDataObject {
    private static final long serialVersionUID = 1L;

    // Constants for Available Services
    public static final String SERVICE_COMMENTS = "perc-comments-services";
    public static final String SERVICE_FORMS = "perc-form-processor";
    public static final String SERVICE_FEEDS = "feeds";
    public static final String SERVICE_INDEXER = "perc-metadata-services";
    public static final String SERVICE_EXTRACTOR = "perc-metadata-extractor";
    public static final String SERVICE_CACHING = "perc-cache-manager";
    public static final String SERVICE_MEMBERSHIP = "perc-membership-services";
    public static final String SERVICE_THIRDPARTY = "perc-thirdparty-services";
    // SERVICE_INTEGRATIONS (perc-integrations / EMS) removed with GH#706

    private String username;
    private String password;
    private String connectionUrl;
    private String serverType = "PRODUCTION";
    private String adminConnectionUrl;
    private Boolean allowSelfSignedCertificate;
    private List<String> availableServices = Collections.emptyList();
    private String realm;

    public PSDeliveryInfo(String url) {
        this(url, null, null);
    }

    public PSDeliveryInfo(String url, String username, String password) {
        this.connectionUrl = url;
        this.username = username;
        this.password = password;
    }

    public Optional<String> getUsername() {
        return Optional.ofNullable(username);
    }

    public void setUsername(String value) {
        this.username = value;
    }

    public Optional<String> getPassword() {
        return Optional.ofNullable(password);
    }

    public void setPassword(String value) {
        this.password = value;
    }

    public String getUrl() {
        return connectionUrl;
    }

    public void setUrl(String value) {
        this.connectionUrl = value;
    }

    public Optional<String> getAdminUrl() {
        return Optional.ofNullable(adminConnectionUrl);
    }

    public void setAdminUrl(String adminConnectionUrl) {
        this.adminConnectionUrl = adminConnectionUrl;
    }

    public Optional<Boolean> getAllowSelfSignedCertificate() {
        return Optional.ofNullable(allowSelfSignedCertificate);
    }

    public void setAllowSelfSignedCertificate(Boolean allowSelfSignedCertificate) {
        this.allowSelfSignedCertificate = allowSelfSignedCertificate;
    }

    public List<String> getAvailableServices() {
        return availableServices == null ? Collections.emptyList() : Collections.unmodifiableList(availableServices);
    }

    public void setAvailableServices(List<String> availableServices) {
        this.availableServices = availableServices == null ? Collections.emptyList() : availableServices.stream().collect(Collectors.toList());
    }

    public void setServerType(String serverType) {
        this.serverType = serverType;
    }

    public String getServerType() {
        return this.serverType;
    }

    public Optional<String> getRealm() {
        return Optional.ofNullable(realm);
    }

    public void setRealm(String realm) {
        this.realm = realm;
    }
}
