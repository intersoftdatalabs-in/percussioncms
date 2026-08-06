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
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.percussion.rest.relationsummary;

import com.percussion.share.relationship.data.PSLocalDependencySummary;
import com.percussion.share.relationship.data.PSNodeRelationshipSummary;
import com.percussion.share.relationship.data.PSRelationshipSummary;
import com.percussion.share.relationship.data.PSTaxonomySummary;
import java.net.URI;

/**
 * Adaptor interface for the modern Content Explorer's relationship summary REST surface (US8 /
 * T098). Follows the {@code rest/} module's adaptor pattern: HTTP concerns live on the resource;
 * the adaptor interface is the contract the resource consumes; the sitemanage {@code apibridge}
 * implementation delegates to {@code IPSRelationshipSummaryService}.
 *
 * <p>DTOs live in this module ({@code com.percussion.share.relationship.data}) so rest does not
 * depend on sitemanage (avoids a Maven reactor cycle: sitemanage → rest).
 *
 * <p>The default adaptor raises a {@code WebApplicationException} with HTTP 403 on AuthZ denial; no
 * exception mapper is required because JAX-RS translates the status code automatically.
 *
 * @author Kilo (US8 / T098)
 */
public interface IRelationshipSummaryAdaptor {

  PSRelationshipSummary outgoing(URI baseURI, String itemId);

  PSRelationshipSummary incoming(URI baseURI, String itemId);

  PSTaxonomySummary taxonomy(URI baseURI, String itemId);

  PSLocalDependencySummary local(URI baseURI, String itemId);

  PSRelationshipSummary reverse(URI baseURI, String itemId);

  PSNodeRelationshipSummary summary(URI baseURI, String itemId);
}
