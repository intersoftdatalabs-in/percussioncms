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

import com.percussion.rest.sharedfields.ISharedFieldsAdaptor;
import com.percussion.rest.sharedfields.SharedFieldControlProperties;
import com.percussion.rest.sharedfields.SharedFieldGroupDetail;
import com.percussion.rest.sharedfields.SharedFieldGroupSummary;
import com.percussion.rest.sharedfields.SharedFieldSummary;
import java.net.URI;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Spring test stub for {@link ISharedFieldsAdaptor}. Required for ApplicationContext load after
 * constructor injection on {@code SharedFieldsResource}.
 *
 * <p>Admin 403 is <em>not</em> a global JAX-RS filter; production {@code SharedFieldsAdaptor}
 * enforces it. This stub is only a Spring bean for {@code MainTest} and does not model AuthZ.
 */
@Component
@Lazy
public class TestSharedFieldsAdaptor implements ISharedFieldsAdaptor {

  @Override
  public List<SharedFieldGroupSummary> listGroups(URI baseUri) {
    return List.of();
  }

  @Override
  public SharedFieldGroupDetail getGroup(URI baseUri, String name) {
    return null;
  }

  @Override
  public SharedFieldGroupDetail createGroup(URI baseUri, SharedFieldGroupDetail body) {
    return null;
  }

  @Override
  public SharedFieldGroupDetail updateGroup(
      URI baseUri, String name, SharedFieldGroupDetail body) {
    return null;
  }

  @Override
  public void deleteGroup(URI baseUri, String name) {
    // no-op for tests
  }

  @Override
  public SharedFieldGroupDetail addField(URI baseUri, String groupName, SharedFieldSummary body) {
    return null;
  }

  @Override
  public void deleteField(URI baseUri, String groupName, String fieldName) {
    // no-op for tests
  }

  @Override
  public SharedFieldControlProperties getFieldControlProperties(
      URI baseUri, String idOrName, String fieldName) {
    return null;
  }

  @Override
  public SharedFieldControlProperties replaceFieldControlProperties(
      URI baseUri, String idOrName, String fieldName, SharedFieldControlProperties body) {
    return null;
  }
}
