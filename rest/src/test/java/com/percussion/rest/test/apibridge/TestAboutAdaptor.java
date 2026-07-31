/*
 * Copyright 1999-2026 Percussion Software, Inc.
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

import com.percussion.rest.about.AboutDetail;
import com.percussion.rest.about.IAboutAdaptor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Spring test stub for {@link IAboutAdaptor}. Required for ApplicationContext load after
 * constructor injection on {@code AboutResource}.
 */
@Component
@Lazy
public class TestAboutAdaptor implements IAboutAdaptor {

  @Override
  public AboutDetail getAbout() {
    return new AboutDetail();
  }
}
