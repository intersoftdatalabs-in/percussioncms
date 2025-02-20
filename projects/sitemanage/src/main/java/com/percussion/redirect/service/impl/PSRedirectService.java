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

package com.percussion.redirect.service.impl;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.percussion.error.PSExceptionUtils;
import com.percussion.licensemanagement.data.PSModuleLicense;
import com.percussion.licensemanagement.service.IPSLicenseService;
import com.percussion.licensemanagement.service.impl.PSLicenseService;
import com.percussion.redirect.data.PSCreateRedirectRequest;
import com.percussion.redirect.data.PSRedirectStatus;
import com.percussion.redirect.data.PSRedirectValidationData;
import com.percussion.redirect.data.PSRedirectValidationData.RedirectPathType;
import com.percussion.redirect.data.PSRedirectValidationResponse;
import com.percussion.redirect.data.PSRedirectValidationResponse.RedirectValidationStatus;
import com.percussion.redirect.service.IPSRedirectService;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.share.dao.IPSFolderHelper;
import com.percussion.share.data.PSItemProperties;
import com.percussion.share.service.IPSDataService.DataServiceLoadException;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.sitemanage.data.PSSiteSummary;
import com.percussion.sitemanage.service.IPSSiteDataService;
import com.percussion.sitemanage.service.IPSSitePublishStatusService;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.service.IPSUtilityService;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

@Service("pSRedirectService")
@Deprecated
public class PSRedirectService implements IPSRedirectService
{
    private static final Logger log = LogManager.getLogger(PSRedirectService.class);

    @Autowired
    private IPSLicenseService licenseService;

    @Autowired
    private IPSFolderHelper folderHelper;

    @Autowired
    private IPSSitePublishStatusService pubStatusService;

    @Autowired
    private IPSUtilityService utilityService;

    @Autowired
    private IPSSiteDataService siteDataService;
    
    @Autowired
    private IPSGuidManager guidMgr;

    @Override
    public PSRedirectValidationResponse validate(PSRedirectValidationData data)
    {
        PSRedirectValidationResponse response = new PSRedirectValidationResponse();
        validateInput(data);
        
        PSModuleLicense licInfo = getLicense();
        if (licInfo == null)
        {
            response.setStatus(RedirectValidationStatus.NoLicense);
            return response;
        }
        response.setRedirectLicense(licInfo);
        RedirectValidationStatus status;
        try
        {
            String path = StringUtils.isBlank(data.getToPath())
                    ? StringUtils.defaultString(data.getFromPath())
                    : StringUtils.defaultString(data.getToPath());
            if(path.startsWith("/Sites/")) {
				path = "/" + path;
			}
            String sitename = getSiteNameFromPath(path);
            PSSiteSummary site = siteDataService.find(sitename,true);
            if(site.getPubInfo() != null){
                response.setBucketName(site.getPubInfo().getBucketName());
            }
            
            status = validatePathStatus(path, data.getType(), site);
        }
        catch (Exception e)
        {
            status =RedirectValidationStatus.Error;
            log.error("Error occurred validating the redirect object, data: {}, Error: {}", getDataAsString(data),PSExceptionUtils.getMessageForLog(e));
			log.debug(PSExceptionUtils.getDebugMessageForLog(e));

            response.setErrorMessage("A redirect rule cannot be created at this time, Please note the time and date and submit this problem to Percussion support.");
        }
        response.setStatus(status);
        return response;
    }

    private void validateInput(PSRedirectValidationData data)
    {
        if(StringUtils.isBlank(data.getFromPath())){
            throw new IllegalArgumentException("fromPath must not be blank");
        }
        
        String fpath = data.getFromPath();
        if(!(fpath.startsWith("/Sites/") || fpath.startsWith("//Sites/"))){
            throw new IllegalArgumentException("fromPath must start with either /Sites/ or //Sites/");
        }
        
        if(StringUtils.isNotBlank(data.getToPath())){
            String tpath = data.getToPath();
            if(!(tpath.startsWith("/Sites/") || tpath.startsWith("//Sites/"))){
                throw new IllegalArgumentException("fromPath must start with either /Sites/ or //Sites/");
            }
        }
        
        if(data.getType()==null){
            throw new IllegalArgumentException("type must not be null");
        }
    }

    private String getDataAsString(PSRedirectValidationData data)
    {
        String temp = "";
        ObjectMapper mapper = new ObjectMapper();
        try
        {
            temp = mapper.writeValueAsString(data);
        }
        catch (Exception e)
        {
            temp = "{\"fromPath\":\"" + data.getFromPath() + "\", \"toPath\":\"" + data.getToPath() + "\"}";
        }
        return temp;
    }
    
