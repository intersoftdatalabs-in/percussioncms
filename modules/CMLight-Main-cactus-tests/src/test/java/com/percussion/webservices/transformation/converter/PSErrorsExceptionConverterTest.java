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
package com.percussion.webservices.transformation.converter;

import com.percussion.services.catalog.PSTypeEnum;
import com.percussion.services.guidmgr.PSGuidManagerLocator;
import com.percussion.utils.guid.IPSGuid;
import com.percussion.utils.testing.IntegrationTest;
import com.percussion.webservices.IPSWebserviceErrors;
import com.percussion.webservices.PSErrorException;
import com.percussion.webservices.PSErrorsException;
import com.percussion.webservices.PSLockErrorException;
import com.percussion.webservices.faults.PSErrorsFault;
import org.junit.jupiter.api.Tag;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for the {@link PSErrorsExceptionConverter} class.
 */
@Tag(IntegrationTest.class)
public class PSErrorsExceptionConverterTest extends PSConverterTestBase {

    public void testConversion() throws Exception {
        var manager = PSGuidManagerLocator.getGuidMgr();

        var guid1 = manager.createGuid(PSTypeEnum.SLOT);
        var guid2 = manager.createGuid(PSTypeEnum.SLOT);
        var guid3 = manager.createGuid(PSTypeEnum.SLOT);

        var error = new PSErrorException(
                IPSWebserviceErrors.OBJECT_NOT_FOUND, "message", "stack");
        var lockError = new PSLockErrorException(
                IPSWebserviceErrors.OBJECT_NOT_FOUND, "message", "stack");
        lockError.setLocker("locker");
        lockError.setRemainingTime(1000);

        var source = new PSErrorsException();
        source.addResult(guid1);
        source.addError(guid2, error);
        source.addError(guid3, lockError);

        var target = (PSErrorsException) roundTripConversion(
                PSErrorsException.class,
                PSErrorsFault.class,
                source);

        assertEquals(source, target);
    }
}
