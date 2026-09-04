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

package com.percussion.rest.test.apibridge;

import com.percussion.rest.applicationfiles.ApplicationFileSummary;
import com.percussion.rest.applicationfiles.IApplicationFileAdaptor;
import java.util.List;
import org.springframework.stereotype.Component;

/** Test adaptor for Application Files API bridge (MainTest Spring context). */
@Component
public class TestApplicationFileAdaptor implements IApplicationFileAdaptor {

  @Override
  public List<ApplicationFileSummary> listFiles(String appName) {
    return List.of();
  }

  @Override
  public ApplicationFileSummary getFile(String appName, String relativePath) {
    return null;
  }

  @Override
  public ApplicationFileSummary putFile(
      String appName, String relativePath, ApplicationFileSummary body) {
    return null;
  }
}
