// REFACTORED: CP-JAVA11
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
package com.percussion.content;

import com.percussion.assetmanagement.web.service.PSAssetServiceRestClient;
import com.percussion.content.data.CM1DataDef.SiteFolder.PageDef;
import com.percussion.content.data.Widget;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSRegion;
import com.percussion.pagemanagement.data.PSRegionBranches;
import com.percussion.pagemanagement.data.PSRegionCode;
import com.percussion.pagemanagement.data.PSRegionNode;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.pagemanagement.web.service.PSPageRestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Creates a page as defined in a {@link PageDef} object in a specified site
 * folder. Also transitions the generated page to a specified state.
 *
 * @author paulhoward
 */
public class PSPageGenerator extends PSItemGenerator<PSPageRestClient> {

    private final PSTemplateGenerator templateGen;
    private final PSAssetServiceRestClient assetClient;
    private final PSWidgetGenerator widgetGen;

    public PSPageGenerator(String baseUrl, String uid, String pw) {
        super(PSPageRestClient.class, baseUrl, uid, pw);
        templateGen = new PSTemplateGenerator(baseUrl, uid, pw);
        assetClient = new PSAssetServiceRestClient(baseUrl);
        widgetGen = new PSWidgetGenerator(baseUrl, uid, pw);
        assetClient.login(uid, pw);
    }

    /**
     * If browser title or link title is not specified in the page, the page
     * name is used for each.
     *
     * @param def  Not null. If link title is not specified, it
     *             defaults to name. If browser title is not specified, it
     *             defaults to link title.
     * @param path Must start with a '/' and is relative to /Sites. Trailing slash
     *             is optional.
     * @return The generated page, never null.
     */
    public PSPage createPage(PageDef def, String path) {
        log.info("Creating page '/Sites" + path + (path.endsWith("/") ? "" : "/") + def.getName() + "' ...");
        var page = new PSPage();
        page.setName(def.getName());
        page.setDescription(def.getMetaPageDescription());
        page.setAdditionalHeadContent(def.getAdditionalHeadContent());
        page.setAfterBodyStartContent(def.getAfterBodyStart());
        page.setBeforeBodyCloseContent(def.getBeforeBodyClose());

        var linkTitle = def.getLinkTitle();
        if (linkTitle == null || linkTitle.trim().isEmpty())
            linkTitle = def.getName();
        page.setLinkTitle(linkTitle);

        var bTitle = def.getPageTitle();
        if (bTitle == null || bTitle.trim().isEmpty())
            bTitle = page.getLinkTitle();
        page.setTitle(bTitle);

        page.setFolderPath("//Sites" + path); // must be internal folder path

        PSTemplateSummary tsum;
        if (def.getBlogPostTemplate() != null) { // If a blog is being created
            var templateBlogPage = def.getTemplateName().get(0) + "-" + def.getBlogPostTemplate();
            tsum = templateGen.findTemplateByName(templateBlogPage);
            // Try to find the duplicated template
            if (tsum == null)
                tsum = templateGen.findTemplateByName(def.getBlogPostTemplate());
            // If there is not a duplicated template, try to get the specified template
            if (tsum == null)
                throw new RuntimeException("templateName for PageDef not found: " + templateBlogPage);
        } else {
            tsum = templateGen.findTemplateByName(def.getTemplateName().get(0));
            if (tsum == null)
                throw new RuntimeException("templateName for PageDef not found: " + def.getTemplateName());
        }

        page.setTemplateId(tsum.getId());

        var regions = new PSRegionBranches();
        page.setRegionBranches(regions);

        List<Widget> widgets = def.getWidget();
        Map<String, List<String>> regionToWidgets = widgetGen.parseRegionWidget(widgets);

        var regionOverrides = new ArrayList<PSRegion>();
        regions.setRegions(regionOverrides);
        for (var regionName : regionToWidgets.keySet()) {
            var r = new PSRegion();
            regionOverrides.add(r);
            r.setRegionId(regionName);
            var children = new ArrayList<PSRegionNode>();
            r.setChildren(children);
            var n = new PSRegionCode();
            n.setTemplateCode("#region(\"" + regionName + "\", \"\", \"\", \"\", \"\")");
            children.add(n);
        }

        widgetGen.createAndAssignWidgets(regionToWidgets, null, regions);

        var result = getRestClient().save(page);
        log.info("Created page " + result.getId());

        widgetGen.linkContent(widgets, null, result);

        transitionToState(result.getId(), def.getTargetStateName());
        return result;
    }
}
