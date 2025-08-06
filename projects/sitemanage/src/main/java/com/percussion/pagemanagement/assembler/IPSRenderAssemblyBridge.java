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
package com.percussion.pagemanagement.assembler;

import com.percussion.pagemanagement.assembler.PSAbstractAssemblyContext.EditType;
import com.percussion.pagemanagement.data.PSPage;
import com.percussion.pagemanagement.data.PSTemplate;
import com.percussion.pagemanagement.service.IPSPageService;
import com.percussion.services.assembly.IPSAssemblyItem;
import com.percussion.share.service.exception.PSValidationException;
import com.percussion.utils.guid.IPSGuid;

/**
 * Low-level API that works directly with the Rhythmyx Assembly engine.
 * All render methods assume editMode unless they expose the flag in their signature.
 *
 * <p>Implementations must be thread-safe and stateless.
 *
 * @author adamgent
 */
public interface IPSRenderAssemblyBridge {

    /**
     * Renders a page object.
     *
     * @param page never {@code null}
     * @param editMode edit mode flag
     * @param scriptsOff disables scripts if true
     * @return rendered output, never {@code null}
     * @throws IPSPageService.PSPageException if rendering fails
     */
    String renderPage(PSPage page, boolean editMode, boolean scriptsOff) throws IPSPageService.PSPageException;

    /**
     * Renders a template object. Sets editMode to {@code true}.
     *
     * @param template never {@code null}
     * @param scriptsOff disables scripts if true
     * @return rendered output, never {@code null}
     * @throws IPSPageService.PSPageException if rendering fails
     */
    String renderTemplate(PSTemplate template, boolean scriptsOff) throws IPSPageService.PSPageException;

    /**
     * Renders a template with a page. Sets editMode to {@code true}.
     *
     * @param template never {@code null}
     * @param page never {@code null}
     * @param scriptsOff disables scripts if true
     * @return rendered output, never {@code null}
     * @throws IPSPageService.PSPageException if rendering fails
     */
    String renderTemplateWithPage(PSTemplate template, PSPage page, boolean scriptsOff) throws IPSPageService.PSPageException;

    /**
     * Assembles a page by ID.
     *
     * @param id not blank (string format of an {@link IPSGuid})
     * @param editMode edit mode flag
     * @param scriptsOff disables scripts if true
     * @return rendered output, never {@code null}
     * @throws IPSPageService.PSPageException if rendering fails
     * @throws PSValidationException if validation fails
     */
    String renderPage(String id, boolean editMode, boolean scriptsOff) throws IPSPageService.PSPageException, PSValidationException;

    /**
     * Assembles a page by ID, specifying the edited item type.
     *
     * @param id not blank
     * @param editMode edit mode flag
     * @param scriptsOff disables scripts if true
     * @param type edited item type
     * @return rendered output, never {@code null}
     * @throws IPSPageService.PSPageException if rendering fails
     * @throws PSValidationException if validation fails
     */
    String renderPage(String id, boolean editMode, boolean scriptsOff, EditType type) throws IPSPageService.PSPageException, PSValidationException;

    /**
     * Renders a template by ID. Sets editMode to {@code true}.
     *
     * @param id never {@code null}, empty, or blank
     * @param scriptsOff disables scripts if true
     * @return rendered output, never {@code null}
     * @throws IPSPageService.PSPageException if rendering fails
     * @throws PSValidationException if validation fails
     */
    String renderTemplate(String id, boolean scriptsOff) throws IPSPageService.PSPageException, PSValidationException;

    /**
     * Gets the name of the legacy assembly template used to assemble the page/template.
     * This is for the legacy assembler and is usually configured through Spring.
     *
     * @return never {@code null}, empty, or blank
     */
    String getDispatchTemplate();

    /**
     * Gets the ID of the dispatch template.
     *
     * @return the ID, never {@code null}
     * @throws IPSPageService.PSPageException if lookup fails
     */
    IPSGuid getDispatchTemplateId() throws IPSPageService.PSPageException;

    /**
     * Gets the {@link IPSAssemblyItem} used for previewing the specified page.
     *
     * @param id the page ID, must not be blank
     * @param editMode edit mode flag
     * @param scriptsOff disables scripts if true
     * @param editType edited item type
     * @return the assembly item, never {@code null}
     * @throws IPSPageService.PSPageException if lookup fails
     */
    IPSAssemblyItem getWorkItemForPreview(String id, boolean editMode, boolean scriptsOff, EditType editType) throws IPSPageService.PSPageException;

    /**
     * Exception for render assembly bridge errors.
     */
    class PSRenderAssemblyBridgeException extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public PSRenderAssemblyBridgeException(String message) {
            super(message);
        }

        public PSRenderAssemblyBridgeException(String message, Throwable cause) {
            super(message, cause);
        }

        public PSRenderAssemblyBridgeException(Throwable cause) {
            super(cause);
        }
    }
}
