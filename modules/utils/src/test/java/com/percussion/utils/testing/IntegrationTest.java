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

package com.percussion.utils.testing;

/**
<<<<<<< HEAD
 * Add an annotation to a test to mark it as an integration test for Maven @Tag("IntegrationTest")
=======
 * Add an annotation to a test to mark it as an integration test for
 * Maven @Category(IntegrationTest.class)
>>>>>>> development-8.1.x
 *
 * <p>add to surefire plugin
 * <excludedGroups>com.percussion.utils.testing.IntegrationTest</excludedGroups>
 *
 * <p>add to failsafe plugin
 *
 * <p><groups>com.percussion.utils.testing.IntegrationTest</groups>
 *
 * @author stephenbolton
 */
public interface IntegrationTest {}
