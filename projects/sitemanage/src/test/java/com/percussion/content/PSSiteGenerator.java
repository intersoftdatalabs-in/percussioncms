// REFACTORED: CP-JAVA11
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
package com.percussion.content;

import com.percussion.content.data.CM1DataDef.SectionDefs.SectionDef;
import com.percussion.content.data.CM1DataDef.SiteDefs.SiteDef;
import com.percussion.pagemanagement.data.PSTemplateSummary;
import com.percussion.sitemanage.data.PSCreateExternalLinkSection;
import com.percussion.sitemanage.data.PSCreateSiteSection;
import com.percussion.sitemanage.data.PSSectionNode;
import com.percussion.sitemanage.data.PSSite;
import com.percussion.sitemanage.data.PSSiteSection;
import com.percussion.sitemanage.data.PSSiteSection.PSSectionTypeEnum;
import com.percussion.sitemanage.web.service.PSSiteRestClient;
import com.percussion.sitemanage.web.service.PSSiteSectionRestClient;

import java.util.List;

import org.apache.commons.lang.StringUtils;

public class PSSiteGenerator extends PSGenerator<PSSiteRestClient> {

    private final PSSiteSectionRestClient sectionClient;
    private final PSTemplateGenerator templateGen;

    public PSSiteGenerator(String baseUrl, String uid, String pw) {
        super(PSSiteRestClient.class, baseUrl, uid, pw);
        sectionClient = new PSSiteSectionRestClient(baseUrl);
        sectionClient.login(uid, pw);
        templateGen = new PSTemplateGenerator(baseUrl, uid, pw);
    }

    /**
     * Create a new section in the system, according with the information supplied.
     *
     * @param def  Used to create the section.
     * @param path The path of the parent section. Of the form
     *             /Sites/sitename/section1/...
     * @return The object that represents the generated section.
     */
    public PSSiteSection createSection(SectionDef def, String parentPath) {
        var sectionType = PSSectionTypeEnum.section;
        if (def.getSectionType() != null) {
            sectionType = PSSectionTypeEnum.valueOf(def.getSectionType());
        }

        if (sectionType == PSSectionTypeEnum.externallink) {
            return createExternalLinkSection(def, parentPath);
        }

        if (sectionType == PSSectionTypeEnum.sectionlink) {
            return createSectionLink(def, parentPath);
        } else {
            return createSiteSection(def, parentPath);
        }
    }

    /**
     * Create a new site in the system, according with the information supplied.
     *
     * @param def Used to create the site.
     * @return The object that represents the generated site.
     */
    public PSSite createSite(SiteDef def) {
        log.info("Creating site " + def.getName() + " ...");
        var site = new PSSite();
        site.setName(def.getName());
        site.setLabel("dummySiteLabel"); // don't know what this is used for, but it is required

        var hp = def.getHomePageTitle();
        if (hp == null || hp.trim().isEmpty())
            hp = "Home Page for Site " + site.getName();
        site.setHomePageTitle(hp);

        var nt = def.getNavTitle();
        if (nt == null || nt.trim().isEmpty())
            nt = "Home";
        site.setNavigationTitle(nt);

        site.setBaseTemplateName(def.getTemplateDef().getBaseTemplateName());

        var tName = def.getTemplateDef().getName();
        if (tName == null || tName.trim().isEmpty())
            tName = def.getName() + "Template";
        site.setTemplateName(tName);
        var result = getRestClient().save(site);
        log.info("Created site " + result.getId());
        return result;
    }

