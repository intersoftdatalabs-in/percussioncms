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

package com.percussion.rest.relationsummary;

import com.percussion.share.relationship.data.PSLocalDependencySummary;
import com.percussion.share.relationship.data.PSNodeRelationshipSummary;
import com.percussion.share.relationship.data.PSRelationshipSummary;
import com.percussion.share.relationship.data.PSTaxonomySummary;
import java.net.URI;
import java.util.Collections;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/**
 * Stub {@link IRelationshipSummaryAdaptor} for {@link com.percussion.rest.MainTest}'s Spring
 * component-scan. Without this bean, context load fails because {@link
 * RelationshipSummaryResource} constructor-injects the adaptor.
 *
 * <p>HTTP-layer behavior is covered by {@link RelationshipSummaryResourceTest} (Mockito); this
 * class only satisfies wiring for the shared CXF/Spring harness.
 */
@Component
@Lazy
public class RelationshipSummaryTestAdaptor implements IRelationshipSummaryAdaptor {

  @Override
  public PSRelationshipSummary outgoing(URI baseURI, String itemId) {
    return emptyRelationshipSummary();
  }

  @Override
  public PSRelationshipSummary incoming(URI baseURI, String itemId) {
    return emptyRelationshipSummary();
  }

  @Override
  public PSTaxonomySummary taxonomy(URI baseURI, String itemId) {
    return new PSTaxonomySummary(0L, Collections.emptyList());
  }

  @Override
  public PSLocalDependencySummary local(URI baseURI, String itemId) {
    return new PSLocalDependencySummary(0L, Collections.emptyList());
  }

  @Override
  public PSRelationshipSummary reverse(URI baseURI, String itemId) {
    return emptyRelationshipSummary();
  }

  @Override
  public PSNodeRelationshipSummary summary(URI baseURI, String itemId) {
    return new PSNodeRelationshipSummary();
  }

  private static PSRelationshipSummary emptyRelationshipSummary() {
    return new PSRelationshipSummary(0L, Collections.emptyList());
  }
}
