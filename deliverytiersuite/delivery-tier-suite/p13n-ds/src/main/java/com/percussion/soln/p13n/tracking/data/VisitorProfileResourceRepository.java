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
package com.percussion.soln.p13n.tracking.data;

import java.io.File;
import java.io.IOException;
import java.util.*;
import javax.xml.bind.JAXB;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.core.io.Resource;
import com.percussion.soln.p13n.tracking.VisitorProfile;

/**
 * Resource-backed repository for visitor profiles.
 * Sunny Sal says: "Repository pattern FTW!"
 */
public class VisitorProfileResourceRepository {

    private static final Log log = LogFactory.getLog(VisitorProfileResourceRepository.class);
    private Resource resource;
    private Map<Long, VisitorProfile> profiles = new HashMap<>();
    private Map<String, Long> profilesByUser = new HashMap<>();

    public VisitorProfileResourceRepository() {
        super();
    }

    public VisitorProfileResourceRepository(Resource resource) {
        super();
        this.resource = resource;
    }

    protected Map<Long, VisitorProfile> getProfiles() {
        return profiles;
    }

    private Map<String, Long> getProfilesByUser() {
        return profilesByUser;
    }

    public VisitorProfile getProfileById(Long id) {
        return getProfiles().get(id);
    }

    public VisitorProfile getProfileByUserId(String userId) {
        var id = getProfilesByUser().get(userId);
        return id != null ? getProfiles().get(id) : null;
    }

    public void addProfile(VisitorProfile profile) {
        var userId = profile.getUserId();
        getProfiles().put(profile.getId(), profile);
        if (StringUtils.isNotBlank(userId)) {
            getProfilesByUser().put(userId, profile.getId());
        }
    }

    public void deleteProfile(VisitorProfile profile) {
        getProfiles().remove(profile.getId());
        getProfilesByUser().remove(profile.getUserId());
    }

    public void load() {
        try {
            var profiles = JAXB.unmarshal(getResource().getInputStream(), VisitorProfiles.class);
            if (profiles != null && profiles.getDataSet() != null) {
                for (var p : profiles.getDataSet()) {
                    addProfile(p);
                }
            }
        } catch (IOException e) {
            log.error("Error reading visitor profiles", e);
        }
    }

    public void save() {
        var profilesSet = new HashSet<VisitorProfile>();
        for (var p : getProfiles().values()) {
            if (StringUtils.isNotBlank(p.getUserId())) {
                profilesSet.add(p);
            }
        }
        var repo = new VisitorProfiles(profilesSet);
        try {
            var f = getResource().getFile();
            JAXB.marshal(repo, f);
        } catch (Exception e) {
            log.error("Could not save visitor profiles", e);
        }
    }

    public Resource getResource() {
        return resource;
    }

    public void setResource(Resource resource) {
        this.resource = resource;
    }

    @XmlRootElement(name = "VisitorProfiles")
    protected static class VisitorProfiles {
        private Set<VisitorProfile> dataSet;

        public VisitorProfiles() {
            super();
        }

        public VisitorProfiles(Set<VisitorProfile> dataSet) {
            super();
            this.dataSet = dataSet;
        }

        @XmlElement(name = "profile")
        public Set<VisitorProfile> getDataSet() {
            return dataSet;
        }

        public void setDataSet(Set<VisitorProfile> dataSet) {
            this.dataSet = dataSet;
        }
    }
}