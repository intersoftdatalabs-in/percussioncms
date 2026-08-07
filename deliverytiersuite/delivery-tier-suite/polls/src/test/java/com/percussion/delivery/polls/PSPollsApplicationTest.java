/*
 * Copyright (c) 2026 Intersoft Data Labs, Inc.
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
package com.percussion.delivery.polls;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.percussion.delivery.exceptions.PSJsonMappingErrorResponse;
import com.percussion.delivery.exceptions.PSUncaughtError;
import com.percussion.delivery.polls.services.PSPollsRestService;
import java.lang.reflect.Modifier;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.filter.RolesAllowedDynamicFeature;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import tools.jackson.jakarta.rs.json.JacksonXmlBindJsonProvider;

/**
 * Behavioral coverage for {@link PSPollsApplication}: constructor registration without {@code
 * this-escape} suppressions (class is {@code final}; issue #2042).
 */
public class PSPollsApplicationTest {

  @Test
  @DisplayName("class is final so ResourceConfig.register cannot be overridden by a subclass")
  void classIsFinal() {
    assertTrue(Modifier.isFinal(PSPollsApplication.class.getModifiers()));
  }

  @Test
  @DisplayName("constructor registers polls resource, logging, RBAC, and exception mappers")
  void constructorRegistersExpectedComponents() {
    PSPollsApplication application = new PSPollsApplication();

    assertTrue(application.isRegistered(PSPollsRestService.class));
    assertTrue(application.isRegistered(LoggingFeature.class));
    assertTrue(application.isRegistered(RolesAllowedDynamicFeature.class));
    assertTrue(application.isRegistered(PSJsonMappingErrorResponse.class));
    assertTrue(application.isRegistered(PSUncaughtError.class));
    assertTrue(application.isRegistered(JacksonXmlBindJsonProvider.class));

    assertFalse(application.getClasses().isEmpty());
  }
}
