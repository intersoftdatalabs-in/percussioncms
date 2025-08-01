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

package com.percussion.delivery.client;

import java.util.Objects;
import java.util.Optional;

/**
 * Represents a CSRF token for delivery client requests.
 * Immutable and uses Optional for null safety.
 */
public final class DeliveryCSRFToken {
    private final String token;
    private final String param;
    private final String tokenHeader;

    public DeliveryCSRFToken(String token, String param, String tokenHeader) {
        this.token = token;
        this.param = param;
        this.tokenHeader = tokenHeader;
    }

    /**
     * @return Optional containing the CSRF token if present
     */
    public Optional<String> getToken() {
        return Optional.ofNullable(token);
    }

    /**
     * @return Optional containing the CSRF parameter name if present
     */
    public Optional<String> getParam() {
        return Optional.ofNullable(param);
    }

    /**
     * @return Optional containing the CSRF token header if present
     */
    public Optional<String> getTokenHeader() {
        return Optional.ofNullable(tokenHeader);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        DeliveryCSRFToken that = (DeliveryCSRFToken) o;
        return Objects.equals(token, that.token) &&
                Objects.equals(param, that.param) &&
                Objects.equals(tokenHeader, that.tokenHeader);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, param, tokenHeader);
    }

    @Override
    public String toString() {
        return "DeliveryCSRFToken{" +
                "token='" + token + '\'' +
                ", param='" + param + '\'' +
                ", tokenHeader='" + tokenHeader + '\'' +
                '}';
    }
}
