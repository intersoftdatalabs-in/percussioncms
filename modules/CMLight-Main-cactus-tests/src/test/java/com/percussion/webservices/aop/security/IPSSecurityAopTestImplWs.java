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
package com.percussion.webservices.aop.security;

import com.percussion.webservices.aop.security.data.PSMockDesignObject;
import com.percussion.webservices.aop.security.strategy.PSTestSecurityStrategy;
import java.util.List;

/**
 * Mock web service manager for testing public method patterns for AOP-based security processing.
 * <p>
 * All methods are for test purposes only.
 */
public interface IPSSecurityAopTestImplWs {

    /**
     * Loads all design objects specified by {@link PSSecurityAopTest#getTestAcls()}.
     *
     * @param name Placeholder argument, may be {@code null}.
     * @return List of objects with GUIDs specified by the ACL, never {@code null}.
     */
    List<PSMockDesignObject> loadDesignObjects(String name);

    /**
     * Loads the first design object specified by {@link PSSecurityAopTest#getTestAcls()}.
     *
     * @return Object with the GUID specified by the ACL, never {@code null}.
     */
    PSMockDesignObject loadDesignObject();

    /**
     * Returns all design objects specified by {@link PSSecurityAopTest#getTestAcls()}.
     * Used to test that public find results aren't filtered.
     *
     * @param name Placeholder argument, if {@code null}, a runtime exception is thrown.
     * @return List of objects, never {@code null}.
     */
    List<PSMockDesignObject> findPublicObjects(String name);

    /**
     * No-op method used to test that public save methods aren't protected.
     *
     * @param name Placeholder argument, should not be {@code null}.
     */
    void savePublicObjects(String name);

    /**
     * No-op method used to test that public delete methods aren't protected.
     *
     * @param name Placeholder argument, should not be {@code null}.
     */
    void deletePublicObjects(String name);

    /**
     * Loads the first design object specified by {@link PSSecurityAopTest#getTestAcls()}.
     *
     * @return Object with the GUID specified by the ACL, never {@code null}.
     */
    @IPSWsMethod(ignore = true)
    PSMockDesignObject loadDesignObjectIgnore();

    /**
     * Same as {@link #findPublicObjects(String)} but with a custom strategy.
     *
     * @param name Placeholder argument, if {@code null}, a runtime exception is thrown.
     * @return List of objects, never {@code null}.
     */
    @IPSWsStrategy(PSTestSecurityStrategy.class)
    List<PSMockDesignObject> findPublicObjectsCustom(String name);
}
