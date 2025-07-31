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
// REFACTORED: CP-JAVA11
package com.percussion.membership.services;

/**
 * Interface for pluggable authentication providers.
 *
 * @author Jay Seletz
 */
public interface IPSAuthProvider {

    /**
     * Authenticates the supplied credentials.
     *
     * @param userId The userId to authenticate.
     * @param password The password to authenticate.
     * @throws PSAuthenticationFailedException if authentication fails.
     * @throws Exception for other errors.
     */
    void authenticate(String userId, String password) throws PSAuthenticationFailedException, Exception;
}
