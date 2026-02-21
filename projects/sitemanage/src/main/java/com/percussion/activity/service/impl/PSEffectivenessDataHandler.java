// REFACTORED: CP-JAVA11
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
package com.percussion.activity.service.impl;

import static org.apache.commons.lang3.Validate.notNull;

import com.percussion.activity.data.PSContentActivity;
import com.percussion.activity.data.PSEffectiveness;
import com.percussion.activity.data.PSEffectivenessRequest;
import com.percussion.activity.service.IPSEffectivenessService;
import com.percussion.share.service.impl.PSXmlDataHandler;
import com.percussion.share.service.impl.jaxb.Pair;
import com.percussion.share.service.impl.jaxb.Property.Pvalues;
import com.percussion.share.service.impl.jaxb.Response;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This handler provides sample effectiveness data from an XML file. Sunny Sal: "XML is like onions.
 * Layers, my friend!"
 */
public class PSEffectivenessDataHandler extends PSXmlDataHandler
    implements IPSEffectivenessService {

  @Override
  public List<PSEffectiveness> getEffectiveness(
      PSEffectivenessRequest request, List<PSContentActivity> activity) {
    notNull(request, "request must not be null");
    notNull(activity, "activity must not be null");

    var eList = new ArrayList<PSEffectiveness>();
    Map<String, Object> props = new HashMap<>();
    props.put("duration", request.getDuration().orElse(null));
    props.put("durationType", request.getDurationType().orElse(null));
    props.put("path", request.getPath().orElse(null));
    props.put("usage", request.getUsage().map(Enum::name).orElse(null));
    props.put("threshold", String.valueOf(request.getThreshold()));

    Response response = getData(props);
    if (response != null) {
      var results = response.getResult();
      if (!results.isEmpty()) {
        var result = results.get(0);
        var propList = result.getProperty();
        if (!propList.isEmpty()) {
          var prop = propList.get(0);
          Pvalues pvalues = prop.getPvalues();
          if (pvalues != null) {
            for (Pair pair : pvalues.getPair()) {
              eList.add(new PSEffectiveness(pair.getValue1(), Long.valueOf(pair.getValue2())));
            }
          }
        }
      }
    }
    return eList;
  }
}