    private String getSiteNameFromPath(String path)
    {
        path = path.replace("//Sites/", "");
        String[] paths = path.split("/");
        return paths[0];
    }
    
    @Override
    public  PSModuleLicense getLicense()
    {
        PSModuleLicense licInfo = null;
        try
        {
            licInfo = licenseService.findModuleLicense(PSLicenseService.MODULE_LICENSE_TYPE_REDIRECT);
        }
        catch (Exception e)
        {
            // If there is no redirect license, we don't have to log it for on
            // prem customers.
            if (utilityService.isSaaSEnvironment()) {
				log.error("Error: {}", PSExceptionUtils.getMessageForLog(e));
				log.debug(PSExceptionUtils.getDebugMessageForLog(e));
			}
        }
        return licInfo;
    }

    private RedirectValidationStatus validatePathStatus(String path, RedirectPathType type, PSSiteSummary site) throws DataServiceLoadException, Exception
    {
        if(path.startsWith("/Sites")) {
			path = "/" + path;
		}
        RedirectValidationStatus status = null;
        boolean isSitePublished = isSitePublished(site.getSiteId()+"");
        if (type.equals(RedirectPathType.page))
        {
            PSItemProperties props = folderHelper.findItemProperties(path);
            status = StringUtils.isBlank(props.getLastPublishedDate())
                    ? RedirectValidationStatus.NotPublished
                    : RedirectValidationStatus.Published;
        }
        else if (type.equals(RedirectPathType.site) || type.equals(RedirectPathType.section))
        {
            status = isSitePublished ? RedirectValidationStatus.Published : RedirectValidationStatus.NotPublished;
        }
        else if (type.equals(RedirectPathType.folder))
        {
            status = isSitePublished ? getFolderStatus(path) : RedirectValidationStatus.NotPublished;
        }
        return status;
    }

    private boolean isSitePublished(String siteId) throws PSDataServiceException {
        IPSGuid siteGuid = guidMgr.makeGuid(siteId, PSTypeEnum.SITE);
        return pubStatusService.isSitePublished(siteGuid);
    }
    
    private RedirectValidationStatus getFolderStatus(String path) throws DataServiceLoadException, Exception
    {
        RedirectValidationStatus status = folderHelper.findItems(path).isEmpty()
                ? RedirectValidationStatus.NoChildren
                : RedirectValidationStatus.Published;
        return status;
    }	

	@Override
	public PSRedirectStatus createRedirect(PSCreateRedirectRequest request){

		//Assume failure
		PSRedirectStatus ret = new PSRedirectStatus(PSRedirectStatus.SERVICE_ERROR, "Error.");
		
		Client client = ClientBuilder.newBuilder().newClient();
		

		PSModuleLicense lic = getLicense();
	
		if(lic != null){
			Response response = null;
			try{
			    WebTarget target = client.target(lic.getApiProvider() + "/rest/redirect");
			    target = target.path("entries");
			    Invocation.Builder builder = target.request(MediaType.APPLICATION_JSON_VALUE)
                        .accept(MediaType.APPLICATION_JSON_VALUE)
                        .header("Authorization", "{\"id\":\"" + lic.getKey() + "\",\"type\":\"" + lic.getName() + "\",\"token\":\"" + lic.getHandshake() +"\"}");
			    response = builder.get();
			    
		
			}catch(Exception e){
				log.error("Error creating redirect from {}, to {}, Error: {}", request.getCondition(), request.getRedirectTo(),PSExceptionUtils.getMessageForLog(e));
				log.debug(PSExceptionUtils.getDebugMessageForLog(e));
			}
			if(response != null && response.getStatus() == 200){
				ret.setStatusCode(PSRedirectStatus.SERVICE_OK);
				ret.setMessage("Redirect created successfully.");
			}else{
				if(response != null) {
					ret.setMessage("An error occurred while saving the redirect.  Remote service returned status code: " + response.getStatus());
				}
				else {
					ret.setMessage("An error occurred while saving the redirect.  Remote service returned status code: null");
				}

			}
			
		}
		
		return ret;
	}

	@Override
	public PSRedirectStatus status() {
		PSRedirectStatus ret = new PSRedirectStatus(PSRedirectStatus.SERVICE_ERROR,"Error");

		try{
			PSModuleLicense lic = this.licenseService.findModuleLicense(PSLicenseService.MODULE_LICENSE_TYPE_REDIRECT);
		
			ret.setStatusCode(PSRedirectStatus.SERVICE_OK);
			ret.setMessage(lic.getName() + " is licensed and available.");
		}catch(Exception e){
			ret.setStatusCode(PSRedirectStatus.SERVICE_UNLICENSED);
			ret.setMessage("Unable to locate an activated Redirect Service license.");
		}

		return ret;
	}
    
}
