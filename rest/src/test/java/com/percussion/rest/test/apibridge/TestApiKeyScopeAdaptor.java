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

import com.percussion.rest.apikeys.ApiKeyAssignment;
import com.percussion.rest.apikeys.ApiKeyScope;
import com.percussion.rest.apikeys.ApiKeyScopeCatalog;
import com.percussion.rest.apikeys.ApiKeyScopePreset;
import com.percussion.rest.apikeys.IApiKeyScopeAdaptor;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Spring test stub for {@link IApiKeyScopeAdaptor}. Required for ApplicationContext load after
 * constructor injection on {@code ApiKeysResource}.
 *
 * <p>Admin 403 is <em>not</em> a global JAX-RS filter; production {@code ApiKeyScopeAdaptor}
 * enforces it. This stub is only a Spring bean for {@code MainTest} and does not model AuthZ.
 */
@Component
@Lazy
public class TestApiKeyScopeAdaptor implements IApiKeyScopeAdaptor {

  @Override
  public List<ApiKeyScope> listScopes() {
    return ApiKeyScopeCatalog.scopes();
  }

  @Override
  public List<ApiKeyScopePreset> listPresets() {
    return ApiKeyScopeCatalog.presets();
  }

  @Override
  public List<ApiKeyAssignment> listKeys() {
    return List.of();
  }

  @Override
  public ApiKeyAssignment getKey(String name) {
    return null;
  }

  @Override
  public ApiKeyAssignment saveKey(String name, ApiKeyAssignment body) {
    return null;
  }
}
