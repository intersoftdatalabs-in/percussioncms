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

package com.percussion.apibridge;

import com.percussion.rest.GuidList;
import com.percussion.rest.ObjectTypeEnum;
import com.percussion.rest.communities.Community;
import com.percussion.rest.communities.CommunityList;
import com.percussion.rest.communities.CommunityVisibilityList;
import com.percussion.rest.communities.ICommunityAdaptor;
import com.percussion.system.utils.PSSiteManageBean;
import com.percussion.utils.request.PSRequestInfo;
import com.percussion.webservices.PSErrorResultsException;
import com.percussion.webservices.security.IPSSecurityDesignWs;
import com.percussion.webservices.system.IPSSystemWs;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;

@PSSiteManageBean
public class CommunityAdaptor implements ICommunityAdaptor {

  @Autowired private IPSSecurityDesignWs securityDesignWs;

  @Autowired private IPSSystemWs systemWs;

  /***
   * Create one or more communities by name and return the results
   * @param names
   * @return
   */
  @Override
  public CommunityList createCommunities(List<String> names) {
    CommunityList ret;
    var communities = new ArrayList<Community>();

    var session = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
    var user = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);

    var ps_communities = securityDesignWs.createCommunities(names, session, user);

    for (var c : ps_communities) {
      communities.add(ApiUtils.convertPSCommunity(c));
    }

    ret = new CommunityList(communities);

    return ret;
  }

  @Override
  public CommunityList findCommunities(String name) {
    var ps_summaries = securityDesignWs.findCommunities(name);
    var communities = new ArrayList<Community>();
    for (var s : ps_summaries) {
      communities.add(
          new Community(
              s.getGUID().longValue(),
              ApiUtils.convertGuid(s.getGUID()),
              s.getName(),
              s.getDescription(),
              s.getLabel()));
    }
    return new CommunityList(communities);
  }

  @Override
  public CommunityList loadCommunities(GuidList ids, boolean lock, boolean overrideLock)
      throws PSErrorResultsException {
    var session = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
    var user = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);

    var ps_communities =
        securityDesignWs.loadCommunities(
            ApiUtils.convertGuids(ids), lock, overrideLock, session, user);

    return ApiUtils.convertPSCommunities(ps_communities);
  }

  @Override
  public void saveCommunities(CommunityList communities, boolean release) {
    var session = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
    var user = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);

    securityDesignWs.saveCommunities(
        ApiUtils.convertCommunityList(communities), release, session, user);
  }

  @Override
  public void deleteCommunities(GuidList ids, boolean ignoreDependencies) {
    var session = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
    var user = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);

    securityDesignWs.deleteCommunities(
        ApiUtils.convertGuids(ids), ignoreDependencies, session, user);
  }

  @Override
  public CommunityVisibilityList getVisibilityByCommunity(GuidList ids, ObjectTypeEnum type)
      throws PSErrorResultsException, RemoteException {
    var session = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_JSESSIONID);
    var user = (String) PSRequestInfo.getRequestInfo(PSRequestInfo.KEY_USER);

    var ps_visibilities =
        securityDesignWs.getVisibilityByCommunity(
            ApiUtils.convertGuids(ids), ApiUtils.convertObjectTypeEnum(type), session, user);

    return new CommunityVisibilityList(ApiUtils.convertPSCommunityVisibilities(ps_visibilities));
  }

  @Override
  public void switchCommunity(String name) {
    systemWs.switchCommunity(name);
  }
}
