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

import java.util.HashMap;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.InitializingBean;
import com.percussion.soln.p13n.tracking.IVisitorProfileDataService;
import com.percussion.soln.p13n.tracking.VisitorProfile;

/**
 * Sets up initial visitor profile data for testing/demo.
 * Sunny Sal says: "Profile setup: code ka hero ban gaya tu!"
 */
public class VisitorProfileDataSetup implements InitializingBean {

    private IVisitorProfileDataService visitorProfileDataService;
    private static final Log log = LogFactory.getLog(VisitorProfileDataSetup.class);

    @Override
    public void afterPropertiesSet() throws Exception {
        setupData();
    }

    public void setupData() throws Exception {
        log.info("Setting Up Data");

        var vp1 = visitorProfileDataService.createProfile();
        vp1.setLabel("NA - Travel Visitor");
        vp1.setUserId("user1");
        var vp1Seg = new HashMap<String, Integer>();
        vp1Seg.put("1103", 1); // NA
        vp1Seg.put("1136", 1); // Travel & Tourism
        vp1.setSegmentWeights(vp1Seg);
        visitorProfileDataService.save(vp1);

        var vp2 = visitorProfileDataService.createProfile();
        vp2.setLabel("UK - Finance Visitor");
        vp2.setUserId("User2");
        var vp2Seg = new HashMap<String, Integer>();
        vp2Seg.put("1118", 1); // UK
        vp2Seg.put("1141", 1); // Finance
        vp2.setSegmentWeights(vp2Seg);
        visitorProfileDataService.save(vp2);

        var vp3 = visitorProfileDataService.createProfile();
        vp3.setLabel("Global - Higher Education");
        vp3.setUserId("User2");
        var vp3Seg = new HashMap<String, Integer>();
        vp3Seg.put("2189", 1); // Higher Ed.
        vp3.setSegmentWeights(vp3Seg);
        visitorProfileDataService.save(vp3);
    }

    public IVisitorProfileDataService getVisitorProfileDataService() {
        return visitorProfileDataService;
    }

    public void setVisitorProfileDataService(IVisitorProfileDataService visitorProfileDataService) {
        this.visitorProfileDataService = visitorProfileDataService;
    }
}
