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
package com.percussion.sitemanage.task.impl;

import com.percussion.services.publisher.IPSPubItemStatus;
import com.percussion.services.publisher.IPSSiteItem.Operation;
import com.percussion.services.publisher.IPSSiteItem.Status;

import java.util.Date;

/**
 * Mock implementation of IPSPubItemStatus for testing.
 * // REFACTORED: CP-JAVA11
 */
public class MockPubItemStatus implements IPSPubItemStatus {

    @Override
    public String getAssemblyUrl() {
        throw new UnsupportedOperationException("getAssemblyUrl is not supported in MockPubItemStatus");
    }

    @Override
    public int getContentId() {
        throw new UnsupportedOperationException("getContentId is not supported in MockPubItemStatus");
    }

    @Override
    public Date getDate() {
        throw new UnsupportedOperationException("getDate is not supported in MockPubItemStatus");
    }

    @Override
    public String getDeliveryType() {
        throw new UnsupportedOperationException("getDeliveryType is not supported in MockPubItemStatus");
    }

    @Override
    public Integer getElapsed() {
        throw new UnsupportedOperationException("getElapsed is not supported in MockPubItemStatus");
    }

    @Override
    public Integer getFolderId() {
        throw new UnsupportedOperationException("getFolderId is not supported in MockPubItemStatus");
    }

    @Override
    public String getLocation() {
        throw new UnsupportedOperationException("getLocation is not supported in MockPubItemStatus");
    }

    @Override
    public String getMessage() {
        throw new UnsupportedOperationException("getMessage is not supported in MockPubItemStatus");
    }

    @Override
    public Operation getOperation() {
        throw new UnsupportedOperationException("getOperation is not supported in MockPubItemStatus");
    }

    @Override
    public long getReferenceId() {
        throw new UnsupportedOperationException("getReferenceId is not supported in MockPubItemStatus");
    }

    @Override
    public int getRevisionId() {
        throw new UnsupportedOperationException("getRevisionId is not supported in MockPubItemStatus");
    }

    @Override
    public Status getStatus() {
        throw new UnsupportedOperationException("getStatus is not supported in MockPubItemStatus");
    }

    @Override
    public long getStatusId() {
        throw new UnsupportedOperationException("getStatusId is not supported in MockPubItemStatus");
    }

    @Override
    public Long getTemplateId() {
        throw new UnsupportedOperationException("getTemplateId is not supported in MockPubItemStatus");
    }

    @Override
    public Long getUnpublishRefId() {
        throw new UnsupportedOperationException("getUnpublishRefId is not supported in MockPubItemStatus");
    }

    @Override
    public boolean isHidden() {
        throw new UnsupportedOperationException("isHidden is not supported in MockPubItemStatus");
    }
}
