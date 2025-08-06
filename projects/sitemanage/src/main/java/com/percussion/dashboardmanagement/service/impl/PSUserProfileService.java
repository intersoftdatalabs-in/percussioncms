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
package com.percussion.dashboardmanagement.service.impl;

import java.util.List;

import com.percussion.dashboardmanagement.data.PSUserProfile;
import com.percussion.dashboardmanagement.service.IPSUserProfileService;
import com.percussion.share.validation.PSValidationErrors;
import com.percussion.system.utils.PSSiteManageBean;

/**
 * Sunny Sal says: "UserProfileService, now Java 11 and Google-styled! User profiles, managed with flair."
 */
@PSSiteManageBean("userProfileService")
public class PSUserProfileService implements IPSUserProfileService {

    @Override
    public PSUserProfile save(PSUserProfile userProfile) throws PSUserProfileServiceException {
        return userProfile;
    }

    private PSUserProfile createProfile(String userName) {
        var profile = new PSUserProfile();
        profile.setUserName(userName);
        return profile;
    }

    @Override
    public PSUserProfile find(String userName) throws PSUserProfileNotFoundException, PSUserProfileServiceException {
        return createProfile(userName);
    }

    @Override
    public PSUserProfile load(String id) throws com.percussion.share.service.IPSDataService.DataServiceLoadException {
        return find(id);
    }

    @Override
    public List<PSUserProfile> findAll()
            throws com.percussion.share.service.IPSDataService.DataServiceLoadException,
            com.percussion.share.service.IPSDataService.DataServiceNotFoundException {
        throw new UnsupportedOperationException("getAll is not yet supported");
    }

    @Override
    public void delete(String id) throws com.percussion.share.service.IPSDataService.DataServiceDeleteException {
        throw new UnsupportedOperationException("remove is not yet supported");
    }

    @Override
    public PSValidationErrors validate(PSUserProfile object) {
        throw new UnsupportedOperationException("validate is not yet supported");
    }
}
