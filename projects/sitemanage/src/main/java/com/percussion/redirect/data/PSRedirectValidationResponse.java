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

package com.percussion.redirect.data;

import com.fasterxml.jackson.annotation.JsonRootName;
import com.percussion.licensemanagement.data.PSModuleLicense;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Response object for redirect validation.
 */
@XmlRootElement(name = "response")
@JsonRootName("response")
public class PSRedirectValidationResponse {

    private String errorMessage;
    private PSModuleLicense redirectLicense;
    private String bucketName;
    private RedirectValidationStatus status;

    /** @return the error message, if any */
    public String getErrorMessage() {
        return errorMessage;
    }

    /** Sets the error message. */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /** @return the redirect license */
    public PSModuleLicense getRedirectLicense() {
        return redirectLicense;
    }

    /** Sets the redirect license. */
    public void setRedirectLicense(PSModuleLicense redirectLicense) {
        this.redirectLicense = redirectLicense;
    }

    /** @return the S3 bucket name */
    public String getBucketName() {
        return bucketName;
    }

    /** Sets the S3 bucket name. */
    public void setBucketName(String bucketName) {
        this.bucketName = bucketName;
    }

    /** @return the redirect validation status */
    public RedirectValidationStatus getStatus() {
        return status;
    }

    /** Sets the redirect validation status. */
    public void setStatus(RedirectValidationStatus status) {
        this.status = status;
    }

    /**
     * Enum for redirect validation status.
     */
    public enum RedirectValidationStatus {
        PUBLISHED, NO_LICENSE, NOT_PUBLISHED, NO_CHILDREN, ERROR, NOT_APPLICABLE
    }
}
