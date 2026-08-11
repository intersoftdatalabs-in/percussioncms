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
/*
 * com.percussion.pso.utils PSORequestContext.java
 *
 * @author DavidBenua
 *
 */
package com.percussion.pso.utils;

import com.percussion.server.PSRequest;
import com.percussion.server.PSRequestContext;
import com.percussion.system.utils.IPSHtmlParameters;

/**
 * A system request that overrides the PSRequestContext. Use this class to obtain an
 * IPSRequestContext for the system user (RxServer).
 *
 * <p>{@link PSRequestContext} still exposes several raw-typed methods that implement generic
 * {@code IPSRequestContext} methods. Compiling any subclass under {@code -Xlint} re-emits those
 * unchecked conversion diagnostics here; they are suppressed until perc-system types the parent
 * API. Prefer fixing {@code PSRequestContext} in a dedicated perc-system change.
 *
 * @author DavidBenua
 */
@SuppressWarnings({"unchecked", "rawtypes"}) // parent PSRequestContext raw → IPSRequestContext
public final class PSORequestContext extends PSRequestContext {
  /**
   * Gets a the system user request. This system request is always forced to be local to the server,
   * even if the original user request came from elsewhere.
   */
  public PSORequestContext() {
    super(PSRequest.getContextForRequest(true));
  }

  /**
   * Gets the system user request, specifying a community.
   *
   * @param CommunityId community id to set on the system request
   */
  @SuppressWarnings("this-escape") // setPrivateObject on parent is overridable; no typed API
  public PSORequestContext(String CommunityId) {
    super(PSRequest.getContextForRequest(true));
    super.setPrivateObject(IPSHtmlParameters.SYS_COMMUNITY, CommunityId);
  }

  /**
   * This method always returns <code>false</code>. System requests cannot trace, beccause there is
   * no home application.
   *
   * @see com.percussion.server.IPSRequestContext#isTraceEnabled()
   * @return the result
   */
  @Override
  public boolean isTraceEnabled() {
    return false;
  }

  /**
   * Sets the user community.
   *
   * @param communityId the Community Id to set.
   */
  public void setCommunity(String communityId) {
    super.setPrivateObject(IPSHtmlParameters.SYS_COMMUNITY, communityId);
  }
}
