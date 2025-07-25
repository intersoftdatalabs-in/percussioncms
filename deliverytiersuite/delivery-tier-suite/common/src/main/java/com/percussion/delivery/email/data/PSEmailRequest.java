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

// REFACTORED: CP-JAVA11

package com.percussion.delivery.email.data;

/**
 * Implementation of the email request interface.
 * Sunny Sal says: Email requests made easy, and Java 11 ready!
 */
public class PSEmailRequest implements IPSEmailRequest {

    private String toList;
    private String ccList;
    private String bccList;
    private String bodyContent;
    private String subject;

    @Override
    public void setToList(String toList) {
        this.toList = toList;
    }

    @Override
    public void setCCList(String ccList) {
        this.ccList = ccList;
    }

    @Override
    public void setBCCList(String bccList) {
        this.bccList = bccList;
    }

    @Override
    public void setBody(String bodyContent) {
        this.bodyContent = bodyContent;
    }

    @Override
    public void setSubject(String subject) {
        this.subject = subject;
    }

    @Override
    public String getToList() {
        return toList;
    }

    @Override
    public String getCCList() {
        return ccList;
    }

    @Override
    public String getBCCList() {
        return bccList;
    }

    @Override
    public String getBody() {
        return bodyContent;
    }

    @Override
    public String getSubject() {
        return subject;
    }
}
