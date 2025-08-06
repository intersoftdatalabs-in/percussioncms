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
package com.percussion.ui.service.impl;

import com.percussion.designmanagement.service.IPSFileSystemService;
import com.percussion.pathmanagement.data.PSPathItem;
import com.percussion.share.dao.PSDateUtils;
import com.percussion.ui.service.IPSListViewHelper;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * A file-system based implementation of {@link IPSListViewHelper}.
 *
 * @author miltonpividori
 */
@Component("fileSystemListViewHelper")
@Lazy
public class PSFileSystemListViewHelper extends PSBaseListViewHelper {
    private IPSFileSystemService fileSystemService;

    public PSFileSystemListViewHelper(IPSFileSystemService fileSystemService) {
        this.fileSystemService = fileSystemService;
    }

    @Override
    protected Map<String, String> getDisplayProperties(PSPathItem pathItem) {
        var displayProperties = new HashMap<String, String>();
        var relatedFile = (File) pathItem.getRelatedObject();

        displayProperties.put(TITLE_NAME, fileSystemService.getNameFromFile(relatedFile));

        if (!relatedFile.isDirectory()) {
            displayProperties.put(SIZE, String.valueOf(relatedFile.length()));
        }

        displayProperties.put(CONTENT_LAST_MODIFIED_DATE_NAME,
                PSDateUtils.getDateToString(new Date(relatedFile.lastModified())));

        return displayProperties;
    }

    @Override
    protected Class<?> expectedRelatedObjectType() {
        return File.class;
    }

    @Override
    protected boolean areEmptyRelatedObjectsSupported() {
        return false;
    }
}
