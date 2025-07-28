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

import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.security.PSPermissions;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.aop.security.data.PSMockDesignObject;
import java.rmi.RemoteException;
import java.util.List;

/**
 * Mock web service manager for testing design method patterns for AOP-based security processing.
 * <p>
 * All methods are for test purposes only.
 */
public interface IPSSecurityAopTestImplDesignWs {

    /**
     * Returns mock objects with the supplied GUIDs.
     *
     * @param ids List of GUIDs, must not be {@code null}.
     * @param lock {@code true} to lock the GUIDs, {@code false} otherwise.
     * @param overrideLock Used when creating locks.
     * @param session Used when creating locks.
     * @param user Used when creating locks.
     * @return List of mock objects, never {@code null}.
     * @throws PSErrorResultsException if there are any errors.
     */
    List<PSMockDesignObject> loadDesignObjects(
            List<IPSGuid> ids,
            boolean lock,
            boolean overrideLock,
            String session,
            String user
    ) throws PSErrorResultsException;

    /**
     * Returns mock objects for all GUIDs specified by {@link PSSecurityAopTest#getTestAcls()}.
     *
     * @param name Placeholder argument, may be {@code null} or empty. If {@code null}, an error is generated for a fake GUID.
     * @param lock {@code true} to lock the GUIDs, {@code false} otherwise.
     * @param overrideLock Used when creating locks.
     * @param session Used when creating locks.
     * @param user Used when creating locks.
     * @return List of mock objects, never {@code null}.
     * @throws PSErrorResultsException if there are any errors.
     */
    List<PSMockDesignObject> loadDesignObjects(
            String name,
            boolean lock,
            boolean overrideLock,
            String session,
            String user
    ) throws PSErrorResultsException;

    /**
     * Returns a mock object for the first GUID specified by {@link PSSecurityAopTest#getTestAcls()}.
     *
     * @param lock {@code true} to lock the GUID, {@code false} otherwise.
     * @param overrideLock Used when creating locks.
     * @param session Used when creating locks.
     * @param user Used when creating locks.
     * @return The mock object, never {@code null}.
     * @throws PSLockErrorException if the lock fails.
     * @throws RemoteException if there are any other errors.
     */
    PSMockDesignObject loadDesignObject(
            boolean lock,
            boolean overrideLock,
            String session,
            String user
    ) throws PSLockErrorException, RemoteException;

    /**
     * Returns the first GUID specified by {@link PSSecurityAopTest#getTestAcls()} as a string.
     *
     * @param name Placeholder argument, may be {@code null} or empty.
     * @param lock {@code true} to lock the GUID, {@code false} otherwise.
     * @param overrideLock Used when creating locks.
     * @param session Used when creating locks.
     * @param user Used when creating locks.
     * @return The GUID as a string, never {@code null} or empty.
     * @throws PSLockErrorException if the lock fails.
     * @throws RemoteException if there are any other errors.
     */
    String loadDesignObject(
            String name,
            boolean lock,
            boolean overrideLock,
            String session,
            String user
    ) throws PSLockErrorException, RemoteException;

    /**
     * Returns object summaries for all design objects specified by {@link PSSecurityAopTest#getTestAcls()}.
     *
     * @param name Placeholder argument, if {@code null}, a runtime exception is thrown.
     * @return List of summaries, never {@code null}.
     */
    List<IPSCatalogSummary> findDesignObjects(String name);

    /**
     * No-op method used to test that design save methods are protected.
     *
     * @param name Placeholder argument, should not be {@code null}.
     * @param session The current session, must not be {@code null}.
     * @throws PSLockErrorException if locking fails.
     * @throws RemoteException if there are any other errors.
     */
    void saveDesignObject(String name, String session)
            throws PSLockErrorException, RemoteException;

    /**
     * No-op method used to test that design save methods are protected.
     *
     * @param obj Object to save, not {@code null}.
     * @param throwException {@code true} to throw an OBJECT_NOT_FOUND error for all objects supplied, {@code false} otherwise.
     * @param session The current session, must not be {@code null}.
     * @throws PSErrorsException if specified.
     */
    void saveDesignObjects(Object obj, boolean throwException, String session)
            throws PSErrorsException;

    /**
     * No-op method used to test that design delete methods are protected.
     *
     * @param name Placeholder argument, should not be {@code null}.
     * @param session The current session, must not be {@code null}.
     */
    void deleteDesignObject(String name, String session);

    /**
     * No-op method used to test that design delete methods are protected.
     *
     * @param obj Object to delete, not {@code null}.
     * @param throwException {@code true} to throw an OBJECT_NOT_FOUND error for all objects supplied, {@code false} otherwise.
     * @param session The current session, must not be {@code null}.
     * @throws PSErrorsException if specified.
     */
    void deleteDesignObjects(Object obj, boolean throwException, String session)
            throws PSErrorsException;

    /**
     * Same as {@link #findDesignObjects(String)} but with permission specified.
     *
     * @param name Placeholder argument, if {@code null}, a runtime exception is thrown.
     * @return List of summaries, never {@code null}.
     */
    @IPSWsPermission(PSPermissions.DELETE)
    List<IPSCatalogSummary> findDesignObjectsPerm(String name);
}
