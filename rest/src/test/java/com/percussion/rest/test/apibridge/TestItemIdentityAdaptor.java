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

import com.percussion.rest.explorer.IItemIdentityAdaptor;
import com.percussion.rest.explorer.ItemIdentity;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

/** Spring test stub so rest ApplicationContext can inject {@link IItemIdentityAdaptor}. */
@Component
@Lazy
public class TestItemIdentityAdaptor implements IItemIdentityAdaptor {

  @Override
  public List<ItemIdentity> lookupBatch(URI baseUri, List<String> ids) {
    List<ItemIdentity> out = new ArrayList<>();
    if (ids == null) {
      return out;
    }
    for (String id : ids) {
      ItemIdentity identity = new ItemIdentity(id);
      identity.setCheckoutStatus("");
      out.add(identity);
    }
    return out;
  }
}
