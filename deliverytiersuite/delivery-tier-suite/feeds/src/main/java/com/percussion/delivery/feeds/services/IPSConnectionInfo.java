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
package com.percussion.delivery.feeds.services;

import java.util.Optional;

/**
 * Interface for secure connection information with encrypted credentials support.
 * Implementations should ensure proper handling of sensitive data.
 */
public interface IPSConnectionInfo {
    /**
     * Gets the connection URL.
     * @return URL, may be empty if not set
     */
    Optional<String> getUrl();

    /**
     * Gets the username for authentication.
     * @return username, may be empty if not set
     */
    Optional<String> getUsername();

    /**
     * Gets the encrypted password for authentication.
     * Implementations must ensure this is never exposed in logs or toString().
     * @return encrypted password, may be empty if not set
     */
    Optional<String> getPassword();

    /**
     * Checks if the password is encrypted.
     * @return true if password is encrypted, false otherwise
     */
    boolean isEncrypted();

    /**
     * Gets the unique identifier for this connection.
     * @return connection ID
     */
    long getId();

    /**
     * Creates a safe string representation without sensitive data.
     * @return connection info without password
     */
    default String toSafeString() {
        return String.format("ConnectionInfo{id=%d, url='%s', username='%s', encrypted=%b}",
            getId(),
            getUrl().orElse("<not set>"),
            getUsername().orElse("<not set>"),
            isEncrypted());
    }
}
