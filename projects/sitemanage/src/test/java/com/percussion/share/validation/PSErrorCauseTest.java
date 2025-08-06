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
package com.percussion.share.validation;

import com.percussion.share.dao.PSSerializerUtils;
import com.percussion.share.validation.PSErrors.PSObjectError;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

/**
 * Unit test for {@link PSErrorCause}.
 */
public class PSErrorCauseTest {

    @Test
    void testCreateErrorCause() throws Throwable {
        Exception runtimeException;
        try {
            throw new RuntimeException("Fail");
        } catch (Exception e) {
            runtimeException = e;
        }

        var errors = new PSErrors();
        var objectError = new PSObjectError();
        objectError.setCause(new PSErrorCause(runtimeException));
        var args = new ArrayList<String>();
        args.add("arg1");
        args.add("arg2");
        args.add("arg3");
        objectError.setArguments(args);
        errors.setGlobalError(objectError);

        var xml = PSSerializerUtils.marshal(errors);
        log.debug(xml);
        System.out.println(xml);

        var unmarshalledErrors = PSSerializerUtils.unmarshal(xml, PSErrors.class);
        log.debug(unmarshalledErrors.getGlobalError().getCause());
    }

    /**
     * The log instance to use for this class, never null.
     */
    private static final Logger log = LogManager.getLogger(PSErrorCauseTest.class);

}
