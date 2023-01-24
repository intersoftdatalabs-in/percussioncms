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

package com.percussion.pagemanagement.extension;

import com.percussion.cms.IPSConstants;
import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.IPSUdfProcessor;
import com.percussion.extension.PSExtensionException;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.pagemanagement.service.IPSRenderService;
import com.percussion.search.lucene.textconverter.PSTextConverterHtml;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSConsole;
import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.IPSGuidManager;
import com.percussion.share.service.IPSIdMapper;
import com.percussion.share.spring.PSSpringWebApplicationContextUtils;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.tools.IPSUtilsConstants;
import com.percussion.webservices.PSWebserviceUtils;
import com.percussion.webservices.content.IPSContentDesignWs;
import com.percussion.webservices.publishing.IPSPublishingWs;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * This exit assembles a page in the preview context and then extracts the html content.
 * @author peterfrontiero
 *
 */
public class PSExtractHtmlContent implements IPSUdfProcessor
{
    private IPSRenderService renderService;
    private IPSGuidManager guidMgr;
    private IPSContentDesignWs contentDesignWs;
    private IPSIdMapper idMapper;
    private IPSPublishingWs publishingWs;
    
    @Override
    public void init(IPSExtensionDef extDef, File file) throws PSExtensionException
    {
        PSSpringWebApplicationContextUtils.injectDependencies(this);
    }

    @Override
    public Object processUdf(Object[] params, IPSRequestContext request)
    {
        // Only run if a load by a search index update is in progress
        Object objIsLoadForSearch = request.getPrivateObject(IPSConstants.LOAD_FOR_SEARCH_INDEX);
        boolean isLoadForSearch = (objIsLoadForSearch instanceof Boolean)
                && ((Boolean) objIsLoadForSearch).booleanValue();
        if (!isLoadForSearch) {
            return "";
        }

    	// We need at least the content id
        if (null == params || params.length == 0 || null == params[0] || StringUtils.isEmpty(params[0].toString()))
        {
            return "";
        }
        String cTypeIdStr = params[0].toString();
        IPSGuid guid = guidMgr.makeGuid(cTypeIdStr, PSTypeEnum.LEGACY_CONTENT);

        if (publishingWs.getItemSites(guid).isEmpty())
        {
            // page is not associated with a site, cannot be assembled
            return "";
        }

        PSWebserviceUtils.setUserName(request.getOriginalSubject().getName());

        // assemble the page
        String renderedPage = renderService.renderPage(idMapper.getString(guid));
        if (renderedPage.contains("<html"))
        {
            // remove everything before the start of the html tag to allow for proper extraction
            renderedPage = renderedPage.substring(renderedPage.indexOf("<html"));
        }

        // extract the html content

        try(InputStream bis = new ByteArrayInputStream(renderedPage.getBytes(IPSUtilsConstants.RX_JAVA_ENC)) ){
            PSTextConverterHtml converter = new PSTextConverterHtml();
            return converter.getConvertedText(bis,"");

        } catch (IOException | PSExtensionProcessingException  e) {
            PSConsole.printMsg(this.getClass().getName(), e.getLocalizedMessage());
        }

        return "";
    }  
    
    public IPSRenderService getRenderService()
    {
        return renderService;
    }

    public void setRenderService(IPSRenderService renderService)
    {
        this.renderService = renderService;
    }
    
    public IPSGuidManager getGuidMgr()
    {
        return guidMgr;
    }

    public void setGuidMgr(IPSGuidManager guidMgr)
    {
        this.guidMgr = guidMgr;
    }

    public IPSContentDesignWs getContentDesignWs()
    {
        return contentDesignWs;
    }

    public void setContentDesignWs(IPSContentDesignWs contentDesignWs)
    {
        this.contentDesignWs = contentDesignWs;
    }
    
    public IPSIdMapper getIdMapper()
    {
        return idMapper;
    }

    public void setIdMapper(IPSIdMapper idMapper)
    {
        this.idMapper = idMapper;
    }
    
    public IPSPublishingWs getPublishingWs()
    {
        return publishingWs;
    }

    public void setPublishingWs(IPSPublishingWs publishingWs)
    {
        this.publishingWs = publishingWs;
    }

    /**
     * Logger for this exit.
     */
    public static final Logger log = LogManager.getLogger(PSExtractHtmlContent.class);
}
