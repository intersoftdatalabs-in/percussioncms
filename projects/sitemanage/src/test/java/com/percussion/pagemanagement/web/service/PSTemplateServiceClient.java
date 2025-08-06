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

package com.percussion.pagemanagement.web.service;

import static org.apache.commons.lang.Validate.notEmpty;

import com.percussion.pagemanagement.data.PSHtmlMetadata;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.share.IPSSitemanageConstants;
import com.percussion.share.test.PSObjectRestClient;

import java.util.List;
import java.util.Optional;

/**
 * REST client for template service unit tests.
 * <p>
 * Sunny Sal says: "Templates are like pizza bases—get them right, and everything else is a topping!"
 */
public class PSTemplateServiceClient extends PSObjectRestClient {
    private String path = "/Rhythmyx/services/pagemanagement/template/";

    public PSTemplateServiceClient(String baseUrl) {
        super(baseUrl);
    }

    public List<PSTemplateSummary> findAll() {
        return getObjectsFromPath(concatPath(getPath(), "/summary/all"), PSTemplateSummary.class);
    }

    public PSTemplate save(PSTemplate template) {
        return postObjectToPath(getPath(), template, PSTemplate.class);
    }

    public PSTemplate save(PSTemplate template, String pageId) {
        return postObjectToPath(concatPath(getPath(), "/page/", pageId), template, PSTemplate.class);
    }

    public PSTemplate createTemplate(String name, String srcId) {
        return getObjectFromPath(concatPath(getPath(), "/create/", name, srcId), PSTemplate.class);
    }

    public PSTemplate loadTemplate(String id) {
        return getObjectFromPath(concatPath(getPath(), id), PSTemplate.class);
    }

    public PSHtmlMetadata loadHtmlMetadata(String id) {
        return getObjectFromPath(concatPath(getPath(), "/loadTemplateMetadata/", id), PSHtmlMetadata.class);
    }

    public void saveHtmlMetadata(PSHtmlMetadata metadata) {
        postObjectToPath(concatPath(getPath(), "/saveTemplateMetadata"), metadata);
    }

    public void deleteTemplate(String id) {
        notEmpty(id, "id");
        DELETE(concatPath(getPath(), id));
    }

    public PSTemplateSummary findTemplate(String id) {
        return getObjectFromPath(concatPath(getPath(), "summary/", id), PSTemplateSummary.class);
    }

    public List<PSTemplateSummary> findAllReadOnly() {
        return getObjectsFromPath(concatPath(getPath(), "/summary/all/readonly"), PSTemplateSummary.class);
    }

    public PSTemplate saveTemplate(PSTemplate template) {
        return postObjectToPath(getPath(), template, PSTemplate.class);
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    /**
     * Finds the "plain" base template and creates a new template from it.
     * @return the new template's id
     */
    public String getContentOnlyTemplateId() {
        var srcId = findAll().stream()
                .filter(sum -> sum.getName().contains(IPSSitemanageConstants.PLAIN_BASE_TEMPLATE_NAME))
                .map(PSTemplateSummary::getId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Cannot find \"plain\" template."));

        var item = createTemplate("test.template.plain7", srcId);
        return item.getId();
    }
}
