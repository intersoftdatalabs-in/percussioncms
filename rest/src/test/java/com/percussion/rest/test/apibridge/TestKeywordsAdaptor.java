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

import com.percussion.rest.keywords.IKeywordsAdaptor;
import com.percussion.rest.keywords.KeywordSummary;
import java.net.URI;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Spring test stub for {@link IKeywordsAdaptor}. Required after {@code KeywordsResource} switched
 * to constructor injection so the rest module ApplicationContext can start.
 */
@Component
@Lazy
public class TestKeywordsAdaptor implements IKeywordsAdaptor {

  @Override
  public List<KeywordSummary> listKeywords(URI baseUri, boolean includeChoices) {
    return List.of();
  }

  @Override
  public KeywordSummary getKeyword(URI baseUri, String idOrValue) {
    return null;
  }

  @Override
  public KeywordSummary createKeyword(URI baseUri, KeywordSummary body) {
    return null;
  }

  @Override
  public KeywordSummary updateKeyword(URI baseUri, String id, KeywordSummary body) {
    return null;
  }

  @Override
  public void deleteKeyword(URI baseUri, String id) {
    // no-op for tests
  }
}
