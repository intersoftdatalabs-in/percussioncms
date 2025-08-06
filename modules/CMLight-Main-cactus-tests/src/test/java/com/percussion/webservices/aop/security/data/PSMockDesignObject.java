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
package com.percussion.webservices.aop.security.data;

import com.percussion.services.security.IPSAcl;
import com.percussion.services.security.data.PSAclImpl;
import com.percussion.utils.guid.IPSGuid;
import java.util.Objects;

/**
 * Mock data object for testing.
 */
public class PSMockDesignObject {

    private IPSGuid guid;

    /**
     * Gets the guid.
     *
     * @return The guid, may be {@code null}.
     */
    public IPSGuid getGuid() {
        return guid;
    }

    /**
     * Sets the guid.
     *
     * @param guid The guid, may not be {@code null}.
     */
    public void setGUID(IPSGuid guid) {
        this.guid = Objects.requireNonNull(guid, "guid may not be null");
    }

    /**
     * Creates a mock object with the guid specified by the supplied acl.
     *
     * @param acl The acl to use, may not be {@code null}.
     * @return The object, never {@code null}.
     */
    public static PSMockDesignObject createMockObject(IPSAcl acl) {
        Objects.requireNonNull(acl, "acl may not be null");
        var obj = new PSMockDesignObject();
        var guid = ((PSAclImpl) acl).getObjectGuid();
        obj.setGUID(guid);
        return obj;
    }
}
