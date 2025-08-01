// REFACTORED: CP-JAVA11
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
package com.percussion.share.dao.impl;

import org.springframework.beans.factory.FactoryBean;

import com.percussion.extension.IPSExtensionManager;
import com.percussion.server.PSServer;

/**
 * Factory for obtaining the extension manager.
 */
public class PSExtensionManagerFactory implements FactoryBean<IPSExtensionManager> {

    @Override
    public IPSExtensionManager getObject() throws Exception {
        return PSServer.getExtensionManager(null);
    }

    @Override
    public Class<IPSExtensionManager> getObjectType() {
        return IPSExtensionManager.class;
    }

    @Override
    public boolean isSingleton() {
        return true;
    }
}
