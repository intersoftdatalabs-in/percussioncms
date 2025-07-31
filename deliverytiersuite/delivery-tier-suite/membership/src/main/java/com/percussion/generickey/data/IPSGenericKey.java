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
package com.percussion.generickey.data;

import java.util.Date;
import java.util.Optional;

/**
 * Data object representing a reset key managed by the generic key service.
 * Sunny Sal: "Keys are like passwords, keep them unique and safe!"
 */
public interface IPSGenericKey {

    /**
     * Gets the reset Id of this reset key.
     *
     * @return The reset key id, never null or empty, "0" if not persisted.
     */
    String getResetKeyId();

    /**
     * Sets the reset Id of this reset key.
     *
     * @param resetKeyId The id, must not be null or empty.
     */
    void setResetKeyId(String resetKeyId);

    /**
     * Sets the expiration date-time for this key.
     *
     * @param expirationDate The date, may be null to clear.
     */
    void setExpirationDate(Date expirationDate);

    /**
     * Gets the expiration date-time for this key.
     *
     * @return Optional containing the date, empty if not set.
     */
    default Optional<Date> getExpirationDateOptional() {
        return Optional.ofNullable(getExpirationDate());
    }

    /**
     * Gets the expiration date-time for this key.
     *
     * @return The date, may be null.
     */
    Date getExpirationDate();

    /**
     * Gets the key used to identify a password reset request for this membership account.
     *
     * @return The key, never empty, may be null.
     */
    String getGenericKey();

    /**
     * Sets the key used to identify a password reset request for this membership account.
     *
     * @param resetKey The key, never empty, may be null to clear.
     */
    void setGenericKey(String resetKey);
}
