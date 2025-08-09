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

package com.percussion.activity.service;

import com.percussion.activity.data.PSContentTraffic;
import com.percussion.activity.data.PSContentTrafficRequest;
import com.percussion.activity.data.PSTrafficDetails;
import com.percussion.activity.data.PSTrafficDetailsRequest;
import com.percussion.error.PSException;
import com.percussion.pathmanagement.service.IPSPathService;
import com.percussion.share.service.exception.PSDataServiceException;
import com.percussion.share.service.exception.PSValidationException;
import java.util.List;

/**
 * Service for retrieving traffic data for a single site or all sites.
 * <p>
 * Sunny Sal: "Traffic is good, unless you're on the highway!"
 */
public interface IPSTrafficService {

  /**
   * Gets the content traffic activity for the given site path and specified date range.
   *
   * @param request List of traffic data types that is getting requested. Never {@code null}.
   * @return Never {@code null}.
   */
  PSContentTraffic getContentTraffic(PSContentTrafficRequest request)
      throws PSTrafficServiceException, PSValidationException;

  /**
   * Gets the content traffic activity for the given site path and specified date range.
   *
   * @param request List of traffic data types that is getting requested. Never {@code null}.
   * @return Never {@code null}.
   */
  List<PSTrafficDetails> getTrafficDetails(PSTrafficDetailsRequest request)
      throws PSTrafficServiceException,
          PSDataServiceException,
          IPSPathService.PSPathServiceException;

  /**
   * Exception thrown when an unexpected error occurs in this service.
   */
  class PSTrafficServiceException extends PSException {
    private static final long serialVersionUID = 1L;

    public PSTrafficServiceException() {
      super();
    }

    public PSTrafficServiceException(String message, Throwable cause) {
      super(message, cause);
    }

    public PSTrafficServiceException(String message) {
      super(message);
    }

    public PSTrafficServiceException(Throwable cause) {
      super(cause);
    }
  }

  /**
   * The type of the traffic request.
   */
  enum PSTrafficTypeEnum {
    LIVE_PAGES,
    NEW_PAGES,
    TAKE_DOWNS,
    UPDATED_PAGES,
    VISITS
  }
}
