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
// REFACTORED: CP-JAVA11
package com.percussion.webservices.aop.security.strategy;

import com.percussion.services.security.PSPermissions;

/**
 * A test security strategy roughly equivalent to
 * {@link PSFindSecurityStrategy}, but enforces the delete permission.
 */
public class PSTestSecurityStrategy extends PSCustomSecurityStrategy {

    @Override
    protected boolean acceptName(String name) {
        // Any method this is on is fine
        return true;
    }

    @Override
    protected int getFilterArg() {
        return -1;
    }

    @Override
    protected PSPermissions getRequiredPermission() {
        return PSPermissions.DELETE;
    }

    @Override
    protected boolean shouldPostProcess() {
        return true;
    }

    @Override
    protected boolean shouldReturnResults() {
        return true;
    }
}
