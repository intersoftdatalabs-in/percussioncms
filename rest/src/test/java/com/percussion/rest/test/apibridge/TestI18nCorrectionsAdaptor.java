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

import com.percussion.rest.i18n.I18nCorrectionResult;
import com.percussion.rest.i18n.I18nCorrectionSubmission;
import com.percussion.rest.i18n.I18nCorrectionsAdaptor;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Spring test stub for {@link I18nCorrectionsAdaptor}. Required for ApplicationContext load after
 * constructor injection on {@code I18nCorrectionsResource}.
 *
 * <p>HTTP-layer behavior is covered by {@link com.percussion.rest.i18n.I18nCorrectionsResourceTest}
 * (Mockito).
 */
@Component
@Lazy
public class TestI18nCorrectionsAdaptor implements I18nCorrectionsAdaptor {

  @Override
  public I18nCorrectionResult submit(I18nCorrectionSubmission submission) {
    return I18nCorrectionResult.ok("test-message-id");
  }
}
