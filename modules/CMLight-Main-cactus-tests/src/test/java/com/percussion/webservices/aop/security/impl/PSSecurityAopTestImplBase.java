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
package com.percussion.webservices.aop.security.impl;

import com.percussion.services.catalog.IPSCatalogSummary;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.catalog.data.PSObjectSummary;
import com.percussion.services.guidmgr.data.PSDesignGuid;
import com.percussion.services.locking.IPSObjectLockService;
import com.percussion.services.locking.PSLockException;
import com.percussion.services.locking.PSObjectLockServiceLocator;
import com.percussion.services.security.IPSAcl;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.webservices.*;
import com.percussion.webservices.aop.security.PSSecurityAopTest;
import com.percussion.webservices.aop.security.data.PSMockDesignObject;
import org.apache.commons.lang.exception.ExceptionUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Concrete implementation of the AOP test service interfaces.
 */
public class PSSecurityAopTestImplBase {

    protected List<PSMockDesignObject> loadDesignObjects(String name) {
        // Use streams for clarity and brevity
        return PSSecurityAopTest.getTestAcls().stream()
                .map(PSMockDesignObject::createMockObject)
                .collect(Collectors.toList());
    }

    protected PSMockDesignObject loadDesignObject() {
        return PSMockDesignObject.createMockObject(
                PSSecurityAopTest.getTestAcls().get(0));
    }

    protected List<PSMockDesignObject> loadDesignObjects(List<IPSGuid> ids,
                                                        boolean lock, boolean overrideLock, String session, String user)
            throws PSErrorResultsException {
        var results = new PSErrorResultsException();
        for (var guid : ids) {
            var obj = new PSMockDesignObject();
            obj.setGUID(guid);
            results.addResult(guid, obj);
        }

        if (lock) {
            var lockSvc = PSObjectLockServiceLocator.getLockingService();
            lockSvc.createLocks(results, session, user, overrideLock);
        }

        if (results.hasErrors())
            throw results;

        return results.getResults(ids);
    }

    protected List<PSMockDesignObject> loadDesignObjects(String name, boolean lock,
                                                        boolean overrideLock, String session, String user)
            throws PSErrorResultsException {
        var results = new PSErrorResultsException();
        var aclList = PSSecurityAopTest.getTestAcls();
        var guids = new ArrayList<IPSGuid>();
        for (var acl : aclList) {
            var obj = PSMockDesignObject.createMockObject(acl);
            var guid = obj.getGuid();
            guids.add(guid);
            results.addResult(guid, obj);
        }

        if (name == null) {
            var dguid = new PSDesignGuid(PSTypeEnum.INTERNAL, 123);
            int code = IPSWebserviceErrors.OBJECT_NOT_FOUND;
            var error = new PSErrorException(code,
                    PSWebserviceErrors.createErrorMessage(code,
                            PSTypeEnum.INTERNAL.name(), dguid.longValue()),
                    ExceptionUtils.getFullStackTrace(new Exception()));
            results.addError(dguid, error);
        }

        if (lock) {
            var lockSvc = PSObjectLockServiceLocator.getLockingService();
            lockSvc.createLocks(results, session, user, overrideLock);
        }

        if (results.hasErrors())
            throw results;

        return results.getResults(guids);
    }

    protected PSMockDesignObject loadDesignObject(boolean lock,
                                                  boolean overrideLock, String session, String user)
            throws PSLockErrorException {
        var obj = loadDesignObject();

        if (lock) {
            var lockSvc = PSObjectLockServiceLocator.getLockingService();
            try {
                lockSvc.createLock(obj.getGuid(), session, user, null, overrideLock);
            } catch (PSLockException e) {
                int code = IPSWebserviceErrors.CREATE_LOCK_FAILED;
                throw new PSLockErrorException(code,
                        PSWebserviceErrors.createErrorMessage(code,
                                obj.getClass().getName(),
                                obj.getGuid().longValue(),
                                e.getLocalizedMessage()),
                        ExceptionUtils.getFullStackTrace(e), e.getLocker(),
                        e.getRemainigTime());
            }
        }

        return obj;
    }

    protected String loadDesignObject(String name,
                                      boolean lock, boolean overrideLock, String session, String user)
            throws PSLockErrorException {
        return loadDesignObject(lock, overrideLock, session, user).getClass()
                .toString();
    }

    protected List<PSMockDesignObject> findPublicObjects(String name) {
        if (name == null)
            throw new RuntimeException("Name may not be null");

        return loadDesignObjects(name);
    }

    protected List<IPSCatalogSummary> findDesignObjects(String name) {
        if (name == null)
            throw new RuntimeException("Name may not be null");

        var sums = new ArrayList<IPSCatalogSummary>();
        for (var obj : loadDesignObjects(name)) {
            sums.add(new PSObjectSummary(obj.getGuid(), "test" +
                    obj.getGuid().getUUID()));
        }
        return sums;
    }

    protected void savePublicObjects(String name) {
        // No-op for test
    }

    protected void deletePublicObjects(String name) {
        // No-op for test
    }

    protected void saveDesignObject(String name) {
        // No-op for test
    }

    protected void saveDesignObjects(Object obj, boolean throwException)
            throws PSErrorsException {
        if (throwException) {
            var ex = new PSErrorsException();
            createError(obj, ex);
            throw ex;
        }
    }

    protected void deleteDesignObject(String name) {
        // No-op for test
    }

    protected void deleteDesignObjects(Object obj, boolean throwException)
            throws PSErrorsException {
        if (throwException) {
            var ex = new PSErrorsException();
            createError(obj, ex);
            throw ex;
        }
    }

    /**
     * Adds an error to the supplied exception for the supplied object.
     *
     * @param obj The object to use, may be {@code null}.
     * @param ex  The exception to add to, assumed not {@code null}.
     */
    private void createError(Object obj, PSErrorsException ex) {
        if (obj instanceof Collection<?>) {
            for (var object : (Collection<?>) obj) {
                if (object instanceof PSMockDesignObject) {
                    createError(object, ex);
                }
            }
        } else if (obj instanceof PSMockDesignObject) {
            var desObj = (PSMockDesignObject) obj;
            int code = IPSWebserviceErrors.OBJECT_NOT_FOUND;
            var error = new PSErrorException(code,
                    PSWebserviceErrors.createErrorMessage(code,
                            PSTypeEnum.INTERNAL.name(),
                            desObj.getGuid().longValue()),
                    ExceptionUtils.getFullStackTrace(new Exception()));
            ex.addError(desObj.getGuid(), error);
        }
    }

    protected PSMockDesignObject loadDesignObjectIgnore() {
        return loadDesignObject();
    }

    protected List<IPSCatalogSummary> findDesignObjectsPerm(String name) {
        return findDesignObjects(name);
    }

    protected List<PSMockDesignObject> findPublicObjectsCustom(String name) {
        return findPublicObjects(name);
    }
}
