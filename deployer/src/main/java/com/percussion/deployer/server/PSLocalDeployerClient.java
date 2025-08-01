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
package com.percussion.deployer.server;

import com.percussion.deployer.client.IPSDeployConstants;
import com.percussion.deployer.objectstore.PSArchive;
import com.percussion.deployer.objectstore.PSArchiveInfo;
import com.percussion.deployer.objectstore.PSImportDescriptor;
import com.percussion.deployer.objectstore.PSImportPackage;
import com.percussion.deployer.objectstore.PSValidationResult;
import com.percussion.error.PSDeployException;
import com.percussion.error.PSException;
import com.percussion.error.PSLockedException;
import com.percussion.security.PSSecurityToken;
import com.percussion.server.PSRequest;
import com.percussion.server.job.PSJobException;
import com.percussion.services.error.PSNotFoundException;
import com.percussion.utils.collections.PSMultiValueHashMap;
import com.percussion.utils.request.PSRequestInfo;
import org.apache.commons.lang.Validate;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * Client that enables server-side deployment operations.
 */
public class PSLocalDeployerClient implements IPSPackageInstaller {
    private static final Logger log = LogManager.getLogger(PSLocalDeployerClient.class);

    public PSLocalDeployerClient() {
        // Default constructor
    }

    @Override
    public void installPackage(File packageFile) throws PSDeployException, PSNotFoundException {
        installPackage(packageFile, false);
    }

    @Override
    public void installPackage(File packageFile, boolean shouldValidateVersion) throws PSDeployException, PSNotFoundException {
        Validate.notNull(packageFile, "Package file may not be null");
        var dh = PSDeploymentHandler.getInstance();
        var sessionId = getDeploymentLock(dh);

        try {
            var archive = new PSArchive(packageFile);
            var archiveInfo = archive.getArchiveInfo(true);
            var importDesc = validateArchive(dh, archiveInfo, shouldValidateVersion);
            installArchive(packageFile, importDesc);
        } finally {
            if (sessionId != null) {
                dh.releaseLock(sessionId);
            }
        }
    }

    private String getDeploymentLock(PSDeploymentHandler dh) throws PSLockedException {
        var userId = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
        var sessionId = getRequest().getUserSessionId();
        dh.acquireLock(userId, sessionId, true);
        return sessionId;
    }

    private PSImportDescriptor validateArchive(PSDeploymentHandler dh, PSArchiveInfo info, boolean shouldValidateVersion) throws PSDeployException, PSNotFoundException {
        var results = dh.validateArchive(info, false, false, true, shouldValidateVersion);
        var errors = results.get(IPSDeployConstants.ERROR_KEY);
        handleErrors(info, errors);

        var importDesc = PSImportDescriptor.configureFromArchive(info);
        var validationJob = new PSValidationJob();
        validationJob.validate(importDesc, new PSMockJobHandle(), new PSSecurityToken(getRequest().getUserSession()));

        var validationErrors = importDesc.getImportPackageList().stream()
                .flatMap(pkg -> pkg.getValidationResults().getResults())
                .filter(PSValidationResult::isError)
                .map(result -> result.getDependency().getDisplayIdentifier() + ": " + result.getMessage())
                .toList();

        if (!validationErrors.isEmpty()) {
            handleErrors(info, validationErrors);
        }

        return importDesc;
    }

    private void installArchive(File packageFile, PSImportDescriptor descriptor) throws PSDeployException {
        var importJob = new PSImportJob();
        try {
            importJob.install(getRequest(), packageFile, descriptor, true);
        } catch (PSJobException e) {
            throw new PSDeployException(new PSException(e));
        }
    }

    private PSRequest getRequest() {
        return (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
    }

    private void handleErrors(PSArchiveInfo info, List<String> errors) throws PSDeployException {
        if (!errors.isEmpty()) {
            var msg = "Error installing package " + info.getArchiveRef() + ": " + String.join("\n", errors);
            log.error(msg);
            throw new PSDeployException(new PSException(msg));
        }
    }

    private final class PSMockJobHandle implements IPSJobHandle {
        @Override
        public void updateStatus(String message) {
            // Mock implementation
        }

        @Override
        public boolean isCancelled() {
            return false;
        }
    }
}
