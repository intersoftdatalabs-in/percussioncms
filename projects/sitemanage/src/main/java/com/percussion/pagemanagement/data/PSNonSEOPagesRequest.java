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
package com.percussion.pagemanagement.data;

import com.percussion.pagemanagement.data.PSSEOStatistics.SEO_SEVERITY;
import com.percussion.pathmanagement.data.PSItemByWfStateRequest;

import javax.xml.bind.annotation.XmlRootElement;
import net.sf.oval.constraint.NotNull;

/**
 * Request to find non-SEO pages by path, workflow, and workflow state.
 * Optionally filter by keyword and severity.
 * @author peterfrontiero
 */
@XmlRootElement(name = "NonSEOPagesRequest")
public class PSNonSEOPagesRequest extends PSItemByWfStateRequest {

    /**
     * The severity for which all pages will be requested, never {@code null}.
     */
    @NotNull
    private SEO_SEVERITY severity;

    /**
     * The keyword to search for as part of the request.
     * May be {@code null} or empty.
     */
    private String keyword;

    /**
     * Gets the severity for which all pages will be requested.
     * @return the severity, never {@code null}
     */
    public SEO_SEVERITY getSeverity() {
        return severity;
    }

    /**
     * Sets the severity for which all pages will be requested.
     * @param severity the severity
     */
    public void setSeverity(SEO_SEVERITY severity) {
        this.severity = severity;
    }

    /**
     * Gets the keyword to search for as part of the request.
     * May be {@code null} or empty.
     * @return the keyword
     */
    public String getKeyword() {
        return keyword;
    }

    /**
     * Sets the keyword to search for as part of the request.
     * @param keyword the keyword
     */
    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
}
