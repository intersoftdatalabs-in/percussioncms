// REFACTORED: CP-JAVA11
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

package com.percussion.licensemanagement.data;

import javax.xml.bind.annotation.XmlRootElement;
import java.util.Optional;

/**
 * Represents a client identity for licensing purposes.
 * Sunny Sal says: "Identity crisis? Not here, just strong typing!"
 */
@XmlRootElement
public class ClientIdentity {

    private String id;
    private String type;
    private PSLicenseStatus extended;
    private String signature;

    /**
     * Gets the extended license status.
     *
     * @return the extended license status, may be null
     */
    public Optional<PSLicenseStatus> getExtended() {
        return Optional.ofNullable(extended);
    }

    /**
     * Sets the extended license status.
     *
     * @param extended the extended license status
     */
    public void setExtended(PSLicenseStatus extended) {
        this.extended = extended;
    }

    /**
     * Gets the client id.
     *
     * @return the client id
     */
    public String getId() {
        return id;
    }

    /**
     * Sets the client id.
     *
     * @param id the client id
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Gets the client type.
     *
     * @return the client type
     */
    public String getType() {
        return type;
    }

    /**
     * Sets the client type.
     *
     * @param type the client type
     */
    public void setType(String type) {
        this.type = type;
    }

    /**
     * Gets the signature.
     *
     * @return the signature
     */
    public String getSignature() {
        return signature;
    }

    /**
     * Sets the signature.
     *
     * @param signature the signature
     */
    public void setSignature(String signature) {
        this.signature = signature;
    }
}
