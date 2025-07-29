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
package com.percussion.security.dao.impl;

import com.percussion.cms.IPSConstants;
import com.percussion.designmanagement.service.IPSFileSystemService;
import com.percussion.error.PSExceptionUtils;
import com.percussion.security.dao.IPSSecurityItemsDao;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * DAO implementation for security items such as SSH private keys.
 */
public class PSSecurityItemsDao implements IPSSecurityItemsDao {

    private static final Logger logger = LogManager.getLogger(IPSConstants.PUBLISHING_LOG);

    /**
     * File system service pointing to the root folder where SSH private keys are stored.
     */
    private final IPSFileSystemService fileSystemService;

    public PSSecurityItemsDao(IPSFileSystemService privateKeysFileSystemService) {
        this.fileSystemService = privateKeysFileSystemService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getAvailablePrivateKeys() {
        try {
            var privateKeys = fileSystemService.getChildren("/");
            // Exclude the SSH config file, return only key file names
            return privateKeys.stream()
                    .map(File::getName)
                    .filter(name -> !name.equalsIgnoreCase("config"))
                    .collect(Collectors.toList());
        } catch (FileNotFoundException e) {
            logger.warn("rxconfig/ssh-keys folder is missing. Error: {}",
                    PSExceptionUtils.getMessageForLog(e));
            return List.of();
        }
    }
}
