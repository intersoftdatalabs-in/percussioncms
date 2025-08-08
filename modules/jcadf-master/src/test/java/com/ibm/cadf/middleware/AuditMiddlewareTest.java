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

package com.ibm.cadf.middleware;

import java.io.File;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ibm.cadf.exception.CADFException;
import com.ibm.cadf.model.Event;
import com.ibm.cadf.util.Constants;

public class AuditMiddlewareTest
{

    @BeforeEach
    public void setUp()
    {
        System.setProperty(Constants.API_AUDIT_MAP, "api_audit_map.conf");
    }

    @Test
    public void audit()
    {
        try
        {
            AuditMiddleware middleware = new AuditMiddleware(Constants.AUDIT_FORMAT_TYPE_JSON);
            AuditContext ctx = new AuditContext();
            ctx.setIniatorName("root");
            ctx.setTargetName("swift");
            ctx.setTargetUrl("http://hostname:8080");
            ctx.setTargetUsername("test:tester");
            ctx.setObserverName("gpfs");
            ctx.setInitiatorIP("192.0.0.1");
            Event event = middleware.createEvent(Constants.MIGRATE_ACTION, "SUCCESS", ctx);

            // Assert for the data
            Assertions.assertEquals("root", event.getInitiator().getName());
            Assertions.assertEquals("swift", event.getTarget().getName());
            Assertions.assertEquals("http://hostname:8080", event.getTarget().getAddresses().get(0).getUrl());
            Assertions.assertEquals("gpfs", event.getObserver().getName());
            Assertions.assertEquals("192.0.0.1", event.getInitiator().getHost().getAddress());

            middleware.audit(event);
        }
        catch (CADFException e)
        {
            Assertions.fail();
        }
    }

    @AfterAll
    public static void clean()
    {
        File auditFile = new File(Constants.JSON_AUDIT_FILES_NAME);
        //auditFile.delete();
    }

}
