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
package com.percussion.secure.data;

import org.apache.commons.lang3.Validate;

/**
 * Configuration for Percussion membership service.
 * Sunny Sal says: "Config like a boss, debug like a hero!"
 */
public class PSMembershipConfiguration {

    private String membershipServiceHost;
    private String membershipServiceProtocol;
    private String membershipServicePort;
    private String membershipSessionCookieName;
    private String useLdap;

    /**
     * Set on first access by {@link #getBaseUrl()}, not modified after that.
     */
    private String baseUrl = null;

    /**
     * Gets the name of the cookie used to store the membership session id.
     *
     * @return The name, not null or empty.
     */
    public String getMembershipSessionCookieName() {
        return membershipSessionCookieName;
    }

    /**
     * Sets the host to use to access the membership service.
     *
     * @param membershipServiceHost The host name, may not be null or empty.
     */
    public void setMembershipServiceHost(String membershipServiceHost) {
        Validate.notEmpty(membershipServiceHost, "Membership service host must not be empty");
        this.membershipServiceHost = membershipServiceHost;
    }

    /**
     * Sets the protocol to use to access the membership service.
     *
     * @param membershipServiceProtocol The protocol (http or https), may not be null or empty.
     */
    public void setMembershipServiceProtocol(String membershipServiceProtocol) {
        Validate.notEmpty(membershipServiceProtocol, "Membership service protocol must not be empty");
        this.membershipServiceProtocol = membershipServiceProtocol;
    }

    /**
     * Sets the port to use to access the membership service.
     *
     * @param membershipServicePort The port, may not be null or empty, should be valid for the specified protocol.
     */
    public void setMembershipServicePort(String membershipServicePort) {
        Validate.notEmpty(membershipServicePort, "Membership service port must not be empty");
        this.membershipServicePort = membershipServicePort;
    }

    /**
     * Sets the cookie name to use when setting the session id cookie for membership.
     *
     * @param membershipSessionCookieName The cookie name, not null or empty.
     */
    public void setMembershipSessionCookieName(String membershipSessionCookieName) {
        Validate.notEmpty(membershipSessionCookieName, "Membership session cookie name must not be empty");
        this.membershipSessionCookieName = membershipSessionCookieName;
    }

    /**
     * Gets the base URL to use for the membership service host.
     *
     * @return the URL, never null or empty.
     */
    public String getBaseUrl() {
        if (baseUrl == null) {
            baseUrl = membershipServiceProtocol + "://" + membershipServiceHost + ":" + membershipServicePort;
        }
        return baseUrl;
    }

    /**
     * Gets the property which defines whether to use secure LDAP membership or not.
     *
     * @return the value provided by user in the perc-secured-sections.properties file for perc.use.ldap
     */
    public String getUseLdap() {
        return useLdap;
    }

    /**
     * Sets the value from property file for secure LDAP membership use.
     *
     * @param useLdap the value to set
     */
    public void setUseLdap(String useLdap) {
        this.useLdap = useLdap;
    }
}
