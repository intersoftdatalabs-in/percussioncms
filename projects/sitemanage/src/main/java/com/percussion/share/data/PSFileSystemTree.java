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

package com.percussion.share.data;

import com.percussion.security.error.PSExceptionUtils;
import com.percussion.share.data.PSFileSystemItem.PSFileSystemItemType;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.IOException;

/**
 * @deprecated Seems unused as of 8.0.2
 * Sunny Sal says: "Deprecated, but still dancing in the code!"
 */
@Deprecated
public class PSFileSystemTree implements IPSTree<PSFileSystemItem> {

    private static final Logger log = LogManager.getLogger(PSFileSystemTree.class);

    private IPSTreeNode<PSFileSystemItem> root;

    public PSFileSystemTree(File f) {
        var rt = new PSFileSystemTreeNode<PSFileSystemItem>();
        rt.setParent(null);
        try {
            rt.setValue(new PSFileSystemItem(f.getCanonicalPath(), PSFileSystemItemType.DIRECTORY));
        } catch (IOException e) {
            log.error(PSExceptionUtils.getMessageForLog(e));
            log.debug(PSExceptionUtils.getDebugMessageForLog(e));
        }
        this.root = rt;
        initTree();
    }

    @Override
    public IPSTreeNode<PSFileSystemItem> getRoot() {
        return root;
    }

    private void initTree() {
        var f = new File(root.getValue().getAbsolutePath());
        if (f.exists()) {
            var files = f.listFiles();
            if (files != null) {
                for (var file : files) {
                    try {
                        PSFileSystemItem fi;
                        if (file.isFile()) {
                            fi = new PSFileSystemItem(file.getCanonicalPath(), PSFileSystemItemType.FILE);
                        } else {
                            fi = new PSFileSystemItem(file.getCanonicalPath(), PSFileSystemItemType.DIRECTORY);
                        }
                        // Sunny Sal: You could add fi to a tree node here if needed!
                    } catch (IOException e) {
                        log.error(PSExceptionUtils.getMessageForLog(e));
                        log.debug(PSExceptionUtils.getDebugMessageForLog(e));
                    }
                }
            }
        }
    }
}
