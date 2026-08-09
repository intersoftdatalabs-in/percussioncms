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

import com.percussion.rest.translations.CreateTranslationsRequest;
import com.percussion.rest.translations.CreateTranslationsResult;
import com.percussion.rest.translations.IContentTranslationsAdaptor;
import com.percussion.rest.translations.ItemTranslationVariants;
import java.net.URI;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Spring test stub for {@link IContentTranslationsAdaptor}. Required for ApplicationContext load
 * after constructor injection on {@code ContentTranslationsResource}.
 *
 * <p>HTTP-layer behavior is covered by {@link
 * com.percussion.rest.translations.ContentTranslationsResourceTest}; domain path by sitemanage
 * apibridge tests.
 */
@Component
@Lazy
public class TestContentTranslationsAdaptor implements IContentTranslationsAdaptor {

  @Override
  public CreateTranslationsResult createTranslations(
      URI baseUri, CreateTranslationsRequest request) {
    return new CreateTranslationsResult(List.of());
  }

  @Override
  public ItemTranslationVariants listItemVariants(URI baseUri, String itemId) {
    ItemTranslationVariants out = new ItemTranslationVariants();
    out.setItemId(0L);
    out.setVariants(List.of());
    return out;
  }
}
