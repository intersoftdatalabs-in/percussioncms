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
package com.percussion.share.extension;

import com.percussion.data.PSConversionException;
import com.percussion.extension.IPSItemInputTransformer;
import com.percussion.extension.PSDefaultExtension;
import com.percussion.extension.PSExtensionParams;
import com.percussion.extension.PSExtensionProcessingException;
import com.percussion.extension.PSParameterMismatchException;
import com.percussion.security.PSAuthorizationException;
import com.percussion.server.IPSRequestContext;
import com.percussion.server.PSRequestValidationException;
import com.percussion.utils.PSJsoupPreserver;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

/**
 * Removes reserved html class names from content in the specified html parameter value.
 * Also adds a placeholder text for empty iframe elements.
 *
 * @author JaySeletz
 */
public class PSCleanReservedHtmlClassesInputTransformer extends PSDefaultExtension implements IPSItemInputTransformer {
    private static final String[] PERC_CLASSES = {
        "perc-widget", "perc-region", "perc-vertical", "perc-fixed", "perc-region-leaf",
        "perc-horizontal", "perc-itool-selectable-elem", "perc-itool-region-elem", "perc-zero-size-elem"
    };

    @Override
    public void preProcessRequest(Object[] params, IPSRequestContext request)
            throws PSAuthorizationException, PSRequestValidationException, PSParameterMismatchException, PSExtensionProcessingException {
        try {
            var ep = new PSExtensionParams(params);
            var fieldName = ep.getStringParam(0, null, true);
            if (StringUtils.isBlank(fieldName))
                throw new PSParameterMismatchException("No fieldName supplied");

            var value = request.getParameter(fieldName);
            if (StringUtils.isBlank(value))
                return;
            var newValue = processContent(value);
            request.setParameter(fieldName, newValue);
        } catch (PSConversionException e) {
            throw new PSParameterMismatchException(e.getLocalizedMessage());
        }
    }

    /**
     * Parses the supplied content as HTML, removes reserved class names, and adds placeholder text for empty iframes.
     *
     * @param value The value to clean, not null or empty.
     * @return The cleaned value.
     */
    String processContent(String value) {
        var doc = Jsoup.parseBodyFragment(PSJsoupPreserver.formatPreserveTagsForJSoupParse(value));
        var didChange = false;

        for (var elem : doc.getAllElements()) {
            for (var className : PERC_CLASSES) {
                if (elem.hasClass(className)) {
                    elem.removeClass(className);
                    didChange = true;
                }
            }
            if (elem.tagName().equalsIgnoreCase("iframe")
                    && (elem.childNodes().isEmpty() || StringUtils.isBlank(elem.text()))) {
                elem.text(EMPTY_IFRAME_TEXT);
                didChange = true;
            }
        }

        if (!didChange)
            return value;

        return PSJsoupPreserver.formatPreserveTagsForOutput(doc.body().html());
    }

    public static final String EMPTY_IFRAME_TEXT = "Alternate iframe text";
}
