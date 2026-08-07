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

package com.percussion.soln.linkback.servlet;

import com.percussion.system.utils.IPSHtmlParameters;
import java.util.List;

/**
 * Linkback controller to redirect to Rhythmyx Content Explorer. The redirect path is internal (hard
 * coded), so there is no need to specify <code>redirectPath</code> in bean configuration. Recommend
 * to set <code>helpViewName</code>.
 *
 * <p>Marked {@code final} so constructor configuration via parent setters cannot observe a
 * partially constructed subclass (javac {@code this-escape}).
 */
public final class ContentExplorerLinkbackController extends GenericLinkbackController {

  private static final String REDIRECT_PATH = "/sys_cx/mainpage.html";

  /** Creates a controller configured for the Content Explorer redirect. */
  public ContentExplorerLinkbackController() {
    super();
    setRedirectPath(REDIRECT_PATH);
    setRequiredParameterNames(List.of(IPSHtmlParameters.SYS_CONTENTID));
  }
}
