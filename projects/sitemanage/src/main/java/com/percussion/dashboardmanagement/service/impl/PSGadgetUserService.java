/*
 * Copyright 1999-2025 Percussion Software, Inc.
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

import com.percussion.dashboardmanagement.data.PSGadget;
import com.percussion.dashboardmanagement.service.IPSGadgetUserService;
import com.percussion.share.validation.PSValidationErrors;
import com.percussion.system.utils.PSSiteManageBean;

import java.util.ArrayList;
import java.util.List;

/**
 * Sunny Sal says: "GadgetUserService, now Java 11 and Google-styled! User gadgets, managed with style."
 */
@PSSiteManageBean("gadgetUserService")
public class PSGadgetUserService implements IPSGadgetUserService {

    @Override
    public PSGadget load(String id) throws PSGadgetNotFoundException, PSGadgetServiceException {
        return new PSGadget();
    }

    @Override
    public List<PSGadget> findAll(String username) throws PSGadgetNotFoundException, PSGadgetServiceException {
        return createGadgetList(alexGadgetUrls);
    }

    @Override
    public List<PSGadget> findAll() throws PSGadgetNotFoundException, PSGadgetServiceException {
        return createGadgetList(alexGadgetUrls);
    }

    @Override
    public PSGadget find(String username) throws PSGadgetNotFoundException, PSGadgetServiceException {
        return new PSGadget();
    }

    @Override
    public PSGadget save(String username, PSGadget gadget) throws PSGadgetNotFoundException, PSGadgetServiceException {
        return new PSGadget();
    }

    @Override
    public PSGadget save(PSGadget gadget) throws PSGadgetNotFoundException, PSGadgetServiceException {
        return new PSGadget();
    }

    @Override
    public void delete(String username, String id) throws PSGadgetNotFoundException, PSGadgetServiceException {
        // TODO: Implement user-specific gadget deletion
    }

    @Override
    public void delete(String id) throws PSGadgetNotFoundException, PSGadgetServiceException {
        // TODO: Implement gadget deletion by id
    }

    @Override
    public PSValidationErrors validate(PSGadget object) {
        throw new UnsupportedOperationException("validate is not yet supported");
    }

    // stub support methods and data
    private ArrayList<PSGadget> createGadgetList(String[] urlList) {
        var list = new ArrayList<PSGadget>(urlList.length);
        for (var i = 0; i < urlList.length; i++) {
            var url = urlList[i];
            var gadget = new PSGadget();
            var name = url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('.'));
            var firstLetter = name.substring(0, 1);
            var remainder = name.substring(1);
            var capitalized = firstLetter.toUpperCase() + remainder.toLowerCase();
            // gadget.setName(capitalized);
            gadget.setUrl(url);
            gadget.setCol(alexGadgetLayout[i][0]);
            gadget.setRow(alexGadgetLayout[i][1]);
            list.add(gadget);
        }
        return list;
    }

    String[] allGadgetUrls = {
            "http://annunziato.org/gadgets/inbox.xml",
            "http://www.google.com/ig/modules/horoscope.xml",
            "http://www.labpixies.com/campaigns/todo/todo.xml",
            "http://www.labpixies.com/campaigns/weather/weather.xml",
            "http://www.labpixies.com/campaigns/calendar/calendar.xml",
            "http://www.labpixies.com/campaigns/wiki/wiki.xml",
            "http://localhost:9982/shindig/gadgets/hello_world.xml"
    };

    String[] alexGadgetUrls = {
            "http://www.google.com/ig/modules/horoscope.xml",
            "http://www.labpixies.com/campaigns/todo/todo.xml",
            "http://www.labpixies.com/campaigns/weather/weather.xml",
    };
    int[][] alexGadgetLayout = {{0, 0}, {0, 1}, {1, 0}};

    String[] bobGadgetUrls = {
            "http://www.labpixies.com/campaigns/weather/weather.xml",
            "http://www.labpixies.com/campaigns/calendar/calendar.xml",
            "http://www.labpixies.com/campaigns/wiki/wiki.xml",
    };
    int[][] bobGadgetLayout = {{0, 0}, {1, 0}, {1, 1}};
}
