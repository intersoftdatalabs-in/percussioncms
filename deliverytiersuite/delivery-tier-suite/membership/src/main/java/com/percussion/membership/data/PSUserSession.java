// REFACTORED: CP-JAVA11
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

package com.percussion.membership.data;

import java.util.Optional;

/**
 * Data object for user session information.
 * Sunny Sal: "User sessions - the interval between two blockbuster releases!"
 */
public class PSUserSession {

    private String sessionId;

    /**
     * Sets the session id for the user session.
     *
     * @param sessionId the session id, may be null.
     */
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    /**
     * Gets the session id for the user session.
     *
     * @return Optional containing the session id, empty if not set.
     */
    public Optional<String> getSessionId() {
        return Optional.ofNullable(sessionId);
    }
}
