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
package com.percussion.pagemanagement.service;

import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSRegion;
import com.percussion.pagemanagement.data.PSRenderResult;
import com.percussion.pagemanagement.data.PSTemplate;

/**
 * 
 * Renders templates, pages and regions.
 * 
 * @author adamgent
 *
 */
public interface IPSRenderService
{
    /**
     * Assembles a single region that appears on the supplied page and returns
     * the serialized version. Sets the edit mode flag. See
     * {@link #renderPageForEdit(String)} for details of this flag.
     * 
     * @param page The page to assemble. Not <code>null</code>.
     * @param regionId The name of the region within the supplied page. Not
     * blank.
     * 
     * @return Just the rendered region as a string within the returned object.
     * The supplied <code>regionId</code> is set on the returned object.
     * 
     * @throws PSRenderServiceException
     */
    public PSRenderResult renderRegion(PSPage page, String regionId) throws PSRenderServiceException;

    public String renderRegionAll( PSTemplate template ) throws PSRenderServiceException;
    
    public PSRenderResult renderRegion(PSTemplate template, String regionId) throws PSRenderServiceException;
    
    public String renderTemplate(String id) throws PSRenderServiceException;
    
    /**
     * Similar to the {@link #renderTemplate(String)}, except sets a scriptsOff variable as true to the context.
     * So that the macros can render the template by stripping the script tags.
     * @param id, the string format of template item guid.
     * @return The rendered template, typically an (x)html document. Never
     * <code>null</code> or empty.
     * @throws PSRenderServiceException
     */
    public String renderTemplateScriptsOff(String id) throws PSRenderServiceException;
    
    public String renderPage(String id) throws PSRenderServiceException;

    /**
     * Similar to {@link #renderPage(String)}, except sets a flag that can be
     * used by widgets if they want or need to render their output differently
     * when a page is being edited. For example, if a widget has no content, it
     * may render some sample content or it may allow in-line editing of its
     * content. Each widget is responsible for what is rendered in this
     * situation.
     * 
     * @param id The unique identifier of the page to be rendered. Not blank.
     * (TODO: ph - as a user of the API, where do I get one of these ids?)
     * 
     * @return The rendered page, typically an (x)html document. Never
     * <code>null</code> or empty.
     * @throws PSRenderServiceException 
     */
    public String renderPageForEdit(String id, String editType) throws PSRenderServiceException;
    
    /**
     * Similar to {@link #renderPageForEdit(String)}, this method sets scripts off flag to the $perc context. So that
     * the velocity macros can strip the scripts if the flag is set to false.
     * @param id, the string format of the guid of the page.
     * @return The rendered page, typically an (x)html document. Never
     * <code>null</code> or empty.
     * @throws PSRenderServiceException
     */
    public String renderPageForEditScriptsOff(String id, String editType) throws PSRenderServiceException;

    public PSRegion parse(String html) throws PSRenderServiceException;
    
    public static class PSRenderServiceException extends RuntimeException {
        
        private static final long serialVersionUID = 1L;
        
        public PSRenderServiceException(String message) {
            super(message);
        }
        
        public PSRenderServiceException(String message, Throwable cause) {
            super(message, cause);
        }
        public PSRenderServiceException(Throwable cause) {
            super(cause);
        }
        
    }

}