    /**
     * Helper method to create a section in the site. Also this method will
     * create a blog section, if it is required in the request.
     *
     * @param def  Used to create the section.
     * @param path The path of the parent section. Of the form
     *             /Sites/sitename/section1/...
     * @return The object that represents the section.
     */
    private PSSiteSection createSiteSection(SectionDef def, String parentPath) {
        var section = new PSCreateSiteSection();
        var sectionPath = "/" + parentPath;
        section.setFolderPath(sectionPath);
        final String NAME = "index.html";
        section.setPageUrlIdentifier(def.getName());
        var linkTitle = def.getLinkTitle();
        if (linkTitle == null || linkTitle.trim().isEmpty())
            linkTitle = def.getName();
        log.info("Creating section " + parentPath + "/" + linkTitle);
        section.setPageTitle(linkTitle);
        section.setPageLinkTitle(linkTitle);
        section.setCopyTemplates(def.isCopyTemplates());
        var tsum = templateGen.findTemplateByName(def.getTemplateName());
        if (tsum == null)
            throw new RuntimeException("templateName for SectionDef not found: " + def.getTemplateName());
        section.setTemplateId(tsum.getId());
        section.setPageName(NAME);
        section.setPageUrlIdentifier(linkTitle);
        // Check if the section type is not null
        if (def.getSectionType() != null) {
            var sectionType = PSSectionTypeEnum.valueOf(def.getSectionType());
            section.setSectionType(sectionType);

            // Process the blog type section
            if (sectionType == PSSectionTypeEnum.blog) {
                if (def.getBlogPostTemplate() != null) {
                    var tsum2 = templateGen.findTemplateByName(def.getBlogPostTemplate());
                    if (tsum2 == null)
                        throw new RuntimeException("templateBlogName for SectionDef not found: "
                                + def.getBlogPostTemplate());
                    section.setBlogPostTemplateId(tsum2.getId());
                } else {
                    throw new RuntimeException("templateBlogName for Blog SectionDef cannot be null");
                }
            }
        }
        var result = sectionClient.create(section);
        log.info("Created section " + result.getId());
        return result;
    }

    /**
     * Helper method to create an external link section. Given the information
     * supplied, it will create an object to send in the request in order to
     * create the section.
     *
     * @param def  Used to create the section.
     * @param path The path of the parent section. Of the form
     *             /Sites/sitename/section1/...
     * @return The object that represents the generated external link section.
     */
    private PSSiteSection createExternalLinkSection(SectionDef def, String parentPath) {
        var externalLinkSection = new PSCreateExternalLinkSection();
        var sectionPath = "/" + parentPath;
        externalLinkSection.setFolderPath(sectionPath);
        var linkTitle = def.getLinkTitle();
        if (linkTitle == null || linkTitle.trim().isEmpty())
            linkTitle = def.getName();
        log.info("Creating section " + parentPath + "/" + linkTitle);
        externalLinkSection.setLinkTitle(linkTitle);
        externalLinkSection.setExternalUrl(def.getExternalUrl());
        externalLinkSection.setSectionType(PSSectionTypeEnum.externallink);

        var result = sectionClient.createExternalLinkSection(externalLinkSection);
        log.info("Created external link section " + result.getId());
        return result;
    }

    /**
     * Helper method to create a section link. Given the information supplied,
     * it will determine which is the parent section folder, in order to get the
     * Id appropriately. If the target section is not found, an exception is
     * thrown.
     *
     * @param def  Used to create the section.
     * @param path The path of the parent section. Of the form
     *             /Sites/sitename/section1/...
     * @return The object that represents the generated section link.
     */
    private PSSiteSection createSectionLink(SectionDef def, String parentPath) {
        var targetSection = def.getTargetSection();
        var splittedParentPath = parentPath.split("/");
        var siteName = splittedParentPath[2];

        // Load the tree for the given site
        var sectionNode = sectionClient.loadTree(siteName);

        if (sectionNode != null) {
            var parentId = StringUtils.EMPTY;

            // Check if the target section is the home
            if (parentPath.endsWith(targetSection)) {
                parentId = sectionNode.getId();
            }

            if (StringUtils.isBlank(parentId)) {
                List<PSSectionNode> childNodes = sectionNode.getChildNodes();
                for (var childNode : childNodes) {
                    if (childNode.getFolderPath().endsWith(targetSection)) {
                        parentId = childNode.getId();
                        break;
                    }
                }
            }

            // Call the service to create the section link
            var result = sectionClient.createSectionLink(parentId, parentId);
            log.info("Created section link " + result.getId());
            return result;
        } else {
            throw new RuntimeException(
                    "Error when creating a section link, the target section was not found in the system.");
        }
    }
}
