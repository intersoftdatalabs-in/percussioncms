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
 * Client that enables server-side deployment operations 
 * 
 * @author JaySeletz
 *
 */
public class PSLocalDeployerClient implements IPSPackageInstaller
{
    private static final Logger log = LogManager.getLogger(PSLocalDeployerClient.class);
    
    public PSLocalDeployerClient()
    {
        
    }
    
    @Override
    public void installPackage(File packageFile) throws PSDeployException, PSNotFoundException {
        installPackage(packageFile, false);
    }
    
    @Override
    public void installPackage(File packageFile, boolean shouldValidateVersion) throws PSDeployException, PSNotFoundException {
    	Validate.notNull(packageFile);
        PSDeploymentHandler dh = null;
        String sessionId = null;
        
        try
        {
            // make sure file is good
            PSArchive archive = new PSArchive(packageFile);
            PSArchiveInfo archiveInfo = archive.getArchiveInfo(true);
            
            // get the deployment handler
            dh = PSDeploymentHandler.getInstance();
            
            sessionId = getDeploymentLock(dh);
            PSImportDescriptor importDesc = validateArchive(dh, archiveInfo, shouldValidateVersion);
            installArchive(packageFile, importDesc);
        }
        finally
        {
            if (sessionId != null)
                dh.releaseLock(sessionId);
        }
    }

    /**
     * need to get the single lock
     * @param dh 
     * 
     * @return The session id
     * @throws PSLockedException 
     */
    private String getDeploymentLock(PSDeploymentHandler dh) throws PSLockedException
    {
        String userId = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);
        String sessionId = getRequest().getUserSessionId();
        dh.acquireLock(userId, sessionId, true);
        
        return sessionId;
    }

    /**
     * Check the validity of the archive
     * 
     * @param dh The deployment handler to use
     * @param info the archive info to validate
     * 
     * @return A valid import descriptor to use for import 
     *  
     * @throws PSDeployException If there are any errors.
     */
    private PSImportDescriptor validateArchive(PSDeploymentHandler dh, PSArchiveInfo info) throws PSDeployException, PSNotFoundException {
    	return validateArchive(dh, info, false);
    }
    
    /**
     * Check the validity of the archive
     * 
     * @param dh The deployment handler to use
     * @param info the archive info to validate
     * @param shouldValidateVersion <code>false</code> if the version should be skipped for reverted packages on uninstall 
     * of patch
     * 
     * @return A valid import descriptor to use for import 
     *  
     * @throws PSDeployException If there are any errors.
     */
    private PSImportDescriptor validateArchive(PSDeploymentHandler dh, PSArchiveInfo info, 
    		boolean shouldValidateVersion) throws PSDeployException, PSNotFoundException {
        // Validate archive file is valid
        PSMultiValueHashMap<String, String> results = dh.validateArchive(info, false, false, true, shouldValidateVersion);
        List<String> errors = results.get(IPSDeployConstants.ERROR_KEY);
        handleErrors(info, errors);
        
        // now run validation job
        PSImportDescriptor importDesc = PSImportDescriptor.configureFromArchive(info);
        PSValidationJob validationJob = new PSValidationJob();        
        validationJob.validate(importDesc, new PSMockJobHandle(), new PSSecurityToken(getRequest().getUserSession()));
        List<PSImportPackage> packageList = importDesc.getImportPackageList();
        
        List<String> validationErrors = new ArrayList<>();
        for (PSImportPackage importPackage : packageList)
        {
            Iterator<PSValidationResult> valResults = importPackage.getValidationResults().getResults();
            while (valResults.hasNext())
            {
                PSValidationResult result = valResults.next();
                if (!result.isError())
                    continue;
                
                validationErrors.add(result.getDependency().getDisplayIdentifier() + ": " + result.getMessage());
            }
        }
        if (!validationErrors.isEmpty())
            handleErrors(info, validationErrors);
        
        return importDesc;
    }


    /**
     * Install an archive
     * @param packageFile The archive file 
     * @param descriptor The import descriptor to use
     * 
     * @throws PSDeployException If there are any errors. 
     * 
     */
    private void installArchive(File packageFile, PSImportDescriptor descriptor) throws PSDeployException
    {
        PSImportJob importJob = new PSImportJob();
        try
        {
            importJob.install(getRequest(), packageFile, descriptor, true);
        }
        catch (PSJobException e)
        {
            throw new PSDeployException(new PSException(e));
        }
        
    }
    
    private PSRequest getRequest()
    {
        PSRequest request = (PSRequest) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_PSREQUEST);
        return request;
    }

    private void handleErrors(PSArchiveInfo info, List<String> errors) throws PSDeployException
    {
        if (!errors.isEmpty())
        {
            String msg = "Error installing package " + info.getArchiveRef() + ": ";
            
            for (String error : errors)
            {
                msg += "\n" + error;                
            }
            
            log.error(msg);
            throw new PSDeployException(new PSException(msg));
        }
    }


    private final class PSMockJobHandle implements IPSJobHandle
    {
        @Override
        public void updateStatus(String message)
        {
            // TODO Auto-generated method stub
            
        }

        @Override
        public boolean isCancelled()
        {
            // TODO Auto-generated method stub
            return false;
        }
    }
}
