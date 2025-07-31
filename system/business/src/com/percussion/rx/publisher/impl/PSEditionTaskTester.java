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
package com.percussion.rx.publisher.impl;

import com.percussion.extension.IPSExtensionDef;
import com.percussion.extension.PSExtensionException;
import com.percussion.rx.publisher.IPSEditionTask;
import com.percussion.rx.publisher.IPSEditionTaskStatusCallback;
import com.percussion.services.publisher.IPSEdition;
import com.percussion.services.sitemgr.IPSSite;

import java.io.File;
import java.util.Date;
import java.util.Map;

/**
 * Test the edition task system by printing out information passed in. To use,
 * register the extension and create an edition (or modify an existing edition)
 * and use this both before and after the edition is run.
 *
 * @author dougrand
 */
public class PSEditionTaskTester implements IPSEditionTask {

    @Override
    public TaskType getType() {
        return TaskType.PREANDPOSTEDITION;
    }

    @Override
    public void perform(IPSEdition edition, IPSSite site, Date startTime,
                       Date endTime, long jobId, long duration, boolean success,
                       Map<String, String> params, IPSEditionTaskStatusCallback status) {
        if (edition == null) {
            throw new IllegalArgumentException("edition may not be null");
        }
        if (site == null) {
            throw new IllegalArgumentException("site may not be null");
        }
        if (startTime == null) {
            throw new IllegalArgumentException("startTime may not be null");
        }
        if (endTime == null) {
            if (duration > -1) {
                throw new IllegalStateException("Pre task should have no duration");
            }
            if (status != null) {
                throw new IllegalStateException("Pre task should have no status callback");
            }
        } else {
            if (duration < 0) {
                throw new IllegalStateException("Post task should have a duration");
            }
            if (status == null) {
                throw new IllegalStateException("Post task should have a status callback");
            }
         // NOTE: getJobStatus() is deprecated but required for legacy compatibility
         var entries = status.getJobStatus();
            if (entries == null) {
                throw new IllegalStateException("Post task should have status information");
            }
            if (entries.isEmpty()) {
                throw new IllegalStateException("Post task should have log entries");
            }
        }
    }

    @Override
    public void init(IPSExtensionDef def, File codeRoot) throws PSExtensionException {
        // No initialization required
    }
}